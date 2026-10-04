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
package ai.areas.Giran.Alexandria;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.item.holders.ItemChanceHolder;
import com.l2journey.gameserver.model.item.holders.ItemHolder;

import ai.AbstractNpcAI;

/**
 * Alexandria (Armor Merchant) AI.
 * @author xban1x
 */
public class Alexandria extends AbstractNpcAI
{
	// NPC
	private static final int ALEXANDRIA = 30098;
	// Items
	private static final ItemHolder[] REQUIRED_ITEMS = new ItemHolder[]
	{
		new ItemHolder(57, 7500000),
		new ItemHolder(5094, 50),
		new ItemHolder(6471, 25),
		new ItemHolder(9814, 4),
		new ItemHolder(9815, 3),
		new ItemHolder(9816, 5),
		new ItemHolder(9817, 5),
	};
	// Agathions
	private static final ItemChanceHolder[] LITTLE_DEVILS = new ItemChanceHolder[]
	{
		new AdditionalItemChanceHolder(10321, 600, 1, 10408),
		new ItemChanceHolder(10322, 10),
		new ItemChanceHolder(10323, 10),
		new ItemChanceHolder(10324, 5),
		new ItemChanceHolder(10325, 5),
		new ItemChanceHolder(10326, 370),
	};
	private static final ItemChanceHolder[] LITTLE_ANGELS = new ItemChanceHolder[]
	{
		new AdditionalItemChanceHolder(10315, 600, 1, 10408),
		new ItemChanceHolder(10316, 10),
		new ItemChanceHolder(10317, 10),
		new ItemChanceHolder(10318, 5),
		new ItemChanceHolder(10319, 5),
		new ItemChanceHolder(10320, 370),
	};
	private static final Map<String, List<ItemChanceHolder>> AGATHIONS = new HashMap<>();
	static
	{
		AGATHIONS.put("littleAngel", Arrays.asList(LITTLE_ANGELS));
		AGATHIONS.put("littleDevil", Arrays.asList(LITTLE_DEVILS));
	}
	
	private Alexandria()
	{
		addStartNpc(ALEXANDRIA);
		addTalkId(ALEXANDRIA);
		addFirstTalkId(ALEXANDRIA);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		if (event.equals("30098-02.html"))
		{
			htmltext = event;
		}
		else if (AGATHIONS.containsKey(event))
		{
			final int chance = getRandom(1000);
			int chance2 = 0;
			int chance3 = 0;
			for (ItemChanceHolder agathion : AGATHIONS.get(event))
			{
				chance3 += agathion.getChance();
				if ((chance2 <= chance) && (chance < chance3))
				{
					if (takeAllItems(player, REQUIRED_ITEMS))
					{
						giveItems(player, agathion);
						htmltext = "30098-03.html";
						if (agathion instanceof AdditionalItemChanceHolder additionalItemChanceHolder)
						{
							giveItems(player, additionalItemChanceHolder.getAdditionalId(), 1);
							htmltext = "30098-03a.html";
						}
					}
					else
					{
						htmltext = "30098-04.html";
					}
					break;
				}
				chance2 += agathion.getChance();
			}
		}
		return htmltext;
	}
	
	private static class AdditionalItemChanceHolder extends ItemChanceHolder
	{
		private final int _additionalId;
		
		AdditionalItemChanceHolder(int id, int chance, long count, int additionalId)
		{
			super(id, chance, count);
			_additionalId = additionalId;
		}
		
		public int getAdditionalId()
		{
			return _additionalId;
		}
	}
	
	public static void main(String[] args)
	{
		new Alexandria();
	}
}
