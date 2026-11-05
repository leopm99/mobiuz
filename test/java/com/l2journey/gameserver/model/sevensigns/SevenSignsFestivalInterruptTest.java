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
package com.l2journey.gameserver.model.sevensigns;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.l2journey.Config;

/**
 * Covers interrupt restoration in {@link SevenSignsFestival.FestivalManager} (java:S2142).
 */
class SevenSignsFestivalInterruptTest
{
	@BeforeAll
	static void festivalConfigBeforeClassLoad()
	{
		Config.ALT_FESTIVAL_CYCLE_LENGTH = 2_280_000L;
		Config.ALT_FESTIVAL_LENGTH = 1_080_000L;
		Config.ALT_FESTIVAL_MANAGER_START = 120_000L;
		Config.ALT_FESTIVAL_FIRST_SPAWN = 120_000L;
		Config.ALT_FESTIVAL_FIRST_SWARM = 300_000L;
		Config.ALT_FESTIVAL_SECOND_SPAWN = 540_000L;
		Config.ALT_FESTIVAL_SECOND_SWARM = 720_000L;
		Config.ALT_FESTIVAL_CHEST_SPAWN = 900_000L;
	}
	
	@Test
	void festivalManagerPauseRestoresInterrupt()
	{
		final SevenSigns sevenSigns = mock(SevenSigns.class);
		when(sevenSigns.getCurrentCycle()).thenReturn(1);
		when(sevenSigns.isSealValidationPeriod()).thenReturn(true);
		
		try (MockedStatic<SevenSigns> sevenSignsStatic = mockStatic(SevenSigns.class))
		{
			sevenSignsStatic.when(SevenSigns::getInstance).thenReturn(sevenSigns);
			
			final SevenSignsFestival festival = new SevenSignsFestival();
			final SevenSignsFestival.FestivalManager manager = festival.newFestivalManager();
			Thread.currentThread().interrupt();
			try
			{
				assertFalse(manager.pause(60_000));
				assertTrue(Thread.currentThread().isInterrupted());
			}
			finally
			{
				Thread.interrupted();
			}
		}
	}
	
	@Test
	void festivalManagerPauseCompletesWithoutInterrupt()
	{
		final SevenSigns sevenSigns = mock(SevenSigns.class);
		when(sevenSigns.getCurrentCycle()).thenReturn(1);
		when(sevenSigns.isSealValidationPeriod()).thenReturn(true);
		
		try (MockedStatic<SevenSigns> sevenSignsStatic = mockStatic(SevenSigns.class))
		{
			sevenSignsStatic.when(SevenSigns::getInstance).thenReturn(sevenSigns);
			
			final SevenSignsFestival festival = new SevenSignsFestival();
			final SevenSignsFestival.FestivalManager manager = festival.newFestivalManager();
			assertTrue(manager.pause(1));
			assertFalse(Thread.currentThread().isInterrupted());
		}
	}
	
	@Test
	void festivalManagerRunRestoresInterruptOnFirstPause()
	{
		final SevenSigns sevenSigns = mock(SevenSigns.class);
		when(sevenSigns.getCurrentCycle()).thenReturn(1);
		when(sevenSigns.isSealValidationPeriod()).thenReturn(true);
		
		try (MockedStatic<SevenSigns> sevenSignsStatic = mockStatic(SevenSigns.class))
		{
			sevenSignsStatic.when(SevenSigns::getInstance).thenReturn(sevenSigns);
			
			final SevenSignsFestival festival = new SevenSignsFestival();
			when(sevenSigns.isSealValidationPeriod()).thenReturn(false);
			when(sevenSigns.getMilliToPeriodChange()).thenReturn(Config.ALT_FESTIVAL_CYCLE_LENGTH + 1);
			
			final SevenSignsFestival.FestivalManager manager = festival.newFestivalManager();
			Thread.currentThread().interrupt();
			try
			{
				manager.run();
				assertTrue(Thread.currentThread().isInterrupted());
			}
			finally
			{
				Thread.interrupted();
			}
		}
	}
	
	@Test
	void festivalManagerRunWalksUntilFirstSpawnPause()
	{
		final SevenSigns sevenSigns = mock(SevenSigns.class);
		when(sevenSigns.getCurrentCycle()).thenReturn(1);
		when(sevenSigns.isSealValidationPeriod()).thenReturn(true);
		
		try (MockedStatic<SevenSigns> sevenSignsStatic = mockStatic(SevenSigns.class))
		{
			sevenSignsStatic.when(SevenSigns::getInstance).thenReturn(sevenSigns);
			
			final SevenSignsFestival festival = new SevenSignsFestival();
			when(sevenSigns.isSealValidationPeriod()).thenReturn(false);
			when(sevenSigns.getMilliToPeriodChange()).thenReturn(Config.ALT_FESTIVAL_CYCLE_LENGTH + 1);
			
			final SevenSignsFestival.FestivalManager manager = festival.newFestivalManager();
			final AtomicInteger pauses = new AtomicInteger();
			manager._pauseHook = _ ->
			{
				final int step = pauses.getAndIncrement();
				// Complete signup wait, then abort on the empty-party cycle wait (no DarknessFestival spawn).
				return step == 0;
			};
			
			manager.run();
			assertTrue(pauses.get() >= 2);
			assertFalse(festival.isFestivalInitialized());
		}
	}
	
	@Test
	void festivalManagerRunAbortsAtEachPostSignupPauseWithEmptyParticipants()
	{
		final SevenSigns sevenSigns = mock(SevenSigns.class);
		when(sevenSigns.getCurrentCycle()).thenReturn(1);
		when(sevenSigns.isSealValidationPeriod()).thenReturn(true);
		
		try (MockedStatic<SevenSigns> sevenSignsStatic = mockStatic(SevenSigns.class))
		{
			sevenSignsStatic.when(SevenSigns::getInstance).thenReturn(sevenSigns);
			
			final SevenSignsFestival festival = new SevenSignsFestival();
			when(sevenSigns.isSealValidationPeriod()).thenReturn(false);
			when(sevenSigns.getMilliToPeriodChange()).thenReturn(Config.ALT_FESTIVAL_CYCLE_LENGTH + 1);
			
			// Empty participant lists still create DarknessFestival instances; witch spawn fails safely.
			// festivalStart / swarm / chest pauses run against empty instance mob lists (no live spawns).
			for (int failAt = 1; failAt <= 6; failAt++)
			{
				festival._duskFestivalParticipants.clear();
				festival._dawnFestivalParticipants.clear();
				festival._duskFestivalParticipants.put(0, new ArrayList<>());
				
				final SevenSignsFestival.FestivalManager manager = festival.newFestivalManager();
				final int stopAfter = failAt;
				final AtomicInteger pauses = new AtomicInteger();
				manager._pauseHook = _ -> pauses.getAndIncrement() < stopAfter;
				
				manager.run();
				assertTrue(pauses.get() >= failAt, "expected abort at pause " + failAt);
			}
		}
	}
	
	@Test
	void festivalManagerRunRestoresInterruptFromOuterCatch()
	{
		final SevenSigns sevenSigns = mock(SevenSigns.class);
		when(sevenSigns.getCurrentCycle()).thenReturn(1);
		when(sevenSigns.isSealValidationPeriod()).thenReturn(true);
		
		try (MockedStatic<SevenSigns> sevenSignsStatic = mockStatic(SevenSigns.class))
		{
			sevenSignsStatic.when(SevenSigns::getInstance).thenReturn(sevenSigns);
			
			final SevenSignsFestival festival = new SevenSignsFestival();
			final SevenSignsFestival.FestivalManager manager = festival.newFestivalManager();
			manager._testException = new InterruptedException("test");
			try
			{
				manager.run();
				assertTrue(Thread.currentThread().isInterrupted());
			}
			finally
			{
				Thread.interrupted();
			}
		}
	}
}
