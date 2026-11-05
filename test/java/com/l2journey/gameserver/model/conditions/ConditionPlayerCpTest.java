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

/**
 * CP percentage threshold for {@link ConditionPlayerCp}.
 */
class ConditionPlayerCpTest
{
	@Test
	void returnsFalseWhenEffectorNull()
	{
		final ConditionPlayerCp condition = new ConditionPlayerCp(50);
		assertFalse(condition.testImpl(null, null, null, null));
	}

	@Test
	void returnsTrueWhenCpPercentageAtOrAbove()
	{
		final Creature effector = mock(Creature.class);
		when(effector.getCurrentCp()).thenReturn(50.0);
		when(effector.getMaxCp()).thenReturn(100);

		final ConditionPlayerCp condition = new ConditionPlayerCp(50);
		assertTrue(condition.testImpl(effector, null, null, null));
	}

	@Test
	void returnsFalseWhenCpPercentageBelow()
	{
		final Creature effector = mock(Creature.class);
		when(effector.getCurrentCp()).thenReturn(49.0);
		when(effector.getMaxCp()).thenReturn(100);

		final ConditionPlayerCp condition = new ConditionPlayerCp(50);
		assertFalse(condition.testImpl(effector, null, null, null));
	}
}
