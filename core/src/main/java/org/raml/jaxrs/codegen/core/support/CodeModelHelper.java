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

import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.raml.jaxrs.codegen.core.Names.EXAMPLE_PREFIX;

import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JType;

public class CodeModelHelper {

  public static JType getGeneratorType(JCodeModel codeModel, final Class<?> clazz) {
    return clazz.isPrimitive() ? JType.parse(codeModel, clazz.getSimpleName()) : codeModel.ref(clazz);
  }

  public static String getPrefixedExampleOrBlank(final String example) {
    return isNotBlank(example) ? EXAMPLE_PREFIX + example : "";
  }
}