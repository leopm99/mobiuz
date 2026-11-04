/*
 * Copyright (c) 2013 L2jMobius
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.l2journey.gameserver.model.actor.instance;

import com.l2journey.RebirthEngineConfigs;

import java.util.List;
import java.util.Map;

import com.l2journey.gameserver.data.xml.SkillData;
import com.l2journey.gameserver.data.xml.SkillTreeData;
import com.l2journey.gameserver.model.SkillLearn;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.enums.creature.InstanceType;
import com.l2journey.gameserver.model.actor.enums.player.PlayerClass;
import com.l2journey.gameserver.model.actor.status.FolkStatus;
import com.l2journey.gameserver.model.actor.templates.NpcTemplate;
import com.l2journey.gameserver.model.skill.Skill;
import com.l2journey.gameserver.model.skill.enums.AcquireSkillType;
import com.l2journey.gameserver.network.SystemMessageId;
import com.l2journey.gameserver.network.serverpackets.AcquireSkillList;
import com.l2journey.gameserver.network.serverpackets.NpcHtmlMessage;
import com.l2journey.gameserver.network.serverpackets.SystemMessage;
import com.l2journey.gameserver.rebirth.RebirthManager;

public class Folk extends Npc
{
	/**
	 * Creates a NPC.
	 * @param template the NPC template
	 */
	public Folk(NpcTemplate template)
	{
		super(template);
		setInstanceType(InstanceType.Folk);
		setInvul(false);
	}
	
	@Override
	public FolkStatus getStatus()
	{
		return (FolkStatus) super.getStatus();
	}
	
	@Override
	public void initCharStatus()
	{
		setStatus(new FolkStatus(this));
	}
	
	public List<PlayerClass> getClassesToTeach()
	{
		return getTemplate().getTeachInfo();
	}
	
	/**
	 * Displays Skill Tree for a given player, npc and class Id.
	 * @param player the active character.
	 * @param npc the last folk.
	 * @param classId player's active class id.
	 */
	public static void showSkillList(Player player, Npc npc, PlayerClass classId)
	{
		final int npcId = npc.getTemplate().getId();
		if (npcId == RebirthEngineConfigs.REBIRTH_SKILL_SELLER_ID)
		{
			showRebirthSkillList(player, npc);
			return;
		}
		if (npcId == 32611) // Tolonis (Officer)
		{
			final List<SkillLearn> skills = SkillTreeData.getInstance().getAvailableCollectSkills(player);
			final AcquireSkillList asl = new AcquireSkillList(AcquireSkillType.COLLECT);
			int counts = 0;
			for (SkillLearn s : skills)
			{
				final Skill sk = SkillData.getInstance().getSkill(s.getSkillId(), s.getSkillLevel());
				if (sk != null)
				{
					counts++;
					asl.addSkill(s.getSkillId(), s.getSkillLevel(), s.getSkillLevel(), 0, 1);
				}
			}
			
			if (counts == 0) // No more skills to learn, come back when you level.
			{
				final int minLevel = SkillTreeData.getInstance().getMinLevelForNewSkill(player, SkillTreeData.getInstance().getCollectSkillTree());
				if (minLevel > 0)
				{
					final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_DO_NOT_HAVE_ANY_FURTHER_SKILLS_TO_LEARN_COME_BACK_WHEN_YOU_HAVE_REACHED_LEVEL_S1);
					sm.addInt(minLevel);
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
			return;
		}
		
		if (!npc.getTemplate().canTeach(classId))
		{
			npc.showNoTeachHtml(player);
			return;
		}
		
		if (((Folk) npc).getClassesToTeach().isEmpty())
		{
			final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
			final String sb = "<html><body>I cannot teach you. My class list is empty.<br>Ask admin to fix it. Need add my npcid and classes to skill_learn.sql.<br>NpcId:" + npcId + ", Your classId:" + player.getPlayerClass().getId() + "</body></html>";
			html.setHtml(sb);
			player.sendPacket(html);
			return;
		}
		
		// Normal skills, No LearnedByFS, no AutoGet skills.
		final List<SkillLearn> skills = SkillTreeData.getInstance().getAvailableSkills(player, classId, false, false);
		final AcquireSkillList asl = new AcquireSkillList(AcquireSkillType.CLASS);
		int count = 0;
		player.setLearningClass(classId);
		for (SkillLearn s : skills)
		{
			if (SkillData.getInstance().getSkill(s.getSkillId(), s.getSkillLevel()) != null)
			{
				asl.addSkill(s.getSkillId(), s.getSkillLevel(), s.getSkillLevel(), s.getCalculatedLevelUpSp(player.getPlayerClass(), classId), 0);
				count++;
			}
		}
		
		if (count == 0)
		{
			final Map<Integer, SkillLearn> skillTree = SkillTreeData.getInstance().getCompleteClassSkillTree(classId);
			final int minLevel = SkillTreeData.getInstance().getMinLevelForNewSkill(player, skillTree);
			if (minLevel > 0)
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_DO_NOT_HAVE_ANY_FURTHER_SKILLS_TO_LEARN_COME_BACK_WHEN_YOU_HAVE_REACHED_LEVEL_S1);
				sm.addInt(minLevel);
				player.sendPacket(sm);
			}
			else
			{
				if (player.getPlayerClass().level() == 1)
				{
					final SystemMessage sm = new SystemMessage(SystemMessageId.THERE_ARE_NO_OTHER_SKILLS_TO_LEARN_PLEASE_COME_BACK_AFTER_S1ND_CLASS_CHANGE);
					sm.addInt(2);
					player.sendPacket(sm);
				}
				else
				{
					player.sendPacket(SystemMessageId.THERE_ARE_NO_OTHER_SKILLS_TO_LEARN);
				}
			}
		}
		else
		{
			player.sendPacket(asl);
		}
	}

	public static void showRebirthSkillList(Player player, Npc npc)
	{
		if (!RebirthEngineConfigs.REBIRTH_ENABLED || npc.getId() != RebirthEngineConfigs.REBIRTH_SKILL_SELLER_ID || !npc.canInteract(player) || player.isSubClassActive())
		{
			player.sendMessage("Rebirth skills are available from the configured trainer on your main class.");
			return;
		}
		if (!RebirthManager.getInstance().canLearnRebirthSkills(player))
		{
			player.sendMessage("Finish your pending rebirth operation before learning skills.");
			return;
		}
		player.setLastFolkNPC(npc);
		player.setLearningClass(player.getPlayerClass());
		final AcquireSkillList list = new AcquireSkillList(AcquireSkillType.CLASS);
		int count = 0;
		for (SkillLearn learn : SkillTreeData.getInstance().getAvailableRebirthSkills(player))
		{
			if (SkillData.getInstance().getSkill(learn.getSkillId(), learn.getSkillLevel()) != null)
			{
				list.addSkill(learn.getSkillId(), learn.getSkillLevel(), learn.getSkillLevel(), 0, 1);
				count++;
			}
		}
		if (count == 0) { player.sendPacket(SystemMessageId.THERE_ARE_NO_OTHER_SKILLS_TO_LEARN); }
		else { player.sendPacket(list); }
	}
}
