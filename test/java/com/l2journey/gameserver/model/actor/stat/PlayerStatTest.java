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
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.l2journey.gameserver.data.xml.ExperienceData;
import com.l2journey.gameserver.model.Nevit;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.holders.player.SubClassHolder;
import com.l2journey.gameserver.model.actor.status.PlayerStatus;
import com.l2journey.gameserver.model.actor.templates.PlayerTemplate;
import com.l2journey.gameserver.model.stats.Calculator;
import com.l2journey.gameserver.model.stats.Stat;
import com.l2journey.gameserver.network.serverpackets.ExVoteSystemInfo;

/**
 * Unit tests for null-safe player access in {@link PlayerStat}.
 */
class PlayerStatTest
{
	private static final int CLASS_INDEX = 1;
	private static final byte LEVEL = 20;
	
	@Test
	void getActiveCharReturnsPlayerAndRejectsNullCreature()
	{
		final Player player = mock(Player.class);
		final PlayerStat stat = new PlayerStat(player);
		assertEquals(player, stat.getActiveChar());
		
		final PlayerStat nullStat = new PlayerStat(null);
		assertThrows(IllegalStateException.class, nullStat::getActiveChar);
	}
	
	@Test
	void baseClassExpSpAndLevelUseCreatureStat()
	{
		final Player player = mock(Player.class);
		when(player.isSubClassActive()).thenReturn(false);
		final ExperienceData experience = mock(ExperienceData.class);
		
		final PlayerStat stat = new PlayerStat(player);
		try (MockedStatic<ExperienceData> experienceData = mockStatic(ExperienceData.class))
		{
			experienceData.when(ExperienceData::getInstance).thenReturn(experience);
			when(experience.getMaxLevel()).thenReturn((byte) 85);
			
			stat.setExp(1000L);
			stat.setSp(50L);
			stat.setLevel(LEVEL);
			
			assertEquals(1000L, stat.getExp());
			assertEquals(50L, stat.getSp());
			assertEquals(LEVEL, stat.getLevel());
		}
	}
	
	@Test
	void activeSubclassUsesHolderForExpSpAndLevel()
	{
		final Player player = mock(Player.class);
		final SubClassHolder holder = mock(SubClassHolder.class);
		final Map<Integer, SubClassHolder> subclasses = new HashMap<>();
		subclasses.put(CLASS_INDEX, holder);
		when(player.isSubClassActive()).thenReturn(true);
		when(player.getClassIndex()).thenReturn(CLASS_INDEX);
		when(player.getSubClasses()).thenReturn(subclasses);
		when(holder.getExp()).thenReturn(2000L);
		when(holder.getSp()).thenReturn(75L);
		when(holder.getLevel()).thenReturn(LEVEL);
		final ExperienceData experience = mock(ExperienceData.class);
		
		final PlayerStat stat = new PlayerStat(player);
		try (MockedStatic<ExperienceData> experienceData = mockStatic(ExperienceData.class))
		{
			experienceData.when(ExperienceData::getInstance).thenReturn(experience);
			when(experience.getMaxLevel()).thenReturn((byte) 85);
			
			assertEquals(2000L, stat.getExp());
			assertEquals(75L, stat.getSp());
			assertEquals(LEVEL, stat.getLevel());
			
			stat.setExp(3000L);
			stat.setSp(80L);
			stat.setLevel((byte) 25);
			verify(holder).setExp(3000L);
			verify(holder).setSp(80L);
			verify(holder).setLevel((byte) 25);
		}
	}
	
	@Test
	void missingSubclassHolderFallsBackToBase()
	{
		final Player player = mock(Player.class);
		final Map<Integer, SubClassHolder> subclasses = new HashMap<>();
		when(player.isSubClassActive()).thenReturn(true);
		when(player.getClassIndex()).thenReturn(CLASS_INDEX);
		when(player.getSubClasses()).thenReturn(subclasses);
		final ExperienceData experience = mock(ExperienceData.class);
		
		final PlayerStat stat = new PlayerStat(player);
		try (MockedStatic<ExperienceData> experienceData = mockStatic(ExperienceData.class))
		{
			experienceData.when(ExperienceData::getInstance).thenReturn(experience);
			when(experience.getMaxLevel()).thenReturn((byte) 85);
			
			stat.setExp(500L);
			stat.setSp(10L);
			stat.setLevel(LEVEL);
			
			assertEquals(500L, stat.getExp());
			assertEquals(10L, stat.getSp());
			assertEquals(LEVEL, stat.getLevel());
		}
	}
	
	@Test
	void setLevelClampsAboveMaxOnBaseClass()
	{
		final Player player = mock(Player.class);
		when(player.isSubClassActive()).thenReturn(false);
		final ExperienceData experience = mock(ExperienceData.class);
		
		final PlayerStat stat = new PlayerStat(player);
		try (MockedStatic<ExperienceData> experienceData = mockStatic(ExperienceData.class))
		{
			experienceData.when(ExperienceData::getInstance).thenReturn(experience);
			when(experience.getMaxLevel()).thenReturn((byte) 85);
			
			stat.setLevel((byte) 100);
			assertEquals((byte) 84, stat.getLevel());
		}
	}
	
	@Test
	void maxStatsRequireTemplateAndTriggerRegenWhenCurrentDiffers()
	{
		final Player player = mock(Player.class);
		final PlayerTemplate template = mock(PlayerTemplate.class);
		final PlayerStatus status = mock(PlayerStatus.class);
		when(player.isSubClassActive()).thenReturn(false);
		when(player.getTemplate()).thenReturn(template);
		when(player.getStatus()).thenReturn(status);
		when(player.getCalculators()).thenReturn(new Calculator[Stat.values().length]);
		when(player.getLevel()).thenReturn((int) LEVEL);
		when(template.getBaseCpMax(LEVEL)).thenReturn(100f);
		when(template.getBaseHpMax(LEVEL)).thenReturn(200f);
		when(template.getBaseMpMax(LEVEL)).thenReturn(300f);
		when(status.getCurrentCp()).thenReturn(50d);
		when(status.getCurrentHp()).thenReturn(50d);
		when(status.getCurrentMp()).thenReturn(50d);
		
		final PlayerStat stat = new PlayerStat(player);
		assertEquals(100, stat.getMaxCp());
		assertEquals(200, stat.getMaxHp());
		assertEquals(300, stat.getMaxMp());
		verify(status).setCurrentCp(50d);
		verify(status).setCurrentHp(50d);
		verify(status).setCurrentMp(50d);
		
		// Second call: max unchanged, regen block skipped.
		assertEquals(100, stat.getMaxCp());
		verify(status).setCurrentCp(50d);
	}
	
	@Test
	void maxStatsSkipRegenWhenCurrentAlreadyMatches()
	{
		final Player player = mock(Player.class);
		final PlayerTemplate template = mock(PlayerTemplate.class);
		final PlayerStatus status = mock(PlayerStatus.class);
		when(player.isSubClassActive()).thenReturn(false);
		when(player.getTemplate()).thenReturn(template);
		when(player.getStatus()).thenReturn(status);
		when(player.getCalculators()).thenReturn(new Calculator[Stat.values().length]);
		when(player.getLevel()).thenReturn((int) LEVEL);
		when(template.getBaseCpMax(LEVEL)).thenReturn(100f);
		when(status.getCurrentCp()).thenReturn(100d);
		
		final PlayerStat stat = new PlayerStat(player);
		assertEquals(100, stat.getMaxCp());
		verify(status, never()).setCurrentCp(anyDouble());
	}
	
	@Test
	void nullPlayerTemplateThrows()
	{
		final Player player = mock(Player.class);
		when(player.isSubClassActive()).thenReturn(false);
		when(player.getTemplate()).thenReturn(null);
		when(player.getCalculators()).thenReturn(new Calculator[Stat.values().length]);
		when(player.getLevel()).thenReturn((int) LEVEL);
		
		final PlayerStat stat = new PlayerStat(player);
		assertThrows(NullPointerException.class, stat::getMaxCp);
	}
	
	@Test
	void vitalityLevelUsesNevitBlessing()
	{
		final Player player = mock(Player.class);
		final Nevit nevit = mock(Nevit.class);
		when(player.getNevitSystem()).thenReturn(nevit);
		when(nevit.isAdventBlessingActive()).thenReturn(true);
		
		final PlayerStat stat = new PlayerStat(player);
		assertEquals((byte) 4, stat.getVitalityLevel());
		
		when(nevit.isAdventBlessingActive()).thenReturn(false);
		assertEquals((byte) 0, stat.getVitalityLevel());
	}
	
	@Test
	void setPausedNevitHourglassStatusSendsPacket()
	{
		final Player player = mock(Player.class);
		when(player.getRecomLeft()).thenReturn(1);
		when(player.getRecomHave()).thenReturn(2);
		when(player.getNevitHourglassTime()).thenReturn(3);
		when(player.getNevitHourglassBonus()).thenReturn(4);
		when(player.getNevitHourglassStatus()).thenReturn(5);
		
		final PlayerStat stat = new PlayerStat(player);
		stat.setPausedNevitHourglassStatus(true);
		
		assertEquals(true, stat.hasPausedNevitHourglass());
		verify(player).sendPacket(any(ExVoteSystemInfo.class));
	}
}
