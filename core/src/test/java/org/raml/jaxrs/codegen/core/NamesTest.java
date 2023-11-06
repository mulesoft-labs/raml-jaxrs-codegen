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
