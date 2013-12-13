
package org.raml.jaxrs.example;

import java.io.Closeable;
import java.net.URI;
import java.util.Scanner;

import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.simple.SimpleContainerFactory;
import org.raml.jaxrs.example.impl.PresentationResource;

public class Main
{
    @SuppressWarnings("resource")
    public static void main(final String[] args) throws Exception
    {
        final ResourceConfig config = new ResourceConfig();
        config.register(PresentationResource.class);
        config.register(MultiPartFeature.class);

        final Closeable simpleContainer = SimpleContainerFactory.create(new URI("http://0.0.0.0:8181"),
            config);

        System.out.println("Strike ENTER to stop...");
        new Scanner(System.in).nextLine();

        simpleContainer.close();
        System.exit(0);

        System.out.println("Bye!");
    }
}
