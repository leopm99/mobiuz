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
package ai.areas.KetraOrcOutpust.KetraOrcSupport;

import java.util.HashMap;
import java.util.Map;

import com.l2journey.commons.util.StringUtil;
import com.l2journey.gameserver.data.xml.SkillData;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.skill.Skill;

import ai.AbstractNpcAI;

/**
 * Ketra Orc Support AI.<br>
 * Original Jython script by Emperorc and Kerberos.
 * @authors Nyaran
 */
public class KetraOrcSupport extends AbstractNpcAI
{
	private static class BuffsData
	{
		private final int _skill;
		private final int _cost;
		
		public BuffsData(int skill, int cost)
		{
			_skill = skill;
			_cost = cost;
		}
		
		public Skill getSkill()
		{
			return SkillData.getInstance().getSkill(_skill, 1);
		}
		
		public int getCost()
		{
			return _cost;
		}
	}
	
	// NPCs
	private static final int KADUN = 31370; // Hierarch
	private static final int WAHKAN = 31371; // Messenger
	private static final int ASEFA = 31372; // Soul Guide
	private static final int ATAN = 31373; // Grocer
	private static final int JAFF = 31374; // Warehouse Keeper
	private static final int JUMARA = 31375; // Trader
	private static final int KURFA = 31376; // Gate Keeper
	// Items
	private static final int HORN = 7186;
	private static final int[] KETRA_MARKS =
	{
		7211, // Mark of Ketra's Alliance - Level 1
		7212, // Mark of Ketra's Alliance - Level 2
		7213, // Mark of Ketra's Alliance - Level 3
		7214, // Mark of Ketra's Alliance - Level 4
		7215, // Mark of Ketra's Alliance - Level 5
	};
	// Misc
	private static final Map<Integer, BuffsData> BUFF = new HashMap<>();
	static
	{
		BUFF.put(1, new BuffsData(4359, 2)); // Focus: Requires 2 Buffalo Horns
		BUFF.put(2, new BuffsData(4360, 2)); // Death Whisper: Requires 2 Buffalo Horns
		BUFF.put(3, new BuffsData(4345, 3)); // Might: Requires 3 Buffalo Horns
		BUFF.put(4, new BuffsData(4355, 3)); // Acumen: Requires 3 Buffalo Horns
		BUFF.put(5, new BuffsData(4352, 3)); // Berserker: Requires 3 Buffalo Horns
		BUFF.put(6, new BuffsData(4354, 3)); // Vampiric Rage: Requires 3 Buffalo Horns
		BUFF.put(7, new BuffsData(4356, 6)); // Empower: Requires 6 Buffalo Horns
		BUFF.put(8, new BuffsData(4357, 6)); // Haste: Requires 6 Buffalo Horns
	}
	
	private KetraOrcSupport()
	{
		addFirstTalkId(KADUN, WAHKAN, ASEFA, ATAN, JAFF, JUMARA, KURFA);
		addTalkId(ASEFA, KURFA, JAFF);
		addStartNpc(KURFA, JAFF);
	}
	
	private int getAllianceLevel(Player player)
	{
		for (int i = 0; i < KETRA_MARKS.length; i++)
		{
			if (hasQuestItems(player, KETRA_MARKS[i]))
			{
				return i + 1;
			}
		}
		return 0;
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		if (StringUtil.isNumeric(event) && BUFF.containsKey(Integer.parseInt(event)))
		{
			final BuffsData buff = BUFF.get(Integer.parseInt(event));
			if (getQuestItemsCount(player, HORN) >= buff.getCost())
			{
				takeItems(player, HORN, buff.getCost());
				npc.setTarget(player);
				npc.doCast(buff.getSkill());
				npc.setCurrentHpMp(npc.getMaxHp(), npc.getMaxMp());
			}
			else
			{
				htmltext = "31372-02.html";
			}
		}
		else if (event.equals("Teleport"))
		{
			final int AllianceLevel = getAllianceLevel(player);
			if (AllianceLevel == 4)
			{
				htmltext = "31376-04.html";
			}
			else if (AllianceLevel == 5)
			{
				htmltext = "31376-05.html";
			}
		}
		return htmltext;
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		String htmltext = getNoQuestMsg(player);
		final int AllianceLevel = getAllianceLevel(player);
		switch (npc.getId())
		{
			case KADUN:
			{
				htmltext = (AllianceLevel > 0) ? "31370-friend.html" : "31370-no.html";
				break;
			}
			case WAHKAN:
			{
				htmltext = (AllianceLevel > 0) ? "31371-friend.html" : "31371-no.html";
				break;
			}
			case ASEFA:
			{
				if (AllianceLevel > 0)
				{
					if (AllianceLevel < 3)
					{
						htmltext = "31372-01.html";
					}
					else
					{
						htmltext = "31372-04.html";
					}
				}
				else
				{
					htmltext = "31372-03.html";
				}
				break;
			}
			case ATAN:
			{
				htmltext = (AllianceLevel > 0) ? "31373-friend.html" : "31373-no.html";
				break;
			}
			case JAFF:
			{
				if (AllianceLevel > 0)
				{
					if (AllianceLevel == 1)
					{
						htmltext = "31374-01.html";
					}
					else
					{
						htmltext = "31374-02.html";
					}
				}
				else
				{
					htmltext = "31374-no.html";
				}
				break;
			}
			case JUMARA:
			{
				switch (AllianceLevel)
				{
					case 1, 2:
					{
						htmltext = "31375-01.html";
						break;
					}
					case 3, 4:
					{
						htmltext = "31375-02.html";
						break;
					}
					case 5:
					{
						htmltext = "31375-03.html";
						break;
					}
					default:
					{
						htmltext = "31375-no.html";
						break;
					}
				}
				break;
			}
			case KURFA:
			{
				switch (AllianceLevel)
				{
					case 1, 2, 3:
					{
						htmltext = "31376-01.html";
						break;
					}
					case 4:
					{
						htmltext = "31376-02.html";
						break;
					}
					case 5:
					{
						htmltext = "31376-03.html";
						break;
					}
					default:
					{
						htmltext = "31376-no.html";
						break;
					}
				}
				break;
			}
			default:
			{
				break;
			}
		}
		return htmltext;
	}
	
	public static void main(String[] args)
	{
		new KetraOrcSupport();
	}
}