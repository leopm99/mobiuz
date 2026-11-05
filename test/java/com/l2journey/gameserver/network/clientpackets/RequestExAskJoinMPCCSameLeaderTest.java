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
package com.l2journey.gameserver.network.clientpackets;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.l2journey.commons.network.ReadableBuffer;
import com.l2journey.gameserver.model.World;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.groups.Party;
import com.l2journey.gameserver.network.GameClient;
import com.l2journey.gameserver.network.SystemMessageId;
import com.l2journey.gameserver.network.serverpackets.ServerPacket;

/**
 * Same-party invites are refused via {@link Party#hasSameLeader(Party)}.
 */
class RequestExAskJoinMPCCSameLeaderTest
{
	private static final String TARGET_NAME = "Target";
	
	@Test
	void samePartyLeaderReturnsWithoutInvite() throws Exception
	{
		final Player player = mock(Player.class);
		final Player target = mock(Player.class);
		final Party party = mock(Party.class);
		when(player.isInParty()).thenReturn(true);
		when(target.isInParty()).thenReturn(true);
		when(player.getParty()).thenReturn(party);
		when(target.getParty()).thenReturn(party);
		when(party.hasSameLeader(party)).thenReturn(true);
		
		runPacket(player, target);
		
		verify(party).hasSameLeader(party);
		verify(player, never()).sendPacket(any(ServerPacket.class));
	}
	
	@Test
	void missingTargetReturnsWithoutInvite() throws Exception
	{
		final Player player = mock(Player.class);
		
		runPacket(player, null);
		
		verify(player, never()).sendPacket(any(ServerPacket.class));
	}
	
	@Test
	void playerNotInPartySkipsSameLeaderCheck() throws Exception
	{
		final Player player = mock(Player.class);
		final Player target = mock(Player.class);
		when(player.isInParty()).thenReturn(false);
		
		runPacket(player, target);
		
		verify(player, never()).getParty();
	}
	
	@Test
	void targetNotInPartyContinuesPastSameLeaderCheck() throws Exception
	{
		final Player player = mock(Player.class);
		final Player target = mock(Player.class);
		final Player otherLeader = mock(Player.class);
		final Party party = mock(Party.class);
		when(player.isInParty()).thenReturn(true);
		when(target.isInParty()).thenReturn(false);
		when(player.getParty()).thenReturn(party);
		when(party.getLeader()).thenReturn(otherLeader);
		
		runPacket(player, target);
		
		verify(player).sendPacket(any(SystemMessageId.class));
	}
	
	private static void runPacket(Player player, Player target) throws Exception
	{
		final GameClient client = mock(GameClient.class);
		when(client.getPlayer()).thenReturn(player);
		final RequestExAskJoinMPCC packet = new RequestExAskJoinMPCC();
		packet.init(client, mock(ReadableBuffer.class));
		
		final Field name = RequestExAskJoinMPCC.class.getDeclaredField("_name");
		name.setAccessible(true);
		name.set(packet, TARGET_NAME);
		
		final World world = mock(World.class);
		when(world.getPlayer(TARGET_NAME)).thenReturn(target);
		try (MockedStatic<World> worldStatic = mockStatic(World.class))
		{
			worldStatic.when(World::getInstance).thenReturn(world);
			packet.run();
		}
	}
}
