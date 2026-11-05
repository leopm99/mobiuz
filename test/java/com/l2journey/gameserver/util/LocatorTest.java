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
package com.l2journey.gameserver.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Characterization tests for {@link Locator}.
 */
class LocatorTest
{
	@Test
	void getClassSourceReturnsDirectoryContainingClassFile()
	{
		final File source = Locator.getClassSource(Locator.class);
		assertNotNull(source);
		final File classFile = new File(source, "com/l2journey/gameserver/util/Locator.class");
		assertTrue(classFile.exists(), classFile.getAbsolutePath());
	}
	
	@Test
	void getResourceSourceHandlesNullLoaderMissingResourceAndJar() throws Exception
	{
		assertNull(Locator.getResourceSource(null, "does/not/exist.class"));
		
		final File jar = new File("dist/libs/slf4j-api-2.0.17.jar");
		assertTrue(jar.isFile(), jar.getAbsolutePath());
		try (URLClassLoader loader = new URLClassLoader(new URL[]
		{
			jar.toURI().toURL()
		}, null))
		{
			final File source = Locator.getResourceSource(loader, "org/slf4j/Logger.class");
			assertNotNull(source);
			assertEquals(jar.getCanonicalFile(), source.getCanonicalFile());
		}
	}
	
	@Test
	void fromURIRoundTripsTempFileAndQueryAndRejectsInvalid(@TempDir File tempDir) throws Exception
	{
		final File plain = new File(tempDir, "plain.txt");
		Files.writeString(plain.toPath(), "x");
		assertEquals(plain.getAbsolutePath(), new File(Locator.fromURI(plain.toURI().toString())).getAbsolutePath());
		
		final File spaced = new File(tempDir, "has space.txt");
		Files.writeString(spaced.toPath(), "y");
		assertEquals(spaced.getAbsolutePath(), new File(Locator.fromURI(spaced.toURI().toString())).getAbsolutePath());
		
		final String withQuery = plain.toURI().toString() + "?ignored=1";
		assertEquals(plain.getAbsolutePath(), new File(Locator.fromURI(withQuery)).getAbsolutePath());
		
		// getFile keeps %252; decodeUri turns %25 into a literal '%' and leaves the trailing '2'.
		final String encodedPercent = Locator.fromURI(plain.getParentFile().toURI().toString() + "incomplete%252");
		assertTrue(encodedPercent.endsWith("incomplete%2"), encodedPercent);
		
		assertThrows(IllegalArgumentException.class, () -> Locator.fromURI(plain.getParentFile().toURI().toString() + "incomplete%2"));
		assertThrows(IllegalArgumentException.class, () -> Locator.fromURI("http://example.com/x"));
		assertThrows(IllegalArgumentException.class, () -> Locator.fromURI("not a uri ://"));
	}
	
	@Test
	void fromURIStripsWindowsDriveSlashWhenPathSeparatorIsSemicolon()
	{
		if (File.pathSeparatorChar != ';')
		{
			return;
		}
		assertEquals("C:\\temp\\a.txt", Locator.fromURI("file:///C:/temp/a.txt"));
	}
	
	@Test
	void getToolsJarReturnsNullOrExistingFile()
	{
		final File toolsJar = Locator.getToolsJar();
		if (toolsJar != null)
		{
			assertTrue(toolsJar.exists());
		}
	}
	
	@Test
	void getLocationURLsHandlesMissingFileMatchMismatchAndDirectory(@TempDir File tempDir) throws Exception
	{
		final File missing = new File(tempDir, "missing");
		assertEquals(0, Locator.getLocationURLs(missing).length);
		
		final File jar = new File(tempDir, "lib.jar");
		Files.writeString(jar.toPath(), "jar");
		final URL[] matching = Locator.getLocationURLs(jar);
		assertEquals(1, matching.length);
		assertEquals(jar.toURI().toURL(), matching[0]);
		
		final File txt = new File(tempDir, "notes.txt");
		Files.writeString(txt.toPath(), "txt");
		final URL[] mismatch = Locator.getLocationURLs(txt);
		assertEquals(1, mismatch.length);
		assertNull(mismatch[0]);
		
		final File nestedJar = new File(tempDir, "nested.jar");
		Files.writeString(nestedJar.toPath(), "nested");
		final URL[] directory = Locator.getLocationURLs(tempDir, new String[]
		{
			".jar"
		});
		assertEquals(2, directory.length);
	}
}
