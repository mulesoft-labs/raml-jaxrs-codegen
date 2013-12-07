
package org.raml.jaxrs.codegen.core;

import java.io.InputStreamReader;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class GeneratorTestCase
{
    @Rule
    public TemporaryFolder outputFolder = new TemporaryFolder();

    @Test
    public void run() throws Exception
    {
        final Configuration configuration = new Configuration();
        configuration.setOutputDirectory(outputFolder.getRoot());

        configuration.setBasePackageName("org.raml.jaxrs.test");
        new Generator().run(
            new InputStreamReader(getClass().getResourceAsStream("/org/raml/full-config-with-patch.yaml")),
            configuration);

        configuration.setBasePackageName("org.raml.jaxrs.test.params");
        new Generator().run(
            new InputStreamReader(getClass().getResourceAsStream(
                "/org/raml/params/param-types-with-repeat.yaml")), configuration);

        configuration.setBasePackageName("org.raml.jaxrs.test.integration");
        new Generator().run(
            new InputStreamReader(getClass().getResourceAsStream(
                "/org/raml/integration/sales-enablement-api.yaml")), configuration);

        configuration.setBasePackageName("org.raml.jaxrs.test.rules");
        new Generator().run(
            new InputStreamReader(getClass().getResourceAsStream("/org/raml/rules/resource-full-ok.yaml")),
            configuration);
        new Generator().run(
            new InputStreamReader(getClass().getResourceAsStream(
                "/org/raml/rules/resource-with-description-ok.yaml")), configuration);
        new Generator().run(
            new InputStreamReader(getClass().getResourceAsStream("/org/raml/rules/resource-with-uri.yaml")),
            configuration);

        // TODO test compile the classes, add assertions
    }
}
