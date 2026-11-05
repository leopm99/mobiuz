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
package com.l2journey.tools.dashboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

/**
 * Exit-path coverage for the dashboard control accept loop (java:S2189).
 */
class DashboardControlServerTest
{
	@Test
	void unknownCommandThenStopEndsWorker() throws Exception
	{
		DashboardControlServer.start();
		final Thread worker = waitForWorker();
		waitUntilListening();
		
		assertEquals("ERROR Unknown command", send("NOPE"));
		assertEquals("OK", send("STOP"));
		
		worker.join(TimeUnit.SECONDS.toMillis(5));
		assertFalse(worker.isAlive());
	}
	
	private static Thread waitForWorker() throws InterruptedException
	{
		final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		Thread worker = DashboardControlServer.worker();
		while ((worker == null) && (System.nanoTime() < deadline))
		{
			Thread.sleep(20);
			worker = DashboardControlServer.worker();
		}
		assertNotNull(worker);
		return worker;
	}
	
	private static void waitUntilListening() throws InterruptedException
	{
		final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		while (System.nanoTime() < deadline)
		{
			try (Socket socket = new Socket())
			{
				socket.connect(new InetSocketAddress(InetAddress.getLoopbackAddress(), DashboardControlServer.PORT), 200);
				return;
			}
			catch (IOException _)
			{
				Thread.sleep(20);
			}
		}
		throw new AssertionError("Dashboard control bridge did not start listening on port " + DashboardControlServer.PORT);
	}
	
	private static String send(String command) throws IOException
	{
		try (Socket socket = new Socket(InetAddress.getLoopbackAddress(), DashboardControlServer.PORT);
			PrintWriter output = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
			BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8)))
		{
			output.println(command);
			return input.readLine();
		}
	}
}
