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
package ai.others.ClanTrader;

import com.l2journey.Config;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.clan.ClanAccess;
import com.l2journey.gameserver.network.SystemMessageId;
import com.l2journey.gameserver.network.serverpackets.SystemMessage;

import ai.AbstractNpcAI;

/**
 * Clan Trader AI.
 * @author St3eT
 */
public class ClanTrader extends AbstractNpcAI
{
	// Npc
	private static final int[] CLAN_TRADER =
	{
		32024, // Mulia
		32025, // Ilia
	};
	// Items
	private static final int BLOOD_ALLIANCE = 9911; // Blood Alliance
	private static final int BLOOD_ALLIANCE_COUNT = 1; // Blood Alliance Count
	private static final int BLOOD_OATH = 9910; // Blood Oath
	private static final int BLOOD_OATH_COUNT = 10; // Blood Oath Count
	private static final int KNIGHTS_EPAULETTE = 9912; // Knight's Epaulette
	private static final int KNIGHTS_EPAULETTE_COUNT = 100; // Knight's Epaulette Count
	
	private ClanTrader()
	{
		addStartNpc(CLAN_TRADER);
		addTalkId(CLAN_TRADER);
		addFirstTalkId(CLAN_TRADER);
	}
	
	private String giveReputation(Npc npc, Player player, int count, int itemId, int itemCount)
	{
		if (getQuestItemsCount(player, itemId) >= itemCount)
		{
			takeItems(player, itemId, itemCount);
			player.getClan().addReputationScore(count);
			
			final SystemMessage sm = new SystemMessage(SystemMessageId.YOUR_CLAN_HAS_ADDED_1S_POINTS_TO_ITS_CLAN_REPUTATION_SCORE);
			sm.addInt(count);
			player.sendPacket(sm);
			return npc.getId() + "-04.html";
		}
		return npc.getId() + "-03.html";
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		switch (event)
		{
			case "32024.html", "32024-02.html", "32025.html", "32025-02.html":
			{
				htmltext = event;
				break;
			}
			case "repinfo":
			{
				htmltext = (player.getClan().getLevel() > 4) ? npc.getId() + "-02.html" : npc.getId() + "-05.html";
				break;
			}
			case "exchange-ba":
			{
				htmltext = giveReputation(npc, player, Config.BLOODALLIANCE_POINTS, BLOOD_ALLIANCE, BLOOD_ALLIANCE_COUNT);
				break;
			}
			case "exchange-bo":
			{
				htmltext = giveReputation(npc, player, Config.BLOODOATH_POINTS, BLOOD_OATH, BLOOD_OATH_COUNT);
				break;
			}
			case "exchange-ke":
			{
				htmltext = giveReputation(npc, player, Config.KNIGHTSEPAULETTE_POINTS, KNIGHTS_EPAULETTE, KNIGHTS_EPAULETTE_COUNT);
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
	public String onFirstTalk(Npc npc, Player player)
	{
		if (player.isClanLeader() || player.hasAccess(ClanAccess.MEMBER_FAME))
		{
			return npc.getId() + ".html";
		}
		return npc.getId() + "-01.html";
	}
	
	public static void main(String[] args)
	{
		new ClanTrader();
	}
}