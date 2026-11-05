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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.l2journey.Config;
import com.l2journey.PhantomConfig;
import com.l2journey.commons.threads.ThreadPool;
import com.l2journey.gameserver.model.Location;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.network.serverpackets.ServerPacket;

/**
 * Unit tests for {@link PhantomEngine} return relocation and enable gates.
 */
class PhantomEngineTest
{
	private boolean _debugWasOn;
	private boolean _phantomEnableWas;
	
	@BeforeEach
	void setUp()
	{
		_debugWasOn = PhantomManager.isDebugMode();
		if (_debugWasOn)
		{
			PhantomManager.toggleDebug();
		}
		_phantomEnableWas = Config.PHANTOM_ENABLE;
		Config.PHANTOM_ENABLE = true;
		PhantomEngine.isRunning = false;
		PhantomEngine.activePhantoms.clear();
		PhantomEngine.clearPendingReturns();
		PhantomState.clear();
	}
	
	@AfterEach
	void tearDown()
	{
		PhantomEngine.clearPendingReturns();
		PhantomEngine.activePhantoms.clear();
		PhantomEngine.isRunning = false;
		PhantomState.clear();
		PhantomConfig.ROUTES.clear();
		Config.PHANTOM_ENABLE = _phantomEnableWas;
		if (_debugWasOn != PhantomManager.isDebugMode())
		{
			PhantomManager.toggleDebug();
		}
	}
	
	@Test
	void startSystemDoesNothingWhenDisabled()
	{
		Config.PHANTOM_ENABLE = false;
		PhantomEngine.startSystem(null);
		assertFalse(PhantomEngine.isRunning);
	}
	
	@Test
	void startBatchDoesNothingWhenDisabled()
	{
		Config.PHANTOM_ENABLE = false;
		PhantomEngine.startBatch(1, null);
		assertFalse(PhantomEngine.isRunning);
		assertTrue(PhantomEngine.activePhantoms.isEmpty());
	}
	
	@Test
	void createAndStartDoesNothingWhenDisabled()
	{
		Config.PHANTOM_ENABLE = false;
		PhantomEngine.createAndStart(1, null);
		assertFalse(PhantomEngine.isRunning);
		assertTrue(PhantomEngine.activePhantoms.isEmpty());
	}
	
	@Test
	void nullArgsReturnFalse()
	{
		assertFalse(PhantomEngine.movePhantomToWithReturn(null, new Location(1, 2, 3), "x"));
		assertFalse(PhantomEngine.movePhantomToWithReturn(mock(Player.class), null, "x"));
	}
	
	@Test
	void startsReturnWindowAndBlocksSecondCall()
	{
		Player bot = mockReadyBot(11);
		AtomicReference<Runnable> scheduled = new AtomicReference<>();
		ScheduledFuture<?> future = mock(ScheduledFuture.class);
		
		try (MockedStatic<ThreadPool> threadPool = mockStatic(ThreadPool.class))
		{
			threadPool.when(() -> ThreadPool.schedule(any(Runnable.class), anyLong())).thenAnswer(inv ->
			{
				scheduled.set(inv.getArgument(0));
				return future;
			});
			
			assertTrue(PhantomEngine.movePhantomToWithReturn(bot, new Location(100, 200, 300), "test"));
			verify(bot).abortAttack();
			verify(bot).abortCast();
			verify(bot).stopMove(null);
			verify(bot).setTarget(null);
			verify(bot).broadcastPacket(any(ServerPacket.class));
			assertTrue(PhantomState.RETURN_UNTIL.get(11) > System.currentTimeMillis());
			assertTrue(PhantomState.NEXT_HUNT_TELEPORT.get(11) >= PhantomState.RETURN_UNTIL.get(11) - 400L);
			
			assertFalse(PhantomEngine.movePhantomToWithReturn(bot, new Location(1, 1, 1), "again"));
			
			PhantomEngine.cancelReturnRelocate(11);
			assertFalse(PhantomState.RETURN_UNTIL.containsKey(11));
			assertTrue(PhantomEngine.movePhantomToWithReturn(bot, new Location(2, 2, 2), "retry"));
		}
	}
	
	@Test
	void scheduledRunnableRelocatesOnlyWhenActive()
	{
		Player bot = mockReadyBot(22);
		AtomicReference<Runnable> scheduled = new AtomicReference<>();
		ScheduledFuture<?> future = mock(ScheduledFuture.class);
		Location dest = new Location(10, 20, 30);
		
		try (MockedStatic<ThreadPool> threadPool = mockStatic(ThreadPool.class))
		{
			threadPool.when(() -> ThreadPool.schedule(any(Runnable.class), anyLong())).thenAnswer(inv ->
			{
				scheduled.set(inv.getArgument(0));
				return future;
			});
			
			assertTrue(PhantomEngine.movePhantomToWithReturn(bot, dest, "go"));
			PhantomEngine.isRunning = false;
			PhantomEngine.activePhantoms.add(bot);
			when(bot.isOnline()).thenReturn(true);
			scheduled.get().run();
			verify(bot, never()).spawnMe(anyInt(), anyInt(), anyInt());
			
			PhantomState.RETURN_UNTIL.clear();
			assertTrue(PhantomEngine.movePhantomToWithReturn(bot, dest, "go2"));
			PhantomEngine.isRunning = true;
			when(bot.isOnline()).thenReturn(false);
			scheduled.get().run();
			verify(bot, never()).spawnMe(anyInt(), anyInt(), anyInt());
			
			PhantomState.RETURN_UNTIL.clear();
			PhantomEngine.activePhantoms.clear();
			assertTrue(PhantomEngine.movePhantomToWithReturn(bot, dest, "go3"));
			when(bot.isOnline()).thenReturn(true);
			scheduled.get().run();
			verify(bot, never()).spawnMe(anyInt(), anyInt(), anyInt());
			
			PhantomState.RETURN_UNTIL.clear();
			PhantomEngine.activePhantoms.add(bot);
			assertTrue(PhantomEngine.movePhantomToWithReturn(bot, dest, "go4"));
			when(bot.isDead()).thenReturn(false);
			when(bot.isSpawned()).thenReturn(false);
			scheduled.get().run();
			verify(bot).spawnMe(10, 20, 30);
		}
	}
	
	@Test
	void clearPendingReturnsCancelsFutures()
	{
		Player bot = mockReadyBot(33);
		ScheduledFuture<?> future = mock(ScheduledFuture.class);
		
		try (MockedStatic<ThreadPool> threadPool = mockStatic(ThreadPool.class))
		{
			threadPool.when(() -> ThreadPool.schedule(any(Runnable.class), anyLong())).thenReturn(future);
			assertTrue(PhantomEngine.movePhantomToWithReturn(bot, new Location(1, 2, 3), "c"));
			PhantomEngine.clearPendingReturns();
			verify(future).cancel(false);
			assertTrue(PhantomState.RETURN_UNTIL.isEmpty());
		}
	}

	@Test
	void handleDeadPhantomSchedulesReviveOnceAndSetsMood()
	{
		Player bot = mockReadyBot(88);
		when(bot.isDead()).thenReturn(true);
		ScheduledFuture<?> future = mock(ScheduledFuture.class);

		try (MockedStatic<ThreadPool> threadPool = mockStatic(ThreadPool.class))
		{
			threadPool.when(() -> ThreadPool.schedule(any(Runnable.class), anyLong())).thenReturn(future);
			assertTrue(PhantomEngine.handleDeadPhantom(bot));
			assertEquals(PhantomMood.Mood.ANNOYED, PhantomMood.getMood(bot));
			assertFalse(PhantomEngine.handleDeadPhantom(bot));
		}
		PhantomMood.clear();
		PhantomState.REVIVING_PHANTOMS.clear();
	}

	@Test
	void handleDeadPhantomReviveCallbackRunsWhenActive()
	{
		Player bot = mockReadyBot(89);
		when(bot.isDead()).thenReturn(true);
		when(bot.isSpawned()).thenReturn(false);
		when(bot.getMaxHp()).thenReturn(200);
		when(bot.getMaxMp()).thenReturn(150);
		when(bot.getLevel()).thenReturn(45);
		PhantomConfig.ROUTES.clear();
		PhantomConfig.ROUTES.add(new PhantomConfig.FarmZone(40, 50, 111, 222, 333, 1, 2, 3));
		com.l2journey.gameserver.model.actor.templates.PlayerTemplate template = mock(com.l2journey.gameserver.model.actor.templates.PlayerTemplate.class);
		when(bot.getTemplate()).thenReturn(template);
		when(template.getCreationPoint()).thenReturn(new Location(5, 6, 7));
		AtomicReference<Runnable> scheduled = new AtomicReference<>();

		try (MockedStatic<ThreadPool> threadPool = mockStatic(ThreadPool.class);
			MockedStatic<PhantomGeo> geo = mockStatic(PhantomGeo.class))
		{
			threadPool.when(() -> ThreadPool.schedule(any(Runnable.class), anyLong())).thenAnswer(inv ->
			{
				scheduled.set(inv.getArgument(0));
				return mock(ScheduledFuture.class);
			});
			geo.when(() -> PhantomGeo.getNpcLikeSpawn(any(Location.class))).thenAnswer(inv -> inv.getArgument(0));

			assertTrue(PhantomEngine.handleDeadPhantom(bot));
			PhantomEngine.isRunning = true;
			PhantomEngine.activePhantoms.add(bot);
			when(bot.isOnline()).thenReturn(true);
			when(bot.isDead()).thenReturn(false);
			scheduled.get().run();

			verify(bot).doRevive();
			verify(bot).setCurrentHp(200);
			verify(bot).setCurrentMp(150);
			verify(bot).spawnMe(111, 222, 333);
			verify(bot, atLeastOnce()).broadcastUserInfo();
			assertFalse(PhantomState.REVIVING_PHANTOMS.contains(89));
			assertEquals(45, PhantomState.HUNT_LEVEL_BAND.get(89).intValue());
		}
		PhantomMood.clear();
		PhantomState.REVIVING_PHANTOMS.clear();
	}

	@Test
	void handleDeadPhantomReviveCallbackSkipsWhenOffline()
	{
		Player bot = mockReadyBot(90);
		when(bot.isDead()).thenReturn(true);
		AtomicReference<Runnable> scheduled = new AtomicReference<>();

		try (MockedStatic<ThreadPool> threadPool = mockStatic(ThreadPool.class))
		{
			threadPool.when(() -> ThreadPool.schedule(any(Runnable.class), anyLong())).thenAnswer(inv ->
			{
				scheduled.set(inv.getArgument(0));
				return mock(ScheduledFuture.class);
			});
			assertTrue(PhantomEngine.handleDeadPhantom(bot));
			PhantomEngine.isRunning = true;
			PhantomEngine.activePhantoms.add(bot);
			when(bot.isOnline()).thenReturn(false);
			scheduled.get().run();
			verify(bot, never()).doRevive();
			assertFalse(PhantomState.REVIVING_PHANTOMS.contains(90));
		}
		PhantomMood.clear();
		PhantomState.REVIVING_PHANTOMS.clear();
	}

	@Test
	void handleDeadPhantomNullReturnsFalse()
	{
		assertFalse(PhantomEngine.handleDeadPhantom(null));
	}
	
	private static Player mockReadyBot(int objectId)
	{
		Player bot = mock(Player.class);
		when(bot.getObjectId()).thenReturn(objectId);
		when(bot.getName()).thenReturn("Bot" + objectId);
		when(bot.isOnline()).thenReturn(true);
		return bot;
	}
}
