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
package village_master.DwarfWarehouseChange2;

import com.l2journey.gameserver.data.enums.CategoryType;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.enums.player.PlayerClass;

import ai.AbstractNpcAI;

/**
 * Dwarf class transfer AI.
 * @author Adry_85
 */
public class DwarfWarehouseChange2 extends AbstractNpcAI
{
	// NPCs
	private static int[] NPCS =
	{
		30511, // Gesto
		30676, // Croop
		30685, // Baxt
		30845, // Klump
		30894, // Natools
		31269, // Mona
		31314, // Donal
		31958, // Yasheni
	};
	
	// Items
	private static final int SHADOW_ITEM_EXCHANGE_COUPON_C_GRADE = 8870;
	private static final int MARK_OF_SEARCHER = 2809; // proof11z
	private static final int MARK_OF_GUILDSMAN = 3119; // proof11x
	private static final int MARK_OF_PROSPERITY = 3238; // proof11y
	// Class
	private static final int BOUNTY_HUNTER = 55;
	
	private DwarfWarehouseChange2()
	{
		addStartNpc(NPCS);
		addTalkId(NPCS);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		switch (event)
		{
			case "30511-03.htm", "30511-04.htm", "30511-05.htm": // master_lv3_ware006fa; master_lv3_ware007fa; master_lv3_ware007fat
			{
				htmltext = event;
				break;
			}
			case "55":
			{
				htmltext = ClassChangeRequested(player, Integer.parseInt(event));
				break;
			}
			default:
			{
				break;
			}
		}
		return htmltext;
	}
	
	private String ClassChangeRequested(Player player, int classId)
	{
		String htmltext = null;
		if (player.isInCategory(CategoryType.THIRD_CLASS_GROUP))
		{
			htmltext = "30511-08.htm"; // fnYouAreThirdClass
		}
		else if ((classId == BOUNTY_HUNTER) && (player.getPlayerClass() == PlayerClass.SCAVENGER))
		{
			if (player.getLevel() < 40)
			{
				if (hasQuestItems(player, MARK_OF_GUILDSMAN, MARK_OF_PROSPERITY, MARK_OF_SEARCHER))
				{
					htmltext = "30511-09.htm"; // fnLowLevel11
				}
				else
				{
					htmltext = "30511-10.htm"; // fnLowLevelNoProof11
				}
			}
			else if (hasQuestItems(player, MARK_OF_GUILDSMAN, MARK_OF_PROSPERITY, MARK_OF_SEARCHER))
			{
				takeItems(player, -1, MARK_OF_GUILDSMAN, MARK_OF_PROSPERITY, MARK_OF_SEARCHER);
				player.setPlayerClass(BOUNTY_HUNTER);
				player.setBaseClass(BOUNTY_HUNTER);
				// SystemMessage and cast skill is done by setClassId
				player.broadcastUserInfo();
				giveItems(player, SHADOW_ITEM_EXCHANGE_COUPON_C_GRADE, 15);
				htmltext = "30511-11.htm"; // fnAfterClassChange11
			}
			else
			{
				htmltext = "30511-12.htm"; // fnNoProof11
			}
		}
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		String htmltext = null;
		if (player.isInCategory(CategoryType.FOURTH_CLASS_GROUP) && player.isInCategory(CategoryType.BOUNTY_HUNTER_GROUP))
		{
			htmltext = "30511-01.htm"; // fnYouAreFourthClass
		}
		else if (player.isInCategory(CategoryType.BOUNTY_HUNTER_GROUP))
		{
			final PlayerClass classId = player.getPlayerClass();
			if ((classId == PlayerClass.SCAVENGER) || (classId == PlayerClass.BOUNTY_HUNTER))
			{
				htmltext = "30511-02.htm"; // fnClassList1
			}
			else
			{
				htmltext = "30511-06.htm"; // fnYouAreFirstClass
			}
		}
		else
		{
			htmltext = "30511-07.htm"; // fnClassMismatch
		}
		return htmltext;
	}
	
	public static void main(String[] args)
	{
		new DwarfWarehouseChange2();
	}
}
