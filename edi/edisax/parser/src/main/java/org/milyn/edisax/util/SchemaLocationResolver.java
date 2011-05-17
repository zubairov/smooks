package org.milyn.edisax.util;

import org.milyn.edisax.EDIParser;
import org.xml.sax.XMLReader;

/**
 * This interface could be registered as property
 * {@link EDIParser#SCHEMA_LOCATION_RESOLVER} in the
 * {@link XMLReader#setProperty(String, Object)} so that
 * {@link NamespaceDeclarationStack} will generate xsi:schemaLocation
 * attributes every time it adds a namespace declaration
 * 
 * @author zubairov
 * 
 */
public interface SchemaLocationResolver {

	/**
	 * This method should return a schema location or null
	 * 
	 * @param namespace
	 * @return location of the schema or null
	 */
	public String getSchemaLocation(String namespace);

}