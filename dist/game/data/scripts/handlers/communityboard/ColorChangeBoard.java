package handlers.communityboard;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.l2journey.Config;
import com.l2journey.commons.database.DatabaseFactory;
import com.l2journey.gameserver.data.xml.ItemData;
import com.l2journey.gameserver.handler.CommunityBoardHandler;
import com.l2journey.gameserver.handler.IParseBoardHandler;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.item.ItemTemplate;
import com.l2journey.gameserver.model.item.enums.ItemProcessType;
import com.l2journey.gameserver.model.variables.PlayerVariables;
import com.l2journey.gameserver.model.zone.ZoneId;

/**
 * Name/title colors using the same native HTML layout as the Subclass Master.
 * @author Debug
 */
public class ColorChangeBoard implements IParseBoardHandler
{
	private static final Logger LOGGER = Logger.getLogger(ColorChangeBoard.class.getName());
	private static final String ROOT = "_bbcolorchange";
	private record Settings(String[] colors, String[] names, int itemId, long amount) { }

	@Override
	public String[] getCommunityBoardCommands()
	{
		return new String[] { ROOT };
	}

	@Override
	public boolean parseCommunityBoardCommand(String command, Player player)
	{
		if ((player == null) || !Config.ENABLE_COMMUNITY_BOARD || !Config.COMMUNITYBOARD_ENABLED || !Config.COLOR_CHANGE_SERVICE_ENABLED)
		{
			if (player != null) { player.sendMessage("Color service is disabled."); }
			return false;
		}
		final String lang = player.getLang();
		if (player.isJailed() || player.isAlikeDead() || player.isInStoreMode() || player.isProcessingTransaction() || (player.getActiveTradeList() != null)
			|| player.isEnchanting() || player.isTeleporting() || player.getInventory().hasInventoryBlock()
			|| (Config.COMMUNITYBOARD_COMBAT_DISABLED && (player.isInCombat() || player.isCastingNow() || player.isCastingSimultaneouslyNow()
				|| player.isAttackingNow() || player.isInDuel() || player.isInOlympiadMode() || player.isOnEvent() || player.isInsideZone(ZoneId.SIEGE) || player.isInsideZone(ZoneId.PVP) || (player.getPvpFlag() > 0)))
			|| (Config.COMMUNITYBOARD_KARMA_DISABLED && player.getKarma() > 0)
			|| (Config.COMMUNITYBOARD_PEACE_ONLY && !player.isInsideZone(ZoneId.PEACE)))
		{
			player.sendMessage(localize(lang,"You cannot use the color service right now.","Voce nao pode usar o servico de cores agora."));
			return false;
		}
		final Settings settings;
		try { settings = settings(); }
		catch (IllegalArgumentException e)
		{
			LOGGER.warning("ColorChangeBoard: Invalid color service configuration: " + e.getMessage());
			player.sendMessage(localize(lang,"The color service configuration is invalid. Please contact a GM.","A configuracao do servico de cores e invalida. Contate um GM."));
			return false;
		}
		if (ROOT.equals(command))
		{
			CommunityBoardHandler.separateAndSend(getSelectionPage(player, settings),player);
			return true;
		}
		if ((command == null) || !command.startsWith(ROOT + ";")) { return false; }
		final String[] params = command.split(";",-1);
		if ((params.length != 3) || !(params[1].equals("nick") || params[1].equals("title"))) { return false; }
		String color = null;
		for (String candidate : settings.colors())
		{
			if (candidate.equalsIgnoreCase(params[2])) { color = candidate; break; }
		}
		if (color == null) { player.sendMessage(localize(lang,"Color not found.","Cor nao encontrada.")); return false; }
		if (!player.getAccessLevel().allowTransaction() || (player.getClient() == null) || !player.getClient().getFloodProtectors().canPerformTransaction()) { return false; }
		synchronized (player)
		{
			changeColor(player, settings, rgbToBgr(color), params[1].equals("nick"));
		}
		CommunityBoardHandler.separateAndSend(getSelectionPage(player,settings),player);
		return true;
	}

	private Settings settings()
	{
		final String[] colors = Config.COLOR_CHANGE_COLORS == null ? new String[0] : Config.COLOR_CHANGE_COLORS.clone();
		final String[] names = Config.COLOR_CHANGE_COLOR_NAMES == null ? new String[0] : Config.COLOR_CHANGE_COLOR_NAMES.clone();
		if ((colors.length == 0) || (colors.length > 20) || (colors.length != names.length)) { throw new IllegalArgumentException("Provide 1 to 20 matching color codes and names."); }
		for (int i=0;i<colors.length;i++)
		{
			if ((colors[i] == null) || !colors[i].trim().matches("[0-9A-Fa-f]{6}") || (names[i] == null) || names[i].isBlank()) { throw new IllegalArgumentException("Each color needs six hexadecimal digits and a non-empty name."); }
			colors[i] = colors[i].trim().toUpperCase(Locale.ROOT);
		}
		if ((Config.COLOR_CHANGE_ITEM_ID < 1) || (Config.COLOR_CHANGE_ITEM_AMOUNT < 0) || (ItemData.getInstance().getTemplate(Config.COLOR_CHANGE_ITEM_ID) == null)) { throw new IllegalArgumentException("Invalid payment item or quantity."); }
		return new Settings(colors,names,Config.COLOR_CHANGE_ITEM_ID,Config.COLOR_CHANGE_ITEM_AMOUNT);
	}

	private String getSelectionPage(Player player, Settings settings)
	{
		final String lang = player.getLang();
		final ItemTemplate payment = ItemData.getInstance().getTemplate(settings.itemId());
		final StringBuilder html = new StringBuilder(9000);
		html.append("<html noscrollbar><body><center>");
		html.append("<table width=720 height=100 cellpadding=0 cellspacing=0 border=0 background=\"L2UI_CH3.refinewnd_back_Pattern\"><tr><td fixwidth=720 fixheight=100 align=center valign=top>");
		html.append("<table width=720 height=95 cellpadding=0 cellspacing=0 border=0><tr><td width=30></td><td width=80 align=center valign=center><img src=\"icon.color_name_i00\" width=64 height=64></td>");
		html.append("<td width=440 align=left valign=center><font name=\"hs12\" color=\"F2C75C\">Color Manager</font><br1><font color=\"AE9977\">")
			.append(localize(lang,"Name and title colors","Cores do nome e do titulo")).append("</font><br1><font color=\"6B6B6B\">")
			.append(localize(lang,"Customize your character's appearance.","Personalize a aparencia do seu personagem.")).append("</font></td>");
		html.append("<td width=180 align=right valign=top><button value=\"").append(localize(lang,"Back to Services","Voltar aos Servicos"))
			.append("\" action=\"bypass _bbstop;merchant/servicesMain.html\" back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\" width=180 height=25>")
			.append("</td><td width=10>&nbsp;</td></tr></table></td></tr></table><br>");
		html.append("<table width=720 cellpadding=0 cellspacing=0 border=0 background=\"L2UI_CT1.Windows_DF_Drawer_Bg\"><tr><td align=center><br><font color=\"FFE497\" name=\"__SYSTEMWORLDFONT\">")
			.append(localize(lang,"C O L O R   S E L E C T I O N","S E L E C A O   D E   C O R E S")).append("</font><br><br>");
		html.append("<table width=640 cellpadding=2 cellspacing=0 border=0><tr><td width=40 align=center><font color=\"AE9977\">#</font></td><td width=280 align=left><font color=\"AE9977\">")
			.append(localize(lang,"Color preview","Previa da cor")).append("</font></td><td width=160 align=center><font color=\"AE9977\">").append(localize(lang,"Name","Nome"))
			.append("</font></td><td width=160 align=center><font color=\"AE9977\">").append(localize(lang,"Title","Titulo")).append("</font></td></tr>");
		for (int i=0;i<settings.colors().length;i++)
		{
			final String color = settings.colors()[i];
			final int bgr = rgbToBgr(color);
			html.append("<tr><td width=40 height=27 align=center>").append(i+1).append("</td><td width=280 align=left><font color=\"").append(color).append("\">")
				.append(escape(colorName(lang,settings.names()[i]))).append("</font></td><td width=160 align=center>");
			appendChoice(html,lang,color,true,player.getAppearance().getNameColor() == bgr);
			html.append("</td><td width=160 align=center>");
			appendChoice(html,lang,color,false,player.getAppearance().getTitleColor() == bgr);
			html.append("</td></tr>");
		}
		html.append("</table><br>");
		if (settings.amount() == 0)
		{
			html.append("<font color=\"CDB67F\">").append(localize(lang,"Color changes are free.","As alteracoes de cor sao gratuitas.")).append("</font><br>");
		}
		else
		{
			html.append("<table cellpadding=0 cellspacing=0 border=0><tr><td width=40><img src=\"").append(icon(payment.getIcon())).append("\" width=32 height=32></td><td><font color=\"CDB67F\">")
				.append(localize(lang,"Cost per change: ","Custo por alteracao: ")).append(String.format(Locale.US,"%,d",settings.amount()).replace(',',' ')).append(" ").append(escape(payment.getName())).append("</font></td></tr></table><br>");
		}
		html.append("</td></tr></table></center></body></html>");
		return html.toString();
	}

	private static void appendChoice(StringBuilder html, String lang, String color, boolean name, boolean selected)
	{
		if (selected) { html.append("<font color=\"6B6B6B\">").append(localize(lang,"Current","Atual")).append("</font>"); return; }
		html.append("<button value=\"").append(localize(lang,"Select","Selecionar")).append("\" action=\"bypass _bbcolorchange;")
			.append(name ? "nick" : "title").append(";").append(color).append("\" width=100 height=25 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
	}

	static int rgbToBgr(String color)
	{
		if ((color == null) || !color.matches("[0-9A-Fa-f]{6}")) { throw new IllegalArgumentException("Invalid RGB color."); }
		final int rgb = Integer.parseInt(color,16);
		return ((rgb & 255) << 16) | (rgb & 0xFF00) | ((rgb >> 16) & 255);
	}

	private void changeColor(Player player, Settings settings, int color, boolean name)
	{
		final String lang = player.getLang();
		if ((name ? player.getAppearance().getNameColor() : player.getAppearance().getTitleColor()) == color)
		{
			player.sendMessage(localize(lang,"This color is already selected.","Esta cor ja esta selecionada.")); return;
		}
		if ((settings.amount() > 0) && !player.destroyItemByItemId(ItemProcessType.BUY,settings.itemId(),settings.amount(),player,true))
		{
			player.sendMessage(localize(lang,"You do not have enough items to change the color.","Voce nao tem os itens necessarios para alterar a cor.")); return;
		}
		boolean committed = false;
		boolean commitStarted = false;
		boolean transactionStarted = false;
		boolean rolledBack = false;
		final String variable = name ? PlayerVariables.COMMUNITY_NAME_COLOR : PlayerVariables.COMMUNITY_TITLE_COLOR;
		try (Connection con = DatabaseFactory.getConnection())
		{
			con.setAutoCommit(false); transactionStarted = true;
			try
			{
				try (PreparedStatement ps = con.prepareStatement(name ? "UPDATE characters SET name_color=? WHERE charId=?" : "UPDATE characters SET title_color=? WHERE charId=?"))
				{
					ps.setInt(1,color); ps.setInt(2,player.getObjectId());
					if (ps.executeUpdate() != 1) { throw new SQLException("Character color was not updated"); }
				}
				// Keep login overrides consistent with the character columns; the table has no unique key.
				try (PreparedStatement ps = con.prepareStatement("DELETE FROM character_variables WHERE charId=? AND var=?"))
				{
					ps.setInt(1,player.getObjectId()); ps.setString(2,variable); ps.executeUpdate();
				}
				try (PreparedStatement ps = con.prepareStatement("INSERT INTO character_variables (charId,var,val) VALUES (?,?,?)"))
				{
					ps.setInt(1,player.getObjectId()); ps.setString(2,variable); ps.setString(3,Integer.toString(color));
					if (ps.executeUpdate() != 1) { throw new SQLException("Character color override was not saved"); }
				}
				commitStarted = true; con.commit(); committed = true;
			}
			catch (SQLException e)
			{
				try { con.rollback(); rolledBack = true; } catch (SQLException rollbackError) { e.addSuppressed(rollbackError); }
				throw e;
			}
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING,"ColorChangeBoard: Could not save color for " + player.getName(),e);
			if (!committed)
			{
				if (!commitStarted && (!transactionStarted || rolledBack) && settings.amount() > 0) { player.addItem(ItemProcessType.REFUND,settings.itemId(),settings.amount(),player,true); }
				player.sendMessage(localize(lang,"The color change could not be confirmed. Please contact a GM.","Nao foi possivel confirmar a alteracao. Contate um GM.")); return;
			}
		}
		player.getVariables().set(variable,color);
		if (name) { player.getAppearance().setNameColor(color); }
		else { player.getAppearance().setTitleColor(color); }
		player.broadcastUserInfo();
		player.sendMessage(localize(lang,"Your color has been changed successfully.","Sua cor foi alterada com sucesso."));
	}

	private static String localize(String lang,String en,String br) { return "br".equals(lang) ? br : en; }
	private static String escape(String value) { return value == null ? "" : value.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;"); }
	private static String icon(String value) { return value != null && value.matches("[A-Za-z0-9_.]+") ? value : "icon.etc_adena_i00"; }
	private static String colorName(String lang,String name)
	{
		if (!"br".equals(lang)) { return name; }
		return switch(name.toLowerCase(Locale.ROOT))
		{
			case "white" -> "Branco"; case "red" -> "Vermelho"; case "green" -> "Verde"; case "blue" -> "Azul";
			case "gold" -> "Dourado"; case "pink" -> "Rosa"; case "cyan" -> "Ciano"; case "orange" -> "Laranja";
			case "purple" -> "Roxo"; case "silver" -> "Prata"; default -> name;
		};
	}
}
