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
package com.l2journey.gameserver.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import com.l2journey.gameserver.network.ConnectionState;
import com.l2journey.gameserver.network.GameClient;

/**
 * Null-safe client logging in {@link FloodProtectorAction}.
 */
class FloodProtectorActionNullGuardTest
{
	@Test
	void logAllowsNullClient() throws Exception
	{
		final FloodProtectorAction action = actionWith(null, config());
		assertDoesNotThrow(() -> invokeLog(action, "flood"));
	}
	
	@Test
	void logAllowsDetachedClient() throws Exception
	{
		final GameClient client = mock(GameClient.class);
		when(client.isDetached()).thenReturn(true);
		when(client.getConnectionState()).thenReturn(ConnectionState.CONNECTED);
		final FloodProtectorAction action = actionWith(client, config());
		assertDoesNotThrow(() -> invokeLog(action, "flood"));
	}
	
	@Test
	void logAllowsConnectedClient() throws Exception
	{
		final GameClient client = mock(GameClient.class);
		when(client.isDetached()).thenReturn(false);
		when(client.getIp()).thenReturn("127.0.0.1");
		when(client.getConnectionState()).thenReturn(ConnectionState.CONNECTED);
		final FloodProtectorAction action = actionWith(client, config());
		assertDoesNotThrow(() -> invokeLog(action, "flood"));
	}
	
	private static FloodProtectorConfig config()
	{
		return new FloodProtectorConfig("TestFlood");
	}
	
	private static FloodProtectorAction actionWith(GameClient client, FloodProtectorConfig config) throws Exception
	{
		final FloodProtectorAction action = mock(FloodProtectorAction.class, CALLS_REAL_METHODS);
		final Field clientField = FloodProtectorAction.class.getDeclaredField("_client");
		clientField.setAccessible(true);
		clientField.set(action, client);
		final Field configField = FloodProtectorAction.class.getDeclaredField("_config");
		configField.setAccessible(true);
		configField.set(action, config);
		return action;
	}
	
	private static void invokeLog(FloodProtectorAction action, String... lines) throws Exception
	{
		final Method method = FloodProtectorAction.class.getDeclaredMethod("log", String[].class);
		method.setAccessible(true);
		method.invoke(action, (Object) lines);
	}
}
