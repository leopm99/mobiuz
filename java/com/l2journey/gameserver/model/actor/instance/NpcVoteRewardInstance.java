/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.l2journey.gameserver.model.actor.instance;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.l2journey.Config;
import com.l2journey.commons.threads.ThreadPool;
import com.l2journey.gameserver.data.xml.ItemData;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.templates.NpcTemplate;
import com.l2journey.gameserver.network.serverpackets.NpcHtmlMessage;
import com.l2journey.gameserver.votesystem.Enum.VoteSite;
import com.l2journey.gameserver.votesystem.handler.VoteManager;
import com.l2journey.gameserver.votesystem.handler.VoteHandler;
import com.l2journey.gameserver.votesystem.model.Reward;
import com.l2journey.gameserver.votesystem.util.VoteSiteData;

/**
 * @author l2.topgameserver.net
 */
public class NpcVoteRewardInstance extends Folk
{
	private static final Logger LOGGER = Logger.getLogger(NpcVoteRewardInstance.class.getName());
	private static final Set<Integer> PENDING_PLAYERS = ConcurrentHashMap.newKeySet();
	
	/**
	 * @param template
	 */
	public NpcVoteRewardInstance(NpcTemplate template)
	{
		super(template);
	}
	
	@Override
	public void onBypassFeedback(Player player, String command)
	{
		if ((player == null) || (command == null) || !canInteract(player))
		{
			return;
		}
		if (!command.startsWith("vote_reward "))
		{
			super.onBypassFeedback(player, command);
			return;
		}
		if (!Config.ENABLE_VOTE_SYSTEM || !Config.ENABLE_INDIVIDUAL_VOTE || player.isJailed())
		{
			player.sendMessage("Vote rewards are unavailable.");
			return;
		}
		final int site;
		try
		{
			site = Integer.parseInt(command.substring("vote_reward ".length()).trim());
		}
		catch (NumberFormatException e)
		{
			return;
		}
		if (VoteSite.fromId(site) == null || VoteSiteData.getInstance().getRewards(site).isEmpty())
		{
			return;
		}
		if (!PENDING_PLAYERS.add(player.getObjectId()))
		{
			player.sendMessage("Your votes are already being checked. Please wait.");
			return;
		}
		try
		{
			ThreadPool.execute(() ->
			{
				try
				{
					if (player.isOnline() && !player.isJailed() && canInteract(player))
					{
						VoteManager.getInstance().getReward(player, site);
						if (player.isOnline() && canInteract(player) && (player.getTarget() == this))
						{
							showChatWindow(player, 0);
						}
					}
				}
				catch (RuntimeException e)
				{
					LOGGER.log(Level.WARNING, "Could not process NPC vote reward.", e);
					player.sendMessage("Could not finish checking your vote. Please try again later.");
				}
				finally
				{
					PENDING_PLAYERS.remove(player.getObjectId());
				}
			});
		}
		catch (RuntimeException e)
		{
			PENDING_PLAYERS.remove(player.getObjectId());
			LOGGER.log(Level.WARNING, "Could not schedule NPC vote reward.", e);
			player.sendMessage("Could not start checking your vote. Please try again later.");
		}
	}
	
	@Override
	public void showChatWindow(Player player, int val)
	{
		
		final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		StringBuilder sb = new StringBuilder();
		html.setFile(player, getHtmlPath(this.getTemplate().getId(), 0));
		if (Config.ENABLE_VOTE_SYSTEM && Config.ENABLE_INDIVIDUAL_VOTE)
		{
			for (VoteSite vs : VoteSite.values())
			{
				if (!VoteSiteData.getInstance().isIndividualEnabled(vs.getId()) || !VoteHandler.isConfigured(vs.getId())) { continue; }
				sb.append("<table bgcolor=000000 width=280><tr>");
				sb.append("<td width=42><img src=\"icon.etc_treasure_box_i08\" width=32 height=32></td>");
				sb.append("<td width=220><table width=220>");
				sb.append("<tr><td><table width=220><tr><td width=145>On " + String.format("%s", VoteSiteData.getInstance().getSiteName(vs.getId())) + "</td>");
				if (VoteManager.getInstance().checkIndividualAvailableVote(player, vs.getId()))
				{
					sb.append("<td width=75>" + String.format("<button value=\"Get reward\" action=\"bypass -h npc_%s_vote_reward %s\" height=17 width=64 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">", getObjectId(), vs.getId()) + "</td>");
				}
				else
				{
					sb.append(String.format("<td width=75 align=center><font color=C68E00>%s</font></td>", VoteManager.getInstance().getTimeRemainingWithSampleFormat(player, vs.getId())));
				}
				sb.append("</tr></table></td></tr>");
				sb.append("<tr><td><table width=220><tr>");
				int i = 0;
				for (Reward r : VoteSiteData.getInstance().getRewards(vs.getId()))
				{
					sb.append(String.format("<td width=110 height=32 align=center><font color=BFAF00>%s x%s</font></td>", ItemData.getInstance().getTemplate(r.getItemId()).getName(), r.getItemCount()));
					i++;
					if ((i % 2) == 0)
					{
						sb.append("</tr><tr>");
					}
				}
				sb.append("</tr></table></td></tr></table></td></tr></table><br>");
			}
		}
		else
		{
			sb.append("<table bgcolor=000000 width=280><tr><td><p><font color=#bfb300 >Individual vote system has been disabled for your server owner.</font></p></td></tr></table>");
		}
		html.replace("%everyXtime%", Config.INTERVAL_TO_NEXT_VOTE / (3600 * 1000));
		html.replace("%enablevote%", sb.toString());
		html.replace("%accountName%", player.getName());
		player.sendPacket(html);
	}
	
	@Override
	public String getHtmlPath(int npcId, int val)
	{
		String filename = "";
		if (val == 0)
		{
			filename = "" + npcId;
		}
		else
		{
			filename = npcId + "-" + val;
		}
		
		return "data/html/mods/votesystem/" + filename + ".html";
	}
	
}
