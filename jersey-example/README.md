# RAML to JAX-RS codegen - Jersey Example

## Manual Testing

Run `org.raml.jaxrs.example.Main` for example with:

    mvn exec:java

Then:

    curl -H "Authorization: s3cr3t" http://localhost:8181/presentations?title=Some%20title

    curl -H "Authorization: s3cr3t" -H "Content-Type: application/json" -d '{"title":"New presentation"}' http://localhost:8181/presentations
