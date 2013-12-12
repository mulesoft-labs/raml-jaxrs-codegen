
package org.raml.jaxrs.codegen.core;

import static org.apache.commons.lang.ArrayUtils.EMPTY_STRING_ARRAY;
import static org.hamcrest.Matchers.emptyArray;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.jci.compilers.CompilationResult;
import org.apache.commons.jci.compilers.JavaCompiler;
import org.apache.commons.jci.compilers.JavaCompilerFactory;
import org.apache.commons.jci.compilers.JavaCompilerSettings;
import org.apache.commons.jci.readers.FileResourceReader;
import org.apache.commons.jci.stores.FileResourceStore;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.sun.jersey.api.core.PackagesResourceConfig;
import com.sun.jersey.api.core.ResourceConfig;

public class GeneratorTestCase
{
    private static final String TEST_BASE_PACKAGE = "org.raml.jaxrs.test";

    @Rule
    public TemporaryFolder codegenOutputFolder = new TemporaryFolder();

    @Rule
    public TemporaryFolder compilationOutputFolder = new TemporaryFolder();

    @Test
    public void run() throws Exception
    {
        final Set<String> generatedSources = new HashSet<String>();

        final Configuration configuration = new Configuration();
        configuration.setOutputDirectory(codegenOutputFolder.getRoot());

        configuration.setBasePackageName(TEST_BASE_PACKAGE);
        generatedSources.addAll(new Generator().run(
            new InputStreamReader(getClass().getResourceAsStream("/org/raml/full-config-with-patch.yaml")),
            configuration));

        configuration.setBasePackageName(TEST_BASE_PACKAGE + ".params");
        generatedSources.addAll(new Generator().run(
            new InputStreamReader(getClass().getResourceAsStream(
                "/org/raml/params/param-types-with-repeat.yaml")), configuration));

        configuration.setBasePackageName(TEST_BASE_PACKAGE + ".integration");
        generatedSources.addAll(new Generator().run(
            new InputStreamReader(getClass().getResourceAsStream(
                "/org/raml/integration/sales-enablement-api.yaml")), configuration));

        configuration.setBasePackageName(TEST_BASE_PACKAGE + ".rules");
        generatedSources.addAll(new Generator().run(
            new InputStreamReader(getClass().getResourceAsStream("/org/raml/rules/resource-full-ok.yaml")),
            configuration));
        generatedSources.addAll(new Generator().run(
            new InputStreamReader(getClass().getResourceAsStream(
                "/org/raml/rules/resource-with-description-ok.yaml")), configuration));
        generatedSources.addAll(new Generator().run(
            new InputStreamReader(getClass().getResourceAsStream("/org/raml/rules/resource-with-uri.yaml")),
            configuration));

        // test compile the classes
        final JavaCompiler compiler = new JavaCompilerFactory().createCompiler("eclipse");

        final JavaCompilerSettings settings = compiler.createDefaultSettings();
        settings.setSourceVersion("1.5");
        settings.setTargetVersion("1.5");
        settings.setDebug(true);

        final String[] sources = generatedSources.toArray(EMPTY_STRING_ARRAY);
        System.out.println("Test compiling: " + Arrays.toString(sources));

        final FileResourceReader sourceReader = new FileResourceReader(codegenOutputFolder.getRoot());
        final FileResourceStore classWriter = new FileResourceStore(compilationOutputFolder.getRoot());
        final CompilationResult result = compiler.compile(sources, sourceReader, classWriter,
            Thread.currentThread().getContextClassLoader(), settings);

        assertThat(ToStringBuilder.reflectionToString(result.getErrors(), ToStringStyle.SHORT_PREFIX_STYLE),
            result.getErrors(), is(emptyArray()));

        assertThat(
            ToStringBuilder.reflectionToString(result.getWarnings(), ToStringStyle.SHORT_PREFIX_STYLE),
            result.getWarnings(), is(emptyArray()));

        // test load the classes with Jersey
        final URLClassLoader resourceClassLoader = new URLClassLoader(
            new URL[]{compilationOutputFolder.getRoot().toURI().toURL()});

        final ClassLoader initialClassLoader = Thread.currentThread().getContextClassLoader();
        try
        {
            Thread.currentThread().setContextClassLoader(resourceClassLoader);
            final ResourceConfig config = new PackagesResourceConfig(TEST_BASE_PACKAGE);

            assertThat(config.getRootResourceClasses(), hasSize(11));

            // TODO testing: actually send HTTP requests at the resources
        }
        finally
        {
            Thread.currentThread().setContextClassLoader(initialClassLoader);
        }
    }
}
