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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.item.enums.ItemProcessType;
import com.l2journey.gameserver.model.item.instance.Item;
import com.l2journey.gameserver.model.itemcontainer.PlayerInventory;

/**
 * Unit tests for {@link PhantomProgression#checkLowLevelItems}.
 */
class PhantomProgressionTest
{
	@AfterEach
	void tearDown()
	{
		PhantomState.clear();
	}
	
	@Test
	void mageBelowTwentyDestroysKarmianPieces()
	{
		Player bot = mock(Player.class);
		PlayerInventory inventory = mock(PlayerInventory.class);
		Item tunic = mock(Item.class);
		Item stockings = mock(Item.class);
		
		when(bot.isMageClass()).thenReturn(true);
		when(bot.getLevel()).thenReturn(19);
		when(bot.getInventory()).thenReturn(inventory);
		when(inventory.getItems()).thenReturn(List.of(tunic, stockings));
		when(tunic.getId()).thenReturn(438);
		when(stockings.getId()).thenReturn(470);
		when(tunic.isEquipped()).thenReturn(false);
		when(stockings.isEquipped()).thenReturn(false);
		
		PhantomProgression.checkLowLevelItems(bot);
		
		verify(bot).destroyItem(eq(ItemProcessType.DESTROY), eq(tunic), eq(bot), eq(false));
		verify(bot).destroyItem(eq(ItemProcessType.DESTROY), eq(stockings), eq(bot), eq(false));
	}
	
	@Test
	void mageAtTwentyKeepsStockings()
	{
		Player bot = mock(Player.class);
		PlayerInventory inventory = mock(PlayerInventory.class);
		Item stockings = mock(Item.class);
		
		when(bot.isMageClass()).thenReturn(true);
		when(bot.getLevel()).thenReturn(20);
		when(bot.getInventory()).thenReturn(inventory);
		when(inventory.getItems()).thenReturn(List.of(stockings));
		when(stockings.getId()).thenReturn(470);
		
		PhantomProgression.checkLowLevelItems(bot);
		
		verify(bot, never()).destroyItem(any(ItemProcessType.class), any(Item.class), any(Player.class), anyBoolean());
	}
	
	@Test
	void fighterDoesNotDestroyKarmian()
	{
		Player bot = mock(Player.class);
		PlayerInventory inventory = mock(PlayerInventory.class);
		Item tunic = mock(Item.class);
		
		when(bot.isMageClass()).thenReturn(false);
		when(bot.getLevel()).thenReturn(10);
		when(bot.getInventory()).thenReturn(inventory);
		when(inventory.getItems()).thenReturn(List.of(tunic));
		when(tunic.getId()).thenReturn(438);
		
		PhantomProgression.checkLowLevelItems(bot);
		
		verify(bot, never()).destroyItem(any(ItemProcessType.class), any(Item.class), any(Player.class), anyBoolean());
	}
}
