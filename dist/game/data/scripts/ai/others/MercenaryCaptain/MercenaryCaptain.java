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
package ai.others.MercenaryCaptain;

import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import com.l2journey.gameserver.data.xml.MultisellData;
import com.l2journey.gameserver.managers.TerritoryWarManager;
import com.l2journey.gameserver.managers.TerritoryWarManager.Territory;
import com.l2journey.gameserver.managers.TerritoryWarManager.TerritoryNPCSpawn;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.network.NpcStringId;
import com.l2journey.gameserver.network.enums.ChatType;
import com.l2journey.gameserver.network.serverpackets.ExShowDominionRegistry;
import com.l2journey.gameserver.network.serverpackets.NpcHtmlMessage;

import ai.AbstractNpcAI;

/**
 * Mercenary Captain AI.
 * @author malyelfik
 */
public class MercenaryCaptain extends AbstractNpcAI
{
	// NPCs
	private static final Map<Integer, Integer> NPCS = new HashMap<>();
	static
	{
		NPCS.put(36481, 13757); // Mercenary Captain (Gludio)
		NPCS.put(36482, 13758); // Mercenary Captain (Dion)
		NPCS.put(36483, 13759); // Mercenary Captain (Giran)
		NPCS.put(36484, 13760); // Mercenary Captain (Oren)
		NPCS.put(36485, 13761); // Mercenary Captain (Aden)
		NPCS.put(36486, 13762); // Mercenary Captain (Innadril)
		NPCS.put(36487, 13763); // Mercenary Captain (Goddard)
		NPCS.put(36488, 13764); // Mercenary Captain (Rune)
		NPCS.put(36489, 13765); // Mercenary Captain (Schuttgart)
	}
	// Items
	private static final int STRIDER_WIND = 4422;
	private static final int STRIDER_STAR = 4423;
	private static final int STRIDER_TWILIGHT = 4424;
	private static final int GUARDIAN_STRIDER = 14819;
	private static final int ELITE_MERCENARY_CERTIFICATE = 13767;
	private static final int TOP_ELITE_MERCENARY_CERTIFICATE = 13768;
	// Misc
	private static final int DELAY = 3600000; // 1 hour
	private static final int MIN_LEVEL = 40;
	private static final int CLASS_LEVEL = 2;
	
	private MercenaryCaptain()
	{
		for (int id : NPCS.keySet())
		{
			addStartNpc(id);
			addFirstTalkId(id);
			addTalkId(id);
		}
		
		for (Territory terr : TerritoryWarManager.getInstance().getAllTerritories())
		{
			for (TerritoryNPCSpawn spawn : terr.getSpawnList())
			{
				if (NPCS.containsKey(spawn.getId()))
				{
					startQuestTimer("say", DELAY, spawn.getNpc(), null, true);
				}
			}
		}
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		if (player != null)
		{
			final StringTokenizer st = new StringTokenizer(event, " ");
			switch (st.nextToken())
			{
				case "36481-02.html":
				{
					htmltext = event;
					break;
				}
				case "36481-03.html":
				{
					final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
					html.setHtml(getHtm(player, "36481-03.html"));
					html.replace("%strider%", String.valueOf(TerritoryWarManager.MINTWBADGEFORSTRIDERS));
					html.replace("%gstrider%", String.valueOf(TerritoryWarManager.MINTWBADGEFORBIGSTRIDER));
					player.sendPacket(html);
					break;
				}
				case "territory":
				{
					player.sendPacket(new ExShowDominionRegistry(npc.getCastle().getResidenceId(), player));
					break;
				}
				case "strider":
				{
					final String type = st.nextToken();
					final int price = (type.equals("3")) ? TerritoryWarManager.MINTWBADGEFORBIGSTRIDER : TerritoryWarManager.MINTWBADGEFORSTRIDERS;
					final int badgeId = NPCS.get(npc.getId());
					if (getQuestItemsCount(player, badgeId) < price)
					{
						return "36481-07.html";
					}
					
					final int striderId;
					switch (type)
					{
						case "0":
						{
							striderId = STRIDER_WIND;
							break;
						}
						case "1":
						{
							striderId = STRIDER_STAR;
							break;
						}
						case "2":
						{
							striderId = STRIDER_TWILIGHT;
							break;
						}
						case "3":
						{
							striderId = GUARDIAN_STRIDER;
							break;
						}
						default:
						{
							LOGGER.warning(MercenaryCaptain.class.getSimpleName() + ": Unknown strider type: " + type);
							return null;
						}
					}
					takeItems(player, badgeId, price);
					giveItems(player, striderId, 1);
					htmltext = "36481-09.html";
					break;
				}
				case "elite":
				{
					if (!hasQuestItems(player, ELITE_MERCENARY_CERTIFICATE))
					{
						htmltext = "36481-10.html";
					}
					else
					{
						final int listId = 676 + npc.getCastle().getResidenceId();
						MultisellData.getInstance().separateAndSend(listId, player, npc, false);
					}
					break;
				}
				case "top-elite":
				{
					if (!hasQuestItems(player, TOP_ELITE_MERCENARY_CERTIFICATE))
					{
						htmltext = "36481-10.html";
					}
					else
					{
						final int listId = 685 + npc.getCastle().getResidenceId();
						MultisellData.getInstance().separateAndSend(listId, player, npc, false);
					}
					break;
				}
				default:
				{
					break;
				}
			}
		}
		else if (event.equalsIgnoreCase("say") && !npc.isDecayed())
		{
			if (TerritoryWarManager.getInstance().isTWInProgress())
			{
				npc.broadcastSay(ChatType.NPC_SHOUT, NpcStringId.CHARGE_CHARGE_CHARGE);
			}
			else if (getRandom(2) == 0)
			{
				npc.broadcastSay(ChatType.NPC_SHOUT, NpcStringId.COURAGE_AMBITION_PASSION_MERCENARIES_WHO_WANT_TO_REALIZE_THEIR_DREAM_OF_FIGHTING_IN_THE_TERRITORY_WAR_COME_TO_ME_FORTUNE_AND_GLORY_ARE_WAITING_FOR_YOU);
			}
			else
			{
				npc.broadcastSay(ChatType.NPC_SHOUT, NpcStringId.DO_YOU_WISH_TO_FIGHT_ARE_YOU_AFRAID_NO_MATTER_HOW_HARD_YOU_TRY_YOU_HAVE_NOWHERE_TO_RUN_BUT_IF_YOU_FACE_IT_HEAD_ON_OUR_MERCENARY_TROOP_WILL_HELP_YOU_OUT);
			}
		}
		return htmltext;
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		final String htmltext;
		if ((player.getLevel() < MIN_LEVEL) || (player.getPlayerClass().level() < CLASS_LEVEL))
		{
			htmltext = "36481-08.html";
		}
		else if (npc.isMyLord(player))
		{
			htmltext = (npc.getCastle().getSiege().isInProgress() || TerritoryWarManager.getInstance().isTWInProgress()) ? "36481-05.html" : "36481-04.html";
		}
		else
		{
			htmltext = (npc.getCastle().getSiege().isInProgress() || TerritoryWarManager.getInstance().isTWInProgress()) ? "36481-06.html" : npc.getId() + "-01.html";
		}
		return htmltext;
	}
	
	public static void main(String[] args)
	{
		new MercenaryCaptain();
	}
}