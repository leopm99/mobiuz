package handlers.voicedcommandhandlers;

import com.l2journey.Config;
import com.l2journey.gameserver.handler.IVoicedCommandHandler;
import com.l2journey.gameserver.managers.CastleManager;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.siege.Castle;
import com.l2journey.gameserver.network.serverpackets.NpcHtmlMessage;
import com.l2journey.gameserver.network.serverpackets.SiegeInfo;

public class SiegeManagerCmd implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS =
	{
		"siege",
		"siege_of_gludio",
		"siege_of_dion",
		"siege_of_giran",
		"siege_of_oren",
		"siege_of_aden",
		"siege_of_innadril",
		"siege_of_goddard",
		"siege_of_rune",
		"siege_of_schuttgart",
		"siege_of_"
	};
	
	@Override
	public boolean useVoicedCommand(String command, Player activeChar, String target)
	{
		if ((activeChar == null) || !Config.ENABLE_SIEGE_VCMD) { return false; }
		if ("siege".equals(command))
		{
			if (Config.ENABLE_GNU_PANEL) { sendHtml(activeChar); }
			else { sendErrorHtml(activeChar); }
			return true;
		}
		int castleId = switch (command)
		{
			case "siege_of_gludio" -> 1;
			case "siege_of_dion" -> 2;
			case "siege_of_giran" -> 3;
			case "siege_of_oren" -> 4;
			case "siege_of_aden" -> 5;
			case "siege_of_innadril" -> 6;
			case "siege_of_goddard" -> 7;
			case "siege_of_rune" -> 8;
			case "siege_of_schuttgart" -> 9;
			default -> 0;
		};
		if (castleId == 0) { return false; }
		if ((activeChar.getClan() != null) && !activeChar.isClanLeader())
		{
			activeChar.sendMessage("Only Clan Leader can use this command.");
			return false;
		}
		final Castle castle = CastleManager.getInstance().getCastleById(castleId);
		if (castle == null) { return false; }
		activeChar.sendPacket(new SiegeInfo(castle, activeChar));
		return true;
	}

	private void sendHtml(Player activeChar)
	{
		String htmFile = "data/html/mods/Castle_Info.htm";
		
		NpcHtmlMessage msg = new NpcHtmlMessage(5);
		msg.setFile(activeChar, htmFile);
		activeChar.sendPacket(msg);
	}
	
	private void sendErrorHtml(Player activeChar)
	{
		String htmFile = "data/html/mods/Castle_Error_Info.htm";
		
		NpcHtmlMessage msg = new NpcHtmlMessage(5);
		msg.setFile(activeChar, htmFile);
		activeChar.sendPacket(msg);
	}
	
	@Override
	public String[] getVoicedCommandList()
	{
		return VOICED_COMMANDS;
	}
}