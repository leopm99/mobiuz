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
package instances.MithrilMine;

import com.l2journey.gameserver.ai.Intention;
import com.l2journey.gameserver.managers.InstanceManager;
import com.l2journey.gameserver.model.Location;
import com.l2journey.gameserver.model.World;
import com.l2journey.gameserver.model.actor.Attackable;
import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.instancezone.InstanceWorld;
import com.l2journey.gameserver.model.quest.QuestState;
import com.l2journey.gameserver.model.skill.holders.SkillHolder;
import com.l2journey.gameserver.network.NpcStringId;
import com.l2journey.gameserver.network.enums.ChatType;

import instances.AbstractInstance;
import quests.Q10284_AcquisitionOfDivineSword.Q10284_AcquisitionOfDivineSword;

/**
 * Mithril Mine instance zone.
 * @author Adry_85
 */
public class MithrilMine extends AbstractInstance
{
	private static final String COUNT = "count";

	// NPCs
	private static final int KEGOR = 18846;
	private static final int MITHRIL_MILLIPEDE = 22766;
	private static final int KRUN = 32653;
	private static final int TARUN = 32654;
	// Item
	private static final int COLD_RESISTANCE_POTION = 15514;
	// Skill
	private static final SkillHolder BLESS_OF_SWORD = new SkillHolder(6286, 1);
	// Location
	private static final Location START_LOC = new Location(186852, -173492, -3763, 0, 0);
	private static final Location EXIT_LOC = new Location(178823, -184303, -347, 0, 0);
	private static final Location[] MOB_SPAWNS =
	{
		new Location(185216, -184112, -3308, -15396),
		new Location(185456, -184240, -3308, -19668),
		new Location(185712, -184384, -3308, -26696),
		new Location(185920, -184544, -3308, -32544),
		new Location(185664, -184720, -3308, 27892)
	};
	// Misc
	private static final int TEMPLATE_ID = 138;
	
	private MithrilMine()
	{
		addFirstTalkId(KEGOR);
		addKillId(KEGOR, MITHRIL_MILLIPEDE);
		addStartNpc(TARUN, KRUN);
		addTalkId(TARUN, KRUN, KEGOR);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		final InstanceWorld world = InstanceManager.getInstance().getWorld(npc);
		
		switch (event)
		{
			case "BUFF":
			{
				if ((player != null) && npc.isInsideRadius3D(player, 1000) && npc.isScriptValue(1) && !player.isDead())
				{
					npc.setTarget(player);
					npc.doCast(BLESS_OF_SWORD.getSkill());
				}
				startQuestTimer("BUFF", 30000, npc, player);
				break;
			}
			case "TIMER":
			{
				if (world != null)
				{
					for (Location loc : MOB_SPAWNS)
					{
						final Attackable spawnedMob = addSpawn(MITHRIL_MILLIPEDE, loc, false, 0, false, world.getInstanceId()).asAttackable();
						spawnedMob.setScriptValue(1);
						spawnedMob.setRunning();
						spawnedMob.getAI().setIntention(Intention.ATTACK, npc);
						spawnedMob.addDamageHate(npc, 0, 999999);
					}
				}
				break;
			}
			case "FINISH":
			{
				World.getInstance().forEachVisibleObject(npc, Creature.class, knownChar ->
				{
					if (knownChar.getId() == KEGOR)
					{
						final Npc kegor = knownChar.asNpc();
						kegor.setScriptValue(2);
						kegor.setWalking();
						kegor.setTarget(player);
						kegor.getAI().setIntention(Intention.FOLLOW, player);
						kegor.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.I_CAN_FINALLY_TAKE_A_BREATHER_BY_THE_WAY_WHO_ARE_YOU_HMM_I_THINK_I_KNOW_WHO_SENT_YOU);
					}
				});
				InstanceManager.getInstance().getInstance(world.getInstanceId()).setDuration(3000);
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
	public String onFirstTalk(Npc npc, Player player)
	{
		final QuestState qs = player.getQuestState(Q10284_AcquisitionOfDivineSword.class.getSimpleName());
		if ((qs != null))
		{
			if (qs.isMemoState(2))
			{
				return npc.isScriptValue(0) ? "18846.html" : "18846-01.html";
			}
			else if (qs.isMemoState(3))
			{
				final InstanceWorld world = InstanceManager.getInstance().getPlayerWorld(player);
				world.removeAllowed(player);
				player.setInstanceId(0);
				player.teleToLocation(EXIT_LOC, 0);
				giveAdena(player, 296425, true);
				addExpAndSp(player, 921805, 82230);
				qs.exitQuest(false, true);
				return "18846-03.html";
			}
		}
		return super.onFirstTalk(npc, player);
	}
	
	@Override
	public void onKill(Npc npc, Player player, boolean isSummon)
	{
		final InstanceWorld world = InstanceManager.getInstance().getWorld(npc);
		if (npc.getId() == KEGOR)
		{
			npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.HOW_COULD_I_FALL_IN_A_PLACE_LIKE_THIS);
			InstanceManager.getInstance().getInstance(world.getInstanceId()).setDuration(1000);
		}
		else
		{
			if (npc.isScriptValue(1))
			{
				final int count = world.getParameters().getInt(COUNT, 0);
				world.setParameter(COUNT, count + 1);
			}
			
			if (world.getParameters().getInt(COUNT, 0) >= 5)
			{
				final QuestState qs = player.getQuestState(Q10284_AcquisitionOfDivineSword.class.getSimpleName());
				if ((qs != null) && qs.isMemoState(2))
				{
					cancelQuestTimer("BUFF", npc, player);
					qs.setMemoState(3);
					qs.setCond(6, true);
					startQuestTimer("FINISH", 3000, npc, player);
				}
			}
		}
	}
	
	@Override
	public String onTalk(Npc npc, Player talker)
	{
		switch (npc.getId())
		{
			case TARUN, KRUN:
			{
				final QuestState qs = talker.getQuestState(Q10284_AcquisitionOfDivineSword.class.getSimpleName());
				if ((qs != null) && qs.isMemoState(2))
				{
					if (!hasQuestItems(talker, COLD_RESISTANCE_POTION))
					{
						giveItems(talker, COLD_RESISTANCE_POTION, 1);
					}
					qs.setCond(4, true);
					enterInstance(talker, TEMPLATE_ID);
				}
				break;
			}
			case KEGOR:
			{
				final QuestState qs = talker.getQuestState(Q10284_AcquisitionOfDivineSword.class.getSimpleName());
				if ((qs != null) && qs.isMemoState(2) && hasQuestItems(talker, COLD_RESISTANCE_POTION) && npc.isScriptValue(0))
				{
					takeItems(talker, COLD_RESISTANCE_POTION, -1);
					qs.setCond(5, true);
					npc.setScriptValue(1);
					startQuestTimer("TIMER", 3000, npc, talker);
					startQuestTimer("BUFF", 3500, npc, talker);
					return "18846-02.html";
				}
				break;
			}
			default:
			{
				break;
			}
		}
		return super.onTalk(npc, talker);
	}
	
	@Override
	public void onEnterInstance(Player player, InstanceWorld world, boolean firstEntrance)
	{
		if (firstEntrance)
		{
			world.addAllowed(player);
		}
		teleportPlayer(player, START_LOC, world.getInstanceId(), false);
	}
	
	public static void main(String[] args)
	{
		new MithrilMine();
	}
}
