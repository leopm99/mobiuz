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
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.enums.creature.InstanceType;
import com.l2journey.gameserver.model.events.EventDispatcher;
import com.l2journey.gameserver.model.events.EventType;
import com.l2journey.gameserver.model.events.holders.actor.player.OnPlayerSummonTalk;
import com.l2journey.gameserver.network.SystemMessageId;
import com.l2journey.gameserver.network.serverpackets.ActionFailed;
import com.l2journey.gameserver.network.serverpackets.PetStatusShow;

public class SummonAction implements IActionHandler
{
	@Override
	public boolean action(Player player, WorldObject target, boolean interact)
	{
		// Aggression target lock effect
		if (player.isLockedTarget() && (player.getLockedTarget() != target))
		{
			player.sendPacket(SystemMessageId.FAILED_TO_CHANGE_ATTACK_TARGET);
			return false;
		}
		
		if ((player == target.asSummon().getOwner()) && (player.getTarget() == target))
		{
			player.sendPacket(new PetStatusShow(target.asSummon()));
			player.setPetStatusWindowOpen(true);
			player.updateNotMoveUntil();
			player.sendPacket(ActionFailed.STATIC_PACKET);
			
			// Notify to scripts
			if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_SUMMON_TALK, target))
			{
				EventDispatcher.getInstance().notifyEventAsync(new OnPlayerSummonTalk(target.asSummon()), target);
			}
		}
		else if (player.getTarget() != target)
		{
			player.setTarget(target);
		}
		else if (interact)
		{
			if (target.isAutoAttackable(player))
			{
				player.getAI().setIntention(Intention.ATTACK, target);
				player.onActionRequest();
			}
			else
			{
				// This Action Failed packet avoids player getting stuck when clicking three or more times
				player.sendPacket(ActionFailed.STATIC_PACKET);
				if (target.asSummon().isInsideRadius2D(player, 150))
				{
					player.updateNotMoveUntil();
				}
				player.getAI().setIntention(Intention.FOLLOW, target);
			}
		}
		return true;
	}
	
	@Override
	public InstanceType getInstanceType()
	{
		return InstanceType.Summon;
	}
}
