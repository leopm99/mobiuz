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

import java.util.logging.Level;
import java.util.logging.Logger;

import com.l2journey.Config;
import com.l2journey.gameserver.data.xml.EnchantSkillGroupsData;
import com.l2journey.gameserver.data.xml.SkillData;
import com.l2journey.gameserver.model.EnchantSkillGroup.EnchantSkillHolder;
import com.l2journey.gameserver.model.EnchantSkillLearn;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.item.enums.ItemProcessType;
import com.l2journey.gameserver.model.item.instance.Item;
import com.l2journey.gameserver.model.itemcontainer.Inventory;
import com.l2journey.gameserver.model.skill.Skill;
import com.l2journey.gameserver.network.SystemMessageId;
import com.l2journey.gameserver.network.serverpackets.ExEnchantSkillInfo;
import com.l2journey.gameserver.network.serverpackets.ExEnchantSkillInfoDetail;
import com.l2journey.gameserver.network.serverpackets.ExEnchantSkillResult;
import com.l2journey.gameserver.network.serverpackets.SystemMessage;

/**
 * Format (ch) dd c: (id) 0xD0 h: (subid) 0x33 d: skill id d: skill level
 * @author -Wooden-
 */
public class RequestExEnchantSkillUntrain extends ClientPacket
{
	private static final Logger LOGGER_ENCHANT = Logger.getLogger("enchant.skills");
	
	private int _skillId;
	private int _skillLevel;
	
	@Override
	protected void readImpl()
	{
		_skillId = readInt();
		_skillLevel = readInt();
	}
	
	@Override
	protected void runImpl()
	{
		if ((_skillId <= 0) || (_skillLevel <= 0))
		{
			return;
		}
		
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		if (player.getPlayerClass().level() < 3) // requires to have 3rd class quest completed
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_USE_THE_SKILL_ENHANCING_FUNCTION_IN_THIS_CLASS_YOU_CAN_USE_CORRESPONDING_FUNCTION_WHEN_COMPLETING_THE_THIRD_CLASS_CHANGE);
			return;
		}
		
		if (player.getLevel() < 76)
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_USE_THE_SKILL_ENHANCING_FUNCTION_ON_THIS_LEVEL_YOU_CAN_USE_THE_CORRESPONDING_FUNCTION_ON_LEVELS_HIGHER_THAN_76LV);
			return;
		}
		
		if (!player.isAllowedToEnchantSkills())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_USE_THE_SKILL_ENHANCING_FUNCTION_IN_THIS_CLASS_YOU_CAN_USE_THE_SKILL_ENHANCING_FUNCTION_UNDER_OFF_BATTLE_STATUS_AND_CANNOT_USE_THE_FUNCTION_WHILE_TRANSFORMING_BATTLING_AND_ON_BOARD);
			return;
		}
		
		final EnchantSkillLearn s = EnchantSkillGroupsData.getInstance().getSkillEnchantmentBySkillId(_skillId);
		if (s == null)
		{
			return;
		}
		
		if ((_skillLevel % 100) == 0)
		{
			_skillLevel = s.getBaseLevel();
		}
		
		final Skill skill = SkillData.getInstance().getSkill(_skillId, _skillLevel);
		if (skill == null)
		{
			return;
		}
		
		final int reqItemId = EnchantSkillGroupsData.UNTRAIN_ENCHANT_BOOK;
		final int beforeUntrainSkillLevel = player.getSkillLevel(_skillId);
		if (((beforeUntrainSkillLevel - 1) != _skillLevel) && (((beforeUntrainSkillLevel % 100) != 1) || (_skillLevel != s.getBaseLevel())))
		{
			return;
		}
		
		final EnchantSkillHolder esd = s.getEnchantSkillHolder(beforeUntrainSkillLevel);
		final int requiredSp = esd.getSpCost();
		final int requireditems = esd.getAdenaCost();
		final Item spb = player.getInventory().getItemByItemId(reqItemId);
		// does not have spellbook
		if ((Config.ES_SP_BOOK_NEEDED && (spb == null)) || (player.getInventory().getAdena() < requireditems))
		{
			player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ALL_OF_THE_ITEMS_NEEDED_TO_ENCHANT_THAT_SKILL);
			return;
		}
		
		boolean check = true;
		if (Config.ES_SP_BOOK_NEEDED)
		{
			check &= player.destroyItem(ItemProcessType.NONE, spb.getObjectId(), 1, player, true);
		}
		
		check &= player.destroyItemByItemId(null, Inventory.ADENA_ID, requireditems, player, true);
		if (!check)
		{
			player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ALL_OF_THE_ITEMS_NEEDED_TO_ENCHANT_THAT_SKILL);
			return;
		}
		
		player.getStat().addSp((int) (requiredSp * 0.8));
		if (Config.LOG_SKILL_ENCHANTS)
		{
			final Skill loggedSkill = skill;
			final Item spbItem = spb;
			LOGGER_ENCHANT.log(Level.INFO, () -> "Untrain:" + player.getName() + " [" + player.getObjectId() + "] Account:" + player.getAccountName() + " IP:" + player.getIPAddress() + ", Skill:" + loggedSkill + ", SPB:" + spbItem);
		}
		
		player.addSkill(skill, true);
		player.sendPacket(ExEnchantSkillResult.valueOf(true));
		
		player.updateUserInfo();
		if (_skillLevel > 100)
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.UNTRAIN_OF_ENCHANT_SKILL_WAS_SUCCESSFUL_CURRENT_LEVEL_OF_ENCHANT_SKILL_S1_HAS_BEEN_DECREASED_BY_1);
			sm.addSkillName(_skillId);
			player.sendPacket(sm);
		}
		else
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.UNTRAIN_OF_ENCHANT_SKILL_WAS_SUCCESSFUL_CURRENT_LEVEL_OF_ENCHANT_SKILL_S1_BECAME_0_AND_ENCHANT_SKILL_WILL_BE_INITIALIZED);
			sm.addSkillName(_skillId);
			player.sendPacket(sm);
		}
		player.sendSkillList();
		final int afterUntrainSkillLevel = player.getSkillLevel(_skillId);
		player.sendPacket(new ExEnchantSkillInfo(_skillId, afterUntrainSkillLevel));
		player.sendPacket(new ExEnchantSkillInfoDetail(2, _skillId, afterUntrainSkillLevel - 1, player));
		player.updateShortcuts(_skillId, afterUntrainSkillLevel);
	}
}
