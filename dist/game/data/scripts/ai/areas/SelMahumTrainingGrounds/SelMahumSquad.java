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
package ai.areas.SelMahumTrainingGrounds;

import java.util.List;

import com.l2journey.gameserver.ai.Intention;
import com.l2journey.gameserver.data.xml.SkillData;
import com.l2journey.gameserver.model.Location;
import com.l2journey.gameserver.model.WorldObject;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.skill.Skill;
import com.l2journey.gameserver.network.NpcStringId;
import com.l2journey.gameserver.network.enums.ChatType;
import com.l2journey.gameserver.taskmanagers.GameTimeTaskManager;
import com.l2journey.gameserver.util.ArrayUtil;

import ai.AbstractNpcAI;

/**
 * Sel Mahum Training Ground AI for squads and chefs.
 * @author GKR
 */
public class SelMahumSquad extends AbstractNpcAI
{
	private static final String INVUL_REMOVE_TIMER_STARTED = "INVUL_REMOVE_TIMER_STARTED";
	private static final String CHEF_DISABLE_REWARD = "chef_disable_reward";
	private static final String CHEF_REMOVE_INVUL = "chef_remove_invul";
	private static final String CHEF_HEAL_PLAYER = "chef_heal_player";
	private static final String CHEF_SET_INVUL = "chef_set_invul";
	private static final String REMOVE_EFFECTS = "remove_effects";
	private static final String DESTINATION_X = "DESTINATION_X";
	private static final String DESTINATION_Y = "DESTINATION_Y";
	private static final String BUSY_STATE = "BUSY_STATE";

	// NPCs
	private static final int CHEF = 18908;
	private static final int FIRE = 18927;
	private static final int STOVE = 18933;
	
	private static final int OHS_WEAPON = 15280;
	private static final int THS_WEAPON = 15281;
	
	// Sel Mahum Squad Leaders
	private static final int[] SQUAD_LEADERS =
	{
		22786,
		22787,
		22788
	};
	
	private static final NpcStringId[] CHEF_FSTRINGS =
	{
		NpcStringId.I_BROUGHT_THE_FOOD,
		NpcStringId.COME_AND_EAT
	};
	
	private static final int FIRE_EFFECT_BURN = 1;
	private static final int FIRE_EFFECT_NONE = 2;
	
	private static final int MAHUM_EFFECT_EAT = 1;
	private static final int MAHUM_EFFECT_SLEEP = 2;
	private static final int MAHUM_EFFECT_NONE = 3;
	
	private SelMahumSquad()
	{
		addAttackId(CHEF);
		addAttackId(SQUAD_LEADERS);
		addEventReceivedId(CHEF, FIRE, STOVE);
		addEventReceivedId(SQUAD_LEADERS);
		addFactionCallId(SQUAD_LEADERS);
		addKillId(CHEF);
		addMoveFinishedId(SQUAD_LEADERS);
		addNodeArrivedId(CHEF);
		addSkillSeeId(STOVE);
		addSpawnId(CHEF, FIRE);
		addSpawnId(SQUAD_LEADERS);
		addSpellFinishedId(CHEF);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case CHEF_DISABLE_REWARD:
			{
				npc.getVariables().set("REWARD_TIME_GONE", 1);
				break;
			}
			case CHEF_HEAL_PLAYER:
			{
				healPlayer(npc, player);
				break;
			}
			case CHEF_REMOVE_INVUL:
			{
				if (npc.isMonster())
				{
					npc.setInvul(false);
					npc.getVariables().remove(INVUL_REMOVE_TIMER_STARTED);
					if ((player != null) && !player.isDead() && npc.isInSurroundingRegion(player))
					{
						addAttackDesire(npc, player);
					}
				}
				break;
			}
			case CHEF_SET_INVUL:
			{
				if (!npc.isDead())
				{
					npc.setInvul(true);
				}
				break;
			}
			case "fire":
			{
				startQuestTimer("fire", 30000 + getRandom(5000), npc, null);
				npc.setDisplayEffect(FIRE_EFFECT_NONE);
				
				if (getRandom(GameTimeTaskManager.getInstance().isNight() ? 2 : 4) < 1)
				{
					npc.setDisplayEffect(FIRE_EFFECT_BURN); // fire burns
					npc.broadcastEvent("SCE_CAMPFIRE_START", 600, null);
				}
				else
				{
					npc.setDisplayEffect(FIRE_EFFECT_NONE); // fire goes out
					npc.broadcastEvent("SCE_CAMPFIRE_END", 600, null);
				}
				break;
			}
			case "fire_arrived":
			{
				// myself.i_quest0 = 1;
				npc.setWalking();
				npc.setTarget(npc);
				
				if (!npc.isRandomWalkingEnabled())
				{
					npc.doCast(SkillData.getInstance().getSkill(6331, 1));
					npc.setDisplayEffect(MAHUM_EFFECT_SLEEP);
				}
				if (npc.getVariables().getInt(BUSY_STATE) == 1) // Eating
				{
					npc.doCast(SkillData.getInstance().getSkill(6332, 1));
					npc.setDisplayEffect(MAHUM_EFFECT_EAT);
				}
				
				startQuestTimer(REMOVE_EFFECTS, 300000, npc, null);
				break;
			}
			case "notify_dinner":
			{
				npc.broadcastEvent("SCE_DINNER_EAT", 600, null);
				break;
			}
			case REMOVE_EFFECTS:
			{
				// myself.i_quest0 = 0;
				npc.setRunning();
				npc.setDisplayEffect(MAHUM_EFFECT_NONE);
				break;
			}
			case "reset_full_bottle_prize":
			{
				npc.getVariables().remove("FULL_BARREL_REWARDING_PLAYER");
				break;
			}
			case "return_from_fire":
			{
				if (npc.isMonster() && !npc.isDead())
				{
					npc.asMonster().returnHome();
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
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon, Skill skill)
	{
		if ((npc.getId() == CHEF) && (npc.getVariables().getInt(BUSY_STATE) == 0))
		{
			if (npc.getVariables().getInt(INVUL_REMOVE_TIMER_STARTED) == 0)
			{
				startQuestTimer(CHEF_REMOVE_INVUL, 180000, npc, attacker);
				startQuestTimer(CHEF_DISABLE_REWARD, 60000, npc, null);
				npc.getVariables().set(INVUL_REMOVE_TIMER_STARTED, 1);
			}
			startQuestTimer(CHEF_HEAL_PLAYER, 1000, npc, attacker);
			startQuestTimer(CHEF_SET_INVUL, 60000, npc, null);
			npc.getVariables().set(BUSY_STATE, 1);
		}
		else if (ArrayUtil.contains(SQUAD_LEADERS, npc.getId()))
		{
			handlePreAttackMotion(npc);
		}
	}
	
	@Override
	public void onFactionCall(Npc npc, Npc caller, Player attacker, boolean isSummon)
	{
		handlePreAttackMotion(npc);
	}
	
	@Override
	public String onEventReceived(String eventName, Npc sender, Npc receiver, WorldObject reference)
	{
		switch (eventName)
		{
			case "SCE_DINNER_CHECK":
			{
				if (receiver.getId() == FIRE)
				{
					receiver.setDisplayEffect(FIRE_EFFECT_BURN);
					final Npc stove = addSpawn(STOVE, receiver.getX(), receiver.getY(), receiver.getZ() + 100, 0, false, 0);
					stove.setSummoner(receiver);
					startQuestTimer("notify_dinner", 2000, receiver, null); // @SCE_DINNER_EAT
					sender.broadcastSay(ChatType.NPC_GENERAL, CHEF_FSTRINGS[getRandom(2)], 1250);
				}
				break;
			}
			case "SCE_CAMPFIRE_START":
			{
				if (receiver.isRandomWalkingEnabled() && !receiver.isDead() && (receiver.getAI().getIntention() != Intention.ATTACK) && ArrayUtil.contains(SQUAD_LEADERS, receiver.getId()))
				{
					receiver.setRandomWalking(false); // Moving to fire - i_ai0 = 1
					receiver.setRunning();
					final Location loc = sender.getPointInRange(100, 200);
					loc.setHeading(receiver.getHeading());
					receiver.stopMove(null);
					receiver.getVariables().set(DESTINATION_X, loc.getX());
					receiver.getVariables().set(DESTINATION_Y, loc.getY());
					receiver.getAI().setIntention(Intention.MOVE_TO, loc);
				}
				break;
			}
			case "SCE_CAMPFIRE_END":
			{
				if ((receiver.getId() == STOVE) && (receiver.getSummoner() == sender))
				{
					receiver.deleteMe();
				}
				else if ((receiver.getAI().getIntention() != Intention.ATTACK) && ArrayUtil.contains(SQUAD_LEADERS, receiver.getId()))
				{
					receiver.setRandomWalking(true);
					receiver.getVariables().remove(BUSY_STATE);
					receiver.setRHandId(THS_WEAPON);
					startQuestTimer("return_from_fire", 3000, receiver, null);
				}
				break;
			}
			case "SCE_DINNER_EAT":
			{
				if (!receiver.isDead() && (receiver.getAI().getIntention() != Intention.ATTACK) && (receiver.getVariables().getInt(BUSY_STATE, 0) == 0) && ArrayUtil.contains(SQUAD_LEADERS, receiver.getId()))
				{
					if (!receiver.isRandomWalkingEnabled()) // i_ai0 == 1
					{
						receiver.setRHandId(THS_WEAPON);
					}
					receiver.setRandomWalking(false); // Moving to fire - i_ai0 = 1
					receiver.getVariables().set(BUSY_STATE, 1); // Eating - i_ai3 = 1
					receiver.setRunning();
					receiver.broadcastSay(ChatType.NPC_GENERAL, (getRandom(3) < 1) ? NpcStringId.LOOKS_DELICIOUS : NpcStringId.LET_S_GO_EAT, 1250);
					final Location loc = sender.getPointInRange(100, 200);
					loc.setHeading(receiver.getHeading());
					receiver.stopMove(null);
					receiver.getVariables().set(DESTINATION_X, loc.getX());
					receiver.getVariables().set(DESTINATION_Y, loc.getY());
					receiver.getAI().setIntention(Intention.MOVE_TO, loc);
				}
				break;
			}
			case "SCE_SOUP_FAILURE":
			{
				if (ArrayUtil.contains(SQUAD_LEADERS, receiver.getId()))
				{
					receiver.getVariables().set("FULL_BARREL_REWARDING_PLAYER", reference.getObjectId()); // TODO: Use it in 289 quest
					startQuestTimer("reset_full_bottle_prize", 180000, receiver, null);
				}
				break;
			}
			default:
			{
				break;
			}
		}
		return super.onEventReceived(eventName, sender, receiver, reference);
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		if (npc.isMonster() && (npc.getVariables().getInt("REWARD_TIME_GONE") == 0))
		{
			npc.asMonster().dropItem(killer, 15492, 1);
		}
		cancelQuestTimer(CHEF_REMOVE_INVUL, npc, null);
		cancelQuestTimer(CHEF_DISABLE_REWARD, npc, null);
		cancelQuestTimer(CHEF_HEAL_PLAYER, npc, null);
		cancelQuestTimer(CHEF_SET_INVUL, npc, null);
		
		npc.setDisplayEffect(MAHUM_EFFECT_NONE);
	}
	
	@Override
	public void onMoveFinished(Npc npc)
	{
		// NPC moves to fire
		if (!npc.isRandomWalkingEnabled() && (npc.getX() == npc.getVariables().getInt(DESTINATION_X)) && (npc.getY() == npc.getVariables().getInt(DESTINATION_Y)))
		{
			npc.setRHandId(OHS_WEAPON);
			startQuestTimer("fire_arrived", 3000, npc, null);
		}
	}
	
	@Override
	public void onNodeArrived(Npc npc)
	{
		npc.broadcastEvent("SCE_DINNER_CHECK", 300, null);
	}
	
	@Override
	public void onSkillSee(Npc npc, Player caster, Skill skill, List<WorldObject> targets, boolean isSummon)
	{
		if ((npc.getId() == STOVE) && (skill.getId() == 9075) && targets.contains(npc))
		{
			npc.doCast(SkillData.getInstance().getSkill(6688, 1));
			npc.broadcastEvent("SCE_SOUP_FAILURE", 600, caster);
		}
	}
	
	@Override
	public void onSpawn(Npc npc)
	{
		if (npc.getId() == CHEF)
		{
			npc.setInvul(false);
		}
		else if (npc.getId() == FIRE)
		{
			cancelQuestTimer("fire", npc, null);
			startQuestTimer("fire", 1000, npc, null);
		}
		else if (ArrayUtil.contains(SQUAD_LEADERS, npc.getId()))
		{
			npc.setDisplayEffect(3);
			npc.setRandomWalking(true);
		}
	}
	
	@Override
	public void onSpellFinished(Npc npc, Player player, Skill skill)
	{
		if ((skill != null) && (skill.getId() == 6330))
		{
			healPlayer(npc, player);
		}
	}
	
	private void healPlayer(Npc npc, Player player)
	{
		if ((player != null) && !player.isDead() && (npc.getVariables().getInt(INVUL_REMOVE_TIMER_STARTED) != 1) && ((npc.getAI().getIntention() == Intention.ATTACK) || (npc.getAI().getIntention() == Intention.CAST)))
		{
			npc.setTarget(player);
			npc.doCast(SkillData.getInstance().getSkill(6330, 1));
		}
		else
		{
			cancelQuestTimer(CHEF_SET_INVUL, npc, null);
			npc.getVariables().remove(BUSY_STATE);
			npc.getVariables().remove(INVUL_REMOVE_TIMER_STARTED);
			npc.setWalking();
		}
	}
	
	private void handlePreAttackMotion(Npc attacked)
	{
		cancelQuestTimer(REMOVE_EFFECTS, attacked, null);
		attacked.getVariables().remove(BUSY_STATE);
		attacked.setRandomWalking(true);
		attacked.setDisplayEffect(MAHUM_EFFECT_NONE);
		if (attacked.getRightHandItem() == OHS_WEAPON)
		{
			attacked.setRHandId(THS_WEAPON);
		}
		// TODO: Check about i_quest0
	}
	
	public static void main(String[] args)
	{
		new SelMahumSquad();
	}
}