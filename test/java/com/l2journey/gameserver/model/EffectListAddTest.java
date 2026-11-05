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
package com.l2journey.gameserver.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.l2journey.Config;
import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.actor.stat.CreatureStat;
import com.l2journey.gameserver.model.skill.AbnormalType;
import com.l2journey.gameserver.model.skill.BuffInfo;
import com.l2journey.gameserver.model.skill.Skill;
import com.l2journey.gameserver.model.skill.enums.SkillFinishType;

/**
 * Characterization of {@link EffectList#add}, counts, stop, and blocked abnormal slots.
 */
class EffectListAddTest
{
	private static final int SKILL_A = 99001;
	private static final int SKILL_B = 99002;
	private static final int SKILL_C = 99003;
	
	enum SkillKind
	{
		BUFF,
		DANCE,
		TOGGLE
	}
	
	static Stream<Arguments> landingCases()
	{
		return Stream.of(
			Arguments.of(SkillKind.BUFF),
			Arguments.of(SkillKind.DANCE),
			Arguments.of(SkillKind.TOGGLE));
	}
	
	@Test
	void emptyListReportsZeroCountsAndNoBlockedTypes()
	{
		final EffectList effects = newEffectList(20);
		assertEquals(0, effects.getBuffCount());
		assertEquals(0, effects.getDanceCount());
		assertTrue(effects.getToggles().isEmpty());
		assertTrue(effects.getBlockedAbnormalTypes().isEmpty());
	}
	
	@Test
	void addNullLeavesQueuesUnchanged()
	{
		final EffectList effects = newEffectList(20);
		effects.add(null);
		assertEquals(0, effects.getBuffCount());
		assertEquals(0, effects.getDanceCount());
		assertTrue(effects.getToggles().isEmpty());
		assertTrue(effects.getPassives().isEmpty());
	}
	
	@Test
	void addRejectsDeadEffected()
	{
		final EffectList effects = newEffectList(20);
		final Creature effected = mock(Creature.class);
		when(effected.isDead()).thenReturn(true);
		final BuffInfo info = buff(SKILL_A, AbnormalType.NONE, SkillKind.BUFF, effected);
		effects.add(info);
		assertEquals(0, effects.getBuffCount());
		verify(info, never()).initializeEffects();
	}
	
	@Test
	void blockedAbnormalTypesRejectThenAllowAfterRemoval()
	{
		final EffectList effects = newEffectList(20);
		effects.addBlockedAbnormalTypes(EnumSet.of(AbnormalType.SPEED_UP));
		final Set<AbnormalType> blocked = effects.getBlockedAbnormalTypes();
		assertEquals(Set.of(AbnormalType.SPEED_UP), blocked);
		assertThrows(UnsupportedOperationException.class, () -> blocked.add(AbnormalType.POISON));
		
		final BuffInfo rejected = buff(SKILL_A, AbnormalType.SPEED_UP, SkillKind.BUFF);
		effects.add(rejected);
		assertEquals(0, effects.getBuffCount());
		verify(rejected, never()).initializeEffects();
		
		assertTrue(effects.removeBlockedAbnormalTypes(EnumSet.of(AbnormalType.SPEED_UP)));
		assertFalse(effects.removeBlockedAbnormalTypes(EnumSet.of(AbnormalType.SPEED_UP)));
		assertTrue(effects.getBlockedAbnormalTypes().isEmpty());
		
		final BuffInfo accepted = buff(SKILL_B, AbnormalType.SPEED_UP, SkillKind.BUFF);
		effects.add(accepted);
		assertEquals(1, effects.getBuffCount());
		verify(accepted).initializeEffects();
	}
	
	@ParameterizedTest(name = "{0}")
	@MethodSource("landingCases")
	void addRoutesSkillToExpectedQueue(SkillKind kind)
	{
		final EffectList effects = newEffectList(20);
		final BuffInfo info = buff(SKILL_A, AbnormalType.NONE, kind);
		effects.add(info);
		verify(info).initializeEffects();
		
		assertEquals(kind == SkillKind.BUFF ? 1 : 0, effects.getBuffCount());
		assertEquals(kind == SkillKind.DANCE ? 1 : 0, effects.getDanceCount());
		assertEquals(kind == SkillKind.TOGGLE ? 1 : 0, effects.getToggles().size());
		assertEquals(kind == SkillKind.BUFF, effects.getBuffs().contains(info));
		assertEquals(kind == SkillKind.DANCE, effects.getDances().contains(info));
		assertEquals(kind == SkillKind.TOGGLE, effects.getToggles().contains(info));
	}
	
	@Test
	void sameSkillIdWithNoneReplacesPreviousBuff()
	{
		final EffectList effects = newEffectList(20);
		final BuffInfo first = buff(SKILL_A, AbnormalType.NONE, SkillKind.BUFF);
		final BuffInfo second = buff(SKILL_A, AbnormalType.NONE, SkillKind.BUFF);
		effects.add(first);
		effects.add(second);
		assertEquals(1, effects.getBuffCount());
		assertFalse(effects.getBuffs().contains(first));
		assertTrue(effects.getBuffs().contains(second));
		verify(first).stopAllEffects(eq(SkillFinishType.NORMAL), anyBoolean());
		verify(second).initializeEffects();
	}
	
	@Test
	void stackedAbnormalRejectsLowerLevelAndReplacesEqualOrHigher()
	{
		final EffectList effects = newEffectList(20);
		final BuffInfo first = stackedBuff(SKILL_A, AbnormalType.SPEED_UP, 2);
		effects.add(first);
		assertEquals(1, effects.getBuffCount());
		
		final BuffInfo lower = stackedBuff(SKILL_B, AbnormalType.SPEED_UP, 1);
		effects.add(lower);
		assertEquals(1, effects.getBuffCount());
		assertTrue(effects.getBuffs().contains(first));
		assertFalse(effects.getBuffs().contains(lower));
		verify(lower, never()).initializeEffects();
		
		final BuffInfo higher = stackedBuff(SKILL_C, AbnormalType.SPEED_UP, 2);
		effects.add(higher);
		assertEquals(1, effects.getBuffCount());
		assertFalse(effects.getBuffs().contains(first));
		assertTrue(effects.getBuffs().contains(higher));
		verify(higher).initializeEffects();
	}
	
	@Test
	void stopSkillEffectsByAbnormalTypeAndSkillId()
	{
		final EffectList effects = newEffectList(20);
		assertFalse(effects.stopSkillEffects(SkillFinishType.NORMAL, AbnormalType.SPEED_UP));
		
		final BuffInfo info = stackedBuff(SKILL_A, AbnormalType.SPEED_UP, 1);
		effects.add(info);
		assertTrue(effects.stopSkillEffects(SkillFinishType.NORMAL, AbnormalType.SPEED_UP));
		assertEquals(0, effects.getBuffCount());
		assertFalse(effects.isAffectedBySkill(SKILL_A));
		
		final BuffInfo again = stackedBuff(SKILL_A, AbnormalType.SPEED_UP, 1);
		effects.add(again);
		effects.stopSkillEffects(SkillFinishType.NORMAL, 99999);
		assertEquals(1, effects.getBuffCount());
		effects.stopSkillEffects(SkillFinishType.NORMAL, SKILL_A);
		assertEquals(0, effects.getBuffCount());
		assertFalse(effects.isAffectedBySkill(SKILL_A));
	}
	
	@Test
	void buffCapEvictsOldestNonStackingBuff()
	{
		final EffectList effects = newEffectList(1);
		final BuffInfo first = buff(SKILL_A, AbnormalType.NONE, SkillKind.BUFF);
		final BuffInfo second = buff(SKILL_B, AbnormalType.NONE, SkillKind.BUFF);
		when(first.isInUse()).thenReturn(true);
		when(second.isInUse()).thenReturn(true);
		effects.add(first);
		effects.add(second);
		assertEquals(1, effects.getBuffCount());
		assertFalse(effects.isAffectedBySkill(SKILL_A));
		assertTrue(effects.isAffectedBySkill(SKILL_B));
	}
	
	@Test
	void danceCapKeepsConfiguredMaximum()
	{
		final EffectList effects = newEffectList(20);
		final int maxDances = Config.DANCES_MAX_AMOUNT;
		final int expectedKept = Math.max(1, maxDances);
		final int toAdd = Math.max(2, maxDances + 1);
		
		for (int i = 0; i < toAdd; i++)
		{
			final BuffInfo dance = buff(SKILL_A + i, AbnormalType.NONE, SkillKind.DANCE);
			when(dance.isInUse()).thenReturn(true);
			effects.add(dance);
		}
		
		assertEquals(expectedKept, effects.getDanceCount());
	}
	
	@Test
	void passiveCheckConditionAndSameIdReplacement()
	{
		final EffectList effects = newEffectList(20);
		final BuffInfo rejected = passive(SKILL_A, false);
		effects.add(rejected);
		assertTrue(effects.getPassives().isEmpty());
		verify(rejected, never()).initializeEffects();
		
		final BuffInfo first = passive(SKILL_A, true);
		effects.add(first);
		assertEquals(1, effects.getPassives().size());
		assertEquals(0, effects.getBuffCount());
		assertEquals(0, effects.getDanceCount());
		assertTrue(effects.getToggles().isEmpty());
		verify(first).initializeEffects();
		
		final BuffInfo second = passive(SKILL_A, true);
		effects.add(second);
		assertEquals(1, effects.getPassives().size());
		assertTrue(effects.getPassives().contains(second));
		assertFalse(effects.getPassives().contains(first));
		verify(first).setInUse(false);
		verify(first).removeStats();
		verify(second).initializeEffects();
	}
	
	private static EffectList newEffectList(int maxBuffCount)
	{
		final Creature owner = mock(Creature.class);
		final CreatureStat stat = mock(CreatureStat.class);
		when(owner.getStat()).thenReturn(stat);
		when(stat.getMaxBuffCount()).thenReturn(maxBuffCount);
		return new EffectList(owner);
	}
	
	private static BuffInfo buff(int skillId, AbnormalType abnormalType, SkillKind kind)
	{
		return buff(skillId, abnormalType, kind, mock(Creature.class));
	}
	
	private static BuffInfo buff(int skillId, AbnormalType abnormalType, SkillKind kind, Creature effected)
	{
		final Skill skill = mock(Skill.class);
		when(skill.getId()).thenReturn(skillId);
		when(skill.getAbnormalType()).thenReturn(abnormalType);
		when(skill.getAbnormalLevel()).thenReturn(1);
		when(skill.isPassive()).thenReturn(false);
		when(skill.isDebuff()).thenReturn(false);
		when(skill.isTriggeredSkill()).thenReturn(false);
		when(skill.is7Signs()).thenReturn(false);
		when(skill.isHealingPotionSkill()).thenReturn(false);
		when(skill.isManaPotionSkill()).thenReturn(false);
		when(skill.isAbnormalInstant()).thenReturn(false);
		when(skill.isDance()).thenReturn(kind == SkillKind.DANCE);
		when(skill.isToggle()).thenReturn(kind == SkillKind.TOGGLE);
		
		final BuffInfo info = mock(BuffInfo.class);
		when(info.getSkill()).thenReturn(skill);
		when(info.getEffected()).thenReturn(effected);
		when(info.getEffector()).thenReturn(mock(Creature.class));
		when(info.getEffects()).thenReturn(Collections.emptyList());
		when(info.isInUse()).thenReturn(true);
		return info;
	}
	
	private static BuffInfo stackedBuff(int skillId, AbnormalType abnormalType, int abnormalLevel)
	{
		final BuffInfo info = buff(skillId, abnormalType, SkillKind.BUFF);
		when(info.getSkill().getAbnormalLevel()).thenReturn(abnormalLevel);
		return info;
	}
	
	private static BuffInfo passive(int skillId, boolean conditionOk)
	{
		final Skill skill = mock(Skill.class);
		when(skill.getId()).thenReturn(skillId);
		when(skill.getAbnormalType()).thenReturn(AbnormalType.NONE);
		when(skill.isPassive()).thenReturn(true);
		when(skill.checkCondition(any(), any(), eq(false))).thenReturn(conditionOk);
		
		final BuffInfo info = mock(BuffInfo.class);
		when(info.getSkill()).thenReturn(skill);
		when(info.getEffected()).thenReturn(mock(Creature.class));
		when(info.getEffector()).thenReturn(mock(Creature.class));
		when(info.getEffects()).thenReturn(Collections.emptyList());
		return info;
	}
}
