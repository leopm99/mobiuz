/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.l2journey.gameserver.model.actor.instance;

import com.l2journey.gameserver.data.xml.NpcData;
import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.enums.creature.InstanceType;
import com.l2journey.gameserver.model.actor.templates.NpcTemplate;
import com.l2journey.gameserver.model.skill.CommonSkill;
import com.l2journey.gameserver.model.skill.Skill;

/**
 * This class manages all chest.
 * @author Julian
 */
public class Chest extends Monster
{
	private volatile boolean _specialDrop;
	
	/**
	 * Creates a chest.
	 * @param template the chest NPC template
	 */
	public Chest(NpcTemplate template)
	{
		super(template);
		setInstanceType(InstanceType.Chest);
		setRandomWalking(false);
		_specialDrop = false;
	}
	
	@Override
	public void onSpawn()
	{
		super.onSpawn();
		_specialDrop = false;
		setMustRewardExpSp(true);
	}
	
	public synchronized void setSpecialDrop()
	{
		_specialDrop = true;
	}
	
	@Override
	public void doItemDrop(NpcTemplate npcTemplate, Creature lastAttacker)
	{
		int id = getTemplate().getId();
		if (!_specialDrop)
		{
			if ((id >= 18265) && (id <= 18286))
			{
				id += 3536;
			}
			else if ((id == 18287) || (id == 18288))
			{
				id = 21671;
			}
			else if ((id == 18289) || (id == 18290))
			{
				id = 21694;
			}
			else if ((id == 18291) || (id == 18292))
			{
				id = 21717;
			}
			else if ((id == 18293) || (id == 18294))
			{
				id = 21740;
			}
			else if ((id == 18295) || (id == 18296))
			{
				id = 21763;
			}
			else if ((id == 18297) || (id == 18298))
			{
				id = 21786;
			}
		}
		super.doItemDrop(NpcData.getInstance().getTemplate(id), lastAttacker);
	}
	
	@Override
	public boolean isMovementDisabled()
	{
		return true;
	}
	
	@Override
	public boolean hasRandomAnimation()
	{
		return false;
	}
	
	/**
	 * Hidden treasure chests (spawned invisible by the HiddenTreasureChest script) are also visible
	 * to players currently affected by the "Treasure Hunter Search" buff, in addition to the default
	 * GM-only invisible object visibility.
	 * @param player the player checking visibility
	 * @return {@code true} if the chest should be visible for the given player
	 */
	@Override
	public boolean isVisibleFor(Player player)
	{
		return super.isVisibleFor(player) || (isInvisible() && player.isAffectedBySkill(CommonSkill.TREASURE_HUNTER_SEARCH.getId()));
	}
	
	/**
	 * As soon as a hidden treasure chest is attacked, it stops being invisible and becomes visible for everyone.
	 */
	@Override
	public void reduceCurrentHp(double damage, Creature attacker, boolean awake, boolean isDOT, Skill skill)
	{
		if (isInvisible())
		{
			setInvisible(false);
		}
		super.reduceCurrentHp(damage, attacker, awake, isDOT, skill);
	}
}
