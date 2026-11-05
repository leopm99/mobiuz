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
package com.l2journey.gameserver.model.clan;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.lang.reflect.Field;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.clan.Clan.SubPledge;
import com.l2journey.gameserver.network.SystemMessageId;

/**
 * {@link Clan#createSubPledge} messages when no sub-pledge slot is available.
 */
class ClanCreateSubPledgeTest
{
	@Test
	void createSubPledgeSendsAcademyMessageWhenAcademyAlreadyExists() throws Exception
	{
		final Clan clan = new Clan(1001, "AcademyClan");
		putSubPledge(clan, new SubPledge(Clan.SUBUNIT_ACADEMY, "Academy", 0));
		
		final Player player = mock(Player.class);
		assertNull(clan.createSubPledge(player, Clan.SUBUNIT_ACADEMY, 42, "Academy"));
		
		verify(player).sendPacket(SystemMessageId.YOUR_CLAN_HAS_ALREADY_ESTABLISHED_A_CLAN_ACADEMY);
		verify(player, never()).sendMessage(anyString());
		verifyNoMoreInteractions(player);
	}
	
	@Test
	void createSubPledgeSendsGenericMessageWhenRoyalSlotsExhausted() throws Exception
	{
		final Clan clan = new Clan(1002, "RoyalClan");
		putSubPledge(clan, new SubPledge(Clan.SUBUNIT_ROYAL1, "Royal1", 1));
		putSubPledge(clan, new SubPledge(Clan.SUBUNIT_ROYAL2, "Royal2", 2));
		
		final Player player = mock(Player.class);
		assertNull(clan.createSubPledge(player, Clan.SUBUNIT_ROYAL1, 42, "Royal3"));
		
		verify(player).sendMessage("You can't create any more sub-units of this type");
		verify(player, never()).sendPacket(SystemMessageId.YOUR_CLAN_HAS_ALREADY_ESTABLISHED_A_CLAN_ACADEMY);
		verifyNoMoreInteractions(player);
	}
	
	@SuppressWarnings("unchecked")
	private static void putSubPledge(Clan clan, SubPledge pledge) throws Exception
	{
		final Field field = Clan.class.getDeclaredField("_subPledges");
		field.setAccessible(true);
		((Map<Integer, SubPledge>) field.get(clan)).put(pledge.getId(), pledge);
	}
}
