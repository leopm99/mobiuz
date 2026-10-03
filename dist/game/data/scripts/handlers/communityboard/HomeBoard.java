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
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import com.l2journey.Config;
import com.l2journey.EventsConfig;
import com.l2journey.commons.database.DatabaseFactory;
import com.l2journey.commons.threads.ThreadPool;
import com.l2journey.gameserver.cache.HtmCache;
import com.l2journey.gameserver.data.sql.ClanTable;
import com.l2journey.gameserver.data.xml.BuyListData;
import com.l2journey.gameserver.data.xml.ExperienceData;
import com.l2journey.gameserver.data.xml.MultisellData;
import com.l2journey.gameserver.handler.CommunityBoardHandler;
import com.l2journey.gameserver.handler.IParseBoardHandler;
import com.l2journey.gameserver.managers.PcCafePointsManager;
import com.l2journey.gameserver.managers.PremiumManager;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.clan.ClanAccess;
import com.l2journey.gameserver.model.item.Henna;
import com.l2journey.gameserver.model.item.enums.ItemProcessType;
import com.l2journey.gameserver.model.zone.ZoneId;
import com.l2journey.gameserver.network.SystemMessageId;
import com.l2journey.gameserver.network.serverpackets.ActionFailed;
import com.l2journey.gameserver.network.serverpackets.BuyList;
import com.l2journey.gameserver.network.serverpackets.ExBuySellList;
import com.l2journey.gameserver.network.serverpackets.ExShowVariationCancelWindow;
import com.l2journey.gameserver.network.serverpackets.ExShowVariationMakeWindow;
import com.l2journey.gameserver.network.serverpackets.HennaEquipList;
import com.l2journey.gameserver.network.serverpackets.HennaRemoveList;
import com.l2journey.gameserver.network.serverpackets.ShowBoard;
import com.l2journey.gameserver.network.serverpackets.WareHouseDepositList;
import com.l2journey.gameserver.network.serverpackets.WareHouseWithdrawalList;

/**
 * Home board.
 * @author Zoey76, Mobius, KingHanker
 */
public class HomeBoard implements IParseBoardHandler
{
	// Carrega TipsBoard (e a dica ativa) na inicializacao do servidor.
	static
	{
		TipsBoard.getInstance();
	}
	
	// SQL Queries
	private static final String COUNT_FAVORITES = "SELECT COUNT(*) AS favorites FROM `bbs_favorites` WHERE `playerId`=?";
	private final TopBoard topBoard = new TopBoard();
	
	private static final String[] COMMANDS =
	{
		"_bbshome",
		"_bbstop",
	};
	
	private static final String[] CUSTOM_COMMANDS =
	{
		Config.COMMUNITY_PREMIUM_SYSTEM_ENABLED ? "_bbspremium" : null,
		// Multisell, ExcMultisell e Sell fazem parte do Merchant.
		Config.COMMUNITYBOARD_MERCHANT ? "_bbsexcmultisell" : null,
		Config.COMMUNITYBOARD_MERCHANT ? "_bbsmultisell" : null,
		Config.COMMUNITYBOARD_MERCHANT ? "_bbssell" : null,
		Config.COMMUNITYBOARD_ENABLE_TELEPORTS ? "_bbsteleport" : null,
		Config.COMMUNITYBOARD_ENABLE_DELEVEL ? "_bbsdelevel" : null,
		// Warehouse, Augment e Draw fazem parte do BlackSmith.
		Config.COMMUNITYBOARD_BLACKSMITH ? "_bbswarhouse" : null,
		Config.COMMUNITYBOARD_BLACKSMITH ? "_bbsaugment" : null,
		Config.COMMUNITYBOARD_BLACKSMITH ? "_bbsdraw" : null
	};
	
	private static final BiPredicate<String, Player> COMBAT_CHECK = (command, player) ->
	{
		boolean commandCheck = false;
		for (String c : CUSTOM_COMMANDS)
		{
			if ((c != null) && command.startsWith(c))
			{
				commandCheck = true;
				break;
			}
		}
		
		return commandCheck && (player.isCastingNow() || player.isCastingSimultaneouslyNow() || player.isInCombat() || player.isInDuel() || player.isInOlympiadMode() || player.isInsideZone(ZoneId.SIEGE) || player.isInsideZone(ZoneId.PVP) || (player.getPvpFlag() > 0) || player.isAlikeDead() || player.isOnEvent() || player.isInStoreMode());
	};
	
	private static final Predicate<Player> KARMA_CHECK = player -> Config.COMMUNITYBOARD_KARMA_DISABLED && (player.getKarma() > 0);
	
	private static final Map<Integer, String> BOSS_STATUS_CACHE = new ConcurrentHashMap<>();
	private static final int[] BOSSES =
	{
		29068,
		29020,
		29118,
		29006,
		29014,
		29001,
		29028
	};
	
	private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
	private static LocalDateTime lastUpdateTime = LocalDateTime.now();
	private static LocalDateTime nextUpdateTime = lastUpdateTime.plusHours(1);
	private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
	
	static
	{
		// Updates the cache immediately on startup
		updateBossStatusCache();
		// Schedules update every 1 hour
		scheduler.scheduleAtFixedRate(HomeBoard::updateBossStatusCache, 1, 1, TimeUnit.HOURS);
	}
	
	private static void updateBossStatusCache()
	{
		for (int bossId : BOSSES)
		{
			long delay = com.l2journey.gameserver.managers.GrandBossManager.getInstance().getStatSet(bossId).getLong("respawn_time");
			if (delay <= System.currentTimeMillis())
			{
				BOSS_STATUS_CACHE.put(bossId, "<font color=\"32C332\">Is Alive</font>");
			}
			else
			{
				BOSS_STATUS_CACHE.put(bossId, "<font color=\"FF3333\">Is Dead</font>");
			}
		}
		
		lastUpdateTime = LocalDateTime.now();
		nextUpdateTime = lastUpdateTime.plusHours(1);
	}
	
	/**
	 * Returns the last update time for the boss status cache.
	 * @return String formatted as HH:mm
	 */
	public static String getLastUpdateTime()
	{
		return lastUpdateTime.format(TIME_FORMAT);
	}
	
	/**
	 * Returns the next scheduled update time for the boss status cache.
	 * @return String formatted as HH:mm
	 */
	public static String getNextUpdateTime()
	{
		return nextUpdateTime.format(TIME_FORMAT);
	}
	
	/**
	 * Builds the top row with feature cards (Buffer, Buffer Premium, DropSearch, Teleport). Cards are filtered by their corresponding configs and resized to fit the 720px row.
	 * @return the HTML fragment, or empty string if no card is enabled
	 */
	private static String buildTopFeaturesBlock()
	{
		// Cells in fixed display order: { icon, title, subtitle, button_label, button_action }.
		final List<String[]> cells = new ArrayList<>(4);
		if (Config.COMMUNITYBOARD_BUFFER)
		{
			cells.add(new String[]
			{
				"Icon.skill6319",
				"Buffs",
				"Get ready for the fight.",
				"Abrir Buffer",
				"bypass _bbsbuffer"
			});
		}
		if (Config.COMMUNITYBOARD_BUFFER_PREMIUM)
		{
			cells.add(new String[]
			{
				"icon.xmas_miracle_i00",
				"Buffs Premium",
				"Exclusive advantages",
				"Premium Buffs",
				"bypass _cbbbuffer"
			});
		}
		if (Config.COMMUNITYBOARD_DROP_SEARCH)
		{
			cells.add(new String[]
			{
				"icon.action046",
				"Drop Search",
				"Find any item",
				"Pesquisar",
				"bypass _bbstop;dropsearch/main.html"
			});
		}
		if (Config.COMMUNITYBOARD_ENABLE_TELEPORTS)
		{
			cells.add(new String[]
			{
				"Icon.Action053",
				"Teleport",
				"Travel the world",
				"Gatekeeper",
				"bypass _bbstop;gatekeeper/main.html"
			});
		}
		
		if (cells.isEmpty())
		{
			return "";
		}
		
		// Each cell shares the 720px row equally (max 4 cells).
		final int count = cells.size();
		final int cellWidth = 720 / count;
		final int innerWidth = cellWidth - 4; // accounts for cellpadding/spacing.
		
		final StringBuilder sb = new StringBuilder(2048);
		sb.append("<table width=720 cellpadding=2 cellspacing=2 border=0><tr>");
		for (String[] c : cells)
		{
			sb.append("<td fixwidth=").append(cellWidth).append(" fixheight=130 align=center valign=top background=\"L2UI_CT1.Windows_DF_Drawer_Bg\">");
			sb.append("<table width=").append(innerWidth).append(" height=130 cellpadding=0 cellspacing=0 border=0>");
			sb.append("<tr><td height=8></td></tr>");
			sb.append("<tr><td align=center><img src=\"").append(c[0]).append("\" width=32 height=32></td></tr>");
			sb.append("<tr><td align=center>");
			sb.append("<font color=\"F2C75C\">").append(c[1]).append("</font><br1>");
			sb.append("<font color=\"6B6B6B\">").append(c[2]).append("</font>");
			sb.append("</td></tr>");
			sb.append("<tr><td align=center>");
			sb.append("<button value=\"").append(c[3]).append("\" action=\"").append(c[4]).append("\" width=140 height=24 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
			sb.append("</td></tr>");
			sb.append("</table>");
			sb.append("</td>");
		}
		sb.append("</tr></table>");
		return sb.toString();
	}
	
	/**
	 * Builds the full row containing the "Raid Boss Watch" panel and the "Hot Features" panel. Each panel is rendered only if its corresponding config is enabled. When only one of them is enabled, that panel expands laterally to occupy the full 720px width. When both are disabled, an empty string
	 * is returned (the entire row is omitted from the page).
	 * @return the HTML fragment for the row, or an empty string if both panels are disabled
	 */
	private static String buildBossHotSection()
	{
		final boolean epicEnabled = Config.COMMUNITYBOARD_EPIC_STATUS;
		final boolean hotEnabled = Config.COMMUNITYBOARD_HOT_FEATURES_ENABLED;
		if (!epicEnabled && !hotEnabled)
		{
			return "";
		}
		
		// When both are enabled, keep the original side-by-side dimensions.
		// When only one is enabled, that panel takes the full 720px row.
		final int bossOuter = epicEnabled ? (hotEnabled ? 336 : 720) : 0;
		final int hotOuter = hotEnabled ? (epicEnabled ? 372 : 720) : 0;
		
		final StringBuilder sb = new StringBuilder(4096);
		sb.append("<table width=732 height=210 cellpadding=2 cellspacing=2 border=0><tr>");
		if (epicEnabled)
		{
			sb.append(buildBossWatchBlock(bossOuter));
		}
		if (hotEnabled)
		{
			sb.append(buildHotFeaturesBlock(hotOuter));
		}
		sb.append("</tr></table><br1>");
		return sb.toString();
	}
	
	/**
	 * Builds the "Raid Boss Watch" panel as a single &lt;td&gt; cell with the given outer width.
	 * @param outerWidth target width for the outer cell (e.g. 336 when paired with hot features, 720 when alone)
	 * @return the HTML fragment for the cell
	 */
	private static String buildBossWatchBlock(int outerWidth)
	{
		final int innerWidth = outerWidth - 6;
		// When the panel is wide (alone), give the boss-name and status columns more room.
		final boolean wide = outerWidth >= 720;
		final int nameColWidth = wide ? (innerWidth / 2) - 10 : 140;
		final int statusColWidth = wide ? (innerWidth / 2) - 10 : 180;
		
		final StringBuilder sb = new StringBuilder(2048);
		sb.append("<td fixwidth=").append(outerWidth).append(" valign=top background=\"L2UI_CT1.Windows_DF_Drawer_Bg\">");
		sb.append("<table width=").append(outerWidth).append(" cellpadding=2 cellspacing=0 border=0>");
		sb.append("<tr><td height=10></td></tr>");
		sb.append("<tr><td height=24 align=center bgcolor=\"2A2418\"><font name=\"hs9\" color=\"F2C75C\">Raid Boss Status</font></td></tr>");
		sb.append("<tr><td><table width=").append(innerWidth).append(" cellpadding=2 cellspacing=1 border=0>");
		appendBossRow(sb, "1F1F1F", "Antharas", "%antharas_status%", nameColWidth, statusColWidth);
		appendBossRow(sb, "161616", "Valakas", "%valakas_status%", nameColWidth, statusColWidth);
		appendBossRow(sb, "1F1F1F", "Baium", "%baium_status%", nameColWidth, statusColWidth);
		appendBossRow(sb, "161616", "Beleth", "%beleth_status%", nameColWidth, statusColWidth);
		appendBossRow(sb, "1F1F1F", "Core", "%core_status%", nameColWidth, statusColWidth);
		appendBossRow(sb, "161616", "Orfen", "%orfen_status%", nameColWidth, statusColWidth);
		appendBossRow(sb, "1F1F1F", "Queen Ant", "%queenant_status%", nameColWidth, statusColWidth);
		sb.append("</table></td></tr>");
		sb.append("<tr><td height=20 align=center bgcolor=\"111111\">&nbsp;</td></tr>");
		sb.append("<tr><td height=20 align=center bgcolor=\"111111\"><font color=\"6B6B6B\">Next update:</font> <font color=\"CDB67F\">%boss_next_update_time%</font></td></tr>");
		sb.append("</table></td>");
		return sb.toString();
	}
	
	private static void appendBossRow(StringBuilder sb, String bg, String name, String statusPlaceholder, int nameWidth, int statusWidth)
	{
		sb.append("<tr>");
		sb.append("<td bgcolor=\"").append(bg).append("\" width=").append(nameWidth).append(" height=20 align=center><font color=\"CDB67F\">").append(name).append("</font></td>");
		sb.append("<td bgcolor=\"").append(bg).append("\" width=").append(statusWidth).append(" height=20 align=center>").append(statusPlaceholder).append("</td>");
		sb.append("</tr>");
	}
	
	private static String buildHotFeaturesBlock(int outerWidth)
	{
		final StringBuilder sb = new StringBuilder(2048);
		sb.append("<td fixwidth=").append(outerWidth).append(" valign=top background=\"L2UI_CT1.Windows_DF_Drawer_Bg\">");
		
		// Build the list of enabled cells.
		// Ordem fixa de exibicao (linha 1: Ranking, BlackSmith, Merchant; linha 2: Premium, Delevel, Recompensa).
		final List<String[]> cells = new ArrayList<>(6);
		// Ranking (TopBoard built-in).
		if (Config.COMMUNITYBOARD_PLAYER_RANKING)
		{
			cells.add(new String[]
			{
				"BranchSys.br_rank_last_normal",
				"Ranking",
				"Top Players",
				"bypass _bbstopboard"
			});
		}
		// BlackSmith (acessivel quando multisells estao habilitados, pois usa _bbsmultisell internamente).
		if (Config.COMMUNITYBOARD_BLACKSMITH)
		{
			cells.add(new String[]
			{
				"L2UI.Crystalize_active",
				"BlackSmith",
				"Access",
				"bypass _bbstop;merchant/blacksmithMain.html"
			});
		}
		if (Config.COMMUNITYBOARD_MERCHANT)
		{
			cells.add(new String[]
			{
				"Icon.skill0629",
				"Merchant",
				"Shop",
				"bypass _bbstop;merchant/gmshop.html"
			});
		}
		if (Config.COMMUNITY_PREMIUM_SYSTEM_ENABLED)
		{
			cells.add(new String[]
			{
				"BranchSys2.br_spirit_rune_i00",
				"Premium",
				"Access",
				"bypass _bbstop;premium/main.html"
			});
		}
		if (Config.COMMUNITYBOARD_ENABLE_DELEVEL)
		{
			cells.add(new String[]
			{
				"icon.skill1411",
				"Delevel",
				"Access",
				"bypass _bbstop;delevel/main.html"
			});
		}
		if (Config.DAILY_REWARD_ENABLED)
		{
			cells.add(new String[]
			{
				"BranchSys.br_bright_gold_box_i00",
				"Daily Reward",
				"Colect",
				"bypass -h voice .dailyreward"
			});
		}
		
		// Quando o painel ocupa toda a largura, distribui mais celulas por linha.
		final int cellsPerRow = outerWidth >= 720 ? 6 : 3;
		final int cellWidth = (outerWidth - 12) / cellsPerRow;
		final int buttonWidth = cellWidth - 24;
		
		sb.append("<table width=").append(outerWidth).append(" cellpadding=2 cellspacing=0 border=0>");
		sb.append("<tr><td height=15></td></tr>");
		
		// Layout em linhas com cores alternadas.
		final String[] bgColors =
		{
			"161616",
			"1F1F1F"
		};
		int cellIndex = 0;
		for (int i = 0; i < cells.size(); i += cellsPerRow)
		{
			sb.append("<tr>");
			for (int j = i; j < Math.min(i + cellsPerRow, cells.size()); j++)
			{
				final String[] c = cells.get(j);
				sb.append("<td fixwidth=").append(cellWidth).append(" fixheight=82 align=center valign=top bgcolor=\"").append(bgColors[cellIndex % 2]).append("\">");
				sb.append("<br1>");
				sb.append("<img src=\"").append(c[0]).append("\" width=32 height=32><br>");
				sb.append("<font color=\"F2C75C\">").append(c[1]).append("</font><br1>");
				sb.append("<button value=\"").append(c[2]).append("\" action=\"").append(c[3]).append("\" width=").append(buttonWidth).append(" height=22 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
				sb.append("</td>");
				cellIndex++;
			}
			sb.append("</tr>");
		}
		sb.append("</table>");
		sb.append("</td>");
		return sb.toString();
	}
	
	/**
	 * Returns the list of supported Community Board commands.
	 * @return Array of command strings
	 */
	@Override
	public String[] getCommunityBoardCommands()
	{
		final List<String> commands = new ArrayList<>();
		commands.addAll(Arrays.asList(COMMANDS));
		commands.addAll(Arrays.asList(CUSTOM_COMMANDS));
		commands.addAll(Arrays.asList(topBoard.getCommunityBoardCommands()));
		return commands.stream().filter(Objects::nonNull).toArray(String[]::new);
	}
	
	/**
	 * Parses and executes a Community Board command for the given player.
	 * @param command The command string
	 * @param player The player instance
	 * @return true if the command was handled, false otherwise
	 */
	@Override
	public boolean parseCommunityBoardCommand(String command, Player player)
	{
		if (!Config.COMMUNITYBOARD_ENABLED)
		{
			player.sendPacket(SystemMessageId.THE_COMMUNITY_SERVER_IS_CURRENTLY_OFFLINE);
			return false;
		}
		// Old custom conditions check move to here
		if (Config.COMMUNITYBOARD_COMBAT_DISABLED && COMBAT_CHECK.test(command, player))
		{
			player.sendMessage("You can't use the Community Board right now.");
			return false;
		}
		
		if (KARMA_CHECK.test(player))
		{
			player.sendMessage("Players with Karma cannot use the Community Board.");
			return false;
		}
		
		if (Config.COMMUNITYBOARD_PEACE_ONLY && !player.isInsideZone(ZoneId.PEACE))
		{
			player.sendMessage("Community Board cannot be used out of peace zone.");
			return false;
		}
		
		String returnHtml = null;
		if (command.equals("_bbshome") || command.equals("_bbstop"))
		{
			CommunityBoardHandler.getInstance().addBypass(player, "Home", command);
			returnHtml = HtmCache.getInstance().getHtm(player, "data/html/CommunityBoard/home.html");
			
			// Injeta os blocos dinamicos PRIMEIRO, antes das substituicoes de placeholders
			// individuais (%antharas_status%, %boss_next_update_time%, etc.), pois esses
			// placeholders existem dentro do HTML gerado por buildBossHotSection().
			returnHtml = returnHtml.replace("%boss_hot_section%", buildBossHotSection());
			returnHtml = returnHtml.replace("%top_features_block%", buildTopFeaturesBlock());
			returnHtml = returnHtml.replace("%tip_block%", TipsBoard.getInstance().getActiveTipHtml());
			
			returnHtml = returnHtml.replace("%fav_count%", Integer.toString(getFavoriteCount(player)));
			returnHtml = returnHtml.replace("%region_count%", Integer.toString(getRegionCount(player)));
			returnHtml = returnHtml.replace("%clan_count%", Integer.toString(ClanTable.getInstance().getClanCount()));
			
			// Epic boss status replacement
			returnHtml = returnHtml.replace("%antharas_status%", HomeBoard.getBossStatus(29068));
			returnHtml = returnHtml.replace("%baium_status%", HomeBoard.getBossStatus(29020));
			returnHtml = returnHtml.replace("%beleth_status%", HomeBoard.getBossStatus(29118));
			returnHtml = returnHtml.replace("%core_status%", HomeBoard.getBossStatus(29006));
			returnHtml = returnHtml.replace("%orfen_status%", HomeBoard.getBossStatus(29014));
			returnHtml = returnHtml.replace("%queenant_status%", HomeBoard.getBossStatus(29001));
			returnHtml = returnHtml.replace("%valakas_status%", HomeBoard.getBossStatus(29028));
			// Add last and next update times
			returnHtml = returnHtml.replace("%boss_update_time%", HomeBoard.getLastUpdateTime());
			returnHtml = returnHtml.replace("%boss_next_update_time%", HomeBoard.getNextUpdateTime());
		}
		else if (command.startsWith("_bbstopboard"))
		{
			return topBoard.parseCommunityBoardCommand(command, player);
		}
		else if (command.startsWith("_bbstop;"))
		{
			final String path = command.replace("_bbstop;", "");
			if ((path.length() > 0) && path.endsWith(".html"))
			{
				returnHtml = HtmCache.getInstance().getHtm(player, "data/html/CommunityBoard/" + path);
			}
		}
		else if (command.startsWith("_bbsmultisell"))
		{
			final String fullBypass = command.replace("_bbsmultisell;", "");
			final String[] buypassOptions = fullBypass.split(",");
			final int multisellId = Integer.parseInt(buypassOptions[0]);
			final String page = buypassOptions[1];
			returnHtml = HtmCache.getInstance().getHtm(player, "data/html/CommunityBoard/" + page + ".html");
			MultisellData.getInstance().separateAndSend(multisellId, player, null, false);
		}
		else if (command.startsWith("_bbsexcmultisell"))
		{
			final String fullBypass = command.replace("_bbsexcmultisell;", "");
			final String[] buypassOptions = fullBypass.split(",");
			final int multisellId = Integer.parseInt(buypassOptions[0]);
			final String page = buypassOptions[1];
			returnHtml = HtmCache.getInstance().getHtm(player, "data/html/CommunityBoard/" + page + ".html");
			MultisellData.getInstance().separateAndSend(multisellId, player, null, true);
		}
		else if (command.startsWith("_bbssell"))
		{
			final String page = command.replace("_bbssell;", "");
			returnHtml = HtmCache.getInstance().getHtm(player, "data/html/CommunityBoard/" + page + ".html");
			player.sendPacket(new BuyList(BuyListData.getInstance().getBuyList(423), player.getAdena(), 0));
			player.sendPacket(new ExBuySellList(player, false));
		}
		else if (command.startsWith("_bbswarhouse"))
		{
			if (command.equals("_bbswarhouse:chardeposit"))
			{
				player.sendPacket(ActionFailed.STATIC_PACKET);
				player.setActiveWarehouse(player.getWarehouse());
				if (player.getWarehouse().getSize() == player.getWareHouseLimit())
				{
					player.sendPacket(SystemMessageId.YOUR_WAREHOUSE_IS_FULL);
					return false;
				}
				
				player.setWarehouseOpenedViaCommunityBoard(true);
				player.setInventoryBlockingStatus(true);
				player.sendPacket(new WareHouseDepositList(player, 1));
				returnHtml = HtmCache.getInstance().getHtm(player, "data/html/CommunityBoard/merchant/warehouse.html");
			}
			else if (command.equals("_bbswarhouse:clandeposit"))
			{
				if (player.isEnchanting())
				{
					return false;
				}
				
				if (player.getClan() == null)
				{
					player.sendPacket(SystemMessageId.YOU_ARE_NOT_A_CLAN_MEMBER_AND_CANNOT_PERFORM_THIS_ACTION);
					return false;
				}
				
				player.sendPacket(ActionFailed.STATIC_PACKET);
				player.setActiveWarehouse(player.getClan().getWarehouse());
				if (player.getClan().getLevel() == 0)
				{
					player.sendPacket(SystemMessageId.ONLY_CLANS_OF_CLAN_LEVEL_1_OR_HIGHER_CAN_USE_A_CLAN_WAREHOUSE);
					return false;
				}
				
				player.setActiveWarehouse(player.getClan().getWarehouse());
				player.setWarehouseOpenedViaCommunityBoard(true);
				player.setInventoryBlockingStatus(true);
				player.sendPacket(new WareHouseDepositList(player, 4));
			}
			else if (command.equals("_bbswarhouse:charwithdraw"))
			{
				player.sendPacket(ActionFailed.STATIC_PACKET);
				player.setActiveWarehouse(player.getWarehouse());
				if (player.getActiveWarehouse().getSize() == 0)
				{
					player.sendPacket(SystemMessageId.YOU_HAVE_NOT_DEPOSITED_ANY_ITEMS_IN_YOUR_WAREHOUSE);
					return false;
				}
				
				player.setWarehouseOpenedViaCommunityBoard(true);
				player.sendPacket(new WareHouseWithdrawalList(player, 1));
			}
			else if (command.equals("_bbswarhouse:clanwithdraw"))
			{
				if (player.isEnchanting())
				{
					return false;
				}
				
				if (player.getClan() == null)
				{
					player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_THE_RIGHT_TO_USE_THE_CLAN_WAREHOUSE);
					return false;
				}
				
				player.sendPacket(ActionFailed.STATIC_PACKET);
				if (!player.hasAccess(ClanAccess.ACCESS_WAREHOUSE))
				{
					player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_THE_RIGHT_TO_USE_THE_CLAN_WAREHOUSE);
					return false;
				}
				
				if (player.getClan().getLevel() == 0)
				{
					player.sendPacket(SystemMessageId.ONLY_CLANS_OF_CLAN_LEVEL_1_OR_HIGHER_CAN_USE_A_CLAN_WAREHOUSE);
					return false;
				}
				
				player.setActiveWarehouse(player.getClan().getWarehouse());
				player.setWarehouseOpenedViaCommunityBoard(true);
				player.setInventoryBlockingStatus(true);
				
				player.sendPacket(new WareHouseWithdrawalList(player, 4));
			}
		}
		else if (command.startsWith("_bbsaugment"))
		{
			if (command.equals("_bbsaugment;add"))
			{
				player.sendPacket(SystemMessageId.SELECT_THE_ITEM_TO_BE_AUGMENTED);
				player.sendPacket(ExShowVariationMakeWindow.STATIC_PACKET);
				player.cancelActiveTrade();
				returnHtml = HtmCache.getInstance().getHtm(player, "data/html/CommunityBoard/merchant/blacksmith.html");
			}
			else if (command.equals("_bbsaugment;remove"))
			{
				player.sendPacket(SystemMessageId.SELECT_THE_ITEM_FROM_WHICH_YOU_WISH_TO_REMOVE_AUGMENTATION);
				player.sendPacket(ExShowVariationCancelWindow.STATIC_PACKET);
				player.cancelActiveTrade();
			}
		}
		else if (command.startsWith("_bbsdraw"))
		{
			if (command.equals("_bbsdraw:add"))
			{
				player.sendPacket(new HennaEquipList(player));
			}
			else if (command.equals("_bbsdraw:remove"))
			{
				for (Henna henna : player.getHennaList())
				{
					if (henna != null)
					{
						returnHtml = HtmCache.getInstance().getHtm(player, "data/html/CommunityBoard/merchant/symbolMaker.html");
						player.sendPacket(new HennaRemoveList(player));
						break;
					}
				}
			}
		}
		else if (command.startsWith("_bbsteleport"))
		{
			final String teleBuypass = command.replace("_bbsteleport;", "");
			if (player.getInventory().getInventoryItemCount(Config.COMMUNITYBOARD_CURRENCY, -1) < Config.COMMUNITYBOARD_TELEPORT_PRICE)
			{
				player.sendMessage("Not enough currency!");
			}
			else if (Config.COMMUNITY_AVAILABLE_TELEPORTS.get(teleBuypass) != null)
			{
				player.disableAllSkills();
				player.sendPacket(new ShowBoard());
				player.destroyItemByItemId(ItemProcessType.FEE, Config.COMMUNITYBOARD_CURRENCY, Config.COMMUNITYBOARD_TELEPORT_PRICE, player, true);
				player.setIn7sDungeon(false);
				player.setInstanceId(0);
				player.teleToLocation(Config.COMMUNITY_AVAILABLE_TELEPORTS.get(teleBuypass), 0);
				ThreadPool.schedule(player::enableAllSkills, 3000);
			}
		}
		else if (command.equals("_bbsdelevel"))
		{
			if (player.getInventory().getInventoryItemCount(Config.COMMUNITYBOARD_CURRENCY, -1) < Config.COMMUNITYBOARD_DELEVEL_PRICE)
			{
				player.sendMessage("Not enough currency!");
			}
			else if (player.getLevel() == 1)
			{
				player.sendMessage("You are at minimum level!");
			}
			else
			{
				player.destroyItemByItemId(ItemProcessType.FEE, Config.COMMUNITYBOARD_CURRENCY, Config.COMMUNITYBOARD_DELEVEL_PRICE, player, true);
				final int newLevel = player.getLevel() - 1;
				player.setExp(ExperienceData.getInstance().getExpForLevel(newLevel));
				player.getStat().setLevel((byte) newLevel);
				player.setCurrentHpMp(player.getMaxHp(), player.getMaxMp());
				player.setCurrentCp(player.getMaxCp());
				player.broadcastUserInfo();
				player.checkPlayerSkills(); // Adjust skills according to new level.
				returnHtml = HtmCache.getInstance().getHtm(player, "data/html/CommunityBoard/delevel/complete.html");
			}
		}
		else if (command.startsWith("_bbspremium"))
		{
			final String fullBypass = command.replace("_bbspremium;", "");
			final String[] buypassOptions = fullBypass.split(",");
			final int premiumDays = Integer.parseInt(buypassOptions[0]);
			if ((premiumDays < 1) || (premiumDays > 30) || (player.getInventory().getInventoryItemCount(Config.COMMUNITY_PREMIUM_COIN_ID, -1) < (Config.COMMUNITY_PREMIUM_PRICE_PER_DAY * premiumDays)))
			{
				player.sendMessage("Not enough currency!");
			}
			else
			{
				player.destroyItemByItemId(ItemProcessType.FEE, Config.COMMUNITY_PREMIUM_COIN_ID, Config.COMMUNITY_PREMIUM_PRICE_PER_DAY * premiumDays, player, true);
				PremiumManager.getInstance().addPremiumTime(player.getAccountName(), premiumDays, TimeUnit.DAYS);
				player.sendMessage("Your account will now have premium status until " + new SimpleDateFormat("dd.MM.yyyy HH:mm").format(PremiumManager.getInstance().getPremiumExpiration(player.getAccountName())) + ".");
				if (EventsConfig.PC_CAFE_RETAIL_LIKE)
				{
					PcCafePointsManager.getInstance().run(player);
				}
				
				returnHtml = HtmCache.getInstance().getHtm(player, "data/html/CommunityBoard/premium/thankyou.html");
			}
		}
		
		if (returnHtml != null)
		{
			CommunityBoardHandler.separateAndSend(returnHtml, player);
		}
		
		return false;
	}
	
	/**
	 * Gets the Favorite links for the given player.
	 * @param player the player
	 * @return the favorite links count
	 */
	private static int getFavoriteCount(Player player)
	{
		int count = 0;
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(COUNT_FAVORITES))
		{
			ps.setInt(1, player.getObjectId());
			try (ResultSet rs = ps.executeQuery())
			{
				if (rs.next())
				{
					count = rs.getInt("favorites");
				}
			}
		}
		catch (Exception _)
		{
			LOG.warning(FavoriteBoard.class.getSimpleName() + ": Couldn't load favorites count for " + player);
		}
		
		return count;
	}
	
	/**
	 * Gets the registered regions count for the given player.
	 * @param player the player
	 * @return the registered regions count
	 */
	private static int getRegionCount(Player player)
	{
		return 0; // TODO: Implement.
	}
	
	/**
	 * Returns the status of the boss for Community Board HTML.
	 * @param bossId Boss NPC ID
	 * @return HTML string: 'Is Alive' (green) or 'Is Dead' (red)
	 */
	public static String getBossStatus(int bossId)
	{
		return BOSS_STATUS_CACHE.getOrDefault(bossId, "?");
	}
}
