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

package org.raml.jaxrs.codegen.core.dataobjects;

import java.util.ArrayList;
import java.util.List;

import org.raml.model.MimeType;
import org.raml.model.Response;

import com.sun.codemodel.JMethod;

public class ResponseClassMethod extends RamlObject<JMethod> {

  private final ResponseClass responseClass;
  private final MimeType mimeType;
  private final int statusCode;
  private final Response response;
  private boolean repeat;
  private final List<ResponseClassMethodArgument> arguments = new ArrayList<ResponseClassMethodArgument>();

  public ResponseClassMethod(ResponseClass responseClass,
                             MimeType mimeType, int statusCode, Response response) {
    super();
    this.responseClass = responseClass;
    this.mimeType = mimeType;
    this.statusCode = statusCode;
    this.response = response;
  }

  public ResponseClassMethod(ResponseClass responseClass,
                             MimeType mimeType, int statusCode, Response response, boolean repeat) {
    super();
    this.responseClass = responseClass;
    this.mimeType = mimeType;
    this.statusCode = statusCode;
    this.response = response;
    this.repeat = repeat;
  }

  /**
   * @return the responseClass
   */
  public ResponseClass getResponseClass() {
    return responseClass;
  }
  /**
   * @return the mimeTypes
   */
  public MimeType getMimeType() {
    return mimeType;
  }
  /**
   * @return the statusCode
   */
  public int getStatusCode() {
    return statusCode;
  }

  /**
   * @return the response
   */
  public Response getResponse() {
    return response;
  }

  public void addArgument(ResponseClassMethodArgument argument) {
    arguments.add(argument);
  }

  /**
   * @return the arguments
   */
  public List<ResponseClassMethodArgument> getArguments() {
    return arguments;
  }

  /**
   * @return the repeat
   */
  public boolean isRepeat() {
    return repeat;
  }

  /**
   * @param repeat the repeat to set
   */
  public void setRepeat(boolean repeat) {
    this.repeat = repeat;
  }
}
