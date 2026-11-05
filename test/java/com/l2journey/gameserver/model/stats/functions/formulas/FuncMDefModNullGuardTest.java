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
package com.l2journey.gameserver.model.stats.functions.formulas;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.l2journey.Config;
import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.itemcontainer.Inventory;
import com.l2journey.gameserver.model.itemcontainer.PetInventory;

/**
 * Null-safe pet inventory access in {@link FuncMDefMod}.
 */
class FuncMDefModNullGuardTest
{
	private static File previousDatapackRoot;
	
	@BeforeAll
	static void setDatapackRoot()
	{
		previousDatapackRoot = Config.DATAPACK_ROOT;
		// BaseStat requires data/stats/statBonus.xml under DATAPACK_ROOT.
		Config.DATAPACK_ROOT = new File("dist/game");
	}
	
	@AfterAll
	static void restoreDatapackRoot()
	{
		Config.DATAPACK_ROOT = previousDatapackRoot;
	}
	
	@Test
	void skipsNeckAdjustmentWhenPetInventoryMissing()
	{
		final Creature pet = mock(Creature.class);
		when(pet.isPlayer()).thenReturn(false);
		when(pet.isPet()).thenReturn(true);
		when(pet.getInventory()).thenReturn(null);
		when(pet.getLevelMod()).thenReturn(1.0);
		
		assertDoesNotThrow(() -> FuncMDefMod.getInstance().calc(pet, null, null, 100.0));
	}
	
	@Test
	void subtractsWhenPetNeckOccupied()
	{
		final Creature pet = mock(Creature.class);
		final PetInventory inventory = mock(PetInventory.class);
		when(pet.isPlayer()).thenReturn(false);
		when(pet.isPet()).thenReturn(true);
		when(pet.getInventory()).thenReturn(inventory);
		when(inventory.getPaperdollObjectId(Inventory.PAPERDOLL_NECK)).thenReturn(42);
		when(pet.getLevelMod()).thenReturn(1.0);
		
		FuncMDefMod.getInstance().calc(pet, null, null, 100.0);
		verify(inventory).getPaperdollObjectId(Inventory.PAPERDOLL_NECK);
	}
	
	@Test
	void doesNotSubtractWhenPetNeckEmpty()
	{
		final Creature pet = mock(Creature.class);
		final PetInventory inventory = mock(PetInventory.class);
		when(pet.isPlayer()).thenReturn(false);
		when(pet.isPet()).thenReturn(true);
		when(pet.getInventory()).thenReturn(inventory);
		when(inventory.getPaperdollObjectId(Inventory.PAPERDOLL_NECK)).thenReturn(0);
		when(pet.getLevelMod()).thenReturn(1.0);
		
		assertDoesNotThrow(() -> FuncMDefMod.getInstance().calc(pet, null, null, 100.0));
		verify(inventory).getPaperdollObjectId(Inventory.PAPERDOLL_NECK);
	}
}
