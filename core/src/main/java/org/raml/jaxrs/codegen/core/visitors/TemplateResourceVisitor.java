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

import org.raml.jaxrs.codegen.core.dataobjects.ArgumentType;
import org.raml.jaxrs.codegen.core.dataobjects.GlobalSchema;
import org.raml.jaxrs.codegen.core.dataobjects.ResourceEntity;
import org.raml.jaxrs.codegen.core.dataobjects.ResourceInterface;
import org.raml.jaxrs.codegen.core.dataobjects.ResourceMethod;
import org.raml.jaxrs.codegen.core.dataobjects.ResourceMethodArgument;
import org.raml.jaxrs.codegen.core.dataobjects.ResponseClass;
import org.raml.jaxrs.codegen.core.dataobjects.ResponseClassMethod;
import org.raml.jaxrs.codegen.core.dataobjects.ResponseClassMethodArgument;
import org.raml.jaxrs.codegen.core.visitor.ResourceVisitor;

public abstract class TemplateResourceVisitor implements ResourceVisitor {
	/*
	 * (non-Javadoc)
	 * @see org.raml.jaxrs.codegen.core.visitor.ResourceVisitor#start()
	 */
	@Override
	public void start() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.raml.jaxrs.codegen.core.visitor.ResourceVisitor#visit(org.raml.jaxrs.codegen.core.dataobjects.GlobalSchema)
	 */
	@Override
	public void visit(GlobalSchema globalSchema) {
	}

	/*
	 * (non-Javadoc)
	 * @see org.raml.jaxrs.codegen.core.visitor.ResourceVisitor#visit(org.raml.jaxrs.codegen.core.dataobjects.ResourceEntity)
	 */
	@Override
	public void visit(ResourceEntity resourceEntity) {
	}

	/*
	 * (non-Javadoc)
	 * @see org.raml.jaxrs.codegen.core.visitor.ResourceVisitor#visit(org.raml.jaxrs.codegen.core.dataobjects.ResourceInterface)
	 */
	@Override
	public void visit(ResourceInterface resourceInterface) {
	}

	/*
	 * (non-Javadoc)
	 * @see org.raml.jaxrs.codegen.core.visitor.ResourceVisitor#visit(org.raml.jaxrs.codegen.core.dataobjects.ResourceMethod)
	 */
	@Override
	public void visit(ResourceMethod resourceMethod) {
	}

	/*
	 * (non-Javadoc)
	 * @see org.raml.jaxrs.codegen.core.visitor.ResourceVisitor#visit(org.raml.jaxrs.codegen.core.dataobjects.ResourceMethodArgument)
	 */
	@Override
	public void visit(ResourceMethodArgument resourceMethodParameter) {
	}

	/*
	 * (non-Javadoc)
	 * @see org.raml.jaxrs.codegen.core.visitor.ResourceVisitor#visit(org.raml.jaxrs.codegen.core.dataobjects.ResponseClass)
	 */
	@Override
	public void visit(ResponseClass responseClass) {
	}

	/*
	 * (non-Javadoc)
	 * @see org.raml.jaxrs.codegen.core.visitor.ResourceVisitor#visit(org.raml.jaxrs.codegen.core.dataobjects.ResponseClassMethod)
	 */
	@Override
	public void visit(ResponseClassMethod responseClassMethod) {
	}

	/*
	 * (non-Javadoc)
	 * @see org.raml.jaxrs.codegen.core.visitor.ResourceVisitor#visit(org.raml.jaxrs.codegen.core.dataobjects.ResponseClassMethodArgument)
	 */
	@Override
	public void visit(ResponseClassMethodArgument responseClassMethodArgument) {
	}

	/*
	 * (non-Javadoc)
	 * @see org.raml.jaxrs.codegen.core.visitor.ResourceVisitor#visit(org.raml.jaxrs.codegen.core.dataobjects.ArgumentType)
	 */
	@Override
	public void visit(ArgumentType methodArgumentType) {
	}

	/*
	 * (non-Javadoc)
	 * @see org.raml.jaxrs.codegen.core.visitor.ResourceVisitor#end()
	 */
	@Override
	public void end() {
	}
}
