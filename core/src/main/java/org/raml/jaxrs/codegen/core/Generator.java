/*
 * Copyright (c) MuleSoft, Inc.
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

import static com.sun.codemodel.JMod.PUBLIC;
import static com.sun.codemodel.JMod.STATIC;
import static org.apache.commons.lang.StringUtils.capitalize;
import static org.apache.commons.lang.StringUtils.defaultString;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.apache.commons.lang.StringUtils.join;
import static org.apache.commons.lang.StringUtils.strip;
import static org.raml.jaxrs.codegen.core.Constants.RESPONSE_HEADER_WILDCARD_SYMBOL;
import static org.raml.jaxrs.codegen.core.Names.EXAMPLE_PREFIX;
import static org.raml.jaxrs.codegen.core.Names.GENERIC_PAYLOAD_ARGUMENT_NAME;
import static org.raml.jaxrs.codegen.core.Names.MULTIPLE_RESPONSE_HEADERS_ARGUMENT_NAME;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.mail.internet.MimeMultipart;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang.math.NumberUtils;
import org.raml.model.Action;
import org.raml.model.MimeType;
import org.raml.model.Raml;
import org.raml.model.Resource;
import org.raml.model.Response;
import org.raml.model.parameter.AbstractParam;
import org.raml.model.parameter.FormParameter;
import org.raml.model.parameter.Header;
import org.raml.model.parameter.QueryParameter;
import org.raml.model.parameter.UriParameter;
import org.raml.parser.rule.ValidationResult;
import org.raml.parser.visitor.RamlDocumentBuilder;
import org.raml.parser.visitor.RamlValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.sun.codemodel.JBlock;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JDocComment;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JInvocation;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JType;
import com.sun.codemodel.JVar;

public class Generator
{
    private static final Logger LOGGER = LoggerFactory.getLogger(Generator.class);

    private Context context;
    private Types types;

    public Set<String> run(final Reader ramlReader, final Configuration configuration) throws Exception
    {
        final String ramlBuffer = IOUtils.toString(ramlReader);

        final List<ValidationResult> results = RamlValidationService.createDefault().validate(ramlBuffer);

        if (ValidationResult.areValid(results))
        {
            return run(new RamlDocumentBuilder().build(ramlBuffer), configuration);
        }
        else
        {
            final List<String> validationErrors = Lists.transform(results,
                new Function<ValidationResult, String>()
                {
                    @Override
                    public String apply(final ValidationResult vr)
                    {
                        return String.format("%s %s", vr.getStartColumn(), vr.getMessage());
                    }
                });

            throw new IllegalArgumentException("Invalid RAML definition:\n" + join(validationErrors, "\n"));
        }
    }

    private void validate(final Configuration configuration)
    {
        Validate.notNull(configuration, "configuration can't be null");

        final File outputDirectory = configuration.getOutputDirectory();
        Validate.notNull(outputDirectory, "outputDirectory can't be null");

        Validate.isTrue(outputDirectory.isDirectory(), outputDirectory + " is not a pre-existing directory");
        Validate.isTrue(outputDirectory.canWrite(), outputDirectory + " can't be written to");

        if (outputDirectory.listFiles().length > 0)
        {
            LOGGER.warn("Directory "
                        + outputDirectory
                        + " is not empty, generation will work but pre-existing files may remain and produce unexpected results");
        }

        Validate.notEmpty(configuration.getBasePackageName(), "base package name can't be empty");

        // TODO remove when JSR-303 annotations are supported
        if (configuration.isUseJsr303Annotations())
        {
            LOGGER.warn("JSR-303 annotation support is currently not available");
        }
    }

    private Set<String> run(final Raml raml, final Configuration configuration) throws Exception
    {
        validate(configuration);

        context = new Context(configuration, raml);
        types = new Types(context);

        for (final Resource resource : raml.getResources().values())
        {
            createResourceInterface(resource);
        }

        return context.generate();
    }

    private void createResourceInterface(final Resource resource) throws Exception
    {
        final String resourceInterfaceName = Names.buildResourceInterfaceName(resource);
        final JDefinedClass resourceInterface = context.createResourceInterface(resourceInterfaceName);
        context.setCurrentResourceInterface(resourceInterface);

        final String path = strip(resource.getRelativeUri(), "/");
        resourceInterface.annotate(Path.class).param("value", StringUtils.defaultIfBlank(path, "/"));

        if (isNotBlank(resource.getDescription()))
        {
            resourceInterface.javadoc().add(resource.getDescription());
        }

        addResourceMethods(resource, resourceInterface, path);
    }

    private void addResourceMethods(final Resource resource,
                                    final JDefinedClass resourceInterface,
                                    final String resourceInterfacePath) throws Exception
    {
        for (final Action action : resource.getActions().values())
        {
            if (action.getBody().size() <= 1)
            {
                final MimeType bodyMimeType = action.getBody().isEmpty() ? null : action.getBody()
                    .values()
                    .iterator()
                    .next();

                addResourceMethods(resourceInterface, resourceInterfacePath, action, bodyMimeType, false);
            }
            else
            {
                for (final MimeType bodyMimeType : action.getBody().values())
                {
                    addResourceMethods(resourceInterface, resourceInterfacePath, action, bodyMimeType, true);
                }
            }
        }

        for (final Resource childResource : resource.getResources().values())
        {
            addResourceMethods(childResource, resourceInterface, resourceInterfacePath);
        }
    }

    private void addResourceMethods(final JDefinedClass resourceInterface,
                                    final String resourceInterfacePath,
                                    final Action action,
                                    final MimeType bodyMimeType,
                                    final boolean addBodyMimeTypeInMethodName) throws Exception
    {
        final Collection<MimeType> uniqueResponseMimeTypes = getUniqueResponseMimeTypes(action);

        // one or zero response mime types, we don't need to add them in the method name
        if (uniqueResponseMimeTypes.size() <= 1)
        {
            final MimeType responseMimeTypeOrNull = uniqueResponseMimeTypes.isEmpty()
                                                                                     ? null
                                                                                     : uniqueResponseMimeTypes.iterator()
                                                                                         .next();

            addResourceMethod(resourceInterface, resourceInterfacePath, action, bodyMimeType,
                addBodyMimeTypeInMethodName, responseMimeTypeOrNull, false);
        }
        else
        {
            for (final MimeType responseMimeType : uniqueResponseMimeTypes)
            {
                addResourceMethod(resourceInterface, resourceInterfacePath, action, bodyMimeType,
                    addBodyMimeTypeInMethodName, responseMimeType, true);
            }
        }
    }

    private void addResourceMethod(final JDefinedClass resourceInterface,
                                   final String resourceInterfacePath,
                                   final Action action,
                                   final MimeType bodyMimeType,
                                   final boolean addBodyMimeTypeInMethodName,
                                   final MimeType responseMimeType,
                                   final boolean addResponseMimeTypeInMethodName) throws Exception
    {
        final String methodName = Names.buildResourceMethodName(action,
            addBodyMimeTypeInMethodName ? bodyMimeType : null,
            addResponseMimeTypeInMethodName ? responseMimeType : null);

        final JType resourceMethodReturnType = getResourceMethodReturnType(methodName, action,
            responseMimeType, resourceInterface);

        // the actually created unique method name should be needed in the previous method but
        // no way of doing this :(
        final JMethod method = context.createResourceMethod(resourceInterface, methodName,
            resourceMethodReturnType);

        context.addHttpMethodAnnotation(action.getType().toString(), method);

        addParamAnnotation(resourceInterfacePath, action, method);
        addConsumesAnnotation(bodyMimeType, method);
        addProducesAnnotation(responseMimeType, method);

        final JDocComment javadoc = addBaseJavaDoc(action, method);

        // TODO add JSR-303 request/response @constraints if config.isUseJsr303Annotations

        addPathParameters(action, method, javadoc);
        addHeaderParameters(action, method, javadoc);
        addQueryParameters(action, method, javadoc);
        addBodyParameters(bodyMimeType, method, javadoc);
    }

    private JType getResourceMethodReturnType(final String methodName,
                                              final Action action,
                                              final MimeType responseMimeType,
                                              final JDefinedClass resourceInterface) throws Exception
    {
        if (responseMimeType == null)
        {
            return types.getGeneratorType(void.class);
        }
        else
        {
            return createResourceMethodReturnType(methodName, action, responseMimeType, resourceInterface);
        }
    }

    private JDefinedClass createResourceMethodReturnType(final String methodName,
                                                         final Action action,
                                                         final MimeType responseMimeType,
                                                         final JDefinedClass resourceInterface)
        throws Exception
    {
        final JDefinedClass responseClass = resourceInterface._class(capitalize(methodName) + "Response")
            ._extends(context.getResponseWrapperType());

        final JMethod responseClassConstructor = responseClass.constructor(JMod.PRIVATE);
        responseClassConstructor.param(javax.ws.rs.core.Response.class, "delegate");
        responseClassConstructor.body().invoke("super").arg(JExpr.ref("delegate"));

        for (final Entry<String, Response> statusCodeAndResponse : action.getResponses().entrySet())
        {
            createResponseBuilderInResourceMethodReturnType(action, responseMimeType, responseClass,
                statusCodeAndResponse);
        }

        return responseClass;
    }

    private void createResponseBuilderInResourceMethodReturnType(final Action action,
                                                                 final MimeType responseMimeType,
                                                                 final JDefinedClass responseClass,
                                                                 final Entry<String, Response> statusCodeAndResponse)
        throws Exception
    {
        final int statusCode = NumberUtils.toInt(statusCodeAndResponse.getKey());
        final Response response = statusCodeAndResponse.getValue();

        for (final MimeType mimeType : response.getBody().values())
        {
            if (!mimeType.getType().equals(responseMimeType.getType()))
            {
                continue;
            }

            final String responseBuilderMethodName = Names.buildResponseMethodName(statusCode);

            final JMethod responseBuilderMethod = responseClass.method(PUBLIC + STATIC, responseClass,
                responseBuilderMethodName);

            final JDocComment javadoc = responseBuilderMethod.javadoc();

            if (isNotBlank(response.getDescription()))
            {
                javadoc.add(response.getDescription());
            }
            if (isNotBlank(responseMimeType.getExample()))
            {
                javadoc.add(EXAMPLE_PREFIX + responseMimeType.getExample());
            }

            JInvocation builderArgument = types.getGeneratorClass(javax.ws.rs.core.Response.class)
                .staticInvoke("status")
                .arg(JExpr.lit(statusCode));

            builderArgument = builderArgument.invoke("header")
                .arg(HttpHeaders.CONTENT_TYPE)
                .arg(responseMimeType.getType());

            final StringBuilder freeFormHeadersDescription = new StringBuilder();

            for (final Entry<String, Header> namedHeaderParameter : response.getHeaders().entrySet())
            {
                final String headerName = namedHeaderParameter.getKey();
                final Header header = namedHeaderParameter.getValue();

                if (headerName.contains(RESPONSE_HEADER_WILDCARD_SYMBOL))
                {
                    appendParameterJavadocDescription(header, freeFormHeadersDescription);
                    continue;
                }

                final String argumentName = Names.buildVariableName(headerName);

                builderArgument = builderArgument.invoke("header")
                    .arg(headerName)
                    .arg(JExpr.ref(argumentName));

                addParameterJavaDoc(header, argumentName, javadoc);

                responseBuilderMethod.param(types.buildParameterType(header, argumentName), argumentName);
            }

            final JBlock responseBuilderMethodBody = responseBuilderMethod.body();

            final JVar builderVariable = responseBuilderMethodBody.decl(
                types.getGeneratorType(ResponseBuilder.class), "responseBuilder", builderArgument);

            if (freeFormHeadersDescription.length() > 0)
            {
                // generate a Map<String, List<Object>> argument
                final JClass listOfObjectsClass = types.getGeneratorClass(List.class).narrow(Object.class);
                final JClass headersArgument = types.getGeneratorClass(Map.class).narrow(
                    types.getGeneratorClass(String.class), listOfObjectsClass);

                builderArgument = responseBuilderMethodBody.invoke("headers")
                    .arg(JExpr.ref(MULTIPLE_RESPONSE_HEADERS_ARGUMENT_NAME))
                    .arg(builderVariable);

                final JVar param = responseBuilderMethod.param(headersArgument,
                    MULTIPLE_RESPONSE_HEADERS_ARGUMENT_NAME);

                javadoc.addParam(param).add(freeFormHeadersDescription.toString());
            }

            responseBuilderMethodBody.invoke(builderVariable, "entity").arg(
                JExpr.ref(GENERIC_PAYLOAD_ARGUMENT_NAME));
            responseBuilderMethod.param(types.getResponseEntityClass(responseMimeType),
                GENERIC_PAYLOAD_ARGUMENT_NAME);
            javadoc.addParam(GENERIC_PAYLOAD_ARGUMENT_NAME).add(defaultString(responseMimeType.getExample()));

            responseBuilderMethodBody._return(JExpr._new(responseClass).arg(builderVariable.invoke("build")));
        }
    }

    private JDocComment addBaseJavaDoc(final Action action, final JMethod method)
    {
        final JDocComment javadoc = method.javadoc();
        if (isNotBlank(action.getDescription()))
        {
            javadoc.add(action.getDescription());
        }
        return javadoc;
    }

    private void addParamAnnotation(final String resourceInterfacePath,
                                    final Action action,
                                    final JMethod method)
    {
        final String path = StringUtils.substringAfter(action.getResource().getUri(), resourceInterfacePath
                                                                                      + "/");
        if (isNotBlank(path))
        {
            method.annotate(Path.class).param("value", path);
        }
    }

    private void addConsumesAnnotation(final MimeType bodyMimeType, final JMethod method)
    {
        if (bodyMimeType != null)
        {
            method.annotate(Consumes.class).param("value", bodyMimeType.getType());
        }
    }

    private void addProducesAnnotation(final MimeType responseMimeType, final JMethod method)
    {
        if (responseMimeType != null)
        {
            method.annotate(Produces.class).param("value", responseMimeType.getType());
        }
    }

    private Collection<MimeType> getUniqueResponseMimeTypes(final Action action)
    {
        final Map<String, MimeType> responseMimeTypes = new HashMap<String, MimeType>();
        for (final Response response : action.getResponses().values())
        {
            for (final MimeType responseMimeType : response.getBody().values())
            {
                if (responseMimeType != null)
                {
                    responseMimeTypes.put(responseMimeType.getType(), responseMimeType);
                }
            }
        }
        return responseMimeTypes.values();
    }

    private void addBodyParameters(final MimeType bodyMimeType,
                                   final JMethod method,
                                   final JDocComment javadoc) throws Exception
    {
        if (bodyMimeType == null)
        {
            return;
        }
        else if (MediaType.APPLICATION_FORM_URLENCODED.equals(bodyMimeType.getType()))
        {
            addFormParameters(bodyMimeType, method, javadoc);
        }
        else if (MediaType.MULTIPART_FORM_DATA.equals(bodyMimeType.getType()))
        {
            // use a "catch all" javax.mail.internet.MimeMultipart parameter
            addCatchAllFormParametersArgument(bodyMimeType, method, javadoc,
                types.getGeneratorType(MimeMultipart.class));
        }
        else
        {
            addPlainBodyArgument(bodyMimeType, method, javadoc);
        }
    }

    private void addPathParameters(final Action action, final JMethod method, final JDocComment javadoc)
        throws Exception
    {
        for (final Entry<String, UriParameter> namedUriParameter : action.getResource()
            .getUriParameters()
            .entrySet())
        {
            addParameter(namedUriParameter.getKey(), namedUriParameter.getValue(), PathParam.class, method,
                javadoc);
        }
    }

    private void addHeaderParameters(final Action action, final JMethod method, final JDocComment javadoc)
        throws Exception
    {
        for (final Entry<String, Header> namedHeaderParameter : action.getHeaders().entrySet())
        {
            addParameter(namedHeaderParameter.getKey(), namedHeaderParameter.getValue(), HeaderParam.class,
                method, javadoc);
        }
    }

    private void addQueryParameters(final Action action, final JMethod method, final JDocComment javadoc)
        throws Exception
    {
        for (final Entry<String, QueryParameter> namedQueryParameter : action.getQueryParameters().entrySet())
        {
            addParameter(namedQueryParameter.getKey(), namedQueryParameter.getValue(), QueryParam.class,
                method, javadoc);
        }
    }

    private void addFormParameters(final MimeType bodyMimeType,
                                   final JMethod method,
                                   final JDocComment javadoc) throws Exception
    {
        if (hasAMultiTypeFormParameter(bodyMimeType))
        {
            // use a "catch all" MultivaluedMap<String, String> parameter
            final JClass type = types.getGeneratorClass(MultivaluedMap.class).narrow(String.class,
                String.class);

            addCatchAllFormParametersArgument(bodyMimeType, method, javadoc, type);
        }
        else
        {
            for (final Entry<String, List<FormParameter>> namedFormParameters : bodyMimeType.getFormParameters()
                .entrySet())
            {
                addParameter(namedFormParameters.getKey(), namedFormParameters.getValue().get(0),
                    FormParam.class, method, javadoc);
            }
        }
    }

    private void addCatchAllFormParametersArgument(final MimeType bodyMimeType,
                                                   final JMethod method,
                                                   final JDocComment javadoc,
                                                   final JType argumentType)
    {
        method.param(argumentType, GENERIC_PAYLOAD_ARGUMENT_NAME);

        // build a javadoc text out of all the params
        for (final Entry<String, List<FormParameter>> namedFormParameters : bodyMimeType.getFormParameters()
            .entrySet())
        {
            final StringBuilder sb = new StringBuilder();
            sb.append(namedFormParameters.getKey()).append(": ");

            for (final FormParameter formParameter : namedFormParameters.getValue())
            {
                appendParameterJavadocDescription(formParameter, sb);
            }

            javadoc.addParam(GENERIC_PAYLOAD_ARGUMENT_NAME).add(sb.toString());
        }
    }

    private void addPlainBodyArgument(final MimeType bodyMimeType,
                                      final JMethod method,
                                      final JDocComment javadoc) throws IOException
    {

        method.param(types.getRequestEntityClass(bodyMimeType), GENERIC_PAYLOAD_ARGUMENT_NAME);

        javadoc.addParam(GENERIC_PAYLOAD_ARGUMENT_NAME).add(
            getPrefixedExampleOrBlank(bodyMimeType.getExample()));
    }

    private boolean hasAMultiTypeFormParameter(final MimeType bodyMimeType)
    {
        for (final List<FormParameter> formParameters : bodyMimeType.getFormParameters().values())
        {
            if (formParameters.size() > 1)
            {
                return true;
            }
        }
        return false;
    }

    private void addParameter(final String name,
                              final AbstractParam parameter,
                              final Class<? extends Annotation> annotationClass,
                              final JMethod method,
                              final JDocComment javadoc) throws Exception
    {
        final String argumentName = Names.buildVariableName(name);

        final JVar codegenParam = method.param(types.buildParameterType(parameter, argumentName),
            argumentName);

        codegenParam.annotate(annotationClass).param("value", name);

        if (parameter.getDefaultValue() != null)
        {
            codegenParam.annotate(DefaultValue.class).param("value", parameter.getDefaultValue());
        }

        addParameterJavaDoc(parameter, codegenParam.name(), javadoc);
    }

    private void addParameterJavaDoc(final AbstractParam parameter,
                                     final String parameterName,
                                     final JDocComment javadoc)
    {
        javadoc.addParam(parameterName).add(
            defaultString(parameter.getDescription()) + getPrefixedExampleOrBlank(parameter.getExample()));
    }

    private String getPrefixedExampleOrBlank(final String example)
    {
        return isNotBlank(example) ? EXAMPLE_PREFIX + example : "";
    }

    private void appendParameterJavadocDescription(final AbstractParam param, final StringBuilder sb)
    {
        if (isNotBlank(param.getDisplayName()))
        {
            sb.append(param.getDisplayName());
        }

        if (isNotBlank(param.getDescription()))
        {
            if (sb.length() > 0)
            {
                sb.append(" - ");
            }
            sb.append(param.getDescription());
        }

        if (isNotBlank(param.getExample()))
        {
            sb.append(EXAMPLE_PREFIX).append(param.getExample());
        }

        sb.append("<br/>\n");
    }
}
