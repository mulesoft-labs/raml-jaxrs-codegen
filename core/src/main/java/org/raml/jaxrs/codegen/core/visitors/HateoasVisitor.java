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
import static com.sun.codemodel.JMod.FINAL;
import static com.sun.codemodel.JMod.PRIVATE;
import static com.sun.codemodel.JMod.PUBLIC;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.raml.jaxrs.codegen.core.Names;
import org.raml.jaxrs.codegen.core.dataobjects.ResourceInterface;
import org.raml.jaxrs.codegen.core.dataobjects.ResourceMethod;
import org.raml.jaxrs.codegen.core.dataobjects.ResourceMethodArgument;
import org.raml.jaxrs.codegen.core.dataobjects.ResponseClass;
import org.raml.jaxrs.codegen.core.dataobjects.ResponseClassMethod;
import org.raml.jaxrs.codegen.core.support.RamlHelper;
import org.raml.model.parameter.AbstractParam;

import com.sun.codemodel.JClass;
import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JPackage;
import com.sun.codemodel.JType;
import com.sun.codemodel.JVar;

public class HateoasVisitor extends TemplateResourceVisitor {

  private final static String PACKAGE_NAME = "link";
  private final static String LINK_BUILDER_NAME = "LinkBuilder";

  /*
   * (non-Javadoc)
   * @see org.raml.jaxrs.codegen.core.ramlresource.RamlObjectVisitor#visit(org.raml.jaxrs.codegen.core.ramlresource.util.dataobjects.ResourceMethodParameter)
   */
  @Override
  public void visit(ResourceMethodArgument parameter) {

    if(!getConfiguration().isHateoasResourceSupport()) {
      return;
    }

    AbstractParam abstractParam = parameter.getParameter();
    if(abstractParam != null) {
      ResourceMethod resourceMethod = parameter.getResourceMethod();
      ResponseClass responseClass = resourceMethod.getResponseClass();
      if(responseClass == null) {
        return;
      }
      JType argumentType = parameter.getArgumentType().getGeneratedObject();
      String linkBuilderName = getPathPackage(responseClass) + "." + getLinkBuilderName(responseClass);

      String name = Names.buildVariableName(parameter.getHeaderName());
      JDefinedClass linkBuilderClass = getCodeModel()._getClass(linkBuilderName);
      JMethod jMethod = linkBuilderClass.method(PUBLIC, linkBuilderClass, "set" + StringUtils.capitalize(name));
      JVar argument = jMethod.param(argumentType, name);

      switch (parameter.getArgumentType().getType()) {
        case QUERY:
          jMethod.body().invoke(JExpr.ref("queryArgs"), "put").
          arg(JExpr.lit(name)).arg(argument);
          break;
        default:
          jMethod.body().invoke(JExpr.ref("pathArgs"), "put").
          arg(JExpr.lit(name)).arg(argument);
          break;
      }

      jMethod.body()._return(JExpr._this());
    }
  }

  /*
   * (non-Javadoc)
   * @see org.raml.jaxrs.codegen.core.ramlresource.RamlObjectVisitor#visit(org.raml.jaxrs.codegen.core.ramlresource.util.dataobjects.ResponseClass)
   */
  @Override
  public void visit(ResponseClass responseClass) {

    //Resource path constants
    if(!getConfiguration().isHateoasResourceSupport()) {
      return;
    }

    JPackage pkg = getCodeModel()._package(getPathPackage(responseClass));
    ResourceInterface resourceInterface = responseClass.getResourceMethod().getResourceInterface();
    String resourceInterfacePath = RamlHelper.getResourceInterfacePath(resourceInterface);
    if(!StringUtils.isBlank(resourceInterfacePath)) {
      resourceInterfacePath += "/";
    }

    try {
      String resourceMethodName = responseClass.getResourceMethod().getGeneratedObject().name();
      JDefinedClass linkClass = pkg._class(getLinkBuilderName(responseClass));
      linkClass.field(
        PRIVATE + FINAL,
        getCodeModel().ref(Map.class).narrow(String.class, Object.class),
        "pathArgs", JExpr._new(getCodeModel().ref(HashMap.class).narrow(String.class, Object.class)));
      linkClass.field(
        PRIVATE + FINAL,
        getCodeModel().ref(Map.class).narrow(String.class, Object.class),
        "queryArgs", JExpr._new(getCodeModel().ref(HashMap.class).narrow(String.class, Object.class)));

      linkClass.field(PRIVATE, String.class, "rel");
      JMethod jMethodRel = linkClass.method(PUBLIC, linkClass, "rel");
      jMethodRel.body().assign(JExpr._this().ref("rel"), jMethodRel.param(String.class, "rel"));
      jMethodRel.body()._return(JExpr._this());

      JClass jClassLink = getCodeModel().ref("javax.ws.rs.core.Link");
      JMethod jMethod = linkClass.method(PUBLIC, jClassLink, "build");

      jMethod.body()._return(
        getCodeModel().ref(getConfiguration().getSupportPackage() + "." + LINK_BUILDER_NAME).
        staticInvoke("start").
        arg(JExpr.dotclass(resourceInterface.getGeneratedObject())).
        arg(JExpr.lit(resourceMethodName)).
        arg(JExpr.ref("queryArgs")).
        arg(JExpr.ref("pathArgs")).
        invoke("rel").arg(JExpr.ref("rel")).
        invoke("build"));
    } catch (JClassAlreadyExistsException e) {
      throw new RuntimeException(e.getMessage(), e);
    }


    try {
      String supportPackage = getConfiguration().getSupportPackage();
      String template = IOUtils.toString(getClass().getResourceAsStream(
          "/org/raml/templates/LinkBuilder.template"));

      File supportPackageOutputDirectory = new File(getConfiguration().getOutputDirectory(), supportPackage.replace('.', File.separatorChar));
      File sourceOutputFile = new File(supportPackageOutputDirectory, LINK_BUILDER_NAME +".java");
      if(sourceOutputFile.exists()) {
        return;
      }

      supportPackageOutputDirectory.mkdirs();

      final String source = template.replace("${codegen.support.package}", supportPackage);
      final FileWriter fileWriter = new FileWriter(sourceOutputFile);

      IOUtils.write(source, fileWriter);
      IOUtils.closeQuietly(fileWriter);

      getSchemaRepository().saveSupport(supportPackage.replace('.', '/') + "/" + LINK_BUILDER_NAME + ".java", sourceOutputFile);
    } catch (IOException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  /*
   * (non-Javadoc)
   * @see org.raml.jaxrs.codegen.core.visitors.TemplateResourceVisitor#visit(org.raml.jaxrs.codegen.core.dataobjects.ResponseClassMethod)
   */
  @Override
  public void visit(ResponseClassMethod responseClassMethod) {
  }

  private String getLinkBuilderName(ResponseClass responseClass) {
    String resourceMethodName = responseClass.getResourceMethod().getGeneratedObject().name();
    return StringUtils.capitalize(resourceMethodName)  + "LinkBuilder";
  }

  private String getPathPackage(ResponseClass responseClass) {
    String supportSubPackage = RamlHelper.getResourceInterfaceName(responseClass.getResourceMethod().getResourceInterface().getGeneratedObject().name(), getConfiguration().getResourcePostfix());
    return getConfiguration().getSupportPackage() + "." + supportSubPackage.toLowerCase() + "." + PACKAGE_NAME;
  }
}