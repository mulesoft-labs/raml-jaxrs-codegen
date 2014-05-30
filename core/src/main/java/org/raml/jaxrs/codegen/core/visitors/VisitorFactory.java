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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jsonschema2pojo.SchemaMapper;
import org.raml.jaxrs.codegen.core.Configuration;
import org.raml.jaxrs.codegen.core.repositories.RamlRepository;
import org.raml.jaxrs.codegen.core.repositories.SchemaRepository;
import org.raml.jaxrs.codegen.core.visitor.RamlVisitor;
import org.raml.jaxrs.codegen.core.visitor.ResourceVisitor;

import com.sun.codemodel.JCodeModel;

public class VisitorFactory {
	
	/**
	 * Instance of self.
	 */
	private static VisitorFactory INSTANCE;

	/**
	 * Singleton.
	 */
	private VisitorFactory() {
		super();
	}

	public static VisitorFactory getInstance() {
		if(INSTANCE == null) {
			INSTANCE = new VisitorFactory();
		}
		return INSTANCE;
	}

	public List<RamlVisitor> createRamlVisitors(RamlRepository ramlRepository) {
		List<RamlVisitor> result = new ArrayList<RamlVisitor>();
		result.add(new GenericRamlVisitor(ramlRepository));
		return Collections.unmodifiableList(result);
	}
	
	public List<ResourceVisitor> createResourceVisitors(SchemaRepository schemaRepository,
			JCodeModel codeModel, SchemaMapper mapper, Configuration configuration) {		
		List<ResourceVisitor> result = new ArrayList<ResourceVisitor>();
		result.add(new CodeModelVisitor(codeModel, mapper, schemaRepository, configuration));
		result.add(new HttpMethodAnnotationVisitor(codeModel, configuration));
		result.add(new ResponseWrapperVisitor(configuration, schemaRepository));
		result.add(new JSR303Visitor());
		return Collections.unmodifiableList(result);
	}
}
