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
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.skill.BuffInfo;
import com.l2journey.gameserver.model.skill.Skill;

/**
 * Characterization of {@link ConditionPlayerActiveEffectId}.
 */
class ConditionPlayerActiveEffectIdTest
{
	private static final int EFFECT_ID = 1204;
	
	@Test
	void returnsFalseWhenBuffInfoMissing()
	{
		final Player player = mock(Player.class);
		final EffectList effects = mock(EffectList.class);
		when(player.getEffectList()).thenReturn(effects);
		when(effects.getBuffInfoBySkillId(EFFECT_ID)).thenReturn(null);
		
		assertFalse(new ConditionPlayerActiveEffectId(EFFECT_ID).testImpl(player, null, null, null));
	}
	
	@Test
	void levelMinusOneAcceptsAnyPresentBuff()
	{
		final Player player = mock(Player.class);
		final EffectList effects = mock(EffectList.class);
		final BuffInfo info = mock(BuffInfo.class);
		when(player.getEffectList()).thenReturn(effects);
		when(effects.getBuffInfoBySkillId(EFFECT_ID)).thenReturn(info);
		
		assertTrue(new ConditionPlayerActiveEffectId(EFFECT_ID).testImpl(player, null, null, null));
	}
	
	@Test
	void requiredLevelMustBeAtMostSkillLevel()
	{
		final Player player = mock(Player.class);
		final EffectList effects = mock(EffectList.class);
		final BuffInfo info = mock(BuffInfo.class);
		final Skill skill = mock(Skill.class);
		when(player.getEffectList()).thenReturn(effects);
		when(effects.getBuffInfoBySkillId(EFFECT_ID)).thenReturn(info);
		when(info.getSkill()).thenReturn(skill);
		when(skill.getLevel()).thenReturn(3);
		
		assertTrue(new ConditionPlayerActiveEffectId(EFFECT_ID, 3).testImpl(player, null, null, null));
		assertTrue(new ConditionPlayerActiveEffectId(EFFECT_ID, 2).testImpl(player, null, null, null));
		assertFalse(new ConditionPlayerActiveEffectId(EFFECT_ID, 4).testImpl(player, null, null, null));
	}
}
