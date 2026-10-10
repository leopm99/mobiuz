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

import com.l2journey.gameserver.model.actor.enums.creature.InstanceType;
import com.l2journey.gameserver.model.actor.templates.NpcTemplate;

/**
 * @author Gnacik
 */
public class EventMonster extends Monster
{
	// Block offensive skills usage on event mobs
	// mainly for AoE skills, disallow kill many event mobs
	// with one skill
	public boolean blockSkillAttack = false;
	
	// Event mobs should drop items to ground
	// but item pickup must be protected to killer
	// TODO: Some mobs need protect drop for spawner
	public boolean dropOnGround = false;
	
	/**
	 * Creates an event monster.
	 * @param template the event monster NPC template
	 */
	public EventMonster(NpcTemplate template)
	{
		super(template);
		setInstanceType(InstanceType.EventMob);
	}
	
	public void eventSetBlockOffensiveSkills(boolean value)
	{
		blockSkillAttack = value;
	}
	
	public void eventSetDropOnGround(boolean value)
	{
		dropOnGround = value;
	}
	
	public boolean eventDropOnGround()
	{
		return dropOnGround;
	}
	
	public boolean eventSkillAttackBlocked()
	{
		return blockSkillAttack;
	}
}