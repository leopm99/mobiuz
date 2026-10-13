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
package ai.others.FameManager;

import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.clan.Clan;
import com.l2journey.gameserver.network.SystemMessageId;

import ai.AbstractNpcAI;

/**
 * Fame Manager AI.
 * @author St3eT
 */
public class FameManager extends AbstractNpcAI
{
	private static final String HTML_N_01 = "-01.html";

	// Npc
	private static final int[] FAME_MANAGER =
	{
		36479, // Rapidus
		36480, // Scipio
	};
	// Misc
	private static final int MIN_LEVEL = 40;
	private static final int DECREASE_COST = 5000;
	private static final int REPUTATION_COST = 1000;
	private static final int MIN_CLAN_LEVEL = 5;
	private static final int CLASS_LEVEL = 2;
	
	private FameManager()
	{
		addStartNpc(FAME_MANAGER);
		addTalkId(FAME_MANAGER);
		addFirstTalkId(FAME_MANAGER);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		switch (event)
		{
			case "36479.html", "36479-02.html", "36479-07.html", "36480.html", "36480-02.html", "36480-07.html":
			{
				htmltext = event;
				break;
			}
			case "decreasePk":
			{
				if (player.getPkKills() > 0)
				{
					if ((player.getFame() >= DECREASE_COST) && (player.getLevel() >= MIN_LEVEL) && (player.getPlayerClass().level() >= CLASS_LEVEL))
					{
						player.setFame(player.getFame() - DECREASE_COST);
						player.setPkKills(player.getPkKills() - 1);
						player.updateUserInfo();
						htmltext = npc.getId() + "-06.html";
					}
					else
					{
						htmltext = npc.getId() + HTML_N_01;
					}
				}
				else
				{
					htmltext = npc.getId() + "-05.html";
				}
				break;
			}
			case "clanRep":
			{
				final Clan clan = player.getClan();
				if ((clan != null) && (clan.getLevel() >= MIN_CLAN_LEVEL))
				{
					if ((player.getFame() >= REPUTATION_COST) && (player.getLevel() >= MIN_LEVEL) && (player.getPlayerClass().level() >= CLASS_LEVEL))
					{
						player.setFame(player.getFame() - REPUTATION_COST);
						clan.addReputationScore(50);
						player.updateUserInfo();
						player.sendPacket(SystemMessageId.YOU_HAVE_ACQUIRED_50_CLAN_FAME_POINTS);
						htmltext = npc.getId() + "-04.html";
					}
					else
					{
						htmltext = npc.getId() + HTML_N_01;
					}
				}
				else
				{
					htmltext = npc.getId() + "-03.html";
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
	public String onFirstTalk(Npc npc, Player player)
	{
		return ((player.getFame() > 0) && (player.getLevel() >= MIN_LEVEL) && (player.getPlayerClass().level() >= CLASS_LEVEL)) ? npc.getId() + ".html" : npc.getId() + HTML_N_01;
	}
	
	public static void main(String[] args)
	{
		new FameManager();
	}
}