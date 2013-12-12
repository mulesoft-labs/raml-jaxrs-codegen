
package org.raml.jaxrs.codegen.maven;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.raml.jaxrs.codegen.core.Configuration;
import org.raml.jaxrs.codegen.core.Configuration.JaxrsVersion;
import org.raml.jaxrs.codegen.core.Generator;

/**
 * When invoked, this goals read one or more <a href="http://raml.org">RAML</a> files and produces
 * JAX-RS annotated Java classes.
 */
public class RamlJaxrsCodegenMojo extends AbstractMojo
{
    /**
     * Target directory for generated Java source files.
     */
    @Parameter(property = "outputDirectory", defaultValue = "${project.build.directory}/generated-sources/raml-jaxrs")
    private File outputDirectory;

    /**
     * An array of locations of the RAML file(s). Note: each item may refer to a single file or a
     * directory of files.
     */
    @Parameter(property = "sourcePaths", required = true)
    private File[] sourcePaths;

    /**
     * The targetted JAX-RS version: either "1.1" or "2.0" .
     */
    @Parameter(property = "jaxrsVersion", defaultValue = "1.1")
    private String jaxrsVersion;

    /**
     * Base package name used for generated Java classes.
     */
    @Parameter(property = "jaxrsVersion", required = true)
    private String basePackageName;

    /**
     * Should JSR-303 annotations be used?
     */
    @Parameter(property = "jaxrsVersion", defaultValue = "false")
    private boolean useJsr303Annotations;

    /**
     * Whether to empty the output directory before generation occurs, to clear out all source files
     * that have been generated previously.
     */
    @Parameter(property = "removeOldOutput", defaultValue = "false")
    private boolean removeOldOutput;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        try
        {
            FileUtils.forceMkdir(outputDirectory);
        }
        catch (final IOException ioe)
        {
            throw new MojoExecutionException("Failed to create directory: " + outputDirectory, ioe);
        }

        if (removeOldOutput)
        {
            try
            {
                FileUtils.cleanDirectory(outputDirectory);
            }
            catch (final IOException ioe)
            {
                throw new MojoExecutionException("Failed to clean directory: " + outputDirectory, ioe);
            }
        }

        final Configuration configuration = new Configuration();

        try
        {
            configuration.setBasePackageName("org.raml.jaxrs.test");
            configuration.setJaxrsVersion(JaxrsVersion.fromAlias(jaxrsVersion));
            configuration.setOutputDirectory(outputDirectory);
            configuration.setUseJsr303Annotations(useJsr303Annotations);
        }
        catch (final Exception e)
        {
            throw new MojoExecutionException("Failed to configure plug-in", e);
        }

        File currentSourcePath = null;

        try
        {
            final Generator generator = new Generator();

            for (final File sourcePath : sourcePaths)
            {
                currentSourcePath = sourcePath;
                generator.run(new FileReader(sourcePath), configuration);
            }
        }
        catch (final Exception e)
        {
            throw new MojoExecutionException("Error generating Java classes from: " + currentSourcePath, e);
        }
    }
}
