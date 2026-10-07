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
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.l2journey.commons.util.IXmlReader;
import com.l2journey.commons.util.StringUtil;
import com.l2journey.gameserver.model.SiegeScheduleDate;
import com.l2journey.gameserver.model.StatSet;

/**
 * @author UnAfraid
 */
public class SiegeScheduleData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(SiegeScheduleData.class.getName());
	
	private final Map<Integer, SiegeScheduleDate> _scheduleData = new HashMap<>();
	
	protected SiegeScheduleData()
	{
		load();
	}
	
	@Override
	public synchronized void load()
	{
		parseDatapackFile("config/siege/siegeschedule.xml");
		LOGGER.info(() -> getClass().getSimpleName() + ": Loaded " + _scheduleData.size() + " siege schedulers.");
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				for (Node cd = n.getFirstChild(); cd != null; cd = cd.getNextSibling())
				{
					switch (cd.getNodeName())
					{
						case "schedule":
						{
							final StatSet set = new StatSet();
							final NamedNodeMap attrs = cd.getAttributes();
							for (int i = 0; i < attrs.getLength(); i++)
							{
								final Node node = attrs.item(i);
								final String key = node.getNodeName();
								String val = node.getNodeValue();
								if ("day".equals(key) && !StringUtil.isNumeric(val))
								{
									val = Integer.toString(getValueForField(val));
								}
								set.set(key, val);
							}
							_scheduleData.put(set.getInt("castleId"), new SiegeScheduleDate(set));
							break;
						}
					}
				}
			}
		}
	}
	
	/**
	 * Retrieves the integer value associated with a specific field name in the {@code Calendar} class. This method uses reflection to access the value of the specified field from {@code Calendar}.
	 * @param field the name of the field in the {@code Calendar} class to retrieve
	 * @return the integer value of the specified {@code Calendar} field, or {@code -1} if the field cannot be accessed or does not exist
	 */
	private int getValueForField(String field)
	{
		try
		{
			return Calendar.class.getField(field).getInt(Calendar.class.getName());
		}
		catch (Exception e)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Could not get value for field " + field + ". " + e.getMessage());
			return -1;
		}
	}
	
	/**
	 * Retrieves the scheduled siege date associated with a specific castle ID.
	 * @param castleId the ID of the castle for which to retrieve the siege schedule date
	 * @return the {@code SiegeScheduleDate} for the specified castle ID, or {@code null} if no schedule data is available for this castle ID
	 */
	public SiegeScheduleDate getScheduleDateForCastleId(int castleId)
	{
		return _scheduleData.get(castleId);
	}
	
	public static SiegeScheduleData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final SiegeScheduleData INSTANCE = new SiegeScheduleData();
	}
}
