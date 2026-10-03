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
 * 
 * ---
 * 
 * Portions of this software are derived from the L2JMobius Project, 
 * shared under the MIT License. The original license terms are preserved where 
 * applicable..
 * 
 */
package com.l2journey.gameserver.scripting.java;

import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map.Entry;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for {@link JavaExecutionContext}.
 */
class JavaExecutionContextTest
{
	@TempDir
	Path _tempDir;
	
	@Test
	void compilesOrdinaryJava8StyleScript() throws Exception
	{
		final Path script = _tempDir.resolve("Java8Probe.java");
		Files.writeString(script, """
			public class Java8Probe
			{
				public static void main(String[] args)
				{
				}
			}
			""", StandardCharsets.UTF_8);
		
		final Entry<Path, Throwable> failure = new JavaExecutionContext().executeScript(script);
		assertNull(failure);
	}
	
	@Test
	void compilesJava25FlexibleConstructor() throws Exception
	{
		final Path script = _tempDir.resolve("Java25Probe.java");
		Files.writeString(script, """
			public class Java25Probe
			{
				public Java25Probe()
				{
					int value = 1;
					super();
					if (value != 1)
					{
						throw new IllegalStateException();
					}
				}
				
				public static void main(String[] args)
				{
					new Java25Probe();
				}
			}
			""", StandardCharsets.UTF_8);
		
		final Entry<Path, Throwable> failure = new JavaExecutionContext().executeScript(script);
		assertNull(failure);
	}
}
