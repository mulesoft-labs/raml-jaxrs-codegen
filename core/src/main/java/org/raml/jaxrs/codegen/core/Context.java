
package org.raml.jaxrs.codegen.core;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;

import org.apache.commons.lang.Validate;

import com.sun.codemodel.JAnnotatable;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JPackage;

class Context
{
    @SuppressWarnings("unchecked")
    private static final List<Class<? extends Annotation>> JAXRS_HTTP_METHODS = Arrays.asList(DELETE.class,
        GET.class, HEAD.class, OPTIONS.class, POST.class, PUT.class);

    private static final Map<String, Object> HTTP_METHOD_ANNOTATIONS = new HashMap<String, Object>();
    static
    {
        for (final Class<? extends Annotation> clazz : JAXRS_HTTP_METHODS)
        {
            HTTP_METHOD_ANNOTATIONS.put(clazz.getSimpleName(), clazz);
        }
    }

    private final Configuration configuration;
    private final JCodeModel codeModel;

    public Context(final Configuration configuration)
    {
        Validate.notNull(configuration, "configuration can't be null");
        this.configuration = configuration;

        codeModel = new JCodeModel();
    }

    public void generate() throws IOException
    {
        codeModel.build(configuration.getOutputDirectory());
    }

    public JDefinedClass createResourceInterface(final String name) throws Exception
    {
        final JPackage pkg = codeModel._package(configuration.getBasePackageName() + ".resource");
        return pkg._interface(name);
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
