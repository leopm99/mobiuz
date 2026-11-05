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
package com.l2journey.gameserver.model.actor.stat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.l2journey.gameserver.data.xml.PetDataTable;
import com.l2journey.gameserver.model.PetLevelData;
import com.l2journey.gameserver.model.actor.instance.Pet;
import com.l2journey.gameserver.model.actor.templates.NpcTemplate;
import com.l2journey.gameserver.model.item.instance.Item;
import com.l2journey.gameserver.model.stats.Calculator;
import com.l2journey.gameserver.model.stats.Stat;

/**
 * Unit tests for null-safe pet level data access in {@link PetStat}.
 */
class PetStatTest
{
	private static final int NPC_ID = 12077;
	private static final byte LEVEL = 10;
	
	@Test
	void getActiveCharReturnsPetAndRejectsNullCreature()
	{
		final Pet pet = mock(Pet.class);
		final PetStat stat = new PetStat(pet);
		assertEquals(pet, stat.getActiveChar());
		
		final PetStat nullStat = new PetStat(null);
		assertThrows(IllegalStateException.class, nullStat::getActiveChar);
	}
	
	@Test
	void levelDataGettersUsePetLevelData()
	{
		final Pet pet = mock(Pet.class);
		final PetLevelData levelData = mock(PetLevelData.class);
		when(pet.getPetLevelData()).thenReturn(levelData);
		when(pet.getId()).thenReturn(NPC_ID);
		when(pet.getCalculators()).thenReturn(new Calculator[Stat.values().length]);
		when(levelData.getPetFeedBattle()).thenReturn(11);
		when(levelData.getPetFeedNormal()).thenReturn(22);
		when(levelData.getPetMaxFeed()).thenReturn(33);
		when(levelData.getPetMaxHP()).thenReturn(44f);
		when(levelData.getPetMaxMP()).thenReturn(55f);
		when(levelData.getPetMAtk()).thenReturn(66f);
		when(levelData.getPetMDef()).thenReturn(77f);
		when(levelData.getPetPAtk()).thenReturn(88f);
		when(levelData.getPetPDef()).thenReturn(99f);
		
		final PetStat stat = new PetStat(pet);
		assertEquals(11, stat.getFeedBattle());
		assertEquals(22, stat.getFeedNormal());
		assertEquals(33, stat.getMaxFeed());
		assertEquals(44, stat.getMaxHp());
		assertEquals(55, stat.getMaxMp());
		assertEquals(66d, stat.getMAtk(null, null));
		assertEquals(77d, stat.getMDef(null, null));
		assertEquals(88d, stat.getPAtk(null));
		assertEquals(99d, stat.getPDef(null));
	}
	
	@Test
	void requireLevelDataThrowsWhenPetLevelDataMissing()
	{
		final Pet pet = mock(Pet.class);
		when(pet.getPetLevelData()).thenReturn(null);
		when(pet.getId()).thenReturn(NPC_ID);
		
		final PetStat stat = new PetStat(pet);
		assertThrows(IllegalArgumentException.class, stat::getFeedBattle);
	}
	
	@Test
	void getExpForLevelReturnsMaxExpOrThrowsWhenMissing()
	{
		final Pet pet = mock(Pet.class);
		final PetLevelData levelData = mock(PetLevelData.class);
		final PetDataTable table = mock(PetDataTable.class);
		when(pet.getId()).thenReturn(NPC_ID);
		when(pet.getObjectId()).thenReturn(1001);
		when(levelData.getPetMaxExp()).thenReturn(12345L);
		
		final PetStat stat = new PetStat(pet);
		try (MockedStatic<PetDataTable> petData = mockStatic(PetDataTable.class))
		{
			petData.when(PetDataTable::getInstance).thenReturn(table);
			when(table.getPetLevelData(NPC_ID, LEVEL)).thenReturn(levelData);
			assertEquals(12345L, stat.getExpForLevel(LEVEL));
			
			when(table.getPetLevelData(NPC_ID, LEVEL)).thenReturn(null);
			assertThrows(IllegalArgumentException.class, () -> stat.getExpForLevel(LEVEL));
		}
	}
	
	@Test
	void setLevelStoresDataAndUpdatesControlItem()
	{
		final Pet pet = mock(Pet.class);
		final NpcTemplate template = mock(NpcTemplate.class);
		final PetLevelData levelData = mock(PetLevelData.class);
		final Item controlItem = mock(Item.class);
		final PetDataTable table = mock(PetDataTable.class);
		when(pet.getTemplate()).thenReturn(template);
		when(template.getId()).thenReturn(NPC_ID);
		when(pet.getControlItem()).thenReturn(controlItem);
		
		final PetStat stat = new PetStat(pet);
		try (MockedStatic<PetDataTable> petData = mockStatic(PetDataTable.class))
		{
			petData.when(PetDataTable::getInstance).thenReturn(table);
			when(table.getPetLevelData(NPC_ID, LEVEL)).thenReturn(levelData);
			
			stat.setLevel(LEVEL);
			
			verify(pet).setPetData(levelData);
			verify(pet).stopFeed();
			verify(pet).startFeed();
			verify(controlItem).setEnchantLevel(LEVEL);
			assertEquals(LEVEL, stat.getLevel());
		}
	}
	
	@Test
	void setLevelMissingDataDoesNotSetPetDataOrStopFeed()
	{
		final Pet pet = mock(Pet.class);
		final NpcTemplate template = mock(NpcTemplate.class);
		final PetDataTable table = mock(PetDataTable.class);
		when(pet.getTemplate()).thenReturn(template);
		when(template.getId()).thenReturn(NPC_ID);
		
		final PetStat stat = new PetStat(pet);
		try (MockedStatic<PetDataTable> petData = mockStatic(PetDataTable.class))
		{
			petData.when(PetDataTable::getInstance).thenReturn(table);
			when(table.getPetLevelData(NPC_ID, LEVEL)).thenReturn(null);
			
			assertThrows(IllegalArgumentException.class, () -> stat.setLevel(LEVEL));
			verify(pet, never()).setPetData(any());
			verify(pet, never()).stopFeed();
		}
	}
	
	@Test
	void setLevelNullTemplateThrows()
	{
		final Pet pet = mock(Pet.class);
		when(pet.getTemplate()).thenReturn(null);
		
		final PetStat stat = new PetStat(pet);
		assertThrows(NullPointerException.class, () -> stat.setLevel(LEVEL));
	}
}
