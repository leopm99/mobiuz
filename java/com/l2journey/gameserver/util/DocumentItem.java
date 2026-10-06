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
package com.l2journey.gameserver.util;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.l2journey.gameserver.model.StatSet;
import com.l2journey.gameserver.model.conditions.Condition;
import com.l2journey.gameserver.model.item.ItemTemplate;

/**
 * @author mkizub, JIV
 */
public class DocumentItem extends DocumentBase
{
	private DocumentItemDataHolder _currentItem = null;
	private final List<ItemTemplate> _itemsInFile = new ArrayList<>();
	
	private class DocumentItemDataHolder
	{
		public DocumentItemDataHolder()
		{
		}
		
		int id;
		String type;
		StatSet set;
		int currentLevel;
		ItemTemplate item;
	}
	
	/**
	 * @param file
	 */
	public DocumentItem(File file)
	{
		super(file);
	}
	
	@Override
	protected StatSet getStatSet()
	{
		return _currentItem.set;
	}
	
	@Override
	protected String getTableValue(String name)
	{
		return _tables.get(name)[_currentItem.currentLevel];
	}
	
	@Override
	protected String getTableValue(String name, int idx)
	{
		return _tables.get(name)[idx - 1];
	}
	
	@Override
	protected void parseDocument(Document document)
	{
		for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("item".equalsIgnoreCase(d.getNodeName()))
					{
						try
						{
							_currentItem = new DocumentItemDataHolder();
							parseItem(d);
							_itemsInFile.add(_currentItem.item);
							resetTable();
						}
						catch (Exception e)
						{
							LOGGER.log(Level.WARNING, e, () -> "Cannot create item " + _currentItem.id);
						}
					}
				}
			}
		}
	}
	
	private void parseItem(Node node) throws InvocationTargetException
	{
		Node n = node;
		final int itemId = Integer.parseInt(n.getAttributes().getNamedItem("id").getNodeValue());
		final String className = n.getAttributes().getNamedItem("type").getNodeValue();
		final String itemName = n.getAttributes().getNamedItem("name").getNodeValue();
		
		_currentItem.id = itemId;
		_currentItem.type = className;
		_currentItem.set = new StatSet();
		_currentItem.set.set("item_id", itemId);
		_currentItem.set.set("name", itemName);
		
		final Node first = n.getFirstChild();
		for (n = first; n != null; n = n.getNextSibling())
		{
			if ("table".equalsIgnoreCase(n.getNodeName()))
			{
				if (_currentItem.item != null)
				{
					throw new IllegalStateException("Item created but table node found! Item " + itemId);
				}
				parseTable(n);
			}
			else if ("set".equalsIgnoreCase(n.getNodeName()))
			{
				if (_currentItem.item != null)
				{
					throw new IllegalStateException("Item created but set node found! Item " + itemId);
				}
				parseBeanSet(n, _currentItem.set, 1);
			}
			else if ("stats".equalsIgnoreCase(n.getNodeName()))
			{
				makeItem();
				parseTemplate(n, _currentItem.item);
			}
			else if ("conditions".equalsIgnoreCase(n.getNodeName()))
			{
				makeItem();
				final Condition condition = parseCondition(n.getFirstChild(), _currentItem.item);
				final Node msg = n.getAttributes().getNamedItem("msg");
				final Node msgId = n.getAttributes().getNamedItem("msgId");
				if ((condition != null) && (msg != null))
				{
					condition.setMessage(msg.getNodeValue());
				}
				else if ((condition != null) && (msgId != null))
				{
					condition.setMessageId(Integer.decode(getValue(msgId.getNodeValue(), null)));
					final Node addName = n.getAttributes().getNamedItem("addName");
					if ((addName != null) && (Integer.decode(getValue(msgId.getNodeValue(), null)) > 0))
					{
						condition.addName();
					}
				}
				_currentItem.item.attach(condition);
			}
		}
		// bah! in this point item doesn't have to be still created
		makeItem();
	}
	
	private void makeItem() throws InvocationTargetException
	{
		// If item exists just reload the data.
		if (_currentItem.item != null)
		{
			_currentItem.item.set(_currentItem.set);
			return;
		}
		
		try
		{
			final Constructor<?> itemClass = Class.forName("com.l2journey.gameserver.model.item." + _currentItem.type).getConstructor(StatSet.class);
			_currentItem.item = (ItemTemplate) itemClass.newInstance(_currentItem.set);
		}
		catch (Exception e)
		{
			throw new InvocationTargetException(e);
		}
	}
	
	public List<ItemTemplate> getItemList()
	{
		return _itemsInFile;
	}
}
