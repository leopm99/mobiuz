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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.io.File;
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.l2journey.Config;
import com.l2journey.gameserver.data.SpawnTable;
import com.l2journey.gameserver.model.Spawn;
import com.l2journey.gameserver.model.World;
import com.l2journey.gameserver.model.actor.templates.NpcTemplate;

/**
 * Covers SpawnData try-with-resources write paths.
 */
class SpawnDataResourceTest
{
	private static final Path OTHERS_DIR = Path.of("data", "spawns", "Others");
	private static final int SPAWN_X = World.WORLD_X_MIN + 1000;
	private static final int SPAWN_Y = World.WORLD_Y_MIN + 1000;
	
	private File previousDatapackRoot;
	private boolean othersExisted;
	
	@BeforeEach
	void saveEnv() throws Exception
	{
		previousDatapackRoot = Config.DATAPACK_ROOT;
		// Use the absolute user.dir so spawnFile/tempFile path math matches Windows absolute paths.
		Config.DATAPACK_ROOT = new File(System.getProperty("user.dir"));
		othersExisted = Files.exists(OTHERS_DIR);
		Files.createDirectories(OTHERS_DIR);
	}
	
	@AfterEach
	void restoreEnv() throws Exception
	{
		Config.DATAPACK_ROOT = previousDatapackRoot;
		if (!othersExisted && Files.exists(OTHERS_DIR))
		{
			try (Stream<Path> walk = Files.walk(OTHERS_DIR))
			{
				walk.sorted(Comparator.reverseOrder()).forEach(path ->
				{
					try
					{
						Files.deleteIfExists(path);
					}
					catch (Exception _)
					{
						// Ignore cleanup failures.
					}
				});
			}
			try
			{
				Files.deleteIfExists(OTHERS_DIR.getParent()); // data/spawns
			}
			catch (Exception _)
			{
				// Parent may still contain other spawn folders.
			}
		}
	}
	
	@Test
	void addNewSpawnCreatesFileWithTryWithResources() throws Exception
	{
		final Spawn spawn = mockSpawn(100);
		withSpawnMocks(spawnData -> spawnData.addNewSpawn(spawn));
		
		try (Stream<Path> files = Files.list(OTHERS_DIR))
		{
			assertTrue(files.anyMatch(path -> path.toString().endsWith(".xml")));
		}
	}
	
	@Test
	void addNewSpawnUpdatesExistingFileWithTryWithResources() throws Exception
	{
		final Spawn spawn = mockSpawn(101);
		withSpawnMocks(spawnData ->
		{
			spawnData.addNewSpawn(spawn);
			spawnData.addNewSpawn(spawn);
		});
		
		final Path spawnFile = findSpawnFile();
		final String content = Files.readString(spawnFile, StandardCharsets.UTF_8);
		assertTrue(content.contains("id=\"101\""));
		assertTrue(content.contains("</spawn>"));
	}
	
	@Test
	void deleteSpawnRewritesFileWithTryWithResources() throws Exception
	{
		final Spawn spawn = mockSpawn(102);
		when(spawn.getNpcSpawnTemplateId()).thenReturn(0);
		when(spawn.getSpawnLocation()).thenReturn(null);
		when(spawn.getSpawnTerritory()).thenReturn(null);
		
		withSpawnMocks(spawnData ->
		{
			spawnData.addNewSpawn(spawn);
			final Path created = findSpawnFile();
			assertTrue(Files.exists(created));
			spawnData.deleteSpawn(spawn);
		});
		
		try (Stream<Path> temps = Files.list(OTHERS_DIR))
		{
			assertFalse(temps.anyMatch(path -> path.toString().endsWith(".tmp")));
		}
	}
	
	private static Spawn mockSpawn(int npcId)
	{
		final Spawn spawn = mock(Spawn.class);
		when(spawn.getId()).thenReturn(npcId);
		when(spawn.getAmount()).thenReturn(1);
		when(spawn.getX()).thenReturn(SPAWN_X);
		when(spawn.getY()).thenReturn(SPAWN_Y);
		when(spawn.getZ()).thenReturn(0);
		when(spawn.getHeading()).thenReturn(0);
		when(spawn.getRespawnDelay()).thenReturn(10000);
		return spawn;
	}
	
	@FunctionalInterface
	private interface SpawnDataAction
	{
		void run(SpawnData spawnData) throws Exception;
	}
	
	private static void withSpawnMocks(SpawnDataAction action) throws Exception
	{
		final NpcTemplate template = mock(NpcTemplate.class);
		when(template.getTitle()).thenReturn("");
		when(template.getName()).thenReturn("TestNpc");
		
		final SpawnTable spawnTable = mock(SpawnTable.class);
		final NpcData npcData = mock(NpcData.class);
		when(npcData.getTemplate(anyInt())).thenReturn(template);
		
		final Constructor<SpawnData> constructor = SpawnData.class.getDeclaredConstructor();
		constructor.setAccessible(true);
		
		try (MockedStatic<SpawnTable> spawnTables = mockStatic(SpawnTable.class);
			MockedStatic<NpcData> npcDatas = mockStatic(NpcData.class))
		{
			spawnTables.when(SpawnTable::getInstance).thenReturn(spawnTable);
			npcDatas.when(NpcData::getInstance).thenReturn(npcData);
			action.run(constructor.newInstance());
		}
	}
	
	private static Path findSpawnFile() throws Exception
	{
		try (Stream<Path> files = Files.list(OTHERS_DIR))
		{
			return files.filter(path -> path.toString().endsWith(".xml")).findFirst().orElseThrow();
		}
	}
}
