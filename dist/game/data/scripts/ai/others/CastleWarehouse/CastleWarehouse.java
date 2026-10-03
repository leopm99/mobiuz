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
package ai.others.CastleWarehouse;

import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.clan.Clan;

import ai.AbstractNpcAI;

/**
 * Castle Warehouse Keeper AI.
 * @author malyelfik
 */
public class CastleWarehouse extends AbstractNpcAI
{
	// NPCs
	private static final int[] NPCS =
	{
		35099, // Warehouse Keeper (Gludio)
		35141, // Warehouse Keeper (Dion)
		35183, // Warehouse Keeper (Giran)
		35225, // Warehouse Keeper (Oren)
		35273, // Warehouse Keeper (Aden)
		35315, // Warehouse Keeper (Inadril)
		35362, // Warehouse Keeper (Goddard)
		35508, // Warehouse Keeper (Rune)
		35554, // Warehouse Keeper (Schuttgart)
	};
	// Items
	private static final int BLOOD_OATH = 9910;
	private static final int BLOOD_ALLIANCE = 9911;
	
	private CastleWarehouse()
	{
		addStartNpc(NPCS);
		addTalkId(NPCS);
		addFirstTalkId(NPCS);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		switch (event)
		{
			case "warehouse-01.html", "warehouse-02.html", "warehouse-03.html":
			{
				break;
			}
			case "warehouse-04.html":
			{
				htmltext = !npc.isMyLord(player) ? "warehouse-no.html" : getHtm(player, "warehouse-04.html").replace("%blood%", Integer.toString(player.getClan().getBloodAllianceCount()));
				break;
			}
			case "Receive":
			{
				final Clan clan = player.getClan();
				if (!npc.isMyLord(player))
				{
					htmltext = "warehouse-no.html";
				}
				else if (clan.getBloodAllianceCount() == 0)
				{
					htmltext = "warehouse-05.html";
				}
				else
				{
					giveItems(player, BLOOD_ALLIANCE, clan.getBloodAllianceCount());
					clan.resetBloodAllianceCount();
					htmltext = "warehouse-06.html";
				}
				break;
			}
			case "Exchange":
			{
				if (!npc.isMyLord(player))
				{
					htmltext = "warehouse-no.html";
				}
				else if (!hasQuestItems(player, BLOOD_ALLIANCE))
				{
					htmltext = "warehouse-08.html";
				}
				else
				{
					takeItems(player, BLOOD_ALLIANCE, 1);
					giveItems(player, BLOOD_OATH, 30);
					htmltext = "warehouse-07.html";
				}
				break;
			}
			default:
			{
				htmltext = null;
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return "warehouse-01.html";
	}
	
	public static void main(String[] args)
	{
		new CastleWarehouse();
	}
}