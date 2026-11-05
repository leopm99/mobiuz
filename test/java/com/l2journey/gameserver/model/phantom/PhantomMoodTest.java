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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.l2journey.commons.util.Rnd;
import com.l2journey.gameserver.model.actor.Player;

/**
 * Unit tests for {@link PhantomMood}.
 */
class PhantomMoodTest
{
	@AfterEach
	void tearDown()
	{
		PhantomMood.clear();
	}

	@Test
	void nullBotDefaultsToNeutral()
	{
		assertEquals(PhantomMood.Mood.NEUTRAL, PhantomMood.getMood(null));
		assertFalse(PhantomMood.shouldPauseThink(null));
		assertFalse(PhantomMood.makesMistake(null, 100));
		assertEquals(1.0, PhantomMood.getTalkFactor(null));
		assertTrue(PhantomMood.canReactNow(null));
		assertFalse(PhantomMood.isAfk(null));
		assertFalse(PhantomMood.isDistracted(null));
		PhantomMood.setMood(null, PhantomMood.Mood.ANGRY);
		PhantomMood.onDeath(null);
	}

	@Test
	void setMoodIgnoresNullMood()
	{
		Player bot = mockBot(10);
		PhantomMood.setMood(bot, null);
		assertEquals(PhantomMood.Mood.NEUTRAL, PhantomMood.getMood(bot));
	}

	@Test
	void levelUpSetsCheerful()
	{
		Player bot = mockBot(1);
		PhantomMood.onGoodThing(bot);
		assertEquals(PhantomMood.Mood.CHEERFUL, PhantomMood.getMood(bot));
		assertEquals(1.8, PhantomMood.getTalkFactor(bot));
		assertEquals(0.8, PhantomMood.Mood.CHEERFUL.getMistakeFactor());
		assertEquals(1.0, PhantomMood.Mood.CHEERFUL.getReactionFactor());
	}

	@Test
	void oneDeathSetsAnnoyedThreeSetAngry()
	{
		Player bot = mockBot(2);
		PhantomMood.onDeath(bot);
		assertEquals(PhantomMood.Mood.ANNOYED, PhantomMood.getMood(bot));
		PhantomMood.onDeath(bot);
		PhantomMood.onDeath(bot);
		assertEquals(PhantomMood.Mood.ANGRY, PhantomMood.getMood(bot));
	}

	@Test
	void deathWindowResetsRecentDeaths()
	{
		Player bot = mockBot(20);
		PhantomMood.forceLastDeathAt(20, System.currentTimeMillis() - 700000L);
		PhantomMood.onDeath(bot);
		assertEquals(PhantomMood.Mood.ANNOYED, PhantomMood.getMood(bot));
	}

	@Test
	void moodExpiresBackToNeutral()
	{
		Player bot = mockBot(21);
		PhantomMood.setMood(bot, PhantomMood.Mood.ANGRY);
		PhantomMood.forceMoodUntil(21, System.currentTimeMillis() - 1);
		assertEquals(PhantomMood.Mood.NEUTRAL, PhantomMood.getMood(bot));
	}

	@Test
	void longSessionReturnsTired()
	{
		Player bot = mockBot(22);
		PhantomMood.forceSessionStartedAt(22, System.currentTimeMillis() - 10_000_000L);
		assertEquals(PhantomMood.Mood.TIRED, PhantomMood.getMood(bot));
		assertEquals(0.5, PhantomMood.getTalkFactor(bot));
	}

	@Test
	void canReactNowBlocksUntilDelayElapses()
	{
		Player bot = mockBot(3);
		assertTrue(PhantomMood.canReactNow(bot));
		assertFalse(PhantomMood.canReactNow(bot));
	}

	@Test
	void afkSkippedWhileInCombatOrLowHp()
	{
		Player bot = mockBot(4);
		when(bot.isInCombat()).thenReturn(true);
		when(bot.getCurrentHp()).thenReturn(100.0);
		when(bot.getMaxHp()).thenReturn(100);
		assertFalse(PhantomMood.isAfk(bot));

		when(bot.isInCombat()).thenReturn(false);
		when(bot.getCurrentHp()).thenReturn(50.0);
		assertFalse(PhantomMood.isAfk(bot));
	}

	@Test
	void forcedAfkAndDistractPauseThink()
	{
		Player bot = mockBot(30);
		when(bot.isInCombat()).thenReturn(true);
		PhantomMood.forceAfkUntil(30, System.currentTimeMillis() + 60_000L);
		assertTrue(PhantomMood.isAfk(bot));
		assertTrue(PhantomMood.shouldPauseThink(bot));

		PhantomMood.clear();
		Player bot2 = mockBot(31);
		when(bot2.isInCombat()).thenReturn(true);
		PhantomMood.forceDistractedUntil(31, System.currentTimeMillis() + 5_000L);
		assertTrue(PhantomMood.isDistracted(bot2));
		PhantomMood.canReactNow(bot2);
		assertTrue(PhantomMood.shouldPauseThink(bot2));
	}

	@Test
	void afkAndDistractRollGates()
	{
		Player bot = mockBot(32);
		when(bot.isInCombat()).thenReturn(false);
		when(bot.getCurrentHp()).thenReturn(100.0);
		when(bot.getMaxHp()).thenReturn(100);
		try (MockedStatic<Rnd> rnd = mockStatic(Rnd.class))
		{
			rnd.when(() -> Rnd.get(100)).thenReturn(50);
			rnd.when(() -> Rnd.get(1000)).thenReturn(50);
			assertFalse(PhantomMood.isAfk(bot));
			assertFalse(PhantomMood.isAfk(bot));
			assertFalse(PhantomMood.isDistracted(bot));
			assertFalse(PhantomMood.isDistracted(bot));
		}
	}

	@Test
	void afkAndDistractRollHitsStayActive()
	{
		Player bot = mockBot(33);
		when(bot.isInCombat()).thenReturn(false);
		when(bot.getCurrentHp()).thenReturn(100.0);
		when(bot.getMaxHp()).thenReturn(100);
		try (MockedStatic<Rnd> rnd = mockStatic(Rnd.class))
		{
			rnd.when(() -> Rnd.get(100)).thenReturn(0);
			rnd.when(() -> Rnd.get(anyInt(), anyInt())).thenReturn(60_000);
			assertTrue(PhantomMood.isAfk(bot));
			assertTrue(PhantomMood.isAfk(bot));

			PhantomMood.clear();
			rnd.when(() -> Rnd.get(1000)).thenReturn(0);
			rnd.when(() -> Rnd.get(anyInt(), anyInt())).thenReturn(5_000);
			assertTrue(PhantomMood.isDistracted(bot));
			assertTrue(PhantomMood.isDistracted(bot));
		}
	}

	@Test
	void mistakeUsesMoodFactor()
	{
		Player bot = mockBot(5);
		PhantomMood.setMood(bot, PhantomMood.Mood.ANGRY);
		boolean sawTrue = false;
		boolean sawFalse = false;
		for (int i = 0; i < 200; i++)
		{
			if (PhantomMood.makesMistake(bot, 10))
			{
				sawTrue = true;
			}
			else
			{
				sawFalse = true;
			}
		}
		assertTrue(sawTrue);
		assertTrue(sawFalse);
	}

	@Test
	void unregisterRemovesState()
	{
		Player bot = mockBot(6);
		PhantomMood.onGoodThing(bot);
		PhantomMood.unregister(6);
		assertEquals(PhantomMood.Mood.NEUTRAL, PhantomMood.getMood(bot));
	}

	@Test
	void shouldPauseThinkWhenCannotReact()
	{
		Player bot = mockBot(7);
		when(bot.isInCombat()).thenReturn(true);
		when(bot.getCurrentHp()).thenReturn(100.0);
		when(bot.getMaxHp()).thenReturn(100);
		assertTrue(PhantomMood.canReactNow(bot));
		assertTrue(PhantomMood.shouldPauseThink(bot));
	}

	@Test
	void moodEnumFactorsAreExposed()
	{
		assertEquals("neutral", label(PhantomMood.Mood.NEUTRAL));
		assertEquals(1.0, PhantomMood.Mood.NEUTRAL.getReactionFactor());
		assertEquals(2.0, PhantomMood.Mood.ANGRY.getMistakeFactor());
		assertEquals(0.7, PhantomMood.Mood.ANNOYED.getTalkFactor());
	}

	private static String label(PhantomMood.Mood mood)
	{
		return mood.name().toLowerCase();
	}

	private static Player mockBot(int objectId)
	{
		Player bot = mock(Player.class);
		when(bot.getObjectId()).thenReturn(objectId);
		when(bot.isInCombat()).thenReturn(false);
		when(bot.getCurrentHp()).thenReturn(100.0);
		when(bot.getMaxHp()).thenReturn(100);
		return bot;
	}
}
