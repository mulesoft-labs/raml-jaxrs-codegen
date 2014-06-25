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
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.jsonschema2pojo.AnnotationStyle;
import org.jsonschema2pojo.DefaultGenerationConfig;
import org.jsonschema2pojo.GenerationConfig;

/*
 * Copyright 2013 (c) MuleSoft, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */
public class Configuration
{
  public enum JaxrsVersion
  {
    JAXRS_1_1("1.1"), JAXRS_2_0("2.0");

    private final String alias;

    private JaxrsVersion(final String alias)
    {
      this.alias = alias;
    }

    public static JaxrsVersion fromAlias(final String alias)
    {
      final List<String> supportedAliases = new ArrayList<String>();

      for (final JaxrsVersion jaxrsVersion : JaxrsVersion.values())
      {
        if (jaxrsVersion.alias.equals(alias))
        {
          return jaxrsVersion;
        }
        supportedAliases.add(jaxrsVersion.alias);
      }

      throw new IllegalArgumentException(alias + " is not a supported JAX-RS version ("
          + StringUtils.join(supportedAliases, ',') + ")");
    }
  };

  private File outputDirectory;
  private JaxrsVersion jaxrsVersion = JaxrsVersion.JAXRS_1_1;
  private String basePackageName;
  private boolean responseWrapperAsInnerClass;
  private boolean hateoasResourceSupport;
  private boolean useJsr303Annotations = false;
  private final List<String> externalVisitors = new ArrayList<String>();
  private AnnotationStyle jsonMapper = AnnotationStyle.JACKSON1;
  private String resourcePostfix;

  public GenerationConfig createJsonSchemaGenerationConfig()
  {
    return new DefaultGenerationConfig()
    {
      @Override
      public AnnotationStyle getAnnotationStyle()
      {
        return super.getAnnotationStyle();
      }

      @Override
      public boolean isIncludeJsr303Annotations()
      {
        return useJsr303Annotations;
      }

      @Override
      public boolean isGenerateBuilders()
      {
        return true;
      }

      @Override
      public boolean isIncludeHashcodeAndEquals()
      {
        return false;
      }

      @Override
      public boolean isIncludeToString()
      {
        return false;
      }
      @Override
      public boolean isUseJodaDates() {
        return true;
      }
    };
  }

  public File getOutputDirectory()
  {
    return outputDirectory;
  }

  public void setOutputDirectory(final File outputDirectory)
  {
    this.outputDirectory = outputDirectory;
  }

  public JaxrsVersion getJaxrsVersion()
  {
    return jaxrsVersion;
  }

  public void setJaxrsVersion(final JaxrsVersion jaxrsVersion)
  {
    this.jaxrsVersion = jaxrsVersion;
  }

  public String getBasePackageName()
  {
    return basePackageName;
  }

  public void setBasePackageName(final String basePackageName)
  {
    this.basePackageName = basePackageName;
  }

  public boolean isUseJsr303Annotations()
  {
    return useJsr303Annotations;
  }

  public void setUseJsr303Annotations(final boolean useJsr303Annotations)
  {
    this.useJsr303Annotations = useJsr303Annotations;
  }

  public AnnotationStyle getJsonMapper()
  {
    return jsonMapper;
  }

  public void setJsonMapper(final AnnotationStyle jsonMapper)
  {
    this.jsonMapper = jsonMapper;
  }

  public String getModelPackage()
  {
    return getBasePackageName() + ".model";
  }

  public String getSupportPackage()
  {
    return getBasePackageName() + ".support";
  }

  /**
   * @return the responseWrapperAsInnerClass
   */
  public boolean isResponseWrapperAsInnerClass() {
    return responseWrapperAsInnerClass;
  }

  /**
   * @param responseWrapperAsInnerClass the responseWrapperAsInnerClass to set
   */
  public void setResponseWrapperAsInnerClass(boolean responseWrapperAsInnerClass) {
    this.responseWrapperAsInnerClass = responseWrapperAsInnerClass;
  }

  /**
   * @return the externalCodeGenerators
   */
  public List<String> getExternalCodeGenerators() {
    return externalVisitors;
  }

  /**
   * @return the hateoasResourceSupport
   */
  public boolean isHateoasResourceSupport() {
    return hateoasResourceSupport;
  }

  /**
   * @param hateoasResourceSupport the hateoasResourceSupport to set
   */
  public void setHateoasResourceSupport(boolean hateoasResourceSupport) {
    this.hateoasResourceSupport = hateoasResourceSupport;
  }

  /**
   * @return the resourcePostfix
   */
  public String getResourcePostfix() {
    return resourcePostfix;
  }

  /**
   * @param resourcePostfix the resourcePostfix to set
   */
  public void setResourcePostfix(String resourcePostfix) {
    this.resourcePostfix = resourcePostfix;
  }
}
