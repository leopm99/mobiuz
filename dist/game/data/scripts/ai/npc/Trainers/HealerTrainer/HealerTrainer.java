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
package ai.npc.Trainers.HealerTrainer;

import java.util.Collection;

import com.l2journey.Config;
import com.l2journey.gameserver.data.xml.SkillData;
import com.l2journey.gameserver.data.xml.SkillTreeData;
import com.l2journey.gameserver.model.SkillLearn;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.item.enums.ItemProcessType;
import com.l2journey.gameserver.model.item.holders.ItemHolder;
import com.l2journey.gameserver.model.skill.Skill;
import com.l2journey.gameserver.model.skill.enums.AcquireSkillType;
import com.l2journey.gameserver.network.SystemMessageId;
import com.l2journey.gameserver.network.serverpackets.AcquireSkillList;

import ai.AbstractNpcAI;

/**
 * Trainer healers AI.
 * @author Zoey76
 */
public class HealerTrainer extends AbstractNpcAI
{
	// NPC
	// @formatter:off
	private static final int[] HEALER_TRAINERS =
	{
		30022, 30030, 30032, 30036, 30067, 30068, 30116, 30117, 30118, 30119,
		30144, 30145, 30188, 30194, 30293, 30330, 30375, 30377, 30464, 30473,
		30476, 30680, 30701, 30720, 30721, 30858, 30859, 30860, 30861, 30864,
		30906, 30908, 30912, 31280, 31281, 31287, 31329, 31330, 31335, 31969,
		31970, 31976, 32155, 32162
	};
	// @formatter:on
	// Misc
	private static final int MIN_LEVEL = 76;
	private static final int MIN_CLASS_LEVEL = 3;
	
	private HealerTrainer()
	{
		addStartNpc(HEALER_TRAINERS);
		addTalkId(HEALER_TRAINERS);
		addFirstTalkId(HEALER_TRAINERS);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		switch (event)
		{
			case "30864.html", "30864-1.html":
			{
				htmltext = event;
				break;
			}
			case "SkillTransfer":
			{
				htmltext = "main.html";
				break;
			}
			case "SkillTransferLearn":
			{
				if (!npc.getTemplate().canTeach(player.getPlayerClass()))
				{
					htmltext = npc.getId() + "-noteach.html";
					break;
				}
				
				if ((player.getLevel() < MIN_LEVEL) || (player.getPlayerClass().level() < MIN_CLASS_LEVEL))
				{
					htmltext = "learn-lowlevel.html";
					break;
				}
				
				final AcquireSkillList asl = new AcquireSkillList(AcquireSkillType.TRANSFER);
				int count = 0;
				for (SkillLearn skillLearn : SkillTreeData.getInstance().getAvailableTransferSkills(player))
				{
					if (SkillData.getInstance().getSkill(skillLearn.getSkillId(), skillLearn.getSkillLevel()) != null)
					{
						count++;
						asl.addSkill(skillLearn.getSkillId(), skillLearn.getSkillLevel(), skillLearn.getSkillLevel(), skillLearn.getLevelUpSp(), 0);
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
				break;
			}
			case "SkillTransferCleanse":
			{
				if (!npc.getTemplate().canTeach(player.getPlayerClass()) || (player.getLevel() < MIN_LEVEL) || (player.getPlayerClass().level() < MIN_CLASS_LEVEL))
				{
					htmltext = "cleanse-no.html";
					break;
				}
				
				if (player.getAdena() < Config.FEE_DELETE_TRANSFER_SKILLS)
				{
					player.sendPacket(SystemMessageId.YOU_CANNOT_RESET_THE_SKILL_LINK_BECAUSE_THERE_IS_NOT_ENOUGH_ADENA);
					break;
				}
				
				if (hasTransferSkillItems(player))
				{
					// Come back when you have used all transfer skill items for this class.
					htmltext = "cleanse-no_skills.html";
				}
				else
				{
					boolean hasSkills = false;
					final Collection<SkillLearn> skills = SkillTreeData.getInstance().getTransferSkillTree(player.getPlayerClass()).values();
					for (SkillLearn skillLearn : skills)
					{
						final Skill skill = player.getKnownSkill(skillLearn.getSkillId());
						if (skill != null)
						{
							player.removeSkill(skill);
							for (ItemHolder item : skillLearn.getRequiredItems())
							{
								player.addItem(ItemProcessType.REFUND, item.getId(), item.getCount(), npc, true);
							}
							hasSkills = true;
						}
					}
					
					// Adena gets reduced once.
					if (hasSkills)
					{
						player.reduceAdena(ItemProcessType.FEE, Config.FEE_DELETE_TRANSFER_SKILLS, npc, true);
					}
				}
				break;
			}
		}
		return htmltext;
	}
	
	/**
	 * Verify if the player has the required item.
	 * @param player the player to verify
	 * @return {@code true} if the player has the item for the current class, {@code false} otherwise
	 */
	private static boolean hasTransferSkillItems(Player player)
	{
		int itemId;
		switch (player.getPlayerClass())
		{
			case CARDINAL:
			{
				itemId = 15307;
				break;
			}
			case EVA_SAINT:
			{
				itemId = 15308;
				break;
			}
			case SHILLIEN_SAINT:
			{
				itemId = 15309;
				break;
			}
			default:
			{
				itemId = -1;
			}
		}
		return (player.getInventory().getInventoryItemCount(itemId, -1) > 0);
	}
	
	public static void main(String[] args)
	{
		new HealerTrainer();
	}
}
