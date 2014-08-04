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
package raml.jaxrs.eclipse.plugin;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;

public class UIConfiguration {
	
	ObjectReference<String> basePackageName = new ObjectReference<String>();
	
	ObjectReference<IResource> ramlFile = new ObjectReference<IResource>();
	
	ObjectReference<IResource> srcFolder = new ObjectReference<IResource>();
	
	ObjectReference<IResource> dstFolder = new ObjectReference<IResource>();
	
	public String getBasePackageName() {		
		return basePackageName.get();
	}

	public void setBasePackageName(String basePackageName) {
		this.basePackageName.set(basePackageName);
	}
	
	public void setRamlFile( IFile ramlFile ){
		this.ramlFile.set(ramlFile) ;		
	}
	
	public void setSrcFolder( IContainer srcFolder ){
		this.srcFolder.set(srcFolder) ;
	}
	
	public void setDstFolder( IContainer dstFolder ){
		this.dstFolder.set(dstFolder) ;
	}
	
	public IFile getRamlFile() {
		return (IFile) ramlFile.get();
	}

	public IContainer getSrcFolder() {
		return (IContainer) srcFolder.get();
	}

	public IContainer getDstFolder() {
		return (IContainer) dstFolder.get();
	}

	public boolean isValid() {
		
		if(basePackageName.get() == null){
			return false;
		}
		if(ramlFile.get() == null){
			return false;
		}
		if(dstFolder.get()==null){
			return false;
		}
		return true;
	}
}
