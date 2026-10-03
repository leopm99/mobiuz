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
 */
package handlers.bypasshandlers;

import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import com.l2journey.EventsConfig;
import com.l2journey.gameserver.cache.HtmCache;
import com.l2journey.gameserver.handler.CommunityBoardHandler;
import com.l2journey.gameserver.handler.IBypassHandler;
import com.l2journey.gameserver.managers.HitmanManager;
import com.l2journey.gameserver.managers.HitmanManager.HitmanTarget;
import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.actor.Player;

/**
 * Bypass handler for the Hitman Event system.
 * @author L2Journey, KingHanker
 */
public class Hitman implements IBypassHandler
{
	private static final String[] COMMANDS =
	{
		"hitman",
		"hitman_list",
		"hitman_mycontracts",
		"hitman_place",
		"hitman_placeconfirm",
		"hitman_cancel",
		"hitman_cancelconfirm"
	};
	
	private static final String HTML_FOLDER = "data/html/mods/Hitman/";
	
	@Override
	public boolean useBypass(String command, Player player, Creature target)
	{
		if (!EventsConfig.HITMAN_ENABLED)
		{
			player.sendMessage("Hitman Event is disabled.");
			return false;
		}
		
		final StringTokenizer st = new StringTokenizer(command, " ");
		final String action = st.nextToken();
		
		switch (action)
		{
			case "hitman", "hitman_list":
				showTargetList(player, st.hasMoreTokens() ? Integer.parseInt(st.nextToken()) : 1);
				break;
			case "hitman_mycontracts":
				showMyContracts(player);
				break;
			case "hitman_place":
				showPlaceForm(player);
				break;
			case "hitman_placeconfirm":
				if (st.countTokens() >= 3)
				{
					final String targetName = st.nextToken();
					final String currencyName = st.nextToken();
					final long bounty = Long.parseLong(st.nextToken());
					placeContract(player, targetName, currencyName, bounty);
				}
				break;
			case "hitman_cancel":
				if (st.hasMoreTokens())
				{
					showCancelConfirm(player, st.nextToken());
				}
				break;
			case "hitman_cancelconfirm":
				if (st.hasMoreTokens())
				{
					cancelContract(player, st.nextToken());
				}
				break;
		}
		
		return true;
	}
	
	/**
	 * Show the list of all active targets.
	 * @param player the player
	 * @param page the page number
	 */
	private void showTargetList(Player player, int page)
	{
		final HitmanManager manager = HitmanManager.getInstance();
		final Map<Integer, HitmanTarget> targets = manager.getTargets();
		
		String html = HtmCache.getInstance().getHtm(player, HTML_FOLDER + "list.htm");
		if (html == null)
		{
			player.sendMessage("Hitman HTML not found.");
			return;
		}
		
		final int maxPerPage = EventsConfig.HITMAN_MAX_PER_PAGE;
		final int totalTargets = targets.size();
		final int totalPages = Math.max(1, (int) Math.ceil((double) totalTargets / maxPerPage));
		final int currentPage = Math.min(Math.max(1, page), totalPages);
		final int startIndex = (currentPage - 1) * maxPerPage;
		
		final StringBuilder sb = new StringBuilder();
		int count = 0;
		int displayed = 0;
		
		for (HitmanTarget target : targets.values())
		{
			if (target.isPendingDelete())
			{
				continue;
			}
			
			if ((count >= startIndex) && (displayed < maxPerPage))
			{
				final boolean online = target.isOnline();
				final String statusColor = online ? "00FF00" : "FF0000";
				final String status = online ? "Online" : "Offline";
				
				sb.append("<tr>");
				sb.append("<td width=180 align=center><font color=\"LEVEL\">").append(target.getTargetName()).append("</font></td>");
				sb.append("<td width=160 align=center>").append(HitmanManager.formatNumber(target.getBounty())).append("</td>");
				sb.append("<td width=140 align=center>").append(manager.getCurrencyName(target.getItemId())).append("</td>");
				sb.append("<td width=140 align=center><font color=\"").append(statusColor).append("\">").append(status).append("</font></td>");
				sb.append("</tr>");
				displayed++;
			}
			count++;
		}
		
		if (displayed == 0)
		{
			sb.append("<tr><td colspan=4 align=center><font color=\"999999\">No active bounties at this time.</font></td></tr>");
		}
		
		// Build pagination
		final StringBuilder pagination = new StringBuilder();
		if (totalPages > 1)
		{
			pagination.append("<table width=300><tr>");
			if (currentPage > 1)
			{
				pagination.append("<td><button value=\"Prev\" action=\"bypass -h hitman_list ").append(currentPage - 1).append("\" width=50 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"/></td>");
			}
			pagination.append("<td align=center>Page ").append(currentPage).append(" / ").append(totalPages).append("</td>");
			if (currentPage < totalPages)
			{
				pagination.append("<td><button value=\"Next\" action=\"bypass -h hitman_list ").append(currentPage + 1).append("\" width=50 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"/></td>");
			}
			pagination.append("</tr></table>");
		}
		
		html = html.replace("%targetList%", sb.toString());
		html = html.replace("%pagination%", pagination.toString());
		html = html.replace("%totalTargets%", String.valueOf(totalTargets));
		
		CommunityBoardHandler.separateAndSend(html, player);
	}
	
	/**
	 * Show the player's active contracts.
	 * @param player the player
	 */
	private void showMyContracts(Player player)
	{
		final HitmanManager manager = HitmanManager.getInstance();
		
		String html = HtmCache.getInstance().getHtm(player, HTML_FOLDER + "mycontracts.htm");
		if (html == null)
		{
			player.sendMessage("Hitman HTML not found.");
			return;
		}
		
		final StringBuilder sb = new StringBuilder();
		final List<Integer> playerTargets = player.getHitmanTargets();
		int count = 0;
		
		if ((playerTargets != null) && !playerTargets.isEmpty())
		{
			for (int targetId : playerTargets)
			{
				final HitmanTarget target = manager.getTarget(targetId);
				if (target == null)
				{
					continue;
				}
				
				final boolean online = target.isOnline();
				final String statusColor = online ? "00FF00" : "FF0000";
				final String status = online ? "Online" : "Offline";
				
				sb.append("<tr>");
				sb.append("<td width=120 align=center><font color=\"LEVEL\">").append(target.getTargetName()).append("</font></td>");
				sb.append("<td width=100 align=center>").append(HitmanManager.formatNumber(target.getBounty())).append("</td>");
				sb.append("<td width=80 align=center>").append(manager.getCurrencyName(target.getItemId())).append("</td>");
				sb.append("<td width=60 align=center><font color=\"").append(statusColor).append("\">").append(status).append("</font></td>");
				sb.append("<td width=60 align=center><button value=\"Cancel\" action=\"bypass -h hitman_cancel ").append(target.getTargetName()).append("\" width=55 height=23 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"/></td>");
				sb.append("</tr>");
				count++;
			}
		}
		
		if (count == 0)
		{
			sb.append("<tr><td colspan=5 align=center><font color=\"999999\">You have no active contracts.</font></td></tr>");
		}
		
		html = html.replace("%contractList%", sb.toString());
		html = html.replace("%contractCount%", count + "/" + EventsConfig.HITMAN_TARGETS_LIMIT);
		
		CommunityBoardHandler.separateAndSend(html, player);
	}
	
	/**
	 * Show the form to place a new contract.
	 * @param player the player
	 */
	private void showPlaceForm(Player player)
	{
		final HitmanManager manager = HitmanManager.getInstance();
		
		String html = HtmCache.getInstance().getHtm(player, HTML_FOLDER + "place.htm");
		if (html == null)
		{
			player.sendMessage("Hitman HTML not found.");
			return;
		}
		
		// Build currency options
		final StringBuilder currencyOptions = new StringBuilder();
		for (Map.Entry<String, Integer> entry : manager.getCurrencyMap().entrySet())
		{
			currencyOptions.append(entry.getKey()).append(";");
		}
		if (currencyOptions.length() > 0)
		{
			currencyOptions.setLength(currencyOptions.length() - 1); // Remove last semicolon
		}
		
		html = html.replace("%currencyOptions%", currencyOptions.toString());
		html = html.replace("%minBounty%", HitmanManager.formatNumber(EventsConfig.HITMAN_MIN_BOUNTY));
		
		CommunityBoardHandler.separateAndSend(html, player);
	}
	
	/**
	 * Place a contract on a target.
	 * @param player the player
	 * @param targetName the target name
	 * @param currencyName the currency name
	 * @param bounty the bounty amount
	 */
	private void placeContract(Player player, String targetName, String currencyName, long bounty)
	{
		final HitmanManager manager = HitmanManager.getInstance();
		final Integer itemId = manager.getCurrencyId(currencyName);
		
		if (itemId == null)
		{
			player.sendMessage("Invalid currency selected.");
			showPlaceForm(player);
			return;
		}
		
		if (manager.putHitOn(player, targetName, bounty, itemId))
		{
			showMyContracts(player);
		}
		else
		{
			showPlaceForm(player);
		}
	}
	
	/**
	 * Show cancel confirmation.
	 * @param player the player
	 * @param targetName the target name
	 */
	private void showCancelConfirm(Player player, String targetName)
	{
		String html = HtmCache.getInstance().getHtm(player, HTML_FOLDER + "cancelconfirm.htm");
		if (html == null)
		{
			player.sendMessage("Hitman HTML not found.");
			return;
		}
		
		html = html.replace("%targetName%", targetName);
		CommunityBoardHandler.separateAndSend(html, player);
	}
	
	/**
	 * Cancel a contract.
	 * @param player the player
	 * @param targetName the target name
	 */
	private void cancelContract(Player player, String targetName)
	{
		final HitmanManager manager = HitmanManager.getInstance();
		manager.cancelContract(player, targetName);
		showMyContracts(player);
	}
	
	@Override
	public String[] getBypassList()
	{
		return COMMANDS;
	}
}
