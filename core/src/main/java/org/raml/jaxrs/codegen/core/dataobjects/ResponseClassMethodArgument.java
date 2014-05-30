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

import org.raml.model.parameter.Header;

public class ResponseClassMethodArgument extends Argument {

	private final ResponseClassMethod responseClassMethod;
	
	public ResponseClassMethodArgument(
			ResponseClassMethod responseClassMethod,
			String headerName, Header header, ArgumentType argumentType) {
		
		super(responseClassMethod.getResponseClass().getResourceMethod(), argumentType, headerName, header, null);
		this.responseClassMethod = responseClassMethod;
	}
	
	/**
	 * @return the responseMethod
	 */
	public ResponseClassMethod getResponseClassMethod() {
		return responseClassMethod;
	}
}