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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.l2journey.gameserver.model.options.Options;

/**
 * Unit tests for null-safe option func value parsing in {@link OptionData}.
 */
class OptionDataNullGuardTest
{
	@TempDir
	File tempDir;
	
	@BeforeEach
	@AfterEach
	void clearOptionMap() throws Exception
	{
		optionMap().clear();
	}
	
	@Test
	void parseFuncsSkipsMissingValAndKeepsValidFunc() throws Exception
	{
		final File xmlFile = new File(tempDir, "options.xml");
		Files.writeString(xmlFile.toPath(), """
			<?xml version="1.0" encoding="UTF-8"?>
			<list>
				<option id="1">
					<stats>
						<add stat="pAtk" />
						<add stat="pAtk" val="1.5" />
					</stats>
				</option>
			</list>
			""", StandardCharsets.UTF_8);
		
		final TestableOptionData data = new TestableOptionData();
		data.parseFile(xmlFile);
		
		final Options option = optionMap().get(1);
		assertNotNull(option);
		assertTrue(option.hasFuncs());
		assertEquals(1, option.getId());
	}
	
	@SuppressWarnings("unchecked")
	private static Map<Integer, Options> optionMap() throws Exception
	{
		final Field field = OptionData.class.getDeclaredField("OPTION_MAP");
		field.setAccessible(true);
		return (Map<Integer, Options>) field.get(null);
	}
	
	private static final class TestableOptionData extends OptionData
	{
		private TestableOptionData()
		{
			super();
		}
		
		@Override
		public synchronized void load()
		{
		}
		
		@Override
		public boolean isValidating()
		{
			return false;
		}
	}
}
