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
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.l2journey.commons.util.IXmlReader;
import com.l2journey.gameserver.model.options.OptionSkillHolder;
import com.l2journey.gameserver.model.options.OptionSkillType;
import com.l2journey.gameserver.model.options.Options;
import com.l2journey.gameserver.model.skill.Skill;
import com.l2journey.gameserver.model.stats.Stat;
import com.l2journey.gameserver.model.stats.functions.FuncTemplate;

/**
 * Item Option data.
 * @author UnAfraid
 */
public class OptionData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(OptionData.class.getName());
	
	private static Options[] options;
	private static final Map<Integer, Options> OPTION_MAP = new ConcurrentHashMap<>();
	
	protected OptionData()
	{
		load();
	}
	
	@Override
	public synchronized void load()
	{
		parseDatapackDirectory("data/stats/augmentation/options", false);
		
		options = new Options[Collections.max(OPTION_MAP.keySet()) + 1];
		for (Entry<Integer, Options> option : OPTION_MAP.entrySet())
		{
			options[option.getKey()] = option.getValue();
		}
		
		LOGGER.info(() -> getClass().getSimpleName() + ": Loaded " + OPTION_MAP.size() + " options.");
		OPTION_MAP.clear();
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("option".equalsIgnoreCase(d.getNodeName()))
					{
						final int id = parseInteger(d.getAttributes(), "id");
						final Options option = new Options(id);
						for (Node cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling())
						{
							switch (cd.getNodeName())
							{
								case "stats":
								{
									for (Node fd = cd.getFirstChild(); fd != null; fd = fd.getNextSibling())
									{
										switch (fd.getNodeName())
										{
											case "add", "sub", "mul", "div", "set", "share", "enchant", "enchanthp":
											{
												parseFuncs(fd.getAttributes(), fd.getNodeName(), option);
											}
											default:
											{
												break;
											}
										}
									}
									break;
								}
								case "active_skill":
								{
									final int skillId = parseInteger(cd.getAttributes(), "id");
									final int skillLevel = parseInteger(cd.getAttributes(), "level");
									final Skill skill = SkillData.getInstance().getSkill(skillId, skillLevel);
									if (skill != null)
									{
										option.setActiveSkill(skill);
									}
									else
									{
										LOGGER.info(() -> getClass().getSimpleName() + ": Could not find skill " + skillId + "(" + skillLevel + ") used by option " + id + ".");
									}
									break;
								}
								case "passive_skill":
								{
									final int skillId = parseInteger(cd.getAttributes(), "id");
									final int skillLevel = parseInteger(cd.getAttributes(), "level");
									final Skill skill = SkillData.getInstance().getSkill(skillId, skillLevel);
									if (skill != null)
									{
										option.setPassiveSkill(skill);
									}
									else
									{
										LOGGER.info(() -> getClass().getSimpleName() + ": Could not find skill " + skillId + "(" + skillLevel + ") used by option " + id + ".");
									}
									break;
								}
								case "attack_skill":
								{
									final int skillId = parseInteger(cd.getAttributes(), "id");
									final int skillLevel = parseInteger(cd.getAttributes(), "level");
									final Skill skill = SkillData.getInstance().getSkill(skillId, skillLevel);
									if (skill != null)
									{
										option.addActivationSkill(new OptionSkillHolder(skill, parseDouble(cd.getAttributes(), "chance"), OptionSkillType.ATTACK));
									}
									else
									{
										LOGGER.info(() -> getClass().getSimpleName() + ": Could not find skill " + skillId + "(" + skillLevel + ") used by option " + id + ".");
									}
									break;
								}
								case "magic_skill":
								{
									final int skillId = parseInteger(cd.getAttributes(), "id");
									final int skillLevel = parseInteger(cd.getAttributes(), "level");
									final Skill skill = SkillData.getInstance().getSkill(skillId, skillLevel);
									if (skill != null)
									{
										option.addActivationSkill(new OptionSkillHolder(skill, parseDouble(cd.getAttributes(), "chance"), OptionSkillType.MAGIC));
									}
									else
									{
										LOGGER.info(() -> getClass().getSimpleName() + ": Could not find skill " + skillId + "(" + skillLevel + ") used by option " + id + ".");
									}
									break;
								}
								case "critical_skill":
								{
									final int skillId = parseInteger(cd.getAttributes(), "id");
									final int skillLevel = parseInteger(cd.getAttributes(), "level");
									final Skill skill = SkillData.getInstance().getSkill(skillId, skillLevel);
									if (skill != null)
									{
										option.addActivationSkill(new OptionSkillHolder(skill, parseDouble(cd.getAttributes(), "chance"), OptionSkillType.CRITICAL));
									}
									else
									{
										LOGGER.info(() -> getClass().getSimpleName() + ": Could not find skill " + skillId + "(" + skillLevel + ") used by option " + id + ".");
									}
									break;
								}
								default:
								{
									break;
								}
							}
						}
						OPTION_MAP.put(option.getId(), option);
					}
				}
			}
		}
	}
	
	private void parseFuncs(NamedNodeMap attrs, String functionName, Options op)
	{
		final Stat stat = Stat.valueOfXml(parseString(attrs, "stat"));
		final double val = parseDouble(attrs, "val");
		int order = -1;
		final Node orderNode = attrs.getNamedItem("order");
		if (orderNode != null)
		{
			order = Integer.parseInt(orderNode.getNodeValue());
		}
		op.addFunc(new FuncTemplate(null, null, functionName, order, stat, val));
	}
	
	public Options getOptions(int id)
	{
		if (options.length > id)
		{
			return options[id];
		}
		return null;
	}
	
	/**
	 * Gets the single instance of OptionsData.
	 * @return single instance of OptionsData
	 */
	public static OptionData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final OptionData INSTANCE = new OptionData();
	}
}
