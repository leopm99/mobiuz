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
package com.l2journey.gameserver.model.actor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.MockedStatic;

import com.l2journey.gameserver.GeoData;
import com.l2journey.gameserver.ai.CreatureAI;
import com.l2journey.gameserver.ai.Intention;
import com.l2journey.gameserver.data.xml.DoorData;
import com.l2journey.gameserver.model.WorldRegion;
import com.l2journey.gameserver.model.actor.Creature.MoveData;
import com.l2journey.gameserver.model.actor.instance.Door;
import com.l2journey.gameserver.model.actor.stat.CreatureStat;
import com.l2journey.gameserver.network.serverpackets.ServerPacket;
import com.l2journey.gameserver.network.serverpackets.StopMove;
import com.l2journey.gameserver.taskmanagers.GameTimeTaskManager;

/**
 * Blocked-step writes in {@link Creature#updatePosition()} must tolerate {@code _move} being cleared or replaced mid-check.
 */
class CreatureUpdatePositionBlockedMoveTest
{
	@Test
	void attackBlockedWhenMoveClearedDuringCanMoveFinishesWithoutStopMove() throws Exception
	{
		final Player player = newPlayer();
		final MoveData move = nearDestinationMove();
		setMove(player, move);
		setAttackAI(player);
		
		final GameTimeTaskManager gameTime = mock(GameTimeTaskManager.class);
		when(gameTime.getGameTicks()).thenReturn(2);
		final GeoData geo = mock(GeoData.class);
		when(geo.canMove(anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt())).thenAnswer(invocation ->
		{
			setMove(player, null);
			return false;
		});
		
		try (MockedStatic<GameTimeTaskManager> gameTimes = mockStatic(GameTimeTaskManager.class);
			MockedStatic<GeoData> geos = mockStatic(GeoData.class))
		{
			gameTimes.when(GameTimeTaskManager::getInstance).thenReturn(gameTime);
			geos.when(GeoData::getInstance).thenReturn(geo);
			
			assertTrue(player.updatePosition());
		}
		
		verify(geo).canMove(anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt());
		verify(player, never()).broadcastPacket(any(StopMove.class));
		assertEquals(5, move.onGeodataPathIndex);
		assertNull(getMove(player));
	}
	
	@Test
	void attackBlockedWhenMoveStillInstalledStopsClient() throws Exception
	{
		final Player player = newPlayer();
		final MoveData move = nearDestinationMove();
		setMove(player, move);
		setAttackAI(player);
		
		final GameTimeTaskManager gameTime = mock(GameTimeTaskManager.class);
		when(gameTime.getGameTicks()).thenReturn(2);
		final GeoData geo = mock(GeoData.class);
		when(geo.canMove(anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt())).thenReturn(false);
		
		try (MockedStatic<GameTimeTaskManager> gameTimes = mockStatic(GameTimeTaskManager.class);
			MockedStatic<GeoData> geos = mockStatic(GeoData.class))
		{
			gameTimes.when(GameTimeTaskManager::getInstance).thenReturn(gameTime);
			geos.when(GeoData::getInstance).thenReturn(geo);
			
			assertTrue(player.updatePosition());
		}
		
		verify(geo).canMove(anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt());
		verify(player).broadcastPacket(any(StopMove.class));
		assertEquals(-1, move.onGeodataPathIndex);
		assertSame(move, getMove(player));
	}
	
	@Test
	void attackBlockedWhenMoveReplacedDuringCanMoveContinues() throws Exception
	{
		final Player player = newPlayer();
		final MoveData move = nearDestinationMove();
		final MoveData replacement = nearDestinationMove();
		replacement.onGeodataPathIndex = 7;
		setMove(player, move);
		setAttackAI(player);
		
		final GameTimeTaskManager gameTime = mock(GameTimeTaskManager.class);
		when(gameTime.getGameTicks()).thenReturn(2);
		final GeoData geo = mock(GeoData.class);
		when(geo.canMove(anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt())).thenAnswer(invocation ->
		{
			setMove(player, replacement);
			return false;
		});
		
		try (MockedStatic<GameTimeTaskManager> gameTimes = mockStatic(GameTimeTaskManager.class);
			MockedStatic<GeoData> geos = mockStatic(GeoData.class))
		{
			gameTimes.when(GameTimeTaskManager::getInstance).thenReturn(gameTime);
			geos.when(GeoData::getInstance).thenReturn(geo);
			
			assertFalse(player.updatePosition());
		}
		
		verify(geo).canMove(anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt());
		verify(player, never()).broadcastPacket(any(StopMove.class));
		assertEquals(5, move.onGeodataPathIndex);
		assertEquals(7, replacement.onGeodataPathIndex);
		assertSame(replacement, getMove(player));
	}
	
	@Test
	void cursorBlockedWhenMoveClearedDuringCanMoveFinishesWithoutStopMove() throws Exception
	{
		final Player player = newPlayer();
		player.setCursorKeyMovement(true);
		final MoveData move = nearDestinationMove();
		setMove(player, move);
		
		final GameTimeTaskManager gameTime = mock(GameTimeTaskManager.class);
		when(gameTime.getGameTicks()).thenReturn(2);
		final GeoData geo = mock(GeoData.class);
		when(geo.canMove(anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt())).thenAnswer(invocation ->
		{
			setMove(player, null);
			return false;
		});
		
		try (MockedStatic<GameTimeTaskManager> gameTimes = mockStatic(GameTimeTaskManager.class);
			MockedStatic<GeoData> geos = mockStatic(GeoData.class))
		{
			gameTimes.when(GameTimeTaskManager::getInstance).thenReturn(gameTime);
			geos.when(GeoData::getInstance).thenReturn(geo);
			
			assertTrue(player.updatePosition());
		}
		
		verify(geo).canMove(anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt());
		verify(player, never()).broadcastPacket(any(StopMove.class));
		assertEquals(5, move.onGeodataPathIndex);
	}
	
	@Test
	void cursorBlockedWhenMoveStillInstalledStopsMove() throws Exception
	{
		final Player player = newPlayer();
		player.setCursorKeyMovement(true);
		final MoveData move = nearDestinationMove();
		setMove(player, move);
		
		final GameTimeTaskManager gameTime = mock(GameTimeTaskManager.class);
		when(gameTime.getGameTicks()).thenReturn(2);
		final GeoData geo = mock(GeoData.class);
		when(geo.canMove(anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt())).thenReturn(false);
		
		try (MockedStatic<GameTimeTaskManager> gameTimes = mockStatic(GameTimeTaskManager.class);
			MockedStatic<GeoData> geos = mockStatic(GeoData.class))
		{
			gameTimes.when(GameTimeTaskManager::getInstance).thenReturn(gameTime);
			geos.when(GeoData::getInstance).thenReturn(geo);
			
			assertTrue(player.updatePosition());
		}
		
		verify(geo).canMove(anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt());
		verify(player).broadcastPacket(any(StopMove.class));
		assertEquals(-1, move.onGeodataPathIndex);
		assertNull(getMove(player));
	}
	
	@Test
	void longMouseBlockedWhenMoveClearedDuringCanMoveFinishesWithoutAiIdle() throws Exception
	{
		final Player player = newPlayer();
		final MoveData move = farDestinationMove();
		setMove(player, move);
		
		final GameTimeTaskManager gameTime = mock(GameTimeTaskManager.class);
		when(gameTime.getGameTicks()).thenReturn(2);
		final GeoData geo = mock(GeoData.class);
		when(geo.canMove(anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt())).thenAnswer(invocation ->
		{
			setMove(player, null);
			return false;
		});
		
		try (MockedStatic<GameTimeTaskManager> gameTimes = mockStatic(GameTimeTaskManager.class);
			MockedStatic<GeoData> geos = mockStatic(GeoData.class))
		{
			gameTimes.when(GameTimeTaskManager::getInstance).thenReturn(gameTime);
			geos.when(GeoData::getInstance).thenReturn(geo);
			
			assertTrue(player.updatePosition());
		}
		
		verify(geo).canMove(anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt());
		assertEquals(5, move.onGeodataPathIndex);
		assertNull(getMove(player));
	}
	
	@Test
	void longMouseBlockedWhenMoveStillInstalledMarksPathFinished() throws Exception
	{
		final Player player = newPlayer();
		final MoveData move = farDestinationMove();
		setMove(player, move);
		
		final GameTimeTaskManager gameTime = mock(GameTimeTaskManager.class);
		when(gameTime.getGameTicks()).thenReturn(2);
		final GeoData geo = mock(GeoData.class);
		when(geo.canMove(anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt())).thenReturn(false);
		
		try (MockedStatic<GameTimeTaskManager> gameTimes = mockStatic(GameTimeTaskManager.class);
			MockedStatic<GeoData> geos = mockStatic(GeoData.class))
		{
			gameTimes.when(GameTimeTaskManager::getInstance).thenReturn(gameTime);
			geos.when(GeoData::getInstance).thenReturn(geo);
			
			assertTrue(player.updatePosition());
		}
		
		verify(geo).canMove(anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt());
		assertEquals(-1, move.onGeodataPathIndex);
		assertSame(move, getMove(player));
	}
	
	@Test
	void doorBlockedWhenMoveClearedDuringDoorCheckFinishesWithoutStopMove() throws Exception
	{
		final Player player = newPlayer();
		final MoveData move = nearDestinationMove();
		setMove(player, move);
		attachDoorRegion(player);
		
		final GameTimeTaskManager gameTime = mock(GameTimeTaskManager.class);
		when(gameTime.getGameTicks()).thenReturn(2);
		final DoorData doors = mock(DoorData.class);
		when(doors.checkIfDoorsBetween(anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyBoolean())).thenAnswer(invocation ->
		{
			setMove(player, null);
			return true;
		});
		
		try (MockedStatic<GameTimeTaskManager> gameTimes = mockStatic(GameTimeTaskManager.class);
			MockedStatic<DoorData> doorData = mockStatic(DoorData.class);
			MockedStatic<GeoData> geos = mockStatic(GeoData.class))
		{
			gameTimes.when(GameTimeTaskManager::getInstance).thenReturn(gameTime);
			doorData.when(DoorData::getInstance).thenReturn(doors);
			geos.when(GeoData::getInstance).thenReturn(mock(GeoData.class));
			
			assertTrue(player.updatePosition());
		}
		
		verify(doors).checkIfDoorsBetween(anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyBoolean());
		verify(player, never()).broadcastPacket(any(StopMove.class));
		assertEquals(5, move.onGeodataPathIndex);
	}
	
	@Test
	void doorBlockedWhenMoveStillInstalledStopsMove() throws Exception
	{
		final Player player = newPlayer();
		final MoveData move = nearDestinationMove();
		setMove(player, move);
		attachDoorRegion(player);
		
		final GameTimeTaskManager gameTime = mock(GameTimeTaskManager.class);
		when(gameTime.getGameTicks()).thenReturn(2);
		final DoorData doors = mock(DoorData.class);
		when(doors.checkIfDoorsBetween(anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyBoolean())).thenReturn(true);
		
		try (MockedStatic<GameTimeTaskManager> gameTimes = mockStatic(GameTimeTaskManager.class);
			MockedStatic<DoorData> doorData = mockStatic(DoorData.class);
			MockedStatic<GeoData> geos = mockStatic(GeoData.class))
		{
			gameTimes.when(GameTimeTaskManager::getInstance).thenReturn(gameTime);
			doorData.when(DoorData::getInstance).thenReturn(doors);
			geos.when(GeoData::getInstance).thenReturn(mock(GeoData.class));
			
			assertTrue(player.updatePosition());
		}
		
		verify(doors).checkIfDoorsBetween(anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyBoolean());
		verify(player).broadcastPacket(any(StopMove.class));
		assertEquals(-1, move.onGeodataPathIndex);
		assertNull(getMove(player));
	}
	
	private static Player newPlayer() throws Exception
	{
		final Player player = mock(Player.class, Answers.CALLS_REAL_METHODS);
		doReturn(true).when(player).isSpawned();
		doReturn(100).when(player).getX();
		doReturn(200).when(player).getY();
		doReturn(-100).when(player).getZ();
		doReturn(0).when(player).getHeading();
		doReturn(0).when(player).getInstanceId();
		doReturn(1).when(player).getObjectId();
		doReturn(null).when(player).getLastServerPosition();
		doNothing().when(player).broadcastPacket(any(ServerPacket.class));
		doNothing().when(player).broadcastPacket(any(ServerPacket.class), anyInt());
		
		final CreatureStat stat = mock(CreatureStat.class);
		when(stat.getMoveSpeed()).thenReturn(100.0);
		setField(player, Creature.class, "_stat", stat);
		return player;
	}
	
	private static void setAttackAI(Player player)
	{
		final CreatureAI ai = mock(CreatureAI.class);
		when(ai.getIntention()).thenReturn(Intention.ATTACK);
		player.setAI(ai);
	}
	
	private static void attachDoorRegion(Player player)
	{
		final WorldRegion region = mock(WorldRegion.class);
		when(region.getDoors()).thenReturn(List.of(mock(Door.class)));
		when(region.getFences()).thenReturn(List.of());
		player.setWorldRegion(region);
	}
	
	private static MoveData nearDestinationMove()
	{
		final MoveData move = new MoveData();
		move.moveTimestamp = 1;
		move.xAccurate = 100;
		move.yAccurate = 200;
		move.xDestination = 150;
		move.yDestination = 200;
		move.zDestination = -100;
		move.onGeodataPathIndex = 5;
		return move;
	}
	
	private static MoveData farDestinationMove()
	{
		final MoveData move = new MoveData();
		move.moveTimestamp = 1;
		move.xAccurate = 100;
		move.yAccurate = 200;
		move.xDestination = 5000;
		move.yDestination = 200;
		move.zDestination = -100;
		move.onGeodataPathIndex = 5;
		return move;
	}
	
	private static void setMove(Player player, MoveData move) throws Exception
	{
		setField(player, Creature.class, "_move", move);
	}
	
	private static MoveData getMove(Player player) throws Exception
	{
		final Field field = Creature.class.getDeclaredField("_move");
		field.setAccessible(true);
		return (MoveData) field.get(player);
	}
	
	private static void setField(Object target, Class<?> owner, String name, Object value) throws Exception
	{
		final Field field = owner.getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}
}
