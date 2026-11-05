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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.l2journey.commons.util.Rnd;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.enums.player.PlayerClass;
import com.l2journey.gameserver.model.item.enums.ItemProcessType;
import com.l2journey.gameserver.model.item.instance.Item;
import com.l2journey.gameserver.model.itemcontainer.PlayerInventory;
import com.l2journey.gameserver.model.variables.PlayerVariables;

/**
 * Unit tests for {@link PhantomProgression}.
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

	@Test
	void healerRollRequiresLevelCapAtLeastForty()
	{
		assertFalse(PhantomProgression.allowsHealerRoll(10));
		assertFalse(PhantomProgression.allowsHealerRoll(21));
		assertFalse(PhantomProgression.allowsHealerRoll(35));
		assertTrue(PhantomProgression.allowsHealerRoll(40));
		assertTrue(PhantomProgression.allowsHealerRoll(41));
		assertTrue(PhantomProgression.allowsHealerRoll(85));

		assertEquals(PlayerClass.WIZARD, PhantomProgression.chooseHumanMageFirstClass(90, 35));
		assertEquals(PlayerClass.CLERIC, PhantomProgression.chooseHumanMageFirstClass(90, 40));
		assertEquals(PlayerClass.ELVEN_WIZARD, PhantomProgression.chooseElvenMageFirstClass(90, 21));
		assertEquals(PlayerClass.ORACLE, PhantomProgression.chooseElvenMageFirstClass(90, 41));
		assertEquals(PlayerClass.DARK_WIZARD, PhantomProgression.chooseDarkMageFirstClass(90, 10));
		assertEquals(PlayerClass.SHILLIEN_ORACLE, PhantomProgression.chooseDarkMageFirstClass(90, 53));
	}

	@Test
	void completeClassChangeGrantsSkillsImmediately()
	{
		Player bot = mock(Player.class);
		when(bot.getName()).thenReturn("TestBot");

		PhantomProgression.completeClassChange(bot, PlayerClass.CLERIC, PlayerClass.BISHOP);

		verify(bot).setPlayerClass(PlayerClass.BISHOP.getId());
		verify(bot).setBaseClass(PlayerClass.BISHOP.getId());
		verify(bot).giveAvailableSkills(true, true, true);
		verify(bot).broadcastUserInfo();
	}

	@Test
	void handleClassChangeUsesCapAndParentLookup()
	{
		assertClassChange(PlayerClass.MAGE, 20, 90, 40, PlayerClass.CLERIC);
		assertClassChange(PlayerClass.ELVEN_MAGE, 20, 90, 41, PlayerClass.ORACLE);
		assertClassChange(PlayerClass.DARK_MAGE, 20, 10, 85, PlayerClass.DARK_WIZARD);
		assertClassChange(PlayerClass.WIZARD, 40, 10, 85, PlayerClass.SORCERER);
		assertClassChange(PlayerClass.CLERIC, 40, 0, 85, PlayerClass.BISHOP);
		assertClassChange(PlayerClass.ELVEN_WIZARD, 40, 0, 85, PlayerClass.SPELLSINGER);
	}

	private static void assertClassChange(PlayerClass current, int level, int roll, int cap, PlayerClass expected)
	{
		Player bot = mock(Player.class);
		PlayerVariables variables = mock(PlayerVariables.class);
		when(bot.getPlayerClass()).thenReturn(current);
		when(bot.getLevel()).thenReturn(level);
		when(bot.getName()).thenReturn("TestBot");
		when(bot.getVariables()).thenReturn(variables);
		when(variables.getInt("PHANTOM_MAX_LEVEL_CAP", 85)).thenReturn(cap);

		try (MockedStatic<Rnd> rnd = mockStatic(Rnd.class))
		{
			rnd.when(() -> Rnd.get(org.mockito.ArgumentMatchers.anyInt())).thenReturn(roll);
			PhantomProgression.handleClassChange(bot, level);
		}

		verify(bot).setPlayerClass(expected.getId());
	}

	@Test
	void wizardSecondClassRollsFollowBands()
	{
		assertEquals(PlayerClass.SORCERER, PhantomProgression.chooseWizardSecondClass(0));
		assertEquals(PlayerClass.SORCERER, PhantomProgression.chooseWizardSecondClass(39));
		assertEquals(PlayerClass.NECROMANCER, PhantomProgression.chooseWizardSecondClass(40));
		assertEquals(PlayerClass.NECROMANCER, PhantomProgression.chooseWizardSecondClass(79));
		assertEquals(PlayerClass.WARLOCK, PhantomProgression.chooseWizardSecondClass(80));
		assertEquals(PlayerClass.WARLOCK, PhantomProgression.chooseWizardSecondClass(99));
	}
}
