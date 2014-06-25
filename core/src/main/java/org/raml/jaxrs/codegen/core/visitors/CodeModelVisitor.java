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

import static com.sun.codemodel.JMod.PUBLIC;
import static com.sun.codemodel.JMod.STATIC;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_XML;
import static javax.ws.rs.core.MediaType.TEXT_XML;
import static org.apache.commons.lang.StringUtils.capitalize;
import static org.apache.commons.lang.StringUtils.defaultString;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.apache.commons.lang.StringUtils.startsWith;
import static org.apache.commons.lang.StringUtils.strip;
import static org.raml.jaxrs.codegen.core.Constants.RESPONSE_HEADER_WILDCARD_SYMBOL;
import static org.raml.jaxrs.codegen.core.Names.EXAMPLE_PREFIX;
import static org.raml.jaxrs.codegen.core.Names.GENERIC_PAYLOAD_ARGUMENT_NAME;
import static org.raml.jaxrs.codegen.core.Names.MULTIPLE_RESPONSE_HEADERS_ARGUMENT_NAME;
import static org.raml.jaxrs.codegen.core.Names.buildJavaFriendlyName;
import static org.raml.jaxrs.codegen.core.Names.buildNestedSchemaName;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.mail.internet.MimeMultipart;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.lang.StringUtils;
import org.raml.jaxrs.codegen.core.Names;
import org.raml.jaxrs.codegen.core.Types;
import org.raml.jaxrs.codegen.core.dataobjects.ArgumentType;
import org.raml.jaxrs.codegen.core.dataobjects.GlobalSchema;
import org.raml.jaxrs.codegen.core.dataobjects.ResourceEntity;
import org.raml.jaxrs.codegen.core.dataobjects.ResourceInterface;
import org.raml.jaxrs.codegen.core.dataobjects.ResourceMethod;
import org.raml.jaxrs.codegen.core.dataobjects.ResourceMethodArgument;
import org.raml.jaxrs.codegen.core.dataobjects.ResponseClass;
import org.raml.jaxrs.codegen.core.dataobjects.ResponseClassMethod;
import org.raml.jaxrs.codegen.core.dataobjects.ResponseClassMethodArgument;
import org.raml.jaxrs.codegen.core.support.CodeModelHelper;
import org.raml.jaxrs.codegen.core.support.RamlHelper;
import org.raml.model.MimeType;
import org.raml.model.Resource;
import org.raml.model.Response;
import org.raml.model.parameter.AbstractParam;
import org.raml.model.parameter.Header;

import com.sun.codemodel.JBlock;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JDocComment;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JInvocation;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JPackage;
import com.sun.codemodel.JType;
import com.sun.codemodel.JVar;


public class CodeModelVisitor extends TemplateResourceVisitor {

  /**
   * Locals for avoiding duplicates.
   */
  private final Set<String> schemaEntities = new HashSet<String>();
  private final Map<String, Set<String>> resourcesMethods = new HashMap<String, Set<String>>();
  private final Map<String, JClass> schemaClasses = new HashMap<String, JClass>();

  /**
   * Constants.
   */
  private static final String DEFAULT_ANNOTATION_PARAMETER = "value";
  private static final String LINK_NAME = "Link";

  /*
   * (non-Javadoc)
   * @see org.raml.jaxrs.codegen.core.ramlresource.RamlObjectVisitor#visit(org.raml.jaxrs.codegen.core.ramlresource.util.dataobjects.GlobalSchema)
   */
  @Override
  public void visit(GlobalSchema globalSchema) {
    getSchemaRepository().saveGlobal(globalSchema.getName(), globalSchema.getValue());
  }

  /*
   * (non-Javadoc)
   * @see org.raml.jaxrs.codegen.core.ramlresource.RamlObjectVisitor#visit(org.raml.jaxrs.codegen.core.ramlresource.util.dataobjects.ResourceEntity)
   */
  @Override
  public void visit(ResourceEntity resourceEntity) {
    MimeType mimeType = resourceEntity.getMimeType();

    String schemaNameOrContent = mimeType.getSchema();

    if (Types.isCompatibleWith(mimeType, APPLICATION_XML, TEXT_XML) ||
        isBlank(schemaNameOrContent) ) {

      // TODO support XML schema
      return;
    } else if(Types.isCompatibleWith(mimeType, APPLICATION_JSON)) {

      String buildSchemaKey = Types.buildSchemaKey(mimeType);
      Entry<File, String> schemaNameAndFile = getSchemaRepository().getGlobal(schemaNameOrContent);
      if (isBlank(schemaNameAndFile.getValue())) {
        schemaNameAndFile.setValue(buildNestedSchemaName(mimeType));
      }

      String packageName = getConfiguration().getModelPackage();
      String className = buildJavaFriendlyName(schemaNameAndFile.getValue());
      String classAndPackageName = packageName + "." + className;
      boolean isLink = className.equalsIgnoreCase(LINK_NAME) && getConfiguration().isHateoasResourceSupport();

      if(!schemaEntities.contains(buildSchemaKey)) {
        schemaEntities.add(buildSchemaKey);

        if(!isLink) {
          try {
            URL url = schemaNameAndFile.getKey().toURI().toURL();
            getMapper().generate(getCodeModel(), className, packageName, url).boxify();
          } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
          }
        }
      }

      JClass clazz = getCodeModel().ref(classAndPackageName);
      resourceEntity.setGeneratedObject(clazz);
      schemaClasses.put(buildSchemaKey, clazz);
    }
  }

  /*
   * (non-Javadoc)
   * @see org.raml.jaxrs.codegen.core.ramlresource.RamlObjectVisitor#visit(org.raml.jaxrs.codegen.core.ramlresource.util.dataobjects.ResourceInterface)
   */
  @Override
  public void visit(ResourceInterface resourceInterface) {
    String packageName = getConfiguration().getBasePackageName() + ".resource";

    Resource resource = resourceInterface.getResource();
    String name = Names.buildResourceInterfaceName(resource);
    String path = getResourceInterfacePath(resourceInterface);
    String javadoc = resource.getDescription();

    String postfix = "";
    if(getConfiguration().getResourcePostfix() != null) {
      postfix = getConfiguration().getResourcePostfix();
    }

    String actualName;
    int i = -1;
    while(true) {
      actualName = name + (++i == 0 ? "" : Integer.toString(i));
      if(!resourcesMethods.containsKey(actualName + postfix)){
        actualName += postfix;
        resourcesMethods.put(actualName, new HashSet<String>());
        break;
      }
    }

    try {
      JPackage pkg = getCodeModel()._package(packageName);
      JDefinedClass clazz = pkg._interface(actualName);
      clazz.annotate(Path.class).param(DEFAULT_ANNOTATION_PARAMETER, path);
      if (isNotBlank(javadoc)) {
        clazz.javadoc().add(javadoc);
      }
      resourceInterface.setGeneratedObject(clazz);


    } catch(JClassAlreadyExistsException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  /*
   * (non-Javadoc)
   * @see org.raml.jaxrs.codegen.core.ramlresource.RamlObjectVisitor#visit(org.raml.jaxrs.codegen.core.ramlresource.util.dataobjects.ResourceMethod)
   */
  @Override
  public void visit(ResourceMethod resourceMethod) {
    JType returnType;
    if(resourceMethod.getResponseClass() == null) {
      returnType = CodeModelHelper.getGeneratorType(getCodeModel(), void.class);
    } else {
      returnType = resourceMethod.getResponseClass().getGeneratedObject();
    }

    JDefinedClass resourceInterface = resourceMethod.getResourceInterface().getGeneratedObject();
    Set<String> existingMethodNames = resourcesMethods.get(resourceInterface.name());

    boolean addBodyMimeTypeInMethodName = resourceMethod.getAction().hasBody() && resourceMethod.getAction().getBody().size() > 1;
    String name = Names.buildResourceMethodName(resourceMethod.getAction(), addBodyMimeTypeInMethodName ? resourceMethod.getMimeType() : null);

    String actualMethodName;
    int i = -1;
    while (true) {
      actualMethodName = name + (++i == 0 ? "" : Integer.toString(i));
      if (!existingMethodNames.contains(actualMethodName)) {
        existingMethodNames.add(actualMethodName);
        break;
      }
    }

    JMethod method = resourceInterface.method(JMod.NONE, returnType, actualMethodName);
    JDocComment javadoc = method.javadoc();
    if (isNotBlank(resourceMethod.getAction().getDescription())) {
      javadoc.add(resourceMethod.getAction().getDescription());
    }

    resourceMethod.setGeneratedObject(method);
  }

  /*
   * (non-Javadoc)
   * @see org.raml.jaxrs.codegen.core.ramlresource.RamlObjectVisitor#visit(org.raml.jaxrs.codegen.core.ramlresource.util.dataobjects.ResourceMethodParameter)
   */
  @Override
  public void visit(ResourceMethodArgument parameter) {
    JMethod method = parameter.getResourceMethod().getGeneratedObject();
    JDocComment javadoc = method.javadoc();
    JType variableType = parameter.getArgumentType().getGeneratedObject();

    // Annotations
    switch (parameter.getArgumentType().getType()) {
      case JSON:
        MimeType mimeType = parameter.getMimeType();
        JType type = parameter.getArgumentType().getGeneratedObject();
        method.param(type, GENERIC_PAYLOAD_ARGUMENT_NAME);
        javadoc.addParam(GENERIC_PAYLOAD_ARGUMENT_NAME).add(RamlHelper.getPrefixedExampleOrBlank(mimeType.getExample()));
        break;
      case MULTIPART:
      case MULTIVALUE:
        // build a javadoc text out of all the params
        method.param(variableType, GENERIC_PAYLOAD_ARGUMENT_NAME);
        for(String description : RamlHelper.getAllFormParametersArgument(parameter.getMimeType())) {
          javadoc.addParam(GENERIC_PAYLOAD_ARGUMENT_NAME).add(description);
        }
        break;
      default:
        break;
    }

    // Clutter
    AbstractParam abstractParam = parameter.getParameter();
    if(abstractParam != null) {
      String name = Names.buildVariableName(parameter.getHeaderName());
      JVar variable = method.param(variableType, name);
      parameter.setGeneratedObject(variable);

      switch (parameter.getArgumentType().getType()) {
        case QUERY:
          variable.annotate(QueryParam.class).param(DEFAULT_ANNOTATION_PARAMETER, name);
          break;
        case URI:
          variable.annotate(PathParam.class).param(DEFAULT_ANNOTATION_PARAMETER, name);
          break;
        case HEADER:
          variable.annotate(HeaderParam.class).param(DEFAULT_ANNOTATION_PARAMETER, name);
          break;
        case FORM:
          variable.annotate(FormParam.class).param(DEFAULT_ANNOTATION_PARAMETER, name);
          break;
        default:
          break;
      }
      if (abstractParam.getDefaultValue() != null) {
        variable.annotate(DefaultValue.class).param(DEFAULT_ANNOTATION_PARAMETER, abstractParam.getDefaultValue());
      }
      javadoc.addParam(variable.name()).add(RamlHelper.getJavadoc(abstractParam));
    }
  }

  /*
   * (non-Javadoc)
   * @see org.raml.jaxrs.codegen.core.ramlresource.RamlObjectVisitor#visit(org.raml.jaxrs.codegen.core.ramlresource.util.dataobjects.ResponseClass)
   */
  @Override
  public void visit(ResponseClass responseClass) {
    try {
      JDefinedClass resourceInterface = responseClass.getResourceMethod().getResourceInterface().getGeneratedObject();
      boolean addBodyMimeTypeInMethodName = responseClass.getResourceMethod().getAction().hasBody() && responseClass.getResourceMethod().getAction().getBody().size() > 1;
      String name = capitalize(
        Names.buildResourceMethodName(
          responseClass.getResourceMethod().getAction(), addBodyMimeTypeInMethodName ?
              responseClass.getResourceMethod().getMimeType() : null));

      JClass directClass = getCodeModel().directClass(getConfiguration().getSupportPackage() + ".ResponseWrapper");
      JDefinedClass jDefinedResponseClass;
      if(getConfiguration().isResponseWrapperAsInnerClass()) {
        jDefinedResponseClass = resourceInterface._class(name + "Response");
      } else {
        String resourcePostfix = getConfiguration().getResourcePostfix();
        String supportSubPackage = resourceInterface.name();
        if(resourcePostfix != null) {
          supportSubPackage = supportSubPackage.replace(resourcePostfix, "");
        }

        String supportPackageName = getSupportPackage(resourceInterface.name()) + "." + "response";
        JPackage jPackage = getCodeModel()._package(supportPackageName);
        jDefinedResponseClass = jPackage._class(name + "Response");
      }
      jDefinedResponseClass._extends(directClass);

      JMethod responseClassConstructor = jDefinedResponseClass.constructor(JMod.PRIVATE);
      responseClassConstructor.param(javax.ws.rs.core.Response.class, "delegate");
      responseClassConstructor.body().invoke("super").arg(JExpr.ref("delegate"));

      responseClass.setGeneratedObject(jDefinedResponseClass);
      responseClass.getResourceMethod().setResponseClass(responseClass);
    } catch(JClassAlreadyExistsException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  /*
   * (non-Javadoc)
   * @see org.raml.jaxrs.codegen.core.ramlresource.RamlObjectVisitor#visit(org.raml.jaxrs.codegen.core.ramlresource.util.dataobjects.ResponseClassMethod)
   */
  @Override
  public void visit(ResponseClassMethod responseClassMethod) {
    String name = StringUtils.uncapitalize(
      Names.buildVariableName(
        Names.buildResponseMethodName(
          responseClassMethod.getStatusCode(),
          responseClassMethod.getMimeType()))
        );

    JDefinedClass responseClass = responseClassMethod.getResponseClass().getGeneratedObject();
    JMethod responseBuilderMethod = responseClass.method(PUBLIC + STATIC, responseClass, name);

    Response response = responseClassMethod.getResponse();
    String description = null;
    if (isNotBlank(response.getDescription())) {
      description = response.getDescription();
    }

    JDocComment javadoc = responseBuilderMethod.javadoc();
    if (isNotBlank(description)) {
      javadoc.add(description);
    }

    MimeType mimeType = responseClassMethod.getMimeType();
    String example = null;
    if (mimeType != null && isNotBlank(mimeType.getExample())) {
      example = EXAMPLE_PREFIX + mimeType.getExample();
    }
    if (isNotBlank(example)) {
      javadoc.add(example);
    }

    JInvocation builderArgument = ((JClass) CodeModelHelper.
        getGeneratorType(getCodeModel(), javax.ws.rs.core.Response.class)).
        staticInvoke("status").arg(JExpr.lit(responseClassMethod.getStatusCode()));

    String contentType = null;
    if (mimeType != null) {
      contentType = mimeType.getType();
      builderArgument = builderArgument.
          invoke("header").
          arg(HttpHeaders.CONTENT_TYPE).
          arg(contentType);
    }
    responseClassMethod.setGeneratedObject(responseBuilderMethod);

    // Arguments
    for (ResponseClassMethodArgument argument : responseClassMethod.getArguments()) {
      String argumentName = Names.buildVariableName(argument.getHeaderName());
      builderArgument = builderArgument.
          invoke("header").
          arg(argument.getHeaderName()).
          arg(JExpr.ref(argumentName));
      javadoc.addParam(argumentName).add(argument.getParameter().getDescription());
      JType codegenType = argument.getArgumentType().getGeneratedObject();
      responseBuilderMethod.param(codegenType, argumentName);
    }

    final JBlock responseBuilderMethodBody = responseBuilderMethod.body();
    final JVar builderVariable = responseBuilderMethodBody.decl(
      CodeModelHelper.getGeneratorType(getCodeModel(), ResponseBuilder.class),
      "responseBuilder", builderArgument);

    StringBuilder freeFormHeadersDescription = new StringBuilder();
    for (Entry<String, Header> namedHeaderParameter : response.getHeaders().entrySet()) {
      String headerName = namedHeaderParameter.getKey();
      Header header = namedHeaderParameter.getValue();
      if (headerName.contains(RESPONSE_HEADER_WILDCARD_SYMBOL)) {
        RamlHelper.appendParameterJavadocDescription(header, freeFormHeadersDescription);
      }
    }
    if (freeFormHeadersDescription.length() > 0) {

      // generate a Map<String, List<Object>> argument for {?} headers
      JClass listOfObjectsClass = ((JClass) CodeModelHelper.getGeneratorType(getCodeModel(), List.class)).
          narrow(Object.class);
      JClass headersArgument = ((JClass) CodeModelHelper.getGeneratorType(getCodeModel(), Map.class)).
          narrow((JClass) CodeModelHelper.getGeneratorType(getCodeModel(), String.class), listOfObjectsClass);

      builderArgument = responseBuilderMethodBody.invoke("headers")
          .arg(JExpr.ref(MULTIPLE_RESPONSE_HEADERS_ARGUMENT_NAME))
          .arg(builderVariable);
      JVar param = responseBuilderMethod.param(headersArgument, MULTIPLE_RESPONSE_HEADERS_ARGUMENT_NAME);
      javadoc.addParam(param).add(freeFormHeadersDescription.toString());
    }

    if (contentType != null) {
      JType entity = null;

      if(!isBlank(mimeType.getSchema())) {
        entity = schemaClasses.get(Types.buildSchemaKey(mimeType));

        if(responseClassMethod.isRepeat()) {
          entity = ((JClass) CodeModelHelper.getGeneratorType(getCodeModel(), List.class)).narrow(entity);
        }
      }

      if(entity == null) {
        if (startsWith(contentType, "text/")) {
          entity = CodeModelHelper.getGeneratorType(getCodeModel(), String.class);
        } else {
          // fallback to a streaming output
          entity = CodeModelHelper.getGeneratorType(getCodeModel(), StreamingOutput.class);
        }
      }
      boolean isLink = entity.name().equalsIgnoreCase(LINK_NAME) && getConfiguration().isHateoasResourceSupport();
      if(!isLink) {
        responseBuilderMethodBody.invoke(builderVariable, GENERIC_PAYLOAD_ARGUMENT_NAME).arg(JExpr.ref(GENERIC_PAYLOAD_ARGUMENT_NAME));
        responseBuilderMethod.param(entity, GENERIC_PAYLOAD_ARGUMENT_NAME);
        javadoc.addParam(GENERIC_PAYLOAD_ARGUMENT_NAME).add(defaultString(example));
      }
    }

    if(getConfiguration().isHateoasResourceSupport()) {
      JClass jClassLink = getCodeModel().ref("javax.ws.rs.core.Link");
      JVar jVar = responseClassMethod.getGeneratedObject().varParam(jClassLink, "link");
      responseClassMethod.getGeneratedObject().body().invoke(JExpr.ref("responseBuilder"), "links").arg(jVar);
    }

    responseBuilderMethodBody._return(JExpr._new(responseClass).arg(builderVariable.invoke("build")));
  }

  /*
   * (non-Javadoc)
   * @see org.raml.jaxrs.codegen.core.ramlresource.RamlObjectVisitor#visit(org.raml.jaxrs.codegen.core.ramlresource.util.dataobjects.MethodArgument)
   */
  @Override
  public void visit(ArgumentType methodArgumentType) {
    try {
      // entity reference
      JType codegenType;
      if(methodArgumentType.getType() == ArgumentType.Type.JSON) {
        codegenType = schemaClasses.get(Types.buildSchemaKey(methodArgumentType.getArgument().getMimeType()));

        if(methodArgumentType.isRepeat()) {
          codegenType = ((JClass) CodeModelHelper.getGeneratorType(getCodeModel(), List.class)).narrow(codegenType);
        }

      } else if(methodArgumentType.getType() == ArgumentType.Type.MULTIVALUE) {
        codegenType = ((JClass) CodeModelHelper.getGeneratorType(getCodeModel(), MultivaluedMap.class)).narrow(String.class, String.class);
      } else if(methodArgumentType.getType() == ArgumentType.Type.MULTIPART) {
        codegenType = CodeModelHelper.getGeneratorType(getCodeModel(), MimeMultipart.class);
      } else if(methodArgumentType.getType() == ArgumentType.Type.UNKNOWN) {
        MimeType mimeType = methodArgumentType.getArgument().getMimeType();
        if (startsWith(mimeType.getType(), "text/")) {
          codegenType = CodeModelHelper.getGeneratorType(getCodeModel(), String.class);
        } else {
          // fallback to a generic reader
          codegenType = CodeModelHelper.getGeneratorType(getCodeModel(), Reader.class);
        }
      } else {
        AbstractParam param = methodArgumentType.getArgument().getParameter();
        if (param.getEnumeration() != null && !param.getEnumeration().isEmpty()) {

          String argumentName = Names.buildVariableName(methodArgumentType.getArgument().getHeaderName());
          argumentName = capitalize(argumentName);
          JDefinedClass resourceInterface = methodArgumentType.getArgument().getResourceMethod().getResourceInterface().getGeneratedObject();
          JDefinedClass enumType = resourceInterface._enum(argumentName);
          for (String value : param.getEnumeration()) {
            enumType.enumConstant(value.toUpperCase());
          }
          codegenType = enumType;
        } else if (param.isRepeat()) {
          codegenType = CodeModelHelper.getGeneratorType(getCodeModel(), Types.getJavaType(param));
          codegenType = ((JClass) CodeModelHelper.getGeneratorType(getCodeModel(), List.class)).narrow(codegenType);
        } else {
          codegenType = CodeModelHelper.getGeneratorType(getCodeModel(), Types.getJavaType(param));
        }
      }
      methodArgumentType.setGeneratedObject(codegenType);
    } catch(JClassAlreadyExistsException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  private String getResourceInterfacePath(ResourceInterface resourceInterface) {
    Resource resource = resourceInterface.getResource();
    String path = strip(resource.getRelativeUri(), "/");
    path = StringUtils.defaultIfBlank(path, "/");
    return path;
  }


  private String getSupportPackage(String resourceInterfaceName) {
    String supportSubPackage = RamlHelper.getResourceInterfaceName(resourceInterfaceName, getConfiguration().getResourcePostfix());
    return getConfiguration().getSupportPackage() + "." + supportSubPackage.toLowerCase();
  }
}