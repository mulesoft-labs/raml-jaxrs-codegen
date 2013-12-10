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

import java.io.File;
import java.io.Reader;
import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.List;
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
import javax.ws.rs.core.StreamingOutput;

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
import com.sun.codemodel.JAnnotationArrayMember;
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

    private static final String GENERIC_PAYLOAD_ARGUMENT_NAME = "entity";
    private static final String EXAMPLE_PREFIX = " e.g. ";

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
                        return String.format("%s %s", vr.getStartMark(), vr.getMessage());
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
    }

    private Set<String> run(final Raml raml, final Configuration configuration) throws Exception
    {
        validate(configuration);

        context = new Context(configuration);
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
        resourceInterface.annotate(Path.class).param("value", path);

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
            if (action.getBody().isEmpty())
            {
                addResourceMethod(resourceInterface, resourceInterfacePath, action, null);
            }
            else
            {
                for (final MimeType bodyMimeType : action.getBody().values())
                {
                    addResourceMethod(resourceInterface, resourceInterfacePath, action, bodyMimeType);
                }
            }
        }

        for (final Resource childResource : resource.getResources().values())
        {
            addResourceMethods(childResource, resourceInterface, resourceInterfacePath);
        }
    }

    private void addResourceMethod(final JDefinedClass resourceInterface,
                                   final String resourceInterfacePath,
                                   final Action action,
                                   final MimeType bodyMimeType) throws Exception
    {
        final String methodName = Names.buildResourceMethodName(action, bodyMimeType);

        final JType resourceMethodReturnType = getResourceMethodReturnType(methodName, action,
            resourceInterface);

        // the actually created unique method name should be needed in the previous method but no
        // way of doing this :(
        final JMethod method = context.createResourceMethod(resourceInterface, methodName,
            resourceMethodReturnType);

        context.addHttpMethodAnnotation(action.getType().toString(), method);

        addParamAnnotation(resourceInterfacePath, action, method);
        addConsumesAnnotation(bodyMimeType, method);
        addProducesAnnotation(action, method);

        final JDocComment javadoc = addBaseJavaDoc(action, method);

        // TODO add JSR-303 request/response @constraints if config.isUseJsr303Annotations

        addPathParameters(action, method, javadoc);
        addHeaderParameters(action, method, javadoc);
        addQueryParameters(action, method, javadoc);
        addBodyParameters(bodyMimeType, method, javadoc);
    }

    private JType getResourceMethodReturnType(final String methodName,
                                              final Action action,
                                              final JDefinedClass resourceInterface) throws Exception
    {
        if (action.getResponses().isEmpty())
        {
            return types.getGeneratorType(void.class);
        }

        return createResourceMethodReturnType(methodName, action, resourceInterface);
    }

    private JDefinedClass createResourceMethodReturnType(final String methodName,
                                                         final Action action,
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
            createResponseBuilderInResourceMethodReturnType(action, responseClass, statusCodeAndResponse);
        }

        return responseClass;
    }

    private void createResponseBuilderInResourceMethodReturnType(final Action action,
                                                                 final JDefinedClass responseClass,
                                                                 final Entry<String, Response> statusCodeAndResponse)
        throws Exception
    {
        final int statusCode = NumberUtils.toInt(statusCodeAndResponse.getKey());
        final Response response = statusCodeAndResponse.getValue();

        for (final MimeType responseMimeType : response.getBody().values())
        {
            final String responseBuilderMethodName = Names.buildResponseMethodName(statusCode,
                responseMimeType);

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

            for (final Entry<String, Header> namedHeaderParameter : action.getHeaders().entrySet())
            {
                final String argumentName = Names.buildVariableName(namedHeaderParameter.getKey());

                builderArgument = builderArgument.invoke("header")
                    .arg(namedHeaderParameter.getKey())
                    .arg(JExpr.ref(argumentName));

                addParameterJavaDoc(namedHeaderParameter.getValue(), argumentName, javadoc);

                responseBuilderMethod.param(
                    types.buildParameterType(namedHeaderParameter.getValue(), argumentName), argumentName);
            }

            // TODO generate DTOs from XML/JSON schema and use them instead of generic
            // StreamingOutput
            builderArgument = builderArgument.invoke("entity").arg(JExpr.ref(GENERIC_PAYLOAD_ARGUMENT_NAME));
            responseBuilderMethod.param(StreamingOutput.class, GENERIC_PAYLOAD_ARGUMENT_NAME);

            builderArgument = builderArgument.invoke("build");

            responseBuilderMethod.body()._return(JExpr._new(responseClass).arg(builderArgument));
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
        method.annotate(Path.class).param("value",
            StringUtils.substringAfter(action.getResource().getUri(), resourceInterfacePath + "/"));
    }

    private void addConsumesAnnotation(final MimeType bodyMimeType, final JMethod method)
    {
        if (bodyMimeType != null)
        {
            method.annotate(Consumes.class).param("value", bodyMimeType.getType());
        }
    }

    private void addProducesAnnotation(final Action action, final JMethod method)
    {
        final Set<String> responseMimeTypes = new HashSet<String>();
        for (final Response response : action.getResponses().values())
        {
            for (final MimeType responseMimeType : response.getBody().values())
            {
                if (responseMimeType != null)
                {
                    responseMimeTypes.add(responseMimeType.getType());
                }
            }
        }
        if (!responseMimeTypes.isEmpty())
        {
            final JAnnotationArrayMember paramArray = method.annotate(Produces.class).paramArray("value");
            for (final String responseMimeType : responseMimeTypes)
            {
                paramArray.param(responseMimeType);
            }
        }
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
                sb.append(formParameter.getDescription());
                if (isNotBlank(formParameter.getExample()))
                {
                    sb.append(EXAMPLE_PREFIX).append(formParameter.getExample());
                }

                sb.append("<br/>\n");
            }

            javadoc.addParam(GENERIC_PAYLOAD_ARGUMENT_NAME).add(sb.toString());
        }
    }

    private void addPlainBodyArgument(final MimeType bodyMimeType,
                                      final JMethod method,
                                      final JDocComment javadoc)
    {
        // TODO generate DTOs from XML/JSON schema and use them instead of generic Reader
        method.param(types.getGeneratorType(Reader.class), GENERIC_PAYLOAD_ARGUMENT_NAME);

        final String example = isNotBlank(bodyMimeType.getExample()) ? EXAMPLE_PREFIX
                                                                       + bodyMimeType.getExample() : "";

        javadoc.addParam(GENERIC_PAYLOAD_ARGUMENT_NAME).add(example);
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
        final String example = isNotBlank(parameter.getExample())
                                                                 ? EXAMPLE_PREFIX + parameter.getExample()
                                                                 : "";

        javadoc.addParam(parameterName).add(defaultString(parameter.getDescription()) + example);
    }
}
