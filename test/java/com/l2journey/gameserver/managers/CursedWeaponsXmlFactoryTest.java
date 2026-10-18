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
package com.l2journey.gameserver.managers;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.l2journey.Config;

/**
 * Covers CursedWeaponsManager XmlFactory call site.
 */
class CursedWeaponsXmlFactoryTest
{
	@TempDir
	File tempDir;
	
	private File previousDatapackRoot;
	private boolean previousAllow;
	
	@BeforeEach
	void saveConfig()
	{
		previousDatapackRoot = Config.DATAPACK_ROOT;
		previousAllow = Config.ALLOW_CURSED_WEAPONS;
	}
	
	@AfterEach
	void restoreConfig()
	{
		Config.DATAPACK_ROOT = previousDatapackRoot;
		Config.ALLOW_CURSED_WEAPONS = previousAllow;
	}
	
	@Test
	void loadUsesSecureFactory() throws Exception
	{
		Config.DATAPACK_ROOT = tempDir;
		Config.ALLOW_CURSED_WEAPONS = true;
		final File xml = new File(tempDir, "data/CursedWeapons.xml");
		Files.createDirectories(xml.getParentFile().toPath());
		Files.writeString(xml.toPath(), "<?xml version=\"1.0\" encoding=\"UTF-8\"?><list/>", StandardCharsets.UTF_8);
		
		final Constructor<CursedWeaponsManager> constructor = CursedWeaponsManager.class.getDeclaredConstructor();
		constructor.setAccessible(true);
		final CursedWeaponsManager manager = constructor.newInstance();
		assertNotNull(manager);
		final Method load = CursedWeaponsManager.class.getDeclaredMethod("load");
		load.setAccessible(true);
		assertDoesNotThrow(() -> load.invoke(manager));
	}
}
