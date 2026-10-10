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
package ai.others.NpcBuffers;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.l2journey.commons.util.IXmlReader;
import com.l2journey.gameserver.model.StatSet;

/**
 * @author UnAfraid
 */
public class NpcBuffersData implements IXmlReader
{
	private final Map<Integer, NpcBufferData> _npcBuffers = new HashMap<>();
	
	protected NpcBuffersData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		parseDatapackFile("data/scripts/ai/others/NpcBuffers/NpcBuffersData.xml");
		LOGGER.log(Level.INFO, getClass().getSimpleName() + ": Loaded " + _npcBuffers.size() + " buffers data.");
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		StatSet set;
		Node attr;
		NamedNodeMap attrs;
		for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("npc".equalsIgnoreCase(d.getNodeName()))
					{
						attrs = d.getAttributes();
						final int npcId = parseInteger(attrs, "id");
						final NpcBufferData npc = new NpcBufferData(npcId);
						for (Node c = d.getFirstChild(); c != null; c = c.getNextSibling())
						{
							switch (c.getNodeName())
							{
								case "skill":
								{
									attrs = c.getAttributes();
									set = new StatSet();
									for (int i = 0; i < attrs.getLength(); i++)
									{
										attr = attrs.item(i);
										set.set(attr.getNodeName(), attr.getNodeValue());
									}
									npc.addSkill(new NpcBufferSkillData(set));
									break;
								}
								default:
								{
									break;
								}
							}
						}
						_npcBuffers.put(npcId, npc);
					}
				}
			}
		}
	}
	
	public NpcBufferData getNpcBuffer(int npcId)
	{
		return _npcBuffers.get(npcId);
	}
	
	public Collection<NpcBufferData> getNpcBuffers()
	{
		return _npcBuffers.values();
	}
	
	public Set<Integer> getNpcBufferIds()
	{
		return _npcBuffers.keySet();
	}
}
