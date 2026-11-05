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
package com.l2journey.gameserver.model.conditions;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;

import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.groups.Party;
import com.l2journey.gameserver.model.skill.Skill;
import com.l2journey.gameserver.network.SystemMessageId;
import com.l2journey.gameserver.network.serverpackets.SystemMessage;

/**
 * Same-leader party membership for {@link ConditionTargetMyPartyExceptMe}.
 */
class ConditionTargetMyPartyExceptMeTest
{
	@Test
	void sameLeaderAllowsPartyMember()
	{
		final Player player = mock(Player.class);
		final Player target = mock(Player.class);
		final Party party = mock(Party.class);
		when(player.asPlayer()).thenReturn(player);
		when(target.isPlayer()).thenReturn(true);
		when(player.isInParty()).thenReturn(true);
		when(player.getParty()).thenReturn(party);
		when(target.getParty()).thenReturn(party);
		when(party.hasSameLeader(party)).thenReturn(true);
		
		final ConditionTargetMyPartyExceptMe condition = new ConditionTargetMyPartyExceptMe(true);
		assertTrue(condition.testImpl(player, target, mock(Skill.class), null));
	}
	
	@Test
	void playerNotInPartyRejectsAndSendsMessage() throws Exception
	{
		final Player player = mock(Player.class);
		final Player target = mock(Player.class);
		final Skill skill = mock(Skill.class);
		when(skill.getId()).thenReturn(1000);
		when(skill.getDisplayId()).thenReturn(1000);
		when(skill.getLevel()).thenReturn(1);
		when(player.asPlayer()).thenReturn(player);
		when(target.isPlayer()).thenReturn(true);
		when(player.isInParty()).thenReturn(false);
		
		withUnsuitableTermsMessage(() ->
		{
			final ConditionTargetMyPartyExceptMe condition = new ConditionTargetMyPartyExceptMe(true);
			assertFalse(condition.testImpl(player, target, skill, null));
		});
		
		verify(player).sendPacket(any(SystemMessage.class));
	}
	
	@Test
	void differentLeaderRejectsAndSendsMessage() throws Exception
	{
		final Player player = mock(Player.class);
		final Player target = mock(Player.class);
		final Party playerParty = mock(Party.class);
		final Party targetParty = mock(Party.class);
		final Skill skill = mock(Skill.class);
		when(skill.getId()).thenReturn(1000);
		when(skill.getDisplayId()).thenReturn(1000);
		when(skill.getLevel()).thenReturn(1);
		when(player.asPlayer()).thenReturn(player);
		when(target.isPlayer()).thenReturn(true);
		when(player.isInParty()).thenReturn(true);
		when(player.getParty()).thenReturn(playerParty);
		when(target.getParty()).thenReturn(targetParty);
		when(playerParty.hasSameLeader(targetParty)).thenReturn(false);
		
		withUnsuitableTermsMessage(() ->
		{
			final ConditionTargetMyPartyExceptMe condition = new ConditionTargetMyPartyExceptMe(true);
			assertFalse(condition.testImpl(player, target, skill, null));
		});
		
		verify(player).sendPacket(any(SystemMessage.class));
		verify(playerParty).hasSameLeader(targetParty);
	}
	
	@FunctionalInterface
	private interface ThrowingRunnable
	{
		void run() throws Exception;
	}
	
	private static void withUnsuitableTermsMessage(ThrowingRunnable action) throws Exception
	{
		final SystemMessageId unsuitable = mock(SystemMessageId.class);
		when(unsuitable.getParamCount()).thenReturn(1);
		final Field field = SystemMessageId.class.getDeclaredField("S1_CANNOT_BE_USED_DUE_TO_UNSUITABLE_TERMS");
		field.setAccessible(true);
		final SystemMessageId previous = (SystemMessageId) field.get(null);
		field.set(null, unsuitable);
		try
		{
			action.run();
		}
		finally
		{
			field.set(null, previous);
		}
	}
}
