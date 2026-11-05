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

import java.util.List;

import org.junit.jupiter.api.Test;

import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.skill.Skill;

/**
 * Characterization of {@link ConditionTargetActiveSkillId} via getAllSkills.
 */
class ConditionTargetActiveSkillIdTest
{
	@Test
	void noSkillsReturnsFalse()
	{
		final Creature effected = mock(Creature.class);
		when(effected.getAllSkills()).thenReturn(List.of());
		
		final ConditionTargetActiveSkillId condition = new ConditionTargetActiveSkillId(100);
		assertFalse(condition.testImpl(mock(Creature.class), effected, mock(Skill.class), null));
	}
	
	@Test
	void matchingSkillIdAnyLevelReturnsTrue()
	{
		final Creature effected = mock(Creature.class);
		final Skill known = mock(Skill.class);
		when(known.getId()).thenReturn(100);
		when(known.getLevel()).thenReturn(1);
		when(effected.getAllSkills()).thenReturn(List.of(known));
		
		final ConditionTargetActiveSkillId condition = new ConditionTargetActiveSkillId(100);
		assertTrue(condition.testImpl(mock(Creature.class), effected, mock(Skill.class), null));
	}
	
	@Test
	void matchingSkillIdAtRequiredLevelReturnsTrue()
	{
		final Creature effected = mock(Creature.class);
		final Skill known = mock(Skill.class);
		when(known.getId()).thenReturn(100);
		when(known.getLevel()).thenReturn(5);
		when(effected.getAllSkills()).thenReturn(List.of(known));
		
		final ConditionTargetActiveSkillId condition = new ConditionTargetActiveSkillId(100, 3);
		assertTrue(condition.testImpl(mock(Creature.class), effected, mock(Skill.class), null));
	}
	
	@Test
	void matchingSkillIdBelowRequiredLevelReturnsFalse()
	{
		final Creature effected = mock(Creature.class);
		final Skill known = mock(Skill.class);
		when(known.getId()).thenReturn(100);
		when(known.getLevel()).thenReturn(1);
		when(effected.getAllSkills()).thenReturn(List.of(known));
		
		final ConditionTargetActiveSkillId condition = new ConditionTargetActiveSkillId(100, 5);
		assertFalse(condition.testImpl(mock(Creature.class), effected, mock(Skill.class), null));
	}
	
	@Test
	void differentSkillIdReturnsFalse()
	{
		final Creature effected = mock(Creature.class);
		final Skill known = mock(Skill.class);
		when(known.getId()).thenReturn(200);
		when(known.getLevel()).thenReturn(10);
		when(effected.getAllSkills()).thenReturn(List.of(known));
		
		final ConditionTargetActiveSkillId condition = new ConditionTargetActiveSkillId(100);
		assertFalse(condition.testImpl(mock(Creature.class), effected, mock(Skill.class), null));
	}
	
	@Test
	void nullSkillInCollectionIsSkipped()
	{
		final Creature effected = mock(Creature.class);
		when(effected.getAllSkills()).thenReturn(java.util.Arrays.asList(null, null));
		
		final ConditionTargetActiveSkillId condition = new ConditionTargetActiveSkillId(100);
		assertFalse(condition.testImpl(mock(Creature.class), effected, mock(Skill.class), null));
	}
}
