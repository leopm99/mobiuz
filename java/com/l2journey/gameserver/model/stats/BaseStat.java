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
package com.l2journey.gameserver.model.stats;

import java.io.File;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.l2journey.Config;
import com.l2journey.gameserver.model.actor.Creature;

/**
 * @author DS
 */
public enum BaseStat
{
	STR(new STR()),
	INT(new INT()),
	DEX(new DEX()),
	WIT(new WIT()),
	CON(new CON()),
	MEN(new MEN()),
	NONE(new NONE());
	
	private static final Logger LOGGER = Logger.getLogger(BaseStat.class.getName());
	
	public static final int MAX_STAT_VALUE = 100;
	
	protected static final double[] STRbonus = new double[MAX_STAT_VALUE];
	protected static final double[] INTbonus = new double[MAX_STAT_VALUE];
	protected static final double[] DEXbonus = new double[MAX_STAT_VALUE];
	protected static final double[] WITbonus = new double[MAX_STAT_VALUE];
	protected static final double[] CONbonus = new double[MAX_STAT_VALUE];
	protected static final double[] MENbonus = new double[MAX_STAT_VALUE];
	
	private final IBaseStatFunction _stat;
	
	public String getValue()
	{
		return _stat.getClass().getSimpleName();
	}
	
	private BaseStat(IBaseStatFunction s)
	{
		_stat = s;
	}
	
	public double calcBonus(Creature actor)
	{
		if (actor != null)
		{
			return _stat.calcBonus(actor);
		}
		
		return 1;
	}
	
	public static BaseStat valueOfXml(String name)
	{
		final String internName = name.intern();
		for (BaseStat s : values())
		{
			if (s.getValue().equalsIgnoreCase(internName))
			{
				return s;
			}
		}
		
		throw new NoSuchElementException("Unknown name '" + internName + "' for enum BaseStats");
	}
	
	protected static class STR implements IBaseStatFunction
	{
		@Override
		public double calcBonus(Creature actor)
		{
			return STRbonus[actor.getSTR()];
		}
	}
	
	protected static class INT implements IBaseStatFunction
	{
		@Override
		public double calcBonus(Creature actor)
		{
			return INTbonus[actor.getINT()];
		}
	}
	
	protected static class DEX implements IBaseStatFunction
	{
		@Override
		public double calcBonus(Creature actor)
		{
			return DEXbonus[actor.getDEX()];
		}
	}
	
	protected static class WIT implements IBaseStatFunction
	{
		@Override
		public double calcBonus(Creature actor)
		{
			return WITbonus[actor.getWIT()];
		}
	}
	
	protected static class CON implements IBaseStatFunction
	{
		@Override
		public double calcBonus(Creature actor)
		{
			return CONbonus[actor.getCON()];
		}
	}
	
	protected static class MEN implements IBaseStatFunction
	{
		@Override
		public double calcBonus(Creature actor)
		{
			return MENbonus[actor.getMEN()];
		}
	}
	
	protected static class NONE implements IBaseStatFunction
	{
		@Override
		public double calcBonus(Creature actor)
		{
			return 1f;
		}
	}
	
	static
	{
		final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(false);
		factory.setIgnoringComments(true);
		final File file = new File(Config.DATAPACK_ROOT, "data/stats/statBonus.xml");
		Document document = null;
		
		if (file.exists())
		{
			try
			{
				document = factory.newDocumentBuilder().parse(file);
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, "[BaseStats] Could not parse file: " + e.getMessage(), e);
			}
			
			if (document != null)
			{
				String statName;
				int val;
				double bonus;
				NamedNodeMap attrs;
				for (Node list = document.getFirstChild(); list != null; list = list.getNextSibling())
				{
					if ("list".equalsIgnoreCase(list.getNodeName()))
					{
						for (Node stat = list.getFirstChild(); stat != null; stat = stat.getNextSibling())
						{
							statName = stat.getNodeName();
							for (Node value = stat.getFirstChild(); value != null; value = value.getNextSibling())
							{
								if ("stat".equalsIgnoreCase(value.getNodeName()))
								{
									attrs = value.getAttributes();
									try
									{
										val = Integer.parseInt(attrs.getNamedItem("value").getNodeValue());
										bonus = Double.parseDouble(attrs.getNamedItem("bonus").getNodeValue());
									}
									catch (Exception _)
									{
										LOGGER.severe("[BaseStats] Invalid stats value: " + value.getNodeValue() + ", skipping");
										continue;
									}
									
									if ("STR".equalsIgnoreCase(statName))
									{
										STRbonus[val] = bonus;
									}
									else if ("INT".equalsIgnoreCase(statName))
									{
										INTbonus[val] = bonus;
									}
									else if ("DEX".equalsIgnoreCase(statName))
									{
										DEXbonus[val] = bonus;
									}
									else if ("WIT".equalsIgnoreCase(statName))
									{
										WITbonus[val] = bonus;
									}
									else if ("CON".equalsIgnoreCase(statName))
									{
										CONbonus[val] = bonus;
									}
									else if ("MEN".equalsIgnoreCase(statName))
									{
										MENbonus[val] = bonus;
									}
									else
									{
										LOGGER.severe("[BaseStats] Invalid stats name: " + statName + ", skipping");
									}
								}
							}
						}
					}
				}
			}
		}
		else
		{
			throw new Error("[BaseStats] File not found: " + file.getName());
		}
	}
}
