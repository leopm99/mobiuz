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
package com.l2journey.gameserver.data;

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
 * Covers AugmentationData XmlFactory call sites.
 */
class AugmentationDataXmlFactoryTest
{
	@TempDir
	File tempDir;
	
	private File previousDatapackRoot;
	private boolean previousRetail;
	private boolean previousAccessory;
	
	@BeforeEach
	void saveConfig()
	{
		previousDatapackRoot = Config.DATAPACK_ROOT;
		previousRetail = Config.RETAIL_LIKE_AUGMENTATION;
		previousAccessory = Config.RETAIL_LIKE_AUGMENTATION_ACCESSORY;
	}
	
	@AfterEach
	void restoreConfig()
	{
		Config.DATAPACK_ROOT = previousDatapackRoot;
		Config.RETAIL_LIKE_AUGMENTATION = previousRetail;
		Config.RETAIL_LIKE_AUGMENTATION_ACCESSORY = previousAccessory;
	}
	
	@Test
	void loadUsesXmlFactoryForSkillmapPath() throws Exception
	{
		Config.DATAPACK_ROOT = tempDir;
		Config.RETAIL_LIKE_AUGMENTATION = false;
		Config.RETAIL_LIKE_AUGMENTATION_ACCESSORY = false;
		final File skillmap = new File(tempDir, "data/stats/augmentation/augmentation_skillmap.xml");
		Files.createDirectories(skillmap.getParentFile().toPath());
		Files.writeString(skillmap.toPath(), "<?xml version=\"1.0\" encoding=\"UTF-8\"?><list/>", StandardCharsets.UTF_8);
		
		final AugmentationData data = newInstance();
		assertNotNull(data);
		assertDoesNotThrow(() -> invokeLoad(data));
	}
	
	@Test
	void loadUsesXmlFactoryForRetailChancePaths() throws Exception
	{
		Config.DATAPACK_ROOT = tempDir;
		Config.RETAIL_LIKE_AUGMENTATION = true;
		Config.RETAIL_LIKE_AUGMENTATION_ACCESSORY = true;
		final File dir = new File(tempDir, "data/stats/augmentation");
		Files.createDirectories(dir.toPath());
		Files.writeString(new File(dir, "retailchances.xml").toPath(), "<?xml version=\"1.0\" encoding=\"UTF-8\"?><list/>", StandardCharsets.UTF_8);
		Files.writeString(new File(dir, "retailchances_accessory.xml").toPath(), "<?xml version=\"1.0\" encoding=\"UTF-8\"?><list/>", StandardCharsets.UTF_8);
		
		final AugmentationData data = newInstance();
		assertNotNull(data);
		assertDoesNotThrow(() -> invokeLoad(data));
	}
	
	private static AugmentationData newInstance() throws Exception
	{
		final Constructor<AugmentationData> constructor = AugmentationData.class.getDeclaredConstructor();
		constructor.setAccessible(true);
		return constructor.newInstance();
	}
	
	private static void invokeLoad(AugmentationData data) throws Exception
	{
		final Method load = AugmentationData.class.getDeclaredMethod("load");
		load.setAccessible(true);
		load.invoke(data);
	}
}
