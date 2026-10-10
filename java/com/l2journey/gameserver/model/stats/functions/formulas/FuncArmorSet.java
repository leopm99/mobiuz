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
package com.l2journey.gameserver.model.stats.functions.formulas;

import java.util.EnumMap;
import java.util.Map;

import com.l2journey.gameserver.data.xml.ArmorSetData;
import com.l2journey.gameserver.model.ArmorSet;
import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.item.instance.Item;
import com.l2journey.gameserver.model.skill.Skill;
import com.l2journey.gameserver.model.stats.Stat;
import com.l2journey.gameserver.model.stats.functions.AbstractFunction;

/**
 * @author UnAfraid
 */
public class FuncArmorSet extends AbstractFunction
{
	private static final Map<Stat, FuncArmorSet> _fh_instance = new EnumMap<>(Stat.class);
	
	public static AbstractFunction getInstance(Stat st)
	{
		if (!_fh_instance.containsKey(st))
		{
			_fh_instance.put(st, new FuncArmorSet(st));
		}
		return _fh_instance.get(st);
	}
	
	private FuncArmorSet(Stat stat)
	{
		super(stat, 1, null, 0, null);
	}
	
	@Override
	public double calc(Creature effector, Creature effected, Skill skill, double initVal)
	{
		double value = initVal;
		// Should not apply armor set bonus to summons.
		if (effector.isPlayer())
		{
			final Player player = effector.asPlayer();
			final Item chest = player.getChestArmorInstance();
			if (chest != null)
			{
				final ArmorSet set = ArmorSetData.getInstance().getSet(chest.getId());
				if ((set != null) && set.containAll(player))
				{
					switch (getStat())
					{
						case STAT_STR:
						{
							value += set.getSTR();
							break;
						}
						case STAT_DEX:
						{
							value += set.getDEX();
							break;
						}
						case STAT_INT:
						{
							value += set.getINT();
							break;
						}
						case STAT_MEN:
						{
							value += set.getMEN();
							break;
						}
						case STAT_CON:
						{
							value += set.getCON();
							break;
						}
						case STAT_WIT:
						{
							value += set.getWIT();
							break;
						}
						default:
						{
							break;
						}
					}
				}
			}
		}
		return value;
	}
}