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
package com.l2journey.gameserver.network.clientpackets;

import com.l2journey.gameserver.data.xml.SkillData;
import com.l2journey.gameserver.data.xml.SkillTreeData;
import com.l2journey.gameserver.model.SkillLearn;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.clan.ClanAccess;
import com.l2journey.gameserver.model.skill.Skill;
import com.l2journey.gameserver.model.skill.enums.AcquireSkillType;
import com.l2journey.gameserver.network.PacketLogger;
import com.l2journey.gameserver.network.serverpackets.AcquireSkillInfo;

/**
 * Request Acquire Skill Info client packet implementation.
 * @author Zoey76
 */
public class RequestAcquireSkillInfo extends ClientPacket
{
	private int _id;
	private int _level;
	private AcquireSkillType _skillType;
	
	@Override
	protected void readImpl()
	{
		_id = readInt();
		_level = readInt();
		_skillType = AcquireSkillType.getAcquireSkillType(readInt());
	}
	
	@Override
	protected void runImpl()
	{
		if ((_id <= 0) || (_level <= 0))
		{
			PacketLogger.warning(RequestAcquireSkillInfo.class.getSimpleName() + ": Invalid Id: " + _id + " or level: " + _level + "!");
			return;
		}
		
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		final Npc trainer = player.getLastFolkNPC();
		if ((trainer == null) || !trainer.isNpc() || (!trainer.canInteract(player) && !player.isGM()))
		{
			return;
		}
		
		final Skill skill = SkillData.getInstance().getSkill(_id, _level);
		if (skill == null)
		{
			PacketLogger.warning(RequestAcquireSkillInfo.class.getSimpleName() + ": Skill Id: " + _id + " level: " + _level + " is undefined. " + RequestAcquireSkillInfo.class.getName() + " failed.");
			return;
		}
		
		final SkillLearn s = SkillTreeData.getInstance().getSkillLearn(_skillType, _id, _level, player);
		if (s == null)
		{
			return;
		}
		
		switch (_skillType)
		{
			case TRANSFORM, FISHING, SUBCLASS, COLLECT, TRANSFER:
			{
				player.sendPacket(new AcquireSkillInfo(_skillType, s));
				break;
			}
			case CLASS:
			{
				if (trainer.getTemplate().canTeach(player.getLearningClass()))
				{
					final int customSp = s.getCalculatedLevelUpSp(player.getPlayerClass(), player.getLearningClass());
					player.sendPacket(new AcquireSkillInfo(_skillType, s, customSp));
				}
				break;
			}
			case PLEDGE:
			{
				if (!player.isClanLeader())
				{
					return;
				}
				player.sendPacket(new AcquireSkillInfo(_skillType, s));
				break;
			}
			case SUBPLEDGE:
			{
				if (!player.isClanLeader() || !player.hasAccess(ClanAccess.MEMBER_FAME))
				{
					return;
				}
				player.sendPacket(new AcquireSkillInfo(_skillType, s));
				break;
			}
		}
	}
}
