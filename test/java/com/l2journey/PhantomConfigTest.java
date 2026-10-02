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

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for {@link PhantomConfig} id merge and XML load/save.
 */
class PhantomConfigTest
{
	@TempDir
	File tempDir;

	@BeforeEach
	void clearPhantomIds()
	{
		PhantomConfig.PHANTOM_IDS.clear();
	}

	@Test
	void mergeIntoEmptyListAddsAllIds()
	{
		List<Integer> ids = Arrays.asList(100, 200, 300);

		int added = PhantomConfig.mergePhantomIds(ids);

		assertEquals(3, added);
		assertEquals(PhantomConfig.PHANTOM_IDS, ids);
	}

	@Test
	void mergeSkipsExistingIds()
	{
		PhantomConfig.mergePhantomIds(Arrays.asList(100, 200));

		int added = PhantomConfig.mergePhantomIds(Arrays.asList(100, 200, 300));

		assertEquals(1, added);
		assertEquals(Arrays.asList(100, 200, 300), PhantomConfig.PHANTOM_IDS);
	}

	@Test
	void saveAndLoadRoundTripKeepsIdsInOrder()
	{
		File xmlFile = new File(tempDir, "phantomplayers.xml");
		PhantomConfig.mergePhantomIds(Arrays.asList(10, 20, 20, 30));

		PhantomConfig.saveXML(xmlFile);
		PhantomConfig.PHANTOM_IDS.clear();
		PhantomConfig.loadXML(xmlFile);

		assertEquals(Arrays.asList(10, 20, 30), PhantomConfig.PHANTOM_IDS);
	}

	@Test
	void loadMissingFileLeavesListEmpty()
	{
		PhantomConfig.PHANTOM_IDS.add(999);
		File missing = new File(tempDir, "missing-phantoms.xml");

		PhantomConfig.loadXML(missing);

		assertTrue(PhantomConfig.PHANTOM_IDS.isEmpty());
		assertTrue(!missing.exists());
	}

	@Test
	void loadEmptyListFileLeavesListEmpty() throws Exception
	{
		File xmlFile = new File(tempDir, "empty-phantoms.xml");
		try (FileWriter writer = new FileWriter(xmlFile))
		{
			writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<list>\n\n</list>\n");
		}
		PhantomConfig.PHANTOM_IDS.add(999);

		PhantomConfig.loadXML(xmlFile);

		assertTrue(PhantomConfig.PHANTOM_IDS.isEmpty());
		assertTrue(Files.size(xmlFile.toPath()) > 0);
	}
}
