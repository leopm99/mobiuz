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
package com.l2journey.gameserver.taskmanagers;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.instance.Cubic;

/**
 * Null-safe player cubic access in {@link AttackStanceTaskManager#addAttackStanceTask}.
 */
class AttackStanceTaskManagerNullGuardTest
{
	@Test
	void addAttackStanceTaskTracksPlayableWithoutPlayer()
	{
		final AttackStanceTaskManager manager = mock(AttackStanceTaskManager.class, CALLS_REAL_METHODS);
		final Creature creature = mock(Creature.class);
		doReturn(true).when(creature).isPlayable();
		doReturn(null).when(creature).asPlayer();
		
		try
		{
			manager.addAttackStanceTask(creature);
			assertTrue(manager.hasAttackStanceTask(creature));
		}
		finally
		{
			manager.removeAttackStanceTask(creature);
		}
	}
	
	@Test
	void addAttackStanceTaskTracksPlayerWithEmptyCubics()
	{
		final AttackStanceTaskManager manager = mock(AttackStanceTaskManager.class, CALLS_REAL_METHODS);
		final Player player = mock(Player.class);
		doReturn(true).when(player).isPlayable();
		doReturn(player).when(player).asPlayer();
		doReturn(Collections.<Integer, Cubic> emptyMap()).when(player).getCubics();
		
		try
		{
			manager.addAttackStanceTask(player);
			assertTrue(manager.hasAttackStanceTask(player));
			verify(player).getCubics();
		}
		finally
		{
			manager.removeAttackStanceTask(player);
		}
	}
}
