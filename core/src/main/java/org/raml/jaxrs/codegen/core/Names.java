
package org.raml.jaxrs.codegen.core;

import static org.apache.commons.lang.StringUtils.defaultIfBlank;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.left;
import static org.apache.commons.lang.StringUtils.uncapitalize;
import static org.apache.commons.lang.WordUtils.capitalize;
import static org.apache.commons.lang.math.NumberUtils.isDigits;

import org.raml.model.Action;
import org.raml.model.Resource;

public class Names
{
    public static String buildResourceInterfaceName(final Resource resource)
    {
        final String resourceInterfaceName = buildJavaFriendlyName(defaultIfBlank(resource.getDisplayName(),
            resource.getRelativeUri()));
        return isBlank(resourceInterfaceName) ? "Root" : resourceInterfaceName;
    }

    public static String buildResourceMethodBaseName(final Action action)
    {
        return buildJavaFriendlyName(action.getResource().getUri().replace("{", " By "));
    }

    public static String buildVariableName(final String source)
    {
        final String name = uncapitalize(buildJavaFriendlyName(source));

        return Constants.JAVA_KEYWORDS.contains(name) ? "$" + name : name;
    }

    public static String buildJavaFriendlyName(final String source)
    {
        final String baseName = source.replaceAll("[\\W_]", " ");

        String friendlyName = capitalize(baseName).replaceAll("[\\W_]", "");

        if (isDigits(left(friendlyName, 1)))
        {
            friendlyName = "_" + friendlyName;
        }

        return friendlyName;
    }

    private Names()
    {
        throw new UnsupportedOperationException();
    }
}
