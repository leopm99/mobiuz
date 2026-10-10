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
package ai.areas.Hellbound.AI.NPC.Natives;

import com.l2journey.gameserver.data.xml.DoorData;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.instance.Door;
import com.l2journey.gameserver.network.NpcStringId;
import com.l2journey.gameserver.network.enums.ChatType;

import ai.AbstractNpcAI;
import ai.areas.Hellbound.HellboundEngine;

/**
 * Natives AI.
 * @author DS, GKR
 */
public class Natives extends AbstractNpcAI
{
	// NPCs
	private static final int NATIVE = 32362;
	private static final int INSURGENT = 32363;
	private static final int TRAITOR = 32364;
	private static final int INCASTLE = 32357;
	// Items
	private static final int MARK_OF_BETRAYAL = 9676; // Mark of Betrayal
	private static final int BADGES = 9674; // Darion's Badge
	// Misc
	private static final int[] DOORS =
	{
		19250003,
		19250004,
	};
	
	public Natives()
	{
		addFirstTalkId(NATIVE);
		addFirstTalkId(INSURGENT);
		addFirstTalkId(INCASTLE);
		addStartNpc(TRAITOR);
		addStartNpc(INCASTLE);
		addTalkId(TRAITOR);
		addTalkId(INCASTLE);
		addSpawnId(NATIVE);
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		String htmltext = null;
		final int hellboundLevel = HellboundEngine.getInstance().getLevel();
		switch (npc.getId())
		{
			case NATIVE:
			{
				htmltext = hellboundLevel > 5 ? "32362-01.htm" : "32362.htm";
				break;
			}
			case INSURGENT:
			{
				htmltext = hellboundLevel > 5 ? "32363-01.htm" : "32363.htm";
				break;
			}
			case INCASTLE:
			{
				if (hellboundLevel < 9)
				{
					htmltext = "32357-01a.htm";
				}
				else if (hellboundLevel == 9)
				{
					htmltext = npc.isBusy() ? "32357-02.htm" : "32357-01.htm";
				}
				else
				{
					htmltext = "32357-01b.htm";
				}
				break;
			}
			default:
			{
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		if (npc.getId() == TRAITOR)
		{
			if (event.equalsIgnoreCase("open_door"))
			{
				if (getQuestItemsCount(player, MARK_OF_BETRAYAL) >= 10)
				{
					takeItems(player, MARK_OF_BETRAYAL, 10);
					npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.ALRIGHT_NOW_LEODAS_IS_YOURS);
					HellboundEngine.getInstance().updateTrust(-50, true);
					
					for (int doorId : DOORS)
					{
						final Door door = DoorData.getInstance().getDoor(doorId);
						if (door != null)
						{
							door.openMe();
						}
					}
					
					cancelQuestTimers("close_doors");
					startQuestTimer("close_doors", 1800000, npc, player); // 30 min
				}
				else if (hasQuestItems(player, MARK_OF_BETRAYAL))
				{
					htmltext = "32364-01.htm";
				}
				else
				{
					htmltext = "32364-02.htm";
				}
			}
			else if (event.equalsIgnoreCase("close_doors"))
			{
				for (int doorId : DOORS)
				{
					final Door door = DoorData.getInstance().getDoor(doorId);
					if (door != null)
					{
						door.closeMe();
					}
				}
			}
		}
		else if ((npc.getId() == NATIVE) && event.equalsIgnoreCase("hungry_death"))
		{
			npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.HUN_HUNGRY);
			npc.doDie(null);
		}
		else if (npc.getId() == INCASTLE)
		{
			if (event.equalsIgnoreCase("FreeSlaves"))
			{
				if (getQuestItemsCount(player, BADGES) >= 5)
				{
					takeItems(player, BADGES, 5);
					npc.setBusy(true); // Prevent Native from take items more, than once
					HellboundEngine.getInstance().updateTrust(100, true);
					htmltext = "32357-02.htm";
					startQuestTimer("delete_me", 3000, npc, null);
				}
				else
				{
					htmltext = "32357-02a.htm";
				}
			}
			else if (event.equalsIgnoreCase("delete_me"))
			{
				npc.setBusy(false); // TODO: Does it really need?
				npc.getSpawn().decreaseCount(npc);
				npc.deleteMe();
			}
		}
		return htmltext;
	}
	
	@Override
	public void onSpawn(Npc npc)
	{
		if ((npc.getId() == NATIVE) && (HellboundEngine.getInstance().getLevel() < 6))
		{
			startQuestTimer("hungry_death", 600000, npc, null);
		}
	}
}