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
package handlers.targethandlers;

import java.util.LinkedList;
import java.util.List;

import com.l2journey.gameserver.handler.ITargetTypeHandler;
import com.l2journey.gameserver.model.World;
import com.l2journey.gameserver.model.WorldObject;
import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.skill.Skill;
import com.l2journey.gameserver.model.skill.targets.TargetType;

/**
 * @author UnAfraid
 */
public class PartyClan implements ITargetTypeHandler
{
	@Override
	public List<WorldObject> getTargetList(Skill skill, Creature creature, boolean onlyFirst, Creature target)
	{
		final List<WorldObject> targetList = new LinkedList<>();
		final Player player = creature.asPlayer();
		if (player == null)
		{
			return targetList;
		}
		
		targetList.add(player);
		
		if (onlyFirst)
		{
			return targetList;
		}
		
		final int radius = skill.getAffectRange();
		final boolean hasClan = player.getClan() != null;
		final boolean hasParty = player.isInParty();
		if (Skill.addSummon(creature, player, radius, false))
		{
			targetList.add(player.getSummon());
		}
		
		// if player in clan and not in party
		if (!(hasClan || hasParty))
		{
			return targetList;
		}
		
		// Get all visible objects in a spherical area near the Creature
		final int maxTargets = skill.getAffectLimit();
		for (Player obj : World.getInstance().getVisibleObjectsInRange(creature, Player.class, radius))
		{
			if (obj == null)
			{
				continue;
			}
			
			// olympiad mode - adding only own side
			if ((player.isInOlympiadMode()) && (!obj.isInOlympiadMode() || (player.getOlympiadGameId() != obj.getOlympiadGameId()) || (player.getOlympiadSide() != obj.getOlympiadSide())))
			{
				continue;
			}
			
			if ((player.isInDuel()) && ((player.getDuelId() != obj.getDuelId()) || (hasParty && obj.isInParty() && (player.getParty().getLeaderObjectId() != obj.getParty().getLeaderObjectId()))))
			{
				continue;
			}
			
			// Don't add this target if this is a Pc->Pc pvp
			// casting and pvp condition not met
			if (!((hasClan && (obj.getClanId() == player.getClanId())) || (hasParty && obj.isInParty() && (player.getParty().getLeaderObjectId() == obj.getParty().getLeaderObjectId()))) || !player.checkPvpSkill(obj, skill))
			{
				continue;
			}
			
			if (player.isOnEvent() && !player.isOnSoloEvent() && obj.isOnEvent() && (player.getTeam() != obj.getTeam()))
			{
				continue;
			}
			
			if (!onlyFirst && Skill.addSummon(creature, obj, radius, false))
			{
				targetList.add(obj.getSummon());
			}
			
			if (!Skill.addCharacter(creature, obj, radius, false))
			{
				continue;
			}
			
			targetList.add(obj);
			
			if (onlyFirst)
			{
				return targetList;
			}
			
			if ((maxTargets > 0) && (targetList.size() >= maxTargets))
			{
				break;
			}
		}
		
		return targetList;
	}
	
	@Override
	public Enum<TargetType> getTargetType()
	{
		return TargetType.PARTY_CLAN;
	}
}
