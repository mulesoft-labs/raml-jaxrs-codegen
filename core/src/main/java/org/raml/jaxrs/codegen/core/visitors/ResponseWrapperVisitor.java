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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.raml.jaxrs.codegen.core.dataobjects.ResponseClass;

public class ResponseWrapperVisitor extends TemplateResourceVisitor {
	
	/*
	 * (non-Javadoc)
	 * @see org.raml.jaxrs.codegen.core.visitor.ResourceVisitor#visit(org.raml.jaxrs.codegen.core.dataobjects.ResponseClass)
	 */
	@Override
	public void visit(ResponseClass responseClass) {

		try {
			String supportPackage = getConfiguration().getSupportPackage();
			String template = IOUtils.toString(getClass().getResourceAsStream(
					"/org/raml/templates/ResponseWrapper." + getConfiguration().getJaxrsVersion().toString().toLowerCase() + ".template"));

			File supportPackageOutputDirectory = new File(getConfiguration().getOutputDirectory(), supportPackage.replace('.', File.separatorChar));
			if(supportPackageOutputDirectory.exists()) {
				return;
			}
			
			supportPackageOutputDirectory.mkdirs();
			String name = "ResponseWrapper";
			
			final File sourceOutputFile = new File(supportPackageOutputDirectory, name + ".java");
			final String source = template.replace("${codegen.support.package}", supportPackage);
			final FileWriter fileWriter = new FileWriter(sourceOutputFile);
			
			IOUtils.write(source, fileWriter);
			IOUtils.closeQuietly(fileWriter);
			
			getSchemaRepository().saveSupport(supportPackage.replace('.', '/') + "/" + name + ".java", sourceOutputFile);
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}
}