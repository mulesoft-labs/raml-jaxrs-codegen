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

import static org.apache.commons.lang.StringUtils.defaultIfBlank;
import static org.apache.commons.lang.StringUtils.defaultString;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.apache.commons.lang.StringUtils.join;
import static org.apache.commons.lang.StringUtils.left;
import static org.apache.commons.lang.StringUtils.strip;
import static org.apache.commons.lang.StringUtils.substringAfter;
import static org.apache.commons.lang.StringUtils.uncapitalize;
import static org.apache.commons.lang.WordUtils.capitalize;
import static org.apache.commons.lang.math.NumberUtils.isDigits;

import java.io.File;
import java.io.Reader;
import java.lang.annotation.Annotation;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;

import javax.mail.internet.MimeMultipart;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.raml.model.Action;
import org.raml.model.MimeType;
import org.raml.model.Raml;
import org.raml.model.Resource;
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
import com.sun.codemodel.JClass;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JDocComment;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JType;
import com.sun.codemodel.JVar;

public class Generator
{
    private static final Logger LOGGER = LoggerFactory.getLogger(Generator.class);

    private static final String GENERIC_PAYLOAD_ARGUMENT_NAME = "payload";
    private static final String EXAMPLE_PREFIX = " e.g. ";

    private Context context;

    public void run(final Reader ramlReader, final Configuration configuration) throws Exception
    {
        final String ramlBuffer = IOUtils.toString(ramlReader);

        final List<ValidationResult> results = RamlValidationService.createDefault().validate(ramlBuffer);

        if (ValidationResult.areValid(results))
        {
            run(new RamlDocumentBuilder().build(ramlBuffer), configuration);
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

    private void run(final Raml raml, final Configuration configuration) throws Exception
    {
        validate(configuration);

        context = new Context(configuration);

        for (final Resource resource : raml.getResources().values())
        {
            createResourceInterface(resource);
        }

        context.generate();
    }

    private void createResourceInterface(final Resource resource) throws Exception
    {
        final String resourceInterfaceName = buildResourceInterfaceName(resource);
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
        final String methodBaseName = buildResourceMethodBaseName(action);
        final String bodyTypeInfix = bodyMimeType != null ? buildJavaFriendlyName(substringAfter(
            substringAfter(bodyMimeType.getType(), "/"), "x-www-")) : "";
        final String methodName = action.getType().toString().toLowerCase() + bodyTypeInfix + methodBaseName;

        // TODO use correct return type
        final JMethod method = context.createResourceMethod(resourceInterface, methodName, void.class);

        context.addHttpMethodAnnotation(action.getType().toString(), method);

        method.annotate(Path.class).param("value",
            StringUtils.substringAfter(action.getResource().getUri(), resourceInterfacePath + "/"));

        // TODO add produce annotation
        if (bodyMimeType != null)
        {
            method.annotate(Consumes.class).param("value", bodyMimeType.getType());
        }

        final JDocComment javadoc = method.javadoc();
        if (isNotBlank(action.getDescription()))
        {
            javadoc.add(action.getDescription());
        }

        // TODO add JSR-303 annotations for constraints if config.isUseJsr303Annotations
        addPathParameters(action, method, javadoc);
        addHeaderParameters(action, method, javadoc);
        addQueryParameters(action, method, javadoc);

        if (bodyMimeType != null)
        {
            addBodyParameters(bodyMimeType, method, javadoc);
        }
    }

    private void addBodyParameters(final MimeType bodyMimeType,
                                   final JMethod method,
                                   final JDocComment javadoc) throws Exception
    {
        if (MediaType.APPLICATION_FORM_URLENCODED.equals(bodyMimeType.getType()))
        {
            addFormParameters(bodyMimeType, method, javadoc);
        }
        else if (MediaType.MULTIPART_FORM_DATA.equals(bodyMimeType.getType()))
        {
            // use a "catch all" javax.mail.internet.MimeMultipart parameter
            addCatchAllFormParametersArgument(bodyMimeType, method, javadoc,
                context.getGeneratorType(MimeMultipart.class));
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
            final JClass type = ((JClass) context.getGeneratorType(MultivaluedMap.class)).narrow(
                String.class, String.class);

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
        method.param(context.getGeneratorType(Reader.class), GENERIC_PAYLOAD_ARGUMENT_NAME);

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
        final String argumentName = buildVariableName(name);

        final JVar codegenParam = method.param(getType(parameter, argumentName), argumentName);

        codegenParam.annotate(annotationClass).param("value", name);

        if (parameter.getDefaultValue() != null)
        {
            codegenParam.annotate(DefaultValue.class).param("value", parameter.getDefaultValue());
        }

        final String example = isNotBlank(parameter.getExample())
                                                                 ? EXAMPLE_PREFIX + parameter.getExample()
                                                                 : "";

        javadoc.addParam(codegenParam).add(defaultString(parameter.getDescription()) + example);
    }

    private JType getType(final AbstractParam parameter, final String name) throws Exception
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

    private static String buildResourceInterfaceName(final Resource resource)
    {
        final String resourceInterfaceName = buildJavaFriendlyName(defaultIfBlank(resource.getDisplayName(),
            resource.getRelativeUri()));
        return isBlank(resourceInterfaceName) ? "Root" : resourceInterfaceName;
    }

    private static String buildResourceMethodBaseName(final Action action)
    {
        return buildJavaFriendlyName(action.getResource().getUri().replace("{", " By "));
    }

    private static String buildVariableName(final String source)
    {
        final String name = uncapitalize(buildJavaFriendlyName(source));

        return Constants.JAVA_KEYWORDS.contains(name) ? "$" + name : name;
    }

    private static String buildJavaFriendlyName(final String source)
    {
        final String baseName = source.replaceAll("[\\W_]", " ");

        String friendlyName = capitalize(baseName).replaceAll("[\\W_]", "");

        if (isDigits(left(friendlyName, 1)))
        {
            friendlyName = "_" + friendlyName;
        }

        return friendlyName;
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
