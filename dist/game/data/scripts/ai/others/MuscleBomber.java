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
package ai.others;

import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.skill.holders.SkillHolder;
import com.l2journey.gameserver.util.LocationUtil;

import ai.AbstractNpcAI;

/**
 * Muscle Bomber AI.
 * @author Zoey76
 * @since 2.6.0.0
 */
public class MuscleBomber extends AbstractNpcAI
{
	// NPC
	private static final int MUSCLE_BOMBER = 25724;
	private static final int DRAKOS_ASSASSIN = 22823;
	// Skills
	private static final SkillHolder ENHANCE_LEVEL_1 = new SkillHolder(6842, 1);
	private static final SkillHolder ENHANCE_LEVEL_2 = new SkillHolder(6842, 2);
	// Variables
	private static final String HIGH_HP_FLAG = "HIGH_HP_FLAG";
	private static final String MED_HP_FLAG = "MED_HP_FLAG";
	private static final String LIMIT_FLAG = "LIMIT_FLAG";
	// Timers
	private static final String TIMER_SUMMON = "TIMER_SUMMON";
	private static final String TIMER_LIMIT = "TIMER_LIMIT";
	// Misc
	private static final int MAX_CHASE_DIST = 2500;
	private static final double HIGH_HP_PERCENTAGE = 0.80;
	private static final double MED_HP_PERCENTAGE = 0.50;
	
	public MuscleBomber()
	{
		addAttackId(MUSCLE_BOMBER);
	}
	
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		if (LocationUtil.calculateDistance(npc, npc.getSpawn(), false, false) > MAX_CHASE_DIST)
		{
			npc.teleToLocation(npc.getSpawn().getX(), npc.getSpawn().getY(), npc.getSpawn().getZ());
		}
		
		if ((npc.getCurrentHp() < (npc.getMaxHp() * HIGH_HP_PERCENTAGE)) && !npc.getVariables().getBoolean(HIGH_HP_FLAG, false))
		{
			npc.getVariables().set(HIGH_HP_FLAG, true);
			addSkillCastDesire(npc, npc, ENHANCE_LEVEL_1, 99999);
		}
		
		if ((npc.getCurrentHp() < (npc.getMaxHp() * MED_HP_PERCENTAGE)) && !npc.getVariables().getBoolean(MED_HP_FLAG, false))
		{
			npc.getVariables().set(MED_HP_FLAG, true);
			addSkillCastDesire(npc, npc, ENHANCE_LEVEL_2, 99999);
			startQuestTimer(TIMER_SUMMON, 60000, npc, attacker);
			startQuestTimer(TIMER_LIMIT, 300000, npc, attacker);
		}
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case TIMER_LIMIT:
			{
				npc.getVariables().set(LIMIT_FLAG, true);
				break;
			}
			case TIMER_SUMMON:
			{
				if (!npc.isDead() && !npc.getVariables().getBoolean(LIMIT_FLAG, false))
				{
					if (player != null)
					{
						addAttackDesire(addSpawn(DRAKOS_ASSASSIN, npc.getX() + getRandom(100), npc.getY() + getRandom(10), npc.getZ(), npc.getHeading(), false, 0), player);
						addAttackDesire(addSpawn(DRAKOS_ASSASSIN, npc.getX() + getRandom(100), npc.getY() + getRandom(10), npc.getZ(), npc.getHeading(), false, 0), player);
					}
					startQuestTimer(TIMER_SUMMON, 60000, npc, player);
				}
				break;
			}
			default:
			{
				break;
			}
		}
		return super.onEvent(event, npc, player);
	}
	
	public static void main(String[] args)
	{
		new MuscleBomber();
	}
}
