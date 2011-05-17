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
package org.milyn.edisax.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.xml.XMLConstants;

import org.milyn.edisax.EDIParser;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.AttributesImpl;

/**
 * This class is responsible for managing namespace prefix mapping stack.
 * Limitation - not supported : re-defining a namespace prefix (child element
 * re-define namespace prefix defined by parent element)
 * 
 * @author zubairov
 */
public class NamespaceDeclarationStack {

	private final XMLReader xmlReader;
	private final Stack<List<String>> nsStack = new Stack<List<String>>();

	public NamespaceDeclarationStack(XMLReader xmlReader) {
		this.xmlReader = xmlReader;
	}

	/**
	 * Pop element out of the namespace declaration stack and notifying
	 * {@link ContentHandler} if required
	 * 
	 * @throws SAXException
	 */
	public void pop() throws SAXException {
		List<String> pop = nsStack.pop();
		Collections.reverse(pop);
		for (String ns : pop) {
			xmlReader.getContentHandler().endPrefixMapping(ns);
		}
	}

	/**
	 * Pushing a new element to the stack
	 * 
	 * @param attributes
	 *            optional attributes or null, single element could declare
	 *            multiple namespaces
	 * @return modified attributes declaration in case additional prefix mapping
	 *         should be included
	 * @throws SAXException
	 */
	public Attributes push(String prefix, String namespace,
			Attributes attributes) throws SAXException {
		List<String> namespaces = new ArrayList<String>();
		// Volatile array
		Map<String, String> nsToURI = new HashMap<String, String>();
		AttributesImpl attrs;
		if (attributes != null) {
			attrs = new AttributesImpl(attributes);
		} else {
			attrs = new AttributesImpl();
		}
		// Gather namespace declarations from the attributes
		for (int i = 0; i < attrs.getLength(); i++) {
			String qname = attrs.getQName(i);
			if (qname != null
					&& qname.startsWith(XMLConstants.XMLNS_ATTRIBUTE + ":")) {
				// Add prefix to the list of declared namespaces
				namespaces.add(attrs.getLocalName(i));
				nsToURI.put(attrs.getLocalName(i), attrs.getValue(i));
			}
		}
		if (!prefixAlreadyDeclared(prefix)) {
			// Add a new attribute to the list of attributes
			attrs.addAttribute(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, prefix,
					"xmlns:" + prefix, "CDATA", namespace);
			namespaces.add(prefix);
			nsToURI.put(prefix, namespace);
		}
		nsStack.push(namespaces);
		// Now call start prefixes if namespaces are not empty
		for (String nsPrefix : namespaces) {
			String uri = nsToURI.get(nsPrefix);
			xmlReader.getContentHandler().startPrefixMapping(nsPrefix, uri);
		}
		generateSchemaLocationAttribute(nsToURI, attrs);
		return attrs;
	}

	/**
	 * This method generates schema location if
	 * {@link EDIParser#SCHEMA_LOCATION_RESOLVER} is set
	 * 
	 * @param namespace
	 * @param namespaces
	 * @param nsToURI
	 * @param attrs
	 * @throws SAXNotRecognizedException
	 * @throws SAXNotSupportedException
	 */
	private void generateSchemaLocationAttribute(Map<String, String> nsToURI,
			AttributesImpl attrs) throws SAXNotRecognizedException,
			SAXNotSupportedException {
		// Now check the addition of xsi:schemaLocation attribute
		SchemaLocationResolver resolver = null;
		try {
			resolver = (SchemaLocationResolver) xmlReader
					.getProperty(EDIParser.SCHEMA_LOCATION_RESOLVER);
		} catch (SAXNotRecognizedException e) {
			// Ignore
		}
		if (resolver != null && !nsToURI.isEmpty()) {
			// Add xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance
			attrs.addAttribute(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xsi",
					"xmlns:xsi", "CDATA",
					XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI);
			StringBuffer value = new StringBuffer();
			for (String nsPrefix : nsToURI.keySet()) {
				String uri = nsToURI.get(nsPrefix);
				String location = resolver.getSchemaLocation(uri);
				if (location != null) {
					value.append(uri).append(" ");
					value.append(location).append(" ");
				}
			}
			if (value.length() > 0) {
				attrs.addAttribute(XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI,
						"schemaLocation", "xsi:schemaLocation", "CDATA",
						value.toString().trim());
			}
		}
	}

	/**
	 * This method returns true if namespace with given prefix was already
	 * declared higher the stack
	 * 
	 * @param prefix
	 * @return
	 */
	private boolean prefixAlreadyDeclared(String prefix) {
		for (List<String> set : nsStack) {
			if (set.contains(prefix)) {
				return true;
			}
		}
		return false;
	}

}
