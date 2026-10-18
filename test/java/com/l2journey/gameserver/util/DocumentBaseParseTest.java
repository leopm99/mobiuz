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
package com.l2journey.gameserver.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Document;

import com.l2journey.gameserver.model.StatSet;

/**
 * Unit tests for {@link DocumentBase#parse()}.
 */
class DocumentBaseParseTest
{
	@TempDir
	File tempDir;
	
	@Test
	void parseLoadsXmlThroughSecureFactory() throws Exception
	{
		final File xmlFile = new File(tempDir, "doc.xml");
		Files.writeString(xmlFile.toPath(), "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root attr=\"ok\"/>", StandardCharsets.UTF_8);
		
		final AtomicReference<Document> parsed = new AtomicReference<>();
		final DocumentBase documentBase = new DocumentBase(xmlFile)
		{
			@Override
			protected void parseDocument(Document document)
			{
				parsed.set(document);
			}
			
			@Override
			protected StatSet getStatSet()
			{
				return new StatSet();
			}
			
			@Override
			protected String getTableValue(String name)
			{
				return null;
			}
			
			@Override
			protected String getTableValue(String name, int idx)
			{
				return null;
			}
		};
		
		final Document document = documentBase.parse();
		assertNotNull(document);
		assertNotNull(parsed.get());
		assertEquals("root", document.getDocumentElement().getNodeName());
		assertEquals("ok", document.getDocumentElement().getAttribute("attr"));
	}
}
