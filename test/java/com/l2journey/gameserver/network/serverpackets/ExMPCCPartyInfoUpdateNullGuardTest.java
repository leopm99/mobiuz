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
package com.l2journey.gameserver.network.serverpackets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;

import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.groups.Party;

/**
 * Null-safe party leader name in {@link ExMPCCPartyInfoUpdate}.
 */
class ExMPCCPartyInfoUpdateNullGuardTest
{
	@Test
	void storesEmptyNameWhenLeaderMissing() throws Exception
	{
		final Party party = mock(Party.class);
		when(party.getLeader()).thenReturn(null);
		when(party.getLeaderObjectId()).thenReturn(0);
		when(party.getMemberCount()).thenReturn(0);
		
		final ExMPCCPartyInfoUpdate packet = new ExMPCCPartyInfoUpdate(party, 1);
		assertEquals("", readName(packet));
	}
	
	@Test
	void storesLeaderNameWhenLeaderPresent() throws Exception
	{
		final Player leader = mock(Player.class);
		when(leader.getName()).thenReturn("Leader");
		final Party party = mock(Party.class);
		when(party.getLeader()).thenReturn(leader);
		when(party.getLeaderObjectId()).thenReturn(10);
		when(party.getMemberCount()).thenReturn(3);
		
		final ExMPCCPartyInfoUpdate packet = new ExMPCCPartyInfoUpdate(party, 1);
		assertEquals("Leader", readName(packet));
	}
	
	private static String readName(ExMPCCPartyInfoUpdate packet) throws Exception
	{
		final Field field = ExMPCCPartyInfoUpdate.class.getDeclaredField("_name");
		field.setAccessible(true);
		return (String) field.get(packet);
	}
}
