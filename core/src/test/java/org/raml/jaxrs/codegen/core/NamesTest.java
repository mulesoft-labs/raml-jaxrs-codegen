package org.raml.jaxrs.codegen.core;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.raml.model.MimeType;

public class NamesTest {


    @Test
    public void testResponseMethodName() {
        assertEquals("octetstreamOK", Names.buildResponseMethodName(200, new MimeType("binary/octet-stream")));
    }
    
    @Test
    public void testMimeType() {
        assertEquals("octetstream", Names.getShortMimeType(new MimeType("binary/octet-stream")));
    }

}
