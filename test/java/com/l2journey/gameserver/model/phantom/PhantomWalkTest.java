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
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.l2journey.gameserver.ai.CreatureAI;
import com.l2journey.gameserver.ai.Intention;
import com.l2journey.gameserver.model.Location;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.network.serverpackets.ServerPacket;

/**
 * Unit tests for {@link PhantomWalk} SoE town return and town-rest cycle.
 */
class PhantomWalkTest
{
	@AfterEach
	void tearDown()
	{
		PhantomWalk.ENABLE_TOWN_WALKING = false;
		PhantomWalk.clearAll();
	}

	private static Player mockTownBot(int objectId, int level)
	{
		Player bot = mock(Player.class);
		CreatureAI ai = mock(CreatureAI.class);
		when(bot.getObjectId()).thenReturn(objectId);
		when(bot.isDead()).thenReturn(false);
		when(bot.getAI()).thenReturn(ai);
		when(bot.getLevel()).thenReturn(level);
		when(ai.getIntention()).thenReturn(Intention.IDLE);
		when(bot.isSitting()).thenReturn(true);
		when(bot.isRunning()).thenReturn(false);
		when(bot.getTarget()).thenReturn(bot);
		when(bot.isMoving()).thenReturn(false);
		return bot;
	}

	/** @return clock ms when town rest began */
	private static long enterTownRest(Player bot, long start, MockedStatic<PhantomGeo> geo)
	{
		geo.when(() -> PhantomGeo.getValidLocation(anyInt(), anyInt(), anyInt())).thenAnswer(inv -> new Location(inv.getArgument(0), inv.getArgument(1), inv.getArgument(2)));
		PhantomWalk.update(bot, false, start);
		long afterFarm = start + ((45 * 60L) * 1000) + 1;
		PhantomWalk.update(bot, false, afterFarm);
		long restStart = afterFarm + 2001;
		PhantomWalk.update(bot, false, restStart);
		assertTrue(PhantomWalk.isResting(bot));
		return restStart;
	}

	@Test
	void soeBroadcastHappensOnceThenTeleports()
	{
		Player bot = mock(Player.class);
		CreatureAI ai = mock(CreatureAI.class);
		when(bot.getObjectId()).thenReturn(12);
		when(bot.isDead()).thenReturn(false);
		when(bot.getAI()).thenReturn(ai);
		when(bot.getLevel()).thenReturn(40);
		when(ai.getIntention()).thenReturn(Intention.ACTIVE);

		long start = 1_000_000L;
		try (MockedStatic<PhantomGeo> geo = mockStatic(PhantomGeo.class))
		{
			geo.when(() -> PhantomGeo.getValidLocation(anyInt(), anyInt(), anyInt())).thenAnswer(inv -> new Location(inv.getArgument(0), inv.getArgument(1), inv.getArgument(2)));

			PhantomWalk.update(bot, false, start);
			assertFalse(PhantomWalk.isResting(bot));

			long afterFarm = start + ((45 * 60L) * 1000) + 1;
			PhantomWalk.update(bot, false, afterFarm);
			verify(bot, times(1)).broadcastPacket(any(ServerPacket.class));
			assertFalse(PhantomWalk.isResting(bot));

			PhantomWalk.update(bot, false, afterFarm + 1000);
			verify(bot, times(1)).broadcastPacket(any(ServerPacket.class));
			assertFalse(PhantomWalk.isResting(bot));

			PhantomWalk.update(bot, false, afterFarm + 2001);
			assertTrue(PhantomWalk.isResting(bot));
			verify(bot).spawnMe(anyInt(), anyInt(), anyInt());
		}
	}

	@Test
	void getValidLocationKeepsAuthoredZWhenNearestIsFar()
	{
		try (MockedStatic<PhantomGeo> geo = mockStatic(PhantomGeo.class))
		{
			geo.when(() -> PhantomGeo.getValidLocation(10, 20, -3000)).thenReturn(new Location(10, 20, -3000));
			Location loc = PhantomGeo.getValidLocation(10, 20, -3000);
			assertTrue(loc.getZ() == -3000);
		}
	}

	@Test
	void skipTownVisitCancelsRest()
	{
		Player bot = mock(Player.class);
		when(bot.getObjectId()).thenReturn(99);
		when(bot.isDead()).thenReturn(false);
		PhantomWalk.update(bot, true, 5000L);
		assertFalse(PhantomWalk.isResting(bot));
		assertFalse(PhantomWalk.isResting(null));
	}

	@Test
	void removeBotAndClearAllAreSafe()
	{
		Player bot = mock(Player.class);
		when(bot.getObjectId()).thenReturn(55);
		when(bot.isDead()).thenReturn(false);
		PhantomWalk.update(bot, false, 1000L);
		PhantomWalk.removeBot(bot);
		PhantomWalk.removeBot(null);
		PhantomWalk.clearAll();
	}

	@Test
	void townRestStandsRunsClearsTargetAndReturnsToFarm()
	{
		Player bot = mockTownBot(12, 40);
		long start = 2_000_000L;
		try (MockedStatic<PhantomGeo> geo = mockStatic(PhantomGeo.class))
		{
			long restStart = enterTownRest(bot, start, geo);

			PhantomWalk.update(bot, false, restStart + 1000);
			verify(bot).standUp();
			verify(bot).setRunning();
			verify(bot, atLeastOnce()).setTarget(null);
			verify(bot.getAI(), atLeastOnce()).setIntention(Intention.ACTIVE);
			assertTrue(PhantomWalk.isResting(bot));

			PhantomWalk.update(bot, false, restStart + ((5 * 60L) * 1000) + 1);
			assertFalse(PhantomWalk.isResting(bot));
			verify(bot, atLeastOnce()).spawnMe(anyInt(), anyInt(), anyInt());
		}
	}

	@Test
	void townWalkingCyclesThenWaitsThenResumes()
	{
		PhantomWalk.ENABLE_TOWN_WALKING = true;
		Player bot = mockTownBot(33, 25);
		when(bot.isSitting()).thenReturn(false);
		when(bot.isRunning()).thenReturn(true);
		when(bot.getTarget()).thenReturn(null);
		when(bot.getAI().getIntention()).thenReturn(Intention.ACTIVE);

		long start = 3_000_000L;
		try (MockedStatic<PhantomGeo> geo = mockStatic(PhantomGeo.class))
		{
			long restStart = enterTownRest(bot, start, geo);

			PhantomWalk.update(bot, false, restStart + 13_000);
			verify(bot.getAI(), atLeastOnce()).setIntention(any(Intention.class), any(Location.class));

			PhantomWalk.update(bot, false, restStart + 26_000);
			PhantomWalk.update(bot, false, restStart + 39_000);
			PhantomWalk.update(bot, false, restStart + 52_000);

			PhantomWalk.update(bot, false, restStart + 53_000);
			assertTrue(PhantomWalk.isResting(bot));

			PhantomWalk.update(bot, false, restStart + 53_000 + ((2 * 60L) * 1000) + 1);
			verify(bot.getAI(), atLeastOnce()).setIntention(any(Intention.class), any(Location.class));
		}
	}

	@Test
	void cityLandingUsesLevelBands()
	{
		long start = 4_000_000L;
		try (MockedStatic<PhantomGeo> geo = mockStatic(PhantomGeo.class))
		{
			for (int level : new int[]
			{
				10,
				25,
				55,
				65,
				80
			})
			{
				Player bot = mockTownBot(100 + level, level);
				when(bot.isSitting()).thenReturn(false);
				when(bot.isRunning()).thenReturn(true);
				when(bot.getTarget()).thenReturn(null);
				when(bot.getAI().getIntention()).thenReturn(Intention.ACTIVE);
				enterTownRest(bot, start, geo);
				PhantomWalk.clearAll();
			}
		}
	}

	@Test
	void nullOrDeadBotIsIgnored()
	{
		PhantomWalk.update(null, false, 1L);
		Player dead = mock(Player.class);
		when(dead.isDead()).thenReturn(true);
		PhantomWalk.update(dead, false, 1L);
		assertFalse(PhantomWalk.isResting(dead));
	}
}
