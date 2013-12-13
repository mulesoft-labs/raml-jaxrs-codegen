
package org.raml.jaxrs.codegen.core;

import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.apache.commons.lang.StringUtils.startsWith;
import static org.apache.commons.lang.WordUtils.capitalize;

import java.io.File;
import java.io.Reader;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private final Map<String, Class<?>> schemaClasses;

    public Types(final Context context)
    {
        Validate.notNull(context, "context can't be null");
        this.context = context;

        schemaClasses = new HashMap<String, Class<?>>();
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

    public JType getRequestEntityClass(final MimeType mimeType)
    {
        final Class<?> schemaClass = getSchemaClass(mimeType);

        if (schemaClass != null)
        {
            return getGeneratorType(schemaClass);
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

    public JType getResponseEntityClass(final MimeType mimeType)
    {
        final Class<?> schemaClass = getSchemaClass(mimeType);

        if (schemaClass != null)
        {
            return getGeneratorType(schemaClass);
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

    private Class<?> getSchemaClass(final MimeType mimeType)
    {
        // TODO generate DTOs from XML/JSON schema and use them instead of generic Object
        return isNotBlank(mimeType.getSchema()) ? schemaClasses.get(buildSchemaKey(mimeType)) : null;
    }

    private String buildSchemaKey(final MimeType mimeType)
    {
        return mimeType.getType() + "@" + mimeType.getSchema().hashCode();
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
