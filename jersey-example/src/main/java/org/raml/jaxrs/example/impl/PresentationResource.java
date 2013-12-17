
package org.raml.jaxrs.example.impl;

import org.raml.jaxrs.example.model.Presentation;
import org.raml.jaxrs.example.resource.Presentations;

public class PresentationResource implements Presentations
{
    @Override
    public GetPresentationsResponse getPresentations(final String authorization,
                                                     final String title,
                                                     final Long start,
                                                     final Long pages)
    {
        if (!"s3cr3t".equals(authorization))
        {
            return GetPresentationsResponse.unauthorized();
        }

        final org.raml.jaxrs.example.model.Presentations presentations = new org.raml.jaxrs.example.model.Presentations().withSize(1);

        presentations.getPresentations().add(new Presentation().withId("fake-id").withTitle(title));

        return GetPresentationsResponse.jsonOK(presentations);
    }

    @Override
    public PostPresentationsResponse postPresentations(final String authorization, final Presentation entity)
    {
        if (!"s3cr3t".equals(authorization))
        {
            return PostPresentationsResponse.unauthorized();
        }

        entity.setId("fake-new-id");

        return PostPresentationsResponse.jsonCreated(entity);
    }

    @Override
    public GetPresentationsByPresentationIdResponse getPresentationsByPresentationId(final String presentationId,
                                                                                     final String authorization)
    {
        if (!"s3cr3t".equals(authorization))
        {
            return GetPresentationsByPresentationIdResponse.unauthorized();
        }

        return GetPresentationsByPresentationIdResponse.jsonOK(new Presentation().withId(presentationId)
            .withTitle("Title of " + presentationId));
    }

    @Override
    public PutPresentationsByPresentationIdResponse putPresentationsByPresentationId(final String presentationId,
                                                                                     final String authorization,
                                                                                     final Presentation entity)
    {
        // TODO implement me!
        return null;
    }

    @Override
    public PatchPresentationsByPresentationIdResponse patchPresentationsByPresentationId(final String presentationId,
                                                                                         final String authorization,
                                                                                         final Presentation entity)
    {
        // TODO implement me!
        return null;
    }

    @Override
    public void deletePresentationsByPresentationId(final String presentationId, final String authorization)
    {
        // TODO implement me!
    }
}
