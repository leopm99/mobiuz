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
package com.l2journey.gameserver.model;

import java.util.logging.Level;

import com.l2journey.commons.util.Rnd;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.instance.ControllableMob;
import com.l2journey.gameserver.model.actor.templates.NpcTemplate;

/**
 * A special spawn implementation to spawn controllable mob.
 * @author littlecrow
 */
public class GroupSpawn extends Spawn
{
	private final NpcTemplate _template;
	
	public GroupSpawn(NpcTemplate mobTemplate) throws ClassNotFoundException, NoSuchMethodException
	{
		super(mobTemplate);
		_template = mobTemplate;
		setAmount(1);
	}
	
	public Npc doGroupSpawn()
	{
		try
		{
			if (_template.isType("Pet") || _template.isType("Minion"))
			{
				return null;
			}
			
			int newlocx = 0;
			int newlocy = 0;
			int newlocz = 0;
			if ((getX() == 0) && (getY() == 0))
			{
				if (getLocationId() == 0)
				{
					return null;
				}
				
				return null;
			}
			
			newlocx = getX();
			newlocy = getY();
			newlocz = getZ();
			
			final Npc mob = new ControllableMob(_template);
			mob.setCurrentHpMp(mob.getMaxHp(), mob.getMaxMp());
			mob.setHeading(getHeading() == -1 ? Rnd.get(61794) : getHeading());
			mob.setSpawn(this);
			mob.spawnMe(newlocx, newlocy, newlocz);
			mob.onSpawn();
			
			return mob;
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e, () -> "NPC class not found: " + e.getMessage());
			return null;
		}
	}
}