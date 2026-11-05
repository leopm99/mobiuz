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
import com.l2journey.gameserver.network.SystemMessageId;
import com.l2journey.gameserver.network.serverpackets.SystemMessage;

/**
 * Characterization of {@link ConditionPlayerCanTransform} transform gates.
 */
class ConditionPlayerCanTransformTest
{
	@Test
	void nullPlayerReturnsFalseWithoutMessage()
	{
		final Creature effector = mock(Creature.class);
		when(effector.asPlayer()).thenReturn(null);
		
		final ConditionPlayerCanTransform condition = new ConditionPlayerCanTransform(true);
		assertFalse(condition.testImpl(effector, null, mock(Skill.class), null));
	}
	
	@Test
	void alikeDeadReturnsFalseWithoutMessage()
	{
		final Player player = mockPlayer();
		when(player.isAlikeDead()).thenReturn(true);
		
		final ConditionPlayerCanTransform condition = new ConditionPlayerCanTransform(true);
		assertFalse(condition.testImpl(player, null, mock(Skill.class), null));
		
		verify(player, never()).sendPacket(any(SystemMessageId.class));
		verify(player, never()).sendPacket(any(SystemMessage.class));
		verify(player, never()).sendMessage(any(String.class));
	}
	
	@Test
	void cursedWeaponReturnsFalseWithoutMessage()
	{
		final Player player = mockPlayer();
		when(player.isCursedWeaponEquipped()).thenReturn(true);
		
		final ConditionPlayerCanTransform condition = new ConditionPlayerCanTransform(true);
		assertFalse(condition.testImpl(player, null, mock(Skill.class), null));
		
		verify(player, never()).sendPacket(any(SystemMessageId.class));
		verify(player, never()).sendMessage(any(String.class));
	}
	
	@Test
	void sittingSendsCannotTransformWhileSitting()
	{
		final Player player = mockPlayer();
		when(player.isSitting()).thenReturn(true);
		
		final ConditionPlayerCanTransform condition = new ConditionPlayerCanTransform(true);
		assertFalse(condition.testImpl(player, null, mock(Skill.class), null));
		
		verify(player).sendPacket(SystemMessageId.YOU_CANNOT_TRANSFORM_WHILE_SITTING);
	}
	
	@Test
	void alreadyTransformedSendsAlreadyPolymorphed()
	{
		final Player player = mockPlayer();
		when(player.isTransformed()).thenReturn(true);
		
		final ConditionPlayerCanTransform condition = new ConditionPlayerCanTransform(true);
		assertFalse(condition.testImpl(player, null, mock(Skill.class), null));
		
		verify(player).sendPacket(SystemMessageId.YOU_ALREADY_POLYMORPHED_AND_CANNOT_POLYMORPH_AGAIN);
	}
	
	@Test
	void stanceSendsAlreadyPolymorphed()
	{
		final Player player = mockPlayer();
		when(player.isInStance()).thenReturn(true);
		
		final ConditionPlayerCanTransform condition = new ConditionPlayerCanTransform(true);
		assertFalse(condition.testImpl(player, null, mock(Skill.class), null));
		
		verify(player).sendPacket(SystemMessageId.YOU_ALREADY_POLYMORPHED_AND_CANNOT_POLYMORPH_AGAIN);
	}
	
	@Test
	void inWaterSendsCannotPolymorphInWater()
	{
		final Player player = mockPlayer();
		when(player.isInWater()).thenReturn(true);
		
		final ConditionPlayerCanTransform condition = new ConditionPlayerCanTransform(true);
		assertFalse(condition.testImpl(player, null, mock(Skill.class), null));
		
		verify(player).sendPacket(SystemMessageId.YOU_CANNOT_POLYMORPH_INTO_THE_DESIRED_FORM_IN_WATER);
	}
	
	@Test
	void flyingMountedSendsCannotPolymorphWhileRiding()
	{
		final Player player = mockPlayer();
		when(player.isFlyingMounted()).thenReturn(true);
		
		final ConditionPlayerCanTransform condition = new ConditionPlayerCanTransform(true);
		assertFalse(condition.testImpl(player, null, mock(Skill.class), null));
		
		verify(player).sendPacket(SystemMessageId.YOU_CANNOT_POLYMORPH_WHILE_RIDING_A_PET);
	}
	
	@Test
	void mountedSendsCannotPolymorphWhileRiding()
	{
		final Player player = mockPlayer();
		when(player.isMounted()).thenReturn(true);
		
		final ConditionPlayerCanTransform condition = new ConditionPlayerCanTransform(true);
		assertFalse(condition.testImpl(player, null, mock(Skill.class), null));
		
		verify(player).sendPacket(SystemMessageId.YOU_CANNOT_POLYMORPH_WHILE_RIDING_A_PET);
	}
	
	@Test
	void hasSummonSendsMessage()
	{
		final Player player = mockPlayer();
		when(player.hasSummon()).thenReturn(true);
		
		final ConditionPlayerCanTransform condition = new ConditionPlayerCanTransform(true);
		assertFalse(condition.testImpl(player, null, mock(Skill.class), null));
		
		verify(player).sendMessage("You cannot transform while you have a pet or summon active.");
	}
	
	@Test
	void registeredOnEventSendsMessage()
	{
		final Player player = mockPlayer();
		when(player.isRegisteredOnEvent()).thenReturn(true);
		
		final ConditionPlayerCanTransform condition = new ConditionPlayerCanTransform(true);
		assertFalse(condition.testImpl(player, null, mock(Skill.class), null));
		
		verify(player).sendMessage("You cannot transform while registered on an event.");
	}
	
	@Test
	void allowsTransformWhenChecksPass()
	{
		final Player player = mockPlayer();
		
		final ConditionPlayerCanTransform condition = new ConditionPlayerCanTransform(true);
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
