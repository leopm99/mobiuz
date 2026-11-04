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
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.l2journey.Config;
import com.l2journey.commons.database.DatabaseFactory;
import com.l2journey.gameserver.data.enums.CategoryType;
import com.l2journey.gameserver.data.xml.CategoryData;
import com.l2journey.gameserver.data.xml.ClassListData;
import com.l2journey.gameserver.data.xml.ItemData;
import com.l2journey.gameserver.handler.CommunityBoardHandler;
import com.l2journey.gameserver.handler.IParseBoardHandler;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.enums.creature.Race;
import com.l2journey.gameserver.model.actor.enums.player.PlayerClass;
import com.l2journey.gameserver.model.actor.holders.player.ClassInfoHolder;
import com.l2journey.gameserver.model.actor.holders.player.SubClassHolder;
import com.l2journey.gameserver.model.events.EventDispatcher;
import com.l2journey.gameserver.model.events.EventType;
import com.l2journey.gameserver.model.events.holders.actor.player.OnPlayerProfessionCancel;
import com.l2journey.gameserver.model.item.Henna;
import com.l2journey.gameserver.model.item.ItemTemplate;
import com.l2journey.gameserver.model.item.enums.ItemProcessType;
import com.l2journey.gameserver.model.olympiad.OlympiadManager;
import com.l2journey.gameserver.model.zone.ZoneId;
import com.l2journey.gameserver.network.SystemMessageId;

/**
 * Community Board Subclass Master (Standardized with HomeBoard UI Design).
 * @author Debug, KingHanker
 */
public class SubClassMasterBoard implements IParseBoardHandler
{
	private static final Logger LOGGER = Logger.getLogger(SubClassMasterBoard.class.getName());
	
	private static final String[] COMMANDS =
	{
		"_bbssubclassmaster",
		"_bbssubclassmaster;menu_add",
		"_bbssubclassmaster;menu_change",
		"_bbssubclassmaster;menu_remove",
		"_bbssubclassmaster;add_subclass",
		"_bbssubclassmaster;switch_subclass",
		"_bbssubclassmaster;change_to_base",
		"_bbssubclassmaster;remove_subclass"
	};
	
	private static final int MIN_LEVEL_FOR_SUBCLASS = 75;
	private static final int CLASSES_PER_PAGE = 6;
	
	private static final String DELETE_CHAR_HENNA = "DELETE FROM character_hennas WHERE charId=? AND slot=? AND class_index=?";
	private static final String DELETE_CHAR_SHORTCUTS = "DELETE FROM character_shortcuts WHERE charId=? AND class_index=?";
	private static final String DELETE_SKILL_SAVE = "DELETE FROM character_skills_save WHERE charId=? AND class_index=?";
	private static final String DELETE_CHAR_SKILLS = "DELETE FROM character_skills WHERE charId=? AND class_index=?";
	private static final String DELETE_CHAR_SUBCLASS = "DELETE FROM character_subclasses WHERE charId=? AND class_index=?";
	
	private static final Set<PlayerClass> MAIN_SUBCLASS_SET;
	private static final Set<PlayerClass> NEVER_SUBCLASSED = EnumSet.of(PlayerClass.OVERLORD, PlayerClass.WARSMITH);
	private static final Set<PlayerClass> SUBCLASS_SET_1 = EnumSet.of(PlayerClass.DARK_AVENGER, PlayerClass.PALADIN, PlayerClass.TEMPLE_KNIGHT, PlayerClass.SHILLIEN_KNIGHT);
	private static final Set<PlayerClass> SUBCLASS_SET_2 = EnumSet.of(PlayerClass.TREASURE_HUNTER, PlayerClass.ABYSS_WALKER, PlayerClass.PLAINS_WALKER);
	private static final Set<PlayerClass> SUBCLASS_SET_3 = EnumSet.of(PlayerClass.HAWKEYE, PlayerClass.SILVER_RANGER, PlayerClass.PHANTOM_RANGER);
	private static final Set<PlayerClass> SUBCLASS_SET_4 = EnumSet.of(PlayerClass.WARLOCK, PlayerClass.ELEMENTAL_SUMMONER, PlayerClass.PHANTOM_SUMMONER);
	private static final Set<PlayerClass> SUBCLASS_SET_5 = EnumSet.of(PlayerClass.SORCERER, PlayerClass.SPELLSINGER, PlayerClass.SPELLHOWLER);
	private static final EnumMap<PlayerClass, Set<PlayerClass>> SUBCLASS_SET_MAP = new EnumMap<>(PlayerClass.class);
	
	static
	{
		final Set<PlayerClass> subclasses = CategoryData.getInstance().getCategoryByType(CategoryType.THIRD_CLASS_GROUP).stream().map(PlayerClass::getPlayerClass).collect(Collectors.toSet());
		subclasses.removeAll(NEVER_SUBCLASSED);
		MAIN_SUBCLASS_SET = Collections.unmodifiableSet(subclasses);
		
		SUBCLASS_SET_MAP.put(PlayerClass.DARK_AVENGER, SUBCLASS_SET_1);
		SUBCLASS_SET_MAP.put(PlayerClass.PALADIN, SUBCLASS_SET_1);
		SUBCLASS_SET_MAP.put(PlayerClass.TEMPLE_KNIGHT, SUBCLASS_SET_1);
		SUBCLASS_SET_MAP.put(PlayerClass.SHILLIEN_KNIGHT, SUBCLASS_SET_1);
		SUBCLASS_SET_MAP.put(PlayerClass.TREASURE_HUNTER, SUBCLASS_SET_2);
		SUBCLASS_SET_MAP.put(PlayerClass.ABYSS_WALKER, SUBCLASS_SET_2);
		SUBCLASS_SET_MAP.put(PlayerClass.PLAINS_WALKER, SUBCLASS_SET_2);
		SUBCLASS_SET_MAP.put(PlayerClass.HAWKEYE, SUBCLASS_SET_3);
		SUBCLASS_SET_MAP.put(PlayerClass.SILVER_RANGER, SUBCLASS_SET_3);
		SUBCLASS_SET_MAP.put(PlayerClass.PHANTOM_RANGER, SUBCLASS_SET_3);
		SUBCLASS_SET_MAP.put(PlayerClass.WARLOCK, SUBCLASS_SET_4);
		SUBCLASS_SET_MAP.put(PlayerClass.ELEMENTAL_SUMMONER, SUBCLASS_SET_4);
		SUBCLASS_SET_MAP.put(PlayerClass.PHANTOM_SUMMONER, SUBCLASS_SET_4);
		SUBCLASS_SET_MAP.put(PlayerClass.SORCERER, SUBCLASS_SET_5);
		SUBCLASS_SET_MAP.put(PlayerClass.SPELLSINGER, SUBCLASS_SET_5);
		SUBCLASS_SET_MAP.put(PlayerClass.SPELLHOWLER, SUBCLASS_SET_5);
	}
	
	private static String localize(String lang, String en, String br)
	{
		return "br".equalsIgnoreCase(lang) ? br : en;
	}
	
	@Override
	public String[] getCommunityBoardCommands()
	{
		return COMMANDS;
	}
	
	@Override
	public boolean parseCommunityBoardCommand(String command, Player player)
	{
		if (!Config.COMMUNITYBOARD_ENABLED)
		{
			player.sendPacket(SystemMessageId.THE_COMMUNITY_SERVER_IS_CURRENTLY_OFFLINE);
			return false;
		}
		
		final String lang = player.getLang();
		
		if (!Config.ENABLE_SUBCLASS_BOARD)
		{
			player.sendMessage(localize(lang, "Subclass Master is currently disabled.", "O Subclass Master esta desativado no momento."));
			return false;
		}
		
		if (command.equals("_bbssubclassmaster"))
		{
			CommunityBoardHandler.getInstance().addBypass(player, "Subclass Master", command);
			CommunityBoardHandler.separateAndSend(getMainMenuPage(player), player);
			return true;
		}
<<<<<<< HEAD
		if ((params.length == 3) && params[1].equals("page"))
		{
			try
			{
				CommunityBoardHandler.separateAndSend(getMainPage(player, Integer.parseInt(params[2])), player);
				return true;
			}
			catch (NumberFormatException _)
			{
				return false;
			}
		}
		final String action = params[1];
		final boolean base = action.equals("change_to_base");
		if ((base && (params.length != 2)) || (!base && ((params.length != 3) || !(action.equals("add_subclass") || action.equals("switch_subclass") || action.equals("remove_subclass") || action.equals("confirm_remove")))))
=======
		
		if (command.equals("_bbssubclassmaster;menu_add"))
>>>>>>> 6662aa1e (Added Vote System, Donate System and a lot of features on Community Board)
		{
			CommunityBoardHandler.separateAndSend(getAddSubclassPage(player), player);
			return true;
		}
		
		if (command.equals("_bbssubclassmaster;menu_change"))
		{
			CommunityBoardHandler.separateAndSend(getChangeSubclassPage(player), player);
			return true;
		}
		
		if (command.equals("_bbssubclassmaster;menu_remove"))
		{
			CommunityBoardHandler.separateAndSend(getRemoveSubclassPage(player), player);
			return true;
		}
		
		if (!canPerformSubclassAction(player, lang))
		{
			CommunityBoardHandler.separateAndSend(getMainMenuPage(player), player);
			return false;
		}
		
		if (command.startsWith("_bbssubclassmaster;add_subclass"))
		{
			final String[] params = command.split(";");
			if (params.length == 3)
			{
				try
				{
					final int classId = Integer.parseInt(params[2]);
					handleAddSubclass(player, classId, lang);
				}
				catch (NumberFormatException _)
				{
					player.sendMessage(localize(lang, "Invalid class parameter.", "Parametro de classe invalido."));
				}
			}
			CommunityBoardHandler.separateAndSend(getAddSubclassPage(player), player);
			return true;
		}
		else if (command.startsWith("_bbssubclassmaster;switch_subclass"))
		{
			final String[] params = command.split(";");
			if (params.length == 3)
			{
				try
				{
					final int classIndex = Integer.parseInt(params[2]);
					handleSwitchSubclass(player, classIndex, lang);
				}
				catch (NumberFormatException _)
				{
					player.sendMessage(localize(lang, "Invalid subclass index.", "Indice de subclasse invalido."));
				}
			}
			CommunityBoardHandler.separateAndSend(getChangeSubclassPage(player), player);
			return true;
		}
		else if (command.startsWith("_bbssubclassmaster;remove_subclass"))
		{
			final String[] params = command.split(";");
			if (params.length == 3)
			{
				try
				{
					final int classIndex = Integer.parseInt(params[2]);
					handleRemoveSubclass(player, classIndex, lang);
				}
				catch (NumberFormatException _)
				{
					player.sendMessage(localize(lang, "Invalid subclass index.", "Indice de subclasse invalido."));
				}
			}
			CommunityBoardHandler.separateAndSend(getRemoveSubclassPage(player), player);
			return true;
		}
		else if (command.equals("_bbssubclassmaster;change_to_base"))
		{
			if (player.getClient().getFloodProtectors().canChangeSubclass())
			{
				if (player.getClassIndex() == 0)
				{
					player.sendMessage(localize(lang, "You are already on your main class.", "Voce ja esta na sua classe principal."));
				}
				else
				{
					player.setActiveClass(0);
					player.sendPacket(SystemMessageId.YOU_HAVE_SUCCESSFULLY_SWITCHED_TO_YOUR_SUBCLASS);
				}
			}
			CommunityBoardHandler.separateAndSend(getChangeSubclassPage(player), player);
			return true;
		}
		
		CommunityBoardHandler.separateAndSend(getMainMenuPage(player), player);
		return true;
	}
	
	// =========================================================
	// HANDLERS DE ACOES
	// =========================================================
	
	private void handleAddSubclass(Player player, int classId, String lang)
	{
		if (!player.getClient().getFloodProtectors().canChangeSubclass())
		{
			return;
		}
		
		if (player.getTotalSubClasses() >= Config.MAX_SUBCLASS)
		{
			player.sendMessage(localize(lang, "You have already reached the subclass limit.", "Voce ja atingiu o limite de subclasses."));
			return;
		}
		
		if (player.getLevel() < MIN_LEVEL_FOR_SUBCLASS)
		{
			player.sendMessage(localize(lang, "You must be at least level " + MIN_LEVEL_FOR_SUBCLASS + " to add a subclass.", "Voce precisa estar pelo menos no nivel " + MIN_LEVEL_FOR_SUBCLASS + " para adicionar uma subclasse."));
			return;
		}
		
		for (SubClassHolder subClass : player.getSubClasses().values())
		{
			if (subClass.getLevel() < MIN_LEVEL_FOR_SUBCLASS)
			{
				player.sendMessage(localize(lang, "All your existing subclasses must be at least level " + MIN_LEVEL_FOR_SUBCLASS + " before adding a new one.", "Todas as suas subclasses atuais precisam estar no nivel " + MIN_LEVEL_FOR_SUBCLASS + " ou superior antes de adicionar outra."));
				return;
			}
		}
		
		final Set<PlayerClass> availableSubs = getAvailableSubClasses(player);
		final PlayerClass newClass = PlayerClass.getPlayerClass(classId);
		if ((newClass == null) || (availableSubs == null) || !availableSubs.contains(newClass))
		{
			player.sendMessage(localize(lang, "You cannot select this class as a subclass.", "Voce nao pode selecionar esta classe como subclasse."));
			return;
		}
		
		final int nextIndex = getNextAvailableSubclassIndex(player);
		if (nextIndex == -1)
		{
			player.sendMessage(localize(lang, "No available subclass slot found.", "Nenhum slot de subclasse disponivel."));
			return;
		}
		
		if ((Config.SUBCLASS_BUY_ITEM_ID > 0) && (Config.SUBCLASS_BUY_ITEM_COUNT > 0))
		{
			final ItemTemplate item = ItemData.getInstance().getTemplate(Config.SUBCLASS_BUY_ITEM_ID);
			final String itemName = (item != null) ? item.getName() : ("ID: " + Config.SUBCLASS_BUY_ITEM_ID);
			
			if (player.getInventory().getInventoryItemCount(Config.SUBCLASS_BUY_ITEM_ID, -1) < Config.SUBCLASS_BUY_ITEM_COUNT)
			{
				player.sendMessage(localize(lang, "To add a subclass, you need " + formatAdena(Config.SUBCLASS_BUY_ITEM_COUNT) + " " + itemName + ".", "Para adicionar uma subclasse, voce precisa de " + formatAdena(Config.SUBCLASS_BUY_ITEM_COUNT) + " " + itemName + "."));
				return;
			}
			
			if (!player.destroyItemByItemId(ItemProcessType.FEE, Config.SUBCLASS_BUY_ITEM_ID, Config.SUBCLASS_BUY_ITEM_COUNT, player, true))
			{
				return;
			}
		}
		
		if (!player.addSubClass(classId, nextIndex))
		{
			player.sendMessage(localize(lang, "Could not add subclass. Please try again.", "Nao foi possivel adicionar a subclasse. Tente novamente."));
			return;
		}
		
		player.setActiveClass(nextIndex);
		player.sendPacket(SystemMessageId.THE_NEW_SUBCLASS_HAS_BEEN_ADDED);
	}
	
	private void handleSwitchSubclass(Player player, int classIndex, String lang)
	{
		if (!player.getClient().getFloodProtectors().canChangeSubclass())
		{
			return;
		}
		
		if ((classIndex < 1) || !player.getSubClasses().containsKey(classIndex))
		{
			player.sendMessage(localize(lang, "Subclass not found.", "Subclasse nao encontrada."));
			return;
		}
		
		if (player.getClassIndex() == classIndex)
		{
			player.sendMessage(localize(lang, "You are already on this subclass.", "Voce ja esta com esta subclasse ativa."));
			return;
		}
		
		player.setActiveClass(classIndex);
		player.sendPacket(SystemMessageId.YOU_HAVE_SUCCESSFULLY_SWITCHED_TO_YOUR_SUBCLASS);
	}
	
	private void handleRemoveSubclass(Player player, int classIndex, String lang)
	{
		if (!player.getClient().getFloodProtectors().canChangeSubclass())
		{
			return;
		}
		
		if (classIndex <= 0)
		{
			player.sendMessage(localize(lang, "The main class cannot be deleted.", "A classe principal nao pode ser removida."));
			return;
		}
		
		final SubClassHolder subClass = player.getSubClasses().get(classIndex);
		if (subClass == null)
		{
			player.sendMessage(localize(lang, "Subclass not found.", "Subclasse nao encontrada."));
			return;
		}
		
		if (player.getClassIndex() == classIndex)
		{
			player.setActiveClass(0);
			player.sendMessage(localize(lang, "You are back on the main class because the subclass you deleted was active.", "Voce voltou para a classe principal porque a subclasse removida estava ativa."));
		}
		
		if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_PROFESSION_CANCEL, player))
		{
			EventDispatcher.getInstance().notifyEventAsync(new OnPlayerProfessionCancel(player, subClass.getId()), player);
		}
		
		player.getSubClasses().remove(classIndex);
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement deleteHennas = con.prepareStatement(DELETE_CHAR_HENNA);
			PreparedStatement deleteShortcuts = con.prepareStatement(DELETE_CHAR_SHORTCUTS);
			PreparedStatement deleteSkillReuse = con.prepareStatement(DELETE_SKILL_SAVE);
			PreparedStatement deleteSkills = con.prepareStatement(DELETE_CHAR_SKILLS);
			PreparedStatement deleteSubclass = con.prepareStatement(DELETE_CHAR_SUBCLASS))
		{
			deleteHennas.setInt(1, player.getObjectId());
			deleteHennas.setInt(3, classIndex);
			for (int slot = 1; slot < 4; slot++)
			{
				final Henna henna = player.getHenna(slot);
				if ((henna != null) && !henna.isAllowedClass(player.getPlayerClass()))
				{
					deleteHennas.setInt(2, slot);
					deleteHennas.execute();
				}
			}
			
			deleteShortcuts.setInt(1, player.getObjectId());
			deleteShortcuts.setInt(2, classIndex);
			deleteShortcuts.execute();
			
			deleteSkillReuse.setInt(1, player.getObjectId());
			deleteSkillReuse.setInt(2, classIndex);
			deleteSkillReuse.execute();
			
			deleteSkills.setInt(1, player.getObjectId());
			deleteSkills.setInt(2, classIndex);
			deleteSkills.execute();
			
			deleteSubclass.setInt(1, player.getObjectId());
			deleteSubclass.setInt(2, classIndex);
			deleteSubclass.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e, () -> "SubClassMasterBoard: Could not delete subclass for " + player.getName() + " at index " + classIndex + ": " + e.getMessage());
		}
		
		player.broadcastUserInfo();
		player.sendMessage(localize(lang, "The subclass has been successfully removed.", "A subclasse foi removida com sucesso."));
	}
	
	// =========================================================
	// RENDERIZACAO HTML (PADRAO VISUAL HOMEBOARD)
	// =========================================================
	
	private String getHeaderHtml(Player player)
	{
		return getMainPage(player, 1);
	}

	private String getMainPage(Player player, int requestedPage)
	{
		final String lang = player.getLang();
<<<<<<< HEAD
		final StringBuilder html = new StringBuilder(4096);
		// Keep the High Five HTML viewer layout shallow and bounded. Do not nest
		// a fixed-height full-page table around the class and action tables.
		html.append("<html><title>Subclass Master</title><body><center><br>");
		html.append("<font name=hs12 color=F2C75C>Subclass Master</font><br>");
		html.append(localize(lang, "Current Class: ", "Classe Atual: "));
		html.append("<font color=32C332>").append(getClassName(player.getActiveClass())).append(" (Lv. ").append(player.getLevel()).append(")</font><br><br>");
		if (player.getTotalSubClasses() >= Config.MAX_SUBCLASS)
		{
			html.append(localize(lang, "You have reached the subclass limit.", "Voce atingiu o limite de subclasses.")).append("<br>");
=======
		final StringBuilder sb = new StringBuilder(1024);
		
		sb.append("<table width=720 height=100 cellpadding=0 cellspacing=0 border=0 background=\"L2UI_CH3.refinewnd_back_Pattern\">");
		sb.append("<tr><td fixwidth=720 fixheight=100 align=center valign=top>");
		sb.append("<table width=720 height=95 cellpadding=0 cellspacing=0 border=0>");
		sb.append("<tr>");
		sb.append("<td width=30></td>");
		sb.append("<td width=80 align=center valign=center><img src=\"icon.skill1349\" width=64 height=64></td>");
		sb.append("<td width=440 align=left valign=center>");
		sb.append("<font name=\"hs12\" color=\"F2C75C\">Subclass Master</font><br1>");
		sb.append("<font color=\"AE9977\">").append(localize(lang, "Current Class: ", "Classe Atual: ")).append(getClassName(player.getActiveClass())).append(" (Lv. ").append(player.getLevel()).append(")</font><br1>");
		sb.append("<font color=\"6B6B6B\">").append(localize(lang, "Manage your character paths and progress.", "Gerencie suas classes e progressao.")).append("</font>");
		sb.append("</td>");
		sb.append("<td width=180 align=center valign=center>");
		sb.append("<table cellpadding=0 cellspacing=1 border=0>");
		sb.append("<tr><td bgcolor=\"222222\" width=170 height=20 align=center>&nbsp;</td></tr>");
		sb.append("<tr><td bgcolor=\"1A1A1A\" width=170 height=18 align=center>&nbsp;</td></tr>");
		sb.append("</table>");
		sb.append("</td>");
		sb.append("<td width=10>&nbsp;</td>");
		sb.append("</tr>");
		sb.append("</table>");
		sb.append("</td></tr>");
		sb.append("</table><br>");
		
		return sb.toString();
	}
	
	private String getMainMenuPage(Player player)
	{
		final String lang = player.getLang();
		final StringBuilder html = new StringBuilder(2048);
		
		html.append("<html noscrollbar><body><center>");
		html.append(getHeaderHtml(player));
		
		// Bloco central com fundo Drawer Bg
		html.append("<table width=720 cellpadding=0 cellspacing=0 border=0 background=\"L2UI_CT1.Windows_DF_Drawer_Bg\">");
		html.append("<tr><td align=center><br>");
		
		html.append("<font color=\"FFE497\" name=\"__SYSTEMWORLDFONT\">").append(localize(lang, "S U B C L A S S   M A N A G E M E N T", "G E R E N C I A M E N T O   D E   S U B C L A S S E S")).append("</font><br><br>");
		
		html.append("<table border=\"0\" cellspacing=\"6\" cellpadding=\"0\" width=\"400\">");
		
		// Botao Adicionar
		html.append("<tr><td align=\"center\">");
		html.append("<button value=\"").append(localize(lang, "Add Subclass", "Adicionar Subclasse")).append("\" action=\"bypass -h _bbssubclassmaster;menu_add\" back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\" width=\"280\" height=\"30\" />");
		html.append("</td></tr>");
		
		// Botao Mudar / Ativar
		html.append("<tr><td align=\"center\">");
		html.append("<button value=\"").append(localize(lang, "Change / Activate Subclass", "Mudar / Ativar Subclasse")).append("\" action=\"bypass -h _bbssubclassmaster;menu_change\" back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\" width=\"280\" height=\"30\" />");
		html.append("</td></tr>");
		
		// Botao Remover
		html.append("<tr><td align=\"center\">");
		html.append("<button value=\"").append(localize(lang, "Remove Subclass", "Remover Subclasse")).append("\" action=\"bypass -h _bbssubclassmaster;menu_remove\" back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\" width=\"280\" height=\"30\" />");
		html.append("</td></tr>");
		
		html.append("</table><br>");
		
		html.append("</td></tr></table><br>");
		
		// Botao Voltar
		html.append("<button value=\"").append(localize(lang, "Back to Services", "Voltar aos Servicos")).append("\" action=\"bypass _bbstop;merchant/servicesMain.html\" back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\" width=\"140\" height=\"25\" />");
		
		html.append("</center></body></html>");
		return html.toString();
	}
	
	private String getAddSubclassPage(Player player)
	{
		final String lang = player.getLang();
		final StringBuilder html = new StringBuilder(3072);
		
		html.append("<html noscrollbar><body><center>");
		html.append(getHeaderHtml(player));
		
		html.append("<table width=720 cellpadding=0 cellspacing=0 border=0 background=\"L2UI_CT1.Windows_DF_Drawer_Bg\">");
		html.append("<tr><td align=center><br>");
		
		html.append("<font color=\"FFE497\" name=\"__SYSTEMWORLDFONT\">").append(localize(lang, "A D D   N E W   S U B C L A S S", "A D I C I O N A R   N O V A   S U B C L A S S E")).append("</font><br><br>");
		
		if (player.getTotalSubClasses() >= Config.MAX_SUBCLASS)
		{
			html.append("<font color=\"AAAAAA\">").append(localize(lang, "You have already reached the subclass limit (" + Config.MAX_SUBCLASS + ").", "Voce ja atingiu o limite de subclasses (" + Config.MAX_SUBCLASS + ").")).append("</font><br><br>");
		}
		else if (player.getLevel() < MIN_LEVEL_FOR_SUBCLASS)
		{
			html.append("<font color=\"FF5555\">").append(localize(lang, "You must be at least level " + MIN_LEVEL_FOR_SUBCLASS + " to add a subclass.", "Voce precisa estar pelo menos no nivel " + MIN_LEVEL_FOR_SUBCLASS + " para adicionar uma subclasse.")).append("</font><br><br>");
>>>>>>> 6662aa1e (Added Vote System, Donate System and a lot of features on Community Board)
		}
		else
		{
			boolean canAdd = true;
			for (SubClassHolder subClass : player.getSubClasses().values())
			{
<<<<<<< HEAD
				final ItemTemplate item = ItemData.getInstance().getTemplate(Config.SUBCLASS_BUY_ITEM_ID);
				final String itemName = item == null ? "ID: " + Config.SUBCLASS_BUY_ITEM_ID : item.getName();
				html.append(localize(lang, "Cost to add: ", "Custo para adicionar: ")).append(formatAdena(Config.SUBCLASS_BUY_ITEM_COUNT)).append(' ').append(escapeHtml(itemName)).append("<br>");
			}
			final Set<PlayerClass> available = getAvailableSubClasses(player);
			final java.util.List<PlayerClass> classes = available == null ? java.util.List.of() : available.stream().sorted(java.util.Comparator.comparingInt(PlayerClass::getId)).toList();
			if (classes.isEmpty())
			{
				html.append(localize(lang, "No subclasses available.", "Nenhuma subclasse disponivel.")).append("<br>");
			}
			else
			{
				final int pages = ((classes.size() - 1) / CLASSES_PER_PAGE) + 1;
				final int page = Math.clamp(requestedPage, 1, pages);
				final int start = (page - 1) * CLASSES_PER_PAGE;
				final int end = Math.min(start + CLASSES_PER_PAGE, classes.size());
				html.append("<font color=F2C75C>").append(localize(lang, "Select a Subclass to Add:", "Selecione uma Subclasse para Adicionar:")).append("</font><br>");
				html.append("<table width=700 cellpadding=2 cellspacing=2 border=0>");
				for (int i = start; i < end; i += 3)
				{
					html.append("<tr>");
					for (int column = 0; column < 3; column++)
					{
						html.append("<td width=220 align=center>");
						if ((i + column) < end)
						{
							final PlayerClass sub = classes.get(i + column);
							html.append(button(getClassName(sub.getId()), "add_subclass;" + sub.getId(), 210));
						}
						html.append("</td>");
=======
				if (subClass.getLevel() < MIN_LEVEL_FOR_SUBCLASS)
				{
					canAdd = false;
					break;
				}
			}
			
			if (!canAdd)
			{
				html.append("<font color=\"FF5555\">").append(localize(lang, "All your existing subclasses must be at least level " + MIN_LEVEL_FOR_SUBCLASS + ".", "Todas as suas subclasses atuais precisam estar no nivel " + MIN_LEVEL_FOR_SUBCLASS + ".")).append("</font><br><br>");
			}
			else
			{
				if ((Config.SUBCLASS_BUY_ITEM_ID > 0) && (Config.SUBCLASS_BUY_ITEM_COUNT > 0))
				{
					final ItemTemplate item = ItemData.getInstance().getTemplate(Config.SUBCLASS_BUY_ITEM_ID);
					final String itemName = (item != null) ? item.getName() : ("ID: " + Config.SUBCLASS_BUY_ITEM_ID);
					final String itemIcon = ((item != null) && (item.getIcon() != null)) ? item.getIcon() : "icon.etc_question_mark_i00";
					
					html.append("<table cellpadding=\"0\" cellspacing=\"0\" border=\"0\"><tr>");
					html.append("<td><img src=\"").append(itemIcon).append("\" width=\"32\" height=\"32\"></td>");
					html.append("<td>&nbsp; <font color=\"CDB67F\">").append(localize(lang, "Required: ", "Requisito: ")).append("</font><font color=\"FFFFFF\">").append(formatAdena(Config.SUBCLASS_BUY_ITEM_COUNT)).append(" ").append(itemName).append("</font></td>");
					html.append("</tr></table><br>");
				}
				
				final Set<PlayerClass> availableSubs = getAvailableSubClasses(player);
				if ((availableSubs == null) || availableSubs.isEmpty())
				{
					html.append("<font color=\"FF5555\">").append(localize(lang, "No subclasses available.", "Nenhuma subclasse disponivel.")).append("</font><br><br>");
				}
				else
				{
					html.append("<font color=\"F2C75C\">").append(localize(lang, "Select a Subclass to Add:", "Selecione uma Subclasse para Adicionar:")).append("</font><br><br>");
					
					// Grade em 2 colunas padronizada
					html.append("<table border=\"0\" cellspacing=\"4\" cellpadding=\"2\" width=\"640\">");
					
					int colCount = 0;
					for (PlayerClass possible : availableSubs)
					{
						if ((colCount % 2) == 0)
						{
							if (colCount != 0)
							{
								html.append("</tr>");
							}
							html.append("<tr>");
						}
						
						final String className = getClassName(possible.getId());
						
						html.append("<td fixwidth=\"315\" align=\"center\">");
						html.append("<button value=\"").append(className).append("\" action=\"bypass -h _bbssubclassmaster;add_subclass;").append(possible.getId()).append("\" back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\" width=\"300\" height=\"26\" />");
						html.append("</td>");
						
						colCount++;
>>>>>>> 6662aa1e (Added Vote System, Donate System and a lot of features on Community Board)
					}
					
					if (colCount > 0)
					{
						if ((colCount % 2) != 0)
						{
							html.append("<td fixwidth=\"315\"></td>");
						}
						html.append("</tr>");
					}
					html.append("</table><br>");
				}
<<<<<<< HEAD
				html.append("</table><br>");
				if (page > 1) { html.append("<a action=\"bypass _bbssubclassmaster;page;").append(page - 1).append("\">").append(localize(lang, "Previous", "Anterior")).append("</a> "); }
				html.append(localize(lang, "Page ", "Pagina ")).append(page).append('/').append(pages);
				if (page < pages) { html.append(" <a action=\"bypass _bbssubclassmaster;page;").append(page + 1).append("\">").append(localize(lang, "Next", "Proxima")).append("</a>"); }
				html.append("<br>");
			}
		}
		html.append("<br><font color=F2C75C>").append(localize(lang, "Your Classes and Subclasses", "Suas Classes e Subclasses")).append("</font><br>");
		html.append("<table width=700 cellpadding=3 cellspacing=1 border=0>");
		html.append("<tr bgcolor=1F1F1F><td width=420>[Base] ").append(getClassName(player.getBaseClass())).append("</td><td width=130 align=center>");
		html.append(player.getClassIndex() == 0 ? localize(lang, "Active", "Ativa") : button(localize(lang, "Activate", "Ativar"), "change_to_base", 110));
		html.append("</td><td width=130></td></tr>");
		for (SubClassHolder sub : player.getSubClasses().values().stream().sorted(java.util.Comparator.comparingInt(SubClassHolder::getClassIndex)).toList())
		{
			html.append("<tr bgcolor=161616><td width=420>[Sub #").append(sub.getClassIndex()).append("] ").append(getClassName(sub.getId())).append(" (Lv. ").append(sub.getLevel()).append(")</td><td width=130 align=center>");
			html.append(player.getClassIndex() == sub.getClassIndex() ? localize(lang, "Active", "Ativa") : button(localize(lang, "Activate", "Ativar"), "switch_subclass;" + sub.getClassIndex(), 110));
			html.append("</td><td width=130 align=center>").append(button(localize(lang, "Delete", "Remover"), "confirm_remove;" + sub.getClassIndex(), 110)).append("</td></tr>");
		}
		html.append("</table><br><button value=\"").append(localize(lang, "Back", "Voltar")).append("\" action=\"bypass _bbstop;merchant/blacksmithMain.html\" width=120 height=24 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
		return html.append("</center></body></html>").toString();
=======
			}
		}
		
		html.append("</td></tr></table><br>");
		html.append("<button value=\"").append(localize(lang, "Back", "Voltar")).append("\" action=\"bypass -h _bbssubclassmaster\" back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\" width=\"140\" height=\"25\" />");
		
		html.append("</center></body></html>");
		return html.toString();
	}
	
	private String getChangeSubclassPage(Player player)
	{
		final String lang = player.getLang();
		final StringBuilder html = new StringBuilder(2048);
		
		html.append("<html noscrollbar><body><center>");
		html.append(getHeaderHtml(player));
		
		html.append("<table width=720 cellpadding=0 cellspacing=0 border=0 background=\"L2UI_CT1.Windows_DF_Drawer_Bg\">");
		html.append("<tr><td align=center><br>");
		
		html.append("<font color=\"FFE497\" name=\"__SYSTEMWORLDFONT\">").append(localize(lang, "S W I T C H   C L A S S", "A LT E R A R   O U   A T I V A R   C L A S S E")).append("</font><br><br>");
		
		html.append("<table border=\"0\" cellspacing=\"2\" cellpadding=\"4\" width=\"640\" bgcolor=\"161616\">");
		
		// Linha da Classe Base
		final boolean isBaseActive = player.getClassIndex() == 0;
		html.append("<tr>");
		html.append("<td width=\"400\" align=\"left\"><font color=\"CDB67F\">[Base] ").append(getClassName(player.getBaseClass())).append("</font>");
		if (isBaseActive)
		{
			html.append(" <font color=\"32C332\">[").append(localize(lang, "Active", "Ativa")).append("]</font>");
		}
		html.append("</td>");
		
		html.append("<td width=\"240\" align=\"center\">");
		if (!isBaseActive)
		{
			html.append("<button value=\"").append(localize(lang, "Activate", "Ativar")).append("\" action=\"bypass -h _bbssubclassmaster;change_to_base\" back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\" width=\"130\" height=\"24\" />");
		}
		else
		{
			html.append("<font color=\"4F4F4F\">-</font>");
		}
		html.append("</td>");
		html.append("</tr>");
		
		// Linhas das Subclasses
		for (SubClassHolder sub : player.getSubClasses().values())
		{
			final boolean isActive = player.getClassIndex() == sub.getClassIndex();
			final String subName = getClassName(sub.getId());
			
			html.append("<tr>");
			html.append("<td width=\"400\" align=\"left\"><font color=\"FFFFFF\">[Sub #").append(sub.getClassIndex()).append("] ").append(subName).append(" (Lv. ").append(sub.getLevel()).append(")</font>");
			if (isActive)
			{
				html.append(" <font color=\"32C332\">[").append(localize(lang, "Active", "Ativa")).append("]</font>");
			}
			html.append("</td>");
			
			html.append("<td width=\"240\" align=\"center\">");
			if (!isActive)
			{
				html.append("<button value=\"").append(localize(lang, "Activate", "Ativar")).append("\" action=\"bypass -h _bbssubclassmaster;switch_subclass;").append(sub.getClassIndex()).append("\" back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\" width=\"130\" height=\"24\" />");
			}
			else
			{
				html.append("<font color=\"4F4F4F\">-</font>");
			}
			html.append("</td>");
			html.append("</tr>");
		}
		
		html.append("</table><br>");
		
		html.append("</td></tr></table><br>");
		html.append("<button value=\"").append(localize(lang, "Back", "Voltar")).append("\" action=\"bypass -h _bbssubclassmaster\" back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\" width=\"140\" height=\"25\" />");
		
		html.append("</center></body></html>");
		return html.toString();
	}
	
	private String getRemoveSubclassPage(Player player)
	{
		final String lang = player.getLang();
		final StringBuilder html = new StringBuilder(2048);
		
		html.append("<html noscrollbar><body><center>");
		html.append(getHeaderHtml(player));
		
		html.append("<table width=720 cellpadding=0 cellspacing=0 border=0 background=\"L2UI_CT1.Windows_DF_Drawer_Bg\">");
		html.append("<tr><td align=center><br>");
		
		html.append("<font color=\"FFE497\" name=\"__SYSTEMWORLDFONT\">").append(localize(lang, "R E M O V E   S U B C L A S S", "R E M O V E R   S U B C L A S S E")).append("</font><br><br>");
		
		if (player.getSubClasses().isEmpty())
		{
			html.append("<font color=\"AAAAAA\">").append(localize(lang, "You don't have any subclasses to remove.", "Voce nao possui subclasses para remover.")).append("</font><br><br>");
		}
		else
		{
			html.append("<table border=\"0\" cellspacing=\"2\" cellpadding=\"4\" width=\"640\" bgcolor=\"161616\">");
			
			for (SubClassHolder sub : player.getSubClasses().values())
			{
				final String subName = getClassName(sub.getId());
				
				html.append("<tr>");
				html.append("<td width=\"400\" align=\"left\"><font color=\"FFFFFF\">[Sub #").append(sub.getClassIndex()).append("] ").append(subName).append(" (Lv. ").append(sub.getLevel()).append(")</font></td>");
				html.append("<td width=\"240\" align=\"center\">");
				html.append("<button value=\"").append(localize(lang, "Delete", "Remover")).append("\" action=\"bypass -h _bbssubclassmaster;remove_subclass;").append(sub.getClassIndex()).append("\" back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\" width=\"130\" height=\"24\" />");
				html.append("</td>");
				html.append("</tr>");
			}
			
			html.append("</table><br>");
		}
		
		html.append("</td></tr></table><br>");
		html.append("<button value=\"").append(localize(lang, "Back", "Voltar")).append("\" action=\"bypass -h _bbssubclassmaster\" back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\" width=\"140\" height=\"25\" />");
		
		html.append("</center></body></html>");
		return html.toString();
>>>>>>> 6662aa1e (Added Vote System, Donate System and a lot of features on Community Board)
	}

	private static String button(String label, String action, int width)
	{
		// Labels are escaped at their source; actions contain only fixed names and IDs.
		return "<button value=\"" + label + "\" action=\"bypass _bbssubclassmaster;" + action + "\" width=" + width + " height=24 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">";
	}

	private static String escapeHtml(String text)
	{
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
	}

	// =========================================================
	// REGRAS DE DISPONIBILIDADE DE SUBCLASSE (VILLAGEMASTER)
	// =========================================================
	
	private Set<PlayerClass> getAvailableSubClasses(Player player)
	{
		final int currentBaseId = player.getBaseClass();
		final PlayerClass baseCID = PlayerClass.getPlayerClass(currentBaseId);
		if (baseCID == null)
		{
			return Collections.emptySet();
		}
		
		final int baseClassId = (baseCID.level() > 2) ? baseCID.getParent().getId() : currentBaseId;
		final Set<PlayerClass> availSubs = getSubclasses(player, baseClassId);
		if ((availSubs != null) && !availSubs.isEmpty())
		{
			availSubs.removeIf(pclass ->
			{
				for (SubClassHolder prevSubClass : player.getSubClasses().values())
				{
					final PlayerClass subClassId = PlayerClass.getPlayerClass(prevSubClass.getId());
					if ((subClassId != null) && subClassId.equalsOrChildOf(pclass))
					{
						return true;
					}
				}
				return false;
			});
		}
		return availSubs;
	}
	
	private Set<PlayerClass> getSubclasses(Player player, int classId)
	{
		Set<PlayerClass> subclasses = null;
		final PlayerClass pClass = PlayerClass.getPlayerClass(classId);
		if (CategoryData.getInstance().isInCategory(CategoryType.THIRD_CLASS_GROUP, classId) || CategoryData.getInstance().isInCategory(CategoryType.FOURTH_CLASS_GROUP, classId))
		{
			subclasses = EnumSet.copyOf(MAIN_SUBCLASS_SET);
			subclasses.remove(pClass);
			
			if (player.getRace() == Race.KAMAEL)
			{
				for (PlayerClass cid : PlayerClass.values())
				{
					if (cid.getRace() != Race.KAMAEL)
					{
						subclasses.remove(cid);
					}
				}
				
				if (player.getAppearance().isFemale())
				{
					subclasses.remove(PlayerClass.MALE_SOULBREAKER);
				}
				else
				{
					subclasses.remove(PlayerClass.FEMALE_SOULBREAKER);
				}
				
				if (!player.getSubClasses().containsKey(2) || (player.getSubClasses().get(2).getLevel() < 75))
				{
					subclasses.remove(PlayerClass.INSPECTOR);
				}
			}
			else
			{
				if (player.getRace() == Race.ELF)
				{
					for (PlayerClass cid : PlayerClass.values())
					{
						if (cid.getRace() == Race.DARK_ELF)
						{
							subclasses.remove(cid);
						}
					}
				}
				else if (player.getRace() == Race.DARK_ELF)
				{
					for (PlayerClass cid : PlayerClass.values())
					{
						if (cid.getRace() == Race.ELF)
						{
							subclasses.remove(cid);
						}
					}
				}
				
				for (PlayerClass cid : PlayerClass.values())
				{
					if (cid.getRace() == Race.KAMAEL)
					{
						subclasses.remove(cid);
					}
				}
			}
			
			final Set<PlayerClass> unavailableClasses = SUBCLASS_SET_MAP.get(pClass);
			if (unavailableClasses != null)
			{
				subclasses.removeAll(unavailableClasses);
			}
		}
		
		if (subclasses != null)
		{
			final PlayerClass currClassId = player.getPlayerClass();
			subclasses.removeIf(currClassId::equalsOrChildOf);
		}
		return subclasses;
	}
	
	// =========================================================
	// VALIDACOES DE ESTADO E UTILITARIOS
	// =========================================================
	
	private boolean canPerformSubclassAction(Player player, String lang)
	{
		if (player.isCastingNow() || player.isCastingSimultaneouslyNow() || player.isAllSkillsDisabled() || player.isChangingClass())
		{
			player.sendPacket(SystemMessageId.SUBCLASSES_MAY_NOT_BE_CREATED_OR_CHANGED_WHILE_A_SKILL_IS_IN_USE);
			return false;
		}
		if (player.isInCombat() || player.isInDuel() || (player.getPvpFlag() > 0) || player.isInsideZone(ZoneId.SIEGE) || player.isInsideZone(ZoneId.PVP))
		{
			player.sendMessage(localize(lang, "You cannot manage subclasses while in combat, PvP, or Siege.", "Voce nao pode gerenciar subclasses em combate, PvP ou Siege."));
			return false;
		}
		if (player.isInOlympiadMode() || player.isOnEvent() || player.isRegisteredOnEvent())
		{
			player.sendMessage(localize(lang, "You cannot manage subclasses during Olympiad or Events.", "Voce nao pode gerenciar subclasses nas Olimpiadas ou em Eventos."));
			return false;
		}
		if (OlympiadManager.getInstance().isRegisteredInComp(player))
		{
			OlympiadManager.getInstance().unRegisterNoble(player);
		}
		if (player.isAlikeDead())
		{
			return false;
		}
		if ((player.getTransformation() != null) || player.isMounted())
		{
			player.sendMessage(localize(lang, "You cannot manage subclasses while transformed or mounted.", "Voce nao pode gerenciar subclasses transformado ou montado."));
			return false;
		}
		if (player.hasSummon())
		{
			player.sendMessage(localize(lang, "You cannot manage subclasses while a pet or servitor is summoned.", "Voce nao pode gerenciar subclasses com um pet ou summon ativo."));
			return false;
		}
		if (player.isInStoreMode() || player.isProcessingTransaction() || player.isEnchanting())
		{
			player.sendMessage(localize(lang, "You cannot manage subclasses while trading, in store mode, or enchanting.", "Voce nao pode gerenciar subclasses negociando, em loja ou encantando."));
			return false;
		}
		if (Config.COMMUNITYBOARD_KARMA_DISABLED && (player.getKarma() > 0))
		{
			player.sendMessage(localize(lang, "Players with Karma cannot use the Community Board.", "Jogadores com Karma nao podem usar o Community Board."));
			return false;
		}
		if (Config.COMMUNITYBOARD_PEACE_ONLY && !player.isInsideZone(ZoneId.PEACE))
		{
			player.sendMessage(localize(lang, "Community Board cannot be used out of peace zone.", "O Community Board so pode ser usado em zona de paz."));
			return false;
		}
		if (!player.isInventoryUnder90(true))
		{
			player.sendPacket(SystemMessageId.A_SUB_CLASS_CANNOT_BE_CREATED_OR_CHANGED_BECAUSE_YOU_HAVE_EXCEEDED_YOUR_INVENTORY_LIMIT);
			return false;
		}
		if (player.getWeightPenalty() >= 2)
		{
			player.sendPacket(SystemMessageId.A_SUB_CLASS_CANNOT_BE_CREATED_OR_CHANGED_WHILE_YOU_ARE_OVER_YOUR_WEIGHT_LIMIT);
			return false;
		}
		return true;
	}
	
	private static int getNextAvailableSubclassIndex(Player player)
	{
		for (int idx = 1; idx <= Config.MAX_SUBCLASS; idx++)
		{
			if (!player.getSubClasses().containsKey(idx))
			{
				return idx;
			}
		}
		return -1;
	}
	
	private static String getClassName(int classId)
	{
		final ClassInfoHolder info = ClassListData.getInstance().getClass(classId);
		return escapeHtml((info != null) ? info.getClassName() : ("Class #" + classId));
	}
}