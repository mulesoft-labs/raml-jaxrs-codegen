
package org.raml.jaxrs.codegen.core;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;

public abstract class Constants
{
    public static final Set<String> JAVA_KEYWORDS = Collections.unmodifiableSet(new HashSet<String>(
        Arrays.asList("abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class",
            "const", "continue", "default", "do", "double", "else", "enum", "extends", "false", "final",
            "finally", "float", "for", "goto", "if", "implements", "import", "instanceof", "int",
            "interface", "long", "native", "new", "null", "package", "private", "protected", "public",
            "return", "short", "static", "strictfp", "super", "switch", "synchronized", "this", "throw",
            "throws", "transient", "true", "try", "void", "volatile", "while")));

    @SuppressWarnings("unchecked")
    public static final List<Class<? extends Annotation>> JAXRS_HTTP_METHODS = Arrays.asList(DELETE.class,
        GET.class, HEAD.class, OPTIONS.class, POST.class, PUT.class);

    public static Locale DEFAULT_LOCALE = Locale.ENGLISH;

    public static final String RESPONSE_HEADER_WILDCARD_SYMBOL = "{?}";

    private Constants()
    {
        throw new UnsupportedOperationException();
    }
}
