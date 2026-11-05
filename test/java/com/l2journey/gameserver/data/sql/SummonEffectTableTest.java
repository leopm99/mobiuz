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
package com.l2journey.gameserver.data.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.skill.Skill;

/**
 * Unit tests for null-safe servitor effect access in {@link SummonEffectTable}.
 */
class SummonEffectTableTest
{
	private static final int OWNER_ID = 1001;
	private static final int OTHER_OWNER_ID = 1002;
	private static final int CLASS_INDEX = 0;
	private static final int REFERENCE_SKILL = 1200;
	private static final int OTHER_REFERENCE_SKILL = 1201;
	private static final int EFFECT_SKILL_ID = 42;
	
	@Test
	void addClearAndRemoveServitorEffectsAreNullSafe() throws Exception
	{
		final SummonEffectTable table = new SummonEffectTable();
		final Player owner = mockPlayer(OWNER_ID);
		final Player otherOwner = mockPlayer(OTHER_OWNER_ID);
		final Skill skill = mock(Skill.class);
		when(skill.getId()).thenReturn(EFFECT_SKILL_ID);
		
		table.clearServitorEffects(owner, REFERENCE_SKILL);
		table.removeServitorEffects(owner, REFERENCE_SKILL, EFFECT_SKILL_ID);
		
		table.addServitorEffect(otherOwner, REFERENCE_SKILL, skill, 5);
		assertTrue(table.containsSkill(otherOwner, REFERENCE_SKILL));
		assertEquals(1, skillEffectCount(table, OTHER_OWNER_ID, REFERENCE_SKILL));
		
		table.clearServitorEffects(owner, REFERENCE_SKILL);
		assertEquals(1, skillEffectCount(table, OTHER_OWNER_ID, REFERENCE_SKILL));
		
		table.addServitorEffect(owner, REFERENCE_SKILL, skill, 5);
		table.addServitorEffect(owner, REFERENCE_SKILL, skill, 7);
		assertTrue(table.containsSkill(owner, REFERENCE_SKILL));
		assertEquals(2, skillEffectCount(table, OWNER_ID, REFERENCE_SKILL));
		
		table.clearServitorEffects(owner, OTHER_REFERENCE_SKILL);
		assertEquals(2, skillEffectCount(table, OWNER_ID, REFERENCE_SKILL));
		
		table.clearServitorEffects(owner, REFERENCE_SKILL);
		assertEquals(0, skillEffectCount(table, OWNER_ID, REFERENCE_SKILL));
		assertTrue(table.containsSkill(owner, REFERENCE_SKILL));
		
		table.addServitorEffect(owner, REFERENCE_SKILL, skill, 3);
		assertEquals(1, skillEffectCount(table, OWNER_ID, REFERENCE_SKILL));
		table.removeServitorEffects(owner, REFERENCE_SKILL, EFFECT_SKILL_ID);
		assertEquals(0, skillEffectCount(table, OWNER_ID, REFERENCE_SKILL));
		
		assertFalse(table.containsSkill(mockPlayer(9999), REFERENCE_SKILL));
	}
	
	private static Player mockPlayer(int objectId)
	{
		final Player player = mock(Player.class);
		when(player.getObjectId()).thenReturn(objectId);
		when(player.getClassIndex()).thenReturn(CLASS_INDEX);
		return player;
	}
	
	@SuppressWarnings("unchecked")
	private static int skillEffectCount(SummonEffectTable table, int objectId, int referenceSkill) throws Exception
	{
		final Field field = SummonEffectTable.class.getDeclaredField("_servitorEffects");
		field.setAccessible(true);
		final Map<Integer, Map<Integer, Map<Integer, Collection<?>>>> effects = (Map<Integer, Map<Integer, Map<Integer, Collection<?>>>>) field.get(table);
		final Map<Integer, Map<Integer, Collection<?>>> byOwner = effects.get(objectId);
		if (byOwner == null)
		{
			return 0;
		}
		final Map<Integer, Collection<?>> byClass = byOwner.get(CLASS_INDEX);
		if (byClass == null)
		{
			return 0;
		}
		final Collection<?> skillEffects = byClass.get(referenceSkill);
		return skillEffects == null ? 0 : skillEffects.size();
	}
}
