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

import com.l2journey.gameserver.model.EffectList;
import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.skill.BuffInfo;
import com.l2journey.gameserver.model.skill.Skill;

/**
 * Characterization of {@link ConditionTargetActiveEffectId} via EffectList lookup.
 */
class ConditionTargetActiveEffectIdTest
{
	@Test
	void missingBuffReturnsFalse()
	{
		final Creature effected = mock(Creature.class);
		final EffectList effects = mock(EffectList.class);
		when(effected.getEffectList()).thenReturn(effects);
		when(effects.getBuffInfoBySkillId(100)).thenReturn(null);
		
		final ConditionTargetActiveEffectId condition = new ConditionTargetActiveEffectId(100);
		assertFalse(condition.testImpl(mock(Creature.class), effected, mock(Skill.class), null));
	}
	
	@Test
	void anyLevelMatchesWhenEffectLevelUnspecified()
	{
		final Creature effected = mock(Creature.class);
		final EffectList effects = mock(EffectList.class);
		final BuffInfo info = mock(BuffInfo.class);
		final Skill buffSkill = mock(Skill.class);
		when(effected.getEffectList()).thenReturn(effects);
		when(effects.getBuffInfoBySkillId(100)).thenReturn(info);
		when(info.getSkill()).thenReturn(buffSkill);
		when(buffSkill.getLevel()).thenReturn(1);
		
		final ConditionTargetActiveEffectId condition = new ConditionTargetActiveEffectId(100);
		assertTrue(condition.testImpl(mock(Creature.class), effected, mock(Skill.class), null));
	}
	
	@Test
	void requiredLevelAtOrBelowBuffLevelReturnsTrue()
	{
		final Creature effected = mock(Creature.class);
		final EffectList effects = mock(EffectList.class);
		final BuffInfo info = mock(BuffInfo.class);
		final Skill buffSkill = mock(Skill.class);
		when(effected.getEffectList()).thenReturn(effects);
		when(effects.getBuffInfoBySkillId(100)).thenReturn(info);
		when(info.getSkill()).thenReturn(buffSkill);
		when(buffSkill.getLevel()).thenReturn(5);
		
		final ConditionTargetActiveEffectId condition = new ConditionTargetActiveEffectId(100, 3);
		assertTrue(condition.testImpl(mock(Creature.class), effected, mock(Skill.class), null));
	}
	
	@Test
	void requiredLevelAboveBuffLevelReturnsFalse()
	{
		final Creature effected = mock(Creature.class);
		final EffectList effects = mock(EffectList.class);
		final BuffInfo info = mock(BuffInfo.class);
		final Skill buffSkill = mock(Skill.class);
		when(effected.getEffectList()).thenReturn(effects);
		when(effects.getBuffInfoBySkillId(100)).thenReturn(info);
		when(info.getSkill()).thenReturn(buffSkill);
		when(buffSkill.getLevel()).thenReturn(2);
		
		final ConditionTargetActiveEffectId condition = new ConditionTargetActiveEffectId(100, 5);
		assertFalse(condition.testImpl(mock(Creature.class), effected, mock(Skill.class), null));
	}
}
