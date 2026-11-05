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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for null-safe category id parsing in {@link UIData}.
 */
class UIDataNullGuardTest
{
	@TempDir
	File tempDir;
	
	@Test
	void parseCategorySkipsMissingIdAndKeepsValidCategory() throws Exception
	{
		final File xmlFile = new File(tempDir, "ui.xml");
		Files.writeString(xmlFile.toPath(), """
			<?xml version="1.0" encoding="UTF-8"?>
			<list>
				<category>
					<commands>
						<cmd>99</cmd>
					</commands>
				</category>
				<category id="1">
					<commands>
						<cmd>71</cmd>
					</commands>
				</category>
			</list>
			""", StandardCharsets.UTF_8);
		
		final TestableUIData data = new TestableUIData();
		data.parseFile(xmlFile);
		
		assertFalse(data.getCategories().containsKey(0));
		assertTrue(data.getCategories().containsKey(1));
		final List<Integer> commands = data.getCategories().get(1);
		assertEquals(1, commands.size());
		assertEquals(71, commands.get(0).intValue());
	}
	
	private static final class TestableUIData extends UIData
	{
		private TestableUIData()
		{
			super();
		}
		
		@Override
		public void load()
		{
		}
		
		@Override
		public boolean isValidating()
		{
			return false;
		}
	}
}
