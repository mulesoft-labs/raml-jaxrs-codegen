/*
 * Copyright 2013 (c) MuleSoft, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */
package org.raml.jaxrs.example.impl;

import org.raml.jaxrs.example.model.Presentation;
import org.raml.jaxrs.example.resource.PresentationsResource;

public class PresentationResourceImpl implements PresentationsResource
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
