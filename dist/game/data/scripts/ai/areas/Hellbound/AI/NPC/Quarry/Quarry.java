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
package ai.areas.Hellbound.AI.NPC.Quarry;

import com.l2journey.Config;
import com.l2journey.gameserver.ai.Intention;
import com.l2journey.gameserver.managers.ZoneManager;
import com.l2journey.gameserver.model.actor.Attackable;
import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.instance.QuestGuard;
import com.l2journey.gameserver.model.item.holders.ItemChanceHolder;
import com.l2journey.gameserver.model.zone.ZoneType;
import com.l2journey.gameserver.network.NpcStringId;
import com.l2journey.gameserver.network.enums.ChatType;

import ai.AbstractNpcAI;
import ai.areas.Hellbound.HellboundEngine;

/**
 * Quarry AI.
 * @author DS, GKR
 */
public class Quarry extends AbstractNpcAI
{
	// NPCs
	private static final int SLAVE = 32299;
	// Items
	protected static final ItemChanceHolder[] DROP_LIST =
	{
		new ItemChanceHolder(9628, 261), // Leonard
		new ItemChanceHolder(9630, 175), // Orichalcum
		new ItemChanceHolder(9629, 145), // Adamantine
		new ItemChanceHolder(1876, 6667), // Mithril ore
		new ItemChanceHolder(1877, 1333), // Adamantine nugget
		new ItemChanceHolder(1874, 2222), // Oriharukon ore
	};
	// Zone
	private static final int ZONE = 40107;
	// Misc
	private static final int TRUST = 50;
	
	public Quarry()
	{
		addSpawnId(SLAVE);
		addFirstTalkId(SLAVE);
		addStartNpc(SLAVE);
		addTalkId(SLAVE);
		addKillId(SLAVE);
		addEnterZoneId(ZONE);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		switch (event)
		{
			case "FollowMe":
			{
				npc.getAI().setIntention(Intention.FOLLOW, player);
				npc.setTarget(player);
				npc.setAutoAttackable(true);
				npc.setRHandId(9136);
				npc.setWalking();
				
				if (getQuestTimer("TIME_LIMIT", npc, null) == null)
				{
					startQuestTimer("TIME_LIMIT", 900000, npc, null); // 15 min limit for save
				}
				htmltext = "32299-02.htm";
				break;
			}
			case "TIME_LIMIT":
			{
				for (ZoneType zone : ZoneManager.getInstance().getZones(npc))
				{
					if (zone.getId() == 40108)
					{
						npc.setTarget(null);
						npc.getAI().setIntention(Intention.ACTIVE);
						npc.setAutoAttackable(false);
						npc.setRHandId(0);
						npc.teleToLocation(npc.getSpawn().getLocation());
						return null;
					}
				}
				npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.HUN_HUNGRY);
				npc.doDie(npc);
				break;
			}
			case "DECAY":
			{
				if ((npc != null) && !npc.isDead())
				{
					if (npc.getTarget().isPlayer())
					{
						for (ItemChanceHolder item : DROP_LIST)
						{
							if (getRandom(10000) < item.getChance())
							{
								npc.dropItem(npc.getTarget().asPlayer(), item.getId(), (int) (item.getCount() * Config.RATE_QUEST_DROP));
								break;
							}
						}
					}
					npc.setAutoAttackable(false);
					npc.getSpawn().decreaseCount(npc);
					HellboundEngine.getInstance().updateTrust(TRUST, true);
					npc.deleteMe();
				}
			}
			default:
			{
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public void onSpawn(Npc npc)
	{
		npc.setAutoAttackable(false);
		if (npc instanceof QuestGuard questGuard)
		{
			questGuard.setPassive(true);
		}
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		if (HellboundEngine.getInstance().getLevel() != 5)
		{
			return "32299.htm";
		}
		return "32299-01.htm";
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		npc.setAutoAttackable(false);
	}
	
	@Override
	public void onEnterZone(Creature creature, ZoneType zone)
	{
		if (creature.isAttackable())
		{
			final Attackable npc = creature.asAttackable();
			if ((npc.getId() == SLAVE) && !npc.isDead() && !npc.isDecayed() && (npc.getAI().getIntention() == Intention.FOLLOW) && (HellboundEngine.getInstance().getLevel() == 5))
			{
				startQuestTimer("DECAY", 1000, npc, null);
				npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.THANK_YOU_FOR_THE_RESCUE_IT_S_A_SMALL_GIFT);
			}
		}
	}
}