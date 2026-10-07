/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.l2journey.gameserver.data.xml;

import java.io.File;
import java.util.EnumMap;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.l2journey.commons.util.IXmlReader;
import com.l2journey.gameserver.model.actor.enums.player.PlayerClass;
import com.l2journey.gameserver.model.actor.holders.player.ClassInfoHolder;

/**
 * Loads the the list of classes and it's info.
 * @author Zoey76
 */
public class ClassListData implements IXmlReader
{
	private final Map<PlayerClass, ClassInfoHolder> _classData = new EnumMap<>(PlayerClass.class);
	
	/**
	 * Instantiates a new class list data.
	 */
	protected ClassListData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		_classData.clear();
		parseDatapackFile("data/stats/chars/classList.xml");
		LOGGER.info(() -> getClass().getSimpleName() + ": Loaded " + _classData.size() + " class data.");
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
					final NamedNodeMap attrs = d.getAttributes();
					if ("class".equals(d.getNodeName()))
					{
						Node attr = attrs.getNamedItem("classId");
						final PlayerClass classId = PlayerClass.getPlayerClass(parseInteger(attr));
						attr = attrs.getNamedItem("name");
						final String className = attr.getNodeValue();
						attr = attrs.getNamedItem("parentClassId");
						final PlayerClass parentClassId = (attr != null) ? PlayerClass.getPlayerClass(parseInteger(attr)) : null;
						_classData.put(classId, new ClassInfoHolder(classId, className, parentClassId));
					}
				}
			}
		}
	}
	
	/**
	 * Gets the class list.
	 * @return the complete class list
	 */
	public Map<PlayerClass, ClassInfoHolder> getClassList()
	{
		return _classData;
	}
	
	/**
	 * Gets the class info.
	 * @param classId the class ID
	 * @return the class info related to the given {@code classId}
	 */
	public ClassInfoHolder getClass(PlayerClass classId)
	{
		return _classData.get(classId);
	}
	
	/**
	 * Gets the class info.
	 * @param classId the class Id as integer
	 * @return the class info related to the given {@code classId}
	 */
	public ClassInfoHolder getClass(int classId)
	{
		final PlayerClass id = PlayerClass.getPlayerClass(classId);
		return (id != null) ? _classData.get(id) : null;
	}
	
	/**
	 * Gets the single instance of ClassListData.
	 * @return single instance of ClassListData
	 */
	public static ClassListData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final ClassListData INSTANCE = new ClassListData();
	}
}