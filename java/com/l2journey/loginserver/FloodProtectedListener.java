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
 * 
 * ---
 * 
 * Portions of this software are derived from the L2JMobius Project, 
 * shared under the MIT License. The original license terms are preserved where 
 * applicable..
 * 
 */
package com.l2journey.loginserver;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.l2journey.Config;

/**
 * @author -Wooden-
 */
public abstract class FloodProtectedListener extends Thread
{
	private static final Logger LOGGER = Logger.getLogger(FloodProtectedListener.class.getName());
	
	private final Map<String, ForeignConnection> _floodProtection = new ConcurrentHashMap<>();
	
	private ServerSocket _serverSocket;
	
	public FloodProtectedListener(String listenIp, int port) throws IOException
	{
		// If listening IP is "*", listen on all interfaces.
		if (listenIp.equals("*"))
		{
			_serverSocket = new ServerSocket(port);
		}
		else // Else, listen on the specified IP.
		{
			_serverSocket = new ServerSocket(port, 50, InetAddress.getByName(listenIp));
		}
	}
	
	@Override
	public void run()
	{
		Socket connection = null;
		while (!isInterrupted()) // Continue until the thread is interrupted.
		{
			try
			{
				connection = _serverSocket.accept(); // Accept incoming connections.
				if (Config.FLOOD_PROTECTION)
				{
					// Check for flood protection on the connection.
					ForeignConnection fConnection = _floodProtection.get(connection.getInetAddress().getHostAddress());
					if (fConnection != null) // If there's an existing connection from this IP.
					{
						fConnection.connectionNumber += 1;
						if (((fConnection.connectionNumber > Config.FAST_CONNECTION_LIMIT) && ((System.currentTimeMillis() - fConnection.lastConnection) < Config.NORMAL_CONNECTION_TIME)) || ((System.currentTimeMillis() - fConnection.lastConnection) < Config.FAST_CONNECTION_TIME) || (fConnection.connectionNumber > Config.MAX_CONNECTION_PER_IP))
						{
							fConnection.lastConnection = System.currentTimeMillis();
							connection.close();
							fConnection.connectionNumber -= 1;
							if (!fConnection.isFlooding)
							{
								LOGGER.warning("Potential Flood from " + connection.getInetAddress().getHostAddress());
							}
							fConnection.isFlooding = true;
							continue;
						}
						if (fConnection.isFlooding) // if connection was flooding server but now passed the check
						{
							fConnection.isFlooding = false;
							LOGGER.info(connection.getInetAddress().getHostAddress() + " is not considered as flooding anymore.");
						}
						fConnection.lastConnection = System.currentTimeMillis();
					}
					else // If it's a new connection.
					{
						// Initialize flood protection for the new connection.
						fConnection = new ForeignConnection(System.currentTimeMillis());
						_floodProtection.put(connection.getInetAddress().getHostAddress(), fConnection);
					}
				}
				
				// Add client connection for further processing (implementation in subclasses).
				addClient(connection);
			}
			catch (Exception _)
			{
				// Handle exceptions and potential thread interruption.
				if (isInterrupted())
				{
					// Close server socket and break the loop on thread interruption.
					try
					{
						_serverSocket.close();
					}
					catch (IOException io)
					{
						LOGGER.log(Level.INFO, "", io);
					}
					break;
				}
			}
		}
	}
	
	protected static class ForeignConnection
	{
		public int connectionNumber;
		public long lastConnection;
		public boolean isFlooding = false;
		
		public ForeignConnection(long time)
		{
			// Initialize a new foreign connection.
			lastConnection = time;
			connectionNumber = 1;
		}
	}
	
	public abstract void addClient(Socket s);
	
	public void removeFloodProtection(String ip)
	{
		// Only proceed if flood protection is enabled.
		if (!Config.FLOOD_PROTECTION)
		{
			return;
		}
		// Decrement connection count or remove if no more connections exist.
		final ForeignConnection fConnection = _floodProtection.get(ip);
		if (fConnection != null)
		{
			fConnection.connectionNumber -= 1;
			if (fConnection.connectionNumber == 0)
			{
				_floodProtection.remove(ip);
			}
		}
		else
		{
			LOGGER.warning(() -> "Removing a flood protection for a GameServer that was not in the connection map??? :" + ip);
		}
	}
	
	public void close()
	{
		try
		{
			_serverSocket.close();
		}
		catch (IOException e)
		{
			LOGGER.warning(getClass().getSimpleName() + ": " + e.getMessage());
		}
	}
}