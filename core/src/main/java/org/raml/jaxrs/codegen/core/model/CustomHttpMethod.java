
package org.raml.jaxrs.codegen.core.model;

import java.io.File;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.ws.rs.HttpMethod;

import org.apache.commons.lang.Validate;

import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JPackage;

public class CustomHttpMethod implements JavaRenderable
{
    private final String httpMethodName;

    public CustomHttpMethod(final String name)
    {
        Validate.notEmpty(name, "name can't be empty");

        httpMethodName = name.toUpperCase();
    }

    @Override
    public void toJava(final File outputDirectory) throws Exception
    {
        final JCodeModel codeModel = new JCodeModel();
        final JPackage pkg = codeModel._package(JavaRenderable.SUPPORT_CLASSES_PACKAGE);
        final JDefinedClass clazz = pkg._annotationTypeDeclaration(httpMethodName);

        clazz.annotate(Target.class).param("value", ElementType.METHOD);
        clazz.annotate(Retention.class).param("value", RetentionPolicy.RUNTIME);
        clazz.annotate(HttpMethod.class).param("value", httpMethodName);

        clazz.javadoc().add("Custom JAX-RS support for HTTP " + httpMethodName + ".");

        codeModel.build(outputDirectory);
    }
}
