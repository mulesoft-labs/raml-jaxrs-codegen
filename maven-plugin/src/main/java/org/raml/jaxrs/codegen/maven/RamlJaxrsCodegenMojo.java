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
package org.raml.jaxrs.codegen.maven;

import static org.apache.maven.plugins.annotations.ResolutionScope.COMPILE_PLUS_RUNTIME;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.jsonschema2pojo.AnnotationStyle;
import org.raml.jaxrs.codegen.core.Configuration;
import org.raml.jaxrs.codegen.core.Configuration.JaxrsVersion;
import org.raml.jaxrs.codegen.core.Generator;

/**
 * When invoked, this goals read one or more <a href="http://raml.org">RAML</a> files and produces
 * JAX-RS annotated Java classes.
 */
@Mojo(name = "generate", requiresProject = true, threadSafe = false, requiresDependencyResolution = COMPILE_PLUS_RUNTIME, defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class RamlJaxrsCodegenMojo extends AbstractMojo
{
    @Parameter(defaultValue = "${project}")
    private MavenProject project;

    /**
     * Skip plug-in execution.
     */
    @Parameter(property = "skip", defaultValue = "false")
    private boolean skip;

    /**
     * Target directory for generated Java source files.
     */
    @Parameter(property = "outputDirectory", defaultValue = "${project.build.directory}/generated-sources/raml-jaxrs")
    private File outputDirectory;

    /**
     * An array of locations of the RAML file(s).
     */
    @Parameter(property = "sourcePaths")
    private File[] sourcePaths;

    /**
     * Directory location of the RAML file(s).
     */
    @Parameter(property = "sourceDirectory")
    private File sourceDirectory;

    /**
     * The targeted JAX-RS version: either "1.1" or "2.0" .
     */
    @Parameter(property = "jaxrsVersion", defaultValue = "1.1")
    private String jaxrsVersion;

    /**
     * Base package name used for generated Java classes.
     */
    @Parameter(property = "basePackageName", required = true)
    private String basePackageName;

    /**
     * Model package name used for generated Java classes.
     */
    @Parameter(property = "modelPackageName", defaultValue = "model")
    private String modelPackageName;

    /**
     * Should JSR-303 annotations be used?
     */
    @Parameter(property = "useJsr303Annotations", defaultValue = "false")
    private boolean useJsr303Annotations;

    /**
     * Whether to empty the output directory before generation occurs, to clear out all source files
     * that have been generated previously.
     */
    @Parameter(property = "removeOldOutput", defaultValue = "false")
    private boolean removeOldOutput;

    /**
     * The JSON object mapper to generate annotations to: either "jackson1", "jackson2" or "gson" or
     * "none"
     */
    @Parameter(property = "jsonMapper", defaultValue = "jackson1")
    private String jsonMapper;

    /**
     * Should integers be represented by longs?
     */
    @Parameter(property = "useLongIntegers", defaultValue = "false")
    private boolean useLongIntegers;

    /**
     * Access-Control-Allow-Origin setting to be added into the header of the
     * response for each generated resource.
     */
    @Parameter(property = "accessControlAllowOrigin")
    private String accessControlAllowOrigin;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        if (skip)
        {
            getLog().info("Skipping execution...");
            return;
        }

        if ((sourceDirectory == null) && (sourcePaths == null))
        {
            throw new MojoExecutionException("One of sourceDirectory or sourcePaths must be provided");
        }

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
            configuration.setBasePackageName(basePackageName);
            configuration.setModelPackageName(modelPackageName);
            configuration.setJaxrsVersion(JaxrsVersion.fromAlias(jaxrsVersion));
            configuration.setOutputDirectory(outputDirectory);
            configuration.setUseJsr303Annotations(useJsr303Annotations);
            configuration.setJsonMapper(AnnotationStyle.valueOf(jsonMapper.toUpperCase()));
            configuration.setUseLongIntegers(useLongIntegers);
            configuration.setAccessControlAllowOrigin(accessControlAllowOrigin);
        }
        catch (final Exception e)
        {
            throw new MojoExecutionException("Failed to configure plug-in", e);
        }

        project.addCompileSourceRoot(outputDirectory.getPath());

        File currentSourcePath = null;

        try
        {
            final Generator generator = new Generator();

            for (final File ramlFile : getRamlFiles())
            {
                getLog().info("Generating Java classes from: " + ramlFile);
                currentSourcePath = ramlFile;
                generator.run(new FileReader(ramlFile), configuration);
            }
        }
        catch (final Exception e)
        {
            throw new MojoExecutionException("Error generating Java classes from: " + currentSourcePath, e);
        }
    }

    private Collection<File> getRamlFiles() throws MojoExecutionException
    {
        if (sourceDirectory != null)
        {
            if (!sourceDirectory.isDirectory())
            {
                throw new MojoExecutionException("The provided path doesn't refer to a valid directory: "
                                                 + sourceDirectory);
            }

            getLog().info("Looking for RAML files in and below: " + sourceDirectory);

            return FileUtils.listFiles(sourceDirectory, new String[]{"raml", "yaml"}, true);
        }
        else
        {
            final List<File> sourceFiles = Arrays.asList(sourcePaths);
            getLog().info("Using RAML files: " + sourceFiles);
            return sourceFiles;
        }
    }
}
