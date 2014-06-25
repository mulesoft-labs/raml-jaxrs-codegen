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

package org.raml.jaxrs.codegen.core.visitors;

import static org.raml.jaxrs.codegen.core.Constants.RESPONSE_HEADER_WILDCARD_SYMBOL;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import javax.ws.rs.core.MediaType;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.raml.jaxrs.codegen.core.Types;
import org.raml.jaxrs.codegen.core.dataobjects.ArgumentType;
import org.raml.jaxrs.codegen.core.dataobjects.GlobalSchema;
import org.raml.jaxrs.codegen.core.dataobjects.ResourceEntity;
import org.raml.jaxrs.codegen.core.dataobjects.ResourceInterface;
import org.raml.jaxrs.codegen.core.dataobjects.ResourceMethod;
import org.raml.jaxrs.codegen.core.dataobjects.ResourceMethodArgument;
import org.raml.jaxrs.codegen.core.dataobjects.ResponseClass;
import org.raml.jaxrs.codegen.core.dataobjects.ResponseClassMethod;
import org.raml.jaxrs.codegen.core.dataobjects.ResponseClassMethodArgument;
import org.raml.jaxrs.codegen.core.repositories.RamlRepository;
import org.raml.jaxrs.codegen.core.support.RamlHelper;
import org.raml.jaxrs.codegen.core.visitor.RamlVisitor;
import org.raml.model.Action;
import org.raml.model.MimeType;
import org.raml.model.Raml;
import org.raml.model.Resource;
import org.raml.model.Response;
import org.raml.model.parameter.FormParameter;
import org.raml.model.parameter.Header;
import org.raml.model.parameter.QueryParameter;
import org.raml.model.parameter.UriParameter;
import org.raml.parser.visitor.RamlDocumentBuilder;

public class GenericRamlVisitor implements RamlVisitor {

  private RamlRepository ramlRepository = null;

  public GenericRamlVisitor(RamlRepository ramlRepository) {
    this.ramlRepository = ramlRepository;
  }

  /*
   * (non-Javadoc)
   * @see org.raml.jaxrs.codegen.core.visitor.RamlVisitor#start()
   */
  @Override
  public RamlVisitor start() {
    return this;
  }

  /*
   * (non-Javadoc)
   * @see org.raml.jaxrs.codegen.core.visitor.RamlVisitor#visit(java.lang.String)
   */
  @Override
  public RamlVisitor visit(String ramlAsString) {
    Raml raml = new RamlDocumentBuilder().build(ramlAsString, new File("").getPath());

    // Create consolidated schemas
    for(Entry<String, String> nameAndSchema : raml.getConsolidatedSchemas().entrySet()) {
      ramlRepository.save(new GlobalSchema(nameAndSchema.getKey(), nameAndSchema.getValue()));
    }

    // Create resources
    for (final Resource resource : raml.getResources().values()){
      ResourceInterface resourceInterface = new ResourceInterface(resource);
      ramlRepository.save(resourceInterface);
    }

    // Create resource methods
    for(ResourceInterface resourceInterface : ramlRepository.find(ResourceInterface.class)) {
      visitMethods(resourceInterface, resourceInterface.getResource());
    }

    // Create method response classes
    for(ResourceMethod method : ramlRepository.find(ResourceMethod.class)) {

      if(!RamlHelper.getUniqueResponseMimeTypes(method.getAction()).isEmpty()) {
        ramlRepository.save(new ResponseClass(method));
      }
    }

    // Create response class methods
    for(ResponseClass responseClass : ramlRepository.find(ResponseClass.class)) {
      for(Entry<String, Response> statusCodeAndResponse : responseClass.getResourceMethod().getAction().getResponses().entrySet()) {
        Response response = statusCodeAndResponse.getValue();
        if(!response.hasBody()) {
          ramlRepository.save(new ResponseClassMethod(responseClass, null, NumberUtils.toInt(statusCodeAndResponse.getKey()), response));
          continue;
        }
        // Create response methods
        for (MimeType mimeType : response.getBody().values()) {
          ramlRepository.save(new ResponseClassMethod(responseClass, mimeType, NumberUtils.toInt(statusCodeAndResponse.getKey()), response));
        }
      }
    }

    // Create response class method entities
    for(ResponseClassMethod method : ramlRepository.find(ResponseClassMethod.class)) {
      MimeType mimeType = method.getMimeType();
      if(mimeType == null) {
        continue;
      }
      String schemaNameOrContent = mimeType.getSchema();
      if (StringUtils.isBlank(schemaNameOrContent)) {
        continue;
      }

      if(Types.isCompatibleWith(mimeType, MediaType.APPLICATION_JSON)) {
        GlobalSchema globalSchema = getEntity(schemaNameOrContent);
        boolean repeat = globalSchema != null;
        if(repeat) {
          mimeType.setSchema(globalSchema.getName());
        }
        method.setRepeat(repeat);
        ramlRepository.save(new ResourceEntity(method, mimeType));
      }
    }

    // Create response class method arguments
    for(ResponseClassMethod method : ramlRepository.find(ResponseClassMethod.class)) {
      for (Entry<String, Header> namedHeaderParameter : method.getResponse().getHeaders().entrySet()) {
        String headerName = namedHeaderParameter.getKey();
        if(!headerName.contains(RESPONSE_HEADER_WILDCARD_SYMBOL)) {
          ArgumentType type = new ArgumentType(ArgumentType.Type.HEADER, false);
          ResponseClassMethodArgument argument = new ResponseClassMethodArgument(method, headerName, namedHeaderParameter.getValue(), type);
          method.addArgument(argument);
          ramlRepository.save(argument);
        }
      }
    }

    // Create resource method entities
    for(ResourceMethod method : ramlRepository.find(ResourceMethod.class)) {
      Set<String> uriParamNames = new HashSet<String>();
      getResolvedUriParameters(method.getAction().getResource(), uriParamNames, method);

      for(Entry<String, UriParameter> param : method.getAction().getResource().getUriParameters().entrySet()) {
        if(!uriParamNames.contains(param.getKey())) {
          ArgumentType type = new ArgumentType(ArgumentType.Type.URI, false);
          ramlRepository.save(new ResourceMethodArgument(method, type, param.getKey(), param.getValue()));
        }
      }
      for(Entry<String, Header> param : method.getAction().getHeaders().entrySet()) {
        ArgumentType type = new ArgumentType(ArgumentType.Type.HEADER, false);
        ramlRepository.save(new ResourceMethodArgument(method, type, param.getKey(), param.getValue()));
      }
      for(Entry<String, QueryParameter> param : method.getAction().getQueryParameters().entrySet()) {
        ArgumentType type = new ArgumentType(ArgumentType.Type.QUERY, false);
        ramlRepository.save(new ResourceMethodArgument(method, type, param.getKey(), param.getValue()));
      }

      MimeType mimeType = method.getMimeType();
      if (mimeType != null) {
        if (Types.isCompatibleWith(mimeType, MediaType.APPLICATION_FORM_URLENCODED)) {

          if (RamlHelper.hasAMultiTypeFormParameter(mimeType)) {
            ArgumentType type = new ArgumentType(ArgumentType.Type.MULTIVALUE, false);
            ramlRepository.save(new ResourceMethodArgument(method, type, mimeType));
          } else {
            for (final Entry<String, List<FormParameter>> param : mimeType.getFormParameters().entrySet()) {
              ArgumentType type = new ArgumentType(ArgumentType.Type.FORM, false);
              ramlRepository.save(new ResourceMethodArgument(method, type, param.getKey(), param.getValue().get(0)));
            }
          }
        } else if (Types.isCompatibleWith(mimeType, MediaType.MULTIPART_FORM_DATA)) {
          ArgumentType type = new ArgumentType(ArgumentType.Type.MULTIPART, false);
          ramlRepository.save(new ResourceMethodArgument(method, type, mimeType));

        } else if(Types.isCompatibleWith(mimeType, MediaType.APPLICATION_JSON)) {
          final String schemaNameOrContent = mimeType.getSchema();
          if (!StringUtils.isBlank(schemaNameOrContent)) {

            GlobalSchema globalSchema = getEntity(schemaNameOrContent);
            boolean repeat = globalSchema != null;
            if(repeat) {
              mimeType.setSchema(globalSchema.getName());
            }

            ramlRepository.save(new ResourceEntity(method, mimeType));
            ArgumentType type = new ArgumentType(ArgumentType.Type.JSON, repeat);
            ramlRepository.save(new ResourceMethodArgument(method, type, mimeType));
          } else {
            ArgumentType type = new ArgumentType(ArgumentType.Type.UNKNOWN, false);
            ramlRepository.save(new ResourceMethodArgument(method, type, mimeType));
          }
        }
      }

    }
    return this;
  }

  /*
   * (non-Javadoc)
   * @see org.raml.jaxrs.codegen.core.visitor.RamlVisitor#end()
   */
  @Override
  public RamlVisitor end() {
    return this;
  }

  private void visitMethods(ResourceInterface resourceInterface, Resource resource) {
    for (final Action action : resource.getActions().values()) {

      if(!action.hasBody()) {
        ramlRepository.save(new ResourceMethod(resourceInterface, action, null));
        continue;
      }
      if (action.getBody().size() <= 1) {
        final MimeType bodyMimeType = action.getBody().isEmpty() ? null : action.getBody()
            .values()
            .iterator()
            .next();

        ramlRepository.save(new ResourceMethod(resourceInterface, action, bodyMimeType));
      } else {
        for (final MimeType bodyMimeType : action.getBody().values()) {
          ramlRepository.save(new ResourceMethod(resourceInterface, action, bodyMimeType));
        }
      }
    }

    for (final Resource childResource : resource.getResources().values()) {
      visitMethods(resourceInterface, childResource);
    }
  }

  private GlobalSchema getEntity(String schema) {
    schema = schema.replace(" ", "").toLowerCase();
    if(schema.contains("\"type\":\"array\"")) {
      int referenceStart = schema.indexOf("\"$ref\":");
      if(referenceStart < 0) {
        return null;
      }
      schema = schema.substring(referenceStart);
      schema = schema.substring(schema.indexOf(":\"") + 2);
      schema = schema.substring(0, schema.indexOf("\""));

      for(GlobalSchema globalSchema : ramlRepository.find(GlobalSchema.class)) {
        if(globalSchema.getName().equalsIgnoreCase(schema)) {
          return globalSchema;
        }
      }
    }
    return null;
  }

  public void getResolvedUriParameters(Resource resource, Set<String> uriParamNames, ResourceMethod method) {
    if(resource == null) {
      return;
    }

    for(Entry<String, UriParameter> param : resource.getResolvedUriParameters().entrySet()) {
      if(!uriParamNames.contains(param.getKey())) {
        ArgumentType type = new ArgumentType(ArgumentType.Type.URI, false);
        ramlRepository.save(new ResourceMethodArgument(method, type, param.getKey(), param.getValue()));
        uriParamNames.add(param.getKey());
      }
    }

    getResolvedUriParameters(resource.getParentResource(), uriParamNames, method);
  }
}