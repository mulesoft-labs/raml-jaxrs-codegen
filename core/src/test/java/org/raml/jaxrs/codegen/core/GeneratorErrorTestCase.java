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
package org.raml.jaxrs.codegen.core;

import static org.raml.jaxrs.codegen.core.Configuration.JaxrsVersion.JAXRS_1_1;
import static org.raml.jaxrs.codegen.core.Configuration.JaxrsVersion.JAXRS_2_0;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.raml.jaxrs.codegen.core.Configuration.JaxrsVersion;

public class GeneratorErrorTestCase
{
    private static final String RAML_ERRORS_DIR = "/org/raml/errors";
    private static final String TEST_BASE_PACKAGE = "org.raml.jaxrs.test";

    @Rule
    public TemporaryFolder codegenOutputFolder = new TemporaryFolder();

    private Class<?> expectedException;
    private String[] expectedSubstrings;

    @Test
    public void testInvalidRootElement() throws IOException {
        expect(InvalidRamlException.class, "line:3", "Unknown key: desc");
        generate("invalid-root-element.yaml");
    }
    
    private void expect(Class<?> exceptionClass, String... messageSubstrings) {
        this.expectedException = exceptionClass;
        this.expectedSubstrings = messageSubstrings;
    }

    private void generate(final String filename) throws IOException {
        generate(filename, JAXRS_1_1, false);
        generate(filename, JAXRS_1_1, true);
        generate(filename, JAXRS_2_0, false);
        generate(filename, JAXRS_2_0, true);
    }

    private void generate(final String filename, final JaxrsVersion jaxrsVersion, final boolean useJsr303Annotations) throws IOException {
        File outputFolder = codegenOutputFolder.newFolder();
        final Configuration configuration = new Configuration();
        configuration.setJaxrsVersion(jaxrsVersion);
        configuration.setUseJsr303Annotations(useJsr303Annotations);
        configuration.setOutputDirectory(outputFolder);
        configuration.setBasePackageName(TEST_BASE_PACKAGE);
        String dirPath = getClass().getResource(RAML_ERRORS_DIR).getPath();
        configuration.setSourceDirectory( new File(dirPath) );
        String resourcePath = RAML_ERRORS_DIR + "/" + filename;
        String label = resourcePath + " " + jaxrsVersion + " " + useJsr303Annotations;
        try {
            new Generator().run(
                new InputStreamReader(getClass().getResourceAsStream(resourcePath)),
                configuration);
            assertException(label);
        } catch (Exception e) {
            if (!expected(label, e)) {
                throw new RuntimeException("Failed to generate resource: " + label, e);
            }
        }
    }



    private boolean expected(String label, Exception e) {
        if (expectedException != null && expectedException.isInstance(e)) {
            String message = e.getMessage();
            for (String substring : expectedSubstrings) {
                assertTrue("Missing substring '" + substring 
                        + "' for \n\t" + e + ". \n\t" + label, message.contains(substring));
            }
            return true;
        }
        return false;
    }

    private void assertException(String label) {
        if (expectedException != null) {
            fail(label + ": Expected " + expectedException);
        }
    }

    @Before
    public void setupTests() {
        this.expectedException = null;
        this.expectedSubstrings = null;
    }

}
