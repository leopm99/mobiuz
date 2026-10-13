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
import com.l2journey.gameserver.network.NpcStringId;
import com.l2journey.gameserver.network.enums.ChatType;

import ai.AbstractNpcAI;

/**
 * AI for Kamaloka (33) - Crimson Hatu Otis
 * @author Gladicek
 */
public class CrimsonHatuOtis extends AbstractNpcAI
{
	private static final String SKILL = "SKILL";

	// Npc
	private static final int CRIMSON_HATU_OTIS = 18558;
	// Skills
	private static final SkillHolder BOSS_SPINING_SLASH = new SkillHolder(4737, 1);
	private static final SkillHolder BOSS_HASTE = new SkillHolder(4175, 1);
	
	private CrimsonHatuOtis()
	{
		addAttackId(CRIMSON_HATU_OTIS);
		addKillId(CRIMSON_HATU_OTIS);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case SKILL:
			{
				if (npc.isDead())
				{
					cancelQuestTimer(SKILL, npc, null);
					return null;
				}
				npc.setTarget(player);
				npc.doCast(BOSS_SPINING_SLASH.getSkill());
				startQuestTimer(SKILL, 60000, npc, null);
				break;
			}
			case "BUFF":
			{
				if (npc.isScriptValue(2))
				{
					npc.setTarget(npc);
					npc.doCast(BOSS_HASTE.getSkill());
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
	
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		if (npc.isScriptValue(0))
		{
			npc.setScriptValue(1);
			startQuestTimer(SKILL, 5000, npc, null);
		}
		else if (npc.isScriptValue(1) && (npc.getCurrentHp() < (npc.getMaxHp() * 0.3)))
		{
			npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.I_VE_HAD_IT_UP_TO_HERE_WITH_YOU_I_LL_TAKE_CARE_OF_YOU);
			npc.setScriptValue(2);
			startQuestTimer("BUFF", 1000, npc, null);
		}
	}
	
	@Override
	public void onKill(Npc npc, Player player, boolean isSummon)
	{
		cancelQuestTimer(SKILL, npc, null);
		cancelQuestTimer("BUFF", npc, null);
	}
	
	public static void main(String[] args)
	{
		new CrimsonHatuOtis();
	}
}