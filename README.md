# RAML JAX-RS Codegen

## Using the core code generator

Example:

    File outputDirectory = new File("/some/path/to/target/code-gen");

    Configuration configuration = new Configuration();
    configuration.setOutputDirectory(outputDirectory);
    configuration.setBasePackageName("org.raml.jaxrs.test");

    InputStreamReader ramlReader = new InputStreamReader(getClass().getResourceAsStream("/org/raml/full-config-with-patch.yaml"));

    new Generator().run(ramlReader, configuration);

## Using the Maven plug-in

_Coming soon..._

## Using the Gradle plug-in

_Coming soon..._


