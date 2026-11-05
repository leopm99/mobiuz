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

import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.item.type.ArmorType;
import com.l2journey.gameserver.model.item.type.WeaponType;
import com.l2journey.gameserver.model.itemcontainer.Inventory;
import com.l2journey.gameserver.model.itemcontainer.PlayerInventory;

/**
 * Null-safe inventory access in {@link ConditionUsingItemType}.
 */
class ConditionUsingItemTypeNullGuardTest
{
	@Test
	void returnsFalseWhenPlayerInventoryMissingForArmor()
	{
		final Player player = mock(Player.class);
		when(player.isPlayer()).thenReturn(true);
		when(player.getInventory()).thenReturn(null);
		
		final ConditionUsingItemType condition = new ConditionUsingItemType(ArmorType.LIGHT.mask());
		assertFalse(condition.testImpl(player, null, null, null));
	}
	
	@Test
	void returnsFalseWhenPlayerInventoryMissingForWeaponMask()
	{
		final Player player = mock(Player.class);
		when(player.isPlayer()).thenReturn(true);
		when(player.getInventory()).thenReturn(null);
		
		final ConditionUsingItemType condition = new ConditionUsingItemType(WeaponType.SWORD.mask());
		assertFalse(condition.testImpl(player, null, null, null));
	}
	
	@Test
	void returnsTrueWhenWearedMaskMatches()
	{
		final Player player = mock(Player.class);
		final PlayerInventory inventory = mock(PlayerInventory.class);
		when(player.isPlayer()).thenReturn(true);
		when(player.getInventory()).thenReturn(inventory);
		when(inventory.getWearedMask()).thenReturn(WeaponType.SWORD.mask());
		
		final ConditionUsingItemType condition = new ConditionUsingItemType(WeaponType.SWORD.mask());
		assertTrue(condition.testImpl(player, null, null, null));
	}
	
	@Test
	void returnsFalseWhenArmorSlotsEmptyAndLightMaskRequired()
	{
		final Player player = mock(Player.class);
		final PlayerInventory inventory = mock(PlayerInventory.class);
		when(player.isPlayer()).thenReturn(true);
		when(player.getInventory()).thenReturn(inventory);
		when(inventory.getPaperdollItem(Inventory.PAPERDOLL_CHEST)).thenReturn(null);
		when(inventory.getPaperdollItem(Inventory.PAPERDOLL_LEGS)).thenReturn(null);
		
		final ConditionUsingItemType condition = new ConditionUsingItemType(ArmorType.LIGHT.mask());
		assertFalse(condition.testImpl(player, null, null, null));
	}
}
