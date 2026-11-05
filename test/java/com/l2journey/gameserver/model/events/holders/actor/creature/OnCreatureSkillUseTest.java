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
package com.l2journey.gameserver.model.events.holders.actor.creature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.l2journey.gameserver.model.WorldObject;
import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.events.EventType;
import com.l2journey.gameserver.model.skill.Skill;

/**
 * Getter/setter synchronization for {@link OnCreatureSkillUse} (java:S2886).
 */
class OnCreatureSkillUseTest
{
	@Test
	void gettersAreSynchronized() throws Exception
	{
		assertTrue(Modifier.isSynchronized(OnCreatureSkillUse.class.getDeclaredMethod("getCaster").getModifiers()));
		assertTrue(Modifier.isSynchronized(OnCreatureSkillUse.class.getDeclaredMethod("getSkill").getModifiers()));
		assertTrue(Modifier.isSynchronized(OnCreatureSkillUse.class.getDeclaredMethod("isSimultaneously").getModifiers()));
		assertTrue(Modifier.isSynchronized(OnCreatureSkillUse.class.getDeclaredMethod("getTarget").getModifiers()));
		assertTrue(Modifier.isSynchronized(OnCreatureSkillUse.class.getDeclaredMethod("getTargets").getModifiers()));
	}
	
	@Test
	void settersAndGettersRoundTrip()
	{
		final OnCreatureSkillUse event = new OnCreatureSkillUse();
		assertEquals(EventType.ON_CREATURE_SKILL_USE, event.getType());
		
		assertNull(event.getCaster());
		assertNull(event.getSkill());
		assertFalse(event.isSimultaneously());
		assertNull(event.getTarget());
		assertNull(event.getTargets());
		
		final Creature caster = mock(Creature.class);
		final Skill skill = mock(Skill.class);
		final Creature target = mock(Creature.class);
		final Collection<WorldObject> targets = Collections.emptyList();
		
		event.setCaster(caster);
		event.setSkill(skill);
		event.setSimultaneously(true);
		event.setTarget(target);
		event.setTargets(targets);
		
		assertSame(caster, event.getCaster());
		assertSame(skill, event.getSkill());
		assertTrue(event.isSimultaneously());
		assertSame(target, event.getTarget());
		assertSame(targets, event.getTargets());
		
		event.setSimultaneously(false);
		assertFalse(event.isSimultaneously());
		
		event.setCaster(null);
		event.setSkill(null);
		event.setTarget(null);
		event.setTargets(null);
		assertNull(event.getCaster());
		assertNull(event.getSkill());
		assertNull(event.getTarget());
		assertNull(event.getTargets());
	}
}
