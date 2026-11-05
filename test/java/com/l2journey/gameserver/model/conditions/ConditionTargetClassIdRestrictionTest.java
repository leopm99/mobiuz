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

import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.enums.player.PlayerClass;

/**
 * Characterization of {@link ConditionTargetClassIdRestriction}.
 */
class ConditionTargetClassIdRestrictionTest
{
	@Test
	void returnsFalseWhenEffectedNotPlayer()
	{
		final Creature effected = mock(Creature.class);
		when(effected.isPlayer()).thenReturn(false);
		
		final ConditionTargetClassIdRestriction condition = new ConditionTargetClassIdRestriction(Set.of(PlayerClass.FIGHTER.getId()));
		assertFalse(condition.testImpl(mock(Player.class), effected, null, null));
	}
	
	@Test
	void returnsTrueWhenClassIdListed()
	{
		final Player target = mock(Player.class);
		when(target.isPlayer()).thenReturn(true);
		when(target.asPlayer()).thenReturn(target);
		when(target.getPlayerClass()).thenReturn(PlayerClass.FIGHTER);
		
		final ConditionTargetClassIdRestriction condition = new ConditionTargetClassIdRestriction(Set.of(PlayerClass.FIGHTER.getId(), PlayerClass.MAGE.getId()));
		assertTrue(condition.testImpl(mock(Player.class), target, null, null));
	}
	
	@Test
	void returnsFalseWhenClassIdNotListed()
	{
		final Player target = mock(Player.class);
		when(target.isPlayer()).thenReturn(true);
		when(target.asPlayer()).thenReturn(target);
		when(target.getPlayerClass()).thenReturn(PlayerClass.MAGE);
		
		final ConditionTargetClassIdRestriction condition = new ConditionTargetClassIdRestriction(Collections.singleton(PlayerClass.FIGHTER.getId()));
		assertFalse(condition.testImpl(mock(Player.class), target, null, null));
	}
}
