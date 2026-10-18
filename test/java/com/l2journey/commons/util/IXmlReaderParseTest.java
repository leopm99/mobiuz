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

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Document;

import com.l2journey.Config;

/**
 * Unit tests for {@link IXmlReader#parseFile(File)}.
 */
class IXmlReaderParseTest
{
	@TempDir
	File tempDir;
	
	private boolean previousThreadsForLoading;
	
	@BeforeEach
	void saveConfig()
	{
		previousThreadsForLoading = Config.THREADS_FOR_LOADING;
	}
	
	@AfterEach
	void restoreConfig()
	{
		Config.THREADS_FOR_LOADING = previousThreadsForLoading;
	}
	
	@Test
	void parseFileLoadsDocumentThroughSecureFactory() throws Exception
	{
		final File xmlFile = new File(tempDir, "sample.xml");
		Files.writeString(xmlFile.toPath(), "<?xml version=\"1.0\" encoding=\"UTF-8\"?><item name=\"sword\"/>", StandardCharsets.UTF_8);
		
		final RecordingXmlReader reader = new RecordingXmlReader();
		reader.parseFile(xmlFile);
		
		assertNotNull(reader.document);
		assertEquals("item", reader.document.getDocumentElement().getNodeName());
		assertEquals("sword", reader.document.getDocumentElement().getAttribute("name"));
		assertEquals(xmlFile, reader.file);
	}
	
	@Test
	void parseDirectoryWithThreadsForLoadingClosesExecutor() throws Exception
	{
		Files.writeString(new File(tempDir, "a.xml").toPath(), "<?xml version=\"1.0\" encoding=\"UTF-8\"?><a/>", StandardCharsets.UTF_8);
		Files.writeString(new File(tempDir, "b.xml").toPath(), "<?xml version=\"1.0\" encoding=\"UTF-8\"?><b/>", StandardCharsets.UTF_8);
		
		Config.THREADS_FOR_LOADING = true;
		final CountingXmlReader reader = new CountingXmlReader();
		assertTrue(reader.parseDirectory(tempDir, false));
		assertEquals(2, reader.parsedCount.get());
	}
	
	private static final class RecordingXmlReader implements IXmlReader
	{
		private Document document;
		private File file;
		
		@Override
		public void load()
		{
		}
		
		@Override
		public void parseDocument(Document document, File file)
		{
			this.document = document;
			this.file = file;
		}
		
		@Override
		public boolean isValidating()
		{
			return false;
		}
	}
	
	private static final class CountingXmlReader implements IXmlReader
	{
		private final AtomicInteger parsedCount = new AtomicInteger();
		
		@Override
		public void load()
		{
		}
		
		@Override
		public void parseDocument(Document document, File file)
		{
			parsedCount.incrementAndGet();
		}
		
		@Override
		public boolean isValidating()
		{
			return false;
		}
	}
}
