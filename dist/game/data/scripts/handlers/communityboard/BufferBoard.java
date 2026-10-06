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
package handlers.communityboard;

import static com.l2journey.gameserver.util.FormatUtil.formatAdena;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Logger;

import com.l2journey.Config;
import com.l2journey.commons.database.DatabaseFactory;
import com.l2journey.gameserver.data.xml.SkillData;
import com.l2journey.gameserver.handler.CommunityBoardHandler;
import com.l2journey.gameserver.handler.IParseBoardHandler;
import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.Summon;
import com.l2journey.gameserver.model.actor.instance.Cubic;
import com.l2journey.gameserver.model.actor.instance.Servitor;
import com.l2journey.gameserver.model.actor.stat.PlayerStat;
import com.l2journey.gameserver.model.actor.status.PlayerStatus;
import com.l2journey.gameserver.model.effects.EffectType;
import com.l2journey.gameserver.model.events.Containers;
import com.l2journey.gameserver.model.events.EventType;
import com.l2journey.gameserver.model.events.holders.actor.player.OnPlayerLogout;
import com.l2journey.gameserver.model.events.listeners.ConsumerEventListener;
import com.l2journey.gameserver.model.item.enums.ItemProcessType;
import com.l2journey.gameserver.model.skill.Skill;
import com.l2journey.gameserver.model.skill.skillVariation.ServitorShareConditions;
import com.l2journey.gameserver.network.SystemMessageId;
import com.l2journey.gameserver.network.serverpackets.MagicSkillUse;
import com.l2journey.gameserver.network.serverpackets.SetSummonRemainTime;

/**
 * Buffer completo da Community Board com suporte total a schemes. Usa as mesmas tabelas do banco do NpcBuffer (npcbuffer_buff_list, npcbuffer_scheme_list, npcbuffer_scheme_contents). Todas as configuracoes sao lidas de NpcBuffer.ini via {@link Config}.
 * @author KingHanker
 */
public class BufferBoard implements IParseBoardHandler
{
	private static final Logger LOG = Logger.getLogger(BufferBoard.class.getName());
	
	private static final String TITLE = "Community Buffer";
	private static final String[] COMMANDS =
	{
		"_bbsbuffer"
	};
	
	private static final int MAX_SCHEME_BUFFS = Config.BUFFS_MAX_AMOUNT;
	private static final int MAX_SCHEME_DANCES = Config.DANCES_MAX_AMOUNT;
	private static final int BUFFS_PER_PAGE = 20;
	
	// Configuracao do tempo dos buffs
	private static final int BUFFTIME = Config.BUFF_TIME * 60; // 60 min.
	
	// Rotulos de classe do conjunto de buffs (gerenciamento GM)
	private static final String SET_FIGHTER = "Fighter";
	private static final String SET_MAGE = "Mage";
	private static final String SET_ALL = "All";
	private static final String SET_NONE = "None";
	
	// IDs de skills visuais para animacoes
	private static final int SKILL_HEAL = 6696;
	private static final int SKILL_BUFF_1 = 1411;
	private static final int SKILL_BUFF_2 = 6662;
	
	// Modo de buff de pet por jogador
	private static final Map<Integer, Boolean> PET_MODE = new ConcurrentHashMap<>();
	
	// Flag por jogador para exibir a mensagem de 'nenhum pet invocado' no lugar do botao To Pet
	private static final Map<Integer, Boolean> SHOW_NO_PET = new ConcurrentHashMap<>();
	
	// Cache de skills para desempenho
	private static final Map<Integer, Skill> SKILL_CACHE = new ConcurrentHashMap<>();
	
	// Listener de limpeza para logout do jogador
	private static final Consumer<OnPlayerLogout> ON_PLAYER_LOGOUT = event ->
	{
		final int objId = event.getPlayer().getObjectId();
		PET_MODE.remove(objId);
		SHOW_NO_PET.remove(objId);
	};
	
	public BufferBoard()
	{
		Containers.Players().addListener(new ConsumerEventListener(Containers.Players(), EventType.ON_PLAYER_LOGOUT, ON_PLAYER_LOGOUT, this));
	}
	
	@Override
	public String[] getCommunityBoardCommands()
	{
		return COMMANDS;
	}
	
	// =========================================================
	// ROTEAMENTO DE COMANDOS
	// =========================================================
	
	@Override
	public boolean parseCommunityBoardCommand(String command, Player player)
	{
		if (!Config.COMMUNITYBOARD_ENABLED)
		{
			player.sendPacket(SystemMessageId.THE_COMMUNITY_SERVER_IS_CURRENTLY_OFFLINE);
			return false;
		}
		
		// Validacoes de acesso
		if (!Config.BUFF_WITH_KARMA && (player.getKarma() > 0))
		{
			sendHtml(player, showInfo("Info", "You have too much <font color=FF0000>karma!</font><br>Come back when you don't have any karma!"));
			return false;
		}
		if (player.isInOlympiadMode())
		{
			sendHtml(player, showInfo("Info", "You can't use the buffer while in the <font color=FF0000>Olympiad!</font>"));
			return false;
		}
		if (player.isOnEvent())
		{
			sendHtml(player, showInfo("Info", "You can't use the buffer while in an <font color=FF0000>Event!</font>"));
			return false;
		}
		if (player.getLevel() < Config.MIN_LEVEL)
		{
			sendHtml(player, showInfo("Info", "Your level is too low!<br>You need at least level <font color=LEVEL>" + Config.MIN_LEVEL + "</font>."));
			return false;
		}
		if (!Config.BUFF_WITH_FLAG && (player.getPvpFlag() > 0))
		{
			sendHtml(player, showInfo("Info", "You can't use the buffer while <font color=800080>flagged!</font>"));
			return false;
		}
		if (player.isInCombat())
		{
			sendHtml(player, showInfo("Info", "You can't use the buffer while in <font color=FF0000>combat!</font>"));
			return false;
		}
		
		final String params = command.startsWith("_bbsbuffer;") ? command.substring(11) : "";
		String html = null;
		
		try
		{
			if (params.isEmpty())
			{
				SHOW_NO_PET.remove(player.getObjectId());
				html = buildMainPage(player);
			}
			else if (params.equals("togglePet"))
			{
				if (player.getSummon() == null)
				{
					SHOW_NO_PET.put(player.getObjectId(), true);
					html = buildMainPage(player);
				}
				else
				{
					SHOW_NO_PET.remove(player.getObjectId());
					PET_MODE.put(player.getObjectId(), !isPetMode(player));
					html = buildMainPage(player);
				}
			}
			else if (params.startsWith("view;"))
			{
				html = buildCategoryPage(params.substring(5));
			}
			else if (params.startsWith("give;"))
			{
				html = handleGiveBuff(player, params.substring(5));
			}
			else if (params.equals("heal"))
			{
				html = handleHeal(player);
			}
			else if (params.equals("removeBuffs"))
			{
				html = handleRemoveBuffs(player);
			}
			else if (params.equals("castSet"))
			{
				html = handleCastBuffSet(player);
			}
			else if (params.startsWith("cast;"))
			{
				html = handleCastScheme(player, params.substring(5));
			}
			else if (params.equals("create_1"))
			{
				html = createSchemeForm();
			}
			else if (params.startsWith("create "))
			{
				html = handleCreateScheme(player, params.substring(7));
			}
			else if (params.equals("edit_1"))
			{
				html = editSchemeList(player);
			}
			else if (params.equals("delete_1"))
			{
				html = deleteSchemeList(player);
			}
			else if (params.startsWith("delete_c;"))
			{
				final String[] dp = params.substring(9).split(";", 2);
				html = confirmDeleteScheme(dp[0], dp.length > 1 ? dp[1] : "?");
			}
			else if (params.startsWith("delete;"))
			{
				html = handleDeleteScheme(player, params.substring(7));
			}
			else if (params.startsWith("manage;"))
			{
				html = getSchemeOptions(player, params.substring(7));
			}
			else if (params.startsWith("addView;"))
			{
				final String[] ap = params.substring(8).split(";", 2);
				html = viewSchemeBuffs(player, ap[0], ap.length > 1 ? ap[1] : "1", "add");
			}
			else if (params.startsWith("removeView;"))
			{
				final String[] rp = params.substring(11).split(";", 2);
				html = viewSchemeBuffs(player, rp[0], rp.length > 1 ? rp[1] : "1", "remove");
			}
			else if (params.startsWith("addBuff;"))
			{
				html = handleAddBuffToScheme(player, params.substring(8));
			}
			else if (params.startsWith("removeBuff;"))
			{
				html = handleRemoveBuffFromScheme(player, params.substring(11));
			}
			// GM Management routes
			else if (params.equals("gmManage") && player.isGM())
			{
				html = gmViewAllBuffTypes();
			}
			else if (params.startsWith("gmEditList;") && player.isGM())
			{
				final String[] gp = params.substring(11).split(";", 3);
				html = gmViewAllBuffs(gp[0], gp.length > 1 ? gp[1] : gp[0], gp.length > 2 ? gp[2] : "1");
			}
			else if (params.startsWith("gmEditBuff;") && player.isGM())
			{
				final String[] gp = params.substring(11).split(";", 3);
				final String actionPage = gp.length > 1 ? gp[1] : "1-1";
				final String[] ap = actionPage.split("-", 2);
				if (!gmManageSelectedBuff(gp[0], ap[0]))
				{
					html = showInfo("Error", "Failed to update buff. Please try again later.");
				}
				else
				{
					final String page = ap.length > 1 ? ap[1] : "1";
					final String type = gp.length > 2 ? gp[2] : "buff";
					html = gmViewAllBuffs(type, type, page);
				}
			}
			else if (params.startsWith("gmChangeSet;") && player.isGM())
			{
				final String[] gp = params.substring(12).split(";", 2);
				final String skillPos = gp[0];
				// Format: "PAGE SELECTED_VALUE" (space-separated for client $var substitution)
				final String pageAndValue = gp.length > 1 ? gp[1] : "1 3";
				final int spaceIdx = pageAndValue.indexOf(' ');
				final String page;
				final String newVal;
				if (spaceIdx != -1)
				{
					page = pageAndValue.substring(0, spaceIdx);
					newVal = pageAndValue.substring(spaceIdx + 1);
				}
				else
				{
					page = "1";
					newVal = pageAndValue;
				}
				html = gmManageSelectedSet(skillPos, newVal, page);
			}
			else
			{
				html = buildMainPage(player);
			}
		}
		catch (Exception e)
		{
			LOG.warning("BufferBoard error for " + player.getName() + ": " + e.getMessage());
			html = buildMainPage(player);
		}
		
		if (html != null)
		{
			sendHtml(player, html);
		}
		
		return false;
	}
	
	// =========================================================
	// MAIN PAGE
	// =========================================================
	
	private String buildMainPage(Player player)
	{
		final boolean hasSummon = player.getSummon() != null;
		boolean petMode = isPetMode(player);
		
		// Auto-reset pet mode if summon is no longer active
		if (petMode && !hasSummon)
		{
			PET_MODE.put(player.getObjectId(), false);
			petMode = false;
		}
		
		final String targetName;
		if (petMode && hasSummon)
		{
			final String summonName = player.getSummon().getName();
			targetName = ((summonName == null) || summonName.trim().isEmpty()) ? "No Name" : summonName;
		}
		else
		{
			targetName = player.getName();
		}
		final List<String[]> schemes = Config.ENABLE_SCHEME_SYSTEM ? getPlayerSchemes(player) : new ArrayList<>();
		
		final StringBuilder html = new StringBuilder();
		html.append("<html noscrollbar><title>").append(TITLE).append("</title><body><br>");
		html.append("<table width=755><tr><td align=center>");
		html.append("<table border=0 cellpadding=0 cellspacing=0 width=769 height=492 background=\"l2ui_ct1.SlideShow_DF_Credit_05\">");
		
		// === HEADER ===
		html.append("<tr><td height=50 align=center><br>");
		html.append("<table border=0 width=745 height=50><tr><td align=center>");
		html.append("<table border=0 width=745 height=46 cellspacing=4 cellpadding=3 background=\"l2ui_ct1.ComboBox_DF_Dropmenu_Bg\">");
		html.append("<tr>");
		html.append("<td width=40 align=right valign=top><img src=\"icon.skill6319\" width=32 height=32></td>");
		html.append("<td width=260 align=left valign=top>");
		html.append("<font name=hs12 color=\"LEVEL\">Community Buffer</font><br1>");
		if (!Config.FREE_BUFFS)
		{
			html.append("<font color=FFFFFF name=__SYSTEMWORLDFONT>Buff Price: ").append(formatAdena(Config.BUFF_PRICE)).append(" Adena</font>");
		}
		else
		{
			html.append("<font color=FFFFFF name=__SYSTEMWORLDFONT>All buffs are free!</font>");
		}
		html.append("</td>");
		html.append("<td width=170 align=center valign=center>");
		html.append("<table border=0 cellspacing=2 cellpadding=0><tr>");
		final boolean showNoPetMsg = SHOW_NO_PET.getOrDefault(player.getObjectId(), false);
		if (showNoPetMsg && !hasSummon)
		{
			html.append("<td><button value=\"\" action=\"bypass _bbsbuffer;togglePet\" width=35 height=35 back=\"L2UI_CT1.SystemMenuWnd_df_ReStart\" fore=\"L2UI_CT1.SystemMenuWnd_df_ReStart\"></td>");
			html.append("<td width=90 align=center><font color=FF6666 name=__SYSTEMWORLDFONT>No pet summoned!</font></td>");
		}
		else
		{
			SHOW_NO_PET.remove(player.getObjectId());
			if (hasSummon)
			{
				html.append("<td><button value=\"").append(petMode ? "To Player" : "To Pet").append("\" action=\"bypass _bbsbuffer;togglePet\" width=90 height=37 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
			}
			else
			{
				html.append("<td><button value=\"To Pet\" action=\"bypass _bbsbuffer;togglePet\" width=90 height=37 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
			}
		}
		
		html.append("</tr></table>");
		html.append("</td>");
		html.append("<td width=60 align=right><font name=hs12 color=99BBFF>Target:</font></td>");
		html.append("<td width=120 align=left><font name=hs12 color=LEVEL> ").append(targetName).append("</font></td>");
		html.append("</tr></table>");
		html.append("</td></tr></table>");
		html.append("</td></tr>");
		
		// === MAIN CONTENT (3 columns) ===
		html.append("<tr><td height=400 align=center><br><br>");
		html.append("<table border=0 cellspacing=0 cellpadding=0 width=760><tr>");
		
		// ---- LEFT COLUMN: Manage Scheme ----
		html.append("<td width=240 height=410 align=center>");
		html.append("<table border=0 cellspacing=2 cellpadding=0 width=240 height=400 background=\"l2ui_ct1.ComboBox_DF_Dropmenu_Bg\">");
		html.append("<tr><td width=240 height=55 align=center><table>");
		html.append("<tr><td width=240 height=40 align=center background=\"l2ui_ct1.ComboBox_DF_Dropmenu_Bg\"><br>");
		html.append("<font color=99BBFF name=hs12>Manage Scheme</font></td></tr>");
		if (Config.ENABLE_SCHEME_SYSTEM)
		{
			if (schemes.size() < Config.SCHEMES_PER_PLAYER)
			{
				html.append(menuItem("Icon.skill6439", "New Scheme", null, "_bbsbuffer;create_1"));
			}
			if (!schemes.isEmpty())
			{
				html.append(menuItem("Icon.color_name_i00", "Edit Scheme", null, "_bbsbuffer;edit_1"));
				html.append(menuItem("Icon.etc_ssq_i00", "Delete Scheme", null, "_bbsbuffer;delete_1"));
			}
		}
		// Your Schemes sub-header
		html.append("<tr><td width=240 height=40 align=center valign=top>");
		html.append("<table border=0 width=240 height=40><tr>");
		html.append("<td width=240 align=center valign=center background=\"l2ui_ct1.ComboBox_DF_Dropmenu_Bg\">");
		html.append("<font name=hs12 color=99BBFF>Your Schemes</font>");
		html.append("</td></tr></table></td></tr>");
		if (Config.ENABLE_SCHEME_SYSTEM)
		{
			final String schemePrice = Config.FREE_BUFFS ? "Free" : formatAdena(Config.SCHEME_BUFF_PRICE) + " Adena";
			if (schemes.isEmpty())
			{
				html.append("<tr><td align=center><br><font color=999999>No schemes created yet.</font><br></td></tr>");
			}
			else
			{
				for (String[] scheme : schemes)
				{
					html.append(menuItem("Icon.skill1374", scheme[1], "Price: " + schemePrice, "_bbsbuffer;cast;" + scheme[0]));
				}
			}
		}
		html.append("</table></td></tr></table>");
		html.append("</td>");
		
		// ---- MIDDLE COLUMN: Individual Buffs ----
		html.append("<td width=250 height=400 align=center valign=top>");
		html.append("<table border=0 cellspacing=2 cellpadding=0 width=250 height=300 background=\"l2ui_ct1.ComboBox_DF_Dropmenu_Bg\">");
		html.append("<tr><td width=250 height=55 align=center><table>");
		html.append("<tr><td width=240 height=40 align=center background=\"l2ui_ct1.ComboBox_DF_Dropmenu_Bg\"><br>");
		html.append("<font color=99BBFF name=hs12>Individual Buffs</font></td></tr>");
		if (Config.ENABLE_BUFF_SECTION)
		{
			// Row: Buffs | Resists
			if (Config.ENABLE_BUFFS || Config.ENABLE_RESIST)
			{
				html.append("<tr><td align=center><br1><table><tr>");
				if (Config.ENABLE_BUFFS)
				{
					html.append(buffCategoryLeft("Icon.skill1500", "Buffs", "_bbsbuffer;view;buff"));
				}
				if (Config.ENABLE_RESIST)
				{
					html.append(buffCategoryRight("Icon.skill4333", "Resists", "_bbsbuffer;view;resist"));
				}
				html.append("</tr></table></td></tr>");
			}
			// Row: Songs | Dances
			if (Config.ENABLE_SONGS || Config.ENABLE_DANCES)
			{
				html.append("<tr><td align=center><table><tr>");
				if (Config.ENABLE_SONGS)
				{
					html.append(buffCategoryLeft("Icon.skill0269", "Songs", "_bbsbuffer;view;song"));
				}
				if (Config.ENABLE_DANCES)
				{
					html.append(buffCategoryRight("Icon.skill0275", "Dances", "_bbsbuffer;view;dance"));
				}
				html.append("</tr></table></td></tr>");
			}
			// Row: Chant | Special
			if (Config.ENABLE_CHANTS || Config.ENABLE_SPECIAL)
			{
				html.append("<tr><td align=center><table><tr>");
				if (Config.ENABLE_CHANTS)
				{
					html.append(buffCategoryLeft("Icon.skill1007", "Chant", "_bbsbuffer;view;chant"));
				}
				if (Config.ENABLE_SPECIAL)
				{
					html.append(buffCategoryRight("Icon.skill1331", "Special", "_bbsbuffer;view;special"));
				}
				html.append("</tr></table></td></tr>");
			}
			// Row: Others | Cubics
			if (Config.ENABLE_OTHERS || Config.ENABLE_CUBIC)
			{
				html.append("<tr><td align=center><table><tr>");
				if (Config.ENABLE_OTHERS)
				{
					html.append(buffCategoryLeft("Icon.skill1303", "Others", "_bbsbuffer;view;others"));
				}
				if (Config.ENABLE_CUBIC)
				{
					html.append(buffCategoryRight("Icon.skill0278", "Cubics", "_bbsbuffer;view;cubic"));
				}
				html.append("</tr></table></td></tr>");
			}
		}
		if (player.isGM())
		{
			html.append(menuItem("Icon.skill0487", "Admin Edit buffs", null, "_bbsbuffer;gmManage"));
		}
		html.append("</table></td></tr></table>");
		html.append("</td>");
		
		// ---- RIGHT COLUMN: Fast Actions ----
		html.append("<td width=230 height=400 valign=top align=center>");
		html.append("<table border=0 cellspacing=2 cellpadding=0 width=235 background=\"l2ui_ct1.ComboBox_DF_Dropmenu_Bg\">");
		// Fast Actions sub-header
		html.append("<tr><td width=235 height=40 align=center valign=top>");
		html.append("<table border=0 width=235 height=40><tr>");
		html.append("<td width=235 align=center valign=center background=\"l2ui_ct1.ComboBox_DF_Dropmenu_Bg\">");
		html.append("<font name=hs12 color=99BBFF>Fast Actions</font>");
		html.append("</td></tr></table></td></tr>");
		if (Config.ENABLE_BUFF_SET)
		{
			final String autoLabel = petMode ? "Auto Buff Pet" : "Auto Buff";
			final String autoPrice = Config.FREE_BUFFS ? "Free" : formatAdena(Config.BUFF_SET_PRICE) + " Adena";
			html.append(menuItem("Icon.skill1411", autoLabel, "Price: " + autoPrice, "_bbsbuffer;castSet"));
		}
		if (Config.ENABLE_HEAL)
		{
			final String healLabel = petMode ? "Heal My Pet" : "Heal HP / CP / MP";
			final String healPrice = Config.FREE_BUFFS ? "Free" : formatAdena(Config.HEAL_PRICE) + " Adena";
			html.append(menuItem("Icon.skill0440", healLabel, "Price: " + healPrice, "_bbsbuffer;heal"));
		}
		if (Config.ENABLE_BUFF_REMOVE)
		{
			final String cancelLabel = petMode ? "Remove Pet Buffs" : "Cancel Your Buffs";
			final String cancelPrice = Config.FREE_BUFFS ? "Free" : formatAdena(Config.BUFF_REMOVE_PRICE) + " Adena";
			html.append(menuItem("Icon.skill1056", cancelLabel, "Price: " + cancelPrice, "_bbsbuffer;removeBuffs"));
		}
		html.append("</table>");
		html.append("</td>");
		
		// Close 3-column layout
		html.append("</tr></table>");
		html.append("</td></tr>");
		
		// Close outer frame
		html.append("</table>");
		html.append("</td></tr></table>");
		html.append("</body></html>");
		return html.toString();
	}
	
	// =========================================================
	// BUFF CATEGORY PAGE
	// =========================================================
	
	private String buildCategoryPage(String buffType)
	{
		final StringBuilder html = new StringBuilder();
		html.append("<html noscrollbar><title>").append(TITLE).append("</title><body><center><br>");
		
		final List<String> availableBuffs = new ArrayList<>();
		try (Connection con = DatabaseFactory.getConnection())
		{
			final PreparedStatement ps = con.prepareStatement("SELECT buffId, buffLevel FROM npcbuffer_buff_list WHERE buffType=? AND canUse=1 ORDER BY Buff_Class ASC, id");
			ps.setString(1, buffType);
			final ResultSet rs = ps.executeQuery();
			while (rs.next())
			{
				final int buffId = rs.getInt("buffId");
				final int buffLevel = rs.getInt("buffLevel");
				String buffName = SkillData.getInstance().getSkill(buffId, buffLevel).getName();
				buffName = buffName.replace(" ", "+");
				availableBuffs.add(buffName + "_" + buffId + "_" + buffLevel);
			}
			rs.close();
			ps.close();
		}
		catch (SQLException e)
		{
			LOG.warning("BufferBoard buildCategoryPage error: " + e.getMessage());
		}
		
		if (availableBuffs.isEmpty())
		{
			html.append("No buffs are available at this moment!");
		}
		else
		{
			if (Config.FREE_BUFFS)
			{
				html.append("All buffs are for <font color=LEVEL>free</font>!");
			}
			else
			{
				html.append("Each buff costs <font color=LEVEL>").append(formatAdena(getCategoryPrice(buffType))).append("</font> adena!");
			}
			
			html.append("<BR1><table>");
			for (String buff : availableBuffs)
			{
				final int lastSep = buff.lastIndexOf('_');
				final int secondSep = buff.lastIndexOf('_', lastSep - 1);
				final String name = buff.substring(0, secondSep).replace("+", " ");
				final int id = Integer.parseInt(buff.substring(secondSep + 1, lastSep));
				final int level = Integer.parseInt(buff.substring(lastSep + 1));
				html.append("<tr><td>").append(getSkillIconHtml(id, level)).append("</td>");
				html.append("<td>").append(button(name, "_bbsbuffer;give;" + id + ";" + level + ";" + buffType, 190)).append("</td></tr>");
			}
			html.append("</table>");
		}
		
		html.append("<br>").append(button("Back", "_bbsbuffer", 100));
		html.append("<br><font color=303030>").append(TITLE).append("</font></center></body></html>");
		return html.toString();
	}
	
	// =========================================================
	// ACTION HANDLERS
	// =========================================================
	
	private String handleGiveBuff(Player player, String data)
	{
		final String[] parts = data.split(";");
		if (parts.length < 3)
		{
			return buildMainPage(player);
		}
		
		final int skillId;
		final int skillLevel;
		try
		{
			skillId = Integer.parseInt(parts[0]);
			skillLevel = Integer.parseInt(parts[1]);
		}
		catch (NumberFormatException _)
		{
			return buildMainPage(player);
		}
		final String buffType = parts[2];
		
		if (!Config.FREE_BUFFS)
		{
			final int cost = getCategoryPrice(buffType);
			if (player.getInventory().getInventoryItemCount(Config.CONSUMABLE_ID, -1) < cost)
			{
				return showInfo("Sorry", "You don't have enough items:<br>You need: <font color=LEVEL>" + formatAdena(cost) + " " + getItemNameHtml(Config.CONSUMABLE_ID) + "!");
			}
			player.destroyItemByItemId(ItemProcessType.FEE, Config.CONSUMABLE_ID, cost, player, true);
		}
		
		final Skill skill = getSkillCached(skillId, skillLevel);
		if (skill == null)
		{
			return buildCategoryPage(buffType);
		}
		
		final boolean petMode = isPetMode(player);
		
		// Cubic special handling
		if ("cubic".equals(buffType))
		{
			if (skill.hasEffectType(EffectType.SUMMON) && (player.getInventory().getInventoryItemCount(skill.getItemConsumeId(), -1) < skill.getItemConsumeCount()))
			{
				return showInfo("Sorry", "You don't have enough items for this cubic!");
			}
			
			final Map<Integer, Cubic> playerCubics = player.getCubics();
			if (playerCubics != null)
			{
				playerCubics.forEach((index, cubic) -> cubic.stopAction());
				playerCubics.clear();
			}
			player.broadcastPacket(new MagicSkillUse(player, skillId, skillLevel, 1000, 0));
			player.useMagic(skill, false, false);
		}
		else if (petMode)
		{
			final Summon summon = player.getSummon();
			if (summon == null)
			{
				return showInfo("Info", "You can't use the Pet's options.<br>Summon your pet first!");
			}
			skill.applyEffects(summon, summon, true, BUFFTIME);
		}
		else
		{
			skill.applyEffects(player, player, true, BUFFTIME);
		}
		
		return buildCategoryPage(buffType);
	}
	
	private String handleHeal(Player player)
	{
		final boolean petMode = isPetMode(player);
		
		if (petMode && (player.getSummon() == null))
		{
			return showInfo("Info", "You can't use the Pet's options.<br>Summon your pet first!");
		}
		
		if (!Config.FREE_BUFFS)
		{
			if (player.getInventory().getInventoryItemCount(Config.CONSUMABLE_ID, -1) < Config.HEAL_PRICE)
			{
				return showInfo("Sorry", "You don't have enough items:<br>You need: <font color=LEVEL>" + formatAdena(Config.HEAL_PRICE) + " " + getItemNameHtml(Config.CONSUMABLE_ID) + "!");
			}
			player.destroyItemByItemId(ItemProcessType.FEE, Config.CONSUMABLE_ID, Config.HEAL_PRICE, player, true);
		}
		
		if (petMode)
		{
			final Summon target = player.getSummon();
			if (target == null)
			{
				return showInfo("Info", "You can't use the Pet's options.<br>Summon your pet first!");
			}
			
			final double maxHp = ServitorShareConditions.getMaxServitorRecoverableHp(target);
			final double maxMp = ServitorShareConditions.getMaxServitorRecoverableMp(target);
			target.setCurrentHp(maxHp);
			target.setCurrentMp(maxMp);
			
			if (target instanceof Servitor servitor)
			{
				servitor.setLifeTimeRemaining(servitor.getLifeTimeRemaining() + servitor.getLifeTime());
				player.sendPacket(new SetSummonRemainTime(servitor.getLifeTime(), servitor.getLifeTimeRemaining()));
			}
			
			target.setTarget(target);
			target.broadcastPacket(new MagicSkillUse(target, SKILL_HEAL, 1, 1000, 0));
		}
		else
		{
			final PlayerStatus pcStatus = player.getStatus();
			final PlayerStat pcStat = player.getStat();
			pcStatus.setCurrentHp(pcStat.getMaxHp());
			pcStatus.setCurrentMp(pcStat.getMaxMp());
			pcStatus.setCurrentCp(pcStat.getMaxCp());
			player.setTarget(player);
			player.broadcastPacket(new MagicSkillUse(player, SKILL_HEAL, 1, 1000, 0));
		}
		
		return buildMainPage(player);
	}
	
	private String handleRemoveBuffs(Player player)
	{
		final boolean petMode = isPetMode(player);
		
		if (petMode && (player.getSummon() == null))
		{
			return showInfo("Info", "You can't use the Pet's options.<br>Summon your pet first!");
		}
		
		if (!Config.FREE_BUFFS)
		{
			if (player.getInventory().getInventoryItemCount(Config.CONSUMABLE_ID, -1) < Config.BUFF_REMOVE_PRICE)
			{
				return showInfo("Sorry", "You don't have enough items:<br>You need: <font color=LEVEL>" + formatAdena(Config.BUFF_REMOVE_PRICE) + " " + getItemNameHtml(Config.CONSUMABLE_ID) + "!");
			}
			player.destroyItemByItemId(ItemProcessType.FEE, Config.CONSUMABLE_ID, Config.BUFF_REMOVE_PRICE, player, true);
		}
		
		if (petMode)
		{
			player.getSummon().stopAllEffects();
		}
		else
		{
			player.stopAllEffects();
		}
		
		return buildMainPage(player);
	}
	
	private String handleCastBuffSet(Player player)
	{
		final boolean petMode = isPetMode(player);
		
		if (petMode && (player.getSummon() == null))
		{
			return showInfo("Info", "You can't use the Pet's options.<br>Summon your pet first!");
		}
		
		if (!Config.FREE_BUFFS)
		{
			if (player.getInventory().getInventoryItemCount(Config.CONSUMABLE_ID, -1) < Config.BUFF_SET_PRICE)
			{
				return showInfo("Sorry", "You don't have enough items:<br>You need: <font color=LEVEL>" + formatAdena(Config.BUFF_SET_PRICE) + " " + getItemNameHtml(Config.CONSUMABLE_ID) + "!");
			}
			player.destroyItemByItemId(ItemProcessType.FEE, Config.CONSUMABLE_ID, Config.BUFF_SET_PRICE, player, true);
		}
		
		final int playerClass = player.isMageClass() ? 1 : 0;
		final List<int[]> buffSets = new ArrayList<>();
		
		try (Connection con = DatabaseFactory.getConnection())
		{
			final PreparedStatement ps = con.prepareStatement("SELECT buffId, buffLevel FROM npcbuffer_buff_list WHERE forClass IN (?, ?) ORDER BY id ASC");
			ps.setInt(1, petMode ? 0 : playerClass);
			ps.setString(2, "2");
			final ResultSet rs = ps.executeQuery();
			while (rs.next())
			{
				buffSets.add(new int[]
				{
					rs.getInt("buffId"),
					rs.getInt("buffLevel")
				});
			}
			rs.close();
			ps.close();
		}
		catch (SQLException e)
		{
			LOG.warning("BufferBoard castBuffSet error: " + e.getMessage());
		}
		
		if (petMode)
		{
			final Summon summon = player.getSummon();
			if (summon == null)
			{
				return showInfo("Info", "You can't use the Pet's options.<br>Summon your pet first!");
			}
			summon.setTarget(summon);
			summon.broadcastPacket(new MagicSkillUse(summon, SKILL_BUFF_1, 1, 1000, 0));
			summon.broadcastPacket(new MagicSkillUse(summon, SKILL_BUFF_2, 1, 1000, 0));
			applyBuffsDirect(summon, buffSets);
		}
		else
		{
			player.setTarget(player);
			player.broadcastPacket(new MagicSkillUse(player, SKILL_BUFF_1, 1, 1000, 0));
			player.broadcastPacket(new MagicSkillUse(player, SKILL_BUFF_2, 1, 1000, 0));
			applyBuffsDirect(player, buffSets);
		}
		
		return buildMainPage(player);
	}
	
	private String handleCastScheme(Player player, String schemeId)
	{
		if (!isSchemeOwner(player.getObjectId(), schemeId))
		{
			return buildMainPage(player);
		}
		
		final List<Integer> buffs = new ArrayList<>();
		final List<Integer> levels = new ArrayList<>();
		
		try (Connection con = DatabaseFactory.getConnection())
		{
			final PreparedStatement ps = con.prepareStatement("SELECT sc.skill_id, sc.skill_level, bl.buffType, bl.canUse FROM npcbuffer_scheme_contents sc LEFT JOIN npcbuffer_buff_list bl ON sc.skill_id = bl.buffId AND sc.skill_level = bl.buffLevel WHERE sc.scheme_id=? ORDER BY sc.id");
			ps.setString(1, schemeId);
			final ResultSet rs = ps.executeQuery();
			while (rs.next())
			{
				final int id = rs.getInt("skill_id");
				final int level = rs.getInt("skill_level");
				final String type = rs.getString("buffType");
				final String canUse = rs.getString("canUse");
				
				if ((type != null) && isBuffTypeEnabled(type) && "1".equals(canUse))
				{
					buffs.add(id);
					levels.add(level);
				}
			}
			rs.close();
			ps.close();
		}
		catch (SQLException e)
		{
			LOG.warning("BufferBoard castScheme error: " + e.getMessage());
		}
		
		if (buffs.isEmpty())
		{
			return viewSchemeBuffs(player, schemeId, "1", "add");
		}
		
		if (!Config.FREE_BUFFS)
		{
			if (player.getInventory().getInventoryItemCount(Config.CONSUMABLE_ID, -1) < Config.SCHEME_BUFF_PRICE)
			{
				return showInfo("Sorry", "You don't have enough items:<br>You need: <font color=LEVEL>" + formatAdena(Config.SCHEME_BUFF_PRICE) + " " + getItemNameHtml(Config.CONSUMABLE_ID) + "!");
			}
			player.destroyItemByItemId(ItemProcessType.FEE, Config.CONSUMABLE_ID, Config.SCHEME_BUFF_PRICE, player, true);
		}
		
		final List<int[]> buffList = new ArrayList<>(buffs.size());
		for (int i = 0; i < buffs.size(); i++)
		{
			buffList.add(new int[]
			{
				buffs.get(i),
				levels.get(i)
			});
		}
		
		final boolean petMode = isPetMode(player);
		if (petMode)
		{
			final Summon summon = player.getSummon();
			if (summon == null)
			{
				return showInfo("Info", "You can't use the Pet's options.<br>Summon your pet first!");
			}
			summon.setTarget(summon);
			summon.broadcastPacket(new MagicSkillUse(summon, SKILL_BUFF_1, 1, 1000, 0));
			summon.broadcastPacket(new MagicSkillUse(summon, SKILL_BUFF_2, 1, 1000, 0));
			applyBuffsDirect(summon, buffList);
		}
		else
		{
			player.setTarget(player);
			player.broadcastPacket(new MagicSkillUse(player, SKILL_BUFF_1, 1, 1000, 0));
			player.broadcastPacket(new MagicSkillUse(player, SKILL_BUFF_2, 1, 1000, 0));
			applyBuffsDirect(player, buffList);
		}
		
		return buildMainPage(player);
	}
	
	// =========================================================
	// SCHEME MANAGEMENT
	// =========================================================
	
	/**
	 * Returns list of [id, name] pairs for a player's schemes.
	 * @param player
	 * @return
	 */
	private List<String[]> getPlayerSchemes(Player player)
	{
		final List<String[]> schemes = new ArrayList<>();
		try (Connection con = DatabaseFactory.getConnection())
		{
			final PreparedStatement ps = con.prepareStatement("SELECT id, scheme_name FROM npcbuffer_scheme_list WHERE player_id=?");
			ps.setInt(1, player.getObjectId());
			final ResultSet rs = ps.executeQuery();
			while (rs.next())
			{
				schemes.add(new String[]
				{
					rs.getString("id"),
					rs.getString("scheme_name")
				});
			}
			rs.close();
			ps.close();
		}
		catch (SQLException e)
		{
			LOG.warning("BufferBoard getPlayerSchemes error: " + e.getMessage());
		}
		return schemes;
	}
	
	private String createSchemeForm()
	{
		return "<html noscrollbar><title>" + TITLE + "</title><body><center>" + "<img src=\"L2UI_CH3.herotower_deco\" width=256 height=32><br><br>" + "You MUST separate new words with a dot (.)<br><br>" + "Scheme name: <edit var=\"sname\" width=120><br><br>" + button("Create Scheme", "_bbsbuffer;create $sname", 130) + "<br>" + button("Back", "_bbsbuffer", 100) + "<br><font color=303030>" + TITLE + "</font></center></body></html>";
	}
	
	private String handleCreateScheme(Player player, String rawName)
	{
		if (!rawName.matches("[a-zA-Z0-9]+"))
		{
			// player.sendPacket(SystemMessageId.INCORRECT_NAME_PLEASE_TRY_AGAIN);
			return showInfo("Info", "The scheme name contains invalid characters!<br>Only letters (a-z) and numbers (0-9) are allowed.");
		}
		
		if (rawName.length() > 36)
		{
			// player.sendPacket(SystemMessageId.INCORRECT_NAME_PLEASE_TRY_AGAIN);
			return showInfo("Info", "The scheme name is too long!<br>Max 36 characters.");
		}
		
		if (getPlayerSchemes(player).size() >= Config.SCHEMES_PER_PLAYER)
		{
			return showInfo("Info", "You have reached the maximum number of schemes!");
		}
		
		try (Connection con = DatabaseFactory.getConnection())
		{
			final PreparedStatement ps = con.prepareStatement("INSERT INTO npcbuffer_scheme_list (player_id, scheme_name) VALUES (?, ?)");
			ps.setInt(1, player.getObjectId());
			ps.setString(2, rawName);
			ps.executeUpdate();
			ps.close();
		}
		catch (SQLException e)
		{
			LOG.warning("BufferBoard createScheme error: " + e.getMessage());
			return showInfo("Error", "Failed to create scheme. Please try again later.");
		}
		
		return buildMainPage(player);
	}
	
	private String editSchemeList(Player player)
	{
		final StringBuilder html = new StringBuilder();
		html.append("<html noscrollbar><title>").append(TITLE).append("</title><body><center>");
		html.append("<img src=\"L2UI_CH3.herotower_deco\" width=256 height=32><br>");
		html.append("Select a scheme to manage:<br><br>");
		
		try (Connection con = DatabaseFactory.getConnection())
		{
			final PreparedStatement ps = con.prepareStatement("SELECT id, scheme_name FROM npcbuffer_scheme_list WHERE player_id=?");
			ps.setInt(1, player.getObjectId());
			final ResultSet rs = ps.executeQuery();
			while (rs.next())
			{
				html.append(button(rs.getString("scheme_name"), "_bbsbuffer;manage;" + rs.getString("id"), 130));
			}
			rs.close();
			ps.close();
		}
		catch (SQLException e)
		{
			LOG.warning("BufferBoard editSchemeList error: " + e.getMessage());
		}
		
		html.append("<br>").append(button("Back", "_bbsbuffer", 100));
		html.append("<br><font color=303030>").append(TITLE).append("</font></center></body></html>");
		return html.toString();
	}
	
	private String deleteSchemeList(Player player)
	{
		final StringBuilder html = new StringBuilder();
		html.append("<html noscrollbar><title>").append(TITLE).append("</title><body><center>");
		html.append("<img src=\"L2UI_CH3.herotower_deco\" width=256 height=32><br>");
		html.append("Select a scheme to delete:<br><br>");
		
		try (Connection con = DatabaseFactory.getConnection())
		{
			final PreparedStatement ps = con.prepareStatement("SELECT id, scheme_name FROM npcbuffer_scheme_list WHERE player_id=?");
			ps.setInt(1, player.getObjectId());
			final ResultSet rs = ps.executeQuery();
			while (rs.next())
			{
				final String id = rs.getString("id");
				final String name = rs.getString("scheme_name");
				html.append(button(name, "_bbsbuffer;delete_c;" + id + ";" + name, 130));
			}
			rs.close();
			ps.close();
		}
		catch (SQLException e)
		{
			LOG.warning("BufferBoard deleteSchemeList error: " + e.getMessage());
		}
		
		html.append("<br>").append(button("Back", "_bbsbuffer", 100));
		html.append("<br><font color=303030>").append(TITLE).append("</font></center></body></html>");
		return html.toString();
	}
	
	private String confirmDeleteScheme(String id, String name)
	{
		return "<html noscrollbar><title>" + TITLE + "</title><body><center>" + "<img src=\"L2UI_CH3.herotower_deco\" width=256 height=32><br>" + "Do you really want to delete '<font color=LEVEL>" + name + "</font>'?<br><br>" + button("Yes", "_bbsbuffer;delete;" + id, 50) + button("No", "_bbsbuffer;delete_1", 50) + "<br><font color=303030>" + TITLE + "</font></center></body></html>";
	}
	
	private String handleDeleteScheme(Player player, String schemeId)
	{
		if (!isSchemeOwner(player.getObjectId(), schemeId))
		{
			return buildMainPage(player);
		}
		
		try (Connection con = DatabaseFactory.getConnection())
		{
			PreparedStatement ps = con.prepareStatement("DELETE FROM npcbuffer_scheme_list WHERE id=? LIMIT 1");
			ps.setString(1, schemeId);
			ps.executeUpdate();
			ps.close();
			
			ps = con.prepareStatement("DELETE FROM npcbuffer_scheme_contents WHERE scheme_id=?");
			ps.setString(1, schemeId);
			ps.executeUpdate();
			ps.close();
		}
		catch (SQLException e)
		{
			LOG.warning("BufferBoard deleteScheme error: " + e.getMessage());
			return showInfo("Error", "Failed to delete scheme. Please try again later.");
		}
		
		return buildMainPage(player);
	}
	
	private String getSchemeOptions(Player player, String schemeId)
	{
		if (!isSchemeOwner(player.getObjectId(), schemeId))
		{
			return buildMainPage(player);
		}
		
		final int buffCount = getBuffCount(schemeId);
		final StringBuilder html = new StringBuilder();
		html.append("<html noscrollbar><title>").append(TITLE).append("</title><body><center>");
		html.append("<img src=\"L2UI_CH3.herotower_deco\" width=256 height=32><br>");
		html.append("There are <font color=LEVEL>").append(buffCount).append("</font> buffs in this scheme!<br><br>");
		
		if (buffCount < (MAX_SCHEME_BUFFS + MAX_SCHEME_DANCES))
		{
			html.append(button("Add buffs", "_bbsbuffer;addView;" + schemeId + ";1", 130));
		}
		if (buffCount > 0)
		{
			html.append(button("Remove buffs", "_bbsbuffer;removeView;" + schemeId + ";1", 130));
		}
		
		html.append("<br>").append(button("Back", "_bbsbuffer;edit_1", 100));
		html.append(button("Home", "_bbsbuffer", 100));
		html.append("<br><font color=303030>").append(TITLE).append("</font></center></body></html>");
		return html.toString();
	}
	
	// =========================================================
	// SCHEME BUFF ADD/REMOVE VIEW (PAGINATED)
	// =========================================================
	
	private String viewSchemeBuffs(Player player, String scheme, String page, String mode)
	{
		if (!isSchemeOwner(player.getObjectId(), scheme))
		{
			return buildMainPage(player);
		}
		
		final List<String> buffList = new ArrayList<>();
		final StringBuilder html = new StringBuilder();
		html.append("<html noscrollbar><title>").append(TITLE).append("</title><body><center><br>");
		
		final String[] counts = getSchemeBuffCounts(scheme).split(" ");
		final int totalBuffs = Integer.parseInt(counts[0]);
		final int buffCount = Integer.parseInt(counts[1]);
		final int danceSongCount = Integer.parseInt(counts[2]);
		
		try (Connection con = DatabaseFactory.getConnection())
		{
			if ("add".equals(mode))
			{
				html.append("You can add <font color=LEVEL>").append(MAX_SCHEME_BUFFS - buffCount).append("</font> Buffs and <font color=LEVEL>").append(MAX_SCHEME_DANCES - danceSongCount).append("</font> Dances more!");
				
				final String typeQuery = generateQuery(buffCount, danceSongCount);
				if (typeQuery.isEmpty())
				{
					html.append("<br>No more buff slots available!");
					html.append("<br>").append(button("Back", "_bbsbuffer;manage;" + scheme, 100));
					html.append(button("Home", "_bbsbuffer", 100));
					html.append("<br><font color=303030>").append(TITLE).append("</font></center></body></html>");
					return html.toString();
				}
				
				final PreparedStatement ps = con.prepareStatement("SELECT * FROM npcbuffer_buff_list WHERE buffType IN (" + typeQuery + ") AND canUse=1 ORDER BY Buff_Class ASC, id");
				final ResultSet rs = ps.executeQuery();
				while (rs.next())
				{
					String name = SkillData.getInstance().getSkill(rs.getInt("buffId"), rs.getInt("buffLevel")).getName();
					name = name.replace(" ", "+");
					buffList.add(name + "_" + rs.getInt("buffId") + "_" + rs.getInt("buffLevel"));
				}
				rs.close();
				ps.close();
			}
			else
			{
				html.append("You have <font color=LEVEL>").append(buffCount).append("</font> Buffs and <font color=LEVEL>").append(danceSongCount).append("</font> Dances");
				
				final PreparedStatement ps = con.prepareStatement("SELECT skill_id, skill_level FROM npcbuffer_scheme_contents WHERE scheme_id=? ORDER BY Buff_Class ASC, id");
				ps.setString(1, scheme);
				final ResultSet rs = ps.executeQuery();
				while (rs.next())
				{
					String name = SkillData.getInstance().getSkill(rs.getInt("skill_id"), rs.getInt("skill_level")).getName();
					name = name.replace(" ", "+");
					buffList.add(name + "_" + rs.getInt("skill_id") + "_" + rs.getInt("skill_level"));
				}
				rs.close();
				ps.close();
			}
		}
		catch (SQLException e)
		{
			LOG.warning("BufferBoard viewSchemeBuffs error: " + e.getMessage());
		}
		
		// Pagination
		final int pageCount = Math.max(1, ((buffList.size() - 1) / BUFFS_PER_PAGE) + 1);
		int currentPage;
		try
		{
			currentPage = Integer.parseInt(page);
		}
		catch (NumberFormatException _)
		{
			currentPage = 1;
		}
		if ((currentPage < 1) || (currentPage > pageCount))
		{
			currentPage = 1;
		}
		final String pageName = pageCount > 5 ? "P" : "Page ";
		final String width = pageCount > 5 ? "25" : "50";
		
		html.append("<BR1><table border=0><tr>");
		for (int i = 1; i <= pageCount; i++)
		{
			if (i == currentPage)
			{
				html.append("<td width=").append(width).append(" align=center><font color=LEVEL>").append(pageName).append(i).append("</font></td>");
			}
			else
			{
				final String viewCmd = "add".equals(mode) ? "addView" : "removeView";
				html.append("<td width=").append(width).append(">").append(button(pageName + i, "_bbsbuffer;" + viewCmd + ";" + scheme + ";" + i, Integer.parseInt(width))).append("</td>");
			}
		}
		html.append("</tr></table>");
		
		// Pre-load used buffs to avoid N+1 queries
		final Set<String> usedBuffs = new HashSet<>();
		if ("add".equals(mode))
		{
			try (Connection con = DatabaseFactory.getConnection())
			{
				final PreparedStatement ps = con.prepareStatement("SELECT skill_id, skill_level FROM npcbuffer_scheme_contents WHERE scheme_id=?");
				ps.setString(1, scheme);
				final ResultSet rs = ps.executeQuery();
				while (rs.next())
				{
					usedBuffs.add(rs.getInt("skill_id") + "_" + rs.getInt("skill_level"));
				}
				rs.close();
				ps.close();
			}
			catch (SQLException e)
			{
				LOG.warning("BufferBoard preload usedBuffs error: " + e.getMessage());
			}
		}
		
		// Buff list
		final int start = Math.max(0, (BUFFS_PER_PAGE * currentPage) - BUFFS_PER_PAGE);
		final int end = Math.min(BUFFS_PER_PAGE * currentPage, buffList.size());
		int k = 0;
		
		for (int i = start; i < end; i++)
		{
			final String original = buffList.get(i);
			final int lastSep = original.lastIndexOf('_');
			final int secondSep = original.lastIndexOf('_', lastSep - 1);
			final String name = original.substring(0, secondSep).replace("+", " ");
			final int id = Integer.parseInt(original.substring(secondSep + 1, lastSep));
			final int level = Integer.parseInt(original.substring(lastSep + 1));
			
			if ("add".equals(mode) && usedBuffs.contains(id + "_" + level))
			{
				continue;
			}
			
			final String bgColor = ((k % 2) != 0) ? "333333" : "292929";
			html.append("<BR1><table border=0 bgcolor=").append(bgColor).append(">");
			html.append("<tr><td width=35>").append(getSkillIconHtml(id, level)).append("</td><td fixwidth=170>").append(name).append("</td><td>");
			
			if ("add".equals(mode))
			{
				html.append(button("Add", "_bbsbuffer;addBuff;" + scheme + "_" + id + "_" + level + ";" + page + ";" + totalBuffs, 65));
			}
			else
			{
				html.append(button("Remove", "_bbsbuffer;removeBuff;" + scheme + "_" + id + "_" + level + ";" + page + ";" + totalBuffs, 65));
			}
			
			html.append("</td></tr></table>");
			k++;
		}
		
		html.append("<br><br>").append(button("Back", "_bbsbuffer;manage;" + scheme, 100));
		html.append(button("Home", "_bbsbuffer", 100));
		html.append("<br><font color=303030>").append(TITLE).append("</font></center></body></html>");
		return html.toString();
	}
	
	private String handleAddBuffToScheme(Player player, String data)
	{
		final String[] parts = data.split(";");
		final String[] buffParts = parts[0].split("_");
		if (buffParts.length < 3)
		{
			return buildMainPage(player);
		}
		
		final String scheme = buffParts[0];
		if (!isSchemeOwner(player.getObjectId(), scheme))
		{
			return buildMainPage(player);
		}
		
		final String skill = buffParts[1];
		final String level = buffParts[2];
		final String page = parts.length > 1 ? parts[1] : "1";
		
		// Validate skill/level are numeric
		final int skillId;
		final int skillLevel;
		try
		{
			skillId = Integer.parseInt(skill);
			skillLevel = Integer.parseInt(level);
		}
		catch (NumberFormatException _)
		{
			return buildMainPage(player);
		}
		
		// Server-side capacity check (never trust client total)
		final String[] counts = getSchemeBuffCounts(scheme).split(" ");
		final int currentTotal = Integer.parseInt(counts[0]);
		if (currentTotal >= (MAX_SCHEME_BUFFS + MAX_SCHEME_DANCES))
		{
			return getSchemeOptions(player, scheme);
		}
		
		// Validate buff exists and is enabled in the buff list
		if (!isBuffAvailable(skillId, skillLevel))
		{
			return buildMainPage(player);
		}
		
		// Check for duplicate buff in scheme
		if (isBuffInScheme(scheme, skillId, skillLevel))
		{
			return viewSchemeBuffs(player, scheme, page, "add");
		}
		
		final int buffClass = getClassBuff(skill);
		try (Connection con = DatabaseFactory.getConnection())
		{
			final PreparedStatement ps = con.prepareStatement("INSERT INTO npcbuffer_scheme_contents (scheme_id, skill_id, skill_level, buff_class) VALUES (?, ?, ?, ?)");
			ps.setString(1, scheme);
			ps.setInt(2, skillId);
			ps.setInt(3, skillLevel);
			ps.setInt(4, buffClass);
			ps.executeUpdate();
			ps.close();
		}
		catch (SQLException e)
		{
			LOG.warning("BufferBoard addBuff error: " + e.getMessage());
			return showInfo("Error", "Failed to add buff to scheme. Please try again later.");
		}
		
		if ((currentTotal + 1) >= (MAX_SCHEME_BUFFS + MAX_SCHEME_DANCES))
		{
			return getSchemeOptions(player, scheme);
		}
		return viewSchemeBuffs(player, scheme, page, "add");
	}
	
	private String handleRemoveBuffFromScheme(Player player, String data)
	{
		final String[] parts = data.split(";");
		final String[] buffParts = parts[0].split("_");
		if (buffParts.length < 3)
		{
			return buildMainPage(player);
		}
		
		final String scheme = buffParts[0];
		if (!isSchemeOwner(player.getObjectId(), scheme))
		{
			return buildMainPage(player);
		}
		
		final int skillId;
		final int skillLevel;
		try
		{
			skillId = Integer.parseInt(buffParts[1]);
			skillLevel = Integer.parseInt(buffParts[2]);
		}
		catch (NumberFormatException _)
		{
			return buildMainPage(player);
		}
		final String page = parts.length > 1 ? parts[1] : "1";
		
		try (Connection con = DatabaseFactory.getConnection())
		{
			final PreparedStatement ps = con.prepareStatement("DELETE FROM npcbuffer_scheme_contents WHERE scheme_id=? AND skill_id=? AND skill_level=? LIMIT 1");
			ps.setString(1, scheme);
			ps.setInt(2, skillId);
			ps.setInt(3, skillLevel);
			ps.executeUpdate();
			ps.close();
		}
		catch (SQLException e)
		{
			LOG.warning("BufferBoard removeBuff error: " + e.getMessage());
			return showInfo("Error", "Failed to remove buff from scheme. Please try again later.");
		}
		
		// Use server-side count instead of trusting client total
		final int remaining = getBuffCount(scheme);
		if (remaining <= 0)
		{
			return getSchemeOptions(player, scheme);
		}
		return viewSchemeBuffs(player, scheme, page, "remove");
	}
	
	// =========================================================
	// DATABASE HELPERS
	// =========================================================
	
	/**
	 * Checks if a buff with the given id/level exists and is enabled (canUse=1) in the buff list.
	 * @param skillId
	 * @param skillLevel
	 * @return
	 */
	private boolean isBuffAvailable(int skillId, int skillLevel)
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			final PreparedStatement ps = con.prepareStatement("SELECT buffId FROM npcbuffer_buff_list WHERE buffId=? AND buffLevel=? AND canUse=1 LIMIT 1");
			ps.setInt(1, skillId);
			ps.setInt(2, skillLevel);
			final ResultSet rs = ps.executeQuery();
			final boolean exists = rs.next();
			rs.close();
			ps.close();
			return exists;
		}
		catch (SQLException e)
		{
			LOG.warning("BufferBoard isBuffAvailable error: " + e.getMessage());
		}
		return false;
	}
	
	/**
	 * Checks if a buff is already present in the given scheme.
	 * @param schemeId
	 * @param skillId
	 * @param skillLevel
	 * @return
	 */
	private boolean isBuffInScheme(String schemeId, int skillId, int skillLevel)
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			final PreparedStatement ps = con.prepareStatement("SELECT skill_id FROM npcbuffer_scheme_contents WHERE scheme_id=? AND skill_id=? AND skill_level=? LIMIT 1");
			ps.setString(1, schemeId);
			ps.setInt(2, skillId);
			ps.setInt(3, skillLevel);
			final ResultSet rs = ps.executeQuery();
			final boolean exists = rs.next();
			rs.close();
			ps.close();
			return exists;
		}
		catch (SQLException e)
		{
			LOG.warning("BufferBoard isBuffInScheme error: " + e.getMessage());
		}
		return false;
	}
	
	private int getBuffCount(String scheme)
	{
		int count = 0;
		try (Connection con = DatabaseFactory.getConnection())
		{
			final PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) AS cnt FROM npcbuffer_scheme_contents WHERE scheme_id=?");
			ps.setString(1, scheme);
			final ResultSet rs = ps.executeQuery();
			if (rs.next())
			{
				count = rs.getInt("cnt");
			}
			rs.close();
			ps.close();
		}
		catch (SQLException e)
		{
			LOG.warning("BufferBoard getBuffCount error: " + e.getMessage());
		}
		return count;
	}
	
	/**
	 * Checks if the given scheme belongs to the specified player.
	 * @param playerId
	 * @param schemeId
	 * @return
	 */
	private boolean isSchemeOwner(int playerId, String schemeId)
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			final PreparedStatement ps = con.prepareStatement("SELECT id FROM npcbuffer_scheme_list WHERE id=? AND player_id=? LIMIT 1");
			ps.setString(1, schemeId);
			ps.setInt(2, playerId);
			final ResultSet rs = ps.executeQuery();
			final boolean owns = rs.next();
			rs.close();
			ps.close();
			return owns;
		}
		catch (SQLException e)
		{
			LOG.warning("BufferBoard isSchemeOwner error: " + e.getMessage());
		}
		return false;
	}
	
	/**
	 * Returns "total buffCount danceSongCount" for a scheme.
	 * @param scheme
	 * @return
	 */
	private String getSchemeBuffCounts(String scheme)
	{
		int total = 0;
		int buffCount = 0;
		int danceSongCount = 0;
		try (Connection con = DatabaseFactory.getConnection())
		{
			final PreparedStatement ps = con.prepareStatement("SELECT buff_class FROM npcbuffer_scheme_contents WHERE scheme_id=?");
			ps.setString(1, scheme);
			final ResultSet rs = ps.executeQuery();
			while (rs.next())
			{
				total++;
				final int val = rs.getInt("buff_class");
				if ((val == 1) || (val == 2))
				{
					danceSongCount++;
				}
				else
				{
					buffCount++;
				}
			}
			rs.close();
			ps.close();
		}
		catch (SQLException e)
		{
			LOG.warning("BufferBoard getSchemeBuffCounts error: " + e.getMessage());
		}
		return total + " " + buffCount + " " + danceSongCount;
	}
	
	private int getClassBuff(String id)
	{
		int val = 0;
		try (Connection con = DatabaseFactory.getConnection())
		{
			final PreparedStatement ps = con.prepareStatement("SELECT buff_class FROM npcbuffer_buff_list WHERE buffId=?");
			ps.setString(1, id);
			final ResultSet rs = ps.executeQuery();
			if (rs.next())
			{
				val = rs.getInt("buff_class");
			}
			rs.close();
			ps.close();
		}
		catch (SQLException e)
		{
			LOG.warning("BufferBoard getClassBuff error: " + e.getMessage());
		}
		return val;
	}
	
	private String generateQuery(int buffsCount, int dancesCount)
	{
		final StringBuilder query = new StringBuilder();
		if (Config.ENABLE_BUFFS && (buffsCount < MAX_SCHEME_BUFFS))
		{
			query.append(",\"buff\"");
		}
		if (Config.ENABLE_RESIST && (buffsCount < MAX_SCHEME_BUFFS))
		{
			query.append(",\"resist\"");
		}
		if (Config.ENABLE_SONGS && (dancesCount < MAX_SCHEME_DANCES))
		{
			query.append(",\"song\"");
		}
		if (Config.ENABLE_DANCES && (dancesCount < MAX_SCHEME_DANCES))
		{
			query.append(",\"dance\"");
		}
		if (Config.ENABLE_CHANTS && (buffsCount < MAX_SCHEME_BUFFS))
		{
			query.append(",\"chant\"");
		}
		if (Config.ENABLE_OTHERS && (buffsCount < MAX_SCHEME_BUFFS))
		{
			query.append(",\"others\"");
		}
		if (Config.ENABLE_SPECIAL && (buffsCount < MAX_SCHEME_BUFFS))
		{
			query.append(",\"special\"");
		}
		if (!query.isEmpty())
		{
			query.deleteCharAt(0);
		}
		return query.toString();
	}
	
	// =========================================================
	// GM MANAGEMENT
	// =========================================================
	
	private String gmViewAllBuffTypes()
	{
		final StringBuilder html = new StringBuilder();
		html.append("<html noscrollbar><title>").append(TITLE).append("</title><body><center>");
		html.append("<img src=\"L2UI_CH3.herotower_deco\" width=256 height=32><br>");
		html.append("<font color=LEVEL>[GM Buff Management]</font><br>");
		
		if (Config.ENABLE_BUFFS)
		{
			html.append(button("Buffs", "_bbsbuffer;gmEditList;buff;Buffs;1", 118));
		}
		if (Config.ENABLE_RESIST)
		{
			html.append(button("Resist Buffs", "_bbsbuffer;gmEditList;resist;Resists;1", 118));
		}
		if (Config.ENABLE_SONGS)
		{
			html.append(button("Songs", "_bbsbuffer;gmEditList;song;Songs;1", 118));
		}
		if (Config.ENABLE_DANCES)
		{
			html.append(button("Dances", "_bbsbuffer;gmEditList;dance;Dances;1", 118));
		}
		if (Config.ENABLE_CHANTS)
		{
			html.append(button("Chants", "_bbsbuffer;gmEditList;chant;Chants;1", 118));
		}
		if (Config.ENABLE_SPECIAL)
		{
			html.append(button("Special Buffs", "_bbsbuffer;gmEditList;special;Special_Buffs;1", 118));
		}
		if (Config.ENABLE_OTHERS)
		{
			html.append(button("Others Buffs", "_bbsbuffer;gmEditList;others;Others_Buffs;1", 118));
		}
		if (Config.ENABLE_CUBIC)
		{
			html.append(button("Cubics", "_bbsbuffer;gmEditList;cubic;Cubics;1", 118));
		}
		if (Config.ENABLE_BUFF_SET)
		{
			html.append("<br1>").append(button("Buff Sets", "_bbsbuffer;gmEditList;set;Buff_Sets;1", 118));
		}
		
		html.append("<br>").append(button("Back", "_bbsbuffer", 100));
		html.append("<br><font color=303030>").append(TITLE).append("</font></center></body></html>");
		return html.toString();
	}
	
	private String gmViewAllBuffs(String type, String typeName, String page)
	{
		final List<String> buffList = new ArrayList<>();
		final StringBuilder html = new StringBuilder();
		html.append("<html noscrollbar><title>").append(TITLE).append("</title><body><center>");
		html.append("<img src=\"L2UI_CH3.herotower_deco\" width=256 height=32><br>");
		
		try (Connection con = DatabaseFactory.getConnection())
		{
			final PreparedStatement ps;
			if ("set".equals(type))
			{
				ps = con.prepareStatement("SELECT * FROM npcbuffer_buff_list WHERE buffType IN (" + generateQuery(0, 0) + ") AND canUse=1");
			}
			else
			{
				ps = con.prepareStatement("SELECT buffId, buffLevel, canUse, forClass FROM npcbuffer_buff_list WHERE buffType=?");
				ps.setString(1, type);
			}
			final ResultSet rs = ps.executeQuery();
			while (rs.next())
			{
				String name = SkillData.getInstance().getSkill(rs.getInt("buffId"), rs.getInt("buffLevel")).getName();
				name = name.replace(" ", "+");
				buffList.add(name + "_" + rs.getString("forClass") + "_" + page + "_" + rs.getString("canUse") + "_" + rs.getString("buffId") + "_" + rs.getString("buffLevel"));
			}
			rs.close();
			ps.close();
		}
		catch (SQLException e)
		{
			LOG.warning("BufferBoard gmViewAllBuffs error: " + e.getMessage());
		}
		Collections.sort(buffList);
		
		final int buffsPerPage = "set".equals(type) ? 12 : BUFFS_PER_PAGE;
		final int pageCount = Math.max(1, ((buffList.size() - 1) / buffsPerPage) + 1);
		final int currentPage = Integer.parseInt(page);
		final String pName = pageCount > 5 ? "P" : "Page ";
		final String pWidth = pageCount > 5 ? "25" : "50";
		
		html.append("<font color=LEVEL>[GM Management - ").append(typeName.replace("_", " ")).append(" - Page ").append(page).append("]</font><br><table border=0><tr>");
		for (int i = 1; i <= pageCount; i++)
		{
			if (i == currentPage)
			{
				html.append("<td width=").append(pWidth).append(" align=center><font color=LEVEL>").append(pName).append(i).append("</font></td>");
			}
			else
			{
				html.append("<td width=").append(pWidth).append(">").append(button(pName + i, "_bbsbuffer;gmEditList;" + type + ";" + typeName + ";" + i, Integer.parseInt(pWidth))).append("</td>");
			}
		}
		html.append("</tr></table><br>");
		
		final int start = Math.max(0, (buffsPerPage * currentPage) - buffsPerPage);
		final int end = Math.min(buffsPerPage * currentPage, buffList.size());
		
		for (int i = start; i < end; i++)
		{
			// Encoded as: name_forClass_page_canUse_buffId_buffLevel
			// Extract trailing numeric fields safely using lastIndexOf
			final String raw = buffList.get(i);
			final int sep5 = raw.lastIndexOf('_');
			final int sep4 = raw.lastIndexOf('_', sep5 - 1);
			final int sep3 = raw.lastIndexOf('_', sep4 - 1);
			final int sep2 = raw.lastIndexOf('_', sep3 - 1);
			final int sep1 = raw.lastIndexOf('_', sep2 - 1);
			final String name = raw.substring(0, sep1).replace("+", " ");
			final int forClass = Integer.parseInt(raw.substring(sep1 + 1, sep2));
			final int usable = Integer.parseInt(raw.substring(sep3 + 1, sep4));
			final String skillPos = raw.substring(sep4 + 1);
			
			final String bgColor = ((i % 2) != 0) ? "333333" : "292929";
			html.append("<BR1><table border=0 bgcolor=").append(bgColor).append(">");
			
			if ("set".equals(type))
			{
				final String listOrder = switch (forClass)
				{
					case 0 -> "List=\"" + SET_FIGHTER + ";" + SET_MAGE + ";" + SET_ALL + ";" + SET_NONE + ";\"";
					case 1 -> "List=\"" + SET_MAGE + ";" + SET_FIGHTER + ";" + SET_ALL + ";" + SET_NONE + ";\"";
					case 2 -> "List=\"" + SET_ALL + ";" + SET_FIGHTER + ";" + SET_MAGE + ";" + SET_NONE + ";\"";
					default -> "List=\"" + SET_NONE + ";" + SET_FIGHTER + ";" + SET_MAGE + ";" + SET_ALL + ";\"";
				};
				html.append("<tr><td fixwidth=145>").append(name).append("</td><td width=70><combobox var=\"newSet").append(i).append("\" width=70 ").append(listOrder).append("></td><td width=50>");
				html.append(button("Update", "_bbsbuffer;gmChangeSet;" + skillPos + ";" + page + " $newSet" + i, 50));
				html.append("</td></tr>");
			}
			else
			{
				html.append("<tr><td fixwidth=170>").append(name).append("</td><td width=80>");
				if (usable == 1)
				{
					html.append(button("Disable", "_bbsbuffer;gmEditBuff;" + skillPos + ";0-" + page + ";" + type, 80));
				}
				else
				{
					html.append(button("Enable", "_bbsbuffer;gmEditBuff;" + skillPos + ";1-" + page + ";" + type, 80));
				}
				html.append("</td></tr>");
			}
			html.append("</table>");
		}
		
		html.append("<br><br>").append(button("Back", "_bbsbuffer;gmManage", 100));
		html.append(button("Home", "_bbsbuffer", 100));
		html.append("<br><font color=303030>").append(TITLE).append("</font></center></body></html>");
		return html.toString();
	}
	
	private boolean gmManageSelectedBuff(String buffPosId, String canUseBuff)
	{
		final String[] bpid = buffPosId.split("_");
		final String buffId = bpid[0];
		final String buffLevel = bpid[1];
		try (Connection con = DatabaseFactory.getConnection())
		{
			final PreparedStatement ps = con.prepareStatement("UPDATE npcbuffer_buff_list SET canUse=? WHERE buffId=? AND buffLevel=? LIMIT 1");
			ps.setString(1, canUseBuff);
			ps.setString(2, buffId);
			ps.setString(3, buffLevel);
			ps.executeUpdate();
			ps.close();
			return true;
		}
		catch (SQLException e)
		{
			LOG.warning("BufferBoard gmManageSelectedBuff error: " + e.getMessage());
			return false;
		}
	}
	
	private String gmManageSelectedSet(String id, String newVal, String page)
	{
		// Convert label to DB value
		String dbVal;
		if (SET_FIGHTER.equals(newVal))
		{
			dbVal = "0";
		}
		else if (SET_MAGE.equals(newVal))
		{
			dbVal = "1";
		}
		else if (SET_ALL.equals(newVal))
		{
			dbVal = "2";
		}
		else
		{
			dbVal = "3";
		}
		
		final String[] bpid = id.split("_");
		final String buffId = bpid[0];
		final String buffLevel = bpid[1];
		try (Connection con = DatabaseFactory.getConnection())
		{
			final PreparedStatement ps = con.prepareStatement("UPDATE npcbuffer_buff_list SET forClass=? WHERE buffId=? AND buffLevel=?");
			ps.setString(1, dbVal);
			ps.setString(2, buffId);
			ps.setString(3, buffLevel);
			ps.executeUpdate();
			ps.close();
		}
		catch (SQLException e)
		{
			LOG.warning("BufferBoard gmManageSelectedSet error: " + e.getMessage());
			return showInfo("Error", "Failed to update buff set. Please try again later.");
		}
		return gmViewAllBuffs("set", "Buff_Sets", page);
	}
	
	// =========================================================
	// BUFF APPLICATION
	// =========================================================
	
	private Skill getSkillCached(int skillId, int skillLevel)
	{
		final int cacheKey = (skillId * 10000) + skillLevel;
		return SKILL_CACHE.computeIfAbsent(cacheKey, k -> SkillData.getInstance().getSkill(skillId, skillLevel));
	}
	
	private void applyBuffsDirect(Creature target, List<int[]> buffs)
	{
		if ((buffs == null) || buffs.isEmpty() || (target == null))
		{
			return;
		}
		
		for (int[] buff : buffs)
		{
			final Skill skill = getSkillCached(buff[0], buff[1]);
			if (skill != null)
			{
				skill.applyEffects(target, target, true, BUFFTIME);
			}
		}
	}
	
	// =========================================================
	// CONFIG HELPERS
	// =========================================================
	
	private boolean isBuffTypeEnabled(String type)
	{
		switch (type)
		{
			case "buff":
				return Config.ENABLE_BUFFS;
			case "resist":
				return Config.ENABLE_RESIST;
			case "song":
				return Config.ENABLE_SONGS;
			case "dance":
				return Config.ENABLE_DANCES;
			case "chant":
				return Config.ENABLE_CHANTS;
			case "others":
				return Config.ENABLE_OTHERS;
			case "special":
				return Config.ENABLE_SPECIAL;
			case "cubic":
				return Config.ENABLE_CUBIC;
			default:
				return false;
		}
	}
	
	private int getCategoryPrice(String buffType)
	{
		switch (buffType)
		{
			case "buff":
				return Config.BUFF_PRICE;
			case "resist":
				return Config.RESIST_PRICE;
			case "song":
				return Config.SONG_PRICE;
			case "dance":
				return Config.DANCE_PRICE;
			case "chant":
				return Config.CHANT_PRICE;
			case "others":
				return Config.OTHERS_PRICE;
			case "special":
				return Config.SPECIAL_PRICE;
			case "cubic":
				return Config.CUBIC_PRICE;
			default:
				return 0;
		}
	}
	
	// =========================================================
	// HTML UTILITY
	// =========================================================
	
	private boolean isPetMode(Player player)
	{
		return PET_MODE.getOrDefault(player.getObjectId(), false);
	}
	
	private void sendHtml(Player player, String html)
	{
		CommunityBoardHandler.separateAndSend(html, player);
	}
	
	private String button(String label, String bypass, int width)
	{
		return "<button value=\"" + label + "\" action=\"bypass " + bypass + "\" width=" + width + " height=25 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">";
	}
	
	/**
	 * Creates an icon with a clickable frame overlay, following the L2 UI pattern.
	 * @param icon
	 * @param bypass
	 * @return
	 */
	private String iconFrame(String icon, String bypass)
	{
		return "<table border=0 cellspacing=0 cellpadding=0 width=32 height=32 background=\"" + icon + "\">" + "<tr><td width=32 height=32 align=center valign=top>" + "<button action=\"bypass " + bypass + "\" width=34 height=34 back=\"L2UI_CT1.ItemWindow_DF_Frame_Down\" fore=\"L2UI_CT1.ItemWindow_DF_Frame\"/>" + "</td></tr></table>";
	}
	
	/**
	 * Creates a menu item row with icon, label, optional subtitle, and bypass action.
	 * @param icon
	 * @param label
	 * @param subtitle
	 * @param bypass
	 * @return
	 */
	private String menuItem(String icon, String label, String subtitle, String bypass)
	{
		final StringBuilder sb = new StringBuilder();
		sb.append("<tr><td width=240 height=50 align=center>");
		sb.append("<table border=0 width=240 height=50 cellspacing=4 cellpadding=3 bgcolor=10100E>");
		sb.append("<tr><td align=right valign=top>");
		sb.append(iconFrame(icon, bypass));
		sb.append("</td><td width=150 align=left valign=").append(subtitle != null ? "top" : "center").append(">");
		sb.append("<font name=hs12 color=\"ADA71B\">").append(label).append("</font>");
		if (subtitle != null)
		{
			sb.append("<br1><font color=FFFFFF name=__SYSTEMWORLDFONT>").append(subtitle).append("</font>");
		}
		sb.append("</td></tr></table><br></td></tr>");
		return sb.toString();
	}
	
	/**
	 * Left-side buff category cell: icon on left, text on right.
	 * @param icon
	 * @param label
	 * @param bypass
	 * @return
	 */
	private String buffCategoryLeft(String icon, String label, String bypass)
	{
		return "<td width=110 height=50 align=center>" + "<table border=0 width=110 height=50 cellspacing=4 cellpadding=3 bgcolor=10100E>" + "<tr><td align=right valign=top>" + iconFrame(icon, bypass) + "</td><td width=70 align=left valign=center>" + "<font name=hs12 color=\"ADA71B\">" + label + "</font>" + "</td></tr></table></td>";
	}
	
	/**
	 * Right-side buff category cell: text on left, icon on right.
	 * @param icon
	 * @param label
	 * @param bypass
	 * @return
	 */
	private String buffCategoryRight(String icon, String label, String bypass)
	{
		return "<td width=110 height=50 align=center>" + "<table border=0 width=110 height=50 cellspacing=4 cellpadding=3 bgcolor=10100E>" + "<tr><td width=70 align=right valign=center>" + "<font name=hs12 color=\"ADA71B\">" + label + "</font>" + "</td><td align=right valign=top>" + iconFrame(icon, bypass) + "</td></tr></table><br></td>";
	}
	
	private String showInfo(String title, String message)
	{
		return "<html noscrollbar><title>" + TITLE + "</title><body><center>" + "<img src=\"L2UI_CH3.herotower_deco\" width=256 height=32><br>" + "<font color=LEVEL>" + title + "</font><br>" + message + "<br><br>" + button("Back", "_bbshome", 100) + "<br><font color=303030>" + TITLE + "</font></center></body></html>";
	}
	
	private String getItemNameHtml(int itemId)
	{
		return "&#" + itemId + ";";
	}
	
	private String getSkillIconHtml(int id, int level)
	{
		return "<img src=\"Icon.skill" + getSkillIconNumber(id, level) + "\" width=32 height=32>";
	}
	
	private String getSkillIconNumber(int id, int level)
	{
		if (id == 4)
		{
			return "0004";
		}
		if ((id > 9) && (id < 100))
		{
			return "00" + id;
		}
		if ((id > 99) && (id < 1000))
		{
			return "0" + id;
		}
		if (id == 1517)
		{
			return "1536";
		}
		if (id == 1518)
		{
			return "1537";
		}
		if (id == 1547)
		{
			return "0065";
		}
		if (id == 2076)
		{
			return "0195";
		}
		if ((id > 4550) && (id < 4555))
		{
			return "5739";
		}
		if ((id > 4698) && (id < 4701))
		{
			return "1331";
		}
		if ((id > 4701) && (id < 4704))
		{
			return "1332";
		}
		if (id == 6049)
		{
			return "0094";
		}
		return String.valueOf(id);
	}
}
