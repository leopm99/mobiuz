package handlers.communityboard;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
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
import com.l2journey.gameserver.network.SystemMessageId;

/**
 * Community Board name and title colors.
 * @author Debug
 */
public class ColorChangeBoard implements IParseBoardHandler
{
	private static final Logger LOGGER = Logger.getLogger(ColorChangeBoard.class.getName());
	private static final String[] COMMANDS = { "_bbcolorchange" };

	@Override
	public String[] getCommunityBoardCommands()
	{
		return COMMANDS;
	}

	private static String text(Player player, String en, String br)
	{
		return "br".equalsIgnoreCase(player.getLang()) ? br : en;
	}

	@Override
	public boolean parseCommunityBoardCommand(String command, Player player)
	{
		if (!Config.ENABLE_COMMUNITY_BOARD || !Config.COMMUNITYBOARD_ENABLED)
		{
			player.sendPacket(SystemMessageId.THE_COMMUNITY_SERVER_IS_CURRENTLY_OFFLINE);
			return false;
		}
		if (!Config.COLOR_CHANGE_SERVICE_ENABLED)
		{
			player.sendMessage(text(player, "Color service is disabled.", "O serviço de cores está desativado."));
			return false;
		}
		final String[] params = command.split(";", -1);
		if (!params[0].equals(COMMANDS[0]) || ((params.length != 1) && ((params.length != 3) || !(params[1].equals("nick") || params[1].equals("title")))))
		{
			return false;
		}
		if ((Config.COMMUNITYBOARD_KARMA_DISABLED && (player.getKarma() > 0)) || (Config.COMMUNITYBOARD_PEACE_ONLY && !player.isInsideZone(ZoneId.PEACE)))
		{
			player.sendMessage(text(player, "The Community Board cannot be used here or with Karma.", "O Community Board não pode ser usado aqui ou com Karma."));
			return false;
		}
		// Capture the current palette instead of caching configuration at class initialization.
		final String[] colors = Config.COLOR_CHANGE_COLORS;
		final String[] names = Config.COLOR_CHANGE_COLOR_NAMES;
		final int itemId = Config.COLOR_CHANGE_ITEM_ID;
		final long amount = Config.COLOR_CHANGE_ITEM_AMOUNT;
		if (!validPalette(colors, names) || (amount < 0) || ((amount > 0) && ((itemId <= 0) || (ItemData.getInstance().getTemplate(itemId) == null))))
		{
			player.sendMessage(text(player, "Color service is not configured correctly.", "O serviço de cores não está configurado corretamente."));
			return false;
		}
		if (params.length == 3)
		{
			final String requested = params[2];
			final String color = Arrays.stream(colors).map(String::trim).filter(c -> c.equalsIgnoreCase(requested)).findFirst().orElse(null);
			if (color == null)
			{
				player.sendMessage(text(player, "Color not found.", "Cor não encontrada."));
				return false;
			}
			synchronized (player)
			{
				if (player.isAlikeDead() || player.isInStoreMode() || player.isProcessingTransaction() || player.isEnchanting() || player.isChangingClass()
					|| (Config.COMMUNITYBOARD_COMBAT_DISABLED && (player.isInCombat() || player.isInDuel() || player.isInOlympiadMode() || player.isOnEvent() || (player.getPvpFlag() > 0) || player.isInsideZone(ZoneId.SIEGE) || player.isInsideZone(ZoneId.PVP))))
				{
					player.sendMessage(text(player, "You cannot change colors right now.", "Você não pode alterar cores neste momento."));
					return false;
				}
				if (!player.getClient().getFloodProtectors().canPerformTransaction())
				{
					return false;
				}
				changeColor(player, rgbToBgr(color), params[1].equals("nick"), itemId, amount);
			}
		}
		CommunityBoardHandler.getInstance().addBypass(player, text(player, "Name & Title Color", "Cor do Nome e Título"), COMMANDS[0]);
		CommunityBoardHandler.separateAndSend(getSelectionPage(player, colors, names, itemId, amount), player);
		return true;
	}

	private static boolean validPalette(String[] colors, String[] names)
	{
		return (colors != null) && (names != null) && (colors.length > 0) && (colors.length <= 20) && (colors.length == names.length)
			&& Arrays.stream(colors).allMatch(c -> (c != null) && c.trim().matches("[0-9a-fA-F]{6}"))
			&& Arrays.stream(names).allMatch(n -> (n != null) && !n.isBlank() && (n.length() <= 40));
	}

	private static String escape(String value)
	{
		return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}

	private static String colorName(Player player, String name)
	{
		return switch (name.trim())
		{
			case "White" -> text(player, "White", "Branco");
			case "Red" -> text(player, "Red", "Vermelho");
			case "Green" -> text(player, "Green", "Verde");
			case "Blue" -> text(player, "Blue", "Azul");
			case "Gold" -> text(player, "Gold", "Dourado");
			case "Pink" -> text(player, "Pink", "Rosa");
			case "Cyan" -> text(player, "Cyan", "Ciano");
			default -> name.trim();
		};
	}

	private String getSelectionPage(Player player, String[] colors, String[] names, int itemId, long amount)
	{
		final StringBuilder html = new StringBuilder(4096);
		html.append("<html><body><center><br><font name=hs12 color=F2C75C>").append(text(player, "Name & Title Color", "Cor do Nome e Título")).append("</font><br>");
		html.append("<table width=680 cellpadding=4 cellspacing=2 border=0><tr><td width=280 align=center>").append(text(player, "Color", "Cor"));
		html.append("</td><td width=200 align=center>").append(text(player, "Name", "Nome")).append("</td><td width=200 align=center>").append(text(player, "Title", "Título")).append("</td></tr>");
		for (int i = 0; i < colors.length; i++)
		{
			final String color = colors[i].trim();
			html.append("<tr bgcolor=161616><td align=center><font color=\"").append(color).append("\">").append(escape(colorName(player, names[i]))).append("</font></td>");
			for (String type : new String[] { "nick", "title" })
			{
				html.append("<td align=center><button value=\"").append(text(player, "Select", "Selecionar")).append("\" action=\"bypass _bbcolorchange;").append(type).append(';').append(color);
				html.append("\" width=120 height=22 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
			}
			html.append("</tr>");
		}
		html.append("</table><br><font color=CDB67F>");
		if (amount == 0)
		{
			html.append(text(player, "Free color changes.", "Alterações de cor gratuitas."));
		}
		else
		{
			final ItemTemplate item = ItemData.getInstance().getTemplate(itemId);
			html.append(text(player, "Cost per change: ", "Custo por alteração: ")).append(amount).append(' ').append(escape(item.getName()));
		}
		html.append("</font><br><button value=\"").append(text(player, "Back", "Voltar")).append("\" action=\"bypass _bbstop;merchant/blacksmithMain.html\" width=160 height=24 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></center></body></html>");
		return html.toString();
	}

	static int rgbToBgr(String color)
	{
		final int rgb = Integer.parseInt(color, 16);
		return ((rgb & 0xFF) << 16) | (rgb & 0x00FF00) | ((rgb >>> 16) & 0xFF);
	}

	private void changeColor(Player player, int color, boolean name, int itemId, long amount)
	{
		final int current = name ? player.getAppearance().getNameColor() : player.getAppearance().getTitleColor();
		if (current == color)
		{
			player.sendMessage(text(player, "This color is already selected.", "Esta cor já está selecionada."));
			return;
		}
		if ((amount > 0) && !player.destroyItemByItemId(ItemProcessType.BUY, itemId, amount, player, true))
		{
			player.sendMessage(text(player, "Not enough items to change the color.", "Você não tem os itens necessários para alterar a cor."));
			return;
		}
		// Keep the in-memory appearance unchanged until persistence succeeds.
		try (Connection con = DatabaseFactory.getConnection())
		{
			con.setAutoCommit(false);
			try
			{
				final String variable = name ? PlayerVariables.COMMUNITY_NAME_COLOR : PlayerVariables.COMMUNITY_TITLE_COLOR;
				try (PreparedStatement delete = con.prepareStatement("DELETE FROM character_variables WHERE charId=? AND var=?");
					PreparedStatement insert = con.prepareStatement("INSERT INTO character_variables (charId,var,val) VALUES (?,?,?)"))
				{
					delete.setInt(1, player.getObjectId());
					delete.setString(2, variable);
					delete.executeUpdate();
					insert.setInt(1, player.getObjectId());
					insert.setString(2, variable);
					insert.setString(3, Integer.toString(color));
					if (insert.executeUpdate() != 1) { throw new SQLException("Color variable not stored"); }
				}
				if (!name)
				{
					try (PreparedStatement statement = con.prepareStatement("UPDATE characters SET title_color=? WHERE charId=?"))
					{
						statement.setInt(1, color);
						statement.setInt(2, player.getObjectId());
						if (statement.executeUpdate() != 1) { throw new SQLException("Character not found"); }
					}
				}
				con.commit();
			}
			catch (SQLException e)
			{
				con.rollback();
				throw e;
			}
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, "Could not persist color for player " + player.getObjectId(), e);
			if (amount > 0) { player.addItem(ItemProcessType.REFUND, itemId, amount, player, true); }
			player.sendMessage((amount > 0)
				? text(player, "Could not save the color. Your payment was refunded.", "Não foi possível salvar a cor. Seu pagamento foi devolvido.")
				: text(player, "Could not save the color. Please try again.", "Não foi possível salvar a cor. Tente novamente."));
			return;
		}
		if (name)
		{
			player.getVariables().set(PlayerVariables.COMMUNITY_NAME_COLOR, color);
			player.getAppearance().setNameColor(color);
		}
		else
		{
			player.getVariables().set(PlayerVariables.COMMUNITY_TITLE_COLOR, color);
			player.getAppearance().setTitleColor(color);
		}
		player.broadcastUserInfo();
		player.sendMessage(text(player, "Color changed successfully.", "Cor alterada com sucesso."));
	}
}
