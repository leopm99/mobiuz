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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.actor.templates.CreatureTemplate;
import com.l2journey.gameserver.model.item.Weapon;
import com.l2journey.gameserver.model.stats.Calculator;
import com.l2journey.gameserver.model.stats.Stat;

/**
 * NPC physical attack range uses the template even when a weapon is equipped.
 */
class CreatureStatPhysicalAttackRangeTest
{
	private static final int TEMPLATE_RANGE = 1100;
	private static final int WEAPON_RANGE = 500;
	
	@Test
	void npcWithWeaponUsesTemplateRange()
	{
		final Creature creature = mockCreature(true, true);
		assertEquals(TEMPLATE_RANGE, new CreatureStat(creature).getPhysicalAttackRange());
	}
	
	@Test
	void nonNpcWithWeaponUsesWeaponRange()
	{
		final Creature creature = mockCreature(false, true);
		assertEquals(WEAPON_RANGE, new CreatureStat(creature).getPhysicalAttackRange());
	}
	
	@Test
	void creatureWithoutWeaponUsesTemplateRange()
	{
		final Creature creature = mockCreature(false, false);
		assertEquals(TEMPLATE_RANGE, new CreatureStat(creature).getPhysicalAttackRange());
	}
	
	private static Creature mockCreature(boolean npc, boolean withWeapon)
	{
		final Creature creature = mock(Creature.class);
		final CreatureTemplate template = mock(CreatureTemplate.class);
		when(template.getBaseAttackRange()).thenReturn(TEMPLATE_RANGE);
		when(creature.getTemplate()).thenReturn(template);
		when(creature.getTransformation()).thenReturn(null);
		when(creature.isNpc()).thenReturn(npc);
		when(creature.getCalculators()).thenReturn(new Calculator[Stat.values().length]);
		if (withWeapon)
		{
			final Weapon weapon = mock(Weapon.class);
			when(weapon.getBaseAttackRange()).thenReturn(WEAPON_RANGE);
			when(creature.getActiveWeaponItem()).thenReturn(weapon);
		}
		else
		{
			when(creature.getActiveWeaponItem()).thenReturn(null);
		}
		return creature;
	}
}
