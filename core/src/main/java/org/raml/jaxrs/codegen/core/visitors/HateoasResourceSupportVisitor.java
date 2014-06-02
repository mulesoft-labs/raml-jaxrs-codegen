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

import java.util.Iterator;
import java.util.Map;

import org.raml.jaxrs.codegen.core.dataobjects.ResourceEntity;

import com.sun.codemodel.JClass;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JMethod;

public class HateoasResourceSupportVisitor extends TemplateResourceVisitor {

	/*
	 * (non-Javadoc)
	 * @see org.raml.jaxrs.codegen.core.visitor.ResourceVisitor#visit(org.raml.jaxrs.codegen.core.dataobjects.ResourceEntity)
	 */
	@Override
	public void visit(ResourceEntity resourceEntity) {
		
		if(!getConfiguration().isHateoasResourceSupport()) {
			return;
		}

		JClass json = resourceEntity.getGeneratedObject();
		JDefinedClass jDefinedClass = getCodeModel()._getClass(json.fullName());

		// Find id field
		JFieldVar idField = null;
		for(Map.Entry<String, JFieldVar> entry : jDefinedClass.fields().entrySet()) {
			if(entry.getKey().equals("id")) {
				idField = entry.getValue();
				break;
			}
		}

		// Remove id field overrides if present
		if(idField != null) {
			jDefinedClass.removeField(idField);
			Iterator<JMethod> methods = jDefinedClass.methods().iterator();
			while(methods.hasNext()) {
				JMethod jMethod = methods.next();
				if(jMethod.name().equals("getId") || 
						jMethod.name().equals("setId") ||
						jMethod.name().equals("withId")) {
					methods.remove();
				}
			}
		}

		JClass directClass = getCodeModel().directClass("org.springframework.hateoas.ResourceSupport");
		jDefinedClass._extends(directClass);

	}
}
