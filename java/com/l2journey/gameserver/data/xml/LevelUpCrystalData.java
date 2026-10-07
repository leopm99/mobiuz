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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.l2journey.Config;
import com.l2journey.commons.util.IXmlReader;
import com.l2journey.gameserver.data.enums.AbsorbCrystalType;
import com.l2journey.gameserver.data.holders.LevelingSoulCrystalInfo;
import com.l2journey.gameserver.data.holders.SoulCrystal;
import com.l2journey.gameserver.data.holders.SoulCrystalDisplay;
import com.l2journey.gameserver.model.actor.templates.NpcTemplate;

/**
 * Loads item enchant data.
 * @author UnAfraid
 */
public class LevelUpCrystalData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(LevelUpCrystalData.class.getName());
	
	private static final Map<Integer, SoulCrystal> _soulCrystalsData = new HashMap<>();
	private static final Map<Integer, Map<Integer, LevelingSoulCrystalInfo>> _npcLevelingInfo = new HashMap<>();
	private static List<SoulCrystalDisplay> _crystalDisplayInfo = new ArrayList<>();
	
	protected LevelUpCrystalData()
	{
		load();
	}
	
	@Override
	public synchronized void load()
	{
		_soulCrystalsData.clear();
		_npcLevelingInfo.clear();
		parseDatapackFile("data/LevelUpCrystalData.xml");
		LOGGER.info(() -> "Soul crystal data : Loaded " + _soulCrystalsData.size());
		LOGGER.info(() -> "Npc leveling crystal data : Loaded " + _npcLevelingInfo.size());
	}
	
	/**
	 * Gets the enchant scroll.
	 * @return Leveling SoulCrystal Information Map based NPC
	 */
	public final Map<Integer, Map<Integer, LevelingSoulCrystalInfo>> getNpcsSoulInfo()
	{
		return _npcLevelingInfo;
	}
	
	/**
	 * Gets the enchant scroll.
	 * @param npcid
	 * @return Leveling SoulCrystal Information map by items
	 */
	public final Map<Integer, LevelingSoulCrystalInfo> getNpcSoulInfo(int npcid)
	{
		return _npcLevelingInfo.get(npcid);
	}
	
	/**
	 * Gets the enchant scroll.
	 * @return Map soul cristal based key itemid
	 */
	public final Map<Integer, SoulCrystal> getSoulCrystals()
	{
		return _soulCrystalsData;
	}
	
	/**
	 * Gets the enchant scroll.
	 * @param itemid
	 * @return Leveling SoulCrystal Information map by items
	 */
	public final SoulCrystal getSoulCrystal(int itemid)
	{
		return _soulCrystalsData.get(itemid);
	}
	
	/**
	 * Gets the enchant scroll.
	 * @return SoulCrystalDisplay list
	 */
	public final List<SoulCrystalDisplay> getSoulCrystalInfo()
	{
		return _crystalDisplayInfo;
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		if (!file.exists())
		{
			LOGGER.log(Level.WARNING, "Missing LevelUpCrystalData.xml. The quest wont work without it!");
			return;
		}
		
		Node first = document.getFirstChild();
		if ((first != null) && "list".equalsIgnoreCase(first.getNodeName()))
		{
			for (Node n = first.getFirstChild(); n != null; n = n.getNextSibling())
			{
				if ("crystal".equalsIgnoreCase(n.getNodeName()))
				{
					for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
					{
						if ("item".equalsIgnoreCase(d.getNodeName()))
						{
							NamedNodeMap attrs = d.getAttributes();
							Node att = attrs.getNamedItem("itemId");
							if (att == null)
							{
								LOGGER.info("Missing itemId in Crystal List, skipping!");
								continue;
							}
							int itemId = Integer.parseInt(attrs.getNamedItem("itemId").getNodeValue());
							
							att = attrs.getNamedItem("level");
							if (att == null)
							{
								LOGGER.info(() -> "Missing level in Crystal List item Id " + itemId + " skipping!");
								continue;
							}
							int level = Integer.parseInt(attrs.getNamedItem("level").getNodeValue());
							
							att = attrs.getNamedItem("leveledItemId");
							if (att == null)
							{
								LOGGER.info(() -> "Missing leveledItemId in Crystal List item Id " + itemId + " skipping!");
								continue;
							}
							int leveledItemId = Integer.parseInt(attrs.getNamedItem("leveledItemId").getNodeValue());
							
							_soulCrystalsData.put(itemId, new SoulCrystal(level, itemId, leveledItemId));
						}
					}
				}
				else if ("npc".equalsIgnoreCase(n.getNodeName()))
				{
					for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
					{
						if ("item".equalsIgnoreCase(d.getNodeName()))
						{
							NamedNodeMap attrs = d.getAttributes();
							Node att = attrs.getNamedItem("npcId");
							if (att == null)
							{
								LOGGER.info("Missing npc Id in NPC List, skipping!");
								continue;
							}
							
							int npcId = Integer.parseInt(att.getNodeValue());
							Map<Integer, LevelingSoulCrystalInfo> temp = new HashMap<>();
							for (Node cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling())
							{
								boolean isSkillNeeded = false;
								int chance = 5;
								AbsorbCrystalType absorbType = AbsorbCrystalType.LAST_HIT;
								
								if ("detail".equalsIgnoreCase(cd.getNodeName()))
								{
									attrs = cd.getAttributes();
									
									att = attrs.getNamedItem("absorbType");
									if (att != null)
									{
										absorbType = Enum.valueOf(AbsorbCrystalType.class, att.getNodeValue());
									}
									
									att = attrs.getNamedItem("chance");
									if (att != null)
									{
										chance = Integer.parseInt(att.getNodeValue());
									}
									
									att = attrs.getNamedItem("skill");
									if (att != null)
									{
										isSkillNeeded = Boolean.parseBoolean(att.getNodeValue());
									}
									
									Node att1 = attrs.getNamedItem("maxLevel");
									Node att2 = attrs.getNamedItem("levelList");
									if ((att1 == null) && (att2 == null))
									{
										LOGGER.info(() -> "Missing maxlevel/levelList in NPC List npc Id " + npcId + " skipping!");
										continue;
									}
									chance = (int) (chance * Config.SOUL_CRYSTAL_CHANCE_MULTIPLIER);
									chance = (chance > 100) ? 100 : chance;
									LevelingSoulCrystalInfo info = new LevelingSoulCrystalInfo(absorbType, isSkillNeeded, chance);
									if (att1 != null)
									{
										int maxLevel = Integer.parseInt(att1.getNodeValue());
										for (int i = 0; i <= maxLevel; i++)
										{
											temp.put(i, info);
										}
									}
									else if (att2 != null)
									{
										StringTokenizer st = new StringTokenizer(att2.getNodeValue(), ",");
										int tokenCount = st.countTokens();
										for (int i = 0; i < tokenCount; i++)
										{
											Integer value = Integer.decode(st.nextToken().trim());
											if (value == null)
											{
												LOGGER.info("Bad Level value!! npc Id " + npcId + ", token " + i + "!");
												value = 0;
											}
											temp.put(value, info);
										}
									}
								}
							}
							
							if (temp.isEmpty())
							{
								LOGGER.info(() -> "No leveling info for npc Id " + npcId + " skipping!");
								continue;
							}
							_npcLevelingInfo.put(npcId, temp);
						}
					}
				}
			}
		}
		
		final List<SoulCrystalDisplay> _crystalDisplayAux = new ArrayList<>();
		for (Entry<Integer, Map<Integer, LevelingSoulCrystalInfo>> entry : _npcLevelingInfo.entrySet())
		{
			NpcTemplate npc = NpcData.getInstance().getTemplate(entry.getKey());
			int npcLvl = npc.getLevel();
			String name = npc.getName();
			for (Entry<Integer, LevelingSoulCrystalInfo> entry1 : entry.getValue().entrySet())
			{
				int fromLevel = entry1.getKey();
				int chance = entry1.getValue().getChance();
				AbsorbCrystalType absorb = entry1.getValue().getAbsorbCrystalType();
				_crystalDisplayAux.add(new SoulCrystalDisplay(fromLevel, chance, absorb, npcLvl, name));
			}
		}
		_crystalDisplayInfo = _crystalDisplayAux.stream().sorted(Comparator.comparing(SoulCrystalDisplay::getFromLevel).reversed().thenComparing(Comparator.comparing(SoulCrystalDisplay::getChance)).reversed()).collect(Collectors.toList());
		
		for (Entry<Integer, SoulCrystal> entry : _soulCrystalsData.entrySet())
		{
			entry.getValue().getLeveledItemId();
		}
	}
	
	public static LevelUpCrystalData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final LevelUpCrystalData INSTANCE = new LevelUpCrystalData();
	}
}
