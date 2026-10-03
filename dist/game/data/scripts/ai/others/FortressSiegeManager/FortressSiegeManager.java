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
package ai.others.FortressSiegeManager;

import com.l2journey.gameserver.managers.FortSiegeManager;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.clan.Clan;
import com.l2journey.gameserver.model.clan.ClanAccess;
import com.l2journey.gameserver.model.siege.Castle;
import com.l2journey.gameserver.model.siege.Fort;
import com.l2journey.gameserver.network.SystemMessageId;
import com.l2journey.gameserver.network.serverpackets.NpcHtmlMessage;
import com.l2journey.gameserver.network.serverpackets.SystemMessage;

import ai.AbstractNpcAI;

/**
 * Fortress Siege Manager AI.
 * @author St3eT
 */
public class FortressSiegeManager extends AbstractNpcAI
{
	// NPCs
	private static final int[] MANAGERS =
	{
		35659, // Shanty Fortress
		35690, // Southern Fortress
		35728, // Hive Fortress
		35759, // Valley Fortress
		35797, // Ivory Fortress
		35828, // Narsell Fortress
		35859, // Bayou Fortress
		35897, // White Sands Fortress
		35928, // Borderland Fortress
		35966, // Swamp Fortress
		36004, // Archaic Fortress
		36035, // Floran Fortress
		36073, // Cloud Mountain
		36111, // Tanor Fortress
		36142, // Dragonspine Fortress
		36173, // Antharas's Fortress
		36211, // Western Fortress
		36249, // Hunter's Fortress
		36287, // Aaru Fortress
		36318, // Demon Fortress
		36356, // Monastic Fortress
	};
	
	private FortressSiegeManager()
	{
		addStartNpc(MANAGERS);
		addTalkId(MANAGERS);
		addFirstTalkId(MANAGERS);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		switch (event)
		{
			case "FortressSiegeManager-11.html", "FortressSiegeManager-13.html", "FortressSiegeManager-14.html", "FortressSiegeManager-15.html", "FortressSiegeManager-16.html":
			{
				return htmltext = event;
			}
			case "register":
			{
				if (player.getClan() == null)
				{
					htmltext = "FortressSiegeManager-02.html";
				}
				else
				{
					final Clan clan = player.getClan();
					final Fort fortress = npc.getFort();
					final Castle castle = npc.getCastle();
					if (clan.getFortId() == fortress.getResidenceId())
					{
						final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
						html.setHtml(getHtm(player, "FortressSiegeManager-12.html"));
						html.replace("%clanName%", fortress.getOwnerClan().getName());
						return html.getHtml();
					}
					else if (!player.hasAccess(ClanAccess.CASTLE_SIEGE))
					{
						htmltext = "FortressSiegeManager-10.html";
					}
					else if (clan.getLevel() < FortSiegeManager.getInstance().getSiegeClanMinLevel())
					{
						htmltext = "FortressSiegeManager-04.html";
					}
					else if ((player.getClan().getCastleId() == castle.getResidenceId()) && (fortress.getFortState() == 2))
					{
						htmltext = "FortressSiegeManager-18.html";
					}
					else if ((clan.getCastleId() != 0) && (clan.getCastleId() != castle.getResidenceId()) && FortSiegeManager.getInstance().canRegisterJustTerritory())
					{
						htmltext = "FortressSiegeManager-17.html";
					}
					else if ((fortress.getTimeTillRebelArmy() > 0) && (fortress.getTimeTillRebelArmy() <= 7200))
					{
						htmltext = "FortressSiegeManager-19.html";
					}
					else
					{
						switch (npc.getFort().getSiege().addAttacker(player, true))
						{
							case 1:
							{
								htmltext = "FortressSiegeManager-03.html";
								break;
							}
							case 2:
							{
								htmltext = "FortressSiegeManager-07.html";
								break;
							}
							case 3:
							{
								htmltext = "FortressSiegeManager-06.html";
								break;
							}
							case 4:
							{
								final SystemMessage sm = new SystemMessage(SystemMessageId.YOUR_CLAN_HAS_BEEN_REGISTERED_TO_S1_S_FORTRESS_BATTLE);
								sm.addString(npc.getFort().getName());
								player.sendPacket(sm);
								htmltext = "FortressSiegeManager-05.html";
								break;
							}
						}
					}
				}
				break;
			}
			case "cancel":
			{
				if (player.getClan() == null)
				{
					htmltext = "FortressSiegeManager-02.html";
				}
				else
				{
					final Clan clan = player.getClan();
					final Fort fortress = npc.getFort();
					if (clan.getFortId() == fortress.getResidenceId())
					{
						final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
						html.setHtml(getHtm(player, "FortressSiegeManager-12.html"));
						html.replace("%clanName%", fortress.getOwnerClan().getName());
						return html.getHtml();
					}
					else if (!player.hasAccess(ClanAccess.CASTLE_SIEGE))
					{
						htmltext = "FortressSiegeManager-10.html";
					}
					else if (!FortSiegeManager.getInstance().checkIsRegistered(clan, fortress.getResidenceId()))
					{
						htmltext = "FortressSiegeManager-09.html";
					}
					else
					{
						fortress.getSiege().removeAttacker(player.getClan());
						htmltext = "FortressSiegeManager-08.html";
					}
				}
				break;
			}
			case "warInfo":
			{
				htmltext = npc.getFort().getSiege().getAttackerClans().isEmpty() ? "FortressSiegeManager-20.html" : "FortressSiegeManager-21.html";
			}
		}
		return htmltext;
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		final Fort fortress = npc.getFort();
		final int fortOwner = fortress.getOwnerClan() == null ? 0 : fortress.getOwnerClan().getId();
		if (fortOwner == 0)
		{
			return "FortressSiegeManager.html";
		}
		final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
		html.setHtml(getHtm(player, "FortressSiegeManager-01.html"));
		html.replace("%clanName%", fortress.getOwnerClan().getName());
		html.replace("%objectId%", npc.getObjectId());
		return html.getHtml();
	}
	
	public static void main(String[] args)
	{
		new FortressSiegeManager();
	}
}