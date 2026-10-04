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
package handlers.communityboard;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.l2journey.commons.database.DatabaseFactory;
import com.l2journey.gameserver.cache.HtmCache;
import com.l2journey.gameserver.data.sql.CharInfoTable;
import com.l2journey.gameserver.data.sql.ClanTable;
import com.l2journey.gameserver.data.xml.ClassListData;
import com.l2journey.gameserver.handler.CommunityBoardHandler;
import com.l2journey.gameserver.handler.IWriteBoardHandler;
import com.l2journey.gameserver.model.BlockList;
import com.l2journey.gameserver.model.World;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.enums.creature.Race;
import com.l2journey.gameserver.model.actor.enums.player.PlayerClass;
import com.l2journey.gameserver.model.actor.holders.player.ClassInfoHolder;
import com.l2journey.gameserver.model.clan.Clan;
import com.l2journey.gameserver.network.SystemMessageId;
import com.l2journey.gameserver.network.enums.ChatType;
import com.l2journey.gameserver.network.serverpackets.CreatureSay;
import com.l2journey.gameserver.network.serverpackets.FriendPacket;

/**
 * Friends board.
 * @author Zoey76, L2Journey
 */
public class FriendsBoard implements IWriteBoardHandler
{
	private static final Logger LOGGER = Logger.getLogger(FriendsBoard.class.getName());
	
	private static final String[] COMMANDS =
	{
		"_bbsfriends",
		"_friendlist",
		"_friendinfo",
		"_friendblocklist",
		"_frienddelete",
		"_frienddeleteall",
		"_friendblockdelete",
		"_friendblockdeleteall",
		"_friendblockadd"
	};
	
	@Override
	public String[] getCommunityBoardCommands()
	{
		return COMMANDS;
	}
	
	@Override
	public boolean parseCommunityBoardCommand(String command, Player player)
	{
		if (command.equals("_bbsfriends") || command.equals("_friendlist"))
		{
			showFriendList(player, "");
		}
		else if (command.startsWith("_friendinfo_"))
		{
			final String name = command.substring("_friendinfo_".length());
			showFriendInfo(player, name);
		}
		else if (command.equals("_friendblocklist"))
		{
			showBlockList(player, "");
		}
		else if (command.equals("_frienddeleteall"))
		{
			final int count = deleteAllFriends(player);
			showFriendList(player, count + " friend(s) removed from your list.");
		}
		else if (command.startsWith("_frienddelete_"))
		{
			final String name = command.substring("_frienddelete_".length());
			deleteFriend(player, name);
			showFriendList(player, name + " has been removed from your friends list.");
		}
		else if (command.equals("_friendblockdeleteall"))
		{
			deleteAllBlocked(player);
			showBlockList(player, "All players removed from your ignore list.");
		}
		else if (command.startsWith("_friendblockdelete_"))
		{
			final String name = command.substring("_friendblockdelete_".length());
			final int targetId = CharInfoTable.getInstance().getIdByName(name);
			if (targetId > 0)
			{
				BlockList.removeFromBlockList(player, targetId);
			}
			showBlockList(player, name + " has been removed from your ignore list.");
		}
		return true;
	}
	
	@Override
	public boolean writeCommunityBoardCommand(Player player, String arg1, String arg2, String arg3, String arg4, String arg5)
	{
		if ((arg1 != null) && arg1.equals("PM"))
		{
			// Send PM: arg2 = target name, arg3 = message field (var "pm")
			final String targetName = (arg2 == null) ? "" : arg2.trim();
			final String message = (arg3 == null) ? "" : arg3.trim();
			
			if (targetName.isEmpty() || message.isEmpty())
			{
				showFriendInfo(player, targetName);
				return false;
			}
			
			final Player receiver = World.getInstance().getPlayer(targetName);
			if (receiver == null)
			{
				player.sendPacket(SystemMessageId.THAT_PLAYER_IS_NOT_ONLINE);
				showFriendInfo(player, targetName);
				return false;
			}
			
			if (player.isChatBanned())
			{
				player.sendPacket(SystemMessageId.CHATTING_IS_CURRENTLY_PROHIBITED);
				showFriendInfo(player, targetName);
				return false;
			}
			
			if (receiver.getMessageRefusal())
			{
				player.sendPacket(SystemMessageId.THAT_PERSON_IS_IN_MESSAGE_REFUSAL_MODE);
				showFriendInfo(player, targetName);
				return false;
			}
			
			receiver.sendPacket(new CreatureSay(null, ChatType.WHISPER, player.getName(), message));
			player.sendPacket(new CreatureSay(null, ChatType.WHISPER, "->" + receiver.getName(), message));
			showFriendInfo(player, targetName);
			return true;
		}
		
		// Block a player (from ignore list page)
		if ((arg1 != null) && arg1.equals("AddBlock"))
		{
			final String name = (arg3 == null) ? "" : arg3.trim();
			if (name.isEmpty())
			{
				showBlockList(player, "Please enter a character name.");
				return false;
			}
			
			final int targetId = CharInfoTable.getInstance().getIdByName(name);
			if (targetId <= 0)
			{
				showBlockList(player, "Player '" + name + "' was not found.");
				return false;
			}
			
			if (targetId == player.getObjectId())
			{
				showBlockList(player, "You cannot add yourself to the ignore list.");
				return false;
			}
			
			BlockList.addToBlockList(player, targetId);
			showBlockList(player, name + " has been added to your ignore list.");
			return true;
		}
		
		return false;
	}
	
	// -------------------------------------------------------------------------
	// Friend list page
	// -------------------------------------------------------------------------
	
	private void showFriendList(Player player, String message)
	{
		CommunityBoardHandler.getInstance().addBypass(player, "Friends List", "_friendlist");
		
		String html = HtmCache.getInstance().getHtm(player, "data/html/CommunityBoard/friends_list.html");
		
		final StringBuilder onlineRows = new StringBuilder();
		final StringBuilder offlineRows = new StringBuilder();
		
		for (int id : player.getFriendList())
		{
			final String name = CharInfoTable.getInstance().getNameById(id);
			if (name == null)
			{
				continue;
			}
			
			final Player online = World.getInstance().getPlayer(id);
			final boolean isOnline = (online != null) && online.isOnline();
			
			final StringBuilder currentList = isOnline ? onlineRows : offlineRows;
			
			currentList.append("<table border=\"0\" cellspacing=\"0\" cellpadding=\"3\" width=\"355\">");
			currentList.append("<tr>");
			currentList.append("<td width=\"240\">");
			if (isOnline)
			{
				currentList.append("<a action=\"bypass _friendinfo_").append(name).append("\"><font color=\"00AA00\">").append(name).append("</font></a>");
			}
			else
			{
				currentList.append("<font color=\"999999\">").append(name).append("</font>");
			}
			currentList.append("</td>");
			currentList.append("<td width=\"100\" align=\"right\">");
			currentList.append("<a action=\"bypass _frienddelete_").append(name).append("\">[Remove]</a>");
			currentList.append("</td>");
			currentList.append("</tr></table>");
		}
		
		if (onlineRows.isEmpty())
		{
			onlineRows.append("<font color=\"999999\">No online friends.</font>");
		}
		
		if (offlineRows.isEmpty())
		{
			offlineRows.append("<font color=\"999999\">No offline friends.</font>");
		}
		
		html = html.replace("%friend_list_online%", onlineRows.toString());
		html = html.replace("%friend_list_offline%", offlineRows.toString());
		html = html.replace("%delete_all_msg%", message.isEmpty() ? "" : "<font color=\"LEVEL\">" + message + "</font>");
		
		CommunityBoardHandler.separateAndSend(html, player);
	}
	
	private void showFriendInfo(Player player, String friendName)
	{
		CommunityBoardHandler.getInstance().addBypass(player, "Friend Info", "_friendinfo_" + friendName);
		
		final Player friend = World.getInstance().getPlayer(friendName);
		
		String html = HtmCache.getInstance().getHtm(player, "data/html/CommunityBoard/friends_info.html");
		
		if ((friend != null) && friend.isOnline())
		{
			final String className = ClassListData.getInstance().getClass(friend.getPlayerClass()).getClassName();
			final String clanName = friend.getClan() != null ? friend.getClan().getName() : "None";
			final String sex = friend.getAppearance().isFemale() ? "Female" : "Male";
			
			html = html.replace("%friendName%", friend.getName());
			html = html.replace("%raceIcon%", getRaceIcon(friend.getRace()));
			html = html.replace("%sex%", sex);
			html = html.replace("%class%", className);
			html = html.replace("%level%", String.valueOf(friend.getLevel()));
			html = html.replace("%clan%", clanName);
			html = html.replace("%online%", "<font color=\"00AA00\">Online</font>");
			html = html.replace("%pm_section%", buildPmSection(friend.getName()));
		}
		else
		{
			// Offline friend — query basic info from database
			String className = "Unknown";
			String clanName = "None";
			String sex = "Unknown";
			String level = "?";
			String raceIcon = "icon.skill0000";
			
			final int friendId = CharInfoTable.getInstance().getIdByName(friendName);
			if (friendId > 0)
			{
				try (Connection con = DatabaseFactory.getConnection();
					PreparedStatement st = con.prepareStatement("SELECT level, classid, clanid, sex FROM characters WHERE charId=?"))
				{
					st.setInt(1, friendId);
					try (ResultSet rs = st.executeQuery())
					{
						if (rs.next())
						{
							level = String.valueOf(rs.getInt("level"));
							final int classId = rs.getInt("classid");
							final ClassInfoHolder pc = ClassListData.getInstance().getClass(classId);
							if (pc != null)
							{
								className = pc.getClassName();
							}
							final PlayerClass playerClass = PlayerClass.getPlayerClass(classId);
							if (playerClass != null)
							{
								raceIcon = getRaceIcon(playerClass.getRace());
							}
							final int clanId = rs.getInt("clanid");
							if (clanId > 0)
							{
								final Clan clan = ClanTable.getInstance().getClan(clanId);
								if (clan != null)
								{
									clanName = clan.getName();
								}
							}
							sex = rs.getInt("sex") == 1 ? "Female" : "Male";
						}
					}
				}
				catch (Exception e)
				{
					LOGGER.log(Level.WARNING, "FriendsBoard: error loading friend info for " + friendName, e);
				}
			}
			
			html = html.replace("%friendName%", friendName);
			html = html.replace("%raceIcon%", raceIcon);
			html = html.replace("%sex%", sex);
			html = html.replace("%class%", className);
			html = html.replace("%level%", level);
			html = html.replace("%clan%", clanName);
			html = html.replace("%online%", "<font color=\"D70000\">Offline</font>");
			html = html.replace("%pm_section%", "<font color=\"999999\">Player is offline. PM is not available.</font>");
		}
		
		CommunityBoardHandler.separateAndSend(html, player);
	}
	
	private String buildPmSection(String friendName)
	{
		return "<table border=\"0\" cellspacing=\"0\" cellpadding=\"0\" width=\"280\">" + "<tr><td><multiedit var=\"pm\" width=260 height=70></td></tr>" + "<tr><td height=\"15\"></td></tr>" + "<tr><td align=\"left\"><button value=\"Send PM\" action=\"Write Friends PM " + friendName + " pm pm pm\" " + "back=\"l2ui_ct1.button.button_df_small_down\" width=\"100\" height=\"25\" " + "fore=\"l2ui_ct1.button.button_df_small\"></td></tr>" + "</table>";
	}
	
	private String getRaceIcon(Race race)
	{
		if (race == null)
		{
			return "icon.skill0000";
		}
		
		switch (race)
		{
			case HUMAN:
				return "icon.skillhuman";
			case ELF:
				return "icon.skillelf";
			case DARK_ELF:
				return "icon.skilldarkelf";
			case ORC:
				return "icon.skillorc";
			case DWARF:
				return "icon.skilldwarf";
			case KAMAEL:
				return "icon.skillkamael";
			default:
				return "icon.skill0000";
		}
	}
	
	private void deleteFriend(Player player, String friendName)
	{
		final int id = CharInfoTable.getInstance().getIdByName(friendName);
		if ((id <= 0) || !player.getFriendList().contains(id))
		{
			return;
		}
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement st = con.prepareStatement("DELETE FROM character_friends WHERE (charId=? AND friendId=?) OR (charId=? AND friendId=?)"))
		{
			st.setInt(1, player.getObjectId());
			st.setInt(2, id);
			st.setInt(3, id);
			st.setInt(4, player.getObjectId());
			st.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "FriendsBoard: error deleting friend " + friendName, e);
			return;
		}
		
		player.getFriendList().remove(Integer.valueOf(id));
		player.sendPacket(new FriendPacket(false, id));
		
		final Player target = World.getInstance().getPlayer(id);
		if (target != null)
		{
			target.getFriendList().remove(Integer.valueOf(player.getObjectId()));
			target.sendPacket(new FriendPacket(false, player.getObjectId()));
		}
	}
	
	private int deleteAllFriends(Player player)
	{
		final Collection<Integer> copy = new ArrayList<>(player.getFriendList());
		int count = 0;
		for (int id : copy)
		{
			final String name = CharInfoTable.getInstance().getNameById(id);
			if (name != null)
			{
				deleteFriend(player, name);
				count++;
			}
		}
		return count;
	}
	
	// -------------------------------------------------------------------------
	// Block (ignore) list page
	// -------------------------------------------------------------------------
	
	private void showBlockList(Player player, String message)
	{
		CommunityBoardHandler.getInstance().addBypass(player, "Ignore List", "_friendblocklist");
		
		String html = HtmCache.getInstance().getHtm(player, "data/html/CommunityBoard/friends_block_list.html");
		
		final StringBuilder col1 = new StringBuilder();
		final StringBuilder col2 = new StringBuilder();
		final Set<Integer> blockedIds = loadBlockedIds(player);
		
		if (blockedIds.isEmpty())
		{
			col1.append("<font color=\"999999\">Your ignore list is empty.</font>");
			col2.append("");
		}
		else
		{
			final java.util.List<Integer> blockedList = new java.util.ArrayList<>(blockedIds);
			final int mid = (blockedList.size() + 1) / 2;
			
			// Primeira coluna
			for (int i = 0; i < mid; i++)
			{
				final int id = blockedList.get(i);
				final String name = CharInfoTable.getInstance().getNameById(id);
				if (name != null)
				{
					col1.append("<table border=\"0\" cellspacing=\"0\" cellpadding=\"3\" width=\"355\">");
					col1.append("<tr>");
					col1.append("<td width=\"240\"><font color=\"999999\">").append(name).append("</font></td>");
					col1.append("<td width=\"100\" align=\"right\">");
					col1.append("<a action=\"bypass _friendblockdelete_").append(name).append("\">[Remove]</a>");
					col1.append("</td>");
					col1.append("</tr></table>");
				}
			}
			
			// Segunda coluna
			for (int i = mid; i < blockedList.size(); i++)
			{
				final int id = blockedList.get(i);
				final String name = CharInfoTable.getInstance().getNameById(id);
				if (name != null)
				{
					col2.append("<table border=\"0\" cellspacing=\"0\" cellpadding=\"3\" width=\"355\">");
					col2.append("<tr>");
					col2.append("<td width=\"240\"><font color=\"999999\">").append(name).append("</font></td>");
					col2.append("<td width=\"100\" align=\"right\">");
					col2.append("<a action=\"bypass _friendblockdelete_").append(name).append("\">[Remove]</a>");
					col2.append("</td>");
					col2.append("</tr></table>");
				}
			}
			
			if (col1.isEmpty())
			{
				col1.append("<font color=\"999999\">No blocked players.</font>");
			}
			if (col2.isEmpty())
			{
				col2.append("");
			}
		}
		
		html = html.replace("%block_list_col1%", col1.toString());
		html = html.replace("%block_list_col2%", col2.toString());
		html = html.replace("%delete_all_msg%", message.isEmpty() ? "" : "<font color=\"LEVEL\">" + message + "</font>");
		
		CommunityBoardHandler.separateAndSend(html, player);
	}
	
	private void deleteAllBlocked(Player player)
	{
		final Set<Integer> copy = loadBlockedIds(player);
		for (int id : copy)
		{
			BlockList.removeFromBlockList(player, id);
		}
	}
	
	/**
	 * Loads the block (ignore) list IDs directly from the database. This avoids any dependency on the compiled BlockList class methods, allowing the script to compile without a full server rebuild.
	 * @param player
	 * @return
	 */
	private Set<Integer> loadBlockedIds(Player player)
	{
		final Set<Integer> ids = new HashSet<>();
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement st = con.prepareStatement("SELECT friendId FROM character_friends WHERE charId=? AND relation=1"))
		{
			st.setInt(1, player.getObjectId());
			try (ResultSet rs = st.executeQuery())
			{
				while (rs.next())
				{
					ids.add(rs.getInt("friendId"));
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "FriendsBoard: error loading block list for " + player.getName(), e);
		}
		return ids;
	}
}
