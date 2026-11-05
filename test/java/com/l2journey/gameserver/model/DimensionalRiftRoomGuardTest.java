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
package com.l2journey.gameserver.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.l2journey.gameserver.managers.DimensionalRiftManager;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.groups.Party;
import com.l2journey.gameserver.model.interfaces.ILocational;

/**
 * Null-safe dimensional rift room lookups for {@link DimensionalRift}.
 */
class DimensionalRiftRoomGuardTest
{
	private static final byte TYPE = 1;
	private static final byte ROOM = 2;
	
	@Test
	void constructorThrowsWhenRoomMissing()
	{
		final Party party = mock(Party.class);
		final DimensionalRiftManager manager = mock(DimensionalRiftManager.class);
		when(manager.getRoom(TYPE, ROOM)).thenReturn(null);
		
		try (MockedStatic<DimensionalRiftManager> managers = mockStatic(DimensionalRiftManager.class))
		{
			managers.when(DimensionalRiftManager::getInstance).thenReturn(manager);
			assertThrows(IllegalStateException.class, () -> new DimensionalRift(party, TYPE, ROOM));
			verify(party, never()).setDimensionalRift(any());
		}
	}
	
	@Test
	void constructorUsesResolvedRoomCoordinates()
	{
		final Party party = mock(Party.class);
		when(party.getMembers()).thenReturn(Collections.emptyList());
		final Location coords = new Location(11, 22, 33);
		final DimensionalRiftRoom room = mock(DimensionalRiftRoom.class);
		when(room.getTeleportCoorinates()).thenReturn(coords);
		final DimensionalRiftManager manager = mock(DimensionalRiftManager.class);
		when(manager.getRoom(TYPE, ROOM)).thenReturn(room);
		
		try (MockedStatic<DimensionalRiftManager> managers = mockStatic(DimensionalRiftManager.class))
		{
			managers.when(DimensionalRiftManager::getInstance).thenReturn(manager);
			final DimensionalRift rift = new TestDimensionalRift(party, TYPE, ROOM);
			assertEquals(ROOM, rift.getCurrentRoom());
			verify(room).setPartyInside(true);
			verify(party).setDimensionalRift(rift);
		}
	}
	
	@Test
	void teleportToNextRoomSkipsWhenRoomMissing()
	{
		final Party party = mock(Party.class);
		when(party.getMembers()).thenReturn(Collections.emptyList());
		final DimensionalRiftRoom room = mock(DimensionalRiftRoom.class);
		when(room.getTeleportCoorinates()).thenReturn(new Location(1, 2, 3));
		final DimensionalRiftManager manager = mock(DimensionalRiftManager.class);
		when(manager.getRoom(TYPE, ROOM)).thenReturn(room).thenReturn(null);
		
		final Player player = mock(Player.class);
		try (MockedStatic<DimensionalRiftManager> managers = mockStatic(DimensionalRiftManager.class))
		{
			managers.when(DimensionalRiftManager::getInstance).thenReturn(manager);
			final TestDimensionalRift rift = new TestDimensionalRift(party, TYPE, ROOM);
			clearInvocations(room, player);
			rift.teleportToNextRoom(player);
			verify(player, never()).teleToLocation(any(ILocational.class));
			verify(room, never()).setPartyInside(true);
		}
	}
	
	@Test
	void teleportToNextRoomUsesResolvedRoomCoordinates()
	{
		final Party party = mock(Party.class);
		when(party.getMembers()).thenReturn(Collections.emptyList());
		final Location coords = new Location(40, 50, 60);
		final DimensionalRiftRoom room = mock(DimensionalRiftRoom.class);
		when(room.getTeleportCoorinates()).thenReturn(coords);
		when(room.isBossRoom()).thenReturn(false);
		final DimensionalRiftManager manager = mock(DimensionalRiftManager.class);
		when(manager.getRoom(TYPE, ROOM)).thenReturn(room);
		
		final Player player = mock(Player.class);
		try (MockedStatic<DimensionalRiftManager> managers = mockStatic(DimensionalRiftManager.class))
		{
			managers.when(DimensionalRiftManager::getInstance).thenReturn(manager);
			final TestDimensionalRift rift = new TestDimensionalRift(party, TYPE, ROOM);
			clearInvocations(room, player);
			rift.teleportToNextRoom(player);
			verify(room).setPartyInside(true);
			verify(player).teleToLocation(coords);
		}
	}
	
	@Test
	void killRiftStillCancelsTimersWhenRoomMissing()
	{
		final Party party = mock(Party.class);
		when(party.getMembers()).thenReturn(Collections.emptyList());
		final DimensionalRiftRoom room = mock(DimensionalRiftRoom.class);
		when(room.getTeleportCoorinates()).thenReturn(new Location(1, 2, 3));
		final DimensionalRiftManager manager = mock(DimensionalRiftManager.class);
		when(manager.getRoom(TYPE, ROOM)).thenReturn(room).thenReturn(null);
		
		try (MockedStatic<DimensionalRiftManager> managers = mockStatic(DimensionalRiftManager.class))
		{
			managers.when(DimensionalRiftManager::getInstance).thenReturn(manager);
			final TestDimensionalRift rift = new TestDimensionalRift(party, TYPE, ROOM);
			clearInvocations(room);
			rift.killRift();
			verify(manager).killRift(rift);
			verify(room, never()).unspawn();
			verify(room, never()).setPartyInside(false);
		}
	}
	
	@Test
	void killRiftUnspawnsLoadedRoom()
	{
		final Party party = mock(Party.class);
		when(party.getMembers()).thenReturn(Collections.emptyList());
		final DimensionalRiftRoom room = mock(DimensionalRiftRoom.class);
		when(room.getTeleportCoorinates()).thenReturn(new Location(1, 2, 3));
		when(room.unspawn()).thenReturn(room);
		final DimensionalRiftManager manager = mock(DimensionalRiftManager.class);
		when(manager.getRoom(TYPE, ROOM)).thenReturn(room);
		
		try (MockedStatic<DimensionalRiftManager> managers = mockStatic(DimensionalRiftManager.class))
		{
			managers.when(DimensionalRiftManager::getInstance).thenReturn(manager);
			final TestDimensionalRift rift = new TestDimensionalRift(party, TYPE, ROOM);
			clearInvocations(room);
			rift.killRift();
			verify(room).unspawn();
			verify(room).setPartyInside(false);
			verify(manager).killRift(rift);
		}
	}
	
	@Test
	void checkBossRoomIsFalseWhenRoomMissing() throws Exception
	{
		final Party party = mock(Party.class);
		when(party.getMembers()).thenReturn(Collections.emptyList());
		final DimensionalRiftRoom room = mock(DimensionalRiftRoom.class);
		when(room.getTeleportCoorinates()).thenReturn(new Location(1, 2, 3));
		final DimensionalRiftManager manager = mock(DimensionalRiftManager.class);
		when(manager.getRoom(TYPE, ROOM)).thenReturn(room).thenReturn(null);
		
		try (MockedStatic<DimensionalRiftManager> managers = mockStatic(DimensionalRiftManager.class))
		{
			managers.when(DimensionalRiftManager::getInstance).thenReturn(manager);
			final TestDimensionalRift rift = new TestDimensionalRift(party, TYPE, ROOM);
			rift.checkBossRoom(ROOM);
			assertFalse(isBossRoom(rift));
		}
	}
	
	@Test
	void checkBossRoomReflectsLoadedRoom() throws Exception
	{
		final Party party = mock(Party.class);
		when(party.getMembers()).thenReturn(Collections.emptyList());
		final DimensionalRiftRoom bossRoom = mock(DimensionalRiftRoom.class);
		when(bossRoom.getTeleportCoorinates()).thenReturn(new Location(1, 2, 3));
		when(bossRoom.isBossRoom()).thenReturn(true);
		final DimensionalRiftRoom normalRoom = mock(DimensionalRiftRoom.class);
		when(normalRoom.isBossRoom()).thenReturn(false);
		final DimensionalRiftManager manager = mock(DimensionalRiftManager.class);
		when(manager.getRoom(TYPE, ROOM)).thenReturn(bossRoom);
		when(manager.getRoom(TYPE, (byte) 3)).thenReturn(normalRoom);
		
		try (MockedStatic<DimensionalRiftManager> managers = mockStatic(DimensionalRiftManager.class))
		{
			managers.when(DimensionalRiftManager::getInstance).thenReturn(manager);
			final TestDimensionalRift rift = new TestDimensionalRift(party, TYPE, ROOM);
			rift.checkBossRoom(ROOM);
			assertTrue(isBossRoom(rift));
			rift.checkBossRoom((byte) 3);
			assertFalse(isBossRoom(rift));
		}
	}
	
	@Test
	void getRoomCoordThrowsWhenRoomMissing()
	{
		final Party party = mock(Party.class);
		when(party.getMembers()).thenReturn(Collections.emptyList());
		final DimensionalRiftRoom room = mock(DimensionalRiftRoom.class);
		when(room.getTeleportCoorinates()).thenReturn(new Location(1, 2, 3));
		final DimensionalRiftManager manager = mock(DimensionalRiftManager.class);
		when(manager.getRoom(TYPE, ROOM)).thenReturn(room).thenReturn(null);
		
		try (MockedStatic<DimensionalRiftManager> managers = mockStatic(DimensionalRiftManager.class))
		{
			managers.when(DimensionalRiftManager::getInstance).thenReturn(manager);
			final TestDimensionalRift rift = new TestDimensionalRift(party, TYPE, ROOM);
			assertThrows(IllegalStateException.class, () -> rift.getRoomCoord(ROOM));
		}
	}
	
	@Test
	void getRoomCoordReturnsLoadedCoordinates()
	{
		final Party party = mock(Party.class);
		when(party.getMembers()).thenReturn(Collections.emptyList());
		final Location coords = new Location(70, 80, 90);
		final DimensionalRiftRoom room = mock(DimensionalRiftRoom.class);
		when(room.getTeleportCoorinates()).thenReturn(coords);
		final DimensionalRiftManager manager = mock(DimensionalRiftManager.class);
		when(manager.getRoom(TYPE, ROOM)).thenReturn(room);
		
		try (MockedStatic<DimensionalRiftManager> managers = mockStatic(DimensionalRiftManager.class))
		{
			managers.when(DimensionalRiftManager::getInstance).thenReturn(manager);
			final TestDimensionalRift rift = new TestDimensionalRift(party, TYPE, ROOM);
			assertSame(coords, rift.getRoomCoord(ROOM));
		}
	}
	
	private static boolean isBossRoom(DimensionalRift rift) throws Exception
	{
		final Field field = DimensionalRift.class.getDeclaredField("isBossRoom");
		field.setAccessible(true);
		return field.getBoolean(rift);
	}
	
	private static final class TestDimensionalRift extends DimensionalRift
	{
		TestDimensionalRift(Party party, byte type, byte room)
		{
			super(party, type, room);
		}
		
		@Override
		public void createSpawnTimer(byte room)
		{
			// Avoid ThreadPool and Timer scheduling in unit tests.
		}
		
		@Override
		protected void createTeleporterTimer(boolean reasonTP)
		{
			// Avoid ThreadPool and Timer scheduling in unit tests.
		}
	}
}
