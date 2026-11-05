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

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.item.instance.Item;
import com.l2journey.gameserver.model.itemcontainer.Inventory;
import com.l2journey.gameserver.model.itemcontainer.PlayerInventory;

/**
 * Characterization of {@link ConditionAgathionItemId}.
 */
class ConditionAgathionItemIdTest
{
	private static final int BRACELET_ID = 10312;
	
	@Test
	void returnsFalseWhenEffectorNull()
	{
		final ConditionAgathionItemId condition = new ConditionAgathionItemId(Collections.singletonList(BRACELET_ID));
		assertFalse(condition.testImpl(null, null, null, null));
	}
	
	@Test
	void returnsFalseWhenNotPlayer()
	{
		final Creature effector = mock(Creature.class);
		when(effector.isPlayer()).thenReturn(false);
		
		final ConditionAgathionItemId condition = new ConditionAgathionItemId(Collections.singletonList(BRACELET_ID));
		assertFalse(condition.testImpl(effector, null, null, null));
	}
	
	@Test
	void returnsFalseWhenBraceletMissing()
	{
		final Player player = mock(Player.class);
		final PlayerInventory inventory = mock(PlayerInventory.class);
		when(player.isPlayer()).thenReturn(true);
		when(player.asPlayer()).thenReturn(player);
		when(player.getInventory()).thenReturn(inventory);
		when(inventory.getPaperdollItem(Inventory.PAPERDOLL_LBRACELET)).thenReturn(null);
		
		final ConditionAgathionItemId condition = new ConditionAgathionItemId(Collections.singletonList(BRACELET_ID));
		assertFalse(condition.testImpl(player, null, null, null));
	}
	
	@Test
	void returnsTrueWhenBraceletIdListed()
	{
		final Player player = mock(Player.class);
		final PlayerInventory inventory = mock(PlayerInventory.class);
		final Item bracelet = mock(Item.class);
		when(player.isPlayer()).thenReturn(true);
		when(player.asPlayer()).thenReturn(player);
		when(player.getInventory()).thenReturn(inventory);
		when(inventory.getPaperdollItem(Inventory.PAPERDOLL_LBRACELET)).thenReturn(bracelet);
		when(bracelet.getId()).thenReturn(BRACELET_ID);
		
		final ConditionAgathionItemId condition = new ConditionAgathionItemId(Arrays.asList(BRACELET_ID, 99999));
		assertTrue(condition.testImpl(player, null, null, null));
	}
	
	@Test
	void returnsFalseWhenBraceletIdNotListed()
	{
		final Player player = mock(Player.class);
		final PlayerInventory inventory = mock(PlayerInventory.class);
		final Item bracelet = mock(Item.class);
		when(player.isPlayer()).thenReturn(true);
		when(player.asPlayer()).thenReturn(player);
		when(player.getInventory()).thenReturn(inventory);
		when(inventory.getPaperdollItem(Inventory.PAPERDOLL_LBRACELET)).thenReturn(bracelet);
		when(bracelet.getId()).thenReturn(1);
		
		final ConditionAgathionItemId condition = new ConditionAgathionItemId(Collections.singletonList(BRACELET_ID));
		assertFalse(condition.testImpl(player, null, null, null));
	}
}
