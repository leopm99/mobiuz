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
package ai.others.CastleMercenaryManager;

import java.util.StringTokenizer;

import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.instance.Merchant;
import com.l2journey.gameserver.model.clan.ClanAccess;
import com.l2journey.gameserver.model.sevensigns.SevenSigns;
import com.l2journey.gameserver.model.siege.Castle;
import com.l2journey.gameserver.network.serverpackets.NpcHtmlMessage;

import ai.AbstractNpcAI;

/**
 * Castle Mercenary Manager AI.
 * @author malyelfik
 */
public class CastleMercenaryManager extends AbstractNpcAI
{
	// NPCs
	private static final int[] NPCS =
	{
		35102, // Greenspan
		35144, // Sanford
		35186, // Arvid
		35228, // Morrison
		35276, // Eldon
		35318, // Solinus
		35365, // Rowell
		35511, // Gompus
		35557, // Kendrew
	};
	
	private CastleMercenaryManager()
	{
		addStartNpc(NPCS);
		addTalkId(NPCS);
		addFirstTalkId(NPCS);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		final StringTokenizer st = new StringTokenizer(event, " ");
		switch (st.nextToken())
		{
			case "limit":
			{
				final Castle castle = npc.getCastle();
				final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
				if (castle.getName().equals("aden"))
				{
					html.setHtml(getHtm(player, "mercmanager-aden-limit.html"));
				}
				else if (castle.getName().equals("rune"))
				{
					html.setHtml(getHtm(player, "mercmanager-rune-limit.html"));
				}
				else
				{
					html.setHtml(getHtm(player, "mercmanager-limit.html"));
				}
				html.replace("%feud_name%", String.valueOf(1001000 + castle.getResidenceId()));
				player.sendPacket(html);
				break;
			}
			case "buy":
			{
				if (SevenSigns.getInstance().isSealValidationPeriod())
				{
					final int listId = Integer.parseInt(npc.getId() + st.nextToken());
					((Merchant) npc).showBuyWindow(player, listId, false); // NOTE: Not affected by Castle Taxes, baseTax is 20% (done in merchant buylists)
				}
				else
				{
					htmltext = "mercmanager-ssq.html";
				}
				break;
			}
			case "main":
			{
				htmltext = onFirstTalk(npc, player);
				break;
			}
			case "mercmanager-01.html":
			{
				htmltext = event;
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
		final String htmltext;
		if (player.isGM() || ((player.getClanId() == npc.getCastle().getOwnerId()) && player.hasAccess(ClanAccess.CASTLE_MERCENARIES)))
		{
			if (npc.getCastle().getSiege().isInProgress())
			{
				htmltext = "mercmanager-siege.html";
			}
			else
			{
				switch (SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE))
				{
					case SevenSigns.CABAL_DUSK:
					{
						htmltext = "mercmanager-dusk.html";
						break;
					}
					case SevenSigns.CABAL_DAWN:
					{
						htmltext = "mercmanager-dawn.html";
						break;
					}
					default:
					{
						htmltext = "mercmanager.html";
					}
				}
			}
		}
		else
		{
			htmltext = "mercmanager-no.html";
		}
		return htmltext;
	}
	
	public static void main(String[] args)
	{
		new CastleMercenaryManager();
	}
}