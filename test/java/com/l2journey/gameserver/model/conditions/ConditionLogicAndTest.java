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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.l2journey.gameserver.model.actor.Player;

/**
 * Characterization of {@link ConditionLogicAnd}.
 */
class ConditionLogicAndTest
{
	@Test
	void emptyConditionsReturnTrue()
	{
		final ConditionLogicAnd condition = new ConditionLogicAnd();
		assertTrue(condition.testImpl(mock(Player.class), null, null, null));
	}
	
	@Test
	void childFalseMakesAndFalse()
	{
		final Condition childTrue = mock(Condition.class);
		final Condition childFalse = mock(Condition.class);
		when(childTrue.test(any(), any(), any(), any())).thenReturn(true);
		when(childFalse.test(any(), any(), any(), any())).thenReturn(false);
		
		final ConditionLogicAnd condition = new ConditionLogicAnd();
		condition.add(childTrue);
		condition.add(childFalse);
		
		assertFalse(condition.testImpl(mock(Player.class), null, null, null));
	}
	
	@Test
	void allChildrenTrueMakesAndTrue()
	{
		final Condition childA = mock(Condition.class);
		final Condition childB = mock(Condition.class);
		when(childA.test(any(), any(), any(), any())).thenReturn(true);
		when(childB.test(any(), any(), any(), any())).thenReturn(true);
		
		final ConditionLogicAnd condition = new ConditionLogicAnd();
		condition.add(childA);
		condition.add(childB);
		
		assertTrue(condition.testImpl(mock(Player.class), null, null, null));
	}
}
