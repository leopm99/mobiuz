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

import java.util.List;

import org.junit.jupiter.api.Test;

import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.Summon;
import com.l2journey.gameserver.model.skill.Skill;

/**
 * Characterization of {@link ConditionPlayerServitorNpcId} summon id matching.
 */
class ConditionPlayerServitorNpcIdTest
{
	@Test
	void wildcardZeroAllowsAnySummon()
	{
		final Player player = mockPlayer();
		final Summon summon = mock(Summon.class);
		when(player.hasSummon()).thenReturn(true);
		when(player.getSummon()).thenReturn(summon);
		when(summon.getId()).thenReturn(12345);
		
		final ConditionPlayerServitorNpcId condition = new ConditionPlayerServitorNpcId(List.of(0));
		assertTrue(condition.testImpl(player, null, mock(Skill.class), null));
	}
	
	@Test
	void nullPlayerReturnsFalse()
	{
		final Creature effector = mock(Creature.class);
		when(effector.asPlayer()).thenReturn(null);
		
		final ConditionPlayerServitorNpcId condition = new ConditionPlayerServitorNpcId(List.of(100));
		assertFalse(condition.testImpl(effector, null, mock(Skill.class), null));
	}
	
	@Test
	void noSummonReturnsFalse()
	{
		final Player player = mockPlayer();
		when(player.hasSummon()).thenReturn(false);
		
		final ConditionPlayerServitorNpcId condition = new ConditionPlayerServitorNpcId(List.of(100));
		assertFalse(condition.testImpl(player, null, mock(Skill.class), null));
	}
	
	@Test
	void matchingSummonIdReturnsTrue()
	{
		final Player player = mockPlayer();
		final Summon summon = mock(Summon.class);
		when(player.hasSummon()).thenReturn(true);
		when(player.getSummon()).thenReturn(summon);
		when(summon.getId()).thenReturn(100);
		
		final ConditionPlayerServitorNpcId condition = new ConditionPlayerServitorNpcId(List.of(100, 200));
		assertTrue(condition.testImpl(player, null, mock(Skill.class), null));
	}
	
	@Test
	void nonMatchingSummonIdReturnsFalse()
	{
		final Player player = mockPlayer();
		final Summon summon = mock(Summon.class);
		when(player.hasSummon()).thenReturn(true);
		when(player.getSummon()).thenReturn(summon);
		when(summon.getId()).thenReturn(999);
		
		final ConditionPlayerServitorNpcId condition = new ConditionPlayerServitorNpcId(List.of(100, 200));
		assertFalse(condition.testImpl(player, null, mock(Skill.class), null));
	}
	
	private static Player mockPlayer()
	{
		final Player player = mock(Player.class);
		when(player.isPlayer()).thenReturn(true);
		when(player.asPlayer()).thenReturn(player);
		return player;
	}
}
