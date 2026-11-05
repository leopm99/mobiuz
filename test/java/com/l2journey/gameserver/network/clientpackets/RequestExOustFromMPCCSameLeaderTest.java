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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.l2journey.commons.network.ReadableBuffer;
import com.l2journey.gameserver.model.World;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.groups.CommandChannel;
import com.l2journey.gameserver.model.groups.Party;
import com.l2journey.gameserver.network.GameClient;
import com.l2journey.gameserver.network.SystemMessageId;

/**
 * Command-channel oust checks leaders via {@link CommandChannel#hasSameLeader(CommandChannel)}.
 */
class RequestExOustFromMPCCSameLeaderTest
{
	private static final String TARGET_NAME = "Target";
	
	@Test
	void sameChannelLeaderSelfTargetReturnsAfterLeaderCheck() throws Exception
	{
		final Player player = mock(Player.class);
		final Party party = mock(Party.class);
		final CommandChannel channel = mock(CommandChannel.class);
		when(player.getParty()).thenReturn(party);
		when(player.isInParty()).thenReturn(true);
		when(party.isInCommandChannel()).thenReturn(true);
		when(party.getCommandChannel()).thenReturn(channel);
		when(channel.getLeader()).thenReturn(player);
		when(channel.hasSameLeader(channel)).thenReturn(true);
		
		runPacket(player, player);
		
		verify(channel).hasSameLeader(channel);
	}
	
	@Test
	void differentChannelLeaderRefusesOust() throws Exception
	{
		final Player player = mock(Player.class);
		final Player target = mock(Player.class);
		final Party party = mock(Party.class);
		final Party targetParty = mock(Party.class);
		final CommandChannel channel = mock(CommandChannel.class);
		final CommandChannel otherChannel = mock(CommandChannel.class);
		when(player.getParty()).thenReturn(party);
		when(target.isInParty()).thenReturn(true);
		when(target.getParty()).thenReturn(targetParty);
		when(party.isInCommandChannel()).thenReturn(true);
		when(targetParty.isInCommandChannel()).thenReturn(true);
		when(party.getCommandChannel()).thenReturn(channel);
		when(targetParty.getCommandChannel()).thenReturn(otherChannel);
		when(channel.getLeader()).thenReturn(player);
		when(channel.hasSameLeader(otherChannel)).thenReturn(false);
		
		runPacket(player, target);
		
		verify(channel).hasSameLeader(otherChannel);
	}
	
	@Test
	void missingTargetRefusesOust() throws Exception
	{
		final Player player = mock(Player.class);
		when(player.getParty()).thenReturn(mock(Party.class));
		
		runPacket(player, null);
		
		verify(player).sendPacket(any(SystemMessageId.class));
	}
	
	@Test
	void targetNotInPartyRefusesOust() throws Exception
	{
		final Player player = mock(Player.class);
		final Player target = mock(Player.class);
		when(player.getParty()).thenReturn(mock(Party.class));
		when(target.isInParty()).thenReturn(false);
		
		runPacket(player, target);
		
		verify(player).sendPacket(any(SystemMessageId.class));
	}
	
	@Test
	void partyNotInCommandChannelRefusesOust() throws Exception
	{
		final Player player = mock(Player.class);
		final Player target = mock(Player.class);
		final Party party = mock(Party.class);
		when(player.getParty()).thenReturn(party);
		when(target.isInParty()).thenReturn(true);
		when(party.isInCommandChannel()).thenReturn(false);
		
		runPacket(player, target);
		
		verify(player).sendPacket(any(SystemMessageId.class));
	}
	
	private static void runPacket(Player player, Player target) throws Exception
	{
		final GameClient client = mock(GameClient.class);
		when(client.getPlayer()).thenReturn(player);
		final RequestExOustFromMPCC packet = new RequestExOustFromMPCC();
		packet.init(client, mock(ReadableBuffer.class));
		
		final Field name = RequestExOustFromMPCC.class.getDeclaredField("_name");
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
