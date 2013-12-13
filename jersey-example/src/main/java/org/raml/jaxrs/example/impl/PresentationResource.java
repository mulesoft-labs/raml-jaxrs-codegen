
package org.raml.jaxrs.example.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;

import javax.ws.rs.core.StreamingOutput;

import org.raml.jaxrs.example.resource.Presentations;

public class PresentationResource implements Presentations
{
    @Override
    public PostPresentationsResponse postPresentations(final String authorization, final Reader entity)
    {
        return PostPresentationsResponse.created(new StreamingOutput()
        {
            @Override
            public void write(final OutputStream output) throws IOException
            {
                output.write("A created JSON presentation".getBytes());
            }
        });
    }

    @Override
    public GetPresentationsResponse getPresentations(final String authorization,
                                                     final String title,
                                                     final Double start,
                                                     final Double pages)
    {
        return GetPresentationsResponse.oK(new StreamingOutput()
        {
            @Override
            public void write(final OutputStream output) throws IOException
            {
                output.write("A JSON object of presentations".getBytes());
            }
        });
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

    @Override
    public PatchPresentationsByPresentationIdResponse patchPresentationsByPresentationId(final String authorization,
                                                                                         final Reader entity)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PutPresentationsByPresentationIdResponse putPresentationsByPresentationId(final String authorization,
                                                                                     final Reader entity)
    {
        // TODO Auto-generated method stub
        return null;
    }
}
