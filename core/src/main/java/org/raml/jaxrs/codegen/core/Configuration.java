/*
 * Copyright (c) MuleSoft, Inc.
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

public class Configuration
{
    private File outputDirectory;
    private String basePackageName;
    private boolean useJsr303Annotations = false;

    public File getOutputDirectory()
    {
        return outputDirectory;
    }

    public void setOutputDirectory(final File outputDirectory)
    {
        this.outputDirectory = outputDirectory;
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
}
