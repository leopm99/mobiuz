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
package handlers.bypasshandlers;

import java.util.logging.Level;

import com.l2journey.gameserver.handler.IBypassHandler;
import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.instance.OlympiadManager;
import com.l2journey.gameserver.model.olympiad.Olympiad;
import com.l2journey.gameserver.model.olympiad.OlympiadGameManager;
import com.l2journey.gameserver.model.olympiad.OlympiadGameTask;
import com.l2journey.gameserver.network.SystemMessageId;
import com.l2journey.gameserver.network.serverpackets.ExOlympiadMatchList;

/**
 * @author DS
 */
public class OlympiadObservation implements IBypassHandler
{
	private static final String[] COMMANDS =
	{
		"watchmatch",
		"arenachange"
	};
	
	@Override
	public boolean useBypass(String command, Player player, Creature target)
	{
		try
		{
			final Npc olymanager = player.getLastFolkNPC();
			if (command.startsWith(COMMANDS[0])) // list
			{
				if (!Olympiad.getInstance().inCompPeriod())
				{
					player.sendPacket(SystemMessageId.THE_GRAND_OLYMPIAD_GAMES_ARE_NOT_CURRENTLY_IN_PROGRESS);
					return false;
				}
				
				player.sendPacket(new ExOlympiadMatchList());
			}
			else
			{
				if (!(olymanager instanceof OlympiadManager) || (!player.inObserverMode() && !player.isInsideRadius2D(olymanager, 300)))
				{
					return false;
				}
				
				if (com.l2journey.gameserver.model.olympiad.OlympiadManager.getInstance().isRegisteredInComp(player))
				{
					player.sendPacket(SystemMessageId.YOU_MAY_NOT_OBSERVE_A_GRAND_OLYMPIAD_GAMES_MATCH_WHILE_YOU_ARE_ON_THE_WAITING_LIST);
					return false;
				}
				
				if (!Olympiad.getInstance().inCompPeriod())
				{
					player.sendPacket(SystemMessageId.THE_GRAND_OLYMPIAD_GAMES_ARE_NOT_CURRENTLY_IN_PROGRESS);
					return false;
				}
				
				if (player.isRegisteredOnEvent())
				{
					player.sendMessage("You can not observe games while registered on an event.");
					return false;
				}
				
				final int arenaId = Integer.parseInt(command.substring(12).trim());
				final OlympiadGameTask nextArena = OlympiadGameManager.getInstance().getOlympiadTask(arenaId);
				if (nextArena != null)
				{
					final int instanceId = OlympiadGameManager.getInstance().getOlympiadTask(arenaId).getZone().getInstanceId();
					player.enterOlympiadObserverMode(nextArena.getZone().getSpectatorSpawns().get(0), arenaId, instanceId);
				}
			}
			
			return true;
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Exception in " + getClass().getSimpleName(), e);
		}
		
		return false;
	}
	
	@Override
	public String[] getBypassList()
	{
		return COMMANDS;
	}
}
