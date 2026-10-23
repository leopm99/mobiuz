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
import com.l2journey.gameserver.model.quest.QuestState;
import com.l2journey.gameserver.model.item.ItemTemplate;
import com.l2journey.gameserver.model.item.enums.ItemProcessType;
import com.l2journey.gameserver.model.olympiad.OlympiadManager;
import com.l2journey.gameserver.model.zone.ZoneId;
import com.l2journey.gameserver.network.SystemMessageId;

/**
 * Community Board Subclass Master. Baseado nas regras oficiais de VillageMaster e Player do L2Journey.
 * @author Debug, KingHanker
 */
public class SubClassMasterBoard implements IParseBoardHandler
{
	private static final Logger LOGGER = Logger.getLogger(SubClassMasterBoard.class.getName());
	
	private static final String[] COMMANDS =
	{
		"_bbssubclassmaster"
	};
	
	private static final int MIN_LEVEL_FOR_SUBCLASS = 75;
	
	// Class-scoped data cleared when permanently removing a subclass.
	private static final String DELETE_CHAR_HENNA = "DELETE FROM character_hennas WHERE charId=? AND class_index=?";
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
		if (!Config.ENABLE_COMMUNITY_BOARD || !Config.COMMUNITYBOARD_ENABLED)
		{
			player.sendPacket(SystemMessageId.THE_COMMUNITY_SERVER_IS_CURRENTLY_OFFLINE);
			return false;
		}
		final String lang = player.getLang();
		if (!Config.SUBCLASS_MASTER_BOARD)
		{
			player.sendMessage(localize(lang, "Subclass Master is currently disabled.", "O Subclass Master esta desativado no momento."));
			return false;
		}
		final String[] params = command.split(";", -1);
		if (!params[0].equals("_bbssubclassmaster"))
		{
			return false;
		}
		if (params.length == 1)
		{
			CommunityBoardHandler.getInstance().addBypass(player, "Subclass Master", command);
			CommunityBoardHandler.separateAndSend(getMainPage(player), player);
			return true;
		}
		final String action = params[1];
		final boolean base = action.equals("change_to_base");
		if ((base && (params.length != 2)) || (!base && ((params.length != 3) || !(action.equals("add_subclass") || action.equals("switch_subclass") || action.equals("remove_subclass") || action.equals("confirm_remove")))))
		{
			return false;
		}
		final int value;
		try
		{
			value = base ? 0 : Integer.parseInt(params[2]);
		}
		catch (NumberFormatException _)
		{
			player.sendMessage(localize(lang, "Invalid subclass parameter.", "Parametro de subclasse invalido."));
			return false;
		}
		// Serialize board actions, including validation, payment and persistence.
		synchronized (player)
		{
			if (!canPerformSubclassAction(player, lang))
			{
				return false;
			}
			switch (action)
			{
				case "add_subclass" -> handleAddSubclass(player, value, lang);
				case "switch_subclass", "change_to_base" -> handleSwitchSubclass(player, value, lang);
				case "confirm_remove" ->
				{
					if ((value < 1) || !player.getSubClasses().containsKey(value))
					{
						return false;
					}
					final String name = getClassName(player.getSubClasses().get(value).getId());
					CommunityBoardHandler.separateAndSend("<html><body><center><br><font color=F2C75C>Subclass Master</font><br>"
						+ localize(lang, "Permanently delete ", "Remover permanentemente ") + name + "?<br>"
						+ localize(lang, "Its levels, skills and dyes will be lost.", "Seus niveis, skills e tatuagens serao perdidos.")
						+ "<br><a action=\"bypass _bbssubclassmaster;remove_subclass;" + value + "\">" + localize(lang, "Confirm removal", "Confirmar remocao")
						+ "</a><br><a action=\"bypass _bbssubclassmaster\">" + localize(lang, "Cancel", "Cancelar") + "</a></center></body></html>", player);
					return true;
				}
				case "remove_subclass" -> handleRemoveSubclass(player, value, lang);
				default -> { return false; }
			}
			CommunityBoardHandler.separateAndSend(getMainPage(player), player);
		}
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
		
		// Verifica se todas as subclasses existentes ja estao no nivel 75+ (regra oficial do VillageMaster)
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
		
		// Encontra o primeiro classIndex livre (1 ate Config.MAX_SUBCLASS)
		final int nextIndex = getNextAvailableSubclassIndex(player);
		if (nextIndex == -1)
		{
			player.sendMessage(localize(lang, "No available subclass slot found.", "Nenhum slot de subclasse disponivel."));
			return;
		}
		
		if (!Config.ALT_GAME_SUBCLASS_WITHOUT_QUESTS && !player.isNoble())
		{
			final QuestState fate = player.getQuestState("Q00234_FatesWhisper");
			final QuestState mimir = player.getQuestState("Q00235_MimirsElixir");
			if ((fate == null) || !fate.isCompleted() || (mimir == null) || !mimir.isCompleted())
			{
				player.sendMessage(localize(lang, "Complete Fate's Whisper and Mimir's Elixir before adding a subclass.", "Complete Fate's Whisper e Mimir's Elixir antes de adicionar uma subclasse."));
				return;
			}
		}
		boolean paid = false;
		// Cobranca de item (se configurado)
		if (Config.SUBCLASS_BUY_ITEM_COUNT > 0)
		{
			final ItemTemplate item = ItemData.getInstance().getTemplate(Config.SUBCLASS_BUY_ITEM_ID);
			final String itemName = (item != null) ? item.getName() : ("ID: " + Config.SUBCLASS_BUY_ITEM_ID);
			
			if (player.getInventory().getInventoryItemCount(Config.SUBCLASS_BUY_ITEM_ID, -1) < Config.SUBCLASS_BUY_ITEM_COUNT)
			{
				player.sendMessage(localize(lang, "To add a subclass, you need " + formatAdena(Config.SUBCLASS_BUY_ITEM_COUNT) + " " + itemName + ".", "Para adicionar uma subclasse, voce precisa de " + formatAdena(Config.SUBCLASS_BUY_ITEM_COUNT) + " " + itemName + "."));
				return;
			}
			
			if (item == null)
			{
				player.sendMessage(localize(lang, "Invalid subclass fee configuration.", "Configuracao de custo de subclasse invalida."));
				return;
			}
			if (!player.destroyItemByItemId(ItemProcessType.FEE, Config.SUBCLASS_BUY_ITEM_ID, Config.SUBCLASS_BUY_ITEM_COUNT, player, true))
			{
				return;
			}
			paid = true;
		}
		
		// Usa o metodo nativo Player.addSubClass que ja persiste no banco e concede as skills ate o nv 40
		if (!player.addSubClass(classId, nextIndex))
		{
			if (paid)
			{
				player.addItem(ItemProcessType.REFUND, Config.SUBCLASS_BUY_ITEM_ID, Config.SUBCLASS_BUY_ITEM_COUNT, player, true);
			}
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
		
		if ((classIndex < 0) || ((classIndex > 0) && !player.getSubClasses().containsKey(classIndex)))
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
		if (player.getClassIndex() == classIndex)
		{
			player.sendPacket(SystemMessageId.YOU_HAVE_SUCCESSFULLY_SWITCHED_TO_YOUR_SUBCLASS);
		}
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
		
		// Se a subclasse deletada estiver ativa no momento, volta para a classe base primeiro
		// (setActiveClass(0) salva dados, limpa cubics/summons e restaura a classe base)
		if (player.getClassIndex() == classIndex)
		{
			player.setActiveClass(0);
			if (player.getClassIndex() != 0)
			{
				return;
			}
			player.sendMessage(localize(lang, "You have been switched back to your main class because your active subclass was removed.", "Voce voltou para a classe principal porque a subclasse ativa foi removida."));
		}
		
		// Delete all class-scoped data atomically. Keep memory unchanged on SQL failure.
		try (Connection con = DatabaseFactory.getConnection())
		{
			con.setAutoCommit(false);
			try
			{
				for (String sql : new String[] { DELETE_CHAR_HENNA, DELETE_CHAR_SHORTCUTS, DELETE_SKILL_SAVE, DELETE_CHAR_SKILLS, DELETE_CHAR_SUBCLASS })
				{
					try (PreparedStatement statement = con.prepareStatement(sql))
					{
						statement.setInt(1, player.getObjectId());
						statement.setInt(2, classIndex);
						statement.executeUpdate();
					}
				}
				con.commit();
			}
			catch (Exception e)
			{
				con.rollback();
				throw e;
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e, () -> "SubClassMasterBoard: Could not delete subclass for " + player.getName() + " at index " + classIndex);
			player.sendMessage(localize(lang, "Could not remove subclass. Please try again.", "Nao foi possivel remover a subclasse. Tente novamente."));
			return;
		}
		player.getSubClasses().remove(classIndex);
		if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_PROFESSION_CANCEL, player))
		{
			EventDispatcher.getInstance().notifyEventAsync(new OnPlayerProfessionCancel(player, subClass.getId()), player);
		}

		player.broadcastUserInfo();
		player.sendMessage(localize(lang, "Subclass removed successfully.", "Subclasse removida com sucesso."));
	}
	
	private String getMainPage(Player player)
	{
		final String lang = player.getLang();
		final StringBuilder html = new StringBuilder(4096);
		
		html.append("<html noscrollbar><body><center><br>");
		html.append("<table width=755 border=0 cellpadding=0 cellspacing=0>");
		html.append("<tr><td align=center>");
		html.append("<table width=750 height=480 border=0 cellpadding=4 cellspacing=2 background=\"L2UI_CT1.Windows_DF_Drawer_Bg\">");
		
		// Header
		html.append("<tr><td height=36 align=center bgcolor=\"2A2418\">");
		html.append("<font name=\"hs12\" color=\"F2C75C\">Subclass Master</font><br1>");
		html.append("<font color=\"AE9977\">").append(localize(lang, "Current Class: ", "Classe Atual: ")).append("</font>");
		html.append("<font color=\"32C332\">").append(getClassName(player.getActiveClass())).append(" (Lv. ").append(player.getLevel()).append(")</font>");
		html.append("</td></tr>");
		
		// Secao: Adicionar Subclasse
		html.append("<tr><td align=center valign=top>");
		if (player.getTotalSubClasses() >= Config.MAX_SUBCLASS)
		{
			html.append("<br><font color=\"AAAAAA\">").append(localize(lang, "You have already added the maximum of " + Config.MAX_SUBCLASS + " subclasses.", "Voce ja adicionou o limite maximo de " + Config.MAX_SUBCLASS + " subclasses.")).append("</font><br>");
		}
		else
		{
			if (Config.SUBCLASS_BUY_ITEM_COUNT > 0)
			{
				final ItemTemplate item = ItemData.getInstance().getTemplate(Config.SUBCLASS_BUY_ITEM_ID);
				final String itemName = (item != null) ? item.getName() : ("ID: " + Config.SUBCLASS_BUY_ITEM_ID);
				final String itemIcon = ((item != null) && (item.getIcon() != null)) ? item.getIcon() : "icon.etc_question_mark_i00";
				
				html.append("<table border=0 cellpadding=2 cellspacing=2 bgcolor=\"161616\" width=360><tr>");
				html.append("<td width=36 align=center><img src=\"").append(itemIcon).append("\" width=32 height=32></td>");
				html.append("<td width=310 align=left><font color=\"CDB67F\">").append(localize(lang, "Required to Add:", "Requisito para Adicionar:")).append("</font><br1>");
				html.append("<font color=\"FFFFFF\">").append(formatAdena(Config.SUBCLASS_BUY_ITEM_COUNT)).append(" ").append(itemName).append("</font></td>");
				html.append("</tr></table><br1>");
			}
			
			final Set<PlayerClass> availableSubs = getAvailableSubClasses(player);
			if ((availableSubs == null) || availableSubs.isEmpty())
			{
				html.append("<br><font color=\"FF6347\">").append(localize(lang, "No subclasses available. Make sure your class is at least 2nd job (Lv. 75+).", "Nenhuma subclasse disponivel. Certifique-se de estar pelo menos na 2a classe (Nv. 75+).")).append("</font><br>");
			}
			else
			{
				html.append("<font color=\"F2C75C\">").append(localize(lang, "Select a Subclass to Add:", "Selecione uma Subclasse para Adicionar:")).append("</font><br1>");
				html.append("<table border=0 cellspacing=2 cellpadding=2 width=730>");
				
				int colCount = 0;
				for (PlayerClass subClass : availableSubs)
				{
					if ((colCount % 4) == 0)
					{
						if (colCount != 0)
						{
							html.append("</tr>");
						}
						html.append("<tr>");
					}
					
					final String className = getClassName(subClass.getId());
					html.append("<td fixwidth=180 align=center>");
					html.append("<button value=\"").append(className).append("\" action=\"bypass -h _bbssubclassmaster;add_subclass;").append(subClass.getId()).append("\" width=170 height=24 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
					html.append("</td>");
					colCount++;
				}
				
				if (colCount > 0)
				{
					while ((colCount % 4) != 0)
					{
						html.append("<td fixwidth=180></td>");
						colCount++;
					}
					html.append("</tr>");
				}
				html.append("</table>");
			}
		}
		html.append("</td></tr>");
		
		// Secao: Classes e Subclasses do Jogador
		html.append("<tr><td height=24 align=center bgcolor=\"2A2418\">");
		html.append("<font name=\"hs9\" color=\"F2C75C\">").append(localize(lang, "Your Classes & Subclasses", "Suas Classes e Subclasses")).append("</font>");
		html.append("</td></tr>");
		
		html.append("<tr><td align=center valign=top>");
		html.append("<table width=720 border=0 cellspacing=1 cellpadding=3>");
		
		// Linha da Classe Base
		final boolean isBaseActive = player.getClassIndex() == 0;
		html.append("<tr bgcolor=\"1F1F1F\">");
		html.append("<td width=420 align=left><font color=\"CDB67F\">[Base] ").append(getClassName(player.getBaseClass())).append("</font> ");
		html.append(isBaseActive ? "<font color=\"32C332\">(" + localize(lang, "Active", "Ativa") + ")</font>" : "").append("</td>");
		html.append("<td width=150 align=center>");
		if (!isBaseActive)
		{
			html.append("<button value=\"").append(localize(lang, "Activate", "Ativar")).append("\" action=\"bypass -h _bbssubclassmaster;change_to_base\" width=100 height=22 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
		}
		html.append("</td>");
		html.append("<td width=150 align=center><font color=\"4F4F4F\">-</font></td>");
		html.append("</tr>");
		
		// Linhas das Subclasses
		int rowIdx = 0;
		for (SubClassHolder sub : player.getSubClasses().values().stream().sorted(java.util.Comparator.comparingInt(SubClassHolder::getClassIndex)).toList())
		{
			final String bg = ((rowIdx++ % 2) == 0) ? "161616" : "1F1F1F";
			final boolean isActive = player.getClassIndex() == sub.getClassIndex();
			final String subName = getClassName(sub.getId());
			
			html.append("<tr bgcolor=\"").append(bg).append("\">");
			html.append("<td width=420 align=left><font color=\"FFFFFF\">[Sub #").append(sub.getClassIndex()).append("] ").append(subName).append(" (Lv. ").append(sub.getLevel()).append(")</font> ");
			html.append(isActive ? "<font color=\"32C332\">(" + localize(lang, "Active", "Ativa") + ")</font>" : "").append("</td>");
			
			html.append("<td width=150 align=center>");
			if (!isActive)
			{
				html.append("<button value=\"").append(localize(lang, "Activate", "Ativar")).append("\" action=\"bypass -h _bbssubclassmaster;switch_subclass;").append(sub.getClassIndex()).append("\" width=100 height=22 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
			}
			html.append("</td>");
			
			html.append("<td width=150 align=center>");
			html.append("<button value=\"").append(localize(lang, "Delete", "Remover")).append("\" action=\"bypass -h _bbssubclassmaster;confirm_remove;").append(sub.getClassIndex()).append("\" width=100 height=22 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
			html.append("</td>");
			html.append("</tr>");
		}
		html.append("</table>");
		html.append("</td></tr>");
		
		// Footer (Botao Voltar)
		html.append("<tr><td height=36 align=center valign=bottom>");
		html.append("<button value=\"").append(localize(lang, "Back", "Voltar")).append("\" action=\"bypass _bbstop;merchant/blacksmithMain.html\" width=120 height=25 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
		html.append("</td></tr>");
		
		html.append("</table>");
		html.append("</td></tr></table>");
		html.append("</center></body></html>");
		return html.toString();
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
			player.sendMessage(localize(lang, "Leave the Olympiad queue before managing subclasses.", "Saia da fila das Olimpiadas antes de gerenciar subclasses."));
			return false;
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
		return (info != null) ? info.getClassName() : ("Class #" + classId);
	}
}