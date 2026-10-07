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
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.l2journey.commons.util.IXmlReader;

/**
 * Point data parser.
 * @author Zoey76
 */
public class HellboundPointData implements IXmlReader
{
	private final Map<Integer, int[]> _pointsInfo = new HashMap<>();
	
	public HellboundPointData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		_pointsInfo.clear();
		parseDatapackFile("data/scripts/ai/areas/Hellbound/hellboundTrustPoints.xml");
		LOGGER.info(() -> getClass().getSimpleName() + ": Loaded " + _pointsInfo.size() + " trust point reward data.");
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equals(n.getNodeName()))
			{
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					parsePoint(d);
				}
			}
		}
	}
	
	/**
	 * Parses the point.
	 * @param d the node to parse
	 */
	private void parsePoint(Node d)
	{
		if ("npc".equals(d.getNodeName()))
		{
			final NamedNodeMap attrs = d.getAttributes();
			Node att = attrs.getNamedItem("id");
			if (att == null)
			{
				LOGGER.severe(getClass().getSimpleName() + ": Missing NPC ID, skipping record!");
				return;
			}
			
			final int npcId = Integer.parseInt(att.getNodeValue());
			att = attrs.getNamedItem("points");
			if (att == null)
			{
				LOGGER.severe(() -> "[Hellbound Trust Points Info] Missing reward point info for NPC ID " + npcId + ", skipping record");
				return;
			}
			
			final int points = Integer.parseInt(att.getNodeValue());
			att = attrs.getNamedItem("minHellboundLvl");
			if (att == null)
			{
				LOGGER.severe(() -> "[Hellbound Trust Points Info] Missing minHellboundLvl info for NPC ID " + npcId + ", skipping record");
				return;
			}
			
			final int minHbLvl = Integer.parseInt(att.getNodeValue());
			att = attrs.getNamedItem("maxHellboundLvl");
			if (att == null)
			{
				LOGGER.severe(() -> "[Hellbound Trust Points Info] Missing maxHellboundLvl info for NPC ID " + npcId + ", skipping record");
				return;
			}
			
			final int maxHbLvl = Integer.parseInt(att.getNodeValue());
			att = attrs.getNamedItem("lowestTrustLimit");
			final int lowestTrustLimit = (att == null) ? 0 : Integer.parseInt(att.getNodeValue());
			_pointsInfo.put(npcId, new int[]
			{
				points,
				minHbLvl,
				maxHbLvl,
				lowestTrustLimit
			});
		}
	}
	
	/**
	 * Gets all the points data.
	 * @return the points data
	 */
	public Map<Integer, int[]> getPointsInfo()
	{
		return _pointsInfo;
	}
	
	/**
	 * Gets the points amount for an specific NPC ID.
	 * @param npcId the NPC ID
	 * @return the points for an specific NPC ID
	 */
	public int getPointsAmount(int npcId)
	{
		return _pointsInfo.get(npcId)[0];
	}
	
	/**
	 * Get the minimum Hellbound level for the given NPC ID.
	 * @param npcId the NPC ID
	 * @return the minimum Hellbound level for the given NPC ID
	 */
	public int getMinHbLvl(int npcId)
	{
		return _pointsInfo.get(npcId)[1];
	}
	
	/**
	 * Get the maximum Hellbound level for the given NPC ID.
	 * @param npcId the NPC ID
	 * @return the maximum Hellbound level for the given NPC ID
	 */
	public int getMaxHbLvl(int npcId)
	{
		return _pointsInfo.get(npcId)[2];
	}
	
	/**
	 * Get the lowest trust limit for the given NPC ID.
	 * @param npcId the NPC ID
	 * @return the lowest trust limit for the given NPC ID
	 */
	public int getLowestTrustLimit(int npcId)
	{
		return _pointsInfo.get(npcId)[3];
	}
	
	public static HellboundPointData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final HellboundPointData INSTANCE = new HellboundPointData();
	}
}