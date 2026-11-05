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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.l2journey.Config;
import com.l2journey.gameserver.network.loginserverpackets.game.ServerStatus;

/**
 * Covers interrupt restoration in {@link Shutdown} pauses (java:S2142).
 */
class ShutdownInterruptTest
{
	@Test
	void countdownRestoresInterruptAndReturns()
	{
		final Shutdown shutdown = new Shutdown(61, false);
		Thread.currentThread().interrupt();
		try
		{
			shutdown.countdown();
			assertTrue(Thread.currentThread().isInterrupted());
		}
		finally
		{
			Thread.interrupted();
		}
	}
	
	@Test
	void countdownCompletesOneSecondWithoutInterrupt()
	{
		final Shutdown shutdown = new Shutdown(1, false);
		shutdown.countdown();
		assertTrue(shutdown._secondsShut <= 0);
	}
	
	@Test
	void countdownAbortRestoresLoginServerStatus()
	{
		final Shutdown shutdown = new Shutdown(61, false);
		shutdown._shutdownMode = 3; // ABORT
		
		final LoginServerThread login = mock(LoginServerThread.class);
		when(login.getServerStatus()).thenReturn(ServerStatus.STATUS_DOWN);
		
		try (MockedStatic<LoginServerThread> loginStatic = mockStatic(LoginServerThread.class))
		{
			loginStatic.when(LoginServerThread::getInstance).thenReturn(login);
			final boolean gmOnly = Config.SERVER_GMONLY;
			Config.SERVER_GMONLY = false;
			try
			{
				shutdown.countdown();
				verify(login).setServerStatus(ServerStatus.STATUS_AUTO);
			}
			finally
			{
				Config.SERVER_GMONLY = gmOnly;
			}
		}
	}
	
	@Test
	void pauseAfterSaveRestoresInterrupt()
	{
		final Shutdown shutdown = new Shutdown(61, false);
		Thread.currentThread().interrupt();
		try
		{
			shutdown.pauseAfterSave();
			assertTrue(Thread.currentThread().isInterrupted());
		}
		finally
		{
			Thread.interrupted();
		}
	}
}
