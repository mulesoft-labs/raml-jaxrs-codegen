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

import com.sun.codemodel.JType;

public class ArgumentType extends RamlObject<JType> {

  private Argument argument;
  private final Type type;
  private final boolean repeat;

  public ArgumentType(Type type, boolean repeat) {
    super();
    this.type = type;
    this.repeat = repeat;
  }
  /**
   * @return the type
   */
  public Type getType() {
    return type;
  }

  /**
   * @return the argument
   */
  public Argument getArgument() {
    return argument;
  }

  /**
   * @param argument the argument to set
   */
  public void setArgument(Argument argument) {
    this.argument = argument;
  }

  public enum Type {
    URI,
    QUERY,
    HEADER,
    FORM,
    JSON,
    MULTIPART,
    MULTIVALUE,
    UNKNOWN
  }

  /**
   * @return the repeat
   */
  public boolean isRepeat() {
    return repeat;
  }
}