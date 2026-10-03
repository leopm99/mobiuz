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
package village_master.OrcChange2;

import com.l2journey.gameserver.data.enums.CategoryType;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.enums.player.PlayerClass;

import ai.AbstractNpcAI;

/**
 * Orc class transfer AI.
 * @author Adry_85
 */
public class OrcChange2 extends AbstractNpcAI
{
	// NPCs
	private static int[] NPCS =
	{
		30513, // Penatus
		30681, // Karia
		30704, // Garvarentz
		30865, // Ladanza
		30913, // Tushku
		31288, // Aklan
		31326, // Lambac
		31336, // Rahorakti
		31977, // Shaka
	};
	
	// Items
	private static final int SHADOW_ITEM_EXCHANGE_COUPON_C_GRADE = 8870;
	private static final int MARK_OF_CHALLENGER = 2627; // proof11x, proof21x
	private static final int MARK_OF_PILGRIM = 2721; // proof31x, proof32x
	private static final int MARK_OF_DUELIST = 2762; // proof21z
	private static final int MARK_OF_WARSPIRIT = 2879; // proof32z
	private static final int MARK_OF_GLORY = 3203; // proof11y, proof21y, proof31y, proof32y
	private static final int MARK_OF_CHAMPION = 3276; // proof11z
	private static final int MARK_OF_LORD = 3390; // proof31z
	// Classes
	private static final int DESTROYER = 46;
	private static final int TYRANT = 48;
	private static final int OVERLORD = 51;
	private static final int WARCRYER = 52;
	
	private OrcChange2()
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
			case "30513-03.htm", "30513-04.htm", "30513-05.htm", "30513-07.htm", "30513-08.htm", "30513-09.htm", "30513-10.htm", "30513-11.htm", "30513-12.htm", "30513-13.htm", "30513-14.htm", "30513-15.htm", "30513-16.htm": // master_lv3_orc006ra; master_lv3_orc007ra; master_lv3_orc007rat; master_lv3_orc006ma; master_lv3_orc007ma; master_lv3_orc007mat; master_lv3_orc003s; master_lv3_orc006sa; master_lv3_orc007sa; master_lv3_orc007sat; master_lv3_orc006sb; master_lv3_orc007sb; master_lv3_orc007sbt
			{
				htmltext = event;
				break;
			}
			case "46", "48", "51", "52":
			{
				htmltext = ClassChangeRequested(player, Integer.parseInt(event));
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
			htmltext = "30513-19.htm"; // fnYouAreThirdClass
		}
		else if ((classId == DESTROYER) && (player.getPlayerClass() == PlayerClass.ORC_RAIDER))
		{
			if (player.getLevel() < 40)
			{
				if (hasQuestItems(player, MARK_OF_CHALLENGER, MARK_OF_GLORY, MARK_OF_CHAMPION))
				{
					htmltext = "30513-20.htm"; // fnLowLevel11
				}
				else
				{
					htmltext = "30513-21.htm"; // fnLowLevelNoProof11
				}
			}
			else if (hasQuestItems(player, MARK_OF_CHALLENGER, MARK_OF_GLORY, MARK_OF_CHAMPION))
			{
				takeItems(player, -1, MARK_OF_CHALLENGER, MARK_OF_GLORY, MARK_OF_CHAMPION);
				player.setPlayerClass(DESTROYER);
				player.setBaseClass(DESTROYER);
				// SystemMessage and cast skill is done by setClassId
				player.broadcastUserInfo();
				giveItems(player, SHADOW_ITEM_EXCHANGE_COUPON_C_GRADE, 15);
				htmltext = "30513-22.htm"; // fnAfterClassChange11
			}
			else
			{
				htmltext = "30513-23.htm"; // fnNoProof11
			}
		}
		else if ((classId == TYRANT) && (player.getPlayerClass() == PlayerClass.ORC_MONK))
		{
			if (player.getLevel() < 40)
			{
				if (hasQuestItems(player, MARK_OF_CHALLENGER, MARK_OF_GLORY, MARK_OF_DUELIST))
				{
					htmltext = "30513-24.htm"; // fnLowLevel21
				}
				else
				{
					htmltext = "30513-25.htm"; // fnLowLevelNoProof21
				}
			}
			else if (hasQuestItems(player, MARK_OF_CHALLENGER, MARK_OF_GLORY, MARK_OF_DUELIST))
			{
				takeItems(player, -1, MARK_OF_CHALLENGER, MARK_OF_GLORY, MARK_OF_DUELIST);
				player.setPlayerClass(TYRANT);
				player.setBaseClass(TYRANT);
				// SystemMessage and cast skill is done by setClassId
				player.broadcastUserInfo();
				giveItems(player, SHADOW_ITEM_EXCHANGE_COUPON_C_GRADE, 15);
				htmltext = "30513-26.htm"; // fnAfterClassChange21
			}
			else
			{
				htmltext = "30513-27.htm"; // fnNoProof21
			}
		}
		else if ((classId == OVERLORD) && (player.getPlayerClass() == PlayerClass.ORC_SHAMAN))
		{
			if (player.getLevel() < 40)
			{
				if (hasQuestItems(player, MARK_OF_PILGRIM, MARK_OF_GLORY, MARK_OF_LORD))
				{
					htmltext = "30513-28.htm"; // fnLowLevel31
				}
				else
				{
					htmltext = "30513-29.htm"; // fnLowLevelNoProof31
				}
			}
			else if (hasQuestItems(player, MARK_OF_PILGRIM, MARK_OF_GLORY, MARK_OF_LORD))
			{
				takeItems(player, -1, MARK_OF_PILGRIM, MARK_OF_GLORY, MARK_OF_LORD);
				player.setPlayerClass(OVERLORD);
				player.setBaseClass(OVERLORD);
				// SystemMessage and cast skill is done by setClassId
				player.broadcastUserInfo();
				giveItems(player, SHADOW_ITEM_EXCHANGE_COUPON_C_GRADE, 15);
				htmltext = "30513-30.htm"; // fnAfterClassChange31
			}
			else
			{
				htmltext = "30513-31.htm"; // fnNoProof31
			}
		}
		else if ((classId == WARCRYER) && (player.getPlayerClass() == PlayerClass.ORC_SHAMAN))
		{
			if (player.getLevel() < 40)
			{
				if (hasQuestItems(player, MARK_OF_PILGRIM, MARK_OF_GLORY, MARK_OF_WARSPIRIT))
				{
					htmltext = "30513-32.htm"; // fnLowLevel32
				}
				else
				{
					htmltext = "30513-33.htm"; // fnLowLevelNoProof32
				}
			}
			else if (hasQuestItems(player, MARK_OF_PILGRIM, MARK_OF_GLORY, MARK_OF_WARSPIRIT))
			{
				takeItems(player, -1, MARK_OF_PILGRIM, MARK_OF_GLORY, MARK_OF_WARSPIRIT);
				player.setPlayerClass(WARCRYER);
				player.setBaseClass(WARCRYER);
				// SystemMessage and cast skill is done by setClassId
				player.broadcastUserInfo();
				giveItems(player, SHADOW_ITEM_EXCHANGE_COUPON_C_GRADE, 15);
				htmltext = "30513-34.htm"; // fnAfterClassChange32
			}
			else
			{
				htmltext = "30513-35.htm"; // fnNoProof32
			}
		}
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		String htmltext = null;
		if (player.isInCategory(CategoryType.FOURTH_CLASS_GROUP) && (player.isInCategory(CategoryType.ORC_MALL_CLASS) || player.isInCategory(CategoryType.ORC_FALL_CLASS)))
		{
			htmltext = "30513-01.htm"; // fnYouAreFourthClass
		}
		else if (player.isInCategory(CategoryType.ORC_MALL_CLASS) || player.isInCategory(CategoryType.ORC_FALL_CLASS))
		{
			final PlayerClass classId = player.getPlayerClass();
			if ((classId == PlayerClass.ORC_RAIDER) || (classId == PlayerClass.DESTROYER))
			{
				htmltext = "30513-02.htm"; // fnClassList1
			}
			else if ((classId == PlayerClass.ORC_MONK) || (classId == PlayerClass.TYRANT))
			{
				htmltext = "30513-06.htm"; // fnClassList2
			}
			else if ((classId == PlayerClass.ORC_SHAMAN) || (classId == PlayerClass.OVERLORD) || (classId == PlayerClass.WARCRYER))
			{
				htmltext = "30513-10.htm"; // fnClassList3
			}
			else
			{
				htmltext = "30513-17.htm"; // fnYouAreFirstClass
			}
		}
		else
		{
			htmltext = "30513-18.htm"; // fnClassMismatch
		}
		return htmltext;
	}
	
	public static void main(String[] args)
	{
		new OrcChange2();
	}
}
