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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.l2journey.PhantomConfig;
import com.l2journey.commons.threads.ThreadPool;
import com.l2journey.gameserver.model.Location;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.zone.ZoneId;

/**
 * Unit tests for {@link PhantomHuntingSpots}.
 */
class PhantomHuntingSpotsTest
{
	private boolean _debugWasOn;
	
	@BeforeEach
	void setUp()
	{
		_debugWasOn = PhantomManager.isDebugMode();
		if (_debugWasOn)
		{
			PhantomManager.toggleDebug();
		}
		PhantomEngine.clearPendingReturns();
		PhantomEngine.activePhantoms.clear();
		PhantomEngine.isRunning = false;
		PhantomState.clear();
		PhantomConfig.ROUTES.clear();
	}
	
	@AfterEach
	void tearDown()
	{
		PhantomEngine.clearPendingReturns();
		PhantomEngine.activePhantoms.clear();
		PhantomEngine.isRunning = false;
		PhantomState.clear();
		PhantomConfig.ROUTES.clear();
		if (_debugWasOn != PhantomManager.isDebugMode())
		{
			PhantomManager.toggleDebug();
		}
	}
	
	@Test
	void nullSpotReturnsFalse()
	{
		Player bot = mock(Player.class);
		assertFalse(PhantomHuntingSpots.travelToLevelSpot(bot, null, "x"));
	}

	@Test
	void forbiddenNamesIncludeQuestAndBox()
	{
		assertTrue(PhantomHuntingSpots.isForbiddenTargetName("Quest Mob"));
		assertTrue(PhantomHuntingSpots.isForbiddenTargetName("Treasure Box"));
		assertTrue(PhantomHuntingSpots.isForbiddenTargetTitle("Quest"));
		assertFalse(PhantomHuntingSpots.isForbiddenTargetTitle(null));
	}

	@Test
	void resolveHuntSpotFallsBackToRouteFarmPoint()
	{
		PhantomConfig.ROUTES.add(new PhantomConfig.FarmZone(40, 50, 100, 200, 300, 1, 2, 3));
		Location fallback = PhantomHuntingSpots.getRouteFarmFallback(45);
		assertEquals(100, fallback.getX());
		assertEquals(200, fallback.getY());
		assertEquals(300, fallback.getZ());

		Location resolved = PhantomHuntingSpots.resolveHuntSpot(45);
		assertEquals(100, resolved.getX());
	}

	@Test
	void isPreyTooWeakUsesTenLevelGap()
	{
		assertFalse(PhantomHuntingSpots.isPreyTooWeak(20, 11));
		assertTrue(PhantomHuntingSpots.isPreyTooWeak(20, 10));
		assertFalse(PhantomHuntingSpots.isPreyTooWeak(1, 1));
		assertTrue(PhantomHuntingSpots.isPreyTooWeak(74, 1));
	}

	@Test
	void onlyWeakPreyRequiresSeenMonstersWithoutAcceptable()
	{
		assertFalse(PhantomHuntingSpots.onlyWeakPrey(false, false));
		assertFalse(PhantomHuntingSpots.onlyWeakPrey(false, true));
		assertFalse(PhantomHuntingSpots.onlyWeakPrey(true, true));
		assertTrue(PhantomHuntingSpots.onlyWeakPrey(true, false));
	}

	@Test
	void travelUsesFallbackAndStartsReturnWindow()
	{
		PhantomConfig.ROUTES.add(new PhantomConfig.FarmZone(40, 50, 111, 222, 333, 1, 2, 3));
		Player bot = mock(Player.class);
		when(bot.getObjectId()).thenReturn(77);
		when(bot.getName()).thenReturn("Hunter");
		when(bot.getLevel()).thenReturn(45);
		ScheduledFuture<?> future = mock(ScheduledFuture.class);

		try (MockedStatic<PhantomGeo> geo = mockStatic(PhantomGeo.class); MockedStatic<ThreadPool> threadPool = mockStatic(ThreadPool.class))
		{
			geo.when(() -> PhantomGeo.getNpcLikeSpawn(any(Location.class))).thenAnswer(inv -> inv.getArgument(0));
			threadPool.when(() -> ThreadPool.schedule(any(Runnable.class), anyLong())).thenReturn(future);

			assertTrue(PhantomHuntingSpots.travelToLevelSpot(bot, "fallback"));
			assertTrue(PhantomState.RETURN_UNTIL.containsKey(77));
			assertNull(PhantomState.HUNT_LEVEL_BAND.get(77));
		}
	}
	
	@Test
	void spotSetsHuntBandOnlyAfterReturnRelocate()
	{
		Player bot = mock(Player.class);
		when(bot.getObjectId()).thenReturn(55);
		when(bot.getName()).thenReturn("Hunter");
		when(bot.getLevel()).thenReturn(42);
		when(bot.isOnline()).thenReturn(true);
		when(bot.isDead()).thenReturn(false);
		when(bot.isSpawned()).thenReturn(false);
		Location spot = new Location(1000, 2000, 3000);
		AtomicReference<Runnable> scheduled = new AtomicReference<>();
		
		try (MockedStatic<PhantomGeo> geo = mockStatic(PhantomGeo.class); MockedStatic<ThreadPool> threadPool = mockStatic(ThreadPool.class))
		{
			geo.when(() -> PhantomGeo.getNpcLikeSpawn(spot)).thenReturn(null);
			threadPool.when(() -> ThreadPool.schedule(any(Runnable.class), anyLong())).thenAnswer(inv ->
			{
				scheduled.set(inv.getArgument(0));
				return mock(ScheduledFuture.class);
			});
			
			assertTrue(PhantomHuntingSpots.travelToLevelSpot(bot, spot, "hunt"));
			assertNull(PhantomState.HUNT_LEVEL_BAND.get(55));
			assertTrue(PhantomState.RETURN_UNTIL.containsKey(55));
			
			PhantomEngine.isRunning = true;
			PhantomEngine.activePhantoms.add(bot);
			scheduled.get().run();
			assertEquals(40, PhantomState.HUNT_LEVEL_BAND.get(55).intValue());
		}
	}

	@Test
	void refusedReturnCastLeavesHuntBandUnset()
	{
		Player bot = mock(Player.class);
		when(bot.getObjectId()).thenReturn(66);
		when(bot.getName()).thenReturn("Hunter");
		when(bot.getLevel()).thenReturn(42);
		PhantomState.RETURN_UNTIL.put(66, System.currentTimeMillis() + 60_000L);
		Location spot = new Location(1000, 2000, 3000);

		try (MockedStatic<PhantomGeo> geo = mockStatic(PhantomGeo.class))
		{
			geo.when(() -> PhantomGeo.getNpcLikeSpawn(spot)).thenReturn(spot);
			assertFalse(PhantomHuntingSpots.travelToLevelSpot(bot, spot, "hunt"));
			assertNull(PhantomState.HUNT_LEVEL_BAND.get(66));
		}
	}

	@Test
	void travelAwayFromWeakPreyRejectsNullBot()
	{
		assertFalse(PhantomHuntingSpots.travelAwayFromWeakPrey(null));
	}

	@Test
	void travelToLevelSpotRejectsNullBot()
	{
		assertFalse(PhantomHuntingSpots.travelToLevelSpot(null, new Location(1, 2, 3), "x"));
	}

	@Test
	void relocateForcesWhenInsidePeaceZone()
	{
		PhantomConfig.ROUTES.add(new PhantomConfig.FarmZone(40, 50, 111, 222, 333, 1, 2, 3));
		Player bot = mock(Player.class);
		when(bot.getObjectId()).thenReturn(81);
		when(bot.getName()).thenReturn("TownBot");
		when(bot.getLevel()).thenReturn(45);
		when(bot.getTarget()).thenReturn(null);
		when(bot.isInsideZone(ZoneId.PEACE)).thenReturn(true);
		when(bot.isInsideZone(ZoneId.TOWN)).thenReturn(false);
		when(bot.isOnline()).thenReturn(true);
		when(bot.isDead()).thenReturn(false);
		when(bot.isSpawned()).thenReturn(false);
		PhantomState.HUNT_LEVEL_BAND.put(81, 45);
		AtomicReference<Runnable> scheduled = new AtomicReference<>();

		try (MockedStatic<PhantomGeo> geo = mockStatic(PhantomGeo.class); MockedStatic<ThreadPool> threadPool = mockStatic(ThreadPool.class))
		{
			geo.when(() -> PhantomGeo.getNpcLikeSpawn(any(Location.class))).thenAnswer(inv -> inv.getArgument(0));
			threadPool.when(() -> ThreadPool.schedule(any(Runnable.class), anyLong())).thenAnswer(inv ->
			{
				scheduled.set(inv.getArgument(0));
				return mock(ScheduledFuture.class);
			});

			assertTrue(PhantomHuntingSpots.relocateForLevelIfNeeded(bot));
			PhantomEngine.isRunning = true;
			PhantomEngine.activePhantoms.add(bot);
			scheduled.get().run();
			assertEquals(45, PhantomState.HUNT_LEVEL_BAND.get(81).intValue());
		}
	}

	@Test
	void relocateSkipsWhenBandMatchesAndNoForce()
	{
		Player bot = mock(Player.class);
		when(bot.getObjectId()).thenReturn(82);
		when(bot.getLevel()).thenReturn(45);
		when(bot.getTarget()).thenReturn(null);
		when(bot.isInsideZone(ZoneId.PEACE)).thenReturn(false);
		when(bot.isInsideZone(ZoneId.TOWN)).thenReturn(false);
		PhantomState.HUNT_LEVEL_BAND.put(82, 45);
		assertFalse(PhantomHuntingSpots.relocateForLevelIfNeeded(bot));
	}

	@Test
	void weakPreyRelocateBypassesHuntTeleportLock()
	{
		PhantomConfig.ROUTES.add(new PhantomConfig.FarmZone(60, 70, 500, 600, 700, 1, 2, 3));
		Player bot = mock(Player.class);
		Npc gremlin = mock(Npc.class);
		when(bot.getObjectId()).thenReturn(71);
		when(bot.getName()).thenReturn("HighBot");
		when(bot.getLevel()).thenReturn(61);
		when(bot.getTarget()).thenReturn(gremlin);
		when(gremlin.isNpc()).thenReturn(true);
		when(gremlin.asNpc()).thenReturn(gremlin);
		when(gremlin.getLevel()).thenReturn(1);
		when(bot.isOnline()).thenReturn(true);
		when(bot.isDead()).thenReturn(false);
		when(bot.isSpawned()).thenReturn(false);
		PhantomState.NEXT_HUNT_TELEPORT.put(71, System.currentTimeMillis() + 30_000L);
		PhantomState.HUNT_LEVEL_BAND.put(71, 60);
		AtomicReference<Runnable> scheduled = new AtomicReference<>();

		try (MockedStatic<PhantomGeo> geo = mockStatic(PhantomGeo.class); MockedStatic<ThreadPool> threadPool = mockStatic(ThreadPool.class))
		{
			geo.when(() -> PhantomGeo.getNpcLikeSpawn(any(Location.class))).thenAnswer(inv -> inv.getArgument(0));
			threadPool.when(() -> ThreadPool.schedule(any(Runnable.class), anyLong())).thenAnswer(inv ->
			{
				scheduled.set(inv.getArgument(0));
				return mock(ScheduledFuture.class);
			});
			assertTrue(PhantomHuntingSpots.relocateForLevelIfNeeded(bot));
			PhantomEngine.isRunning = true;
			PhantomEngine.activePhantoms.add(bot);
			scheduled.get().run();
			assertTrue(PhantomState.NEXT_HUNT_TELEPORT.get(71) > System.currentTimeMillis() + 1000L);
		}
	}

	@Test
	void weakPreyPostLandCooldownSuppressesSecondHop()
	{
		PhantomConfig.ROUTES.add(new PhantomConfig.FarmZone(60, 70, 500, 600, 700, 1, 2, 3));
		Player bot = mock(Player.class);
		Npc gremlin = mock(Npc.class);
		when(bot.getObjectId()).thenReturn(72);
		when(bot.getName()).thenReturn("HighBot");
		when(bot.getLevel()).thenReturn(61);
		when(bot.getTarget()).thenReturn(null);
		when(bot.isInsideZone(ZoneId.PEACE)).thenReturn(false);
		when(bot.isInsideZone(ZoneId.TOWN)).thenReturn(false);
		when(bot.isOnline()).thenReturn(true);
		when(bot.isDead()).thenReturn(false);
		when(bot.isSpawned()).thenReturn(false);
		AtomicReference<Runnable> scheduled = new AtomicReference<>();

		try (MockedStatic<PhantomGeo> geo = mockStatic(PhantomGeo.class); MockedStatic<ThreadPool> threadPool = mockStatic(ThreadPool.class))
		{
			geo.when(() -> PhantomGeo.getNpcLikeSpawn(any(Location.class))).thenAnswer(inv -> inv.getArgument(0));
			threadPool.when(() -> ThreadPool.schedule(any(Runnable.class), anyLong())).thenAnswer(inv ->
			{
				scheduled.set(inv.getArgument(0));
				return mock(ScheduledFuture.class);
			});

			assertTrue(PhantomHuntingSpots.travelAwayFromWeakPrey(bot));
			PhantomEngine.isRunning = true;
			PhantomEngine.activePhantoms.add(bot);
			scheduled.get().run();

			assertFalse(PhantomHuntingSpots.travelAwayFromWeakPrey(bot));

			when(bot.getTarget()).thenReturn(gremlin);
			when(gremlin.isNpc()).thenReturn(true);
			when(gremlin.asNpc()).thenReturn(gremlin);
			when(gremlin.getLevel()).thenReturn(1);
			assertFalse(PhantomHuntingSpots.relocateForLevelIfNeeded(bot));
		}
	}
}
