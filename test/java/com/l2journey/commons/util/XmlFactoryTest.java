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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParserFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Unit tests for {@link XmlFactory}.
 */
class XmlFactoryTest
{
	private static final String BENIGN_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root id=\"1\"/>";
	
	@Test
	void documentBuilderFactoryEnablesSecureProcessing() throws Exception
	{
		final DocumentBuilderFactory factory = XmlFactory.newDocumentBuilderFactory();
		assertTrue(factory.getFeature(XMLConstants.FEATURE_SECURE_PROCESSING));
		assertEquals("", factory.getAttribute(XMLConstants.ACCESS_EXTERNAL_DTD));
		assertEquals("file", factory.getAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA));
		
		final Document document = factory.newDocumentBuilder().parse(new ByteArrayInputStream(BENIGN_XML.getBytes(StandardCharsets.UTF_8)));
		assertNotNull(document);
		assertEquals("root", document.getDocumentElement().getNodeName());
		assertEquals("1", document.getDocumentElement().getAttribute("id"));
	}
	
	@Test
	void saxParserFactoryEnablesSecureProcessing() throws Exception
	{
		final SAXParserFactory factory = XmlFactory.newSaxParserFactory();
		assertTrue(factory.getFeature(XMLConstants.FEATURE_SECURE_PROCESSING));
		
		factory.newSAXParser().parse(new ByteArrayInputStream(BENIGN_XML.getBytes(StandardCharsets.UTF_8)), new DefaultHandler());
	}
	
	@Test
	void utilityClassHasPrivateConstructor() throws Exception
	{
		final Constructor<XmlFactory> constructor = XmlFactory.class.getDeclaredConstructor();
		assertTrue(java.lang.reflect.Modifier.isPrivate(constructor.getModifiers()));
		constructor.setAccessible(true);
		assertNotNull(constructor.newInstance());
	}
}
