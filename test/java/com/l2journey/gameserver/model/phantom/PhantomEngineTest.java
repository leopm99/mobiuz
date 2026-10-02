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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
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
	
	private static Player mockReadyBot(int objectId)
	{
		Player bot = mock(Player.class);
		when(bot.getObjectId()).thenReturn(objectId);
		when(bot.getName()).thenReturn("Bot" + objectId);
		when(bot.isOnline()).thenReturn(true);
		return bot;
	}
}
