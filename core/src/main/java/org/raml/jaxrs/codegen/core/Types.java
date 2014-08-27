/*
 * Copyright 2013 (c) MuleSoft, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */
package org.raml.jaxrs.codegen.core;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_XML;
import static javax.ws.rs.core.MediaType.TEXT_XML;
import static org.apache.commons.lang.StringUtils.defaultIfBlank;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.apache.commons.lang.StringUtils.startsWith;
import static org.apache.commons.lang.StringUtils.substringAfter;
import static org.apache.commons.lang.StringUtils.substringAfterLast;
import static org.apache.commons.lang.StringUtils.substringBefore;
import static org.apache.commons.lang.WordUtils.capitalize;
import static org.raml.jaxrs.codegen.core.Names.buildJavaFriendlyName;
import static org.raml.jaxrs.codegen.core.Names.buildNestedSchemaName;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.lang.Validate;
import org.raml.model.MimeType;
import org.raml.model.parameter.AbstractParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.codemodel.JClass;
import com.sun.codemodel.JType;

public class Types
{
    private static final Logger LOGGER = LoggerFactory.getLogger(Types.class);

    private final Context context;
    private final Map<String, JClass> schemaClasses;

    public Types(final Context context)
    {
        Validate.notNull(context, "context can't be null");

        this.context = context;

        schemaClasses = new HashMap<String, JClass>();
    }

    public JType buildParameterType(final AbstractParam parameter, final String name) throws Exception
    {
        if ((parameter.getEnumeration() != null) && (!parameter.getEnumeration().isEmpty()))
        {
            return context.createResourceEnum(context.getCurrentResourceInterface(), capitalize(name),
                parameter.getEnumeration());
        }

        final JType codegenType = context.getGeneratorType(getJavaType(parameter));

        if (parameter.isRepeat())
        {
            return ((JClass) context.getGeneratorType(List.class)).narrow(codegenType);
        }
        else
        {
            return codegenType;
        }
    }

    public JType getRequestEntityClass(final MimeType mimeType) throws IOException
    {
        final JClass schemaClass = getSchemaClass(mimeType);

        if (schemaClass != null)
        {
            return schemaClass;
        }
        else if (startsWith(mimeType.getType(), "text/"))
        {
            return getGeneratorType(String.class);
        }
        else if (MediaType.APPLICATION_OCTET_STREAM.equals(mimeType.getType()))
        {
            return getGeneratorType(InputStream.class);
        }
        else
        {
            // fallback to a generic reader
            return getGeneratorType(Reader.class);
        }
    }

    public JType getResponseEntityClass(final MimeType mimeType) throws IOException
    {
        final JClass schemaClass = getSchemaClass(mimeType);

        if (schemaClass != null)
        {
            return schemaClass;
        }
        else if (startsWith(mimeType.getType(), "text/"))
        {
            return getGeneratorType(String.class);
        }
        else
        {
            // fallback to a streaming output
            return getGeneratorType(StreamingOutput.class);
        }
    }

    public JType getGeneratorType(final Class<?> clazz)
    {
        return context.getGeneratorType(clazz);
    }

    public JClass getGeneratorClass(final Class<?> clazz)
    {
        return (JClass) context.getGeneratorType(clazz);
    }

    private JClass getSchemaClass(final MimeType mimeType) throws IOException
    {
        final String schemaNameOrContent = mimeType.getSchema();
        if (isBlank(schemaNameOrContent))
        {
            return null;
        }

        final String buildSchemaKey = buildSchemaKey(mimeType);

        final JClass existingClass = schemaClasses.get(buildSchemaKey);
        if (existingClass != null)
        {
            return existingClass;
        }

        if (isCompatibleWith(mimeType, APPLICATION_XML, TEXT_XML))
        {
            // TODO support XML schema
            return null;
        }
        else if (isCompatibleWith(mimeType, APPLICATION_JSON))
        {
            final Entry<File, String> schemaNameAndFile = context.getSchemaFile(schemaNameOrContent);
            if (isBlank(schemaNameAndFile.getValue()))
            {
                schemaNameAndFile.setValue(buildNestedSchemaName(mimeType));
            }

            final String className = buildJavaFriendlyName(schemaNameAndFile.getValue());
            final JClass generatedClass = context.generateClassFromJsonSchema(className,
                schemaNameAndFile.getKey().toURI().toURL());
            schemaClasses.put(buildSchemaKey, generatedClass);
            return generatedClass;
        }
        else
        {
            return null;
        }
    }

    private boolean isCompatibleWith(final MimeType mt, final String... mediaTypes)
    {
        final String mimeType = mt.getType();

        if (isBlank(mimeType))
        {
            return false;
        }

        for (final String mediaType : mediaTypes)
        {
            if (mediaType.toString().equals(mimeType))
            {
                return true;
            }

            final String primaryType = substringBefore(mimeType, "/");

            if (substringBefore(mediaType, "/").equals(primaryType))
            {
                final String subType = defaultIfBlank(substringAfterLast(mimeType, "+"),
                    substringAfter(mimeType, "/"));

                if (substringAfter(mediaType, "/").equals(subType))
                {
                    return true;
                }
            }
        }

        return false;
    }

    private String buildSchemaKey(final MimeType mimeType)
    {
        return Names.getShortMimeType(mimeType) + "@" + mimeType.getSchema().hashCode();
    }

    private static Class<?> getJavaType(final AbstractParam parameter)
    {
        if (parameter.getType() == null)
        {
            return String.class;
        }

        final boolean usePrimitive = !parameter.isRepeat()
                                     && (parameter.isRequired() || isNotBlank(parameter.getDefaultValue()));

        switch (parameter.getType())
        {
            case BOOLEAN :
                return usePrimitive ? boolean.class : Boolean.class;
            case DATE :
                return Date.class;
            case FILE :
                return File.class;
            case INTEGER :
                return usePrimitive ? long.class : Long.class;
            case NUMBER :
                return BigDecimal.class;
            case STRING :
                return String.class;
            default :
                LOGGER.warn("Unsupported RAML type: " + parameter.getType().toString());
                return Object.class;
        }
    }
}
