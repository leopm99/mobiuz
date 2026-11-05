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
import com.l2journey.gameserver.model.item.ItemTemplate;
import com.l2journey.gameserver.model.item.instance.Item;
import com.l2journey.gameserver.model.item.type.ArmorType;
import com.l2journey.gameserver.model.item.type.WeaponType;
import com.l2journey.gameserver.model.itemcontainer.Inventory;
import com.l2journey.gameserver.model.itemcontainer.PlayerInventory;

/**
 * Remaining armor and non-player branches of {@link ConditionUsingItemType}.
 */
class ConditionUsingItemTypeTest
{
	/** NONE alone does not set `_armor`; include LIGHT so the armor branch (and NONE checks) run. */
	private static final int ARMOR_WITH_NONE = ArmorType.NONE.mask() | ArmorType.LIGHT.mask();
	
	@Test
	void returnsFalseWhenEffectorNull()
	{
		assertFalse(new ConditionUsingItemType(WeaponType.SWORD.mask()).testImpl(null, null, null, null));
	}
	
	@Test
	void nonPlayerReturnsTrueWhenAttackTypeMatchesWeaponMask()
	{
		final Creature effector = mock(Creature.class);
		when(effector.isPlayer()).thenReturn(false);
		when(effector.getAttackType()).thenReturn(WeaponType.SWORD);
		
		assertTrue(new ConditionUsingItemType(WeaponType.SWORD.mask()).testImpl(effector, null, null, null));
	}
	
	@Test
	void nonPlayerReturnsFalseWhenAttackTypeMissesWeaponMask()
	{
		final Creature effector = mock(Creature.class);
		when(effector.isPlayer()).thenReturn(false);
		when(effector.getAttackType()).thenReturn(WeaponType.BOW);
		
		assertFalse(new ConditionUsingItemType(WeaponType.SWORD.mask()).testImpl(effector, null, null, null));
	}
	
	@Test
	void nonPlayerReturnsFalseForArmorMask()
	{
		final Creature effector = mock(Creature.class);
		when(effector.isPlayer()).thenReturn(false);
		when(effector.getAttackType()).thenReturn(WeaponType.SWORD);
		
		assertFalse(new ConditionUsingItemType(ArmorType.LIGHT.mask()).testImpl(effector, null, null, null));
	}
	
	@Test
	void emptyArmorSlotsTrueWhenNoneBitRequired()
	{
		final Player player = mock(Player.class);
		final PlayerInventory inventory = mock(PlayerInventory.class);
		when(player.isPlayer()).thenReturn(true);
		when(player.getInventory()).thenReturn(inventory);
		when(inventory.getPaperdollItem(Inventory.PAPERDOLL_CHEST)).thenReturn(null);
		when(inventory.getPaperdollItem(Inventory.PAPERDOLL_LEGS)).thenReturn(null);
		
		assertTrue(new ConditionUsingItemType(ARMOR_WITH_NONE).testImpl(player, null, null, null));
	}
	
	@Test
	void fullArmorMatchesChestMask()
	{
		final Player player = mock(Player.class);
		final PlayerInventory inventory = mock(PlayerInventory.class);
		final Item chest = mock(Item.class);
		final ItemTemplate chestTemplate = mock(ItemTemplate.class);
		when(player.isPlayer()).thenReturn(true);
		when(player.getInventory()).thenReturn(inventory);
		when(inventory.getPaperdollItem(Inventory.PAPERDOLL_CHEST)).thenReturn(chest);
		when(inventory.getPaperdollItem(Inventory.PAPERDOLL_LEGS)).thenReturn(null);
		when(chest.getTemplate()).thenReturn(chestTemplate);
		when(chestTemplate.getBodyPart()).thenReturn(ItemTemplate.SLOT_FULL_ARMOR);
		when(chestTemplate.getItemMask()).thenReturn(ArmorType.HEAVY.mask());
		
		assertTrue(new ConditionUsingItemType(ArmorType.HEAVY.mask()).testImpl(player, null, null, null));
		assertFalse(new ConditionUsingItemType(ArmorType.LIGHT.mask()).testImpl(player, null, null, null));
	}
	
	@Test
	void chestOnlyNonFullArmorUsesNoneBit()
	{
		final Player player = mock(Player.class);
		final PlayerInventory inventory = mock(PlayerInventory.class);
		final Item chest = mock(Item.class);
		final ItemTemplate chestTemplate = mock(ItemTemplate.class);
		when(player.isPlayer()).thenReturn(true);
		when(player.getInventory()).thenReturn(inventory);
		when(inventory.getPaperdollItem(Inventory.PAPERDOLL_CHEST)).thenReturn(chest);
		when(inventory.getPaperdollItem(Inventory.PAPERDOLL_LEGS)).thenReturn(null);
		when(chest.getTemplate()).thenReturn(chestTemplate);
		when(chestTemplate.getBodyPart()).thenReturn(ItemTemplate.SLOT_CHEST);
		
		assertTrue(new ConditionUsingItemType(ARMOR_WITH_NONE).testImpl(player, null, null, null));
		assertFalse(new ConditionUsingItemType(ArmorType.LIGHT.mask()).testImpl(player, null, null, null));
	}
	
	@Test
	void chestAndLegsSameMaskHitAndMiss()
	{
		final Player player = mock(Player.class);
		final PlayerInventory inventory = mock(PlayerInventory.class);
		final Item chest = mock(Item.class);
		final Item legs = mock(Item.class);
		final ItemTemplate chestTemplate = mock(ItemTemplate.class);
		final ItemTemplate legsTemplate = mock(ItemTemplate.class);
		when(player.isPlayer()).thenReturn(true);
		when(player.getInventory()).thenReturn(inventory);
		when(inventory.getPaperdollItem(Inventory.PAPERDOLL_CHEST)).thenReturn(chest);
		when(inventory.getPaperdollItem(Inventory.PAPERDOLL_LEGS)).thenReturn(legs);
		when(chest.getTemplate()).thenReturn(chestTemplate);
		when(legs.getTemplate()).thenReturn(legsTemplate);
		when(chestTemplate.getItemMask()).thenReturn(ArmorType.LIGHT.mask());
		when(legsTemplate.getItemMask()).thenReturn(ArmorType.LIGHT.mask());
		
		assertTrue(new ConditionUsingItemType(ArmorType.LIGHT.mask()).testImpl(player, null, null, null));
		assertFalse(new ConditionUsingItemType(ArmorType.HEAVY.mask()).testImpl(player, null, null, null));
	}
	
	@Test
	void chestAndLegsDifferentMaskUsesNoneBit()
	{
		final Player player = mock(Player.class);
		final PlayerInventory inventory = mock(PlayerInventory.class);
		final Item chest = mock(Item.class);
		final Item legs = mock(Item.class);
		final ItemTemplate chestTemplate = mock(ItemTemplate.class);
		final ItemTemplate legsTemplate = mock(ItemTemplate.class);
		when(player.isPlayer()).thenReturn(true);
		when(player.getInventory()).thenReturn(inventory);
		when(inventory.getPaperdollItem(Inventory.PAPERDOLL_CHEST)).thenReturn(chest);
		when(inventory.getPaperdollItem(Inventory.PAPERDOLL_LEGS)).thenReturn(legs);
		when(chest.getTemplate()).thenReturn(chestTemplate);
		when(legs.getTemplate()).thenReturn(legsTemplate);
		when(chestTemplate.getItemMask()).thenReturn(ArmorType.LIGHT.mask());
		when(legsTemplate.getItemMask()).thenReturn(ArmorType.HEAVY.mask());
		
		assertTrue(new ConditionUsingItemType(ARMOR_WITH_NONE).testImpl(player, null, null, null));
		assertFalse(new ConditionUsingItemType(ArmorType.LIGHT.mask()).testImpl(player, null, null, null));
	}
	
	@Test
	void wearedMaskMissReturnsFalse()
	{
		final Player player = mock(Player.class);
		final PlayerInventory inventory = mock(PlayerInventory.class);
		when(player.isPlayer()).thenReturn(true);
		when(player.getInventory()).thenReturn(inventory);
		when(inventory.getWearedMask()).thenReturn(WeaponType.BOW.mask());
		
		assertFalse(new ConditionUsingItemType(WeaponType.SWORD.mask()).testImpl(player, null, null, null));
	}
}
