
package org.raml.jaxrs.example.impl;

import org.raml.jaxrs.example.resource.Presentations;

public class PresentationResource implements Presentations
{
    @Override
    public PostPresentationsResponse postPresentations(final String authorization, final Object entity)
    {
        return PostPresentationsResponse.created("A created JSON presentation");
    }

    @Override
    public GetPresentationsResponse getPresentations(final String authorization,
                                                     final String title,
                                                     final Double start,
                                                     final Double pages)
    {
        return GetPresentationsResponse.oK("A JSON object of presentations");
    }

    @Override
    public PutPresentationsByPresentationIdResponse putPresentationsByPresentationId(final String authorization,
                                                                                     final Object entity)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PatchPresentationsByPresentationIdResponse patchPresentationsByPresentationId(final String authorization,
                                                                                         final Object entity)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void deletePresentationsByPresentationId(final String authorization)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public GetPresentationsByPresentationIdResponse getPresentationsByPresentationId(final String authorization)
    {
        // TODO Auto-generated method stub
        return null;
    }
}
