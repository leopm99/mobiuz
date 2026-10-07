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
package handlers.actionhandlers;

import com.l2journey.gameserver.ai.Intention;
import com.l2journey.gameserver.handler.IActionHandler;
import com.l2journey.gameserver.model.WorldObject;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.enums.creature.InstanceType;
import com.l2journey.gameserver.model.actor.holders.creature.DoorRequestHolder;
import com.l2journey.gameserver.model.actor.instance.Door;
import com.l2journey.gameserver.model.clan.Clan;
import com.l2journey.gameserver.model.siege.clanhalls.SiegableHall;
import com.l2journey.gameserver.network.serverpackets.ConfirmDlg;

public class DoorAction implements IActionHandler
{
	@Override
	public boolean action(Player player, WorldObject target, boolean interact)
	{
		// Check if the Player already target the Npc
		if (player.getTarget() != target)
		{
			player.setTarget(target);
		}
		else if (interact)
		{
			final Door door = target.asDoor();
			final Clan clan = player.getClan();
			if (target.isAutoAttackable(player))
			{
				if (Math.abs(player.getZ() - target.getZ()) < 400)
				{
					player.getAI().setIntention(Intention.ATTACK, target);
				}
			}
			else if ((clan != null) && (door.getClanHall() != null) && (player.getClanId() == door.getClanHall().getOwnerId()))
			{
				if (!door.isInsideRadius2D(player, Npc.INTERACTION_DISTANCE))
				{
					player.getAI().setIntention(Intention.INTERACT, target);
				}
				else if (!door.getClanHall().isSiegableHall() || !((SiegableHall) door.getClanHall()).isInSiege())
				{
					player.addScript(new DoorRequestHolder(door));
					if (!door.isOpen())
					{
						player.sendPacket(new ConfirmDlg(1140));
					}
					else
					{
						player.sendPacket(new ConfirmDlg(1141));
					}
				}
			}
			else if ((clan != null) && (target.asDoor().getFort() != null) && (clan == target.asDoor().getFort().getOwnerClan()) && target.asDoor().isOpenableBySkill() && !target.asDoor().getFort().getSiege().isInProgress())
			{
				if (!target.asCreature().isInsideRadius2D(player, Npc.INTERACTION_DISTANCE))
				{
					player.getAI().setIntention(Intention.INTERACT, target);
				}
				else
				{
					player.addScript(new DoorRequestHolder(target.asDoor()));
					if (!target.asDoor().isOpen())
					{
						player.sendPacket(new ConfirmDlg(1140));
					}
					else
					{
						player.sendPacket(new ConfirmDlg(1141));
					}
				}
			}
		}
		return true;
	}
	
	@Override
	public InstanceType getInstanceType()
	{
		return InstanceType.Door;
	}
}
