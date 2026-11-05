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

import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.item.Weapon;
import com.l2journey.gameserver.model.item.type.WeaponType;

/**
 * Characterization of {@link ConditionTargetUsesWeaponKind}.
 */
class ConditionTargetUsesWeaponKindTest
{
	@Test
	void returnsFalseWhenEffectedNull()
	{
		assertFalse(new ConditionTargetUsesWeaponKind(WeaponType.SWORD.mask()).testImpl(null, null, null, null));
	}
	
	@Test
	void returnsFalseWhenActiveWeaponMissing()
	{
		final Creature effected = mock(Creature.class);
		when(effected.getActiveWeaponItem()).thenReturn(null);
		
		assertFalse(new ConditionTargetUsesWeaponKind(WeaponType.SWORD.mask()).testImpl(null, effected, null, null));
	}
	
	@Test
	void returnsTrueWhenWeaponKindMatches()
	{
		final Creature effected = mock(Creature.class);
		final Weapon weapon = mock(Weapon.class);
		when(effected.getActiveWeaponItem()).thenReturn(weapon);
		when(weapon.getItemType()).thenReturn(WeaponType.SWORD);
		
		assertTrue(new ConditionTargetUsesWeaponKind(WeaponType.SWORD.mask()).testImpl(null, effected, null, null));
	}
	
	@Test
	void returnsFalseWhenWeaponKindMisses()
	{
		final Creature effected = mock(Creature.class);
		final Weapon weapon = mock(Weapon.class);
		when(effected.getActiveWeaponItem()).thenReturn(weapon);
		when(weapon.getItemType()).thenReturn(WeaponType.BOW);
		
		assertFalse(new ConditionTargetUsesWeaponKind(WeaponType.SWORD.mask()).testImpl(null, effected, null, null));
	}
}
