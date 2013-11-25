
package org.raml.jaxrs.codegen.core.model;

import java.io.File;

public interface JavaRenderable
{
    final static String SUPPORT_CLASSES_PACKAGE = "org.raml.jaxrs.support";

    void toJava(final File outputDirectory) throws Exception;
}
