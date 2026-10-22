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
package handlers.effecthandlers;

import com.l2journey.gameserver.model.StatSet;
import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.conditions.Condition;
import com.l2journey.gameserver.model.effects.AbstractEffect;
import com.l2journey.gameserver.model.skill.Skill;
import com.l2journey.gameserver.model.skill.skillVariation.TreasureHunterSearchUtil;

/**
 * Treasure Hunter Search effect implementation.
 * <p>
 * Gaining or losing this buff never touches the chest directly, so already-known chests would stay in
 * whatever visibility state the player last saw them in. {@link TreasureHunterSearchUtil#refreshKnownInvisibleObjects(com.l2journey.gameserver.model.actor.Player)}
 * re-evaluates every currently known invisible object for the player as soon as the buff starts or ends,
 * sending it info if it just became visible or deleting it client-side if it just stopped being visible.
 * </p>
 * @author KingHanker
 */
public class TreasureHunterSearch extends AbstractEffect
{
	public TreasureHunterSearch(Condition attachCond, Condition applyCond, StatSet set, StatSet params)
	{
		super(attachCond, applyCond, set, params);
	}
	
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill)
	{
		if (effected.isPlayer())
		{
			TreasureHunterSearchUtil.refreshKnownInvisibleObjects(effected.asPlayer());
		}
	}
	
	@Override
	public void onExit(Creature effector, Creature effected, Skill skill)
	{
		if (effected.isPlayer())
		{
			TreasureHunterSearchUtil.refreshKnownInvisibleObjects(effected.asPlayer());
		}
	}
}
