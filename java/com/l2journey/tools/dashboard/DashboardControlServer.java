package com.l2journey.tools.dashboard;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

import com.l2journey.Config;
import com.l2journey.commons.enums.ServerMode;
import com.l2journey.gameserver.cache.HtmCache;
import com.l2journey.gameserver.data.xml.AdminData;
import com.l2journey.gameserver.data.xml.BuyListData;
import com.l2journey.gameserver.data.xml.MultisellData;
import com.l2journey.gameserver.model.World;
import com.l2journey.gameserver.util.Broadcast;

/**
 * Dashboard.
 * @author Aggretsuko
 */
public final class DashboardControlServer
{
	private static final Logger LOGGER = Logger.getLogger(DashboardControlServer.class.getName());
	public static final int PORT = 15432;
	private DashboardControlServer()
	{
	}

	public static void start()
	{
		final Thread thread = new Thread(DashboardControlServer::serve, "JourneyDash-Control");
		thread.setDaemon(true);
		thread.start();
	}
	
	private static void serve()
	{
		try (ServerSocket server = new ServerSocket(PORT, 16, InetAddress.getLoopbackAddress()))
		{
			LOGGER.info("JourneyDash: local control bridge listening on 127.0.0.1:" + PORT);
			while (true)
			{
				try (Socket socket = server.accept();
					BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
					PrintWriter output = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8))
				{
					output.println(execute(input.readLine()));
				}
				catch (Exception e)
				{
					LOGGER.warning("JourneyDash command failed: " + e.getMessage());
				}
			}
		}
		catch (IOException e)
		{
			LOGGER.warning("JourneyDash local control bridge unavailable: " + e.getMessage());
		}
	}
	
	private static String execute(String command)
	{
		if (command == null)
		{
			return "ERROR Empty command";
		}
		if (command.equals("STATUS"))
		{
			return "OK players=" + World.getInstance().getAllPlayersCount();
		}
		if (command.equals("RELOAD CONFIGS"))
		{
			Config.load(ServerMode.GAME);
		}
		else if (command.equals("RELOAD ACCESS"))
		{
			AdminData.getInstance().load();
		}
		else if (command.equals("RELOAD HTML"))
		{
			HtmCache.getInstance().reload();
		}
		else if (command.equals("RELOAD MULTISELLS"))
		{
			MultisellData.getInstance().load();
		}
		else if (command.equals("RELOAD BUYLISTS"))
		{
			BuyListData.getInstance().load();
		}
		else if (command.startsWith("ANNOUNCE NORMAL "))
		{
			Broadcast.toAllOnlinePlayers(command.substring(16), false);
		}
		else if (command.startsWith("ANNOUNCE CRITICAL "))
		{
			Broadcast.toAllOnlinePlayers(command.substring(18), true);
		}
		else
		{
			return "ERROR Unknown command";
		}
		return "OK";
	}
}
