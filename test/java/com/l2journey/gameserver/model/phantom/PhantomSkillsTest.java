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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.skill.Skill;
import com.l2journey.gameserver.model.skill.targets.TargetType;

/**
 * Regression tests for phantom mage offensive skill fallback selection.
 */
class PhantomSkillsTest
{
	private static final int VOLCANO_ID = 1419;
	private static final int PROMINENCE_ID = 1230;
	
	@Test
	void getBestOffensiveSkillSkipsGroundChannelSkills()
	{
		Skill volcano = mockOffensiveSkill(VOLCANO_ID, 500, 900, TargetType.GROUND, 36);
		Skill prominence = mockOffensiveSkill(PROMINENCE_ID, 102, 900, TargetType.ONE, 80);
		
		Player bot = mock(Player.class);
		when(bot.getAllSkills()).thenReturn(List.of(volcano, prominence));
		when(bot.getCurrentMp()).thenReturn(1000.0);
		when(bot.isSkillDisabled(volcano)).thenReturn(false);
		when(bot.isSkillDisabled(prominence)).thenReturn(true);
		
		assertNull(PhantomSkills.getBestOffensiveSkill(bot));
	}
	
	@Test
	void getBestOffensiveSkillPrefersReadyTargetedNukeOverGround()
	{
		Skill volcano = mockOffensiveSkill(VOLCANO_ID, 500, 900, TargetType.GROUND, 36);
		Skill prominence = mockOffensiveSkill(PROMINENCE_ID, 102, 900, TargetType.ONE, 80);
		
		Player bot = mock(Player.class);
		when(bot.getAllSkills()).thenReturn(List.of(volcano, prominence));
		when(bot.getCurrentMp()).thenReturn(1000.0);
		when(bot.isSkillDisabled(volcano)).thenReturn(false);
		when(bot.isSkillDisabled(prominence)).thenReturn(false);
		
		Skill best = PhantomSkills.getBestOffensiveSkill(bot);
		assertEquals(PROMINENCE_ID, best.getId());
	}
	
	private static Skill mockOffensiveSkill(int id, double power, int castRange, TargetType targetType, int mpConsume)
	{
		Skill skill = mock(Skill.class);
		when(skill.getId()).thenReturn(id);
		when(skill.isPassive()).thenReturn(false);
		when(skill.isMagic()).thenReturn(true);
		when(skill.getPower()).thenReturn(power);
		when(skill.getCastRange()).thenReturn(castRange);
		when(skill.getTargetType()).thenReturn(targetType);
		when(skill.getMpConsume()).thenReturn(mpConsume);
		return skill;
	}
}
