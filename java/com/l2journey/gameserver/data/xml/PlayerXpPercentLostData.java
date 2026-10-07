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
package com.l2journey.gameserver.data.xml;

import java.io.File;
import java.util.Arrays;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;

import com.l2journey.commons.util.IXmlReader;

/**
 * @author Zealar, Mobius
 */
public class PlayerXpPercentLostData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(PlayerXpPercentLostData.class.getName());
	
	private final int _maxlevel = ExperienceData.getInstance().getMaxLevel();
	private final double[] _playerXpPercentLost = new double[_maxlevel + 1];
	
	protected PlayerXpPercentLostData()
	{
		Arrays.fill(_playerXpPercentLost, 1.);
		load();
	}
	
	@Override
	public void load()
	{
		parseDatapackFile("data/stats/chars/playerXpPercentLost.xml");
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		forEach(document, "list", listNode ->
		{
			forEach(listNode, "xpLost", xpLostNode ->
			{
				final NamedNodeMap attrs = xpLostNode.getAttributes();
				final int level = parseInteger(attrs, "level");
				if (level > _maxlevel)
				{
					return;
				}
				
				_playerXpPercentLost[level] = parseDouble(attrs, "val");
			});
		});
	}
	
	/**
	 * Retrieves the experience point (XP) percentage lost for a specified level.
	 * @param level the level for which to retrieve the XP loss percentage.
	 * @return the XP loss percentage for the specified level. If the level is higher than the maximum allowed, the XP loss percentage for the maximum level is returned, and a warning is logged.
	 */
	public double getXpPercent(int level)
	{
		if (level > _maxlevel)
		{
			LOGGER.warning(() -> "Require to high level inside PlayerXpPercentLostData (" + level + ")");
			return _playerXpPercentLost[_maxlevel];
		}
		
		return _playerXpPercentLost[level];
	}
	
	public static PlayerXpPercentLostData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final PlayerXpPercentLostData INSTANCE = new PlayerXpPercentLostData();
	}
}
