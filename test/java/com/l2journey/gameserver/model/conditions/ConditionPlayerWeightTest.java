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

import org.junit.jupiter.api.Test;

import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.actor.Player;

/**
 * Characterization of {@link ConditionPlayerWeight}.
 */
class ConditionPlayerWeightTest
{
	@Test
	void returnsTrueWhenAsPlayerNull()
	{
		final Creature effector = mock(Creature.class);
		when(effector.asPlayer()).thenReturn(null);
		
		final ConditionPlayerWeight condition = new ConditionPlayerWeight(50);
		assertTrue(condition.testImpl(effector, null, null, null));
	}
	
	@Test
	void returnsTrueWhenMaxLoadNotPositive()
	{
		final Player player = mock(Player.class);
		when(player.asPlayer()).thenReturn(player);
		when(player.getMaxLoad()).thenReturn(0);
		
		final ConditionPlayerWeight condition = new ConditionPlayerWeight(50);
		assertTrue(condition.testImpl(player, null, null, null));
	}
	
	@Test
	void returnsTrueWhenWeightBelowThreshold()
	{
		final Player player = mock(Player.class);
		when(player.asPlayer()).thenReturn(player);
		when(player.getMaxLoad()).thenReturn(100);
		when(player.getCurrentLoad()).thenReturn(40);
		when(player.getBonusWeightPenalty()).thenReturn(0);
		when(player.getDietMode()).thenReturn(false);
		
		final ConditionPlayerWeight condition = new ConditionPlayerWeight(50);
		assertTrue(condition.testImpl(player, null, null, null));
	}
	
	@Test
	void returnsFalseWhenWeightAtOrAboveThresholdWithoutDiet()
	{
		final Player player = mock(Player.class);
		when(player.asPlayer()).thenReturn(player);
		when(player.getMaxLoad()).thenReturn(100);
		when(player.getCurrentLoad()).thenReturn(50);
		when(player.getBonusWeightPenalty()).thenReturn(0);
		when(player.getDietMode()).thenReturn(false);
		
		final ConditionPlayerWeight condition = new ConditionPlayerWeight(50);
		assertFalse(condition.testImpl(player, null, null, null));
	}
	
	@Test
	void returnsTrueWhenDietModeEvenIfHeavy()
	{
		final Player player = mock(Player.class);
		when(player.asPlayer()).thenReturn(player);
		when(player.getMaxLoad()).thenReturn(100);
		when(player.getCurrentLoad()).thenReturn(90);
		when(player.getBonusWeightPenalty()).thenReturn(0);
		when(player.getDietMode()).thenReturn(true);
		
		final ConditionPlayerWeight condition = new ConditionPlayerWeight(50);
		assertTrue(condition.testImpl(player, null, null, null));
	}
}
