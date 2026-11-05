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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.l2journey.gameserver.model.actor.Player;

/**
 * Leader object-id comparison for {@link Party#hasSameLeader(Party)}.
 */
class PartySameLeaderTest
{
	@Test
	void nullArgumentIsFalse()
	{
		final Party party = partyWithLeader(1);
		assertFalse(party.hasSameLeader(null));
	}
	
	@Test
	void sameLeaderIdMatches()
	{
		final Party left = partyWithLeader(10);
		final Party right = partyWithLeader(10);
		assertTrue(left.hasSameLeader(right));
	}
	
	@Test
	void differentLeaderIdsDoNotMatch()
	{
		final Party left = partyWithLeader(10);
		final Party right = partyWithLeader(20);
		assertFalse(left.hasSameLeader(right));
	}
	
	@Test
	void missingLeadersMatchAsZero()
	{
		final Party left = partyWithLeader(1);
		final Party right = partyWithLeader(2);
		left.getMembers().clear();
		right.getMembers().clear();
		assertTrue(left.hasSameLeader(right));
	}
	
	private static Party partyWithLeader(int objectId)
	{
		final Player leader = mock(Player.class);
		when(leader.getLevel()).thenReturn(40);
		when(leader.getObjectId()).thenReturn(objectId);
		return new Party(leader, PartyDistributionType.FINDERS_KEEPERS);
	}
}
