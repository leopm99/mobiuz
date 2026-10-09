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
 * 
 * ---
 * 
 * Portions of this software are derived from the L2JMobius Project, 
 * shared under the MIT License. The original license terms are preserved where 
 * applicable..
 * 
 */
package ai.areas.FantasyIsle;

import com.l2journey.Config;
import com.l2journey.gameserver.managers.HandysBlockCheckerManager;
import com.l2journey.gameserver.model.ArenaParticipantsHolder;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.network.SystemMessageId;
import com.l2journey.gameserver.network.serverpackets.ExCubeGameChangeTimeToStart;
import com.l2journey.gameserver.network.serverpackets.ExCubeGameRequestReady;
import com.l2journey.gameserver.network.serverpackets.ExCubeGameTeamList;

import ai.AbstractNpcAI;

/**
 * Handys Block Checker Event AI.
 * @authors BiggBoss, Gigiikun
 */
public class HandysBlockCheckerEvent extends AbstractNpcAI
{
	// Arena Managers
	private static final int A_MANAGER_1 = 32521;
	private static final int A_MANAGER_2 = 32522;
	private static final int A_MANAGER_3 = 32523;
	private static final int A_MANAGER_4 = 32524;
	
	public HandysBlockCheckerEvent()
	{
		addFirstTalkId(A_MANAGER_1, A_MANAGER_2, A_MANAGER_3, A_MANAGER_4);
		HandysBlockCheckerManager.getInstance().startUpParticipantsQueue();
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		if ((npc != null) && (player != null))
		{
			final int arena = npc.getId() - A_MANAGER_1;
			if (eventIsFull(arena))
			{
				player.sendPacket(SystemMessageId.YOU_CANNOT_REGISTER_BECAUSE_CAPACITY_HAS_BEEN_EXCEEDED);
			}
			else if (HandysBlockCheckerManager.getInstance().arenaIsBeingUsed(arena))
			{
				player.sendPacket(SystemMessageId.THE_MATCH_IS_BEING_PREPARED_PLEASE_TRY_AGAIN_LATER);
			}
			else if (HandysBlockCheckerManager.getInstance().addPlayerToArena(player, arena))
			{
				final ArenaParticipantsHolder holder = HandysBlockCheckerManager.getInstance().getHolder(arena);
				player.sendPacket(new ExCubeGameTeamList(holder.getRedPlayers(), holder.getBluePlayers(), arena));
				if ((holder.getBlueTeamSize() >= Config.MIN_BLOCK_CHECKER_TEAM_MEMBERS) && (holder.getRedTeamSize() >= Config.MIN_BLOCK_CHECKER_TEAM_MEMBERS))
				{
					holder.updateEvent();
					holder.broadCastPacketToTeam(new ExCubeGameRequestReady());
					holder.broadCastPacketToTeam(new ExCubeGameChangeTimeToStart(10));
				}
			}
		}
		return null;
	}
	
	private boolean eventIsFull(int arena)
	{
		return HandysBlockCheckerManager.getInstance().getHolder(arena).getAllPlayers().size() == 12;
	}
	
	public static void main(String[] args)
	{
		if (Config.ENABLE_BLOCK_CHECKER_EVENT)
		{
			new HandysBlockCheckerEvent();
			LOGGER.info("Handy's Block Checker Event is enabled");
		}
		else
		{
			LOGGER.info("Handy's Block Checker Event is disabled");
		}
	}
}