package handlers.voicedcommandhandlers;

import com.l2journey.Config;
import com.l2journey.gameserver.handler.IVoicedCommandHandler;
import com.l2journey.gameserver.model.Location;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.olympiad.OlympiadManager;
import com.l2journey.gameserver.model.zone.ZoneId;

public class TeleportsVCmd implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS =
	{
		"giran",
		"dion",
		"aden",
		"goddard",
		"gludio",
		"gludin",
		"rune",
		"heine",
		"schuttgart",
		"oren",
		"hunters",
		"zaken",
		"tezza",
		"freya",
		"baium",
		"antharas",
		"valakas",
		"queenant",
		"core",
		"orfen",
		"beleth",
		"ivory",
		"kamael",
		"orcs",
		"talking",
		"elven",
		"darkelf",
		"dwarv",
		"floran"
	
	};
	
	@Override
	public boolean useVoicedCommand(String command, Player activeChar, String target)
	{
		if ((activeChar == null) || !java.util.Arrays.asList(VOICED_COMMANDS).contains(command)) { return false; }
		if (!Config.TELEPORTS_VOICED_COMMAND)
		{
			activeChar.sendMessage("The teleports voiced commands are currently disabled.");
			return false;
		}
		
		if (activeChar.getKarma() > 0)
		{
			activeChar.sendMessage("Cannot use while have karma.");
			return false;
		}
		
		if (activeChar.getPvpFlag() > 0)
		{
			activeChar.sendMessage("Cannot use while have pvp flag.");
			return false;
		}
		
		if (activeChar.isInOlympiadMode() || activeChar.inObserverMode() || OlympiadManager.getInstance().isRegistered(activeChar))
		{
			activeChar.sendMessage("Cannot use while in Olympiad.");
			return false;
		}
		
		if (activeChar.isRegisteredOnEvent() || activeChar.isOnEvent() || activeChar.isOnSoloEvent())
		{
			activeChar.sendMessage("Cannot use while in event.");
			return false;
		}
		
		if (activeChar.isJailed())
		{
			activeChar.sendMessage("Cannot use while in jail.");
			return false;
		}
		
		if (activeChar.isAlikeDead())
		{
			activeChar.sendMessage("Cannot use while in fake death mode.");
			return false;
		}
		
		if (!activeChar.isInsideZone(ZoneId.PEACE) && activeChar.isInCombat())
		{
			activeChar.sendMessage("Cannot use while in combat outside of peace zone.");
			return false;
		}
		
		Location loc = null;
		switch (command)
		{
			case "giran":
				loc = new Location(83473, 148554, -3400);
				activeChar.sendMessage("You have been teleported to the Town of Giran.");
				break;
			case "dion":
				loc = new Location(15619, 143132, -2705);
				activeChar.sendMessage("You have been teleported to the Town of Dion.");
				break;
			case "aden":
				loc = new Location(147974, 26883, -2200);
				activeChar.sendMessage("You have been teleported to the Town of Aden.");
				break;
			case "gludio":
				loc = new Location(-14413, 123044, -3112);
				activeChar.sendMessage("You have been teleported to the Town of Gludio.");
				break;
			case "gludin":
				loc = new Location(-80852, 149906, -3044);
				activeChar.sendMessage("You have been teleported to the Village of Gludin.");
				break;
			case "rune":
				loc = new Location(43759, -48122, -792);
				activeChar.sendMessage("You have been teleported to Rune Township.");
				break;
			case "heine":
				loc = new Location(111381, 218981, -3538);
				activeChar.sendMessage("You have been teleported to the Town of Heine.");
				break;
			case "goddard":
				loc = new Location(147732, -56554, -2776);
				activeChar.sendMessage("You have been teleported to the Town of Goddard.");
				break;
			case "schuttgart":
				loc = new Location(87355, -142095, -1336);
				activeChar.sendMessage("You have been teleported to the Town of Schuttgart.");
				break;
			case "oren":
				loc = new Location(82760, 53578, -1491);
				activeChar.sendMessage("You have been teleported to the Town of Oren.");
				break;
			case "hunters":
				loc = new Location(116863, 76725, -2720);
				activeChar.sendMessage("You have been teleported to the Hunter's Village.");
				break;
			case "queenant":
				loc = new Location(-21595, 185358, -5608);
				activeChar.sendMessage("You have been teleported to the lair of Queen Ant.");
				break;
			case "core":
				loc = new Location(17649, 110895, -6648);
				activeChar.sendMessage("You have been teleported to the lair of Core.");
				break;
			case "orfen":
				loc = new Location(54290, 19307, -5256);
				activeChar.sendMessage("You have been teleported to the lair of Orfen.");
				break;
			case "baium":
				loc = new Location(113862, 15327, 9560);
				activeChar.sendMessage("You have been teleported to the Tower of Insolence.");
				break;
			case "zaken":
				loc = new Location(52214, 218899, -3224);
				activeChar.sendMessage("You have been teleported to Zaken's entrance NPC.");
				break;
			case "freya":
				loc = new Location(102405, -124428, -2768);
				activeChar.sendMessage("You have been teleported to Freya's entrance NPC.");
				break;
			case "tezza":
				loc = new Location(181377, -80712, -2728);
				activeChar.sendMessage("You have been teleported to Frintezza's entrance NPC.");
				break;
			case "antharas":
				loc = new Location(154308, 121317, -3808);
				activeChar.sendMessage("You have been teleported to Anthara's entrance NPC.");
				break;
			case "valakas":
				loc = new Location(183597, -114791, -3336);
				activeChar.sendMessage("You have been teleported to Klein.");
				break;
			case "beleth":
				loc = new Location(-45481, 246352, -14184);
				activeChar.sendMessage("You have been teleported to the Beleth's entrance room.");
				break;
			case "ivory":
				loc = new Location(85332, 16186, -3673);
				activeChar.sendMessage("You have been teleported to the Ivory Tower.");
				break;
			case "kamael":
				loc = new Location(-117033, 46496, 341);
				activeChar.sendMessage("You have been teleported to Kamael Village.");
				break;
			case "orcs":
				loc = new Location(-44429, -113596, -220);
				activeChar.sendMessage("You have been teleported to Orcs Village.");
				break;
			case "talking":
				loc = new Location(-83878, 242955, -3755);
				activeChar.sendMessage("You have been teleported to Talking Island.");
				break;
			case "elven":
				loc = new Location(47050, 50767, -2996);
				activeChar.sendMessage("You have been teleported to Elven Village.");
				break;
			case "darkelf":
				loc = new Location(11620, 16780, -4662);
				activeChar.sendMessage("You have been teleported to  Dark Elf Village.");
				break;
			case "dwarv":
				loc = new Location(115526, -178660, -945);
				activeChar.sendMessage("You have been teleported to  Dwarven Village.");
				break;
			case "floran":
				loc = new Location(17144, 170156, -3502);
				activeChar.sendMessage("You have been teleported to Floran Village.");
				break;
		}
		
		if (loc == null) { return false; }
		activeChar.teleToLocation(loc, false);
		return true;
	}
	
	@Override
	public String[] getVoicedCommandList()
	{
		return VOICED_COMMANDS;
	}
}