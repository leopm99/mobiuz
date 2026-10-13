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
package ai.areas.Hellbound.AI;

import com.l2journey.gameserver.ai.Intention;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.instance.Monster;
import com.l2journey.gameserver.model.skill.BuffInfo;
import com.l2journey.gameserver.model.skill.Skill;
import com.l2journey.gameserver.model.skill.holders.SkillHolder;
import com.l2journey.gameserver.network.NpcStringId;
import com.l2journey.gameserver.network.enums.ChatType;

import ai.AbstractNpcAI;
import ai.areas.Hellbound.HellboundEngine;

/**
 * Manages Amaskari's and minions' chat and some skill usage.
 * @author GKR
 */
public class Amaskari extends AbstractNpcAI
{
	private static final String ONSPAWN_MSG = "onspawn_msg";

	// NPCs
	private static final int AMASKARI = 22449;
	private static final int AMASKARI_PRISONER = 22450;
	// Skills
	private static final int BUFF_ID = 4632;
	private static final SkillHolder[] BUFF =
	{
		new SkillHolder(BUFF_ID, 1),
		new SkillHolder(BUFF_ID, 2),
		new SkillHolder(BUFF_ID, 3)
	};
	// Misc
	private static final NpcStringId[] AMASKARI_NPCSTRING_ID =
	{
		NpcStringId.I_LL_MAKE_EVERYONE_FEEL_THE_SAME_SUFFERING_AS_ME,
		NpcStringId.HA_HA_YES_DIE_SLOWLY_WRITHING_IN_PAIN_AND_AGONY,
		NpcStringId.MORE_NEED_MORE_SEVERE_PAIN,
		NpcStringId.SOMETHING_IS_BURNING_INSIDE_MY_BODY
	};
	private static final NpcStringId[] MINIONS_NPCSTRING_ID =
	{
		NpcStringId.AHH_MY_LIFE_IS_BEING_DRAINED_OUT,
		NpcStringId.THANK_YOU_FOR_SAVING_ME,
		NpcStringId.IT_WILL_KILL_EVERYONE,
		NpcStringId.EEEK_I_FEEL_SICK_YOW
	};
	
	public Amaskari()
	{
		addKillId(AMASKARI, AMASKARI_PRISONER);
		addAttackId(AMASKARI);
		addSpawnId(AMASKARI_PRISONER);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.equalsIgnoreCase("stop_toggle"))
		{
			npc.broadcastSay(ChatType.NPC_GENERAL, AMASKARI_NPCSTRING_ID[2]);
			npc.asMonster().clearAggroList();
			npc.asMonster().getAI().setIntention(Intention.ACTIVE);
			npc.setInvul(false);
			// npc.doCast(INVINCIBILITY.getSkill())
		}
		else if (event.equalsIgnoreCase(ONSPAWN_MSG) && (npc != null) && !npc.isDead())
		{
			if (getRandom(100) > 20)
			{
				npc.broadcastSay(ChatType.NPC_GENERAL, MINIONS_NPCSTRING_ID[2]);
			}
			else if (getRandom(100) > 40)
			{
				npc.broadcastSay(ChatType.NPC_GENERAL, MINIONS_NPCSTRING_ID[3]);
			}
			startQuestTimer(ONSPAWN_MSG, (getRandom(8) + 1) * 30000, npc, null);
		}
		return null;
	}
	
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon, Skill skill)
	{
		if ((npc.getId() == AMASKARI) && (getRandom(1000) < 25))
		{
			npc.broadcastSay(ChatType.NPC_GENERAL, AMASKARI_NPCSTRING_ID[0]);
			for (Monster minion : npc.asMonster().getMinionList().getSpawnedMinions())
			{
				if ((minion != null) && !minion.isDead() && (getRandom(10) == 0))
				{
					minion.broadcastSay(ChatType.NPC_GENERAL, MINIONS_NPCSTRING_ID[0]);
					minion.setCurrentHp(minion.getCurrentHp() - (minion.getCurrentHp() / 5));
				}
			}
		}
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		if (npc.getId() == AMASKARI_PRISONER)
		{
			final Monster master = npc.asMonster().getLeader();
			if ((master != null) && !master.isDead())
			{
				master.broadcastSay(ChatType.NPC_GENERAL, AMASKARI_NPCSTRING_ID[1]);
				final BuffInfo info = master.getEffectList().getBuffInfoBySkillId(BUFF_ID);
				if ((info != null) && (info.getSkill().getAbnormalLevel() == 3) && master.isInvul())
				{
					master.setCurrentHp(master.getCurrentHp() + (master.getCurrentHp() / 5));
				}
				else
				{
					master.clearAggroList();
					master.getAI().setIntention(Intention.ACTIVE);
					if (info == null)
					{
						master.doCast(BUFF[0].getSkill());
					}
					else if (info.getSkill().getAbnormalLevel() < 3)
					{
						master.doCast(BUFF[info.getSkill().getAbnormalLevel()].getSkill());
					}
					else
					{
						master.broadcastSay(ChatType.NPC_GENERAL, AMASKARI_NPCSTRING_ID[3]);
						// master.doCast(INVINCIBILITY.getSkill())
						master.setInvul(true);
						startQuestTimer("stop_toggle", 10000, master, null);
					}
				}
			}
		}
		else if (npc.getId() == AMASKARI)
		{
			for (Monster minion : npc.asMonster().getMinionList().getSpawnedMinions())
			{
				if ((minion != null) && !minion.isDead())
				{
					if (getRandom(1000) > 300)
					{
						minion.broadcastSay(ChatType.NPC_GENERAL, MINIONS_NPCSTRING_ID[1]);
					}
					HellboundEngine.getInstance().updateTrust(30, true);
					minion.deleteMe();
				}
			}
		}
	}
	
	@Override
	public void onSpawn(Npc npc)
	{
		startQuestTimer(ONSPAWN_MSG, (getRandom(3) + 1) * 30000, npc, null);
	}
}