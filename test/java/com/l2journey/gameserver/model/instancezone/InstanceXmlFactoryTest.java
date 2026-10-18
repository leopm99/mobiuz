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
package com.l2journey.gameserver.model.instancezone;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.l2journey.gameserver.managers.InstanceManager;

/**
 * Covers Instance XmlFactory call site.
 */
class InstanceXmlFactoryTest
{
	private static final Path XML = Path.of("data", "instances", "wave-a-instance-test.xml");
	private boolean xmlExisted;
	
	@BeforeEach
	void createXml() throws Exception
	{
		xmlExisted = Files.exists(XML);
		Files.createDirectories(XML.getParent());
		Files.writeString(XML, "<?xml version=\"1.0\" encoding=\"UTF-8\"?><instance id=\"1\"/>", StandardCharsets.UTF_8);
	}
	
	@AfterEach
	void cleanupXml() throws Exception
	{
		if (!xmlExisted)
		{
			Files.deleteIfExists(XML);
		}
	}
	
	@Test
	void loadInstanceTemplateUsesSecureFactory()
	{
		final InstanceManager instanceManager = mock(InstanceManager.class);
		when(instanceManager.getInstanceTemplateFileName(anyInt())).thenReturn("wave-a-instance-test.xml");
		
		try (MockedStatic<InstanceManager> managers = mockStatic(InstanceManager.class))
		{
			managers.when(InstanceManager::getInstance).thenReturn(instanceManager);
			final Instance instance = new Instance(1);
			assertDoesNotThrow(() -> instance.loadInstanceTemplate(1));
		}
	}
}
