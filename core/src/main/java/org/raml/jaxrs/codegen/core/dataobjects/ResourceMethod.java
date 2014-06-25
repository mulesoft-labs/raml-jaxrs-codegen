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

import org.raml.model.Action;
import org.raml.model.MimeType;

import com.sun.codemodel.JMethod;

public class ResourceMethod extends RamlObject<JMethod> {

	private final Action action;
	private final MimeType mimeType;
	private final ResourceInterface resourceInterface;
	private ResponseClass responseClass;
	private final List<ResourceMethodArgument> parameters = new ArrayList<ResourceMethodArgument>();
	
	public ResourceMethod(ResourceInterface resourceInterface, Action action, MimeType bodyMimeType) {
		super();
		this.action = action;
		this.mimeType = bodyMimeType;
		this.resourceInterface = resourceInterface;
	}

	/**
	 * @return the action
	 */
	public Action getAction() {
		return action;
	}

	/**
	 * @return the bodyMimeType
	 */
	public MimeType getMimeType() {
		return mimeType;
	}

	/**
	 * @return the resourceInterface
	 */
	public ResourceInterface getResourceInterface() {
		return resourceInterface;
	}

	/**
	 * @return the responseClass
	 */
	public ResponseClass getResponseClass() {
		return responseClass;
	}

	/**
	 * @param responseClass the responseClass to set
	 */
	public void setResponseClass(ResponseClass responseClass) {
		this.responseClass = responseClass;
	}

	/**
	 * @return the parameters
	 */
	public List<ResourceMethodArgument> getParameters() {
		return parameters;
	}
	
	public void add(ResourceMethodArgument resourceMethod) {
		this.parameters.add(resourceMethod);
	}
}