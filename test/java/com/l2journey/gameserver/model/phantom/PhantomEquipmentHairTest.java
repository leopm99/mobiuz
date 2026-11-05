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
package com.l2journey.gameserver.model.phantom;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import com.l2journey.commons.util.Rnd;

import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.item.enums.ItemProcessType;
import com.l2journey.gameserver.model.item.instance.Item;
import com.l2journey.gameserver.model.itemcontainer.Inventory;
import com.l2journey.gameserver.model.itemcontainer.PlayerInventory;

/**
 * Unit tests for {@link PhantomEquipment#ensureRandomHair}.
 */
class PhantomEquipmentHairTest
{
	@AfterEach
	void tearDown()
	{
		PhantomEquipment.clearRuntimeState();
	}

	@Test
	void ensureRandomHairSkipsNullAndDead()
	{
		PhantomEquipment.ensureRandomHair(null);

		Player dead = mock(Player.class);
		when(dead.isDead()).thenReturn(true);
		PhantomEquipment.ensureRandomHair(dead);
		verify(dead, never()).getInventory();
	}

	@Test
	void ensureRandomHairKeepsNonDemonPoolAccessory()
	{
		Player bot = mock(Player.class);
		PlayerInventory inventory = mock(PlayerInventory.class);
		Item hair = mock(Item.class);
		when(bot.isDead()).thenReturn(false);
		when(bot.getObjectId()).thenReturn(42);
		when(bot.getInventory()).thenReturn(inventory);
		when(inventory.getPaperdollItem(Inventory.PAPERDOLL_HAIR)).thenReturn(hair);
		when(inventory.getPaperdollItem(Inventory.PAPERDOLL_HAIR2)).thenReturn(null);
		when(hair.getId()).thenReturn(5808);

		PhantomEquipment.ensureRandomHair(bot);
		verify(inventory, never()).addItem(any(), anyInt(), anyLong(), any(), any());
		verify(inventory, never()).equipItem(any());
	}

	@Test
	void ensureRandomHairReplacesDemonHornsOnce()
	{
		Player bot = mock(Player.class);
		PlayerInventory inventory = mock(PlayerInventory.class);
		Item horns = mock(Item.class);
		Item replacement = mock(Item.class);
		when(bot.isDead()).thenReturn(false);
		when(bot.getObjectId()).thenReturn(77);
		when(bot.getInventory()).thenReturn(inventory);
		when(inventory.getPaperdollItem(Inventory.PAPERDOLL_HAIR)).thenReturn(horns);
		when(inventory.getPaperdollItem(Inventory.PAPERDOLL_HAIR2)).thenReturn(null);
		when(horns.getId()).thenReturn(8187);
		when(inventory.getItemByItemId(anyInt())).thenReturn(null);
		when(inventory.addItem(eq(ItemProcessType.REWARD), anyInt(), eq(1L), eq(bot), eq(null))).thenReturn(replacement);
		when(replacement.isEquipped()).thenReturn(false);

		PhantomEquipment.ensureRandomHair(bot);
		verify(inventory).equipItem(replacement);
		verify(bot).broadcastUserInfo();

		when(horns.getId()).thenReturn(8918);
		PhantomEquipment.ensureRandomHair(bot);
		verify(inventory).equipItem(replacement);
	}

	@Test
	void ensureRandomHairKeepsAssignmentWhenNothingIsWorn()
	{
		Player bot = mock(Player.class);
		PlayerInventory inventory = mock(PlayerInventory.class);
		Item replacement = mock(Item.class);
		when(bot.isDead()).thenReturn(false);
		when(bot.getObjectId()).thenReturn(88);
		when(bot.getInventory()).thenReturn(inventory);
		when(inventory.getPaperdollItem(Inventory.PAPERDOLL_HAIR)).thenReturn(null);
		when(inventory.getPaperdollItem(Inventory.PAPERDOLL_HAIR2)).thenReturn(null);
		when(inventory.getItemByItemId(anyInt())).thenReturn(null);
		when(inventory.addItem(eq(ItemProcessType.REWARD), anyInt(), eq(1L), eq(bot), eq(null))).thenReturn(replacement);
		when(replacement.isEquipped()).thenReturn(false);

		PhantomEquipment.ensureRandomHair(bot);
		PhantomEquipment.ensureRandomHair(bot);
		verify(inventory).equipItem(replacement);
	}

	@Test
	void ensureRandomHairNeverRollsDemonHorns()
	{
		try (MockedStatic<Rnd> rnd = mockStatic(Rnd.class))
		{
			for (int index = 0; index < 12; index++)
			{
				PhantomEquipment.clearRuntimeState();
				rnd.when(() -> Rnd.get(anyInt())).thenReturn(index);

				Player bot = mock(Player.class);
				PlayerInventory inventory = mock(PlayerInventory.class);
				Item replacement = mock(Item.class);
				when(bot.isDead()).thenReturn(false);
				when(bot.getObjectId()).thenReturn(300 + index);
				when(bot.getInventory()).thenReturn(inventory);
				when(inventory.getPaperdollItem(Inventory.PAPERDOLL_HAIR)).thenReturn(null);
				when(inventory.getPaperdollItem(Inventory.PAPERDOLL_HAIR2)).thenReturn(null);
				when(inventory.getItemByItemId(anyInt())).thenReturn(null);
				when(inventory.addItem(eq(ItemProcessType.REWARD), anyInt(), eq(1L), eq(bot), eq(null))).thenReturn(replacement);
				when(replacement.isEquipped()).thenReturn(false);

				PhantomEquipment.ensureRandomHair(bot);

				ArgumentCaptor<Integer> rolled = ArgumentCaptor.forClass(Integer.class);
				verify(inventory).addItem(eq(ItemProcessType.REWARD), rolled.capture(), eq(1L), eq(bot), eq(null));
				assertNotEquals(8187, rolled.getValue());
			}
		}
	}
}
