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
package com.l2journey.gameserver.ai;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;

import com.l2journey.gameserver.model.actor.Summon;

/**
 * Atomic follow toggle for {@link SummonAI} (java:S3078).
 */
class SummonAIToggleTest
{
	@Test
	void notifyFollowStatusChangeTogglesUnderLock() throws Exception
	{
		final Summon summon = mock(Summon.class);
		final SummonAI ai = mock(SummonAI.class, CALLS_REAL_METHODS);
		
		final Field boundSummon = SummonAI.class.getDeclaredField("_boundSummon");
		boundSummon.setAccessible(true);
		boundSummon.set(ai, summon);
		
		final Field intention = AbstractAI.class.getDeclaredField("_intention");
		intention.setAccessible(true);
		intention.set(ai, Intention.FOLLOW);
		
		final Field startFollow = SummonAI.class.getDeclaredField("_startFollow");
		startFollow.setAccessible(true);
		startFollow.setBoolean(ai, false);
		
		ai.notifyFollowStatusChange();
		assertTrue(startFollow.getBoolean(ai));
		verify(summon).setFollowStatus(true);
		
		ai.notifyFollowStatusChange();
		assertFalse(startFollow.getBoolean(ai));
		verify(summon).setFollowStatus(false);
		
		intention.set(ai, Intention.ATTACK);
		ai.notifyFollowStatusChange();
		assertTrue(startFollow.getBoolean(ai));
		verify(summon, times(1)).setFollowStatus(true);
		verify(summon, times(1)).setFollowStatus(false);
	}
}
