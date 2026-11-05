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
import com.l2journey.gameserver.model.actor.appearance.PlayerAppearance;
import com.l2journey.gameserver.network.GameClient;
import com.l2journey.gameserver.network.SystemMessageId;
import com.l2journey.gameserver.network.serverpackets.ExAskJoinPartyRoom;
import com.l2journey.gameserver.network.serverpackets.SystemMessage;

/**
 * Party-room invites refuse when the inviter is already waiting for a reply.
 */
class RequestAskJoinPartyRoomTest
{
	private static final String TARGET_NAME = "Target";
	
	@Test
	void inviterWaitingSendsWaitingForReply() throws Exception
	{
		final Player inviter = mock(Player.class);
		when(inviter.isProcessingRequest()).thenReturn(true);
		final Player target = mock(Player.class);
		when(target.isProcessingRequest()).thenReturn(false);
		
		runPacket(inviter, target);
		
		verify(inviter).sendPacket(SystemMessageId.WAITING_FOR_ANOTHER_REPLY);
		verify(inviter, never()).onTransactionRequest(any());
		verify(target, never()).sendPacket(any(ExAskJoinPartyRoom.class));
	}
	
	@Test
	void targetWaitingDoesNotStartTransaction() throws Exception
	{
		final Player inviter = mock(Player.class);
		when(inviter.isProcessingRequest()).thenReturn(false);
		final Player target = mock(Player.class);
		when(target.isProcessingRequest()).thenReturn(true);
		final PlayerAppearance appearance = mock(PlayerAppearance.class);
		when(appearance.getVisibleName()).thenReturn(TARGET_NAME);
		when(target.getAppearance()).thenReturn(appearance);
		
		final SystemMessageId busyId = mock(SystemMessageId.class);
		when(busyId.getParamCount()).thenReturn(1);
		final Field busyField = SystemMessageId.class.getDeclaredField("C1_IS_ON_ANOTHER_TASK_PLEASE_TRY_AGAIN_LATER");
		busyField.setAccessible(true);
		final SystemMessageId previous = (SystemMessageId) busyField.get(null);
		busyField.set(null, busyId);
		try
		{
			runPacket(inviter, target);
		}
		finally
		{
			busyField.set(null, previous);
		}
		
		verify(inviter, never()).onTransactionRequest(any());
		verify(inviter).sendPacket(any(SystemMessage.class));
		verify(target, never()).sendPacket(any(ExAskJoinPartyRoom.class));
	}
	
	@Test
	void freeInviterAndTargetStartsTransaction() throws Exception
	{
		final Player inviter = mock(Player.class);
		when(inviter.isProcessingRequest()).thenReturn(false);
		when(inviter.getName()).thenReturn("Inviter");
		final Player target = mock(Player.class);
		when(target.isProcessingRequest()).thenReturn(false);
		
		runPacket(inviter, target);
		
		verify(inviter).onTransactionRequest(target);
		verify(target).sendPacket(any(ExAskJoinPartyRoom.class));
	}
	
	private static void runPacket(Player inviter, Player target) throws Exception
	{
		final GameClient client = mock(GameClient.class);
		when(client.getPlayer()).thenReturn(inviter);
		final RequestAskJoinPartyRoom packet = new RequestAskJoinPartyRoom();
		packet.init(client, mock(ReadableBuffer.class));
		
		final Field name = RequestAskJoinPartyRoom.class.getDeclaredField("_name");
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
