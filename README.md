![](http://raml.org/images/logo.png)

# RAML JAX-RS Codegen

## Objective

The goal of this project is to support a RAML-first approach for JAX-RS enabled API projects.

It consists in a set of tools that API developers can use to generate JAX-RS annotated interfaces
and supporting classes out of one or several RAML files.
It is then the responsibility of the developer to implement these interfaces in concrete classes that reify the API logic.

## Design principles

- Interfaces are generated and will be regenerated when the RAML definition changes,
- One interface is generated per top level resource, sub-resources are defined as different methods in the same interface.
- A response object wrapper is created for each resource action in order to guide the implementer in producing only results
that are compatible with the RAML definition,
- Custom annotations are generated for HTTP methods that are not part of the core JAX-RS specification,
- Objects are generated based on schemas to represent request/response entities,
- English is the language used in the interface and method names generation.

## Status

### Currently supported

- JAX-RS 1.1 and 2.0,
- JSR-303 annotations, except `@Pattern` because RAML uses ECMA 262/Perl 5 patterns and javax.validation uses Java ones,
and with `@Min`/`@Max` support limited to non decimal minimum/maximum constraints defined in RAML.
- Model object generation based on JSON schemas, with Jackson 1, 2 or Gson annotations.

### Not yet supported

- Generation of JAXB annotated classes based on XML Schemas

## Usage

- [Using the Maven Plug-in](maven-plugin/README.md)
- [Using the Core Generator](core/README.md)
