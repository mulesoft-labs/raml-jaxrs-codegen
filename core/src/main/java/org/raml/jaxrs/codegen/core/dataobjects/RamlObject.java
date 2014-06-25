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

import org.raml.jaxrs.codegen.core.visitor.Visitable;
import org.raml.jaxrs.codegen.core.visitor.Visitor;

public abstract class RamlObject<T> implements Visitable {

	/**
	 * Unique identifier.
	 */
	private int id;
	
	/**
	 * Generated object in code model.
	 */
	private T generatedObject;
	
	/**
	 * @return the id
	 */
	public int getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(int id) {
		this.id = id;
	}
	
	public void accept(Visitor visitor) {
		visitor.visit(this);
	}

	/**
	 * @return the generatedObject
	 */
	public T getGeneratedObject() {
		return generatedObject;
	}

	/**
	 * @param generatedObject the generatedObject to set
	 */
	public void setGeneratedObject(T generatedObject) {
		this.generatedObject = generatedObject;
	}
}