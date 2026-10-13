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

import com.l2journey.gameserver.ai.Intention;
import com.l2journey.gameserver.data.SpawnTable;
import com.l2journey.gameserver.model.Location;
import com.l2journey.gameserver.model.Spawn;
import com.l2journey.gameserver.model.WorldObject;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.network.NpcStringId;
import com.l2journey.gameserver.network.enums.ChatType;
import com.l2journey.gameserver.util.ArrayUtil;

import ai.AbstractNpcAI;

/**
 * Sel Mahum Training Ground AI for drill groups.
 * @author GKR
 */
public class SelMahumDrill extends AbstractNpcAI
{
	private static final String SOCIAL_ACTION_REMAINED_COUNT = "SOCIAL_ACTION_REMAINED_COUNT";
	private static final String SOCIAL_ACTION_NEXT_INDEX = "SOCIAL_ACTION_NEXT_INDEX";
	private static final String DO_SOCIAL_ACTION = "do_social_action";
	private static final String BUSY_STATE = "BUSY_STATE";

	private static final int[] MAHUM_CHIEFS =
	{
		22775, // Sel Mahum Drill Sergeant
		22776, // Sel Mahum Training Officer
		22778, // Sel Mahum Drill Sergeant
	};
	private static final int[] MAHUM_SOLDIERS =
	{
		22780, // Sel Mahum Recruit
		22782, // Sel Mahum Recruit
		22783, // Sel Mahum Soldier
		22784, // Sel Mahum Recruit
		22785, // Sel Mahum Soldier
	};
	private static final int[] CHIEF_SOCIAL_ACTIONS =
	{
		1,
		4,
		5,
		7
	};
	private static final Actions[] SOLDIER_SOCIAL_ACTIONS =
	{
		Actions.SCE_TRAINING_ACTION_A,
		Actions.SCE_TRAINING_ACTION_B,
		Actions.SCE_TRAINING_ACTION_C,
		Actions.SCE_TRAINING_ACTION_D
	};
	private static final NpcStringId[] CHIEF_FSTRINGS =
	{
		NpcStringId.HOW_DARE_YOU_ATTACK_MY_RECRUITS,
		NpcStringId.WHO_IS_DISRUPTING_THE_ORDER
	};
	private static final NpcStringId[] SOLDIER_FSTRINGS =
	{
		NpcStringId.THE_DRILLMASTER_IS_DEAD,
		NpcStringId.LINE_UP_THE_RANKS
	};
	// Chiefs event broadcast range
	private static final int TRAINING_RANGE = 1500;
	
	private enum Actions
	{
		SCE_TRAINING_ACTION_A(4, -1, 2, 2333),
		SCE_TRAINING_ACTION_B(1, -1, 2, 4333),
		SCE_TRAINING_ACTION_C(6, 5, 4, 1000),
		SCE_TRAINING_ACTION_D(7, -1, 2, 1000);
		
		private final int _socialActionId;
		private final int _altSocialActionId;
		private final int _repeatCount;
		private final int _repeatInterval;
		
		private Actions(int socialActionId, int altSocialActionId, int repeatCount, int repeatInterval)
		{
			_socialActionId = socialActionId;
			_altSocialActionId = altSocialActionId;
			_repeatCount = repeatCount;
			_repeatInterval = repeatInterval;
		}
		
		protected int getSocialActionId()
		{
			return _socialActionId;
		}
		
		protected int getAltSocialActionId()
		{
			return _altSocialActionId;
		}
		
		protected int getRepeatCount()
		{
			return _repeatCount;
		}
		
		protected int getRepeatInterval()
		{
			return _repeatInterval;
		}
	}
	
	private SelMahumDrill()
	{
		addAttackId(MAHUM_CHIEFS);
		addAttackId(MAHUM_SOLDIERS);
		addKillId(MAHUM_CHIEFS);
		addEventReceivedId(MAHUM_CHIEFS);
		addEventReceivedId(MAHUM_SOLDIERS);
		addSpawnId(MAHUM_CHIEFS);
		addSpawnId(MAHUM_SOLDIERS);
		// Start global return home timer
		startQuestTimer("return_home", 120000, null, null, true);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case DO_SOCIAL_ACTION:
			{
				if ((npc != null) && !npc.isDead())
				{
					if (ArrayUtil.contains(MAHUM_CHIEFS, npc.getId()))
					{
						if ((npc.getVariables().getInt(BUSY_STATE) == 0) && (npc.getAI().getIntention() == Intention.ACTIVE) && npc.staysInSpawnLoc())
						{
							final int idx = getRandom(6);
							if (idx <= (CHIEF_SOCIAL_ACTIONS.length - 1))
							{
								npc.broadcastSocialAction(CHIEF_SOCIAL_ACTIONS[idx]);
								npc.getVariables().set(SOCIAL_ACTION_NEXT_INDEX, idx); // Pass social action index to soldiers via script value
								npc.broadcastEvent(DO_SOCIAL_ACTION, TRAINING_RANGE, null);
							}
						}
						
						startQuestTimer(DO_SOCIAL_ACTION, 15000, npc, null);
					}
					else if (ArrayUtil.contains(MAHUM_SOLDIERS, npc.getId()))
					{
						handleSocialAction(npc, SOLDIER_SOCIAL_ACTIONS[npc.getVariables().getInt(SOCIAL_ACTION_NEXT_INDEX)], false);
					}
				}
				break;
			}
			case "reset_busy_state":
			{
				if (npc != null)
				{
					npc.getVariables().remove(BUSY_STATE);
					npc.disableCoreAI(false);
				}
				break;
			}
			case "return_home":
			{
				for (int npcId : MAHUM_SOLDIERS)
				{
					for (Spawn npcSpawn : SpawnTable.getInstance().getSpawns(npcId))
					{
						final Npc soldier = npcSpawn.getLastSpawn();
						if ((soldier != null) && !soldier.isDead() && (npcSpawn.getName() != null) && npcSpawn.getName().startsWith("smtg_drill_group") && !soldier.staysInSpawnLoc() && ((soldier.getAI().getIntention() == Intention.ACTIVE) || (soldier.getAI().getIntention() == Intention.IDLE)))
						{
							soldier.setHeading(npcSpawn.getHeading());
							soldier.teleToLocation(npcSpawn.getLocation(), false);
						}
					}
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
		if ((getRandom(10) < 1) && (ArrayUtil.contains(MAHUM_SOLDIERS, npc.getId())))
		{
			npc.broadcastEvent("ATTACKED", 1000, null);
		}
	}
	
	@Override
	public String onEventReceived(String eventName, Npc sender, Npc receiver, WorldObject reference)
	{
		if ((receiver != null) && !receiver.isDead() && receiver.isInMySpawnGroup(sender))
		{
			switch (eventName)
			{
				case DO_SOCIAL_ACTION:
				{
					if (ArrayUtil.contains(MAHUM_SOLDIERS, receiver.getId()))
					{
						final int actionIndex = sender.getVariables().getInt(SOCIAL_ACTION_NEXT_INDEX);
						receiver.getVariables().set(SOCIAL_ACTION_NEXT_INDEX, actionIndex);
						handleSocialAction(receiver, SOLDIER_SOCIAL_ACTIONS[actionIndex], true);
					}
					break;
				}
				case "CHIEF_DIED":
				{
					if (!receiver.isAttackable())
					{
						return null;
					}
					if (ArrayUtil.contains(MAHUM_SOLDIERS, receiver.getId()))
					{
						if (getRandom(4) < 1)
						{
							receiver.broadcastSay(ChatType.NPC_GENERAL, SOLDIER_FSTRINGS[getRandom(2)]);
						}
						if (receiver.canBeAttacked())
						{
							receiver.asAttackable().clearAggroList();
						}
						receiver.disableCoreAI(true);
						receiver.getVariables().set(BUSY_STATE, 1);
						receiver.setRunning();
						receiver.getAI().setIntention(Intention.MOVE_TO, new Location((receiver.getX() + getRandom(-800, 800)), (receiver.getY() + getRandom(-800, 800)), receiver.getZ(), receiver.getHeading()));
						startQuestTimer("reset_busy_state", 5000, receiver, null);
					}
					break;
				}
				case "ATTACKED":
				{
					if (ArrayUtil.contains(MAHUM_CHIEFS, receiver.getId()))
					{
						receiver.broadcastSay(ChatType.NPC_GENERAL, CHIEF_FSTRINGS[getRandom(2)]);
					}
					break;
				}
				default:
				{
					break;
				}
			}
		}
		return super.onEventReceived(eventName, sender, receiver, reference);
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		npc.broadcastEvent("CHIEF_DIED", TRAINING_RANGE, null);
	}
	
	@Override
	public void onSpawn(Npc npc)
	{
		if (ArrayUtil.contains(MAHUM_CHIEFS, npc.getId()))
		{
			cancelQuestTimer(DO_SOCIAL_ACTION, npc, null);
			startQuestTimer(DO_SOCIAL_ACTION, 15000, npc, null);
		}
		
		else if ((getRandom(18) < 1) && ArrayUtil.contains(MAHUM_SOLDIERS, npc.getId()))
		{
			npc.getVariables().set("SOCIAL_ACTION_ALT_BEHAVIOR", 1);
		}
		
		// Restore AI handling by core
		npc.disableCoreAI(false);
	}
	
	private void handleSocialAction(Npc npc, Actions action, boolean firstCall)
	{
		if ((npc.getVariables().getInt(BUSY_STATE) != 0) || (npc.getAI().getIntention() != Intention.ACTIVE) || !npc.staysInSpawnLoc())
		{
			return;
		}
		
		final int socialActionId = (npc.getVariables().getInt("SOCIAL_ACTION_ALT_BEHAVIOR") == 0) ? action.getSocialActionId() : action.getAltSocialActionId();
		if (socialActionId < 0)
		{
			return;
		}
		
		if (firstCall)
		{
			npc.getVariables().set(SOCIAL_ACTION_REMAINED_COUNT, action.getRepeatCount());
		}
		
		npc.broadcastSocialAction(socialActionId);
		
		final int remainedCount = npc.getVariables().getInt(SOCIAL_ACTION_REMAINED_COUNT);
		if (remainedCount > 0)
		{
			npc.getVariables().set(SOCIAL_ACTION_REMAINED_COUNT, (remainedCount - 1));
			startQuestTimer(DO_SOCIAL_ACTION, action.getRepeatInterval(), npc, null);
		}
	}
	
	public static void main(String[] args)
	{
		new SelMahumDrill();
	}
}