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

package org.raml.jaxrs.codegen.core.visitors;

import static org.apache.commons.lang.StringUtils.strip;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.apache.commons.lang.StringUtils;
import org.raml.jaxrs.codegen.core.Constants;
import org.raml.jaxrs.codegen.core.dataobjects.ResourceMethod;
import org.raml.jaxrs.codegen.core.support.RamlHelper;
import org.raml.model.MimeType;

import com.sun.codemodel.JAnnotationArrayMember;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JPackage;

public class HttpMethodAnnotationVisitor extends TemplateResourceVisitor {

  /**
   * Constants.
   */
  private static final String DEFAULT_ANNOTATION_PARAMETER = "value";

  /**
   * Locals.
   */
  private Map<String, Object> httpMethodAnnotations;

  /*
   * (non-Javadoc)
   * @see org.raml.jaxrs.codegen.core.visitor.ResourceVisitor#start()
   */
  @Override
  public void start() {
    httpMethodAnnotations = new HashMap<String, Object>();
    for (final Class<? extends Annotation> clazz : Constants.JAXRS_HTTP_METHODS) {
      httpMethodAnnotations.put(clazz.getSimpleName(), clazz);
    }
  }

  /*
   * (non-Javadoc)
   * @see org.raml.jaxrs.codegen.core.visitor.ResourceVisitor#visit(org.raml.jaxrs.codegen.core.dataobjects.ResourceMethod)
   */
  @SuppressWarnings("unchecked")
  @Override
  public void visit(ResourceMethod resourceMethod) {
    JMethod annotatable = resourceMethod.getGeneratedObject();

    String httpMethod = resourceMethod.getAction().getType().toString();
    final Object annotationClass = httpMethodAnnotations.get(httpMethod.toUpperCase());
    if (annotationClass == null) {
      try {
        final JPackage pkg = getCodeModel()._package(getConfiguration().getSupportPackage());
        final JDefinedClass annotationClazz = pkg._annotationTypeDeclaration(httpMethod);
        annotationClazz.annotate(Target.class).param("value", ElementType.METHOD);
        annotationClazz.annotate(Retention.class).param("value", RetentionPolicy.RUNTIME);
        annotationClazz.annotate(HttpMethod.class).param("value", httpMethod);
        annotationClazz.javadoc().add("Custom JAX-RS support for HTTP " + httpMethod + ".");
        annotatable.annotate(annotationClazz);

        httpMethodAnnotations.put(httpMethod.toUpperCase(), annotationClazz);
      } catch (JClassAlreadyExistsException e) {
        throw new RuntimeException(e.getMessage(), e);
      }
    } else if (annotationClass instanceof JClass) {
      annotatable.annotate((JClass) annotationClass);
    } else if (annotationClass instanceof Class) {
      annotatable.annotate((Class<? extends Annotation>) annotationClass);
    } else {
      throw new IllegalStateException("Found annotation: " + annotationClass + " for HTTP method: " + httpMethod);
    }

    String resourceInterfacePath = strip(resourceMethod.getResourceInterface().getResource().getRelativeUri(), "/");
    String path = StringUtils.substringAfter(resourceMethod.getAction().getResource().getUri(), resourceInterfacePath + "/");

    annotatable.annotate(Path.class).param(DEFAULT_ANNOTATION_PARAMETER, path);

    MimeType requestMimeType = resourceMethod.getMimeType();
    if (requestMimeType != null) {
      annotatable.annotate(Consumes.class).param(DEFAULT_ANNOTATION_PARAMETER, requestMimeType.getType());
    }

    List<String> responseContentTypes = new ArrayList<String>();
    for(MimeType mimeType : RamlHelper.getUniqueResponseMimeTypes(resourceMethod.getAction())) {
      responseContentTypes.add(mimeType.getType());
    }
    if (!responseContentTypes.isEmpty()) {
      final JAnnotationArrayMember paramArray = annotatable.annotate(Produces.class).paramArray(DEFAULT_ANNOTATION_PARAMETER);
      for (String responseMimeType : responseContentTypes) {
        paramArray.param(responseMimeType);
      }
    }
  }
}