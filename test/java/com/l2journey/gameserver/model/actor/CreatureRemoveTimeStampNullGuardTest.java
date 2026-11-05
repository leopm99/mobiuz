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
package com.l2journey.gameserver.model.actor;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.Test;

import com.l2journey.gameserver.model.TimeStamp;
import com.l2journey.gameserver.model.skill.Skill;

/**
 * Null-safe skill reuse removal in {@link Creature#removeTimeStamp(Skill)}.
 */
class CreatureRemoveTimeStampNullGuardTest
{
	@Test
	void removeTimeStampReturnsWhenSkillMissing() throws Exception
	{
		final Creature creature = creatureWithReuseMap();
		assertDoesNotThrow(() -> creature.removeTimeStamp(null));
	}
	
	@Test
	void removeTimeStampRemovesExistingSkill() throws Exception
	{
		final Creature creature = creatureWithReuseMap();
		final Skill skill = mock(Skill.class);
		when(skill.getReuseHashCode()).thenReturn(42);
		
		@SuppressWarnings("unchecked")
		final Map<Integer, TimeStamp> reuse = (Map<Integer, TimeStamp>) getReuseMap(creature);
		reuse.put(42, mock(TimeStamp.class));
		
		creature.removeTimeStamp(skill);
		assertNull(reuse.get(42));
	}
	
	private static Creature creatureWithReuseMap() throws Exception
	{
		final Creature creature = mock(Creature.class, CALLS_REAL_METHODS);
		final Field field = Creature.class.getDeclaredField("_reuseTimeStampsSkills");
		field.setAccessible(true);
		field.set(creature, new ConcurrentHashMap<>());
		return creature;
	}
	
	private static Object getReuseMap(Creature creature) throws Exception
	{
		final Field field = Creature.class.getDeclaredField("_reuseTimeStampsSkills");
		field.setAccessible(true);
		return field.get(creature);
	}
}
