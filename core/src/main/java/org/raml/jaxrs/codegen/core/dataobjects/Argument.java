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

import org.raml.model.MimeType;
import org.raml.model.parameter.AbstractParam;

import com.sun.codemodel.JVar;

public abstract class Argument extends RamlObject<JVar> {

	private final ResourceMethod resourceMethod;
	private final String headerName;
	private final AbstractParam parameter;
	private final MimeType mimeType;
	private final ArgumentType argumentType;
	
	public Argument(ResourceMethod resourceMethod, ArgumentType argumentType, String headerName, AbstractParam parameter, MimeType mimeType) {
		super();
		this.resourceMethod = resourceMethod;
		this.headerName = headerName;
		this.parameter = parameter;
		this.argumentType = argumentType;
		this.mimeType = mimeType;
		argumentType.setArgument(this);
	}
	
	/**
	 * @return the headerName
	 */
	public String getHeaderName() {
		return headerName;
	}
	/**
	 * @return the parameter
	 */
	@SuppressWarnings("unchecked")
	public <K extends AbstractParam> K getParameter() {
		return (K) parameter;
	}

	/**
	 * @return the resourceMethod
	 */
	public ResourceMethod getResourceMethod() {
		return resourceMethod;
	}

	/**
	 * @return the argumentType
	 */
	public ArgumentType getArgumentType() {
		return argumentType;
	}



	/**
	 * @return the mimeType
	 */
	public MimeType getMimeType() {
		return mimeType;
	}
}