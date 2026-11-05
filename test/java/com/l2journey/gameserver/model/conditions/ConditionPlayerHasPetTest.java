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

import org.junit.jupiter.api.Test;

import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.Summon;
import com.l2journey.gameserver.model.actor.instance.Pet;
import com.l2journey.gameserver.model.item.instance.Item;

/**
 * Characterization of {@link ConditionPlayerHasPet}.
 */
class ConditionPlayerHasPetTest
{
	@Test
	void listWithZeroAlwaysReturnsTrue()
	{
		final Creature effector = mock(Creature.class);
		final ConditionPlayerHasPet condition = new ConditionPlayerHasPet(Collections.singletonList(0));
		assertTrue(condition.testImpl(effector, null, null, null));
	}
	
	@Test
	void returnsFalseWhenAsPlayerNull()
	{
		final Creature effector = mock(Creature.class);
		when(effector.asPlayer()).thenReturn(null);
		
		final ConditionPlayerHasPet condition = new ConditionPlayerHasPet(Collections.singletonList(2375));
		assertFalse(condition.testImpl(effector, null, null, null));
	}
	
	@Test
	void returnsFalseWhenSummonNotPet()
	{
		final Player player = mock(Player.class);
		final Summon summon = mock(Summon.class);
		when(player.asPlayer()).thenReturn(player);
		when(player.getSummon()).thenReturn(summon);
		
		final ConditionPlayerHasPet condition = new ConditionPlayerHasPet(Collections.singletonList(2375));
		assertFalse(condition.testImpl(player, null, null, null));
	}
	
	@Test
	void returnsFalseWhenControlItemMissing()
	{
		final Player player = mock(Player.class);
		final Pet pet = mock(Pet.class);
		when(player.asPlayer()).thenReturn(player);
		when(player.getSummon()).thenReturn(pet);
		when(pet.asPet()).thenReturn(pet);
		when(pet.getControlItem()).thenReturn(null);
		
		final ConditionPlayerHasPet condition = new ConditionPlayerHasPet(Collections.singletonList(2375));
		assertFalse(condition.testImpl(player, null, null, null));
	}
	
	@Test
	void returnsTrueWhenControlItemIdListed()
	{
		final Player player = mock(Player.class);
		final Pet pet = mock(Pet.class);
		final Item controlItem = mock(Item.class);
		when(player.asPlayer()).thenReturn(player);
		when(player.getSummon()).thenReturn(pet);
		when(pet.asPet()).thenReturn(pet);
		when(pet.getControlItem()).thenReturn(controlItem);
		when(controlItem.getId()).thenReturn(2375);
		
		final ConditionPlayerHasPet condition = new ConditionPlayerHasPet(Collections.singletonList(2375));
		assertTrue(condition.testImpl(player, null, null, null));
	}
	
	@Test
	void returnsFalseWhenControlItemIdNotListed()
	{
		final Player player = mock(Player.class);
		final Pet pet = mock(Pet.class);
		final Item controlItem = mock(Item.class);
		when(player.asPlayer()).thenReturn(player);
		when(player.getSummon()).thenReturn(pet);
		when(pet.asPet()).thenReturn(pet);
		when(pet.getControlItem()).thenReturn(controlItem);
		when(controlItem.getId()).thenReturn(1);
		
		final ConditionPlayerHasPet condition = new ConditionPlayerHasPet(Collections.singletonList(2375));
		assertFalse(condition.testImpl(player, null, null, null));
	}
}
