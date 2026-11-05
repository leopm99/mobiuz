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
package com.l2journey.gameserver.managers.games;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.l2journey.gameserver.model.ArenaParticipantsHolder;
import com.l2journey.gameserver.network.serverpackets.ServerPacket;

/**
 * {@link BlockCheckerManager.EndEvent} reward branch selection after a non-tie.
 */
class BlockCheckerManagerRewardTest
{
	@Test
	void rewardPlayersDoesNothingOnTie() throws Exception
	{
		final ArenaParticipantsHolder holder = emptyHolder();
		final BlockCheckerManager manager = new BlockCheckerManager(holder, 0);
		manager._redPoints = 15;
		manager._bluePoints = 15;
		manager._isRedWinner = false;
		
		invokeRewardPlayers(manager);
		
		assertFalse(manager._isRedWinner);
		verify(holder, never()).broadCastPacketToTeam(any(ServerPacket.class));
	}
	
	@Test
	void rewardPlayersMarksRedWinnerWhenRedAhead() throws Exception
	{
		final ArenaParticipantsHolder holder = emptyHolder();
		final BlockCheckerManager manager = new BlockCheckerManager(holder, 0);
		manager._redPoints = 20;
		manager._bluePoints = 10;
		manager._isRedWinner = false;
		
		invokeRewardPlayers(manager);
		
		assertTrue(manager._isRedWinner);
		verify(holder, times(1)).broadCastPacketToTeam(any(ServerPacket.class));
	}
	
	@Test
	void rewardPlayersMarksBlueWinnerWhenBlueAhead() throws Exception
	{
		final ArenaParticipantsHolder holder = emptyHolder();
		final BlockCheckerManager manager = new BlockCheckerManager(holder, 0);
		manager._redPoints = 10;
		manager._bluePoints = 20;
		manager._isRedWinner = true;
		
		invokeRewardPlayers(manager);
		
		assertFalse(manager._isRedWinner);
		verify(holder, times(1)).broadCastPacketToTeam(any(ServerPacket.class));
	}
	
	private static ArenaParticipantsHolder emptyHolder()
	{
		final ArenaParticipantsHolder holder = mock(ArenaParticipantsHolder.class);
		when(holder.getRedPlayers()).thenReturn(Collections.emptyList());
		when(holder.getBluePlayers()).thenReturn(Collections.emptyList());
		return holder;
	}
	
	private static void invokeRewardPlayers(BlockCheckerManager manager) throws Exception
	{
		final BlockCheckerManager.EndEvent endEvent = manager.new EndEvent();
		final Method method = BlockCheckerManager.EndEvent.class.getDeclaredMethod("rewardPlayers");
		method.setAccessible(true);
		method.invoke(endEvent);
	}
}
