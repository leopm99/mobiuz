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

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import java.lang.reflect.Field;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.MockedStatic;

import com.l2journey.gameserver.taskmanagers.CreatureSeeTaskManager;

/**
 * Lazy seen-creatures set initialization must reuse the same instance.
 */
class CreatureSeenCreaturesLazyInitTest
{
	@Test
	void initSeenCreaturesReusesSet() throws Exception
	{
		try (MockedStatic<CreatureSeeTaskManager> seeManager = mockStatic(CreatureSeeTaskManager.class))
		{
			CreatureSeeTaskManager instance = mock(CreatureSeeTaskManager.class);
			seeManager.when(CreatureSeeTaskManager::getInstance).thenReturn(instance);
			
			Creature creature = mock(Creature.class, Answers.CALLS_REAL_METHODS);
			creature.initSeenCreatures();
			
			Field seenField = Creature.class.getDeclaredField("_seenCreatures");
			seenField.setAccessible(true);
			@SuppressWarnings("unchecked")
			Set<Creature> first = (Set<Creature>) seenField.get(creature);
			
			creature.initSeenCreatures();
			@SuppressWarnings("unchecked")
			Set<Creature> second = (Set<Creature>) seenField.get(creature);
			
			assertSame(first, second);
		}
	}
}
