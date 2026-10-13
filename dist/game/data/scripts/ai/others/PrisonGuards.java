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

import java.util.List;

import com.l2journey.gameserver.model.WorldObject;
import com.l2journey.gameserver.model.actor.Attackable;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.skill.Skill;
import com.l2journey.gameserver.model.skill.holders.SkillHolder;
import com.l2journey.gameserver.network.NpcStringId;
import com.l2journey.gameserver.network.enums.ChatType;

import ai.AbstractNpcAI;

/**
 * Prison Guards AI.
 * @author St3eT
 */
public class PrisonGuards extends AbstractNpcAI
{
	private static final String CHECK_HOME = "CHECK_HOME";

	// NPCs
	private static final int GUARD_HEAD = 18367; // Prison Guard
	private static final int GUARD = 18368; // Prison Guard
	// Item
	private static final int STAMP = 10013; // Race Stamp
	// Skills
	private static final int TIMER = 5239; // Event Timer
	private static final SkillHolder STONE = new SkillHolder(4578, 1); // Petrification
	private static final SkillHolder SILENCE = new SkillHolder(4098, 9); // Silence
	
	private PrisonGuards()
	{
		addAttackId(GUARD_HEAD, GUARD);
		addSpawnId(GUARD_HEAD, GUARD);
		addNpcHateId(GUARD);
		addSkillSeeId(GUARD);
		addSpellFinishedId(GUARD_HEAD, GUARD);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.equals("CLEAR_STATUS"))
		{
			npc.setScriptValue(0);
		}
		else if (event.equals(CHECK_HOME))
		{
			if ((npc.calculateDistance2D(npc.getSpawn().getLocation()) > 10) && !npc.isInCombat() && !npc.isDead())
			{
				npc.teleToLocation(npc.getSpawn().getLocation());
			}
			startQuestTimer(CHECK_HOME, 30000, npc, null);
		}
		return super.onEvent(event, npc, player);
	}
	
	@Override
	public void onAttack(Npc npc, Player player, int damage, boolean isSummon)
	{
		if (npc.getId() == GUARD_HEAD)
		{
			if (player.isAffectedBySkill(TIMER))
			{
				if ((getRandom(100) < 10) && (npc.calculateDistance3D(player) < 100) && (getQuestItemsCount(player, STAMP) <= 3) && npc.isScriptValue(0))
				{
					giveItems(player, STAMP, 1);
					npc.setScriptValue(1);
					startQuestTimer("CLEAR_STATUS", 600000, npc, null);
				}
			}
			else
			{
				npc.setTarget(player);
				npc.doCast(STONE.getSkill());
				npc.broadcastSay(ChatType.GENERAL, NpcStringId.IT_S_NOT_EASY_TO_OBTAIN);
			}
		}
		else
		{
			if (!player.isAffectedBySkill(TIMER) && (npc.calculateDistance2D(npc.getSpawn().getLocation()) < 2000))
			{
				npc.setTarget(player);
				npc.doCast(STONE.getSkill());
				npc.broadcastSay(ChatType.GENERAL, NpcStringId.YOU_RE_OUT_OF_YOUR_MIND_COMING_HERE);
			}
		}
	}
	
	@Override
	public void onSkillSee(Npc npc, Player caster, Skill skill, List<WorldObject> targets, boolean isSummon)
	{
		if (!caster.isAffectedBySkill(TIMER))
		{
			npc.setTarget(caster);
			npc.doCast(SILENCE.getSkill());
		}
	}
	
	@Override
	public void onSpellFinished(Npc npc, Player player, Skill skill)
	{
		if ((skill == SILENCE.getSkill()) || (skill == STONE.getSkill()))
		{
			npc.asAttackable().clearAggroList();
			npc.setTarget(npc);
		}
	}
	
	@Override
	public boolean onNpcHate(Attackable mob, Player player, boolean isSummon)
	{
		return player.isAffectedBySkill(TIMER);
	}
	
	@Override
	public void onSpawn(Npc npc)
	{
		if (npc.getId() == GUARD_HEAD)
		{
			npc.setImmobilized(true);
			npc.setInvul(true);
		}
		else
		{
			npc.setRandomWalking(false);
			cancelQuestTimer(CHECK_HOME, npc, null);
			startQuestTimer(CHECK_HOME, 30000, npc, null);
		}
	}
	
	public static void main(String[] args)
	{
		new PrisonGuards();
	}
}
