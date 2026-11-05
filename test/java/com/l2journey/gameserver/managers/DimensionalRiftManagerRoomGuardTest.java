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
package com.l2journey.gameserver.managers;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.l2journey.Config;
import com.l2journey.gameserver.model.DimensionalRiftRoom;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.interfaces.ILocational;

/**
 * Null-safe waiting-room teleport when dimensional rift rooms are missing.
 */
class DimensionalRiftManagerRoomGuardTest
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
	void teleportToWaitingRoomSkipsWhenRoomMissing() throws Exception
	{
		final DimensionalRiftManager manager = managerWithEmptyRooms();
		final Player player = mock(Player.class);
		
		assertDoesNotThrow(() -> manager.teleportToWaitingRoom(player));
		verify(player, never()).teleToLocation(any(ILocational.class));
	}
	
	@Test
	void teleportToWaitingRoomUsesLoadedRoomCoordinates() throws Exception
	{
		final DimensionalRiftManager manager = managerWithEmptyRooms();
		final DimensionalRiftRoom waitingRoom = new DimensionalRiftRoom((byte) 0, (byte) 0, 0, 100, 0, 100, 0, 100, 10, 20, 30, false);
		rooms(manager).put((byte) 0, new HashMap<>(Map.of((byte) 0, waitingRoom)));
		
		final Player player = mock(Player.class);
		manager.teleportToWaitingRoom(player);
		verify(player).teleToLocation(waitingRoom.getTeleportCoorinates());
	}
	
	private DimensionalRiftManager managerWithEmptyRooms() throws Exception
	{
		Config.DATAPACK_ROOT = tempDir;
		final File xml = new File(tempDir, "data/DimensionalRift.xml");
		Files.createDirectories(xml.getParentFile().toPath());
		Files.writeString(xml.toPath(), "<?xml version=\"1.0\" encoding=\"UTF-8\"?><rift/>", StandardCharsets.UTF_8);
		
		final Constructor<DimensionalRiftManager> constructor = DimensionalRiftManager.class.getDeclaredConstructor();
		constructor.setAccessible(true);
		final DimensionalRiftManager manager = constructor.newInstance();
		rooms(manager).clear();
		return manager;
	}
	
	@SuppressWarnings("unchecked")
	private static Map<Byte, Map<Byte, DimensionalRiftRoom>> rooms(DimensionalRiftManager manager) throws Exception
	{
		final Field field = DimensionalRiftManager.class.getDeclaredField("_rooms");
		field.setAccessible(true);
		return (Map<Byte, Map<Byte, DimensionalRiftRoom>>) field.get(manager);
	}
}
