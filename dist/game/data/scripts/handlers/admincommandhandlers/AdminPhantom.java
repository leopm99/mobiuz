package handlers.admincommandhandlers;

import com.l2journey.Config;
import com.l2journey.PhantomConfig;
import com.l2journey.gameserver.ai.PhantomAI;
import com.l2journey.gameserver.handler.IAdminCommandHandler;
import com.l2journey.gameserver.model.Location;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.phantom.PhantomEngine;
import com.l2journey.gameserver.model.phantom.PhantomHuntingSpots;
import com.l2journey.gameserver.model.phantom.PhantomManager;
import com.l2journey.gameserver.model.phantom.PhantomMenu;
import com.l2journey.gameserver.model.phantom.PhantomState;
import com.l2journey.gameserver.model.phantom.PhantomWalk;

/**
 * Admin command handler for the Phantom bot-farming system.
 */
public class AdminPhantom implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_phantom_start_10",
		"admin_phantom_start_60",
		"admin_phantom_stop_10",
		"admin_phantom_stop_all",
		"admin_phantom_reload_xml",
		"admin_phantom_pload",
		"admin_phantom_restore_db",
		"admin_phantom_create_10",
		"admin_phantom_create_60",
		"admin_phantom_go",
		"admin_phantom_bring",
		"admin_phantom_kill",
		"admin_phantom_debug",
		"admin_phantom_menu"
	};
	
	@Override
	public boolean useAdminCommand(String command, Player player)
	{
		if (command.equals("admin_phantom_create_10"))
		{
			PhantomEngine.createAndStart(10, player);
			return true;
		}
		else if (command.equals("admin_phantom_create_60"))
		{
			PhantomEngine.createAndStart(60, player);
			return true;
		}
		else if (command.equals("admin_phantom_start_10"))
		{
			PhantomEngine.startBatch(10, player);
			return true;
		}
		else if (command.equals("admin_phantom_start_60"))
		{
			PhantomEngine.startBatch(60, player);
			return true;
		}
		else if (command.equals("admin_phantom_stop_10"))
		{
			PhantomEngine.stopSome(10, player);
			return true;
		}
		else if (command.equals("admin_phantom_stop_all"))
		{
			PhantomEngine.stopSystem(player);
			PhantomMenu.showMenu(player, 0);
			return true;
		}
		else if (command.equals("admin_phantom_reload_xml"))
		{
			if (!Config.PHANTOM_ENABLE)
			{
				player.sendMessage("Phantom system is disabled.");
				return true;
			}
			PhantomConfig.loadXML();
			player.sendMessage("Phantom XML reloaded.");
			PhantomMenu.showMenu(player, 0);
			return true;
		}
		else if (command.equals("admin_phantom_pload"))
		{
			if (!Config.PHANTOM_ENABLE)
			{
				player.sendMessage("Phantom system is disabled.");
				return true;
			}
			PhantomConfig.init();
			PhantomConfig.loadXML();
			PhantomHuntingSpots.load();
			
			PhantomAI.clearRuntimeState();
			PhantomState.clear();
			PhantomWalk.clearAll();
			
			player.sendMessage("Phantom scripts, XML, and hunting zones reloaded.");
			PhantomMenu.showMenu(player, 0);
			return true;
		}
		else if (command.equals("admin_phantom_restore_db"))
		{
			return restoreFromDatabase(player);
		}
		else if (command.startsWith("admin_phantom_go "))
		{
			Player phantom = PhantomEngine.getPhantomByName(command.substring(17).trim());
			if (phantom != null)
			{
				player.teleToLocation(phantom.getLocation());
			}
			return true;
		}
		else if (command.startsWith("admin_phantom_bring "))
		{
			Player phantom = PhantomEngine.getPhantomByName(command.substring(20).trim());
			if (phantom != null)
			{
				Location loc = new Location(player.getX(), player.getY(), player.getZ(), player.getHeading());
				PhantomEngine.movePhantomTo(phantom, loc, "Brought by GM");
				player.sendMessage("Brought " + phantom.getName() + ".");
			}
			else
			{
				player.sendMessage("Phantom not found.");
			}
			return true;
		}
		else if (command.startsWith("admin_phantom_kill "))
		{
			Player phantom = PhantomEngine.getPhantomByName(command.substring(19).trim());
			if ((phantom != null) && !phantom.isDead())
			{
				phantom.doDie(player);
			}
			return true;
		}
		else if (command.equals("admin_phantom_debug"))
		{
			player.sendMessage("Text logs: " + (PhantomManager.toggleDebug() ? "ON" : "OFF"));
			PhantomMenu.showMenu(player, 0);
			return true;
		}
		else if (command.startsWith("admin_phantom_menu"))
		{
			PhantomMenu.showMenu(player, parseMenuPage(command));
			return true;
		}
		
		return false;
	}
	
	private static boolean restoreFromDatabase(Player player)
	{
		if (!Config.PHANTOM_ENABLE)
		{
			player.sendMessage("Phantom system is disabled.");
			return true;
		}
		int added = PhantomConfig.restoreFromDatabase();
		player.sendMessage("Restored " + added + " phantoms from the database. List size: " + PhantomConfig.PHANTOM_IDS.size() + ". Use Start 10 / Start 60 to spawn them.");
		PhantomMenu.showMenu(player, 0);
		return true;
	}
	
	private static int parseMenuPage(String command)
	{
		try
		{
			String[] parts = command.split(" ");
			if (parts.length > 1)
			{
				return Integer.parseInt(parts[1]);
			}
		}
		catch (NumberFormatException _)
		{
			return 0;
		}
		return 0;
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
