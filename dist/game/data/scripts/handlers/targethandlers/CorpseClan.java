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

import java.util.ArrayList;
import java.util.List;

import com.l2journey.gameserver.handler.ITargetTypeHandler;
import com.l2journey.gameserver.model.World;
import com.l2journey.gameserver.model.WorldObject;
import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.clan.Clan;
import com.l2journey.gameserver.model.clan.ClanMember;
import com.l2journey.gameserver.model.skill.Skill;
import com.l2journey.gameserver.model.skill.targets.TargetType;
import com.l2journey.gameserver.model.zone.ZoneId;

/**
 * @author UnAfraid
 */
public class CorpseClan implements ITargetTypeHandler
{
	@Override
	public List<WorldObject> getTargetList(Skill skill, Creature creature, boolean onlyFirst, Creature target)
	{
		final List<WorldObject> targetList = new ArrayList<>();
		if (creature.isPlayable())
		{
			final Player player = creature.asPlayer();
			if (player == null)
			{
				return targetList;
			}
			
			if (player.isInOlympiadMode())
			{
				targetList.add(player);
				return targetList;
			}
			
			final Clan clan = player.getClan();
			if (clan != null)
			{
				final int radius = skill.getAffectRange();
				final int maxTargets = skill.getAffectLimit();
				for (ClanMember member : clan.getMembers())
				{
					final Player obj = member.getPlayer();
					if ((obj == null) || (obj == player))
					{
						continue;
					}
					
					if ((player.isInDuel()) && ((player.getDuelId() != obj.getDuelId()) || (player.isInParty() && obj.isInParty() && (player.getParty().getLeaderObjectId() != obj.getParty().getLeaderObjectId()))))
					{
						continue;
					}
					
					// Don't add this target if this is a Pc->Pc pvp casting and pvp condition not met
					if (!player.checkPvpSkill(obj, skill))
					{
						continue;
					}
					
					if (player.isOnEvent() && !player.isOnSoloEvent() && obj.isOnEvent() && (player.getTeam() != obj.getTeam()))
					{
						continue;
					}
					
					// check target is not in a active siege zone
					if (!Skill.addCharacter(creature, obj, radius, true) || (obj.isInsideZone(ZoneId.SIEGE) && !obj.isInSiege()))
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
			}
		}
		else if (creature.isNpc())
		{
			// for buff purposes, returns friendly mobs nearby and mob itself
			targetList.add(creature);
			final Npc npc = creature.asNpc();
			if ((npc.getTemplate().getClans() == null) || npc.getTemplate().getClans().isEmpty())
			{
				return targetList;
			}
			
			for (Npc newTarget : World.getInstance().getVisibleObjectsInRange(creature, Npc.class, skill.getCastRange()))
			{
				if (npc.isInMyClan(newTarget))
				{
					if (targetList.size() >= skill.getAffectLimit())
					{
						break;
					}
					
					targetList.add(newTarget);
				}
			}
		}
		
		return targetList;
	}
	
	@Override
	public Enum<TargetType> getTargetType()
	{
		return TargetType.CORPSE_CLAN;
	}
}
