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

package org.raml.jaxrs.codegen.core.support;

import static org.apache.commons.lang.StringUtils.defaultString;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.apache.commons.lang.StringUtils.strip;
import static org.raml.jaxrs.codegen.core.Names.EXAMPLE_PREFIX;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;
import org.raml.jaxrs.codegen.core.dataobjects.ResourceInterface;
import org.raml.jaxrs.codegen.core.dataobjects.ResourceMethod;
import org.raml.model.Action;
import org.raml.model.MimeType;
import org.raml.model.Resource;
import org.raml.model.Response;
import org.raml.model.parameter.AbstractParam;
import org.raml.model.parameter.FormParameter;

public class RamlHelper {

  public static List<String> getAllFormParametersArgument(final MimeType bodyMimeType)
  {
    // build a javadoc text out of all the params
    List<String> result = new ArrayList<String>();
    for (final Entry<String, List<FormParameter>> namedFormParameters : bodyMimeType.getFormParameters().entrySet()) {

      final StringBuilder sb = new StringBuilder();
      sb.append(namedFormParameters.getKey()).append(": ");

      for (final FormParameter formParameter : namedFormParameters.getValue())
      {
        appendParameterJavadocDescription(formParameter, sb);
      }
      result.add(sb.toString());
    }
    return result;
  }

  public static boolean hasAMultiTypeFormParameter(final MimeType bodyMimeType)
  {
    for (final List<FormParameter> formParameters : bodyMimeType.getFormParameters().values())
    {
      if (formParameters.size() > 1)
      {
        return true;
      }
    }
    return false;
  }

  public static Collection<MimeType> getUniqueResponseMimeTypes(final Action action)
  {
    final Map<String, MimeType> responseMimeTypes = new HashMap<String, MimeType>();
    for (final Response response : action.getResponses().values())
    {
      if (response.hasBody())
      {
        for (final MimeType responseMimeType : response.getBody().values())
        {
          if (responseMimeType != null)
          {
            responseMimeTypes.put(responseMimeType.getType(), responseMimeType);
          }
        }
      }
    }
    return responseMimeTypes.values();
  }

  public static void appendParameterJavadocDescription(final AbstractParam param, final StringBuilder sb) {
    if (isNotBlank(param.getDisplayName()))
    {
      sb.append(param.getDisplayName());
    }

    if (isNotBlank(param.getDescription()))
    {
      if (sb.length() > 0)
      {
        sb.append(" - ");
      }
      sb.append(param.getDescription());
    }

    if (isNotBlank(param.getExample()))
    {
      sb.append(EXAMPLE_PREFIX).append(param.getExample());
    }

    sb.append("<br/>\n");
  }

  public static String getJavadoc(final AbstractParam parameter)
  {
    return defaultString(parameter.getDescription()) + getPrefixedExampleOrBlank(parameter.getExample());
  }

  public static String getPrefixedExampleOrBlank(final String example)
  {
    return isNotBlank(example) ? EXAMPLE_PREFIX + example : "";
  }

  public static String getMethodPath(ResourceMethod resourceMethod) {
    String resourceInterfacePath = strip(resourceMethod.getResourceInterface().getResource().getRelativeUri(), "/");
    String path = StringUtils.substringAfter(resourceMethod.getAction().getResource().getUri(), resourceInterfacePath + "/");
    return path;
  }

  public static final String getResourceInterfacePath(ResourceInterface resourceInterface) {
    Resource resource = resourceInterface.getResource();
    String path = strip(resource.getRelativeUri(), "/");
    path = StringUtils.defaultIfBlank(path, "/");
    return path;
  }
  public static final String getResourceInterfaceName(String resourceInterfaceName, String resourcePostfix) {
    if(resourcePostfix != null) {
      resourceInterfaceName = resourceInterfaceName.substring(0, resourceInterfaceName.length() - resourcePostfix.length());
    }
    return resourceInterfaceName;
  }
}