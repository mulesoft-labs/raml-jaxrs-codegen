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
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.apache.commons.lang.StringUtils.join;
import static org.apache.commons.lang.StringUtils.left;
import static org.apache.commons.lang.StringUtils.strip;
import static org.apache.commons.lang.StringUtils.uncapitalize;
import static org.apache.commons.lang.WordUtils.capitalizeFully;
import static org.apache.commons.lang.math.NumberUtils.isDigits;

import java.io.File;
import java.io.Reader;
import java.util.List;

import javax.ws.rs.Path;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.raml.model.Action;
import org.raml.model.Raml;
import org.raml.model.Resource;
import org.raml.parser.rule.ValidationResult;
import org.raml.parser.visitor.RamlDocumentBuilder;
import org.raml.parser.visitor.RamlValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JMethod;

public class Generator
{
    private static final Logger LOGGER = LoggerFactory.getLogger(Generator.class);

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
            final String methodName = buildResourceMethodName(action);

            // TODO use correct return type
            final JMethod method = context.createResourceMethod(resourceInterface, methodName, void.class);
            // TODO add query and path params

            context.addHttpMethodAnnotation(action.getType().toString(), method);

            method.annotate(Path.class).param("value",
                StringUtils.substringAfter(action.getResource().getUri(), resourceInterfacePath + "/"));

            // TODO add JSR-303 annotations for constraints

            if (isNotBlank(action.getDescription()))
            {
                method.javadoc().add(action.getDescription());
            }
        }

        for (final Resource childResource : resource.getResources().values())
        {
            addResourceMethods(childResource, resourceInterface, resourceInterfacePath);
        }
    }

    private static String buildResourceInterfaceName(final Resource resource)
    {
        final String baseInterfaceName = defaultIfBlank(resource.getDisplayName(), resource.getRelativeUri()
            .replaceAll("[\\W_]", " "));

        String resourceInterfaceName = capitalizeFully(baseInterfaceName).replaceAll("[\\W_]", "");
        if (isDigits(left(resourceInterfaceName, 1)))
        {
            resourceInterfaceName = "_" + resourceInterfaceName;
        }

        return isBlank(resourceInterfaceName) ? "Root" : resourceInterfaceName;
    }

    private static String buildResourceMethodName(final Action action)
    {
        final String baseMethodName = action.getType().toString()
                                      + " "
                                      + defaultIfBlank(action.getResource().getDisplayName(),
                                          action.getResource().getRelativeUri().replaceAll("[\\W_]", " "));
        final String methodName = capitalizeFully(baseMethodName).replaceAll("[\\W_]", "");
        return isDigits(left(methodName, 1)) ? "_" + methodName : uncapitalize(methodName);
    }
}
