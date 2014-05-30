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

import java.lang.annotation.Annotation;
import java.math.BigDecimal;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.raml.jaxrs.codegen.core.dataobjects.ResourceMethodArgument;
import org.raml.model.parameter.AbstractParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.codemodel.JAnnotationUse;
import com.sun.codemodel.JVar;

public class JSR303Visitor extends TemplateResourceVisitor {

	/**
	 * Locals.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(JSR303Visitor.class);
	private static final String DEFAULT_ANNOTATION_PARAMETER = "value";
	
	/*
	 * (non-Javadoc)
	 * @see org.raml.jaxrs.codegen.core.visitor.ResourceVisitor#visit(org.raml.jaxrs.codegen.core.dataobjects.ResourceMethodArgument)
	 */
	@Override
	public void visit(ResourceMethodArgument resourceMethodParameter) {	
		if(resourceMethodParameter.getParameter() == null) {
			return;
		}
		
		final JVar argumentVariable = resourceMethodParameter.getGeneratedObject();
		AbstractParam abstractParam = resourceMethodParameter.getParameter();
		
		if (abstractParam.getMinLength() != null || abstractParam.getMaxLength() != null) {
			final JAnnotationUse sizeAnnotation = argumentVariable.annotate(Size.class);

			if (abstractParam.getMinLength() != null) {
				sizeAnnotation.param("min", abstractParam.getMinLength());
			}
			if (abstractParam.getMaxLength() != null) {
				sizeAnnotation.param("max", abstractParam.getMaxLength());
			}
		}

		if (abstractParam.getMinimum() != null) {
			addMinMaxConstraint("minimum", Min.class, abstractParam.getMinimum(), argumentVariable);
		}

		if (abstractParam.getMaximum() != null) {
			addMinMaxConstraint("maximum", Max.class, abstractParam.getMaximum(), argumentVariable);
		}

		if (abstractParam.isRequired()) {
			argumentVariable.annotate(NotNull.class);
		}
	}


	private void addMinMaxConstraint(
			final String name,
			final Class<? extends Annotation> clazz,
			final BigDecimal value,
			final JVar argumentVariable)
	{
		try {
			long boundary = value.longValueExact();
			argumentVariable.annotate(clazz).param(DEFAULT_ANNOTATION_PARAMETER, boundary);
		} catch (ArithmeticException ae) {
			LOGGER.info("Non integer " + name + " constraint ignored for parameter: " + argumentVariable);
		}
	}
}