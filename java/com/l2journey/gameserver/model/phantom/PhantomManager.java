package com.l2journey.gameserver.model.phantom;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;

import com.l2journey.PhantomConfig;
import com.l2journey.gameserver.handler.IVoicedCommandHandler;
import com.l2journey.gameserver.handler.VoicedCommandHandler;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.network.enums.ChatType;
import com.l2journey.gameserver.network.serverpackets.CreatureSay;

public class PhantomManager implements IVoicedCommandHandler
{
	// Inicializamos o Logger oficial do console
	private static final Logger LOGGER = Logger.getLogger(PhantomManager.class.getName());
	
	private static final String[] VOICED_COMMANDS =
	{
		"pstart",
		"pstop",
		"pload",
		"pcreate",
		"pstop10",
		"pm",
		"pmenu",
		"pdebug"
	};
	
	private static boolean _debugMode = true;
	private static String _sessionLogFile = "log/PhantomManager-session-pending.txt";
	
	public PhantomManager()
	{
		startLogSession("SERVER_START");
		PhantomConfig.init();
		VoicedCommandHandler.getInstance().registerHandler(this);
		
		LOGGER.info("[Phantom System L2Journey]: By Mafias");
		
		// ARRANQUE AUTOMATICO DINAMICO:
		com.l2journey.commons.threads.ThreadPool.schedule(() ->
		{
			// Lemos exatamente quantos bots voce tinha criados/salvos no seu XML
			int botsGuardados = PhantomConfig.PHANTOM_IDS.size();
			
			if (botsGuardados > 0)
			{
				LOGGER.info("[Phantom System L2Journey]: Iniciando " + botsGuardados + " bots automaticamente...");
				PhantomEngine.startBatch(botsGuardados, null);
			}
		}, 60000);
	}
	
	public static void startLogSession(String reason)
	{
		_sessionLogFile = "log/PhantomManager-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".txt";
		logToFile("SYSTEM", "Nueva sesion de logs: " + reason);
	}
	
	public static void logToFile(String botName, String action)
	{
		if (!_debugMode)
		{
			return;
		}
		
		try (FileWriter fw = new FileWriter("log/PhantomManager.txt", true);
			PrintWriter pw = new PrintWriter(fw))
		{
			pw.println("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "] " + botName + ": " + action);
		}
		catch (IOException e)
		{
		}
		
		try (FileWriter fw = new FileWriter(_sessionLogFile, true);
			PrintWriter pw = new PrintWriter(fw))
		{
			pw.println("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "] " + botName + ": " + action);
		}
		catch (IOException e)
		{
		}
	}
	
	public static void logException(String botName, String action, Exception e)
	{
		StringWriter sw = new StringWriter();
		e.printStackTrace(new PrintWriter(sw));
		logToFile(botName, action + ": " + e.getMessage() + System.lineSeparator() + sw);
	}
	
	public static boolean toggleDebug()
	{
		_debugMode = !_debugMode;
		return _debugMode;
	}
	
	public static boolean isDebugMode()
	{
		return _debugMode;
	}
	
	@Override
	public String[] getVoicedCommandList()
	{
		return VOICED_COMMANDS;
	}
	
	@Override
	public boolean useVoicedCommand(String command, Player player, String params)
	{
		// Apenas admins podem controlar o sistema de Phantoms.
		// O comando "pm" (conversar com um Phantom) continua liberado para jogadores comuns.
		if (!command.equalsIgnoreCase("pm") && !player.isGM())
		{
			return false;
		}
		
		if (command.equalsIgnoreCase("pstart"))
		{
			PhantomEngine.startSystem(player);
			return true;
		}
		else if (command.equalsIgnoreCase("pstop"))
		{
			PhantomEngine.stopSystem(player);
			return true;
		}
		else if (command.equalsIgnoreCase("pstop10"))
		{
			PhantomEngine.stopSome(10, player);
			return true;
		}
		else if (command.equalsIgnoreCase("pload"))
		{
			PhantomConfig.loadXML();
			player.sendMessage("Phantom XML reloaded.");
			return true;
		}
		else if (command.equalsIgnoreCase("pcreate"))
		{
			int count = 10;
			
			if ((params != null) && !params.trim().isEmpty())
			{
				try
				{
					count = Math.max(1, Math.min(100, Integer.parseInt(params.trim())));
				}
				catch (NumberFormatException e)
				{
					player.sendMessage("Usage: .pcreate 10");
					return true;
				}
			}
			
			PhantomEngine.createAndStart(count, player);
			return true;
		}
		else if (command.equalsIgnoreCase("pm"))
		{
			if ((params == null) || params.trim().isEmpty())
			{
				return true;
			}
			
			String[] parts = params.split(" ", 2);
			
			if (parts.length < 2)
			{
				return true;
			}
			
			Player targetPhantom = PhantomEngine.getPhantomByName(parts[0]);
			
			if (targetPhantom != null)
			{
				player.sendPacket(new CreatureSay(player, ChatType.WHISPER, "->" + targetPhantom.getName(), parts[1]));
				PhantomChat.requestChatResponse(targetPhantom, player, parts[1], true);
			}
			else
			{
				player.sendMessage("That phantom is not farming.");
			}
			
			return true;
		}
		else if (command.equalsIgnoreCase("pmenu"))
		{
			PhantomMenu.showMenu(player);
			return true;
		}
		else if (command.equalsIgnoreCase("pdebug"))
		{
			player.sendMessage("Text logs: " + (toggleDebug() ? "ON" : "OFF"));
			return true;
		}
		
		return false;
	}
	
	public static void main(String[] args)
	{
		new PhantomManager();
	}
}
