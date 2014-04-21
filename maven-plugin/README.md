![](http://raml.org/images/logo.png)

# RAML to JAX-RS codegen - Maven Plug-in

This plug-in generates JAX-RS annotated interfaces and supporting classes based on one or multiple RAML files.

_NB. The following documentation will soon be superseded by the Maven-generated plug-in documentation._

## Usage

Add `<pluginGroup>org.raml.plugins</pluginGroup>` to the `pluginGroups` section of your Maven `settings.xml`.

In your `pom.xml`, add the following build plug-in:

    <plugin>
        <groupId>org.raml.plugins</groupId>
        <artifactId>raml-jaxrs-maven-plugin</artifactId>
        <version>1.0-SNAPSHOT</version>
        <configuration>
            <!-- Use sourcePaths if you want to provide a single RAML file or a list of RAML files -->
            <sourceDirectory>${basedir}/src/main/resources/raml</sourceDirectory>
            <!-- Optionally configure outputDirectory if you don't like the default value: ${project.build.directory}/generated-sources/raml-jaxrs -->
            <!-- Replace with your package name -->
            <basePackageName>com.acme.api</basePackageName>
            <!-- Valid values: 1.1 2.0 -->
            <jaxrsVersion>2.0</jaxrsVersion>
            <useJsr303Annotations>false</useJsr303Annotations>
            <!-- Valid values: jackson1 jackson2 gson none -->
            <jsonMapper>jackson2</jsonMapper>
            <removeOldOutput>true</removeOldOutput>
        </configuration>
        <executions>
            <execution>
                <goals>
                    <goal>generate</goal>
                </goals>
                <phase>generate-sources</phase>
            </execution>
        </executions>
    </plugin>

