
package org.raml.jaxrs.example.impl;

import org.raml.jaxrs.example.model.Presentation;
import org.raml.jaxrs.example.resource.Presentations;

public class PresentationResource implements Presentations
{
    @Override
    public GetPresentationsResponse getPresentations(final String authorization,
                                                     final String title,
                                                     final Double start,
                                                     final Double pages)
    {
        if (!"s3cr3t".equals(authorization))
        {
            throw new SecurityException("not authorized");
        }

        final Presentation presentation = new Presentation().withId("fake-id").withTitle(title);

        return GetPresentationsResponse.oK(presentation);
    }

    @Override
    public PostPresentationsResponse postPresentations(final String authorization, final Presentation entity)
    {
        if (!"s3cr3t".equals(authorization))
        {
            throw new SecurityException("not authorized");
        }

        entity.setId("fake-new-id");

        return PostPresentationsResponse.created(entity);
    }

    @Override
    public GetPresentationsByPresentationIdResponse getPresentationsByPresentationId(final String authorization)
    {
        // TODO implement me!
        return null;
    }

    @Override
    public PatchPresentationsByPresentationIdResponse patchPresentationsByPresentationId(final String authorization,
                                                                                         final Presentation entity)
    {
        // TODO implement me!
        return null;
    }

    @Override
    public void deletePresentationsByPresentationId(final String authorization)
    {
        // TODO implement me!
    }

    @Override
    public PutPresentationsByPresentationIdResponse putPresentationsByPresentationId(final String authorization,
                                                                                     final Presentation entity)
    {
        // TODO implement me!
        return null;
    }
}
