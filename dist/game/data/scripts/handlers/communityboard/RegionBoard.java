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
package handlers.communityboard;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.l2journey.commons.database.DatabaseFactory;
import com.l2journey.commons.util.StringUtil;
import com.l2journey.gameserver.cache.HtmCache;
import com.l2journey.gameserver.data.sql.ClanTable;
import com.l2journey.gameserver.handler.CommunityBoardHandler;
import com.l2journey.gameserver.handler.IWriteBoardHandler;
import com.l2journey.gameserver.managers.CastleManager;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.clan.Clan;
import com.l2journey.gameserver.model.html.icons.TerritoryIcons;
import com.l2journey.gameserver.model.siege.Castle;

/**
 * Region board.
 * @author Zoey76, Skache
 */
public class RegionBoard implements IWriteBoardHandler
{
	private static final String CLAN_HALL_QUERY = "SELECT id, name, ownerId, location FROM clanhall WHERE location = ?";
	
	// @formatter:on
	private static final String[] COMMANDS =
	{
		"_bbsloc"
	};
	
	private static class ClanHall
	{
		private final String name;
		private final int ownerId;
		
		public ClanHall(String name, int ownerId)
		{
			this.name = name;
			this.ownerId = ownerId;
		}
		
		public String getName()
		{
			return name;
		}
		
		public int getOwnerId()
		{
			return ownerId;
		}
	}
	
	@Override
	public String[] getCommunityBoardCommands()
	{
		return COMMANDS;
	}
	
	@Override
	public boolean parseCommunityBoardCommand(String command, Player player)
	{
		if (command.equals("_bbsloc"))
		{
			CommunityBoardHandler.getInstance().addBypass(player, "Region", command);
			
			final String list = HtmCache.getInstance().getHtm(player, "data/html/CommunityBoard/region_list.html");
			final StringBuilder sb = new StringBuilder();
			final List<Castle> castles = CastleManager.getInstance().getCastles();
			
			for (int i = 0; i < castles.size(); i++)
			{
				final Castle castle = castles.get(i);
				final Clan clan = ClanTable.getInstance().getClan(castle.getOwnerId());
				String icon = "<img src=\"" + TerritoryIcons.getIconByCastleName(castle.getName()) + "\" width=32 height=32 align=\"center\">";
				String link = list.replace("%region_id%", String.valueOf(i)).replace("%region_icon%", icon).replace("%region_name%", castle.getName()).replace("%region_owning_clan%", (clan != null ? "<a action=\"bypass _bbsclan_clanhome;" + clan.getId() + "\">" + clan.getName() + "</a>" : "<font color=\"4F4F4F\">NPC</font>")).replace("%region_owning_clan_alliance%", ((clan != null) && (clan.getAllyName() != null) ? clan.getAllyName() : "<font color=\"4F4F4F\">None</font>")).replace("%region_tax_rate%", (castle.getTaxRate() * 100) + "%");
				sb.append(link);
			}
			
			String html = HtmCache.getInstance().getHtm(player, "data/html/CommunityBoard/region.html");
			html = html.replace("%region_list%", sb.toString());
			CommunityBoardHandler.separateAndSend(html, player);
		}
		else if (command.startsWith("_bbsloc;"))
		{
			CommunityBoardHandler.getInstance().addBypass(player, "Region>", command);
			
			final String id = command.replace("_bbsloc;", "");
			if (!StringUtil.isNumeric(id))
			{
				final String loggedCommand = command;
				LOG.warning(() -> RegionBoard.class.getSimpleName() + ": " + player + " sent an invalid region bypass " + loggedCommand + "!");
				return false;
			}
			
			final int regionId = Integer.parseInt(id);
			final List<Castle> castles = CastleManager.getInstance().getCastles();
			if ((regionId < 0) || (regionId >= castles.size()))
			{
				final int loggedRegionId = regionId;
				LOG.warning(() -> RegionBoard.class.getSimpleName() + ": " + player + " requested invalid region ID " + loggedRegionId + "!");
				return false;
			}
			
			final Castle castle = castles.get(regionId);
			final Clan clan = ClanTable.getInstance().getClan(castle.getOwnerId());
			String html = HtmCache.getInstance().getHtm(player, "data/html/CommunityBoard/region_show.html");
			html = html.replace("%regionName%", castle.getName()).replace("%tax%", (castle.getTaxRate() * 100) + "%").replace("%lord%", clan != null ? clan.getLeaderName() : "NPC").replace("%clanName%", (clan != null ? "<a action=\"bypass _bbsclan_clanhome;" + clan.getId() + "\">" + clan.getName() + "</a>" : "NPC")).replace("%allyName%", ((clan != null) && (clan.getAllyName() != null) ? clan.getAllyName() : "None")).replace("%siegeDate%", new SimpleDateFormat("dd-MM-yyyy HH:mm").format(castle.getSiegeDate().getTimeInMillis()));
			
			final StringBuilder hallsList = new StringBuilder();
			hallsList.append("<br><br><center><table width=730 align=left bgcolor=2E2E2E><tr><td width=10></td><td width=200 align=\"center\"><font name=\"hs12\" name=\"CreditTextSmall\" color=ae9977>Clan Hall Name</font></td><td width=200 align=\"center\"><font name=\"hs12\" name=\"CreditTextSmall\" color=ae9977>Owning Clan</font></td><td width=200 align=\"center\"><font name=\"hs12\" name=\"CreditTextSmall\" color=ae9977>Clan Leader Name</font></td><td width=5></td></tr></table><br1><img src=\"L2UI.Squaregray\" width=720 height=1></center><br1>");
			final Map<Integer, ClanHall> clanHalls = getClanHallsByLocation(castle.getName());
			if (clanHalls.isEmpty())
			{
				hallsList.append("<table width=730><tr><td align=\"left\">There are no clan halls in this region.</td></tr></table>");
			}
			else
			{
				for (ClanHall hall : clanHalls.values())
				{
					Clan hallOwner = ClanTable.getInstance().getClan(hall.getOwnerId());
					hallsList.append("<center><table width=730><tr><td width=10></td><td width=200 align=\"center\">").append(hall.getName()).append("</td><td width=200 align=\"center\">").append(hallOwner != null ? "<a action=\"bypass _bbsclan_clanhome;" + hallOwner.getId() + "\">" + hallOwner.getName() + "</a>" : "<font color=\"4F4F4F\">-</font>").append("</td><td width=200 align=\"center\">").append(hallOwner != null ? hallOwner.getLeaderName() : "<font color=\"4F4F4F\">-</font>").append("</td><td width=5></td></tr></table><br1><img src=\"L2UI.Squaregray\" width=720 height=1></center><br1>");
				}
			}
			
			html = html.replace("%hallsList%", hallsList.toString());
			CommunityBoardHandler.separateAndSend(html, player);
		}
		
		return true;
	}
	
	private Map<Integer, ClanHall> getClanHallsByLocation(String location)
	{
		final Map<Integer, ClanHall> clanHalls = new HashMap<>();
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(CLAN_HALL_QUERY))
		{
			ps.setString(1, location);
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					String name = rs.getString("name");
					int ownerId = rs.getInt("ownerId");
					ClanHall hall = new ClanHall(name, ownerId);
					clanHalls.put(rs.getInt("id"), hall);
				}
			}
		}
		catch (SQLException e)
		{
			LOG.warning(RegionBoard.class.getSimpleName() + ": Could not fetch clan halls data. " + e.getMessage());
		}
		
		return clanHalls;
	}
	
	@Override
	public boolean writeCommunityBoardCommand(Player player, String arg1, String arg2, String arg3, String arg4, String arg5)
	{
		// TODO: Implement.
		return false;
	}
}
