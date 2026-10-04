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
package com.l2journey.gameserver.model;

import java.util.ArrayList;
import java.util.List;

import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.item.instance.Item;
import com.l2journey.gameserver.model.itemcontainer.Inventory;
import com.l2journey.gameserver.model.skill.holders.SkillHolder;

/**
 * @author Luno
 */
public class ArmorSet
{
	private int _chestId;
	private final List<Integer> _legs;
	private final List<Integer> _head;
	private final List<Integer> _gloves;
	private final List<Integer> _feet;
	private final List<Integer> _shield;
	
	private final List<SkillHolder> _skills;
	private final List<SkillHolder> _shieldSkills;
	private final List<SkillHolder> _enchant6Skill;
	
	private int _con;
	private int _dex;
	private int _str;
	private int _men;
	private int _wit;
	private int _int;
	
	public ArmorSet()
	{
		_legs = new ArrayList<>();
		_head = new ArrayList<>();
		_gloves = new ArrayList<>();
		_feet = new ArrayList<>();
		_shield = new ArrayList<>();
		_skills = new ArrayList<>();
		_shieldSkills = new ArrayList<>();
		_enchant6Skill = new ArrayList<>();
	}
	
	public void addChest(int id)
	{
		_chestId = id;
	}
	
	public void addLegs(int id)
	{
		_legs.add(id);
	}
	
	public void addHead(int id)
	{
		_head.add(id);
	}
	
	public void addGloves(int id)
	{
		_gloves.add(id);
	}
	
	public void addFeet(int id)
	{
		_feet.add(id);
	}
	
	public void addShield(int id)
	{
		_shield.add(id);
	}
	
	public void addSkill(SkillHolder holder)
	{
		_skills.add(holder);
	}
	
	public void addShieldSkill(SkillHolder holder)
	{
		_shieldSkills.add(holder);
	}
	
	public void addEnchant6Skill(SkillHolder holder)
	{
		_enchant6Skill.add(holder);
	}
	
	public void addCon(int value)
	{
		_con = value;
	}
	
	public void addDex(int value)
	{
		_dex = value;
	}
	
	public void addStr(int value)
	{
		_str = value;
	}
	
	public void addMen(int value)
	{
		_men = value;
	}
	
	public void addWit(int value)
	{
		_wit = value;
	}
	
	public void addInt(int value)
	{
		_int = value;
	}
	
	/**
	 * Checks if player have equipped all items from set (not checking shield)
	 * @param player whose inventory is being checked
	 * @return True if player equips whole set
	 */
	public boolean containAll(Player player)
	{
		final Inventory inv = player.getInventory();
		final Item legsItem = inv.getPaperdollItem(Inventory.PAPERDOLL_LEGS);
		final Item headItem = inv.getPaperdollItem(Inventory.PAPERDOLL_HEAD);
		final Item glovesItem = inv.getPaperdollItem(Inventory.PAPERDOLL_GLOVES);
		final Item feetItem = inv.getPaperdollItem(Inventory.PAPERDOLL_FEET);
		int legs = 0;
		int head = 0;
		int gloves = 0;
		int feet = 0;
		if (legsItem != null)
		{
			legs = legsItem.getId();
		}
		if (headItem != null)
		{
			head = headItem.getId();
		}
		if (glovesItem != null)
		{
			gloves = glovesItem.getId();
		}
		if (feetItem != null)
		{
			feet = feetItem.getId();
		}
		return containAll(_chestId, legs, head, gloves, feet);
	}
	
	public boolean containAll(int chest, int legs, int head, int gloves, int feet)
	{
		if (((_chestId != 0) && (_chestId != chest)) || (!_legs.isEmpty() && !_legs.contains(legs)))
		{
			return false;
		}
		if ((!_head.isEmpty() && !_head.contains(head)) || (!_gloves.isEmpty() && !_gloves.contains(gloves)))
		{
			return false;
		}
		return !(!_feet.isEmpty() && !_feet.contains(feet));
	}
	
	public boolean containItem(int slot, int itemId)
	{
		switch (slot)
		{
			case Inventory.PAPERDOLL_CHEST:
			{
				return _chestId == itemId;
			}
			case Inventory.PAPERDOLL_LEGS:
			{
				return _legs.contains(itemId);
			}
			case Inventory.PAPERDOLL_HEAD:
			{
				return _head.contains(itemId);
			}
			case Inventory.PAPERDOLL_GLOVES:
			{
				return _gloves.contains(itemId);
			}
			case Inventory.PAPERDOLL_FEET:
			{
				return _feet.contains(itemId);
			}
			default:
			{
				return false;
			}
		}
	}
	
	public int getChestId()
	{
		return _chestId;
	}
	
	public List<SkillHolder> getSkills()
	{
		return _skills;
	}
	
	public boolean containShield(Player player)
	{
		final Inventory inv = player.getInventory();
		final Item shieldItem = inv.getPaperdollItem(Inventory.PAPERDOLL_LHAND);
		return ((shieldItem != null) && _shield.contains(shieldItem.getId()));
	}
	
	public boolean containShield(int shieldId)
	{
		if (_shield.isEmpty())
		{
			return false;
		}
		return _shield.contains(shieldId);
	}
	
	public List<SkillHolder> getShieldSkillId()
	{
		return _shieldSkills;
	}
	
	public List<SkillHolder> getEnchant6skillId()
	{
		return _enchant6Skill;
	}
	
	/**
	 * @param player
	 * @return true if all parts of set are enchanted to +6 or more
	 */
	public boolean isEnchanted6(Player player)
	{
		// Player don't have full set
		if (!containAll(player))
		{
			return false;
		}
		
		final Inventory inv = player.getInventory();
		final Item chestItem = inv.getPaperdollItem(Inventory.PAPERDOLL_CHEST);
		final Item legsItem = inv.getPaperdollItem(Inventory.PAPERDOLL_LEGS);
		final Item headItem = inv.getPaperdollItem(Inventory.PAPERDOLL_HEAD);
		final Item glovesItem = inv.getPaperdollItem(Inventory.PAPERDOLL_GLOVES);
		final Item feetItem = inv.getPaperdollItem(Inventory.PAPERDOLL_FEET);
		if ((chestItem == null) || (chestItem.getEnchantLevel() < 6))
		{
			return false;
		}
		if (!_legs.isEmpty() && ((legsItem == null) || (legsItem.getEnchantLevel() < 6)))
		{
			return false;
		}
		if (!_gloves.isEmpty() && ((glovesItem == null) || (glovesItem.getEnchantLevel() < 6)))
		{
			return false;
		}
		if (!_head.isEmpty() && ((headItem == null) || (headItem.getEnchantLevel() < 6)))
		{
			return false;
		}
		return !(!_feet.isEmpty() && ((feetItem == null) || (feetItem.getEnchantLevel() < 6)));
	}
	
	public int getCON()
	{
		return _con;
	}
	
	public int getDEX()
	{
		return _dex;
	}
	
	public int getSTR()
	{
		return _str;
	}
	
	public int getMEN()
	{
		return _men;
	}
	
	public int getWIT()
	{
		return _wit;
	}
	
	public int getINT()
	{
		return _int;
	}
}
