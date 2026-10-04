/*
 * Copyright (c) 2013 L2jMobius
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package handlers.effecthandlers;

import java.util.HashMap;
import java.util.Map;

import com.l2journey.gameserver.model.StatSet;
import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.Summon;
import com.l2journey.gameserver.model.conditions.Condition;
import com.l2journey.gameserver.model.effects.AbstractEffect;
import com.l2journey.gameserver.model.effects.EffectFlag;
import com.l2journey.gameserver.model.effects.EffectType;
import com.l2journey.gameserver.model.skill.Skill;
import com.l2journey.gameserver.model.stats.Stat;

/**
 * Servitor Share effect implementation.<br>
 * Synchronizing effects on player and servitor if one of them gets removed for some reason the same will happen to another. Partner's effect exit is executed in own thread, since there is no more queue to schedule the effects,<br>
 * partner's effect is called while this effect is still exiting issuing an exit call for the effect, causing a stack over flow.
 * @author UnAfraid, Zoey76
 */
public class ServitorShare extends AbstractEffect
{
	private final Map<Stat, Double> _stats = HashMap.newHashMap(9);
	
	public ServitorShare(Condition attachCond, Condition applyCond, StatSet set, StatSet params)
	{
		super(attachCond, applyCond, set, params);
		
		for (String key : params.getSet().keySet())
		{
			_stats.put(Stat.valueOfXml(key), params.getDouble(key, 1.));
		}
	}
	
	@Override
	public int getEffectFlags()
	{
		return EffectFlag.SERVITOR_SHARE.getMask();
	}
	
	@Override
	public EffectType getEffectType()
	{
		return EffectType.BUFF;
	}
	
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill)
	{
		super.onStart(effector, effected, skill);
		
		final Player player = effected.asPlayer();
		player.setServitorShare(_stats);
		
		final Summon summon = player.getSummon();
		if (summon != null)
		{
			summon.broadcastInfo();
			summon.getStatus().startHpMpRegeneration();
		}
	}
	
	@Override
	public void onExit(Creature effector, Creature effected, Skill skill)
	{
		final Player player = effected.asPlayer();
		player.setServitorShare(null);
		
		final Summon summon = player.getSummon();
		if (summon != null)
		{
			if (summon.getCurrentHp() > summon.getMaxHp())
			{
				summon.setCurrentHp(summon.getMaxHp());
			}
			if (summon.getCurrentMp() > summon.getMaxMp())
			{
				summon.setCurrentMp(summon.getMaxMp());
			}
			summon.broadcastInfo();
		}
	}
}
