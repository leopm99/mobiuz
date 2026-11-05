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
package com.l2journey.gameserver.model.undergroundColiseum;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.l2journey.commons.threads.ThreadPool;
import com.l2journey.gameserver.data.SpawnTable;

/**
 * Covers interrupt handling in {@link UCArena#prepareStart} (java:S2142).
 */
class UCArenaInterruptTest
{
	@Test
	void prepareStartReturnsOnFirstInterruptedPause()
	{
		try (MockedStatic<SpawnTable> spawns = mockStatic(SpawnTable.class))
		{
			final SpawnTable spawnTable = mock(SpawnTable.class);
			spawns.when(SpawnTable::getInstance).thenReturn(spawnTable);
			when(spawnTable.getAnySpawn(anyInt())).thenReturn(null);
			
			final UCArena arena = new UCArena(1, 0, 40, 50);
			arena.setUCTeam(0, new UCTeam(0, arena, 0, 0, 0, 0));
			arena.setUCTeam(1, new UCTeam(1, arena, 0, 0, 0, 0));
			
			final AtomicInteger pauses = new AtomicInteger();
			arena.prepareStart(_ ->
			{
				pauses.incrementAndGet();
				return false;
			});
			
			assertEquals(1, pauses.get());
		}
	}
	
	@Test
	void prepareStartReturnsOnEachLaterInterruptedPause()
	{
		try (MockedStatic<SpawnTable> spawns = mockStatic(SpawnTable.class))
		{
			final SpawnTable spawnTable = mock(SpawnTable.class);
			spawns.when(SpawnTable::getInstance).thenReturn(spawnTable);
			when(spawnTable.getAnySpawn(anyInt())).thenReturn(null);
			
			for (int failAt = 2; failAt <= 8; failAt++)
			{
				final UCArena arena = new UCArena(1, 0, 40, 50);
				arena.setUCTeam(0, new UCTeam(0, arena, 0, 0, 0, 0));
				arena.setUCTeam(1, new UCTeam(1, arena, 0, 0, 0, 0));
				
				final int stopAfter = failAt;
				final AtomicInteger pauses = new AtomicInteger();
				arena.prepareStart(_ -> pauses.incrementAndGet() < stopAfter);
				assertEquals(failAt, pauses.get());
			}
		}
	}
	
	@Test
	void prepareStartPublicDelegatesToPredicateOverload()
	{
		final UCArena arena = new UCArena(1, 0, 40, 50)
		{
			@Override
			void prepareStart(java.util.function.LongPredicate pause)
			{
				assertTrue(pause.test(1));
			}
		};
		arena.prepareStart();
	}
	
	@Test
	void prepareStartCompletesCountdownAndCancelsScheduledTask()
	{
		final ScheduledFuture<?> future = mock(ScheduledFuture.class);
		try (MockedStatic<SpawnTable> spawns = mockStatic(SpawnTable.class);
			MockedStatic<ThreadPool> threadPool = mockStatic(ThreadPool.class))
		{
			final SpawnTable spawnTable = mock(SpawnTable.class);
			spawns.when(SpawnTable::getInstance).thenReturn(spawnTable);
			when(spawnTable.getAnySpawn(anyInt())).thenReturn(null);
			threadPool.when(() -> ThreadPool.schedule(any(Runnable.class), anyLong())).thenReturn(future);
			
			final UCArena arena = new UCArena(1, 0, 40, 50);
			arena.setUCTeam(0, new UCTeam(0, arena, 0, 0, 0, 0));
			arena.setUCTeam(1, new UCTeam(1, arena, 0, 0, 0, 0));
			
			final AtomicInteger pauses = new AtomicInteger();
			try
			{
				arena.prepareStart(_ ->
				{
					pauses.incrementAndGet();
					return true;
				});
				assertEquals(8, pauses.get());
			}
			finally
			{
				arena.cancelTask();
			}
			
			verify(future).cancel(false);
		}
	}
}
