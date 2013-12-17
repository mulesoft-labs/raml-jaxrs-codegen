# RAML JAX-RS Codegen

## Supported

- JAX-RS 1.1 and 2.0,
- JSR-303 annotations, except `@Pattern` because RAML uses ECMA 262/Perl 5 patterns and javax.validation uses Java ones,
and with `@Min`/`@Max` support limited to non decimal minimum/maximum constraints defined in RAML.
- Model object generation based on JSON schema, with Jackson 1, 2 or Gson annotations.


## TODO

- Generation of JAXB annotated class based on XML Schema


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


