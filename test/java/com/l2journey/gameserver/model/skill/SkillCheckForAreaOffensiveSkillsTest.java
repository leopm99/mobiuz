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
package com.l2journey.gameserver.model.skill;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.l2journey.gameserver.GeoData;
import com.l2journey.gameserver.model.actor.Creature;

/**
 * Line-of-sight return for {@link Skill#checkForAreaOffensiveSkills} (java:S2761).
 */
class SkillCheckForAreaOffensiveSkillsTest
{
	@Test
	void returnsGeodataLineOfSightOnNonPlayerPath()
	{
		final Creature caster = mock(Creature.class);
		final Creature target = mock(Creature.class);
		when(target.isDead()).thenReturn(false);
		when(caster.asPlayer()).thenReturn(null);
		when(target.asPlayer()).thenReturn(null);
		when(caster.isAttackable()).thenReturn(false);
		when(target.isAttackable()).thenReturn(false);
		
		final GeoData geoData = mock(GeoData.class);
		when(geoData.canSeeTarget(caster, target)).thenReturn(true, false);
		
		try (MockedStatic<GeoData> geoStatic = mockStatic(GeoData.class))
		{
			geoStatic.when(GeoData::getInstance).thenReturn(geoData);
			assertTrue(Skill.checkForAreaOffensiveSkills(caster, target, null, false));
			assertFalse(Skill.checkForAreaOffensiveSkills(caster, target, null, false));
		}
	}
}
