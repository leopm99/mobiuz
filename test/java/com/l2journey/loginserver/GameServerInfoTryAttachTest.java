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
package com.l2journey.loginserver;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;

import com.l2journey.loginserver.GameServerTable.GameServerInfo;

/**
 * Auth lock attach for {@link GameServerInfo} (java:S2445).
 */
class GameServerInfoTryAttachTest
{
	@Test
	void tryAttachSucceedsOnceThenRejects()
	{
		final byte[] hexId = new byte[]
		{
			1,
			2,
			3,
			4
		};
		final GameServerInfo gsi = new GameServerInfo(1, hexId);
		final GameServerThread server = mock(GameServerThread.class);
		final String[] hosts = new String[0];
		
		doAnswer(invocation ->
		{
			final GameServerInfo info = invocation.getArgument(0);
			info.setAuthed(true);
			return null;
		}).when(server).attachGameServerInfo(eq(gsi), anyInt(), any(String[].class), anyInt());
		
		assertTrue(gsi.tryAttach(server, 7777, hosts, 1000));
		assertTrue(gsi.isAuthed());
		verify(server, times(1)).attachGameServerInfo(gsi, 7777, hosts, 1000);
		
		assertFalse(gsi.tryAttach(server, 7777, hosts, 1000));
		verify(server, times(1)).attachGameServerInfo(gsi, 7777, hosts, 1000);
		verify(server, never()).forceClose(anyInt());
	}
}
