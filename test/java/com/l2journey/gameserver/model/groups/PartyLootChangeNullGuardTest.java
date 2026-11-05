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
package com.l2journey.gameserver.model.groups;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.ScheduledFuture;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.l2journey.commons.threads.ThreadPool;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.network.serverpackets.ExAskModifyPartyLooting;
import com.l2journey.gameserver.network.serverpackets.ServerPacket;
import com.l2journey.gameserver.network.serverpackets.SystemMessage;

/**
 * Null-safe party leader access in {@link Party#requestLootChange}.
 */
class PartyLootChangeNullGuardTest
{
	@Test
	void returnsWhenLeaderMissing()
	{
		final Player leader = mock(Player.class);
		when(leader.getLevel()).thenReturn(40);
		final Party party = new Party(leader, PartyDistributionType.FINDERS_KEEPERS);
		party.getMembers().clear();
		
		try (MockedStatic<ThreadPool> threadPool = mockStatic(ThreadPool.class))
		{
			assertDoesNotThrow(() -> party.requestLootChange(PartyDistributionType.RANDOM));
			threadPool.verify(() -> ThreadPool.schedule(any(Runnable.class), anyLong()), never());
		}
		verify(leader, never()).sendPacket(any(ServerPacket.class));
	}
	
	@Test
	void broadcastsLootChangeWhenLeaderPresent()
	{
		final Player leader = mock(Player.class);
		final Player member = mock(Player.class);
		when(leader.getLevel()).thenReturn(40);
		when(leader.getName()).thenReturn("Leader");
		when(leader.getObjectId()).thenReturn(1);
		when(member.getObjectId()).thenReturn(2);
		
		final Party party = new Party(leader, PartyDistributionType.FINDERS_KEEPERS);
		party.getMembers().add(member);
		
		final ScheduledFuture<?> future = mock(ScheduledFuture.class);
		try (MockedStatic<ThreadPool> threadPool = mockStatic(ThreadPool.class))
		{
			threadPool.when(() -> ThreadPool.schedule(any(Runnable.class), anyLong())).thenReturn(future);
			party.requestLootChange(PartyDistributionType.RANDOM);
			threadPool.verify(() -> ThreadPool.schedule(any(Runnable.class), anyLong()));
		}
		
		verify(member).sendPacket(any(ExAskModifyPartyLooting.class));
		verify(leader).sendPacket(any(SystemMessage.class));
	}
}
