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
package custom.Validators;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.l2journey.Config;
import com.l2journey.gameserver.data.xml.ClassListData;
import com.l2journey.gameserver.managers.PunishmentManager;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.enums.player.IllegalActionPunishmentType;
import com.l2journey.gameserver.model.item.instance.Item;
import com.l2journey.gameserver.model.quest.QuestState;
import com.l2journey.gameserver.model.skill.Skill;
import com.l2journey.gameserver.util.ArrayUtil;

import ai.AbstractNpcAI;

/**
 * Sub-class skills validator.<br>
 * TODO: Rewrite.
 * @author DS
 */
public class SubClassSkills extends AbstractNpcAI
{
	// arrays must be sorted
	// @formatter:off
	private static final int[] _allCertSkillIds =
	{
		631, 632, 633, 634, 637, 638, 639, 640, 641, 642, 643, 644, 645, 646,
		647, 648, 650, 651, 652, 653, 654, 655, 656, 657, 658, 659, 660, 661,
		662, 799, 800, 801, 802, 803, 804, 1489, 1490, 1491
	};
	private static final int[][] _certSkillsByLevel =
	{
		{
			631, 632, 633, 634
		},
		{
			631, 632, 633, 634
		},
		{
			637, 638, 639, 640, 641, 642, 643, 644, 645, 646, 647, 648, 650,
			651, 652, 653, 654, 655, 799, 800, 801, 802, 803, 804, 1489, 1490,
			1491
		},
		{
			656, 657, 658, 659, 660, 661, 662
		}
	};
	
	private static final int[] _allCertItemIds =
	{
		10280, 10281, 10282, 10283, 10284, 10285, 10286, 10287, 10288, 10289,
		10290, 10291, 10292, 10293, 10294, 10612
	};
	private static final int[][] _certItemsByLevel =
	{
		{ 10280 },
		{ 10280 },
		{ 10612, 10281, 10282, 10283, 10284, 10285, 10286, 10287 },
		{ 10288, 10289, 10290, 10291, 10292, 10293, 10294 }
	};
	// @formatter:on
	
	private static final String[] VARS =
	{
		"EmergentAbility65-",
		"EmergentAbility70-",
		"ClassAbility75-",
		"ClassAbility80-"
	};
	
	private SubClassSkills()
	{
		setOnEnterWorld(true);
	}
	
	@Override
	public void onEnterWorld(Player player)
	{
		if (!Config.SKILL_CHECK_ENABLE || (player.isGM() && !Config.SKILL_CHECK_GM))
		{
			return;
		}
		
		final List<Skill> certSkills = getCertSkills(player);
		if (player.isSubClassActive())
		{
			for (Skill s : certSkills)
			{
				PunishmentManager.handleIllegalPlayerAction(player, player + " has cert skill on subclass :" + s.getName() + "(" + s.getId() + "/" + s.getLevel() + "), class:" + ClassListData.getInstance().getClass(player.getPlayerClass()).getClassName(), IllegalActionPunishmentType.NONE);
				if (Config.SKILL_CHECK_REMOVE)
				{
					player.removeSkill(s);
				}
			}
			return;
		}
		
		final int[][] cSkills = new int[certSkills.size()][2]; // skillId/skillLevel
		for (int i = certSkills.size(); --i >= 0;)
		{
			final Skill skill = certSkills.get(i);
			cSkills[i][0] = skill.getId();
			cSkills[i][1] = skill.getLevel();
		}
		
		final List<Item> certItems = getCertItems(player);
		final int[][] cItems = new int[certItems.size()][2]; // objectId/number
		for (int i = certItems.size(); --i >= 0;)
		{
			final Item item = certItems.get(i);
			cItems[i][0] = item.getObjectId();
			cItems[i][1] = (int) Math.min(item.getCount(), Integer.MAX_VALUE);
		}
		
		QuestState qs = player.getQuestState("SubClassSkills");
		if (qs == null)
		{
			qs = newQuestState(player);
		}
		
		String qName;
		String qValue;
		int id;
		int index;
		for (int i = VARS.length; --i >= 0;)
		{
			for (int j = Config.MAX_SUBCLASS; j > 0; j--)
			{
				qName = VARS[i] + j;
				qValue = player.getVariables().getString(qName, "");
				if ((qValue == null) || qValue.isEmpty())
				{
					continue;
				}
				
				if (qValue.endsWith(";")) // found skill
				{
					try
					{
						id = Integer.parseInt(qValue.replace(";", ""));
						Skill skill = null;
						if (certSkills != null)
						{
							// searching skill in test array
							if (cSkills != null)
							{
								for (index = certSkills.size(); --index >= 0;)
								{
									if (cSkills[index][0] == id)
									{
										skill = certSkills.get(index);
										cSkills[index][1]--;
										break;
									}
								}
							}
							if (skill != null)
							{
								if (!ArrayUtil.contains(_certSkillsByLevel[i], id))
								{
									// should remove this skill ?
									PunishmentManager.handleIllegalPlayerAction(player, "Invalid cert variable WITH skill:" + qName + "=" + qValue + " - skill does not match certificate level", IllegalActionPunishmentType.NONE);
								}
							}
							else
							{
								PunishmentManager.handleIllegalPlayerAction(player, "Invalid cert variable:" + qName + "=" + qValue + " - skill not found", IllegalActionPunishmentType.NONE);
							}
						}
						else
						{
							PunishmentManager.handleIllegalPlayerAction(player, "Invalid cert variable:" + qName + "=" + qValue + " - no certified skills found", IllegalActionPunishmentType.NONE);
						}
					}
					catch (NumberFormatException _)
					{
						PunishmentManager.handleIllegalPlayerAction(player, "Invalid cert variable:" + qName + "=" + qValue + " - not a number", IllegalActionPunishmentType.NONE);
					}
				}
				else
				// found item
				{
					try
					{
						id = Integer.parseInt(qValue);
						if (id == 0)
						{
							continue;
						}
						
						Item item = null;
						if (certItems != null)
						{
							// searching item in test array
							if (cItems != null)
							{
								for (index = certItems.size(); --index >= 0;)
								{
									if (cItems[index][0] == id)
									{
										item = certItems.get(index);
										cItems[index][1]--;
										break;
									}
								}
							}
							if (item != null)
							{
								if (!ArrayUtil.contains(_certItemsByLevel[i], item.getId()))
								{
									PunishmentManager.handleIllegalPlayerAction(player, "Invalid cert variable:" + qName + "=" + qValue + " - item found but does not match certificate level", IllegalActionPunishmentType.NONE);
								}
							}
							else
							{
								PunishmentManager.handleIllegalPlayerAction(player, "Invalid cert variable:" + qName + "=" + qValue + " - item not found", IllegalActionPunishmentType.NONE);
							}
						}
						else
						{
							PunishmentManager.handleIllegalPlayerAction(player, "Invalid cert variable:" + qName + "=" + qValue + " - no cert item found in inventory", IllegalActionPunishmentType.NONE);
						}
					}
					catch (NumberFormatException _)
					{
						PunishmentManager.handleIllegalPlayerAction(player, "Invalid cert variable:" + qName + "=" + qValue + " - not a number", IllegalActionPunishmentType.NONE);
					}
				}
			}
		}
		
		if ((certSkills != null) && (cSkills != null))
		{
			for (int i = cSkills.length; --i >= 0;)
			{
				if (cSkills[i][1] == 0)
				{
					continue;
				}
				
				final Skill skill = certSkills.get(i);
				if (cSkills[i][1] > 0)
				{
					if (cSkills[i][1] == skill.getLevel())
					{
						PunishmentManager.handleIllegalPlayerAction(player, player + " has invalid cert skill :" + skill.getName() + "(" + skill.getId() + "/" + skill.getLevel() + ")", IllegalActionPunishmentType.NONE);
					}
					else
					{
						PunishmentManager.handleIllegalPlayerAction(player, player + " has invalid cert skill :" + skill.getName() + "(" + skill.getId() + "/" + skill.getLevel() + "), level too high", IllegalActionPunishmentType.NONE);
					}
					
					if (Config.SKILL_CHECK_REMOVE)
					{
						player.removeSkill(skill);
					}
				}
				else
				{
					PunishmentManager.handleIllegalPlayerAction(player, "Invalid cert skill :" + skill.getName() + "(" + skill.getId() + "/" + skill.getLevel() + "), level too low", IllegalActionPunishmentType.NONE);
				}
			}
		}
		
		if ((certItems != null) && (cItems != null))
		{
			for (int i = cItems.length; --i >= 0;)
			{
				if (cItems[i][1] == 0)
				{
					continue;
				}
				
				final Item item = certItems.get(i);
				PunishmentManager.handleIllegalPlayerAction(player, "Invalid cert item without variable or with wrong count:" + item.getObjectId(), IllegalActionPunishmentType.NONE);
			}
		}
	}
	
	private List<Skill> getCertSkills(Player player)
	{
		final List<Skill> tmp = new ArrayList<>();
		for (Skill s : player.getAllSkills())
		{
			if ((s != null) && (Arrays.binarySearch(_allCertSkillIds, s.getId()) >= 0))
			{
				tmp.add(s);
			}
		}
		return tmp;
	}
	
	private List<Item> getCertItems(Player player)
	{
		final List<Item> tmp = new ArrayList<>();
		for (Item i : player.getInventory().getItems())
		{
			if ((i != null) && (Arrays.binarySearch(_allCertItemIds, i.getId()) >= 0))
			{
				tmp.add(i);
			}
		}
		return tmp;
	}
	
	public static void main(String[] args)
	{
		new SubClassSkills();
	}
}