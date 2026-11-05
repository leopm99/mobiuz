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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.appearance.PlayerAppearance;
import com.l2journey.gameserver.model.skill.Skill;
import com.l2journey.gameserver.model.zone.ZoneId;
import com.l2journey.gameserver.network.SystemMessageId;
import com.l2journey.gameserver.network.serverpackets.SystemMessage;

/**
 * Characterization of {@link ConditionPlayerCallPc} summon-location gates.
 */
class ConditionPlayerCallPcTest
{
	@Test
	void nullPlayerReturnsFalse()
	{
		final Creature effector = mock(Creature.class);
		when(effector.asPlayer()).thenReturn(null);
		
		final ConditionPlayerCallPc condition = new ConditionPlayerCallPc(true);
		assertFalse(condition.testImpl(effector, null, mock(Skill.class), null));
	}
	
	@Test
	void deadPlayerTargetSendsSystemMessage()
	{
		final Player player = mockPlayer();
		final Player target = mock(Player.class);
		final PlayerAppearance appearance = mock(PlayerAppearance.class);
		when(target.isPlayer()).thenReturn(true);
		when(target.asPlayer()).thenReturn(target);
		when(target.isDead()).thenReturn(true);
		when(target.getAppearance()).thenReturn(appearance);
		when(appearance.getVisibleName()).thenReturn("DeadOne");
		
		final ConditionPlayerCallPc condition = new ConditionPlayerCallPc(true);
		assertFalse(condition.testImpl(player, target, mock(Skill.class), null));
		
		verify(player).sendPacket(any(SystemMessage.class));
	}
	
	@Test
	void olympiadModeSendsLocationPacket()
	{
		final Player player = mockPlayer();
		when(player.isInOlympiadMode()).thenReturn(true);
		
		final ConditionPlayerCallPc condition = new ConditionPlayerCallPc(true);
		assertFalse(condition.testImpl(player, null, mock(Skill.class), null));
		
		verify(player).sendPacket(SystemMessageId.YOU_MAY_NOT_SUMMON_FROM_YOUR_CURRENT_LOCATION);
	}
	
	@Test
	void observerModeReturnsFalseWithoutPacket()
	{
		final Player player = mockPlayer();
		when(player.inObserverMode()).thenReturn(true);
		
		final ConditionPlayerCallPc condition = new ConditionPlayerCallPc(true);
		assertFalse(condition.testImpl(player, null, mock(Skill.class), null));
		
		verify(player, never()).sendPacket(any(SystemMessageId.class));
		verify(player, never()).sendPacket(any(SystemMessage.class));
	}
	
	@Test
	void onEventSendsBlocksSummoningPacket()
	{
		final Player player = mockPlayer();
		when(player.isOnEvent()).thenReturn(true);
		
		final ConditionPlayerCallPc condition = new ConditionPlayerCallPc(true);
		assertFalse(condition.testImpl(player, null, mock(Skill.class), null));
		
		verify(player).sendPacket(SystemMessageId.YOUR_TARGET_IS_IN_AN_AREA_WHICH_BLOCKS_SUMMONING);
	}
	
	@Test
	void noSummonFriendZoneSendsBlocksSummoningPacket()
	{
		final Player player = mockPlayer();
		when(player.isInsideZone(ZoneId.NO_SUMMON_FRIEND)).thenReturn(true);
		
		final ConditionPlayerCallPc condition = new ConditionPlayerCallPc(true);
		assertFalse(condition.testImpl(player, null, mock(Skill.class), null));
		
		verify(player).sendPacket(SystemMessageId.YOUR_TARGET_IS_IN_AN_AREA_WHICH_BLOCKS_SUMMONING);
	}
	
	@Test
	void jailZoneSendsBlocksSummoningPacket()
	{
		final Player player = mockPlayer();
		when(player.isInsideZone(ZoneId.JAIL)).thenReturn(true);
		
		final ConditionPlayerCallPc condition = new ConditionPlayerCallPc(true);
		assertFalse(condition.testImpl(player, null, mock(Skill.class), null));
		
		verify(player).sendPacket(SystemMessageId.YOUR_TARGET_IS_IN_AN_AREA_WHICH_BLOCKS_SUMMONING);
	}
	
	@Test
	void flyingMountedSendsBlocksSummoningPacket()
	{
		final Player player = mockPlayer();
		when(player.isFlyingMounted()).thenReturn(true);
		
		final ConditionPlayerCallPc condition = new ConditionPlayerCallPc(true);
		assertFalse(condition.testImpl(player, null, mock(Skill.class), null));
		
		verify(player).sendPacket(SystemMessageId.YOUR_TARGET_IS_IN_AN_AREA_WHICH_BLOCKS_SUMMONING);
	}
	
	@Test
	void allowsCallWhenChecksPass()
	{
		final Player player = mockPlayer();
		
		final ConditionPlayerCallPc condition = new ConditionPlayerCallPc(true);
		assertTrue(condition.testImpl(player, null, mock(Skill.class), null));
	}
	
	private static Player mockPlayer()
	{
		final Player player = mock(Player.class);
		when(player.isPlayer()).thenReturn(true);
		when(player.asPlayer()).thenReturn(player);
		return player;
	}
}
