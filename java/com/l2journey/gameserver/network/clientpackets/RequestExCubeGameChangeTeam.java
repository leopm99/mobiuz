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
package com.l2journey.gameserver.network.clientpackets;

import com.l2journey.gameserver.managers.HandysBlockCheckerManager;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.network.PacketLogger;

/**
 * Format: chdd d: Arena d: Team
 * @author mrTJO
 */
public class RequestExCubeGameChangeTeam extends ClientPacket
{
	private int _arena;
	private int _team;
	
	@Override
	protected void readImpl()
	{
		// client sends -1,0,1,2 for arena parameter
		_arena = readInt() + 1;
		_team = readInt();
	}
	
	@Override
	protected void runImpl()
	{
		// do not remove players after start
		if (HandysBlockCheckerManager.getInstance().arenaIsBeingUsed(_arena))
		{
			return;
		}
		final Player player = getPlayer();
		
		switch (_team)
		{
			case 0, 1:
			{
				// Change Player Team
				HandysBlockCheckerManager.getInstance().changePlayerToTeam(player, _arena);
				break;
			}
			case -1:
			{
				// Remove Player (me)
			}
			{
				final int team = HandysBlockCheckerManager.getInstance().getHolder(_arena).getPlayerTeam(player);
				// client sends two times this packet if click on exit
				// client did not send this packet on restart
				if (team > -1)
				{
					HandysBlockCheckerManager.getInstance().removePlayer(player, _arena, team);
				}
				break;
			}
			default:
			{
				PacketLogger.warning("Wrong Cube Game Team ID: " + _team);
				break;
			}
		}
	}
}
