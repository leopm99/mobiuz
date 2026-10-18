/*
 * Copyright (c) 2025 L2Journey Project
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.l2journey.commons.util;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

/**
 * Factory helpers that create XML parsers with secure processing enabled.
 * <p>
 * External DTDs are blocked. Local {@code file:} schemas remain allowed so datapack XSD
 * validation continues to work under {@link XMLConstants#FEATURE_SECURE_PROCESSING}.
 */
public final class XmlFactory
{
	private static final String EXTERNAL_ACCESS_NONE = "";
	private static final String EXTERNAL_SCHEMA_FILE_ONLY = "file";
	
	private XmlFactory()
	{
	}
	
	/**
	 * @return a {@link DocumentBuilderFactory} with secure processing enabled
	 */
	public static DocumentBuilderFactory newDocumentBuilderFactory()
	{
		try
		{
			final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, EXTERNAL_ACCESS_NONE);
			factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, EXTERNAL_SCHEMA_FILE_ONLY);
			return factory;
		}
		catch (ParserConfigurationException e)
		{
			throw new IllegalStateException("Unable to create a secure DocumentBuilderFactory", e);
		}
	}
	
	/**
	 * @return a {@link SAXParserFactory} with secure processing enabled
	 */
	public static SAXParserFactory newSaxParserFactory()
	{
		try
		{
			final SAXParserFactory factory = SAXParserFactory.newInstance();
			factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			return factory;
		}
		catch (Exception e)
		{
			throw new IllegalStateException("Unable to create a secure SAXParserFactory", e);
		}
	}
}
