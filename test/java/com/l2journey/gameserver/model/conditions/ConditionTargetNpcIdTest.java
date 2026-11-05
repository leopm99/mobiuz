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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.Test;

import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.skill.Skill;

/**
 * Characterization of {@link ConditionTargetNpcId} npc/door id matching.
 */
class ConditionTargetNpcIdTest
{
	@Test
	void nonPlayerEffectorUsesEffectedNpc()
	{
		final Creature effector = mock(Creature.class);
		final Npc npc = mock(Npc.class);
		when(effector.isPlayer()).thenReturn(false);
		when(npc.isNpc()).thenReturn(true);
		when(npc.getId()).thenReturn(1001);
		
		final ConditionTargetNpcId condition = new ConditionTargetNpcId(Set.of(1001));
		assertTrue(condition.testImpl(effector, npc, mock(Skill.class), null));
	}
	
	@Test
	void nullEffectorUsesEffected()
	{
		final Creature door = mock(Creature.class);
		when(door.isDoor()).thenReturn(true);
		when(door.getId()).thenReturn(2002);
		
		final ConditionTargetNpcId condition = new ConditionTargetNpcId(Set.of(2002));
		assertTrue(condition.testImpl(null, door, mock(Skill.class), null));
	}
	
	@Test
	void playerEffectorUsesGetTarget()
	{
		final Player player = mock(Player.class);
		final Npc npc = mock(Npc.class);
		when(player.isPlayer()).thenReturn(true);
		when(player.asPlayer()).thenReturn(player);
		when(player.getTarget()).thenReturn(npc);
		when(npc.isNpc()).thenReturn(true);
		when(npc.getId()).thenReturn(1001);
		
		final ConditionTargetNpcId condition = new ConditionTargetNpcId(Set.of(1001));
		assertTrue(condition.testImpl(player, mock(Npc.class), mock(Skill.class), null));
	}
	
	@Test
	void idNotInSetReturnsFalse()
	{
		final Creature effector = mock(Creature.class);
		final Npc npc = mock(Npc.class);
		when(effector.isPlayer()).thenReturn(false);
		when(npc.isNpc()).thenReturn(true);
		when(npc.getId()).thenReturn(9999);
		
		final ConditionTargetNpcId condition = new ConditionTargetNpcId(Set.of(1001));
		assertFalse(condition.testImpl(effector, npc, mock(Skill.class), null));
	}
	
	@Test
	void nullTargetReturnsFalse()
	{
		final Player player = mock(Player.class);
		when(player.isPlayer()).thenReturn(true);
		when(player.asPlayer()).thenReturn(player);
		when(player.getTarget()).thenReturn(null);
		
		final ConditionTargetNpcId condition = new ConditionTargetNpcId(Set.of(1001));
		assertFalse(condition.testImpl(player, null, mock(Skill.class), null));
	}
	
	@Test
	void nonNpcNonDoorTargetReturnsFalse()
	{
		final Creature effector = mock(Creature.class);
		final Player target = mock(Player.class);
		when(effector.isPlayer()).thenReturn(false);
		when(target.isNpc()).thenReturn(false);
		when(target.isDoor()).thenReturn(false);
		
		final ConditionTargetNpcId condition = new ConditionTargetNpcId(Set.of(1001));
		assertFalse(condition.testImpl(effector, target, mock(Skill.class), null));
	}
}
