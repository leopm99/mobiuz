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
package com.l2journey.gameserver.model.actor.tasks.player;

import java.util.logging.Logger;

import com.l2journey.Config;
import com.l2journey.gameserver.data.xml.AdminData;
import com.l2journey.gameserver.managers.PunishmentManager;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.enums.player.IllegalActionPunishmentType;
import com.l2journey.gameserver.model.punishment.PunishmentAffect;
import com.l2journey.gameserver.model.punishment.PunishmentTask;
import com.l2journey.gameserver.model.punishment.PunishmentType;
import com.l2journey.gameserver.network.Disconnection;
import com.l2journey.gameserver.network.serverpackets.LeaveWorld;

/**
 * Task that handles illegal player actions.
 */
public class IllegalPlayerActionTask implements Runnable
{
	private static final Logger AUDIT_LOGGER = Logger.getLogger("audit");
	
	private final String _message;
	private final IllegalActionPunishmentType _punishment;
	private final Player _actor;
	
	public IllegalPlayerActionTask(Player actor, String message, IllegalActionPunishmentType punishment)
	{
		_message = message;
		_punishment = punishment;
		_actor = actor;
		
		switch (punishment)
		{
			case KICK:
			{
				_actor.sendMessage("You will be kicked for illegal action, GM informed.");
				break;
			}
			case KICKBAN:
			{
				if (!_actor.isGM())
				{
					_actor.setAccessLevel(-1);
					_actor.setAccountAccesslevel(-1);
				}
				_actor.sendMessage("You are banned for illegal action, GM informed.");
				break;
			}
			case JAIL:
			{
				_actor.sendMessage("Illegal action performed!");
				_actor.sendMessage("You will be teleported to GM Consultation Service area and jailed.");
				break;
			}
		}
	}
	
	@Override
	public void run()
	{
		final StringBuilder sb = new StringBuilder();
		sb.append("AUDIT, ");
		sb.append(_message);
		sb.append(", ");
		sb.append(_actor);
		sb.append(", ");
		sb.append(_punishment);
		AUDIT_LOGGER.info(() -> sb.toString());
		
		if (!_actor.isGM())
		{
			switch (_punishment)
			{
				case BROADCAST:
				{
					AdminData.getInstance().broadcastMessageToGMs(_message);
					return;
				}
				case KICK:
				{
					Disconnection.of(_actor).storeAndDeleteWith(LeaveWorld.STATIC_PACKET);
					break;
				}
				case KICKBAN:
				{
					PunishmentManager.getInstance().startPunishment(new PunishmentTask(_actor.getObjectId(), PunishmentAffect.CHARACTER, PunishmentType.BAN, System.currentTimeMillis() + (Config.DEFAULT_PUNISH_PARAM * 1000), _message, getClass().getSimpleName()));
					break;
				}
				case JAIL:
				{
					PunishmentManager.getInstance().startPunishment(new PunishmentTask(_actor.getObjectId(), PunishmentAffect.CHARACTER, PunishmentType.JAIL, System.currentTimeMillis() + (Config.DEFAULT_PUNISH_PARAM * 1000), _message, getClass().getSimpleName()));
					break;
				}
			}
		}
	}
}
