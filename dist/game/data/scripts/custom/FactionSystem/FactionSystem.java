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
package custom.FactionSystem;

import com.l2journey.EventsConfig;
import com.l2journey.gameserver.model.World;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.appearance.PlayerAppearance;
import com.l2journey.gameserver.network.enums.ChatType;
import com.l2journey.gameserver.network.serverpackets.NpcHtmlMessage;

import ai.AbstractNpcAI;

/**
 * @author Mobius
 */
public class FactionSystem extends AbstractNpcAI
{
	private static final String PH_NAME = "%name%";

	// NPCs
	private static final int MANAGER = 500;
	// Other
	private static final String[] TEXTS =
	{
		EventsConfig.FACTION_GOOD_TEAM_NAME + " or " + EventsConfig.FACTION_EVIL_TEAM_NAME + "?",
		"Select your faction!",
		"The choice is yours!"
	};
	
	private FactionSystem()
	{
		addSpawnId(MANAGER);
		addStartNpc(MANAGER);
		addTalkId(MANAGER);
		addFirstTalkId(MANAGER);
		
		if (EventsConfig.FACTION_SYSTEM_ENABLED)
		{
			addSpawn(MANAGER, EventsConfig.FACTION_MANAGER_LOCATION, false, 0);
		}
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "selectGoodFaction":
			{
				if (EventsConfig.FACTION_BALANCE_ONLINE_PLAYERS && (World.getInstance().getAllGoodPlayers().size() >= (World.getInstance().getAllEvilPlayers().size() + EventsConfig.FACTION_BALANCE_PLAYER_EXCEED_LIMIT)))
				{
					final String htmltext = null;
					final NpcHtmlMessage packet = new NpcHtmlMessage(npc.getObjectId());
					packet.setHtml(getHtm(player, "onlinelimit.html"));
					packet.replace(PH_NAME, player.getName());
					packet.replace("%more%", EventsConfig.FACTION_GOOD_TEAM_NAME);
					packet.replace("%less%", EventsConfig.FACTION_EVIL_TEAM_NAME);
					player.sendPacket(packet);
					return htmltext;
				}
				if (EventsConfig.FACTION_AUTO_NOBLESS)
				{
					player.setNoble(true);
				}
				player.setGood();
				final PlayerAppearance appearance = player.getAppearance();
				appearance.setNameColor(EventsConfig.FACTION_GOOD_NAME_COLOR);
				appearance.setTitleColor(EventsConfig.FACTION_GOOD_NAME_COLOR);
				player.setTitle(EventsConfig.FACTION_GOOD_TEAM_NAME);
				player.sendMessage("You are now fighting for the " + EventsConfig.FACTION_GOOD_TEAM_NAME + " faction.");
				player.teleToLocation(EventsConfig.FACTION_GOOD_BASE_LOCATION);
				broadcastMessageToFaction(EventsConfig.FACTION_GOOD_TEAM_NAME, EventsConfig.FACTION_GOOD_TEAM_NAME + " faction grows stronger with the arrival of " + player.getName() + ".");
				World.addFactionPlayerToWorld(player);
				break;
			}
			case "selectEvilFaction":
			{
				if (EventsConfig.FACTION_BALANCE_ONLINE_PLAYERS && (World.getInstance().getAllEvilPlayers().size() >= (World.getInstance().getAllGoodPlayers().size() + EventsConfig.FACTION_BALANCE_PLAYER_EXCEED_LIMIT)))
				{
					final String htmltext = null;
					final NpcHtmlMessage packet = new NpcHtmlMessage(npc.getObjectId());
					packet.setHtml(getHtm(player, "onlinelimit.html"));
					packet.replace(PH_NAME, player.getName());
					packet.replace("%more%", EventsConfig.FACTION_EVIL_TEAM_NAME);
					packet.replace("%less%", EventsConfig.FACTION_GOOD_TEAM_NAME);
					player.sendPacket(packet);
					return htmltext;
				}
				if (EventsConfig.FACTION_AUTO_NOBLESS)
				{
					player.setNoble(true);
				}
				player.setEvil();
				final PlayerAppearance appearance = player.getAppearance();
				appearance.setNameColor(EventsConfig.FACTION_EVIL_NAME_COLOR);
				appearance.setTitleColor(EventsConfig.FACTION_EVIL_NAME_COLOR);
				player.setTitle(EventsConfig.FACTION_EVIL_TEAM_NAME);
				player.sendMessage("You are now fighting for the " + EventsConfig.FACTION_EVIL_TEAM_NAME + " faction.");
				player.teleToLocation(EventsConfig.FACTION_EVIL_BASE_LOCATION);
				broadcastMessageToFaction(EventsConfig.FACTION_EVIL_TEAM_NAME, EventsConfig.FACTION_EVIL_TEAM_NAME + " faction grows stronger with the arrival of " + player.getName() + ".");
				World.addFactionPlayerToWorld(player);
				break;
			}
			case "SPEAK":
			{
				if (npc != null)
				{
					npc.broadcastSay(ChatType.NPC_GENERAL, getRandomEntry(TEXTS), 1500);
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
	public String onFirstTalk(Npc npc, Player player)
	{
		final String htmltext = null;
		final NpcHtmlMessage packet = new NpcHtmlMessage(npc.getObjectId());
		packet.setHtml(getHtm(player, "manager.html"));
		packet.replace(PH_NAME, player.getName());
		packet.replace("%good%", EventsConfig.FACTION_GOOD_TEAM_NAME);
		packet.replace("%evil%", EventsConfig.FACTION_EVIL_TEAM_NAME);
		player.sendPacket(packet);
		return htmltext;
	}
	
	@Override
	public void onSpawn(Npc npc)
	{
		if (npc.getId() == MANAGER)
		{
			startQuestTimer("SPEAK", 10000, npc, null, true);
		}
	}
	
	private void broadcastMessageToFaction(String factionName, String message)
	{
		if (factionName.equals(EventsConfig.FACTION_GOOD_TEAM_NAME))
		{
			for (Player player : World.getInstance().getAllGoodPlayers())
			{
				player.sendMessage(message);
			}
		}
		else
		{
			for (Player player : World.getInstance().getAllEvilPlayers())
			{
				player.sendMessage(message);
			}
		}
	}
	
	public static void main(String[] args)
	{
		new FactionSystem();
	}
}
