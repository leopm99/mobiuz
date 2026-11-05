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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.l2journey.gameserver.data.xml.ExperienceData;
import com.l2journey.gameserver.data.xml.PetDataTable;
import com.l2journey.gameserver.model.actor.Playable;
import com.l2journey.gameserver.model.actor.instance.Pet;
import com.l2journey.gameserver.model.actor.templates.NpcTemplate;
import com.l2journey.gameserver.model.events.EventDispatcher;
import com.l2journey.gameserver.model.events.EventType;
import com.l2journey.gameserver.model.events.ListenersContainer;
import com.l2journey.gameserver.model.item.Weapon;

/**
 * Unit tests for null-safe playable access in {@link PlayableStat}.
 */
class PlayableStatTest
{
	private static final int NPC_ID = 12077;
	private static final byte PET_MIN_LEVEL = 15;
	
	@Test
	void getActiveCharReturnsPlayableAndRejectsNullCreature()
	{
		final Playable playable = mock(Playable.class);
		final PlayableStat stat = new PlayableStat(playable);
		assertEquals(playable, stat.getActiveChar());
		
		final PlayableStat nullStat = new PlayableStat(null);
		assertThrows(IllegalStateException.class, nullStat::getActiveChar);
	}
	
	@Test
	void physicalAttackAngleUsesWeaponOrSuperDefault()
	{
		final Playable playable = mock(Playable.class);
		final Weapon weapon = mock(Weapon.class);
		when(playable.getActiveWeaponItem()).thenReturn(weapon);
		when(weapon.getBaseAttackAngle()).thenReturn(90);
		
		final PlayableStat withWeapon = new PlayableStat(playable);
		assertEquals(90, withWeapon.getPhysicalAttackAngle());
		
		when(playable.getActiveWeaponItem()).thenReturn(null);
		assertEquals(0, withWeapon.getPhysicalAttackAngle());
	}
	
	@Test
	void removeExpWithoutPetLeavesMinimumLevelOne()
	{
		final Playable playable = mock(Playable.class);
		final ExperienceData experience = mock(ExperienceData.class);
		final PlayableStat stat = new PlayableStat(playable);
		stat.setExp(10L);
		
		try (MockedStatic<ExperienceData> experienceData = mockStatic(ExperienceData.class))
		{
			experienceData.when(ExperienceData::getInstance).thenReturn(experience);
			when(experience.getMaxLevel()).thenReturn((byte) 1);
			when(experience.getExpForLevel(anyInt())).thenReturn(100L);
			
			assertTrue(stat.removeExp(1));
			assertEquals(9L, stat.getExp());
		}
	}
	
	@Test
	void addExpAndRemoveExpUsePetMinimumLevel()
	{
		final Pet pet = mock(Pet.class);
		final NpcTemplate template = mock(NpcTemplate.class);
		final ExperienceData experience = mock(ExperienceData.class);
		final PetDataTable petTable = mock(PetDataTable.class);
		final EventDispatcher dispatcher = mock(EventDispatcher.class);
		when(pet.getTemplate()).thenReturn(template);
		when(template.getId()).thenReturn(NPC_ID);
		
		final PlayableStat stat = new PlayableStat(pet);
		try (MockedStatic<ExperienceData> experienceData = mockStatic(ExperienceData.class);
			MockedStatic<PetDataTable> petData = mockStatic(PetDataTable.class);
			MockedStatic<EventDispatcher> events = mockStatic(EventDispatcher.class))
		{
			experienceData.when(ExperienceData::getInstance).thenReturn(experience);
			petData.when(PetDataTable::getInstance).thenReturn(petTable);
			events.when(EventDispatcher::getInstance).thenReturn(dispatcher);
			when(dispatcher.hasListener(any(EventType.class), any(ListenersContainer.class))).thenReturn(false);
			when(petTable.getPetMinLevel(NPC_ID)).thenReturn((int) PET_MIN_LEVEL);
			when(experience.getMaxLevel()).thenReturn(PET_MIN_LEVEL);
			when(experience.getExpForLevel(anyInt())).thenReturn(100L);
			
			assertTrue(stat.addExp(1));
			assertEquals(1L, stat.getExp());
			
			assertTrue(stat.removeExp(1));
			assertEquals(0L, stat.getExp());
		}
	}
	
	@Test
	void petMinimumLevelNullTemplateThrows()
	{
		final Pet pet = mock(Pet.class);
		final ExperienceData experience = mock(ExperienceData.class);
		final EventDispatcher dispatcher = mock(EventDispatcher.class);
		when(pet.getTemplate()).thenReturn(null);
		
		final PlayableStat stat = new PlayableStat(pet);
		try (MockedStatic<ExperienceData> experienceData = mockStatic(ExperienceData.class);
			MockedStatic<EventDispatcher> events = mockStatic(EventDispatcher.class))
		{
			experienceData.when(ExperienceData::getInstance).thenReturn(experience);
			events.when(EventDispatcher::getInstance).thenReturn(dispatcher);
			when(dispatcher.hasListener(any(EventType.class), any(ListenersContainer.class))).thenReturn(false);
			when(experience.getMaxLevel()).thenReturn((byte) 1);
			when(experience.getExpForLevel(anyInt())).thenReturn(100L);
			
			assertThrows(NullPointerException.class, () -> stat.removeExp(1));
			assertThrows(NullPointerException.class, () -> stat.addExp(1));
		}
	}
	
	@Test
	void addExpReturnsEarlyAtMaxExp()
	{
		final Playable playable = mock(Playable.class);
		final ExperienceData experience = mock(ExperienceData.class);
		final EventDispatcher dispatcher = mock(EventDispatcher.class);
		final PlayableStat stat = new PlayableStat(playable);
		
		try (MockedStatic<ExperienceData> experienceData = mockStatic(ExperienceData.class);
			MockedStatic<EventDispatcher> events = mockStatic(EventDispatcher.class))
		{
			experienceData.when(ExperienceData::getInstance).thenReturn(experience);
			events.when(EventDispatcher::getInstance).thenReturn(dispatcher);
			when(dispatcher.hasListener(any(EventType.class), any(ListenersContainer.class))).thenReturn(false);
			when(experience.getMaxLevel()).thenReturn((byte) 1);
			when(experience.getExpForLevel(anyInt())).thenReturn(100L);
			stat.setExp(99L);
			
			assertTrue(stat.addExp(1));
			assertEquals(99L, stat.getExp());
		}
	}
}
