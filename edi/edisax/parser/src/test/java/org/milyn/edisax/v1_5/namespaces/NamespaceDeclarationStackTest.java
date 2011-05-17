/*
 * Milyn - Copyright (C) 2006 - 2011
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License (version 2.1) as published by the Free Software
 * Foundation.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See the GNU Lesser General Public License for more details:
 * http://www.gnu.org/licenses/lgpl.txt
 */
package org.milyn.edisax.v1_5.namespaces;

import java.util.ArrayList;
import java.util.List;

import javax.xml.XMLConstants;

import junit.framework.TestCase;

import org.milyn.edisax.EDIParser;
import org.milyn.edisax.util.NamespaceDeclarationStack;
import org.milyn.edisax.util.SchemaLocationResolver;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLFilterImpl;


public class NamespaceDeclarationStackTest extends TestCase implements SchemaLocationResolver {

	public static final class MockContentHandler extends DefaultHandler {
	
		public List<String> history = new ArrayList<String>();
		
		@Override
		public void startPrefixMapping(String prefix, String uri)
				throws SAXException {
			history.add("start:" + prefix + ":" + uri);
		}
		
		@Override
		public void endPrefixMapping(String prefix) throws SAXException {
			history.add("end:" + prefix);
		}
		
		
	}
	
	public void testSimpleMaping() throws Exception {
		MockContentHandler handler = new MockContentHandler();
		NamespaceDeclarationStack nds = new NamespaceDeclarationStack(new MockXMLReader(handler, null));
		Attributes a1 = nds.push("a", "nsa", null);
		nds.pop();
		assertEquals("[start:a:nsa, end:a]", handler.history.toString());
		assertEquals(1, a1.getLength());
		assertEquals("xmlns:a", a1.getQName(0));
		assertEquals("nsa", a1.getValue(0));
	}
	
	public void testSimpleMaping2() throws Exception {
		MockContentHandler handler = new MockContentHandler();
		NamespaceDeclarationStack nds = new NamespaceDeclarationStack(new MockXMLReader(handler, null));
		Attributes a1 = nds.push("a", "nsa", null);
		Attributes a2 = nds.push("a", "nsa", null);
		nds.pop();
		nds.pop();
		assertEquals("[start:a:nsa, end:a]", handler.history.toString());
		assertEquals(1, a1.getLength());
		assertEquals("xmlns:a", a1.getQName(0));
		assertEquals("nsa", a1.getValue(0));
		assertEquals(0, a2.getLength());
	}
	
	public void testTwoNamespacesMapping() throws Exception {
		MockContentHandler handler = new MockContentHandler();
		NamespaceDeclarationStack nds = new NamespaceDeclarationStack(new MockXMLReader(handler, null));
		Attributes a1 = nds.push("a", "nsa", null);
		Attributes a2 = nds.push("b", "nsb", null);
		nds.pop();
		nds.pop();
		assertEquals("[start:a:nsa, start:b:nsb, end:b, end:a]", handler.history.toString());
		assertEquals("xmlns:a=\"nsa\"", render(a1));
		assertEquals("xmlns:b=\"nsb\"", render(a2));
	}

	public void testTwoNamespacesMappingWithResolver() throws Exception {
		MockContentHandler handler = new MockContentHandler();
		NamespaceDeclarationStack nds = new NamespaceDeclarationStack(new MockXMLReader(handler, this));
		Attributes a1 = nds.push("a", "nsa", null);
		Attributes a2 = nds.push("b", "nsb", null);
		nds.pop();
		nds.pop();
		assertEquals("[start:a:nsa, start:b:nsb, end:b, end:a]", handler.history.toString());
		assertEquals("xmlns:a=\"nsa\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"nsa loc://nsa\"", render(a1));
		assertEquals("xmlns:b=\"nsb\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"nsb loc://nsb\"", render(a2));
	}

	
	public void testNamespacesWithAttributes() throws Exception {
		AttributesImpl attrs = new AttributesImpl();
		attrs.addAttribute(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "b", "xmlns:b", "CDATA", "nsb");
		attrs.addAttribute(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "c", "xmlns:c", "CDATA", "nsc");
		MockContentHandler handler = new MockContentHandler();
		NamespaceDeclarationStack nds = new NamespaceDeclarationStack(new MockXMLReader(handler, this));
		Attributes res = nds.push("a", "nsa", attrs);
		nds.push("b", "nsb", null);
		nds.pop();
		nds.pop();
		assertEquals("[start:b:nsb, start:c:nsc, start:a:nsa, end:a, end:c, end:b]", handler.history.toString());
		assertEquals("xmlns:b=\"nsb\" xmlns:c=\"nsc\" xmlns:a=\"nsa\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"nsb loc://nsb nsc loc://nsc nsa loc://nsa\"", render(res));
	}

    private class MockXMLReader extends XMLFilterImpl {

        private ContentHandler contentHandler;

        private SchemaLocationResolver resolver = null;
        
        private MockXMLReader(ContentHandler contentHandler, SchemaLocationResolver resolver) {
            this.contentHandler = contentHandler;
            this.resolver = resolver;
        }

        public void setContentHandler(ContentHandler contentHandler) {
            this.contentHandler = contentHandler;
        }

        public ContentHandler getContentHandler() {
            return contentHandler;
        }

        @Override
        public Object getProperty(String name)
        		throws SAXNotRecognizedException, SAXNotSupportedException {
        	if (EDIParser.SCHEMA_LOCATION_RESOLVER.equals(name)) {
        		return resolver;
        	}
        	return super.getProperty(name);
        }
    }

	public String getSchemaLocation(String namespace) {
		return "loc://" + namespace;
	}
	
	private String render(Attributes attrs) {
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < attrs.getLength(); i++) {
			String qname = attrs.getQName(i);
			String value = attrs.getValue(i);
			result.append(qname).append("=\"");
			result.append(value).append("\" ");
		}
		return result.toString().trim();
	}
}
