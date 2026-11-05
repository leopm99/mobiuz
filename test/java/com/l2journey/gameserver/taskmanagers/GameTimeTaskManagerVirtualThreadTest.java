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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

/**
 * Covers {@link GameTimeTaskManager} virtual-thread lifecycle (java:S6881) without running the clock loop.
 */
class GameTimeTaskManagerVirtualThreadTest
{
	private static final AtomicReference<CountDownLatch> LATCH = new AtomicReference<>();
	private static final AtomicReference<Thread> WORKER = new AtomicReference<>();
	private static final AtomicReference<AssertionError> FAILURE = new AtomicReference<>();
	
	@Test
	void startsOnNamedVirtualThread() throws Exception
	{
		final CountDownLatch latch = new CountDownLatch(1);
		LATCH.set(latch);
		WORKER.set(null);
		FAILURE.set(null);
		
		final GameTimeTaskManager manager;
		try
		{
			manager = new TestGameTimeTaskManager();
			final Object asObject = manager;
			assertFalse(asObject instanceof Thread);
			assertTrue(latch.await(5, TimeUnit.SECONDS));
			if (FAILURE.get() != null)
			{
				throw FAILURE.get();
			}
			
			final Thread recorded = WORKER.get();
			assertTrue(recorded.isVirtual());
			assertTrue(recorded.isDaemon());
			assertEquals("GameTimeTaskManager", recorded.getName());
			
			manager.interrupt();
			recorded.join(5000);
			assertFalse(recorded.isAlive());
		}
		finally
		{
			LATCH.set(null);
			WORKER.set(null);
			FAILURE.set(null);
		}
	}
	
	private static final class TestGameTimeTaskManager extends GameTimeTaskManager
	{
		@Override
		public void run()
		{
			final Thread current = Thread.currentThread();
			WORKER.set(current);
			try
			{
				if (!current.isVirtual())
				{
					throw new AssertionError("expected virtual thread");
				}
				if (!current.isDaemon())
				{
					throw new AssertionError("expected daemon thread");
				}
				if (!"GameTimeTaskManager".equals(current.getName()))
				{
					throw new AssertionError("unexpected name: " + current.getName());
				}
			}
			catch (AssertionError e)
			{
				FAILURE.set(e);
			}
			finally
			{
				final CountDownLatch latch = LATCH.get();
				if (latch != null)
				{
					latch.countDown();
				}
			}
			
			try
			{
				Thread.sleep(60_000);
			}
			catch (InterruptedException _)
			{
				Thread.currentThread().interrupt();
			}
		}
	}
}
