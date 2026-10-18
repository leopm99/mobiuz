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
package com.l2journey;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@code Config.loadChatFilter()}.
 */
class ConfigChatFilterTest
{
	private static final Path CHAT_FILTER_PATH = Path.of("config", "security", "chatfilter.txt");
	
	private List<String> previousFilterList;
	private byte[] previousFileBytes;
	private boolean hadFile;
	
	@BeforeEach
	void saveState() throws Exception
	{
		previousFilterList = Config.FILTER_LIST == null ? null : new ArrayList<>(Config.FILTER_LIST);
		hadFile = Files.isRegularFile(CHAT_FILTER_PATH);
		if (hadFile)
		{
			previousFileBytes = Files.readAllBytes(CHAT_FILTER_PATH);
		}
		Files.createDirectories(CHAT_FILTER_PATH.getParent());
		Files.writeString(CHAT_FILTER_PATH, "# comment\n\nbadword\n  spaced  \n", StandardCharsets.UTF_8);
	}
	
	@AfterEach
	void restoreState() throws Exception
	{
		Config.FILTER_LIST = previousFilterList;
		if (hadFile)
		{
			Files.write(CHAT_FILTER_PATH, previousFileBytes);
		}
		else
		{
			Files.deleteIfExists(CHAT_FILTER_PATH);
		}
	}
	
	@Test
	void loadChatFilterReadsWordsAndSkipsComments() throws Exception
	{
		final Method method = Config.class.getDeclaredMethod("loadChatFilter");
		method.setAccessible(true);
		method.invoke(null);
		
		assertEquals(List.of("badword", "spaced"), Config.FILTER_LIST);
		assertTrue(Config.FILTER_LIST.stream().noneMatch(word -> word.startsWith("#")));
	}
}
