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
package com.l2journey.loginserver.network.gameserverpackets;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.l2journey.commons.network.base.BaseWritablePacket;
import com.l2journey.loginserver.GameServerTable;
import com.l2journey.loginserver.GameServerTable.GameServerInfo;
import com.l2journey.loginserver.GameServerThread;
import com.l2journey.loginserver.network.GameServerPacketHandler.GameServerState;
import com.l2journey.loginserver.network.loginserverpackets.AuthResponse;
import com.l2journey.loginserver.network.loginserverpackets.LoginServerFail;

/**
 * Game server auth attach under private lock (java:S2445).
 */
class GameServerAuthTest
{
	private static byte[] authPacket(int desiredId, int port, int maxPlayers, byte[] hexId)
	{
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		out.write(0x01); // packet id (discarded)
		out.write(desiredId & 0xff);
		out.write(0); // acceptAlternativeId
		out.write(0); // hostReserved
		out.write(port & 0xff);
		out.write((port >> 8) & 0xff);
		writeInt(out, maxPlayers);
		writeInt(out, hexId.length);
		out.writeBytes(hexId);
		writeInt(out, 0); // no hosts
		return out.toByteArray();
	}
	
	private static void writeInt(ByteArrayOutputStream out, int value)
	{
		out.write(value & 0xff);
		out.write((value >> 8) & 0xff);
		out.write((value >> 16) & 0xff);
		out.write((value >> 24) & 0xff);
	}
	
	@Test
	void attachesRegisteredGameServer()
	{
		final byte[] hexId = "abcd".getBytes(StandardCharsets.US_ASCII);
		final GameServerInfo gsi = new GameServerInfo(1, hexId);
		final GameServerTable table = mock(GameServerTable.class);
		final GameServerThread server = mock(GameServerThread.class);
		
		when(table.getRegisteredGameServerById(1)).thenReturn(gsi);
		when(server.getGameServerInfo()).thenReturn(gsi);
		doAnswer(invocation ->
		{
			final GameServerInfo info = invocation.getArgument(0);
			info.setAuthed(true);
			return null;
		}).when(server).attachGameServerInfo(eq(gsi), anyInt(), any(String[].class), anyInt());
		
		try (MockedStatic<GameServerTable> tableStatic = mockStatic(GameServerTable.class))
		{
			tableStatic.when(GameServerTable::getInstance).thenReturn(table);
			new GameServerAuth(authPacket(1, 7777, 1000, hexId), server);
		}
		
		verify(server).attachGameServerInfo(eq(gsi), eq(7777), any(String[].class), eq(1000));
		verify(server).sendPacket(any(AuthResponse.class));
		verify(server).setLoginConnectionState(GameServerState.AUTHED);
		verify(server, never()).forceClose(anyInt());
	}
	
	@Test
	void rejectsAlreadyAuthedGameServer()
	{
		final byte[] hexId = "abcd".getBytes(StandardCharsets.US_ASCII);
		final GameServerInfo gsi = new GameServerInfo(1, hexId);
		gsi.setAuthed(true);
		final GameServerTable table = mock(GameServerTable.class);
		final GameServerThread server = mock(GameServerThread.class);
		
		when(table.getRegisteredGameServerById(1)).thenReturn(gsi);
		
		try (MockedStatic<GameServerTable> tableStatic = mockStatic(GameServerTable.class))
		{
			tableStatic.when(GameServerTable::getInstance).thenReturn(table);
			new GameServerAuth(authPacket(1, 7777, 1000, hexId), server);
		}
		
		verify(server).forceClose(LoginServerFail.REASON_ALREADY_LOGGED8IN);
		verify(server, never()).attachGameServerInfo(any(), anyInt(), any(), anyInt());
		verify(server, never()).sendPacket(any(BaseWritablePacket.class));
		verify(server, never()).setLoginConnectionState(any());
	}
}
