
package org.raml.jaxrs.codegen.core;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.HttpMethod;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import com.sun.codemodel.JAnnotatable;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JPackage;
import com.sun.codemodel.JType;

class Context
{
    private static final Map<String, Object> HTTP_METHOD_ANNOTATIONS = new HashMap<String, Object>();
    static
    {
        for (final Class<? extends Annotation> clazz : Constants.JAXRS_HTTP_METHODS)
        {
            HTTP_METHOD_ANNOTATIONS.put(clazz.getSimpleName(), clazz);
        }
    }

    private final Configuration configuration;
    private final JCodeModel codeModel;
    private JDefinedClass currentResourceInterface;

    private final Map<String, Set<String>> resourcesMethods;

    public Context(final Configuration configuration)
    {
        Validate.notNull(configuration, "configuration can't be null");
        this.configuration = configuration;

        codeModel = new JCodeModel();

        resourcesMethods = new HashMap<String, Set<String>>();
    }

    public Configuration getConfiguration()
    {
        return configuration;
    }

    public JDefinedClass getCurrentResourceInterface()
    {
        return currentResourceInterface;
    }

    public void setCurrentResourceInterface(final JDefinedClass currentResourceInterface)
    {
        this.currentResourceInterface = currentResourceInterface;
    }

    public List<String> generate() throws IOException
    {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final PrintStream ps = new PrintStream(baos);
        codeModel.build(configuration.getOutputDirectory(), ps);
        ps.close();
        return Arrays.asList(StringUtils.split(baos.toString()));
    }

    public JDefinedClass createResourceInterface(final String name) throws Exception
    {
        String actualName;
        int i = -1;
        while (true)
        {
            actualName = name + (++i == 0 ? "" : Integer.toString(i));
            if (!resourcesMethods.containsKey(actualName))
            {
                resourcesMethods.put(actualName, new HashSet<String>());
                break;
            }
        }

        final JPackage pkg = codeModel._package(configuration.getBasePackageName() + ".resource");
        return pkg._interface(actualName);
    }

    public JMethod createResourceMethod(final JDefinedClass resourceInterface,
                                        final String methodName,
                                        final Class<?> returnClass)
    {
        final Set<String> existingMethodNames = resourcesMethods.get(resourceInterface.name());

        String actualMethodName;
        int i = -1;
        while (true)
        {
            actualMethodName = methodName + (++i == 0 ? "" : Integer.toString(i));
            if (!existingMethodNames.contains(actualMethodName))
            {
                existingMethodNames.add(actualMethodName);
                break;
            }
        }

        return resourceInterface.method(JMod.NONE, returnClass, actualMethodName);
    }

    public JDefinedClass createResourceEnum(final JDefinedClass resourceInterface,
                                            final String name,
                                            final List<String> values) throws Exception
    {
        final JDefinedClass _enum = resourceInterface._enum(name);

        for (final String value : values)
        {
            _enum.enumConstant(value);
        }

        return _enum;
    }

    @SuppressWarnings("unchecked")
    public Context addHttpMethodAnnotation(final String httpMethod, final JAnnotatable annotatable)
        throws Exception
    {
        final Object annotationClass = HTTP_METHOD_ANNOTATIONS.get(httpMethod.toUpperCase());
        if (annotationClass == null)
        {
            final JDefinedClass annotationClazz = createCustomHttpMethodAnnotation(httpMethod);
            annotatable.annotate(annotationClazz);
        }
        else if (annotationClass instanceof JClass)
        {
            annotatable.annotate((JClass) annotationClass);
        }
        else if (annotationClass instanceof Class)
        {
            annotatable.annotate((Class<? extends Annotation>) annotationClass);
        }
        else
        {
            throw new IllegalStateException("Found annotation: " + annotationClass + " for HTTP method: "
                                            + httpMethod);
        }

        return this;
    }

    public JType getGeneratorType(final Class<?> clazz)
    {
        return clazz.isPrimitive() ? JType.parse(codeModel, clazz.getSimpleName()) : codeModel.ref(clazz);
    }

    private JDefinedClass createCustomHttpMethodAnnotation(final String httpMethod)
        throws JClassAlreadyExistsException
    {
        final JPackage pkg = codeModel._package(configuration.getBasePackageName() + ".support");
        final JDefinedClass annotationClazz = pkg._annotationTypeDeclaration(httpMethod);
        annotationClazz.annotate(Target.class).param("value", ElementType.METHOD);
        annotationClazz.annotate(Retention.class).param("value", RetentionPolicy.RUNTIME);
        annotationClazz.annotate(HttpMethod.class).param("value", httpMethod);
        annotationClazz.javadoc().add("Custom JAX-RS support for HTTP " + httpMethod + ".");
        HTTP_METHOD_ANNOTATIONS.put(httpMethod.toUpperCase(), annotationClazz);
        return annotationClazz;
    }
}
