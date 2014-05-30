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

import static org.apache.commons.lang.StringUtils.defaultIfBlank;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.apache.commons.lang.StringUtils.substringAfter;
import static org.apache.commons.lang.StringUtils.substringAfterLast;
import static org.apache.commons.lang.StringUtils.substringBefore;

import java.io.File;
import java.math.BigDecimal;
import java.util.Date;

import org.raml.model.MimeType;
import org.raml.model.parameter.AbstractParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Types
{
  private static final Logger LOGGER = LoggerFactory.getLogger(Types.class);

  public static boolean isCompatibleWith(final MimeType mt, final String... mediaTypes)
  {
    final String mimeType = mt.getType();

    if (isBlank(mimeType))
    {
      return false;
    }

    for (final String mediaType : mediaTypes)
    {
      if (mediaType.toString().equals(mimeType))
      {
        return true;
      }

      final String primaryType = substringBefore(mimeType, "/");

      if (substringBefore(mediaType, "/").equals(primaryType))
      {
        final String subType = defaultIfBlank(substringAfterLast(mimeType, "+"),
          substringAfter(mimeType, "/"));

        if (substringAfter(mediaType, "/").equals(subType))
        {
          return true;
        }
      }
    }

    return false;
  }

  public static String buildSchemaKey(final MimeType mimeType)
  {
    return Names.getShortMimeType(mimeType) + "@" + mimeType.getSchema().hashCode();
  }

  public static Class<?> getJavaType(final AbstractParam parameter)
  {
    if (parameter.getType() == null)
    {
      return String.class;
    }

    final boolean usePrimitive = !parameter.isRepeat()
        && (parameter.isRequired() || isNotBlank(parameter.getDefaultValue()));

    switch (parameter.getType())
    {
      case BOOLEAN :
        return usePrimitive ? boolean.class : Boolean.class;
      case DATE :
        return Date.class;
      case FILE :
        return File.class;
      case INTEGER :
        return usePrimitive ? long.class : Long.class;
      case NUMBER :
        return BigDecimal.class;
      case STRING :
        return String.class;
      default :
        LOGGER.warn("Unsupported RAML type: " + parameter.getType().toString());
        return Object.class;
    }
  }
}
