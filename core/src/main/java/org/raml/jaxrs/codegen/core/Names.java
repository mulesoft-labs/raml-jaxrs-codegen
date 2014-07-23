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
package org.raml.jaxrs.codegen.core;

import org.apache.commons.lang.StringUtils;
import org.apache.http.impl.EnglishReasonPhraseCatalog;
import org.raml.model.Action;
import org.raml.model.MimeType;
import org.raml.model.Resource;

import static org.apache.commons.lang.StringUtils.*;
import static org.apache.commons.lang.WordUtils.capitalize;
import static org.apache.commons.lang.math.NumberUtils.isDigits;
import static org.raml.jaxrs.codegen.core.Constants.DEFAULT_LOCALE;

public class Names
{
    public static final String GENERIC_PAYLOAD_ARGUMENT_NAME = "entity";
    public static final String MULTIPLE_RESPONSE_HEADERS_ARGUMENT_NAME = "headers";
    public static final String EXAMPLE_PREFIX = " e.g. ";

    public static String buildResourceInterfaceName(final Resource resource)
    {
        final String resourceInterfaceName = buildJavaFriendlyName(defaultIfBlank(resource.getDisplayName(),
            resource.getRelativeUri()));

        return isBlank(resourceInterfaceName) ? "Root" : resourceInterfaceName.concat("Resource");
    }

    public static String buildVariableName(final String source)
    {
        final String name = uncapitalize(buildJavaFriendlyName(source));

        return Constants.JAVA_KEYWORDS.contains(name) ? "$" + name : name;
    }

    public static String buildJavaFriendlyName(final String source)
    {
        final String baseName = source.replaceAll("[\\W_]", " ");

        String friendlyName = capitalize(baseName).replaceAll("[\\W_]", "");

        if (isDigits(left(friendlyName, 1)))
        {
            friendlyName = "_" + friendlyName;
        }

        return friendlyName;
    }

    public static String buildResourceMethodName(final Action action, final MimeType bodyMimeType)
    {
        final String methodBaseName = buildJavaFriendlyName(action.getResource()
            .getUri()
            .replace("{", " By "));

        return action.getType().toString().toLowerCase() + buildMimeTypeInfix(bodyMimeType) + methodBaseName;
    }

    public static String buildResponseMethodName(final int statusCode, final MimeType mimeType)
    {
        final String status;
        if (statusCode == 204) {
            status = "withoutContent";
        } else {
            status = EnglishReasonPhraseCatalog.INSTANCE.getReason(statusCode, DEFAULT_LOCALE);
        }

        return uncapitalize(getShortMimeType(mimeType)
                            + buildJavaFriendlyName(defaultIfBlank(status, "_" + statusCode)));
    }

    public static String buildNestedSchemaName(final MimeType mimeType)
    {
        // TODO improve naming strategy for nested schemas
        return getShortMimeType(mimeType)
               + (isBlank(mimeType.getSchema()) ? mimeType.hashCode() : mimeType.getSchema().hashCode());
    }

    private static String buildMimeTypeInfix(final MimeType bodyMimeType)
    {
        return bodyMimeType != null ? buildJavaFriendlyName(getShortMimeType(bodyMimeType)) : "";
    }

    public static String getShortMimeType(final MimeType mimeType)
    {
        if (mimeType == null)
        {
            return "";
        }

        return remove(remove(StringUtils.substringAfter(mimeType.getType().toLowerCase(DEFAULT_LOCALE), "/"),
            "x-www-"),"+");
    }

    private Names()
    {
        throw new UnsupportedOperationException();
    }
}
