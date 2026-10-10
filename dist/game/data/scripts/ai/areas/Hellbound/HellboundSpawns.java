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
package ai.areas.Hellbound;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.l2journey.commons.util.IXmlReader;
import com.l2journey.gameserver.data.SpawnTable;
import com.l2journey.gameserver.model.Location;
import com.l2journey.gameserver.model.Spawn;

/**
 * Hellbound Spawns parser.
 * @author Zoey76
 */
public class HellboundSpawns implements IXmlReader
{
	private final List<Spawn> _spawns = new ArrayList<>();
	private final Map<Spawn, int[]> _spawnLevels = new HashMap<>();
	
	public HellboundSpawns()
	{
		load();
	}
	
	@Override
	public void load()
	{
		_spawns.clear();
		_spawnLevels.clear();
		parseDatapackFile("data/scripts/ai/areas/Hellbound/hellboundSpawns.xml");
		LOGGER.info(() -> getClass().getSimpleName() + ": Loaded " + _spawns.size() + " Hellbound spawns.");
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		for (Node node = document.getFirstChild(); node != null; node = node.getNextSibling())
		{
			if ("list".equals(node.getNodeName()))
			{
				for (Node npc = node.getFirstChild(); npc != null; npc = npc.getNextSibling())
				{
					parseSpawn(npc);
				}
			}
		}
	}
	
	/**
	 * Parses the spawn.
	 * @param npc the NPC to parse
	 */
	private void parseSpawn(Node npc)
	{
		if ("npc".equals(npc.getNodeName()))
		{
			final Node id = npc.getAttributes().getNamedItem("id");
			if (id == null)
			{
				LOGGER.severe(getClass().getSimpleName() + ": Missing NPC ID, skipping record!");
				return;
			}
			
			final int npcId = Integer.parseInt(id.getNodeValue());
			Location loc = null;
			int delay = 0;
			int randomInterval = 0;
			int minLevel = 1;
			int maxLevel = 100;
			for (Node element = npc.getFirstChild(); element != null; element = element.getNextSibling())
			{
				if (element.getNodeType() != Node.ELEMENT_NODE)
				{
					continue;
				}
				final NamedNodeMap attrs = element.getAttributes();
				minLevel = 1;
				maxLevel = 100;
				switch (element.getNodeName())
				{
					case "location":
					{
						loc = new Location(parseInteger(attrs, "x"), parseInteger(attrs, "y"), parseInteger(attrs, "z"), parseInteger(attrs, "heading", 0));
						break;
					}
					case "respawn":
					{
						delay = parseInteger(attrs, "delay");
						randomInterval = attrs.getNamedItem("randomInterval") != null ? parseInteger(attrs, "randomInterval") : 1;
						break;
					}
					case "hellboundLevel":
					{
						minLevel = parseInteger(attrs, "min", 1);
						maxLevel = parseInteger(attrs, "max", 100);
						break;
					}
					default:
					{
						break;
					}
				}
			}
			
			try
			{
				final Spawn spawn = new Spawn(npcId);
				spawn.setAmount(1);
				if (loc == null)
				{
					LOGGER.warning("Hellbound spawn location is null!");
				}
				spawn.setLocation(loc);
				spawn.setRespawnDelay(delay, randomInterval);
				_spawnLevels.put(spawn, new int[]
				{
					minLevel,
					maxLevel
				});
				SpawnTable.getInstance().addSpawn(spawn);
				_spawns.add(spawn);
			}
			catch (SecurityException | ClassNotFoundException | NoSuchMethodException e)
			{
				LOGGER.warning(getClass().getSimpleName() + ": Couldn't load spawns: " + e.getMessage());
			}
		}
	}
	
	/**
	 * Gets all Hellbound spawns.
	 * @return the list of Hellbound spawns.
	 */
	public List<Spawn> getSpawns()
	{
		return _spawns;
	}
	
	/**
	 * Gets the spawn minimum level.
	 * @param spawn the NPC Spawn
	 * @return the spawn minimum level
	 */
	public int getSpawnMinLevel(Spawn spawn)
	{
		return _spawnLevels.containsKey(spawn) ? _spawnLevels.get(spawn)[0] : 1;
	}
	
	/**
	 * Gets the spawn maximum level.
	 * @param spawn the NPC Spawn
	 * @return the spawn maximum level
	 */
	public int getSpawnMaxLevel(Spawn spawn)
	{
		return _spawnLevels.containsKey(spawn) ? _spawnLevels.get(spawn)[1] : 1;
	}
	
	public static HellboundSpawns getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final HellboundSpawns INSTANCE = new HellboundSpawns();
	}
}