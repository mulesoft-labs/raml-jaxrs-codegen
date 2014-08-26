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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.jsonschema2pojo.AnnotationStyle;
import org.raml.jaxrs.codegen.core.Configuration.JaxrsVersion;

public class Launcher {

	public static void main(String[] args) {
		
		Map<String,String> argMap = createArgMap(args);
		
		Configuration configuration = createConfiguration(argMap);
		
		boolean removeOldOutput = false;		
		String removeOldOutputStringValue = argMap.get("removeOldOutput");
		if(removeOldOutputStringValue!=null){
			removeOldOutput = Boolean.parseBoolean(removeOldOutputStringValue);
		}
		
		Collection<File> ramlFiles = getRamlFiles(argMap);
		if(ramlFiles.isEmpty()){
			return;
		}
		
		if (removeOldOutput)
        {
			try {
				FileUtils.cleanDirectory(configuration.getOutputDirectory());
			} catch (IOException e) {
				e.printStackTrace();
			}            
        }
		final Generator generator = new Generator();
		for (final File ramlFile : ramlFiles)
        {
            try {
				generator.run(new FileReader(ramlFile), configuration);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
        }
	}

	private static Collection<File> getRamlFiles(Map<String, String> argMap) {
		
		String sourcePaths = argMap.get("sourcePaths");
		String sourceDirectoryPath = argMap.get("sourceDirectory");
		if ( !isEmptyString(sourcePaths) )
		{
			List<File> sourceFiles = new ArrayList<File>();
			String[] split = sourcePaths.split(System.getProperty("path.separator"));            
            for(String str : split){
            	sourceFiles.add(new File(str));
            }
            return sourceFiles;
		}
		else{
			File sourceDirectory = new File(sourceDirectoryPath);
			if (!sourceDirectory.isDirectory()) {
                throw new RuntimeException("The provided path doesn't refer to a valid directory: "+ sourceDirectory);
            }
            return FileUtils.listFiles(sourceDirectory, new String[]{"raml", "yaml"}, false);
        }
	}

	private static Configuration createConfiguration(Map<String, String> argMap) {
		
		Configuration configuration = new Configuration();
		
		File rootDirectory = new File(System.getProperty("user.dir"));
		File outputDirectory = new File(rootDirectory,"generated-sources/raml-jaxrs");
		File sourceDirectory = new File(rootDirectory,"src/main/raml");
		
		String basePackageName = null;
		String jaxrsVersion = "1.1";
		boolean useJsr303Annotations = false;		
		boolean useLongIntegers = false;
		String jsonMapper = "jackson1";
		
		
		for( Map.Entry<String,String> entry : argMap.entrySet() ){
			
			String argName = entry.getKey();			
			String argValue = entry.getValue();
			
			if(argName.equals("outputDirectory")){				
				outputDirectory = new File(argValue);
			}
			else if(argName.equals("sourceDirectory")){
				sourceDirectory = new File(argValue);
			}
			else if(argName.equals("jaxrsVersion")){
				jaxrsVersion = argValue;
			}
			else if(argName.equals("basePackageName")){
				basePackageName = argValue;
			}
			else if(argName.equals("useJsr303Annotations")){
				useJsr303Annotations = Boolean.parseBoolean(argValue);
			}
			else if(argName.equals("jsonMapper")){
				jsonMapper = argValue;
			}
			else if(argName.equals("useLongIntegers")){
				useLongIntegers = Boolean.parseBoolean(argValue);
			}
		}
		if(basePackageName==null){
			throw new RuntimeException("Base package must be specified.");
		}
		if(!outputDirectory.isDirectory()){
			throw new RuntimeException("Output destination must be a directory.");
		}
		
		configuration.setBasePackageName(basePackageName);
        configuration.setJaxrsVersion(JaxrsVersion.fromAlias(jaxrsVersion));
        configuration.setOutputDirectory(outputDirectory);
        configuration.setUseJsr303Annotations(useJsr303Annotations);
        configuration.setUseLongIntegers(useLongIntegers);
        configuration.setJsonMapper(AnnotationStyle.valueOf(jsonMapper.toUpperCase()));
        configuration.setSourceDirectory(sourceDirectory);
        
        return configuration;
	}

	private static Map<String, String> createArgMap(String[] args) {
		
		HashMap<String,String> map = new HashMap<String, String>(); 
		for(int i = 0 ; i < args.length ; i++ ){
					
			String argName = args[i];
			if(argName.startsWith("-")){
				argName = argName.substring(1);			
				if(i+1 < args.length)
				{
					String argValue = args[i+1];
					if(!argValue.startsWith("-")){					
						map.put(argName, argValue);
						i++;
					}
				}
			}
		}
		return map;
	}

	private static boolean isEmptyString(String str) {		
		
		return str == null || str.trim().isEmpty();
	}
}
