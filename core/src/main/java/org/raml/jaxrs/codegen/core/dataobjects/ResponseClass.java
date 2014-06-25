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

import com.sun.codemodel.JDefinedClass;

public class ResponseClass extends RamlObject<JDefinedClass> {
	private final ResourceMethod responMethod;

	public ResponseClass(ResourceMethod responMethod) {
		super();
		this.responMethod = responMethod;
	}

	/**
	 * @return the responMethod
	 */
	public ResourceMethod getResourceMethod() {
		return responMethod;
	}
}