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
package com.l2journey.gameserver.data.xml;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.l2journey.commons.util.Rnd;
import com.l2journey.gameserver.model.Spawn;
import com.l2journey.gameserver.model.actor.Npc;

/**
 * This class holds parameter, specific to certain NPCs.<br>
 * It can be either general parameters overridden for certain NPC instance instead of template parameters(aggro range, for example), or some optional parameters, handled by datapack scripts.<br>
 * @author GKR
 */
public class NpcPersonalAIData
{
	private final Map<String, Map<String, Integer>> _AIData = new HashMap<>();
	
	/**
	 * Instantiates a new table.
	 */
	protected NpcPersonalAIData()
	{
	}
	
	/**
	 * Stores data for given spawn.
	 * @param spawnDat spawn to process
	 * @param data Map of AI values
	 */
	public void storeData(Spawn spawnDat, Map<String, Integer> data)
	{
		if ((data != null) && !data.isEmpty())
		{
			// check for spawn name. Since spawn name is key for AI Data, generate random name, if spawn name isn't specified
			if (spawnDat.getName() == null)
			{
				spawnDat.setName(Long.toString(Rnd.nextLong()));
			}
			
			_AIData.put(spawnDat.getName(), data);
		}
	}
	
	/**
	 * Gets AI value with given spawnName and paramName
	 * @param spawnName spawn name to check
	 * @param paramName parameter to check
	 * @return value of given parameter for given spawn name
	 */
	public int getAIValue(String spawnName, String paramName)
	{
		return hasAIValue(spawnName, paramName) ? _AIData.get(spawnName).get(paramName) : -1;
	}
	
	/**
	 * Verifies if there is AI value with given spawnName and paramName
	 * @param spawnName spawn name to check
	 * @param paramName parameter name to check
	 * @return {@code true} if parameter paramName is set for spawn spawnName, {@code false} otherwise
	 */
	public boolean hasAIValue(String spawnName, String paramName)
	{
		return (spawnName != null) && _AIData.containsKey(spawnName) && _AIData.get(spawnName).containsKey(paramName);
	}
	
	/**
	 * Initializes npc parameters by specified values.
	 * @param npc NPC to process
	 * @param spawn link to NPC's spawn
	 * @param spawnName name of spawn
	 */
	public void initializeNpcParameters(Npc npc, Spawn spawn, String spawnName)
	{
		if (_AIData.containsKey(spawnName))
		{
			final Map<String, Integer> map = _AIData.get(spawnName);
			
			try
			{
				// for (String key : map.keySet())
				for (Entry<String, Integer> entry : map.entrySet())
				{
					final String key = entry.getKey();
					switch (key)
					{
						case "disableRandomAnimation":
						{
							npc.setRandomAnimationEnabled((entry.getValue() == 0));
							break;
						}
						case "disableRandomWalk":
						{
							final boolean enable = entry.getValue() == 0;
							npc.setRandomWalking(enable);
							spawn.setRandomWalking(enable);
							break;
						}
					}
				}
			}
			catch (Exception _)
			{
				// Do nothing
			}
		}
	}
	
	/**
	 * Gets the single instance of NpcTable.
	 * @return single instance of NpcTable
	 */
	public static NpcPersonalAIData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final NpcPersonalAIData INSTANCE = new NpcPersonalAIData();
	}
}