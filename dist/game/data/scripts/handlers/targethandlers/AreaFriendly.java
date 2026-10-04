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
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import com.l2journey.gameserver.GeoData;
import com.l2journey.gameserver.handler.ITargetTypeHandler;
import com.l2journey.gameserver.model.World;
import com.l2journey.gameserver.model.WorldObject;
import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.instance.SiegeFlag;
import com.l2journey.gameserver.model.skill.Skill;
import com.l2journey.gameserver.model.skill.targets.TargetType;
import com.l2journey.gameserver.model.zone.ZoneId;
import com.l2journey.gameserver.network.SystemMessageId;

/**
 * @author Adry_85, KingHanker
 */
public class AreaFriendly implements ITargetTypeHandler
{
	@Override
	public List<WorldObject> getTargetList(Skill skill, Creature creature, boolean onlyFirst, Creature target)
	{
		final List<WorldObject> targetList = new LinkedList<>();
		final Player player = creature.asPlayer();
		if (!checkPrimaryTarget(player, target) && (skill.getCastRange() >= 0))
		{
			player.sendPacket(SystemMessageId.THAT_IS_AN_INCORRECT_TARGET);
			return targetList;
		}
		
		if (onlyFirst)
		{
			targetList.add(target);
			return targetList;
		}
		
		if (player.isInOlympiadMode())
		{
			targetList.add(player);
			return targetList;
		}
		
		targetList.add(target); // Add target to target list
		if (target != null)
		{
			final int maxTargets = skill.getAffectLimit();
			
			// Collect all valid targets in range first
			final List<Creature> potentialTargets = new ArrayList<>();
			World.getInstance().forEachVisibleObjectInRange(target, Creature.class, skill.getAffectRange(), obj ->
			{
				if (checkSecondaryTarget(player, obj) && (obj != creature))
				{
					potentialTargets.add(obj);
				}
			});
			
			// Sort by HP percentage (most injured first)
			potentialTargets.sort(new CharComparator());
			
			// Add the most injured targets up to the limit
			final int limit = (maxTargets > 0) ? Math.min(maxTargets - 1, potentialTargets.size()) : potentialTargets.size(); // -1 because target is already added
			for (int i = 0; i < limit; i++)
			{
				targetList.add(potentialTargets.get(i));
			}
		}
		
		return targetList;
	}
	
	/**
	 * Check if primary target (clicked target) is valid. Primary target can be any player/summon except NPCs, monsters, or raids.
	 * @param player
	 * @param target
	 * @return
	 */
	private boolean checkPrimaryTarget(Player player, Creature target)
	{
		if ((target == null) || target.isAlikeDead() || target.isDoor() || (target instanceof SiegeFlag))
		{
			return false;
		}
		
		// Cannot target NPCs, monsters, or raids
		if (target.isMonster() || target.isNpc() || target.isRaid() || !GeoData.getInstance().canSeeTarget(player, target))
		{
			return false;
		}
		
		if (target.isPlayable())
		{
			final Player targetPlayer = target.asPlayer();
			
			if (targetPlayer.inObserverMode() || targetPlayer.isInOlympiadMode())
			{
				return false;
			}
			
			// Cannot target enemies - check for war, duel
			if (player.isInDuelWith(target) || player.isAtWarWith(targetPlayer))
			{
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Check if secondary target (area targets) is valid. Secondary targets must be in party, clan, ally, or command channel.
	 * @param player
	 * @param target
	 * @return
	 */
	private boolean checkSecondaryTarget(Player player, Creature target)
	{
		if ((target == null) || target.isAlikeDead() || target.isDoor() || (target instanceof SiegeFlag) || target.isMonster() || target.isNpc())
		{
			return false;
		}
		
		if (!GeoData.getInstance().canSeeTarget(player, target))
		{
			return false;
		}
		
		if (target.isPlayable())
		{
			final Player targetPlayer = target.asPlayer();
			if (player == targetPlayer)
			{
				return true;
			}
			
			if (targetPlayer.inObserverMode() || targetPlayer.isInOlympiadMode() || player.isInDuelWith(target) || player.isAtWarWith(targetPlayer))
			{
				return false;
			}
			
			if (player.isInPartyWith(target))
			{
				return true;
			}
			
			if (target.isInsideZone(ZoneId.PVP))
			{
				return false;
			}
			
			return player.isInClanWith(target) || player.isInAllyWith(target) || player.isInCommandChannelWith(target);
		}
		
		return true;
	}
	
	public class CharComparator implements Comparator<Creature>
	{
		@Override
		public int compare(Creature char1, Creature char2)
		{
			return Double.compare((char1.getCurrentHp() / char1.getMaxHp()), (char2.getCurrentHp() / char2.getMaxHp()));
		}
	}
	
	@Override
	public Enum<TargetType> getTargetType()
	{
		return TargetType.AREA_FRIENDLY;
	}
}
