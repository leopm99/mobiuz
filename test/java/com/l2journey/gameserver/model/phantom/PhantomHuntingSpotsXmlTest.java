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
package com.l2journey.gameserver.model.phantom;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for PhantomHuntingSpots XML parse paths (XmlFactory).
 */
class PhantomHuntingSpotsXmlTest
{
	@TempDir
	File tempDir;
	
	@Test
	void parseNpcFileLoadsMonsterLevels() throws Exception
	{
		final File xml = new File(tempDir, "npc.xml");
		Files.writeString(xml.toPath(), """
			<?xml version="1.0" encoding="UTF-8"?>
			<list>
				<npc id="1001" type="Monster" level="40" name="TestMonster"/>
				<npc id="1002" type="Npc" level="10" name="SkipMe"/>
			</list>
			""", StandardCharsets.UTF_8);
		
		final Field levelsField = PhantomHuntingSpots.class.getDeclaredField("NPC_LEVELS");
		levelsField.setAccessible(true);
		@SuppressWarnings("unchecked")
		final Map<Integer, Integer> levels = (Map<Integer, Integer>) levelsField.get(null);
		levels.clear();
		
		final Method method = PhantomHuntingSpots.class.getDeclaredMethod("parseNpcFile", File.class);
		method.setAccessible(true);
		method.invoke(null, xml);
		
		assertEquals(40, levels.get(1001));
		assertNull(levels.get(1002));
	}
	
	@Test
	void parseSpawnFileUsesSecureFactory() throws Exception
	{
		final File xml = new File(tempDir, "spawn.xml");
		Files.writeString(xml.toPath(), """
			<?xml version="1.0" encoding="UTF-8"?>
			<list>
				<spawn>
					<territory name="test">
						<node x="0" y="0" zmin="0" zmax="0"/>
					</territory>
				</spawn>
			</list>
			""", StandardCharsets.UTF_8);
		
		final Method method = PhantomHuntingSpots.class.getDeclaredMethod("parseSpawnFile", File.class);
		method.setAccessible(true);
		assertDoesNotThrow(() -> method.invoke(null, xml));
	}
}
