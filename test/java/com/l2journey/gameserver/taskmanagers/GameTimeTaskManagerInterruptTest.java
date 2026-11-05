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
package com.l2journey.gameserver.taskmanagers;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.l2journey.commons.threads.ThreadPool;
import com.l2journey.gameserver.model.EffectList;
import com.l2journey.gameserver.model.World;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.enums.creature.Race;
import com.l2journey.gameserver.model.events.EventDispatcher;
import com.l2journey.gameserver.model.skill.CommonSkill;
import com.l2journey.gameserver.model.skill.Skill;
import com.l2journey.gameserver.model.skill.enums.SkillFinishType;

/**
 * Covers interrupt stop of {@link GameTimeTaskManager#runClock()} (java:S2142).
 */
class GameTimeTaskManagerInterruptTest
{
	@Test
	void runClockStopsOnInterruptAndUpdateGameTimeRuns() throws Exception
	{
		final CountDownLatch started = new CountDownLatch(1);
		final AtomicReference<Thread> worker = new AtomicReference<>();
		final GameTimeTaskManager manager = new GameTimeTaskManager()
		{
			@Override
			public void run()
			{
				worker.set(Thread.currentThread());
				started.countDown();
				try
				{
					Thread.sleep(60_000);
				}
				catch (InterruptedException _)
				{
					Thread.currentThread().interrupt();
				}
			}
		};
		
		try
		{
			assertTrue(started.await(5, TimeUnit.SECONDS));
			manager.updateGameTime();
			
			Thread.currentThread().interrupt();
			try
			{
				manager.runClock();
				assertTrue(Thread.currentThread().isInterrupted());
			}
			finally
			{
				Thread.interrupted();
			}
		}
		finally
		{
			manager.interrupt();
			final Thread thread = worker.get();
			if (thread != null)
			{
				thread.join(5000);
				assertFalse(thread.isAlive());
			}
		}
	}
	
	@Test
	void runDelegatesToRunClockAndStopsOnInterrupt() throws Exception
	{
		final CountDownLatch entered = new CountDownLatch(1);
		final GameTimeTaskManager manager = new GameTimeTaskManager()
		{
			@Override
			public void run()
			{
				entered.countDown();
				super.run();
			}
		};
		
		try
		{
			assertTrue(entered.await(5, TimeUnit.SECONDS));
			manager.interrupt();
			manager.workerThread().join(5000);
			assertFalse(manager.workerThread().isAlive());
		}
		finally
		{
			manager.interrupt();
			manager.workerThread().join(5000);
		}
	}
	
	@Test
	void onDayNightChangeWithEmptyWorld()
	{
		final CountDownLatch started = new CountDownLatch(1);
		final AtomicReference<Thread> worker = new AtomicReference<>();
		final GameTimeTaskManager manager = new GameTimeTaskManager()
		{
			@Override
			public void run()
			{
				worker.set(Thread.currentThread());
				started.countDown();
				try
				{
					Thread.sleep(60_000);
				}
				catch (InterruptedException _)
				{
					Thread.currentThread().interrupt();
				}
			}
		};
		
		try
		{
			assertTrue(started.await(5, TimeUnit.SECONDS));
			try (MockedStatic<ThreadPool> threadPool = mockStatic(ThreadPool.class);
				MockedStatic<EventDispatcher> events = mockStatic(EventDispatcher.class))
			{
				threadPool.when(() -> ThreadPool.execute(any(Runnable.class))).thenAnswer(_ -> null);
				final EventDispatcher dispatcher = mock(EventDispatcher.class);
				events.when(EventDispatcher::getInstance).thenReturn(dispatcher);
				when(dispatcher.hasListener(any())).thenReturn(false);
				
				when(dispatcher.hasListener(any())).thenReturn(true);
				final boolean before = manager.isNight();
				manager.onDayNightChange();
				assertTrue(manager.isNight() != before);
				verify(dispatcher).notifyEventAsync(any());
			}
		}
		catch (InterruptedException e)
		{
			Thread.currentThread().interrupt();
			throw new AssertionError(e);
		}
		finally
		{
			manager.interrupt();
			final Thread thread = worker.get();
			if (thread != null)
			{
				try
				{
					thread.join(5000);
				}
				catch (InterruptedException e)
				{
					Thread.currentThread().interrupt();
				}
			}
		}
	}
	
	@Test
	void onDayNightChangeAppliesShadowSenseForDarkElf()
	{
		final CountDownLatch started = new CountDownLatch(1);
		final AtomicReference<Thread> worker = new AtomicReference<>();
		final GameTimeTaskManager manager = new GameTimeTaskManager()
		{
			@Override
			public void run()
			{
				worker.set(Thread.currentThread());
				started.countDown();
				try
				{
					Thread.sleep(60_000);
				}
				catch (InterruptedException _)
				{
					Thread.currentThread().interrupt();
				}
			}
		};
		
		try
		{
			assertTrue(started.await(5, TimeUnit.SECONDS));
			
			final Player player = mock(Player.class);
			final Skill shadowSense = mock(Skill.class);
			final EffectList effects = mock(EffectList.class);
			when(player.getRace()).thenReturn(Race.DARK_ELF);
			when(player.getKnownSkill(CommonSkill.SHADOW_SENSE.getId())).thenReturn(shadowSense);
			when(player.getEffectList()).thenReturn(effects);
			
			try (MockedStatic<World> worldStatic = mockStatic(World.class);
				MockedStatic<ThreadPool> threadPool = mockStatic(ThreadPool.class);
				MockedStatic<EventDispatcher> events = mockStatic(EventDispatcher.class))
			{
				final World world = mock(World.class);
				worldStatic.when(World::getInstance).thenReturn(world);
				when(world.getPlayers()).thenReturn(List.of(player));
				
				threadPool.when(() -> ThreadPool.execute(any(Runnable.class))).thenAnswer(_ -> null);
				final EventDispatcher dispatcher = mock(EventDispatcher.class);
				events.when(EventDispatcher::getInstance).thenReturn(dispatcher);
				when(dispatcher.hasListener(any())).thenReturn(false);
				
				// Flip to night (apply skill) then day (remove skill).
				if (!manager.isNight())
				{
					manager.onDayNightChange();
				}
				verify(player).addSkill(shadowSense, false);
				
				manager.onDayNightChange();
				verify(effects).stopSkillEffects(SkillFinishType.NORMAL, shadowSense);
			}
		}
		catch (InterruptedException e)
		{
			Thread.currentThread().interrupt();
			throw new AssertionError(e);
		}
		finally
		{
			manager.interrupt();
			final Thread thread = worker.get();
			if (thread != null)
			{
				try
				{
					thread.join(5000);
				}
				catch (InterruptedException e)
				{
					Thread.currentThread().interrupt();
				}
			}
		}
	}
}
