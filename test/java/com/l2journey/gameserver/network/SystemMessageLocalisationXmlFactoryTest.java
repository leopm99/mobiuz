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
package com.l2journey.gameserver.network;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.l2journey.Config;

/**
 * Covers SystemMessageId / NpcStringId XmlFactory localisation load paths.
 */
class SystemMessageLocalisationXmlFactoryTest
{
	@TempDir
	File tempDir;
	
	private boolean previousMultilang;
	private List<String> previousAllowed;
	private String previousUserDir;
	
	@BeforeEach
	void saveConfig()
	{
		previousMultilang = Config.MULTILANG_ENABLE;
		previousAllowed = Config.MULTILANG_ALLOWED;
		previousUserDir = System.getProperty("user.dir");
	}
	
	@AfterEach
	void restoreConfig() throws Exception
	{
		Config.MULTILANG_ENABLE = previousMultilang;
		Config.MULTILANG_ALLOWED = previousAllowed;
		System.setProperty("user.dir", previousUserDir);
	}
	
	@Test
	void loadLocalisationsUsesXmlFactory() throws Exception
	{
		System.setProperty("user.dir", tempDir.getAbsolutePath());
		Config.MULTILANG_ENABLE = true;
		Config.MULTILANG_ALLOWED = List.of("en");
		
		final File sm = new File(tempDir, "data/lang/en/SystemMessageLocalisation.xml");
		Files.createDirectories(sm.getParentFile().toPath());
		Files.writeString(sm.toPath(), "<?xml version=\"1.0\" encoding=\"UTF-8\"?><list/>", StandardCharsets.UTF_8);
		
		final File ns = new File(tempDir, "data/lang/en/NpcStringLocalisation.xml");
		Files.writeString(ns.toPath(), "<?xml version=\"1.0\" encoding=\"UTF-8\"?><list/>", StandardCharsets.UTF_8);
		
		assertDoesNotThrow(SystemMessageId::loadLocalisations);
		assertDoesNotThrow(NpcStringId::loadLocalisations);
	}
}
