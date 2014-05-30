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

package org.raml.jaxrs.codegen.core.repositories;

import java.io.File;
import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Files;

public class SchemaRepository {

	private static final Logger LOGGER = LoggerFactory.getLogger(SchemaRepository.class);
	private final File globalSchemaStore = Files.createTempDir();
	private final Map<String, File> support = new HashMap<String, File>();
	private final Map<String, String> consolidatedSchemas = new HashMap<String, String>();

	public void saveGlobal(String fileName, String contents) {
		try {

			final File schemaFile = new File(globalSchemaStore, fileName);
			FileUtils.writeStringToFile(schemaFile, contents);
			consolidatedSchemas.put(fileName, contents);
		} catch(IOException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	/**
	 * @return a {schema file, schema name} tuple.
	 */
	public Entry<File, String> getGlobal(final String schemaNameOrContent)
	{
		try {
			if (consolidatedSchemas.containsKey(schemaNameOrContent))
			{
				// schemaNameOrContent is actually a global name
				return new SimpleEntry<File, String>(new File(globalSchemaStore, schemaNameOrContent), schemaNameOrContent);
			} else {
				// this is not a global reference but a local schema def - dump it to a temp file so
				// the type generators can pick it up
				final String schemaFileName = "schema" + schemaNameOrContent.hashCode();
				final File schemaFile = new File(globalSchemaStore, schemaFileName);
				FileUtils.writeStringToFile(schemaFile, schemaNameOrContent);
				return new SimpleEntry<File, String>(schemaFile, null);
			}
		} catch(IOException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	public void deleteGlobal() {
		try {
			FileUtils.deleteDirectory(globalSchemaStore);
		} catch (final Exception e) {
			LOGGER.warn("Failed to delete temporary directory: " + globalSchemaStore);
		}
	}

	public void saveSupport(String name, File file) {
		support.put(name, file);
	}

	public Set<String> getSupport() {
		return Collections.unmodifiableSet(support.keySet());
	}
}