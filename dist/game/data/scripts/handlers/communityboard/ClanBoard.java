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

import com.l2journey.gameserver.data.sql.ClanHallTable;
import com.l2journey.gameserver.data.sql.ClanTable;
import com.l2journey.gameserver.handler.CommunityBoardHandler;
import com.l2journey.gameserver.handler.IWriteBoardHandler;
import com.l2journey.gameserver.managers.CastleManager;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.clan.Clan;
import com.l2journey.gameserver.model.clan.ClanMember;
import com.l2journey.gameserver.model.residences.AuctionableHall;
import com.l2journey.gameserver.model.siege.Castle;
import com.l2journey.gameserver.model.skill.Skill;
import com.l2journey.gameserver.network.SystemMessageId;
import com.l2journey.gameserver.util.HtmlUtil;

/**
 * Clan board.
 * @author Zoey76, KingHanker
 */
public class ClanBoard implements IWriteBoardHandler
{
	private static final String[] COMMANDS =
	{
		"_bbsclan",
		"_bbsclan_list",
		"_bbsclan_clanhome"
	};
	
	@Override
	public String[] getCommunityBoardCommands()
	{
		return COMMANDS;
	}
	
	@Override
	public boolean parseCommunityBoardCommand(String command, Player player)
	{
		if (command.equals("_bbsclan"))
		{
			CommunityBoardHandler.getInstance().addBypass(player, "Clan", command);
			final Clan clan = player.getClan();
			if ((clan == null) || (clan.getLevel() < 2))
			{
				clanList(player, 1);
			}
			else
			{
				clanHome(player);
			}
		}
		else if (command.startsWith("_bbsclan_clanlist"))
		{
			CommunityBoardHandler.getInstance().addBypass(player, "Clan List", command);
			if (command.equals("_bbsclan_clanlist"))
			{
				clanList(player, 1);
			}
			else if (command.startsWith("_bbsclan_clanlist;"))
			{
				try
				{
					clanList(player, Integer.parseInt(command.split(";")[1]));
				}
				catch (Exception _)
				{
					clanList(player, 1);
					LOG.warning(ClanBoard.class.getSimpleName() + ": " + player + " send invalid clan list bypass " + command + "!");
				}
			}
		}
		else if (command.startsWith("_bbsclan_clanhome"))
		{
			CommunityBoardHandler.getInstance().addBypass(player, "Clan Home", command);
			if (command.equals("_bbsclan_clanhome"))
			{
				clanHome(player);
			}
			else if (command.startsWith("_bbsclan_clanhome;"))
			{
				try
				{
					clanHome(player, Integer.parseInt(command.split(";")[1]));
				}
				catch (Exception _)
				{
					clanHome(player);
					LOG.warning(ClanBoard.class.getSimpleName() + ": " + player + " send invalid clan home bypass " + command + "!");
				}
			}
		}
		else if (command.startsWith("_bbsclan_clannotice_edit;"))
		{
			CommunityBoardHandler.getInstance().addBypass(player, "Clan Edit", command);
			clanNotice(player, player.getClanId());
		}
		else if (command.startsWith("_bbsclan_clannotice_enable"))
		{
			CommunityBoardHandler.getInstance().addBypass(player, "Clan Notice Enable", command);
			final Clan clan = player.getClan();
			if (clan != null)
			{
				clan.setNoticeEnabled(true);
			}
			
			clanNotice(player, player.getClanId());
		}
		else if (command.startsWith("_bbsclan_clannotice_disable"))
		{
			CommunityBoardHandler.getInstance().addBypass(player, "Clan Notice Disable", command);
			final Clan clan = player.getClan();
			if (clan != null)
			{
				clan.setNoticeEnabled(false);
			}
			
			clanNotice(player, player.getClanId());
		}
		else
		{
			CommunityBoardHandler.separateAndSend("<html><body><br><br><center>Command " + command + " need development.</center><br><br></body></html>", player);
		}
		
		return true;
	}
	
	private void clanNotice(Player player, int clanId)
	{
		final Clan cl = ClanTable.getInstance().getClan(clanId);
		if (cl != null)
		{
			if (cl.getLevel() < 2)
			{
				player.sendPacket(SystemMessageId.THERE_ARE_NO_COMMUNITIES_IN_MY_CLAN_CLAN_COMMUNITIES_ARE_ALLOWED_FOR_CLANS_WITH_SKILL_LEVELS_OF_2_AND_HIGHER);
				parseCommunityBoardCommand("_bbsclan_clanlist", player);
			}
			else
			{
				final StringBuilder html = new StringBuilder(2048);
				html.append("<html><body><br><br><table border=0 width=610><tr><td width=10></td><td width=600 align=left><a action=\"bypass _bbshome\">Home</a> &gt; <a action=\"bypass _bbsclan_clanlist\"> Clan Community </a>  &gt; <a action=\"bypass _bbsclan_clanhome;");
				html.append(clanId);
				html.append("\"> &amp;$802; </a></td></tr></table>");
				if (player.isClanLeader())
				{
					html.append("<br><br><center><table width=610 border=0 cellspacing=0 cellpadding=0><tr><td fixwidth=610><font color=\"AAAAAA\">The Clan Notice function allows the clan leader to send messages through a pop-up window to clan members at login.</font> </td></tr><tr><td height=20></td></tr>");
					final Clan clan = player.getClan();
					if (clan.isNoticeEnabled())
					{
						html.append("<tr><td fixwidth=610> Clan Notice Function:&nbsp;&nbsp;&nbsp;on&nbsp;&nbsp;&nbsp;/&nbsp;&nbsp;&nbsp;<a action=\"bypass _bbsclan_clannotice_disable\">off</a>");
					}
					else
					{
						html.append("<tr><td fixwidth=610> Clan Notice Function:&nbsp;&nbsp;&nbsp;<a action=\"bypass _bbsclan_clannotice_enable\">on</a>&nbsp;&nbsp;&nbsp;/&nbsp;&nbsp;&nbsp;off");
					}
					
					html.append("</td></tr></table><img src=\"L2UI.Squaregray\" width=\"610\" height=\"1\"><br> <br><table width=610 border=0 cellspacing=2 cellpadding=0><tr><td>Edit Notice: </td></tr><tr><td height=5></td></tr><tr><td><MultiEdit var =\"Content\" width=610 height=100></td></tr></table><br><table width=610 border=0 cellspacing=0 cellpadding=0><tr><td height=5></td></tr><tr><td align=center FIXWIDTH=65><button value=\"Confirm\" action=\"Write Notice Set _ Content Content Content\" back=\"L2UI_CT1.Button_DF_Down\" width=65 height=22 fore=\"L2UI_CT1.Button_DF\" ></td><td align=center FIXWIDTH=45></td><td align=center FIXWIDTH=500></td></tr></table></center></body></html>");
					HtmlUtil.sendCBHtml(player, html.toString(), clan.getNotice());
				}
				else
				{
					html.append("<img src=\"L2UI.squareblank\" width=\"1\" height=\"10\"><center><table border=0 cellspacing=0 cellpadding=0><tr><td>You are not your clan's leader, and therefore cannot change the clan notice</td></tr></table>");
					final Clan clan = player.getClan();
					if (clan.isNoticeEnabled())
					{
						html.append("<table border=0 cellspacing=0 cellpadding=0><tr><td>The current clan notice:</td></tr><tr><td fixwidth=5></td><td FIXWIDTH=600 align=left>" + clan.getNotice() + "</td><td fixqqwidth=5></td></tr></table>");
					}
					
					html.append("</center></body></html>");
					CommunityBoardHandler.separateAndSend(html.toString(), player);
				}
			}
		}
	}
	
	private void clanList(Player player, int indexValue)
	{
		int index = indexValue;
		if (index < 1)
		{
			index = 1;
		}
		
		final StringBuilder html = new StringBuilder(2048);
		html.append("<html><body><br><br><center><br1><br1><table border=0 cellspacing=0 cellpadding=0><tr><td FIXWIDTH=15>&nbsp;</td><td width=610 height=30 align=left><a action=\"bypass _bbshome\">Home</a> &nbsp;&gt;<a action=\"bypass _bbsclan_clanlist\"> Clan Community </a></td></tr></table><table border=0 cellspacing=0 cellpadding=0 width=610 bgcolor=434343><tr><td height=10></td></tr><tr><td fixWIDTH=5></td><td fixWIDTH=600><a action=\"bypass _bbsclan_clanhome;");
		html.append(player.getClan() != null ? player.getClan().getId() : 0);
		html.append("\">[Go to my clan]</a>&nbsp;&nbsp;</td><td fixWIDTH=5></td></tr><tr><td height=10></td></tr></table><br><table border=0 cellspacing=0 cellpadding=2 bgcolor=5A5A5A width=610><tr><td FIXWIDTH=5></td><td FIXWIDTH=200 align=center>Clan Name</td><td FIXWIDTH=200 align=center>Clan Leader</td><td FIXWIDTH=100 align=center>Clan Level</td><td FIXWIDTH=100 align=center>Clan Members</td><td FIXWIDTH=5></td></tr></table><img src=\"L2UI.Squareblank\" width=\"1\" height=\"5\">");
		int i = 0;
		for (Clan cl : ClanTable.getInstance().getClans())
		{
			if (i > ((index + 1) * 7))
			{
				break;
			}
			
			if (i++ >= ((index - 1) * 7))
			{
				html.append("<img src=\"L2UI.SquareBlank\" width=\"610\" height=\"3\"><table border=0 cellspacing=0 cellpadding=0 width=610><tr> <td FIXWIDTH=5></td><td FIXWIDTH=200 align=center><a action=\"bypass _bbsclan_clanhome;");
				html.append(cl.getId());
				html.append("\">");
				html.append(cl.getName());
				html.append("</a></td><td FIXWIDTH=200 align=center>");
				html.append(cl.getLeaderName());
				html.append("</td><td FIXWIDTH=100 align=center>");
				html.append(cl.getLevel());
				html.append("</td><td FIXWIDTH=100 align=center>");
				html.append(cl.getMembersCount());
				html.append("</td><td FIXWIDTH=5></td></tr><tr><td height=5></td></tr></table><img src=\"L2UI.SquareBlank\" width=\"610\" height=\"3\"><img src=\"L2UI.SquareGray\" width=\"610\" height=\"1\">");
			}
		}
		
		html.append("<img src=\"L2UI.SquareBlank\" width=\"610\" height=\"2\"><table cellpadding=0 cellspacing=2 border=0><tr>");
		if (index == 1)
		{
			html.append("<td><button action=\"\" back=\"l2ui_ch3.prev1_down\" fore=\"l2ui_ch3.prev1\" width=16 height=16 ></td>");
		}
		else
		{
			html.append("<td><button action=\"_bbsclan_clanlist;");
			html.append(index - 1);
			html.append("\" back=\"l2ui_ch3.prev1_down\" fore=\"l2ui_ch3.prev1\" width=16 height=16 ></td>");
		}
		
		int nbp = ClanTable.getInstance().getClanCount() / 8;
		if ((nbp * 8) != ClanTable.getInstance().getClanCount())
		{
			nbp++;
		}
		for (i = 1; i <= nbp; i++)
		{
			if (i == index)
			{
				html.append("<td> ");
				html.append(i);
				html.append(" </td>");
			}
			else
			{
				html.append("<td><a action=\"bypass _bbsclan_clanlist;");
				html.append(i);
				html.append("\"> ");
				html.append(i);
				html.append(" </a></td>");
			}
		}
		
		if (index == nbp)
		{
			html.append("<td><button action=\"\" back=\"l2ui_ch3.next1_down\" fore=\"l2ui_ch3.next1\" width=16 height=16 ></td>");
		}
		else
		{
			html.append("<td><button action=\"bypass _bbsclan_clanlist;");
			html.append(index + 1);
			html.append("\" back=\"l2ui_ch3.next1_down\" fore=\"l2ui_ch3.next1\" width=16 height=16 ></td>");
		}
		
		html.append("</tr></table><table border=0 cellspacing=0 cellpadding=0><tr><td width=610><img src=\"sek.cbui141\" width=\"610\" height=\"1\"></td></tr></table><table border=0><tr><td><combobox width=65 var=keyword list=\"Name;Ruler\"></td><td><edit var = \"Search\" width=130 height=14 length=\"16\"></td>" +
		// TODO: search (Write in BBS)
			"<td><button value=\"&$420;\" action=\"Write 5 -1 0 Search keyword keyword\" back=\"l2ui_ct1.button.button_df_small_down\" width=65 height=23 fore=\"l2ui_ct1.button.button_df_small\"> </td> </tr></table><br><br></center></body></html>");
		CommunityBoardHandler.separateAndSend(html.toString(), player);
	}
	
	private void clanHome(Player player)
	{
		clanHome(player, player.getClan().getId());
	}
	
	private void clanHome(Player player, int clanId)
	{
		final Clan cl = ClanTable.getInstance().getClan(clanId);
		if (cl != null)
		{
			if (cl.getLevel() < 2)
			{
				player.sendPacket(SystemMessageId.THERE_ARE_NO_COMMUNITIES_IN_MY_CLAN_CLAN_COMMUNITIES_ARE_ALLOWED_FOR_CLANS_WITH_SKILL_LEVELS_OF_2_AND_HIGHER);
				parseCommunityBoardCommand("_bbsclan_clanlist", player);
			}
			else
			{
				StringBuilder html = new StringBuilder();
				html.append("<html><body><center><br><br><br1><br1><table border=0 cellspacing=0 cellpadding=0><tr><td FIXWIDTH=15>&nbsp;</td><td width=610 height=30 align=left><a action=\"bypass _bbshome\">Home</a> &nbsp;&gt; <a action=\"bypass _bbsclan_clanlist\"> Clan Community </a> &nbsp;&gt; <a action=\"bypass _bbsclan_clanhome;");
				html.append(clanId);
				html.append("\"> &amp;$802; </a></td></tr></table><table border=0 cellspacing=0 cellpadding=0 width=610 bgcolor=434343><tr><td height=10></td></tr><tr><td fixWIDTH=5></td><td fixwidth=600><a action=\"bypass _bbsclan_clanhome;");
				html.append(clanId);
				html.append(";announce\"></a> <a action=\"bypass _bbsclan_clanhome;");
				html.append(clanId);
				html.append(";cbb\"></a><a action=\"bypass _bbsclan_clanhome;");
				html.append(clanId);
				html.append(";cmail\"></a>&nbsp;&nbsp;<a action=\"bypass _bbsclan_clannotice_edit;");
				html.append(clanId);
				html.append(";cnotice\"></a>&nbsp;&nbsp;</td><td fixWIDTH=5></td></tr><tr><td height=10></td></tr></table>");
				
				html.append("<div style='height:15px'></div>");
				html.append("<table border=0 cellspacing=0 cellpadding=0 width=530><tr><td height=10></td></tr><tr><td fixWIDTH=5></td><td fixwidth=290 valign=top>");
				
				html.append("<table border=0 cellspacing=2 cellpadding=0>");
				
				int col = 0;
				boolean hasSkill = false;
				for (Skill skill : cl.getAllSkills())
				{
					if (col == 0)
					{
						html.append("<tr>");
					}
					
					String iconPath = skill.getIcon();
					if ((iconPath != null) && !iconPath.isEmpty())
					{
						html.append("<td align=\"center\"><img src=\"").append(iconPath).append("\" width=32 height=32 style=\"margin:2px;\"></td>");
						hasSkill = true;
					}
					
					col++;
					if (col == 7)
					{
						html.append("</tr>");
						col = 0;
					}
				}
				
				if (col != 0)
				{
					for (int i = col; i < 7; i++)
					{
						html.append("<td></td>");
					}
					
					html.append("</tr>");
				}
				
				html.append("</table>");
				
				if (!hasSkill)
				{
					html.append(" ");
				}
				
				html.append("</td><td fixWIDTH=5></td><td fixWIDTH=5 align=center valign=top><img src=\"l2ui.squaregray\" width=2  height=128></td><td fixWIDTH=5></td><td fixwidth=295><table border=0 cellspacing=0 cellpadding=0 width=295><tr><td fixWIDTH=100 align=left><font name=\"hs8\" name=\"CreditTextSmall\" color=ae9977>Clan Name:</font></td><td fixWIDTH=195 align=left>");
				html.append("<font name=\"hs8\" name=\"CreditTextSmall\" color=B0C4DE>" + cl.getName() + "</font>");
				html.append("</td></tr><tr><td height=7></td></tr><tr><td fixWIDTH=100 align=left><font name=\"hs8\" name=\"CreditTextSmall\" color=ae9977>Clan Level:</font></td><td fixWIDTH=195 align=left height=16>");
				html.append("<font name=\"hs8\" name=\"CreditTextSmall\" color=B0C4DE>" + cl.getLevel() + "</font>");
				html.append("</td></tr><tr><td height=7></td></tr><tr><td fixWIDTH=100 align=left><font name=\"hs8\" name=\"CreditTextSmall\" color=ae9977>Clan Members:</font></td><td fixWIDTH=195 align=left height=16>");
				html.append("<font name=\"hs8\" name=\"CreditTextSmall\" color=B0C4DE>" + cl.getMembersCount() + "</font>");
				html.append("</td></tr><tr><td height=7></td></tr><tr><td fixWIDTH=100 align=left><font name=\"hs8\" name=\"CreditTextSmall\" color=ae9977>Clan Leader:<font></td><td fixWIDTH=195 align=left height=16>");
				html.append("<font name=\"hs8\" name=\"CreditTextSmall\" color=B0C4DE>" + cl.getLeaderName() + "</font>");
				html.append("</td></tr><tr><td height=7></td></tr><tr><td fixWIDTH=100 align=left><font name=\"hs8\" name=\"CreditTextSmall\" color=ae9977>Clan Alliance:</font></td><td fixWIDTH=195 align=left height=16>");
				html.append((cl.getAllyName() != null) ? "<font name=\"hs8\" name=\"CreditTextSmall\" color=B0C4DE>" + cl.getAllyName() + "</font>" : "");
				html.append("</td></tr><tr><td height=7></td></tr><tr><td fixWIDTH=100 align=left><font name=\"hs8\" name=\"CreditTextSmall\" color=ae9977>Clan Hall:</font></td><td fixWIDTH=195 align=left height=16>");
				
				final AuctionableHall clanHall = ClanHallTable.getInstance().getClanHallByOwner(cl);
				if (clanHall != null)
				{
					html.append("<font name=\"hs8\" name=\"CreditTextSmall\" color=B0C4DE>" + clanHall.getName() + "</font>");
				}
				else
				{
					html.append(" ");
				}
				
				html.append("</td></tr><tr><td height=7></td></tr><tr><td fixWIDTH=100 align=left><font name=\"hs8\" name=\"CreditTextSmall\" color=ae9977>Clan Castle:</font></td><td fixWIDTH=195 align=left height=16>");
				
				final Castle castle = CastleManager.getInstance().getCastleByOwner(cl);
				if (castle != null)
				{
					html.append("<font name=\"hs8\" name=\"CreditTextSmall\" color=B0C4DE>" + castle.getName() + "</font>");
				}
				else
				{
					html.append(" ");
				}
				
				html.append("</td></tr><tr><td height=7></td></tr>");
				
				if (player.isClanLeader())
				{
					html.append("<tr>");
					html.append("<td fixWIDTH=100 align=left>");
					html.append("<button action=\"bypass _bbsclan_clannotice_edit;").append(cl.getId()).append(";cnotice\" value=\"Clan Notice\" width=80 height=27 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
					html.append("</td>");
					html.append("<td></td>");
					html.append("</tr>");
				}
				
				html.append("</table></td><td fixWIDTH=5></td></tr><tr><td height=10></td></tr></table>");
				html.append("<img src=\"L2UI.squareblank\" width=\"1\" height=\"5\"><img src=\"L2UI.squaregray\" width=\"610\" height=\"1\"><br></center>");
				
				html.append("<center><font name=\"hs12\" name=\"CreditTextSmall\" color=ae9977>Clan Members</font></center><br1>");
				html.append("<center><img src=\"L2UI.squareblank\" width=\"1\" height=\"5\"><img src=\"L2UI.squaregray\" width=\"350\" height=\"1\"><br></center>");
				
				html.append("<center><table border=0 cellspacing=10 cellpadding=0 width=610>");
				html.append("<tr><td></td></tr>");
				
				int memberCol = 0;
				int memberCount = 0;
				
				html.append("<tr>");
				
				for (ClanMember member : cl.getMembers())
				{
					if (memberCount >= 40)
					{
						break;
					}
					
					html.append("<td align=\"center\" width=120 style=\"padding:8px 0;\"><font color=\"FFFFFF\">").append(member.getName()).append("</font></td>");
					
					memberCol++;
					memberCount++;
					
					if (memberCol == 6)
					{
						html.append("</tr><tr>");
						memberCol = 0;
					}
				}
				
				if (memberCol != 0)
				{
					for (int i = memberCol; i < 6; i++)
					{
						html.append("<td></td>");
					}
					
					html.append("</tr>");
				}
				else
				{
					html.append("</tr>");
				}
				
				html.append("</table></center></body></html>");
				CommunityBoardHandler.separateAndSend(html.toString(), player);
			}
		}
	}
	
	@Override
	public boolean writeCommunityBoardCommand(Player player, String arg1, String arg2, String arg3, String arg4, String arg5)
	{
		final Clan clan = player.getClan();
		if ((clan != null) && player.isClanLeader())
		{
			clan.setNotice(arg3);
			player.sendPacket(SystemMessageId.YOUR_CLAN_NOTICE_HAS_BEEN_SAVED);
		}
		
		return true;
	}
}
