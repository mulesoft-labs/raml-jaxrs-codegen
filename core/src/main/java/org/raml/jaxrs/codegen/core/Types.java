
package org.raml.jaxrs.codegen.core;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.apache.commons.lang.StringUtils.startsWith;
import static org.apache.commons.lang.WordUtils.capitalize;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.io.FileUtils;
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

        // TODO support XML schema

        if (MediaType.APPLICATION_JSON.equalsIgnoreCase(mimeType.getType()))
        {

            final String globalSchema = context.getGlobalSchema(schemaNameOrContent);

            String schema;
            final String className;

            if (globalSchema == null)
            {
                schema = schemaNameOrContent;
                // TODO improve name of embedded JSON schema
                className = "Anonymous" + Names.buildJavaFriendlyName(buildSchemaKey);
            }
            else
            {
                schema = globalSchema;
                className = Names.buildJavaFriendlyName(schemaNameOrContent);
            }

            // dump it to a temp file so the json schema generator can pick it up
            final File tempFile = File.createTempFile(className.toLowerCase() + "-", ".json");
            tempFile.deleteOnExit();
            FileUtils.writeStringToFile(tempFile, schema);

            final JClass generatedClass = context.generateClassFromJsonSchema(className, tempFile.toURI()
                .toURL());

            schemaClasses.put(buildSchemaKey, generatedClass);

            return generatedClass;
        }
        else
        {
            return null;
        }
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
                return usePrimitive ? double.class : Double.class;
            case STRING :
                return String.class;
            default :
                LOGGER.warn("Unsupported RAML type: " + parameter.getType().toString());
                return Object.class;
        }
    }
}
