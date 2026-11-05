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

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.skill.Skill;

/**
 * Characterization of {@link ConditionPlayerActiveSkillId}.
 */
class ConditionPlayerActiveSkillIdTest
{
	private static final int SKILL_ID = 1001;
	
	@Test
	void returnsFalseWhenNoSkills()
	{
		final Player player = mock(Player.class);
		when(player.getAllSkills()).thenReturn(Collections.emptyList());
		
		assertFalse(new ConditionPlayerActiveSkillId(SKILL_ID).testImpl(player, null, null, null));
	}
	
	@Test
	void skipsNullEntriesAndMatchesIdWithAnyLevel()
	{
		final Player player = mock(Player.class);
		final Skill skill = mock(Skill.class);
		when(skill.getId()).thenReturn(SKILL_ID);
		when(skill.getLevel()).thenReturn(1);
		when(player.getAllSkills()).thenReturn(Arrays.asList(null, skill));
		
		assertTrue(new ConditionPlayerActiveSkillId(SKILL_ID).testImpl(player, null, null, null));
	}
	
	@Test
	void requiredLevelMustBeAtMostSkillLevel()
	{
		final Player player = mock(Player.class);
		final Skill skill = mock(Skill.class);
		when(skill.getId()).thenReturn(SKILL_ID);
		when(skill.getLevel()).thenReturn(5);
		when(player.getAllSkills()).thenReturn(Collections.singletonList(skill));
		
		assertTrue(new ConditionPlayerActiveSkillId(SKILL_ID, 5).testImpl(player, null, null, null));
		assertFalse(new ConditionPlayerActiveSkillId(SKILL_ID, 6).testImpl(player, null, null, null));
	}
	
	@Test
	void returnsFalseWhenSkillIdDiffers()
	{
		final Player player = mock(Player.class);
		final Skill skill = mock(Skill.class);
		when(skill.getId()).thenReturn(999);
		when(skill.getLevel()).thenReturn(10);
		when(player.getAllSkills()).thenReturn(Collections.singletonList(skill));
		
		assertFalse(new ConditionPlayerActiveSkillId(SKILL_ID).testImpl(player, null, null, null));
	}
}
