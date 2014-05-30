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
package org.raml.jaxrs.codegen.core;

import static org.apache.commons.lang.StringUtils.join;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.jsonschema2pojo.AnnotatorFactory;
import org.jsonschema2pojo.GenerationConfig;
import org.jsonschema2pojo.SchemaGenerator;
import org.jsonschema2pojo.SchemaMapper;
import org.jsonschema2pojo.SchemaStore;
import org.jsonschema2pojo.rules.RuleFactory;
import org.raml.jaxrs.codegen.core.dataobjects.ArgumentType;
import org.raml.jaxrs.codegen.core.dataobjects.GlobalSchema;
import org.raml.jaxrs.codegen.core.dataobjects.ResourceEntity;
import org.raml.jaxrs.codegen.core.dataobjects.ResourceInterface;
import org.raml.jaxrs.codegen.core.dataobjects.ResourceMethod;
import org.raml.jaxrs.codegen.core.dataobjects.ResourceMethodArgument;
import org.raml.jaxrs.codegen.core.dataobjects.ResponseClass;
import org.raml.jaxrs.codegen.core.dataobjects.ResponseClassMethod;
import org.raml.jaxrs.codegen.core.dataobjects.ResponseClassMethodArgument;
import org.raml.jaxrs.codegen.core.repositories.RamlRepository;
import org.raml.jaxrs.codegen.core.repositories.SchemaRepository;
import org.raml.jaxrs.codegen.core.visitor.RamlVisitor;
import org.raml.jaxrs.codegen.core.visitor.ResourceVisitor;
import org.raml.jaxrs.codegen.core.visitors.VisitorFactory;
import org.raml.parser.rule.ValidationResult;
import org.raml.parser.visitor.RamlValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.sun.codemodel.JCodeModel;

public class Generator {

	private static final Logger LOGGER = LoggerFactory.getLogger(Generator.class);


	public Set<String> run(final Reader ramlReader, final Configuration configuration) throws Exception {

		final String ramlBuffer = IOUtils.toString(ramlReader);

		validateRaml(ramlBuffer);
		validateConfiguration(configuration);

		return generate(configuration, ramlBuffer);
	}

	public Set<String> generate(Configuration configuration, String ramlAsString) throws IOException {

		GenerationConfig jsonSchemaGenerationConfig = configuration.createJsonSchemaGenerationConfig();
		SchemaMapper schemaMapper = new SchemaMapper(new RuleFactory(jsonSchemaGenerationConfig,
				new AnnotatorFactory().getAnnotator(configuration.getJsonMapper()),
				new SchemaStore()), new SchemaGenerator());
		JCodeModel codeModel = new JCodeModel();
		SchemaRepository schemaRepository = new SchemaRepository();
		RamlRepository ramlRepository = new RamlRepository();

		// Parse RAML format to generator end result like structure
		for(RamlVisitor ramlVisitor : VisitorFactory.getInstance().createRamlVisitors(ramlRepository)) {
			ramlVisitor.
			start().
			visit(ramlAsString).
			end();
		}

		// Create code model resource
		List<ResourceVisitor> visitors = VisitorFactory.getInstance().createResourceVisitors(schemaRepository, codeModel, schemaMapper, configuration);
		for(ResourceVisitor visitor : visitors) {
			visitor.start();

			// Global schemas
			for(GlobalSchema visitable : ramlRepository.find(GlobalSchema.class)) {
				visitor.visit(visitable);
			}

			// JSON entities
			for(ResourceEntity visitable : ramlRepository.find(ResourceEntity.class)) {
				visitor.visit(visitable);
			}

			// Resource interfaces
			for(ResourceInterface visitable : ramlRepository.find(ResourceInterface.class)) {
				visitor.visit(visitable);
			}

			// Argument types in the resource and response method signatures
			for(ArgumentType visitable : ramlRepository.find(ArgumentType.class)) {
				visitor.visit(visitable);
			}

			// Response classes
			for(ResponseClass visitable : ramlRepository.find(ResponseClass.class)) {
				visitor.visit(visitable);
			}

			// Response class methods
			for(ResponseClassMethod visitable : ramlRepository.find(ResponseClassMethod.class)) {
				visitor.visit(visitable);
			}

			// Response class method arguments
			for(ResponseClassMethodArgument visitable : ramlRepository.find(ResponseClassMethodArgument.class)) {
				visitor.visit(visitable);
			}				

			// Resource methods
			for(ResourceMethod visitable : ramlRepository.find(ResourceMethod.class)) {
				visitor.visit(visitable);
			}

			// Resource method arguments
			for(ResourceMethodArgument visitable : ramlRepository.find(ResourceMethodArgument.class)) {
				visitor.visit(visitable);
			}

			visitor.end();
		}


		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		final PrintStream ps = new PrintStream(baos);
		codeModel.build(configuration.getOutputDirectory(), ps);
		ps.close();

		Set<String> generatedFiles = new HashSet<String>();
		generatedFiles.addAll(schemaRepository.getSupport());
		generatedFiles.addAll(Arrays.asList(StringUtils.split(baos.toString())));

		schemaRepository.deleteGlobal();
		
		Set<String> result = new HashSet<String>();
		for(String generatedFile : generatedFiles) {
			result.add(generatedFile.replace("\\", "/"));
		}
		
		return result;
	}
	
	public void validateRaml(String ramlAsString) {
		final List<ValidationResult> results = RamlValidationService.createDefault().validate(ramlAsString, new File("").getPath());

		if (!ValidationResult.areValid(results)) {

			List<String> validationErrors = Lists.transform(results, new Function<ValidationResult, String>() {
				@Override
				public String apply(final ValidationResult vr) {
					return String.format("%s %s", vr.getStartColumn(), vr.getMessage());
				}
			});

			throw new IllegalArgumentException("Invalid RAML definition:\n" + join(validationErrors, "\n"));
		}
	}

	public void validateConfiguration(Configuration configuration) {
		// Validate RAML configuration
		Validate.notNull(configuration, "configuration can't be null");
		File outputDirectory = configuration.getOutputDirectory();
		Validate.notNull(outputDirectory, "outputDirectory can't be null");
		Validate.isTrue(outputDirectory.isDirectory(), outputDirectory + " is not a pre-existing directory");
		Validate.isTrue(outputDirectory.canWrite(), outputDirectory + " can't be written to");

		if (outputDirectory.listFiles().length > 0) {
			LOGGER.warn("Directory "
					+ outputDirectory
					+ " is not empty, generation will work but pre-existing files may remain and produce unexpected results");
		}
		Validate.notEmpty(configuration.getBasePackageName(), "base package name can't be empty");		
	}
}
