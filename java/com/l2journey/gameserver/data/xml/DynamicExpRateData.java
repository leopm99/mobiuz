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

import java.io.File;
import java.util.Arrays;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.l2journey.Config;
import com.l2journey.commons.util.IXmlReader;

/**
 * @author MrNiceGuy
 */
public class DynamicExpRateData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(DynamicExpRateData.class.getName());
	
	private static float[] _expRates = new float[Config.PLAYER_MAXIMUM_LEVEL + 1];
	private static float[] _spRates = new float[Config.PLAYER_MAXIMUM_LEVEL + 1];
	private static boolean _enabled = false;
	
	protected DynamicExpRateData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		parseDatapackFile("config/admin/dynamicexprates.xml");
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		int count = 0;
		Arrays.fill(_expRates, 1f);
		Arrays.fill(_spRates, 1f);
		
		for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equals(n.getNodeName()))
			{
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("dynamic".equals(d.getNodeName()))
					{
						final NamedNodeMap attrs = d.getAttributes();
						final int level = parseInteger(attrs, "level");
						final float exp = parseFloat(attrs, "exp");
						final float sp = parseFloat(attrs, "sp");
						if ((exp != 1) || (sp != 1))
						{
							_expRates[level] = exp;
							_spRates[level] = sp;
							count++;
						}
					}
				}
			}
		}
		
		_enabled = count > 0;
		if (_enabled)
		{
			final int loggedCount = count;
			LOGGER.info(() -> getClass().getSimpleName() + ": Loaded dynamic rates for " + loggedCount + " levels.");
		}
	}
	
	/**
	 * @param level
	 * @return the dynamic EXP rate for specified level.
	 */
	public float getDynamicExpRate(int level)
	{
		return _expRates[level];
	}
	
	/**
	 * @param level
	 * @return the dynamic SP rate for specified level.
	 */
	public float getDynamicSpRate(int level)
	{
		return _spRates[level];
	}
	
	/**
	 * @return if dynamic rates are enabled.
	 */
	public boolean isEnabled()
	{
		return _enabled;
	}
	
	public static DynamicExpRateData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final DynamicExpRateData INSTANCE = new DynamicExpRateData();
	}
}
