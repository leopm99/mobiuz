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
package ai.areas.DenOfEvil;

import com.l2journey.gameserver.ai.Intention;
import com.l2journey.gameserver.model.Location;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.itemcontainer.Inventory;
import com.l2journey.gameserver.model.skill.holders.SkillHolder;
import com.l2journey.gameserver.network.NpcStringId;
import com.l2journey.gameserver.network.enums.ChatType;

import ai.AbstractNpcAI;

/**
 * Frightened Ragna Orc AI.
 * @author Gladicek, malyelfik
 */
public class FrightenedRagnaOrc extends AbstractNpcAI
{
	private static final String REWARD = "reward";

	// NPC ID
	private static final int MOB_ID = 18807;
	// Chances
	private static final int ADENA = 10000;
	private static final int CHANCE = 1000;
	private static final int ADENA2 = 1000000;
	private static final int CHANCE2 = 10;
	// Skill
	private static final SkillHolder SKILL = new SkillHolder(6234, 1);
	
	private FrightenedRagnaOrc()
	{
		addAttackId(MOB_ID);
		addKillId(MOB_ID);
	}
	
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		if (npc.isScriptValue(0))
		{
			npc.setScriptValue(1);
			startQuestTimer("say", (getRandom(5) + 3) * 1000, npc, null, true);
		}
		else if ((npc.getCurrentHp() < (npc.getMaxHp() * 0.2)) && npc.isScriptValue(1))
		{
			startQuestTimer(REWARD, 10000, npc, attacker);
			npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.WAIT_WAIT_STOP_SAVE_ME_AND_I_LL_GIVE_YOU_10_000_000_ADENA);
			npc.setScriptValue(2);
		}
	}
	
	@Override
	public void onKill(Npc npc, Player player, boolean isSummon)
	{
		final NpcStringId msg = getRandomBoolean() ? NpcStringId.UGH_A_CURSE_UPON_YOU : NpcStringId.I_REALLY_DIDN_T_WANT_TO_FIGHT;
		npc.broadcastSay(ChatType.NPC_GENERAL, msg);
		cancelQuestTimer("say", npc, null);
		cancelQuestTimer(REWARD, npc, player);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "say":
			{
				if (npc.isDead() || !npc.isScriptValue(1))
				{
					cancelQuestTimer("say", npc, null);
					return null;
				}
				final NpcStringId msg = getRandomBoolean() ? NpcStringId.I_DON_T_WANT_TO_FIGHT : NpcStringId.IS_THIS_REALLY_NECESSARY;
				npc.broadcastSay(ChatType.NPC_GENERAL, msg);
				break;
			}
			case REWARD:
			{
				if (!npc.isDead() && npc.isScriptValue(2))
				{
					if (getRandom(100000) < CHANCE2)
					{
						final NpcStringId msg = getRandomBoolean() ? NpcStringId.TH_THANKS_I_COULD_HAVE_BECOME_GOOD_FRIENDS_WITH_YOU : NpcStringId.I_LL_GIVE_YOU_10_000_000_ADENA_LIKE_I_PROMISED_I_MIGHT_BE_AN_ORC_WHO_KEEPS_MY_PROMISES;
						npc.broadcastSay(ChatType.NPC_GENERAL, msg);
						npc.setScriptValue(3);
						npc.doCast(SKILL.getSkill());
						for (int i = 0; i < 10; i++)
						{
							npc.dropItem(player, Inventory.ADENA_ID, ADENA2);
						}
					}
					else if (getRandom(100000) < CHANCE)
					{
						final NpcStringId msg = getRandomBoolean() ? NpcStringId.TH_THANKS_I_COULD_HAVE_BECOME_GOOD_FRIENDS_WITH_YOU : NpcStringId.SORRY_BUT_THIS_IS_ALL_I_HAVE_GIVE_ME_A_BREAK;
						npc.broadcastSay(ChatType.NPC_GENERAL, msg);
						npc.setScriptValue(3);
						npc.doCast(SKILL.getSkill());
						for (int i = 0; i < 10; i++)
						{
							npc.asAttackable().dropItem(player, Inventory.ADENA_ID, ADENA);
						}
					}
					else
					{
						npc.broadcastSay(ChatType.NPC_GENERAL, getRandomBoolean() ? NpcStringId.THANKS_BUT_THAT_THING_ABOUT_10_000_000_ADENA_WAS_A_LIE_SEE_YA : NpcStringId.YOU_RE_PRETTY_DUMB_TO_BELIEVE_ME);
					}
					startQuestTimer("despawn", 1000, npc, null);
				}
				break;
			}
			case "despawn":
			{
				npc.setRunning();
				npc.getAI().setIntention(Intention.MOVE_TO, new Location((npc.getX() + getRandom(-800, 800)), (npc.getY() + getRandom(-800, 800)), npc.getZ(), npc.getHeading()));
				npc.deleteMe();
				break;
			}
			default:
			{
				break;
			}
		}
		return null;
	}
	
	public static void main(String[] args)
	{
		new FrightenedRagnaOrc();
	}
}