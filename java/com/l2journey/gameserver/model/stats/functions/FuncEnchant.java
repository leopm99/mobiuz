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
package com.l2journey.gameserver.model.stats.functions;

import com.l2journey.EventsConfig;
import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.conditions.Condition;
import com.l2journey.gameserver.model.item.ItemTemplate;
import com.l2journey.gameserver.model.item.instance.Item;
import com.l2journey.gameserver.model.item.type.WeaponType;
import com.l2journey.gameserver.model.skill.Skill;
import com.l2journey.gameserver.model.stats.Stat;

public class FuncEnchant extends AbstractFunction
{
	public FuncEnchant(Stat stat, int order, Object owner, double value, Condition applyCond)
	{
		super(stat, order, owner, value, applyCond);
	}
	
	@Override
	public double calc(Creature effector, Creature effected, Skill skill, double initVal)
	{
		double value = initVal;
		if ((getApplyCond() != null) && !getApplyCond().test(effector, effected, skill))
		{
			return value;
		}
		
		final Item item = (Item) getFuncOwner();
		int enchant = item.getEnchantLevel();
		if (enchant <= 0)
		{
			return value;
		}
		
		int overenchant = 0;
		if (enchant > 3)
		{
			overenchant = enchant - 3;
			enchant = 3;
		}
		
		if (effector.isPlayer() && effector.asPlayer().isInOlympiadMode() && (EventsConfig.OLYMPIAD_ENCHANT_LIMIT >= 0) && ((enchant + overenchant) > EventsConfig.OLYMPIAD_ENCHANT_LIMIT))
		{
			if (EventsConfig.OLYMPIAD_ENCHANT_LIMIT > 3)
			{
				overenchant = EventsConfig.OLYMPIAD_ENCHANT_LIMIT - 3;
			}
			else
			{
				overenchant = 0;
				enchant = EventsConfig.OLYMPIAD_ENCHANT_LIMIT;
			}
		}
		
		if ((getStat() == Stat.MAGIC_DEFENCE) || (getStat() == Stat.POWER_DEFENCE))
		{
			return value + enchant + (3 * overenchant);
		}
		
		if (getStat() == Stat.MAGIC_ATTACK)
		{
			switch (item.getTemplate().getCrystalTypePlus())
			{
				case S:
				{
					// M. Atk. increases by 4 for all weapons.
					// Starting at +4, M. Atk. bonus double.
					value += (4 * enchant) + (8 * overenchant);
					break;
				}
				case A, B, C:
				{
					// M. Atk. increases by 3 for all weapons.
					// Starting at +4, M. Atk. bonus double.
					value += (3 * enchant) + (6 * overenchant);
					break;
				}
				case D, NONE:
				{
					// M. Atk. increases by 2 for all weapons. Starting at +4, M. Atk. bonus double.
					// Starting at +4, M. Atk. bonus double.
					value += (2 * enchant) + (4 * overenchant);
					break;
				}
				default:
				{
					break;
				}
			}
			return value;
		}
		
		if (item.isWeapon())
		{
			final WeaponType type = (WeaponType) item.getItemType();
			switch (item.getTemplate().getCrystalTypePlus())
			{
				case S:
				{
					if (item.getWeaponItem().getBodyPart() == ItemTemplate.SLOT_LR_HAND)
					{
						switch (type)
						{
							case BOW:
							{
								// P. Atk. increases by 10 for bows.
								// Starting at +4, P. Atk. bonus double.
								value += (10 * enchant) + (20 * overenchant);
								break;
							}
							case CROSSBOW:
							{
								// P. Atk. increases by 7 for crossbows.
								// Starting at +4, P. Atk. bonus double.
								value += (7 * enchant) + (14 * overenchant);
								break;
							}
							default:
							{
								// P. Atk. increases by 6 for two-handed swords, two-handed blunts, dualswords, and two-handed combat weapons.
								// Starting at +4, P. Atk. bonus double.
								value += (6 * enchant) + (12 * overenchant);
								break;
							}
						}
					}
					else
					{
						// P. Atk. increases by 5 for one-handed swords, one-handed blunts, daggers, spears, and other weapons.
						// Starting at +4, P. Atk. bonus double.
						value += (5 * enchant) + (10 * overenchant);
					}
					break;
				}
				case A:
				{
					if (item.getWeaponItem().getBodyPart() == ItemTemplate.SLOT_LR_HAND)
					{
						switch (type)
						{
							case BOW:
							{
								// P. Atk. increases by 8 for bows.
								// Starting at +4, P. Atk. bonus double.
								value += (8 * enchant) + (16 * overenchant);
								break;
							}
							case CROSSBOW:
							{
								// P. Atk. increases by 6 for crossbows.
								// Starting at +4, P. Atk. bonus double.
								value += (6 * enchant) + (12 * overenchant);
								break;
							}
							default:
							{
								// P. Atk. increases by 5 for two-handed swords, two-handed blunts, dualswords, and two-handed combat weapons.
								// Starting at +4, P. Atk. bonus double.
								value += (5 * enchant) + (10 * overenchant);
								break;
							}
						}
					}
					else
					{
						// P. Atk. increases by 4 for one-handed swords, one-handed blunts, daggers, spears, and other weapons.
						// Starting at +4, P. Atk. bonus double.
						value += (4 * enchant) + (8 * overenchant);
					}
					break;
				}
				case B, C:
				{
					if (item.getWeaponItem().getBodyPart() == ItemTemplate.SLOT_LR_HAND)
					{
						switch (type)
						{
							case BOW:
							{
								// P. Atk. increases by 6 for bows.
								// Starting at +4, P. Atk. bonus double.
								value += (6 * enchant) + (12 * overenchant);
								break;
							}
							case CROSSBOW:
							{
								// P. Atk. increases by 5 for crossbows.
								// Starting at +4, P. Atk. bonus double.
								value += (5 * enchant) + (10 * overenchant);
								break;
							}
							default:
							{
								// P. Atk. increases by 4 for two-handed swords, two-handed blunts, dualswords, and two-handed combat weapons.
								// Starting at +4, P. Atk. bonus double.
								value += (4 * enchant) + (8 * overenchant);
								break;
							}
						}
					}
					else
					{
						// P. Atk. increases by 3 for one-handed swords, one-handed blunts, daggers, spears, and other weapons.
						// Starting at +4, P. Atk. bonus double.
						value += (3 * enchant) + (6 * overenchant);
					}
					break;
				}
				case D, NONE:
				{
					switch (type)
					{
						case BOW:
						{
							// Bows increase by 4.
							// Starting at +4, P. Atk. bonus double.
							value += (4 * enchant) + (8 * overenchant);
							break;
						}
						case CROSSBOW:
						{
							// Crossbows increase by 3.
							// Starting at +4, P. Atk. bonus double.
							value += (3 * enchant) + (6 * overenchant);
							break;
						}
						default:
						{
							// P. Atk. increases by 2 for all weapons with the exception of bows.
							// Starting at +4, P. Atk. bonus double.
							value += (2 * enchant) + (4 * overenchant);
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
		}
		return value;
	}
}
