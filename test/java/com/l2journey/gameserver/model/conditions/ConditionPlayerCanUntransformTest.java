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
import com.l2journey.gameserver.model.skill.Skill;
import com.l2journey.gameserver.model.zone.ZoneId;
import com.l2journey.gameserver.network.SystemMessageId;
import com.l2journey.gameserver.network.serverpackets.SystemMessage;

/**
 * Characterization of {@link ConditionPlayerCanUntransform} altitude and state gates.
 */
class ConditionPlayerCanUntransformTest
{
	@Test
	void nullPlayerReturnsFalse()
	{
		final Creature effector = mock(Creature.class);
		when(effector.asPlayer()).thenReturn(null);
		
		final ConditionPlayerCanUntransform condition = new ConditionPlayerCanUntransform(true);
		assertFalse(condition.testImpl(effector, null, mock(Skill.class), null));
	}
	
	@Test
	void alikeDeadReturnsFalseWithoutPacket()
	{
		final Player player = mockPlayer();
		when(player.isAlikeDead()).thenReturn(true);
		
		final ConditionPlayerCanUntransform condition = new ConditionPlayerCanUntransform(true);
		assertFalse(condition.testImpl(player, null, mock(Skill.class), null));
		
		verify(player, never()).sendPacket(any(SystemMessageId.class));
		verify(player, never()).sendPacket(any(SystemMessage.class));
	}
	
	@Test
	void cursedWeaponReturnsFalseWithoutPacket()
	{
		final Player player = mockPlayer();
		when(player.isCursedWeaponEquipped()).thenReturn(true);
		
		final ConditionPlayerCanUntransform condition = new ConditionPlayerCanUntransform(true);
		assertFalse(condition.testImpl(player, null, mock(Skill.class), null));
		
		verify(player, never()).sendPacket(any(SystemMessageId.class));
	}
	
	@Test
	void flyingTransformedOutsideLandingSendsAltitudeMessage()
	{
		final Player player = mockPlayer();
		when(player.isTransformed()).thenReturn(true);
		when(player.isFlyingMounted()).thenReturn(true);
		when(player.isInsideZone(ZoneId.LANDING)).thenReturn(false);
		
		final ConditionPlayerCanUntransform condition = new ConditionPlayerCanUntransform(true);
		assertFalse(condition.testImpl(player, null, mock(Skill.class), null));
		
		verify(player).sendPacket(SystemMessageId.YOU_ARE_TOO_HIGH_TO_PERFORM_THIS_ACTION_PLEASE_LOWER_YOUR_ALTITUDE_AND_TRY_AGAIN);
	}
	
	@Test
	void flyingStanceOutsideLandingSendsAltitudeMessage()
	{
		final Player player = mockPlayer();
		when(player.isInStance()).thenReturn(true);
		when(player.isFlyingMounted()).thenReturn(true);
		when(player.isInsideZone(ZoneId.LANDING)).thenReturn(false);
		
		final ConditionPlayerCanUntransform condition = new ConditionPlayerCanUntransform(true);
		assertFalse(condition.testImpl(player, null, mock(Skill.class), null));
		
		verify(player).sendPacket(SystemMessageId.YOU_ARE_TOO_HIGH_TO_PERFORM_THIS_ACTION_PLEASE_LOWER_YOUR_ALTITUDE_AND_TRY_AGAIN);
	}
	
	@Test
	void allowsUntransformWhenChecksPass()
	{
		final Player player = mockPlayer();
		
		final ConditionPlayerCanUntransform condition = new ConditionPlayerCanUntransform(true);
		assertTrue(condition.testImpl(player, null, mock(Skill.class), null));
	}
	
	@Test
	void allowsUntransformWhenFlyingInLandingZone()
	{
		final Player player = mockPlayer();
		when(player.isTransformed()).thenReturn(true);
		when(player.isFlyingMounted()).thenReturn(true);
		when(player.isInsideZone(ZoneId.LANDING)).thenReturn(true);
		
		final ConditionPlayerCanUntransform condition = new ConditionPlayerCanUntransform(true);
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
