package handlers.communityboard;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.l2journey.Config;
import com.l2journey.commons.threads.ThreadPool;
import com.l2journey.gameserver.handler.CommunityBoardHandler;
import com.l2journey.gameserver.handler.IParseBoardHandler;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.appearance.PlayerVisualProfile;
import com.l2journey.gameserver.model.actor.enums.creature.Race;
import com.l2journey.gameserver.model.actor.enums.player.PlayerClass;
import com.l2journey.gameserver.model.variables.PlayerVariables;
import com.l2journey.gameserver.network.serverpackets.MagicSkillUse;

public class AppearanceBoard implements IParseBoardHandler
{
	private static final String COMMAND = "_bbsappearance";
	private static final int TRANSFORM_EFFECT = 617;
	private static final int RESTORE_EFFECT = 619;
	private static final int EFFECT_DURATION = 2500;
	private static final Set<Player> PENDING_CHANGES = ConcurrentHashMap.newKeySet();

	@Override
	public String[] getCommunityBoardCommands()
	{
		return new String[] { COMMAND };
	}

	@Override
	public boolean parseCommunityBoardCommand(String command, Player player)
	{
		if (player == null || !Config.COMMUNITYBOARD_ENABLED || !PlayerVisualProfile.supports(player))
		{
			if (player != null && Config.COMMUNITYBOARD_ENABLED)
			{
				player.sendMessage("Custom appearances are unavailable for Kamael characters.");
			}
			return false;
		}
		if (COMMAND.equals(command))
		{
			show(player);
			return true;
		}
		final String[] parts = command.split(";", -1);
		if (parts.length != 2 || !COMMAND.equals(parts[0]))
		{
			return false;
		}
		final boolean restore = "restore".equals(parts[1]);
		final PlayerVisualProfile profile = PlayerVisualProfile.fromKey(parts[1]);
		if (!restore && profile == null)
		{
			return false;
		}

		synchronized (player)
		{
			if (!restore && (player.isTransformed() || player.isMounted()))
			{
				player.sendMessage("End your transformation or dismount to change appearance.");
				return true;
			}
			if (player.getClient() == null || !player.getClient().getFloodProtectors().canPerformTransaction())
			{
				return false;
			}
			final PlayerVisualProfile selected = PlayerVisualProfile.getSelected(player);
			if (selected == profile || !PENDING_CHANGES.add(player))
			{
				return true;
			}
			final PlayerClass baseClass = PlayerClass.getPlayerClass(player.getBaseClass());
			final PlayerClass currentModel = selected == null ? baseClass : selected.getModelClass();
			final PlayerClass nextModel = profile == null ? baseClass : profile.getModelClass();
			// Let the client swap human skeletons without a cast animation in progress.
			if (currentModel.getRace() == Race.HUMAN && nextModel.getRace() == Race.HUMAN && currentModel.isMage() != nextModel.isMage())
			{
				applyAppearance(player, profile);
				return true;
			}
			if (ThreadPool.schedule(() -> applyAppearance(player, profile), EFFECT_DURATION) == null)
			{
				PENDING_CHANGES.remove(player);
				player.sendMessage("The appearance change could not start. Try again.");
				return true;
			}
			player.broadcastPacket(new MagicSkillUse(player, player, restore ? RESTORE_EFFECT : TRANSFORM_EFFECT, 1, EFFECT_DURATION, 0));
		}
		return true;
	}

	private static void applyAppearance(Player player, PlayerVisualProfile profile)
	{
		synchronized (player)
		{
			PENDING_CHANGES.remove(player);
			if (!player.isOnline() || player.getClient() == null || !Config.COMMUNITYBOARD_ENABLED || !PlayerVisualProfile.supports(player))
			{
				return;
			}
			if (profile != null && (player.isTransformed() || player.isMounted()))
			{
				player.sendMessage("End your transformation or dismount to change appearance.");
				return;
			}
			final var variables = player.getVariables();
			if (profile == null)
			{
				variables.remove(PlayerVariables.COMMUNITY_VISUAL_PROFILE);
			}
			else
			{
				variables.set(PlayerVariables.COMMUNITY_VISUAL_PROFILE, profile.name());
			}
			player.broadcastUserInfo();
			show(player);
		}
	}

	private static void show(Player player)
	{
		final PlayerVisualProfile selected = PlayerVisualProfile.getSelected(player);
		final StringBuilder html = new StringBuilder(2500);
		html.append("<html noscrollbar><body><center><br>");
		html.append("<table width=720 height=100 cellpadding=0 cellspacing=0 border=0 background=\"L2UI_CH3.refinewnd_back_Pattern\">");
		html.append("<tr><td width=30></td><td width=80 align=center valign=center>");
		html.append("<table width=64 cellpadding=0 cellspacing=0 border=0>");
		html.append("<tr><td><img src=\"L2UI.SquareBlank\" width=1 height=18></td><td></td></tr><tr><td>");
		icon(html, Race.HUMAN, 32);
		html.append("</td><td>");
		icon(html, Race.ELF, 32);
		html.append("</td></tr><tr><td>");
		icon(html, Race.ORC, 32);
		html.append("</td><td>");
		icon(html, Race.DWARF, 32);
		html.append("</td></tr><tr><td><img src=\"L2UI.SquareBlank\" width=1 height=18></td><td></td></tr></table></td>");
		html.append("<td width=420 align=left valign=center><font name=\"hs12\" color=\"F2C75C\">Custom Appearance</font><br1><font color=\"AE9977\">Selected: ");
		html.append(selected == null ? "Original appearance" : selected.getLabel());
		html.append("</font><br1><font color=\"6B6B6B\">Choose a visual profile. Changes are free.</font></td>");
		html.append("<td width=180 align=right valign=top>");
		html.append("<table width=165 cellpadding=0 cellspacing=0 border=0><tr><td><img src=\"L2UI.SquareBlank\" width=1 height=8></td></tr><tr><td align=right>");
		button(html, "Back to Services", "_bbstop;merchant/servicesMain.html", 165);
		html.append("</td></tr></table></td><td width=10></td></tr></table><br>");
		html.append("<table width=720 cellpadding=0 cellspacing=0 border=0 background=\"L2UI_CT1.Windows_DF_Drawer_Bg\"><tr><td align=center><br>");
		html.append("<font color=\"FFE497\" name=\"__SYSTEMWORLDFONT\">A P P E A R A N C E</font><br><br>");
		html.append("<table width=450 cellpadding=2 cellspacing=0 border=0>");
		for (PlayerVisualProfile profile : PlayerVisualProfile.values())
		{
			appearanceRow(html, profile.getLabel(), COMMAND + ";" + profile.name(), profile.getModelClass().getRace(), profile == selected);
		}
		html.append("<tr><td height=6></td><td></td><td></td><td></td><td></td></tr>");
		appearanceRow(html, "Restore Original", COMMAND + ";restore", player.getRace(), false);
		html.append("</table><img src=\"L2UI.SquareBlank\" width=1 height=29></td></tr></table></center></body></html>");
		CommunityBoardHandler.separateAndSend(html.toString(), player);
	}

	private static void appearanceRow(StringBuilder html, String label, String action, Race race, boolean selected)
	{
		html.append("<tr><td width=80></td><td width=40 align=center valign=center>");
		icon(html, race, 28);
		html.append("</td><td width=210 align=center valign=center>");
		button(html, label, action, 210);
		html.append("</td><td width=40 align=center valign=center>");
		icon(html, race, 28);
		html.append("</td><td width=80 valign=center>");
		if (selected)
		{
			html.append("<font color=\"CDB67F\">Selected</font>");
		}
		html.append("</td></tr>");
	}

	private static void icon(StringBuilder html, Race race, int size)
	{
		final String texture = switch (race)
		{
			case HUMAN -> "icon.skillhuman";
			case ELF -> "icon.skillelf";
			case DARK_ELF -> "icon.skilldarkelf";
			case ORC -> "icon.skillorc";
			case DWARF -> "icon.skilldwarf";
			default -> "icon.etc_party_mask_i00";
		};
		html.append("<img src=\"").append(texture).append("\" width=").append(size).append(" height=").append(size).append(">");
	}

	private static void button(StringBuilder html, String label, String action, int width)
	{
		html.append("<button value=\"").append(label).append("\" action=\"bypass ").append(action)
			.append("\" width=").append(width).append(" height=29 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
	}
}
