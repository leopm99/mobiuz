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
package com.l2journey.gameserver.data.xml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.l2journey.Config;
import com.l2journey.gameserver.model.options.Options;

/**
 * Covers {@link OptionData#load()} when no option XML files are present.
 */
class OptionDataEmptyLoadTest
{
	@TempDir
	File tempDir;
	
	private File previousDatapackRoot;
	
	@BeforeEach
	void saveConfig()
	{
		previousDatapackRoot = Config.DATAPACK_ROOT;
	}
	
	@AfterEach
	void restoreConfig()
	{
		Config.DATAPACK_ROOT = previousDatapackRoot;
	}
	
	@Test
	void loadWithMissingOptionsDirectoryLeavesEmptyArray() throws Exception
	{
		Config.DATAPACK_ROOT = tempDir;
		
		final Constructor<OptionData> constructor = OptionData.class.getDeclaredConstructor();
		constructor.setAccessible(true);
		final OptionData data = constructor.newInstance();
		assertNotNull(data);
		
		final Field optionsField = OptionData.class.getDeclaredField("options");
		optionsField.setAccessible(true);
		final Options[] options = (Options[]) optionsField.get(null);
		assertNotNull(options);
		assertEquals(0, options.length);
	}
}
