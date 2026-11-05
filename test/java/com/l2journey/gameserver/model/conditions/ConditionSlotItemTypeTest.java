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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.item.ItemTemplate;
import com.l2journey.gameserver.model.item.instance.Item;
import com.l2journey.gameserver.model.item.type.WeaponType;
import com.l2journey.gameserver.model.itemcontainer.Inventory;
import com.l2journey.gameserver.model.itemcontainer.PlayerInventory;

/**
 * Remaining branches of {@link ConditionSlotItemType}.
 */
class ConditionSlotItemTypeTest
{
	private static final int SLOT = Inventory.PAPERDOLL_RHAND;
	private static final int MASK = WeaponType.SWORD.mask();
	
	@Test
	void returnsFalseWhenNotPlayer()
	{
		final Creature effector = mock(Creature.class);
		when(effector.isPlayer()).thenReturn(false);
		
		assertFalse(new ConditionSlotItemType(SLOT, MASK).testImpl(effector, null, null, null));
	}
	
	@Test
	void returnsFalseWhenMaskMisses()
	{
		final Player player = mock(Player.class);
		final PlayerInventory inventory = mock(PlayerInventory.class);
		final Item item = mock(Item.class);
		final ItemTemplate template = mock(ItemTemplate.class);
		when(player.isPlayer()).thenReturn(true);
		when(player.getInventory()).thenReturn(inventory);
		when(inventory.getPaperdollItem(SLOT)).thenReturn(item);
		when(item.getTemplate()).thenReturn(template);
		when(template.getItemMask()).thenReturn(WeaponType.BOW.mask());
		
		assertFalse(new ConditionSlotItemType(SLOT, MASK).testImpl(player, null, null, null));
	}
}
