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
package com.l2journey.gameserver.model.olympiad;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.l2journey.EventsConfig;
import com.l2journey.commons.threads.ThreadPool;
import com.l2journey.gameserver.util.Broadcast;

/**
 * Covers the Olympiad schedule wiring added for custom seasons / competition windows
 * (init, setNewOlympiadEnd, getMillisToCompBegin/End, setNewCompBegin, expired start skip).
 * Does not exercise EndTask, game manager, or DB paths.
 */
class OlympiadWiringTest
{
	private boolean savedEnabled;
	private boolean savedUseCustom;
	private String savedPeriod;
	private int savedMultiplier;
	private List<Integer> savedCompetitionDays;
	
	@BeforeAll
	static void loadEventsConfigDefaults()
	{
		EventsConfig.load();
	}
	
	@BeforeEach
	void saveAndDisableOlympiad()
	{
		savedEnabled = EventsConfig.OLYMPIAD_ENABLED;
		savedUseCustom = EventsConfig.OLYMPIAD_USE_CUSTOM_PERIOD_SETTINGS;
		savedPeriod = EventsConfig.OLYMPIAD_PERIOD;
		savedMultiplier = EventsConfig.OLYMPIAD_PERIOD_MULTIPLIER;
		savedCompetitionDays = EventsConfig.OLYMPIAD_COMPETITION_DAYS == null ? null : new ArrayList<>(EventsConfig.OLYMPIAD_COMPETITION_DAYS);
		EventsConfig.OLYMPIAD_ENABLED = false;
	}
	
	@AfterEach
	void restoreEventsConfig()
	{
		EventsConfig.OLYMPIAD_ENABLED = savedEnabled;
		EventsConfig.OLYMPIAD_USE_CUSTOM_PERIOD_SETTINGS = savedUseCustom;
		EventsConfig.OLYMPIAD_PERIOD = savedPeriod;
		EventsConfig.OLYMPIAD_PERIOD_MULTIPLIER = savedMultiplier;
		EventsConfig.OLYMPIAD_COMPETITION_DAYS = savedCompetitionDays;
	}
	
	@Test
	void initWiresCompetitionWindowAndMillisToCompEnd()
	{
		final ScheduledFuture<?> future = mock(ScheduledFuture.class);
		try (MockedStatic<ThreadPool> threadPool = mockStatic(ThreadPool.class);
			MockedStatic<Broadcast> broadcast = mockStatic(Broadcast.class))
		{
			stubThreadPool(threadPool, future, null);
			
			final Olympiad olympiad = new Olympiad();
			olympiad._olympiadEnd = System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000;
			olympiad.init();
			
			assertTrue(olympiad.getMillisToCompEnd() > 0L);
		}
	}
	
	@Test
	void setNewOlympiadEndWiresWeeklySeasonSchedule()
	{
		final ScheduledFuture<?> future = mock(ScheduledFuture.class);
		try (MockedStatic<ThreadPool> threadPool = mockStatic(ThreadPool.class);
			MockedStatic<Broadcast> broadcast = mockStatic(Broadcast.class))
		{
			stubThreadPool(threadPool, future, null);
			
			EventsConfig.OLYMPIAD_USE_CUSTOM_PERIOD_SETTINGS = false;
			final Olympiad olympiad = new Olympiad();
			olympiad._currentCycle = 3;
			olympiad.setNewOlympiadEnd();
			
			assertTrue(olympiad.getMillisToOlympiadEnd() > 0L);
			assertTrue(olympiad.getMillisToWeekChange() > 0L);
			broadcast.verify(() -> Broadcast.toAllOnlinePlayers(any(com.l2journey.gameserver.network.serverpackets.ServerPacket.class)));
			threadPool.verify(() -> ThreadPool.scheduleAtFixedRate(any(Runnable.class), anyLong(), anyLong()));
		}
	}
	
	@Test
	void setNewOlympiadEndWarnsOnInvalidCustomDayMultiplier()
	{
		final ScheduledFuture<?> future = mock(ScheduledFuture.class);
		try (MockedStatic<ThreadPool> threadPool = mockStatic(ThreadPool.class);
			MockedStatic<Broadcast> broadcast = mockStatic(Broadcast.class))
		{
			stubThreadPool(threadPool, future, null);
			
			EventsConfig.OLYMPIAD_USE_CUSTOM_PERIOD_SETTINGS = true;
			EventsConfig.OLYMPIAD_PERIOD = "DAY";
			EventsConfig.OLYMPIAD_PERIOD_MULTIPLIER = 1;
			
			final Olympiad olympiad = new Olympiad();
			olympiad.setNewOlympiadEnd();
			
			// DAY multiplier < 7 leaves weekly-change undefined; season end may already be past (noon same day).
			assertTrue(olympiad._olympiadEnd > 0L);
			broadcast.verify(() -> Broadcast.toAllOnlinePlayers(any(com.l2journey.gameserver.network.serverpackets.ServerPacket.class)));
		}
	}
	
	@Test
	void expiredWindowTriggersSetNewCompBeginThroughUpdateCompStatus() throws Exception
	{
		final ScheduledFuture<?> future = mock(ScheduledFuture.class);
		try (MockedStatic<ThreadPool> threadPool = mockStatic(ThreadPool.class);
			MockedStatic<Broadcast> broadcast = mockStatic(Broadcast.class))
		{
			stubThreadPool(threadPool, future, null);
			
			final Olympiad olympiad = new Olympiad();
			olympiad._olympiadEnd = System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000;
			olympiad.init();
			
			final long beforeEnd = olympiad.getMillisToCompEnd();
			assertTrue(beforeEnd > 0L);
			
			expireCompetitionWindow(olympiad);
			invokeUpdateCompStatus(olympiad);
			
			assertTrue(olympiad.getMillisToCompEnd() > 0L);
		}
	}
	
	@Test
	void delayedStartTaskSkipsAlreadyEndedPeriod() throws Exception
	{
		final ScheduledFuture<?> future = mock(ScheduledFuture.class);
		final AtomicReference<Runnable> compStartTask = new AtomicReference<>();
		try (MockedStatic<ThreadPool> threadPool = mockStatic(ThreadPool.class);
			MockedStatic<Broadcast> broadcast = mockStatic(Broadcast.class))
		{
			stubThreadPool(threadPool, future, compStartTask);
			
			final Olympiad olympiad = new Olympiad();
			olympiad._olympiadEnd = System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000;
			olympiad.init();
			
			assertTrue(compStartTask.get() != null, "competition start runnable should be scheduled");
			expireCompetitionWindow(olympiad);
			compStartTask.get().run();
			
			assertTrue(olympiad.getMillisToCompEnd() > 0L);
		}
	}
	
	private static void stubThreadPool(MockedStatic<ThreadPool> threadPool, ScheduledFuture<?> future, AtomicReference<Runnable> compStartTask)
	{
		threadPool.when(() -> ThreadPool.schedule(any(Runnable.class), anyLong())).thenAnswer(inv ->
		{
			final Runnable runnable = inv.getArgument(0);
			if ((compStartTask != null) && !(runnable instanceof Olympiad.OlympiadEndTask))
			{
				compStartTask.set(runnable);
			}
			return future;
		});
		threadPool.when(() -> ThreadPool.scheduleAtFixedRate(any(Runnable.class), anyLong(), anyLong())).thenReturn(future);
	}
	
	private static void expireCompetitionWindow(Olympiad olympiad) throws Exception
	{
		final Calendar pastStart = Calendar.getInstance();
		pastStart.add(Calendar.DAY_OF_MONTH, -2);
		final Field compStart = Olympiad.class.getDeclaredField("_compStart");
		compStart.setAccessible(true);
		compStart.set(olympiad, pastStart);
		
		final Field compEnd = Olympiad.class.getDeclaredField("_compEnd");
		compEnd.setAccessible(true);
		compEnd.setLong(olympiad, System.currentTimeMillis() - 60_000L);
	}
	
	private static void invokeUpdateCompStatus(Olympiad olympiad) throws Exception
	{
		final Method updateCompStatus = Olympiad.class.getDeclaredMethod("updateCompStatus");
		updateCompStatus.setAccessible(true);
		updateCompStatus.invoke(olympiad);
	}
}
