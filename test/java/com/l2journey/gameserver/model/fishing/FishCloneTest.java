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
package com.l2journey.gameserver.model.fishing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import org.junit.jupiter.api.Test;

import com.l2journey.gameserver.model.StatSet;

/**
 * Unit tests for {@link Fish#clone()}.
 */
class FishCloneTest
{
	@Test
	void cloneCopiesAllFieldsIncludingMutatedGroup()
	{
		final StatSet set = new StatSet();
		set.set("fishId", 101);
		set.set("itemId", 202);
		set.set("itemName", "Blue Fish");
		set.set("fishGroup", "swift");
		set.set("fishLevel", 3);
		set.set("fishBiteRate", 0.25);
		set.set("fishGuts", 1.5);
		set.set("fishHp", 40);
		set.set("fishMaxLength", 128);
		set.set("fishLengthRate", 0.75);
		set.set("hpRegen", 2.5);
		set.set("startCombatTime", 10);
		set.set("combatDuration", 20);
		set.set("gutsCheckTime", 30);
		set.set("gutsCheckProbability", 0.4);
		set.set("cheatingProb", 0.1);
		set.set("fishGrade", "fish_hard");
		
		final Fish original = new Fish(set);
		original.setFishGroup(-1);
		
		final Fish copy = original.clone();
		
		assertNotSame(original, copy);
		assertEquals(101, copy.getFishId());
		assertEquals(202, copy.getItemId());
		assertEquals("Blue Fish", copy.getItemName());
		assertEquals(-1, copy.getFishGroup());
		assertEquals(3, copy.getFishLevel());
		assertEquals(0.25, copy.getFishBiteRate(), 0.0);
		assertEquals(1.5, copy.getFishGuts(), 0.0);
		assertEquals(40, copy.getFishHp());
		assertEquals(128, copy.getFishMaxLength());
		assertEquals(0.75, copy.getFishLengthRate(), 0.0);
		assertEquals(2.5, copy.getHpRegen(), 0.0);
		assertEquals(10, copy.getStartCombatTime());
		assertEquals(20, copy.getCombatDuration());
		assertEquals(30, copy.getGutsCheckTime());
		assertEquals(0.4, copy.getGutsCheckProbability(), 0.0);
		assertEquals(0.1, copy.getCheatingProb(), 0.0);
		assertEquals(2, copy.getFishGrade());
		
		copy.setFishGroup(5);
		assertEquals(-1, original.getFishGroup());
		assertEquals(5, copy.getFishGroup());
	}
}
