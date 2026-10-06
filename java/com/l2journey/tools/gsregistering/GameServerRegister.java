/*
 * Copyright (c) 2013 L2jMobius
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.l2journey.tools.gsregistering;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.sql.SQLException;
import java.util.Map.Entry;

import com.l2journey.loginserver.GameServerTable;

public class GameServerRegister extends BaseGameServerRegister
{
	private LineNumberReader _in;
	
	public static void main(String[] args)
	{
		// Backwards compatibility, redirect to the new one
		BaseGameServerRegister.main(args);
	}
	
	public GameServerRegister()
	{
		load();
		
		if (GameServerTable.getInstance().getServerNames().isEmpty())
		{
			System.out.println("No available names for GameServer, verify servername.xml file exists in the LoginServer folder.");
			System.exit(1);
		}
	}
	
	public void consoleUI() throws IOException
	{
		_in = new LineNumberReader(new InputStreamReader(System.in));
		boolean choiceOk = false;
		String choice;
		
		while (true)
		{
			hr();
			System.out.println("GSRegister");
			System.out.println(System.lineSeparator());
			System.out.println("1 - Register GameServer");
			System.out.println("2 - List GameServers Names and IDs");
			System.out.println("3 - Remove GameServer");
			System.out.println("4 - Remove ALL GameServers");
			System.out.println("5 - Exit");
			
			do
			{
				System.out.print("Choice: ");
				choice = _in.readLine();
				try
				{
					final int choiceNumber = Integer.parseInt(choice);
					choiceOk = true;
					
					switch (choiceNumber)
					{
						case 1:
						{
							registerNewGS();
							break;
						}
						case 2:
						{
							listGSNames();
							break;
						}
						case 3:
						{
							unregisterSingleGS();
							break;
						}
						case 4:
						{
							unregisterAllGS();
							break;
						}
						case 5:
						{
							System.exit(0);
							break;
						}
						default:
						{
							System.out.printf("Invalid Choice: %s" + "%n", choice);
							choiceOk = false;
						}
					}
				}
				catch (NumberFormatException _)
				{
					System.out.printf("Invalid Choice: %s" + "%n", choice);
				}
			}
			while (!choiceOk);
		}
	}
	
	/**
	 * 
	 */
	private void hr()
	{
		System.out.println("_____________________________________________________" + System.lineSeparator());
	}
	
	/**
	 * 
	 */
	private void listGSNames()
	{
		int idMaxLen = 0;
		int nameMaxLen = 0;
		for (Entry<Integer, String> e : GameServerTable.getInstance().getServerNames().entrySet())
		{
			if (e.getKey().toString().length() > idMaxLen)
			{
				idMaxLen = e.getKey().toString().length();
			}
			if (e.getValue().length() > nameMaxLen)
			{
				nameMaxLen = e.getValue().length();
			}
		}
		idMaxLen += 2;
		nameMaxLen += 2;
		
		String id;
		boolean inUse;
		final String gsInUse = "In Use";
		final String gsFree = "Free";
		final int gsStatusMaxLen = Math.max(gsInUse.length(), gsFree.length()) + 2;
		for (Entry<Integer, String> e : GameServerTable.getInstance().getServerNames().entrySet())
		{
			id = e.getKey().toString();
			System.out.print(id);
			
			for (int i = id.length(); i < idMaxLen; i++)
			{
				System.out.print(' ');
			}
			System.out.print("| ");
			
			System.out.print(e.getValue());
			
			for (int i = e.getValue().length(); i < nameMaxLen; i++)
			{
				System.out.print(' ');
			}
			System.out.print("| ");
			
			inUse = GameServerTable.getInstance().hasRegisteredGameServerOnId(e.getKey());
			final String inUseStr = inUse ? gsInUse : gsFree;
			System.out.print(inUseStr);
			
			for (int i = inUseStr.length(); i < gsStatusMaxLen; i++)
			{
				System.out.print(' ');
			}
			System.out.println('|');
		}
	}
	
	/**
	 * @throws IOException
	 */
	private void unregisterAllGS() throws IOException
	{
		if (yesNoQuestion("Are you sure you want to remove ALL GameServers?"))
		{
			try
			{
				BaseGameServerRegister.unregisterAllGameServers();
				System.out.println("All GameServers were successfully removed.");
			}
			catch (SQLException e)
			{
				showError("An SQL error occurred while trying to remove ALL GameServers.", e);
			}
		}
	}
	
	private boolean yesNoQuestion(String question) throws IOException
	{
		do
		{
			hr();
			System.out.println(question);
			System.out.println("1 - Yes");
			System.out.println("2 - No");
			System.out.print("Choice: ");
			String choice;
			choice = _in.readLine();
			if (choice != null)
			{
				if (choice.equals("1"))
				{
					return true;
				}
				else if (choice.equals("2"))
				{
					return false;
				}
				else
				{
					System.out.printf("Invalid Choice: %s" + "%n", choice);
				}
			}
		}
		while (true);
	}
	
	/**
	 * @throws IOException
	 */
	private void unregisterSingleGS() throws IOException
	{
		String line;
		int id = Integer.MIN_VALUE;
		
		do
		{
			System.out.print("Enter desired ID: ");
			line = _in.readLine();
			try
			{
				id = Integer.parseInt(line);
			}
			catch (NumberFormatException _)
			{
				System.out.printf("Invalid Choice: %s" + "%n", line);
			}
		}
		while (id == Integer.MIN_VALUE);
		
		final String name = GameServerTable.getInstance().getServerNameById(id);
		if (name == null)
		{
			System.out.printf("No name for ID: %d" + "%n", id);
		}
		else if (GameServerTable.getInstance().hasRegisteredGameServerOnId(id))
		{
			System.out.printf("Are you sure you want to remove GameServer %d - %s?" + "%n", id, name);
			try
			{
				BaseGameServerRegister.unregisterGameServer(id);
				System.out.printf("GameServer ID: %d was successfully removed from LoginServer." + "%n", id);
			}
			catch (SQLException e)
			{
				showError("An SQL error occurred while trying to remove the GameServer.", e);
			}
		}
		else
		{
			System.out.printf("No GameServer is registered on ID: %d" + "%n", id);
		}
	}
	
	private void registerNewGS() throws IOException
	{
		String line;
		int id = Integer.MIN_VALUE;
		
		do
		{
			System.out.println("Enter desired ID:");
			line = _in.readLine();
			try
			{
				id = Integer.parseInt(line);
			}
			catch (NumberFormatException _)
			{
				System.out.printf("Invalid Choice: %s" + "%n", line);
			}
		}
		while (id == Integer.MIN_VALUE);
		
		if (GameServerTable.getInstance().getServerNameById(id) == null)
		{
			System.out.printf("No name for ID: %d" + "%n", id);
		}
		else if (GameServerTable.getInstance().hasRegisteredGameServerOnId(id))
		{
			System.out.println("This ID is not available.");
		}
		else
		{
			try
			{
				BaseGameServerRegister.registerGameServer(id, ".");
			}
			catch (IOException e)
			{
				showError("An error saving the hexid file occurred while trying to register the GameServer.", e);
			}
		}
	}
	
	@Override
	public void showError(String msg, Throwable t)
	{
		System.out.println("Error: " + msg + System.lineSeparator() + "Reason: " + t.getLocalizedMessage());
	}
}