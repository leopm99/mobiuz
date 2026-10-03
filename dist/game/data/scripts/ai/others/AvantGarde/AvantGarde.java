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
package ai.others.AvantGarde;

import java.util.List;

import com.l2journey.Config;
import com.l2journey.commons.util.StringUtil;
import com.l2journey.gameserver.data.xml.MultisellData;
import com.l2journey.gameserver.data.xml.SkillData;
import com.l2journey.gameserver.data.xml.SkillTreeData;
import com.l2journey.gameserver.managers.QuestManager;
import com.l2journey.gameserver.model.SkillLearn;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.item.enums.ItemProcessType;
import com.l2journey.gameserver.model.item.instance.Item;
import com.l2journey.gameserver.model.quest.QuestState;
import com.l2journey.gameserver.model.skill.Skill;
import com.l2journey.gameserver.model.skill.enums.AcquireSkillType;
import com.l2journey.gameserver.network.SystemMessageId;
import com.l2journey.gameserver.network.clientpackets.RequestAcquireSkill;
import com.l2journey.gameserver.network.serverpackets.AcquireSkillList;
import com.l2journey.gameserver.network.serverpackets.SystemMessage;

import ai.AbstractNpcAI;
import custom.Validators.SubClassSkills;

/**
 * Avant-Garde AI.<br>
 * Sub-Class Certification system, skill learning and certification canceling.<br>
 * Transformation skill learning and transformation scroll sell.
 * @author Zoey76
 */
public class AvantGarde extends AbstractNpcAI
{
	// NPC
	private static final int AVANT_GARDE = 32323;
	// Items
	// @formatter:off
	private static final int[] ITEMS =
	{
		10280, 10281, 10282, 10283, 10284, 10285, 10286, 10287, 10288, 10289, 10290, 10291, 10292, 10293, 10294, 10612
	};
	// @formatter:on
	// Misc
	private static final String[] QUEST_VAR_NAMES =
	{
		"EmergentAbility65-",
		"EmergentAbility70-",
		"ClassAbility75-",
		"ClassAbility80-"
	};
	
	public AvantGarde()
	{
		addStartNpc(AVANT_GARDE);
		addTalkId(AVANT_GARDE);
		addFirstTalkId(AVANT_GARDE);
		addAcquireSkillId(AVANT_GARDE);
	}
	
	@Override
	public void onAcquireSkill(Npc npc, Player player, Skill skill, AcquireSkillType type)
	{
		switch (type)
		{
			case TRANSFORM:
			{
				showTransformSkillList(player);
				break;
			}
			case SUBCLASS:
			{
				showSubClassSkillList(player);
				break;
			}
		}
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		switch (event)
		{
			case "32323-02.html", "32323-02a.html", "32323-02b.html", "32323-02c.html", "32323-05.html", "32323-05a.html", "32323-05no.html", "32323-06.html", "32323-06no.html":
			{
				htmltext = event;
				break;
			}
			case "LearnTransformationSkill":
			{
				if (RequestAcquireSkill.canTransform(player))
				{
					showTransformSkillList(player);
				}
				else
				{
					htmltext = "32323-03.html";
				}
				break;
			}
			case "BuyTransformationItems":
			{
				if (RequestAcquireSkill.canTransform(player))
				{
					MultisellData.getInstance().separateAndSend(32323001, player, npc, false);
				}
				else
				{
					htmltext = "32323-04.html";
				}
				break;
			}
			case "LearnSubClassSkill":
			{
				if (!RequestAcquireSkill.canTransform(player))
				{
					htmltext = "32323-04.html";
				}
				if (player.isSubClassActive())
				{
					htmltext = "32323-08.html";
				}
				else
				{
					boolean hasItems = false;
					for (int id : ITEMS)
					{
						if (player.getInventory().getItemByItemId(id) != null)
						{
							hasItems = true;
							break;
						}
					}
					if (hasItems)
					{
						showSubClassSkillList(player);
					}
					else
					{
						htmltext = "32323-08.html";
					}
				}
				break;
			}
			case "CancelCertification":
			{
				if (player.getSubClasses().isEmpty())
				{
					htmltext = "32323-07.html";
				}
				else if (player.isSubClassActive())
				{
					htmltext = "32323-08.html";
				}
				else if (player.getAdena() < Config.FEE_DELETE_SUBCLASS_SKILLS)
				{
					htmltext = "32323-08no.html";
				}
				else
				{
					QuestState qs = player.getQuestState(SubClassSkills.class.getSimpleName());
					if (qs == null)
					{
						qs = QuestManager.getInstance().getQuest(SubClassSkills.class.getSimpleName()).newQuestState(player);
					}
					
					int activeCertifications = 0;
					for (String varName : QUEST_VAR_NAMES)
					{
						for (int i = 1; i <= Config.MAX_SUBCLASS; i++)
						{
							final String qvar = player.getVariables().getString(varName + i, "0");
							if (!qvar.isEmpty() && (qvar.endsWith(";") || !qvar.equals("0")))
							{
								activeCertifications++;
							}
						}
					}
					if (activeCertifications == 0)
					{
						htmltext = "32323-10no.html";
					}
					else
					{
						for (String varName : QUEST_VAR_NAMES)
						{
							for (int i = 1; i <= Config.MAX_SUBCLASS; i++)
							{
								final String qvarName = varName + i;
								final String qvar = player.getVariables().getString(qvarName, "0");
								if (qvar.endsWith(";"))
								{
									final String skillIdVar = qvar.replace(";", "");
									if (StringUtil.isNumeric(skillIdVar))
									{
										final int skillId = Integer.parseInt(skillIdVar);
										final Skill sk = SkillData.getInstance().getSkill(skillId, 1);
										if (sk != null)
										{
											player.removeSkill(sk);
											player.getVariables().set(qvarName, "0");
										}
									}
									else
									{
										LOGGER.warning("Invalid Sub-Class Skill Id: " + skillIdVar + " for " + player + "!");
									}
								}
								else if (!qvar.isEmpty() && !qvar.equals("0"))
								{
									if (StringUtil.isNumeric(qvar))
									{
										final int itemObjId = Integer.parseInt(qvar);
										Item itemInstance = player.getInventory().getItemByObjectId(itemObjId);
										if (itemInstance != null)
										{
											player.destroyItem(ItemProcessType.DESTROY, itemObjId, 1, player, false);
										}
										else
										{
											itemInstance = player.getWarehouse().getItemByObjectId(itemObjId);
											if (itemInstance != null)
											{
												LOGGER.warning("Somehow " + player.getName() + " put a certification book into warehouse!");
												player.getWarehouse().destroyItem(ItemProcessType.DESTROY, itemInstance, 1, player, false);
											}
											else
											{
												LOGGER.warning("Somehow " + player.getName() + " deleted a certification book!");
											}
										}
										player.getVariables().set(qvarName, "0");
									}
									else
									{
										LOGGER.warning("Invalid item object Id: " + qvar + " for " + player + "!");
									}
								}
							}
						}
						
						player.reduceAdena(ItemProcessType.FEE, Config.FEE_DELETE_SUBCLASS_SKILLS, npc, true);
						htmltext = "32323-09no.html";
						player.sendSkillList();
					}
					
					// Let's consume all certification books, even those not present in database.
					for (int itemId : ITEMS)
					{
						final Item item = player.getInventory().getItemByItemId(itemId);
						if (item != null)
						{
							LOGGER.warning(getClass().getName() + ": " + player + " had 'extra' certification skill books while cancelling sub-class certifications!");
							player.destroyItem(ItemProcessType.DESTROY, item, npc, false);
						}
					}
				}
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return "32323-01.html";
	}
	
	@Override
	public String onTalk(Npc npc, Player talker)
	{
		return "32323-01.html";
	}
	
	/**
	 * Display the Sub-Class Skill list to the player.
	 * @param player the player
	 */
	public static void showSubClassSkillList(Player player)
	{
		final List<SkillLearn> subClassSkills = SkillTreeData.getInstance().getAvailableSubClassSkills(player);
		final AcquireSkillList asl = new AcquireSkillList(AcquireSkillType.SUBCLASS);
		int count = 0;
		
		for (SkillLearn s : subClassSkills)
		{
			if (SkillData.getInstance().getSkill(s.getSkillId(), s.getSkillLevel()) != null)
			{
				count++;
				asl.addSkill(s.getSkillId(), s.getSkillLevel(), s.getSkillLevel(), 0, 0);
			}
		}
		if (count > 0)
		{
			player.sendPacket(asl);
		}
		else
		{
			player.sendPacket(SystemMessageId.THERE_ARE_NO_OTHER_SKILLS_TO_LEARN);
		}
	}
	
	/**
	 * This displays Transformation Skill List to the player.
	 * @param player the active character.
	 */
	public static void showTransformSkillList(Player player)
	{
		final List<SkillLearn> skills = SkillTreeData.getInstance().getAvailableTransformSkills(player);
		final AcquireSkillList asl = new AcquireSkillList(AcquireSkillType.TRANSFORM);
		int counts = 0;
		
		for (SkillLearn s : skills)
		{
			if (SkillData.getInstance().getSkill(s.getSkillId(), s.getSkillLevel()) != null)
			{
				counts++;
				asl.addSkill(s.getSkillId(), s.getSkillLevel(), s.getSkillLevel(), s.getLevelUpSp(), 0);
			}
		}
		
		if (counts == 0)
		{
			final int minlevel = SkillTreeData.getInstance().getMinLevelForNewSkill(player, SkillTreeData.getInstance().getTransformSkillTree());
			if (minlevel > 0)
			{
				// No more skills to learn, come back when you level.
				final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_DO_NOT_HAVE_ANY_FURTHER_SKILLS_TO_LEARN_COME_BACK_WHEN_YOU_HAVE_REACHED_LEVEL_S1);
				sm.addInt(minlevel);
				player.sendPacket(sm);
			}
			else
			{
				player.sendPacket(SystemMessageId.THERE_ARE_NO_OTHER_SKILLS_TO_LEARN);
			}
		}
		else
		{
			player.sendPacket(asl);
		}
	}
	
	public static void main(String[] args)
	{
		new AvantGarde();
	}
}
