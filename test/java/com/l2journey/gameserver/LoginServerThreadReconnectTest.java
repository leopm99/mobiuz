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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.l2journey.Config;

/**
 * Covers {@link LoginServerThread#run()} reconnect shutdown after {@code finally}
 * (java:S1143) without parsing login packets.
 */
class LoginServerThreadReconnectTest
{
	private String previousHost;
	private int previousPort;
	private LoginServerThread login;
	
	@AfterEach
	void tearDown() throws Exception
	{
		if (login != null)
		{
			login.interrupt();
			final Thread worker = login.workerThread();
			if (worker != null)
			{
				worker.join(Duration.ofSeconds(5));
			}
		}
		Config.GAME_SERVER_LOGIN_HOST = previousHost;
		Config.GAME_SERVER_LOGIN_PORT = previousPort;
		Config.HEX_ID = null;
	}
	
	@Test
	void skipsReconnectSleepWhenInterruptedAfterSocketClose() throws Exception
	{
		previousHost = Config.GAME_SERVER_LOGIN_HOST;
		previousPort = Config.GAME_SERVER_LOGIN_PORT;
		Config.HEX_ID = null;
		
		final AtomicBoolean stop = new AtomicBoolean();
		try (ServerSocket server = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1")))
		{
			Config.GAME_SERVER_LOGIN_HOST = "127.0.0.1";
			Config.GAME_SERVER_LOGIN_PORT = server.getLocalPort();
			
			login = new LoginServerThread()
			{
				@Override
				boolean isInterrupted()
				{
					return stop.get() || super.isInterrupted();
				}
			};
			login.start();
			
			try (Socket accepted = server.accept())
			{
				stop.set(true);
			}
			
			final Thread worker = login.workerThread();
			assertNotNull(worker);
			assertTrue(worker.join(Duration.ofSeconds(2)), "worker should exit without the 5s reconnect sleep");
			assertFalse(worker.isAlive());
		}
	}
	
	@Test
	void entersReconnectSleepWhenNotInterruptedAfterFailedConnect() throws Exception
	{
		previousHost = Config.GAME_SERVER_LOGIN_HOST;
		previousPort = Config.GAME_SERVER_LOGIN_PORT;
		Config.HEX_ID = null;
		
		final int closedPort;
		try (ServerSocket server = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1")))
		{
			closedPort = server.getLocalPort();
		}
		
		Config.GAME_SERVER_LOGIN_HOST = "127.0.0.1";
		Config.GAME_SERVER_LOGIN_PORT = closedPort;
		
		login = new LoginServerThread();
		login.start();
		
		final Thread worker = login.workerThread();
		assertNotNull(worker);
		assertFalse(worker.join(Duration.ofMillis(400)), "worker should still be in the reconnect sleep");
		assertTrue(worker.isAlive());
		
		login.interrupt();
		assertTrue(worker.join(Duration.ofSeconds(2)));
		assertFalse(worker.isAlive());
	}
}
