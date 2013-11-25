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

import java.io.File;
import java.io.InputStream;
import java.io.Reader;

import org.apache.commons.lang.Validate;
import org.raml.model.Raml;
import org.raml.parser.visitor.RamlDocumentBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Generator
{
    private static final Logger LOGGER = LoggerFactory.getLogger(Generator.class);

    public void run(final String ramlBuffer, final Configuration configuration)
    {
        run(new RamlDocumentBuilder().build(ramlBuffer), configuration);
    }

    public void run(final InputStream ramlBuffer, final Configuration configuration)
    {
        run(new RamlDocumentBuilder().build(ramlBuffer), configuration);
    }

    public void run(final Reader ramlBuffer, final Configuration configuration)
    {
        run(new RamlDocumentBuilder().build(ramlBuffer), configuration);
    }

    private void run(final Raml raml, final Configuration configuration)
    {
        validate(configuration);

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
    }
}
