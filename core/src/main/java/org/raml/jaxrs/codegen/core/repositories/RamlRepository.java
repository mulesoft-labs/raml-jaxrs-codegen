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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.raml.jaxrs.codegen.core.dataobjects.Argument;
import org.raml.jaxrs.codegen.core.dataobjects.RamlObject;

public class RamlRepository {

	/**
	 * Locals.
	 */
	private int runningNumber = 0;
	private Map<Integer, RamlObject<?>> inmemoryObjects = new HashMap<Integer, RamlObject<?>>();
	private Map<Class<?>, List<RamlObject<?>>> inmemoryObjectsByClass = new HashMap<Class<?>, List<RamlObject<?>>>();
	
	public int save(RamlObject<?> object) {
		if(object.getId() != 0) {
			throw new RuntimeException("RAML object has been already persisted!");
		}

		object.setId(allocateId());
		inmemoryObjects.put(object.getId(), object);
		
		List<RamlObject<?>> objectsByClass = inmemoryObjectsByClass.get(object.getClass());
		if(objectsByClass == null) {
			objectsByClass = new ArrayList<RamlObject<?>>();
			inmemoryObjectsByClass.put(object.getClass(), objectsByClass);
		}
		objectsByClass.add(object);
		
		if(object instanceof Argument) {
			objectsByClass = inmemoryObjectsByClass.get(Argument.class);
			if(objectsByClass == null) {
				objectsByClass = new ArrayList<RamlObject<?>>();
				inmemoryObjectsByClass.put(Argument.class, objectsByClass);
			}
			save(((Argument) object).getArgumentType());
		}
		
		return object.getId();
	}

	@SuppressWarnings("unchecked")
	public <T extends RamlObject<?>> T find(int id) {
		return (T) inmemoryObjects.get(id);
	}
	
	@SuppressWarnings("unchecked")
	public <T extends RamlObject<?>> List<T> find(Class<T> ramlClass) {
		if(ramlClass == null) {
			return Collections.emptyList();
		}
		List<?> objectsByClass = inmemoryObjectsByClass.get(ramlClass);
		if(objectsByClass == null) {
			return Collections.emptyList();
		}
		
		List<T> result = new ArrayList<T>();
		for(Object object : objectsByClass) {
			result.add((T) object);
		}
		return result;
	}
	
	private int allocateId() {
		return ++runningNumber;
	}
}