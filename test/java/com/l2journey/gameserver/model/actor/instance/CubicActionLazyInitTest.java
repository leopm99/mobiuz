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
package com.l2journey.gameserver.model.actor.instance;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.util.concurrent.ScheduledFuture;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.l2journey.commons.threads.ThreadPool;
import com.l2journey.gameserver.data.xml.SkillData;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.skill.Skill;

/**
 * Lazy cubic action-task initialization must schedule only once.
 */
class CubicActionLazyInitTest
{
	@Test
	void doActionSchedulesActionTaskOnce()
	{
		Player owner = mock(Player.class);
		ScheduledFuture<?> disappearFuture = mock(ScheduledFuture.class);
		ScheduledFuture<?> actionFuture = mock(ScheduledFuture.class);
		
		try (MockedStatic<SkillData> skillData = mockStatic(SkillData.class);
			MockedStatic<ThreadPool> threadPool = mockStatic(ThreadPool.class))
		{
			SkillData skillDataInstance = mock(SkillData.class);
			when(skillDataInstance.getSkill(anyInt(), anyInt())).thenReturn(mock(Skill.class));
			skillData.when(SkillData::getInstance).thenReturn(skillDataInstance);
			
			threadPool.when(() -> ThreadPool.schedule(any(Runnable.class), anyLong())).thenReturn(disappearFuture);
			threadPool.when(() -> ThreadPool.scheduleAtFixedRate(any(Runnable.class), anyLong(), anyLong())).thenReturn(actionFuture);
			
			Cubic cubic = new Cubic(owner, Cubic.AQUA_CUBIC, 1, 1, 1, 1, 1, 1, false);
			cubic.doAction();
			cubic.doAction();
			
			threadPool.verify(() -> ThreadPool.scheduleAtFixedRate(any(Runnable.class), anyLong(), anyLong()), times(1));
		}
	}
}
