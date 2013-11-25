
package org.raml.jaxrs.codegen.core.model;

import java.io.File;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.ws.rs.HttpMethod;

import org.apache.commons.lang.Validate;

import com.sun.codemodel.JAnnotationUse;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JDocComment;
import com.sun.codemodel.JPackage;

public class CustomHttpMethod implements JavaRenderable
{
    private final String httpMethodName;

    public CustomHttpMethod(final String name)
    {
        Validate.notEmpty(name, "name can't be empty");

        httpMethodName = name.toUpperCase();
    }

    public void toJava(final File outputDirectory) throws Exception
    {
        final JCodeModel codeModel = new JCodeModel();
        final JPackage pkg = codeModel._package(JavaRenderable.SUPPORT_CLASSES_PACKAGE);
        final JDefinedClass clazz = pkg._annotationTypeDeclaration(httpMethodName);

        JAnnotationUse annotation = clazz.annotate(Target.class);
        annotation.param("value", ElementType.METHOD);
        annotation = clazz.annotate(Retention.class);
        annotation.param("value", RetentionPolicy.RUNTIME);
        annotation = clazz.annotate(HttpMethod.class);
        annotation.param("value", httpMethodName);

        final JDocComment jDocComment = clazz.javadoc();
        jDocComment.add("Custom JAX-RS support for HTTP " + httpMethodName + ".");

        codeModel.build(outputDirectory);
    }
}
