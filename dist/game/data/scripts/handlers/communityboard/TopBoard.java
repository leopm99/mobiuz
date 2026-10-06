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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.l2journey.commons.database.DatabaseFactory;
import com.l2journey.gameserver.data.xml.ClassListData;
import com.l2journey.gameserver.handler.CommunityBoardHandler;
import com.l2journey.gameserver.handler.IParseBoardHandler;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.holders.player.ClassInfoHolder;
import com.l2journey.gameserver.model.html.icons.RankingIcons;

/**
 * @author KingHanker
 */
public class TopBoard implements IParseBoardHandler
{
	private static final String LOAD_PVP = "SELECT c.char_name, c.pvpkills, c.classid, cl.clan_name " + "FROM characters c LEFT JOIN clan_data cl ON c.clanid = cl.clan_id " + "WHERE c.accesslevel=0 ORDER BY c.pvpkills DESC LIMIT 25";
	private static final String LOAD_PK = "SELECT c.char_name, c.pkkills, c.classid, cl.clan_name " + "FROM characters c LEFT JOIN clan_data cl ON c.clanid = cl.clan_id " + "WHERE c.accesslevel=0 ORDER BY c.pkkills DESC LIMIT 25";
	
	private static final String[] COMMANDS =
	{
		"_bbstopboard",
		"_bbstopboard;pvp",
		"_bbstopboard;pk"
	};
	
	private static final int CACHE_SIZE = 25;
	private static final long REFRESH_INTERVAL_MINUTES = 10;
	
	private static final List<TopEntry> pvpCache = new ArrayList<>();
	private static final List<TopEntry> pkCache = new ArrayList<>();
	
	private static volatile LocalDateTime lastUpdate = LocalDateTime.now(ZoneId.systemDefault());
	private static volatile LocalDateTime nextUpdate = lastUpdate.plusMinutes(REFRESH_INTERVAL_MINUTES);
	
	static
	{
		ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
		scheduler.scheduleAtFixedRate(TopBoard::refreshAll, 0, REFRESH_INTERVAL_MINUTES, TimeUnit.MINUTES);
	}
	
	private static void refreshAll()
	{
		refreshRanking(LOAD_PVP, pvpCache, "pvpkills");
		refreshRanking(LOAD_PK, pkCache, "pkkills");
		lastUpdate = LocalDateTime.now(ZoneId.systemDefault());
		nextUpdate = lastUpdate.plusMinutes(REFRESH_INTERVAL_MINUTES);
	}
	
	private static void refreshRanking(String query, List<TopEntry> cache, String column)
	{
		List<TopEntry> temp = new ArrayList<>();
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(query);
			ResultSet rs = ps.executeQuery())
		{
			while (rs.next() && (temp.size() < CACHE_SIZE))
			{
				String name = rs.getString("char_name");
				int value = rs.getInt(column);
				String clan = rs.getString("clan_name");
				if ((clan == null) || clan.isEmpty())
				{
					clan = "<font color=\"4F4F4F\">-</font>";
				}
				int classId = rs.getInt("classid");
				ClassInfoHolder classInfo = ClassListData.getInstance().getClass(classId);
				String className = (classInfo != null) ? classInfo.getClassName() : "Unknown";
				temp.add(new TopEntry(name, value, clan, className));
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		// Atualiza o cache de forma thread-safe
		synchronized (cache)
		{
			cache.clear();
			cache.addAll(temp);
		}
	}
	
	private static class TopEntry
	{
		final String name;
		final int value;
		final String clan;
		final String className;
		
		TopEntry(String name, int value, String clan, String className)
		{
			this.name = name;
			this.value = value;
			this.clan = clan;
			this.className = className;
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
		String type = "pvp";
		if (command.contains(";"))
		{
			type = command.split(";")[1];
		}
		
		String html = buildTopHtml(type);
		
		CommunityBoardHandler.separateAndSend(html, player);
		
		return true;
	}
	
	private String buildTopHtml(String type)
	{
		String columnTitle;
		List<TopEntry> cache;
		switch (type)
		{
			case "pk":
				columnTitle = "Pks";
				cache = pkCache;
				break;
			default:
				columnTitle = "PvPs";
				cache = pvpCache;
				break;
		}
		StringBuilder sb = new StringBuilder();
		sb.append("<html noscrollbar><body><center>");
		sb.append("<table width=320><tr>");
		sb.append("<td><button value=\"PvP\" action=\"bypass _bbstopboard;pvp\" width=60 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
		sb.append("<td><button value=\"PK\" action=\"bypass _bbstopboard;pk\" width=60 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
		sb.append("</tr></table><br>");
		sb.append("<table width=750 border=0 cellspacing=0 cellpadding=2 background=L2UI_CT1.Windows_DF_Drawer_Bg>");
		sb.append("<tr>");
		sb.append("<td width=10 align=center></td>");
		sb.append("<td width=130 height=35 align=center><font name=\"hs12\" name=\"CreditTextSmall\" color=ae9977><br>Name</font></td>");
		sb.append("<td width=100 height=35 align=center><font name=\"hs12\" name=\"CreditTextSmall\" color=ae9977><br>").append(columnTitle).append("</font></td>");
		sb.append("<td width=180 height=35 align=center><font name=\"hs12\" name=\"CreditTextSmall\" color=ae9977><br>Clan</font></td>");
		sb.append("<td width=130 height=35 align=center><font name=\"hs12\" name=\"CreditTextSmall\" color=ae9977><br>Class</font></td>");
		sb.append("</tr>");
		
		int rank = 1;
		synchronized (cache)
		{
			for (TopEntry entry : cache)
			{
				String iconName = RankingIcons.getIconForRank(rank);
				String icon = iconName.isEmpty() ? String.valueOf(rank) : "<img src=\"" + iconName + "\" width=32 height=32>";
				sb.append("<tr>");
				sb.append("<td height=35 align=center width=35><br>").append(icon).append("</td>");
				sb.append("<td height=35 align=center><br>").append(entry.name).append("</td>");
				sb.append("<td height=35 align=center><br>").append(entry.value).append("</td>");
				sb.append("<td height=35 align=center><br>").append(entry.clan).append("</td>");
				sb.append("<td height=35 align=center><br>").append(entry.className).append("</td>");
				sb.append("</tr>");
				rank++;
			}
		}
		sb.append("<tr><td height=15></td></tr>");
		sb.append("</table>");
		DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm:ss");
		String last = lastUpdate.format(fmt);
		String next = nextUpdate.format(fmt);
		sb.append("<table width=750><tr>");
		sb.append("<td align=left width=250><font color=FF0000>Last update: ").append(last).append("</font></td>");
		sb.append("<td align=center width=250>");
		sb.append("<button value=\"Back\" action=\"bypass _bbshome\" width=80 height=25 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
		sb.append("</td>");
		sb.append("<td align=right width=250><font color=00FF00>Next update: ").append(next).append("</font></td>");
		sb.append("</tr></table>");
		sb.append("</center></body></html>");
		return sb.toString();
	}
}
