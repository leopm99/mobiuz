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
package handlers.effecthandlers;

import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;

import com.l2journey.gameserver.model.StatSet;
import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.actor.stat.CreatureStat;
import com.l2journey.gameserver.model.conditions.Condition;
import com.l2journey.gameserver.model.effects.AbstractEffect;
import com.l2journey.gameserver.model.skill.Skill;
import com.l2journey.gameserver.model.stats.TraitType;

/**
 * Defence Trait effect implementation.
 * @author NosBit
 */
public class DefenceTrait extends AbstractEffect
{
	private final Map<TraitType, Float> _defenceTraits = new EnumMap<>(TraitType.class);
	
	public DefenceTrait(Condition attachCond, Condition applyCond, StatSet set, StatSet params)
	{
		super(attachCond, applyCond, set, params);
		
		if (params.isEmpty())
		{
			LOGGER.warning(getClass().getSimpleName() + ": must have parameters.");
			return;
		}
		
		for (Entry<String, Object> param : params.getSet().entrySet())
		{
			try
			{
				final TraitType traitType = TraitType.valueOf(param.getKey());
				final float value = Float.parseFloat((String) param.getValue());
				if (value == 0)
				{
					continue;
				}
				_defenceTraits.put(traitType, (value + 100) / 100);
			}
			catch (NumberFormatException _)
			{
				LOGGER.warning(getClass().getSimpleName() + ": value of " + param.getKey() + " must be float value " + param.getValue() + " found.");
			}
			catch (Exception _)
			{
				LOGGER.warning(getClass().getSimpleName() + ": value of TraitType enum required but found: " + param.getValue());
			}
		}
	}
	
	@Override
	public void onExit(Creature effector, Creature effected, Skill skill)
	{
		final CreatureStat charStat = effected.getStat();
		synchronized (charStat.getDefenceTraits())
		{
			for (Entry<TraitType, Float> trait : _defenceTraits.entrySet())
			{
				if (trait.getValue() < 2.0f)
				{
					if (charStat.getDefenceTraitsCount()[trait.getKey().ordinal()] == 0)
					{
						continue;
					}
					
					charStat.getDefenceTraits()[trait.getKey().ordinal()] /= trait.getValue();
					charStat.getDefenceTraitsCount()[trait.getKey().ordinal()]--;
				}
				else
				{
					if (charStat.getTraitsInvul()[trait.getKey().ordinal()] == 0)
					{
						continue;
					}
					
					charStat.getTraitsInvul()[trait.getKey().ordinal()]--;
				}
			}
		}
	}
	
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill)
	{
		final CreatureStat charStat = effected.getStat();
		synchronized (charStat.getDefenceTraits())
		{
			for (Entry<TraitType, Float> trait : _defenceTraits.entrySet())
			{
				if (trait.getValue() < 2.0f)
				{
					charStat.getDefenceTraits()[trait.getKey().ordinal()] *= trait.getValue();
					charStat.getDefenceTraitsCount()[trait.getKey().ordinal()]++;
				}
				else
				{
					charStat.getTraitsInvul()[trait.getKey().ordinal()]++;
				}
			}
		}
	}
}
