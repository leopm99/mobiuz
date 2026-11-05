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
package com.l2journey.gameserver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.l2journey.Config;

/**
 * Covers {@link LoginServerThread} virtual-thread lifecycle (java:S6881) without opening a login socket.
 */
class LoginServerThreadVirtualThreadTest
{
	@AfterEach
	void clearHexId()
	{
		Config.HEX_ID = null;
	}
	
	@Test
	void constructorUsesServerIdWhenHexIdPresent()
	{
		Config.HEX_ID = new byte[]
		{
			1,
			2,
			3,
			4
		};
		Config.SERVER_ID = 7;
		final LoginServerThread login = new LoginServerThread()
		{
			@Override
			public void run()
			{
				// Do not connect.
			}
		};
		final Object asObject = login;
		assertFalse(asObject instanceof Thread);
		assertFalse(login.isInterrupted());
	}
	
	@Test
	void startsOnNamedVirtualThreadAndReportsInterrupt() throws Exception
	{
		final CountDownLatch started = new CountDownLatch(1);
		final AtomicReference<Thread> worker = new AtomicReference<>();
		final AtomicReference<AssertionError> failure = new AtomicReference<>();
		
		final LoginServerThread login = new LoginServerThread()
		{
			@Override
			public void run()
			{
				final Thread current = Thread.currentThread();
				worker.set(current);
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
					if (!"LoginServerThread".equals(current.getName()))
					{
						throw new AssertionError("unexpected name: " + current.getName());
					}
				}
				catch (AssertionError e)
				{
					failure.set(e);
				}
				finally
				{
					started.countDown();
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
		};
		
		final Object asObject = login;
		assertFalse(asObject instanceof Thread);
		assertFalse(login.isInterrupted());
		login.interrupt();
		
		login.start();
		assertTrue(started.await(5, TimeUnit.SECONDS));
		if (failure.get() != null)
		{
			throw failure.get();
		}
		
		final Thread recorded = worker.get();
		assertTrue(recorded.isVirtual());
		assertTrue(recorded.isDaemon());
		assertEquals("LoginServerThread", recorded.getName());
		
		login.interrupt();
		recorded.join(5000);
		assertFalse(recorded.isAlive());
		// Assert after join: InterruptedException clears the flag until the catch restores it.
		assertTrue(login.isInterrupted());
	}
}
