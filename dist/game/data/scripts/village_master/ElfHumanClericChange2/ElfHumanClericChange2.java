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
package village_master.ElfHumanClericChange2;

import com.l2journey.gameserver.data.enums.CategoryType;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.enums.player.PlayerClass;

import ai.AbstractNpcAI;

/**
 * Elf Human class transfer AI.
 * @author Adry_85
 */
public class ElfHumanClericChange2 extends AbstractNpcAI
{
	// NPCs
	private static int[] NPCS =
	{
		30120, // Maximilian
		30191, // Hollint
		30857, // Orven
		30905, // Squillari
		31279, // Gregory
		31328, // Innocentin
		31968, // Baryl
	};
	
	// Items
	private static final int SHADOW_ITEM_EXCHANGE_COUPON_C_GRADE = 8870;
	private static final int MARK_OF_PILGRIM = 2721; // proof11x, proof12x, proof21x
	private static final int MARK_OF_TRUST = 2734; // proof11y, proof12y
	private static final int MARK_OF_HEALER = 2820; // proof11z, proof21z
	private static final int MARK_OF_REFORMER = 2821; // proof12z
	private static final int MARK_OF_LIFE = 3140; // proof21y
	// Classes
	private static final int BISHOP = 16;
	private static final int PROPHET = 17;
	private static final int ELDER = 30;
	
	private ElfHumanClericChange2()
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
			case "30120-02.htm", "30120-03.htm", "30120-04.htm", "30120-05.htm", "30120-06.htm", "30120-07.htm", "30120-08.htm", "30120-10.htm", "30120-11.htm", "30120-12.htm": // master_lv3_hec003h; master_lv3_hec006ha; master_lv3_hec007ha; master_lv3_hec007hat; master_lv3_hec006hb; master_lv3_hec007hb; master_lv3_hec007hbt; master_lv3_hec006ea; master_lv3_hec007ea; master_lv3_hec007eat
			{
				htmltext = event;
				break;
			}
			case "16", "17", "30":
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
			htmltext = "30120-15.htm"; // fnYouAreThirdClass
		}
		else if ((classId == BISHOP) && (player.getPlayerClass() == PlayerClass.CLERIC))
		{
			if (player.getLevel() < 40)
			{
				if (hasQuestItems(player, MARK_OF_PILGRIM, MARK_OF_TRUST, MARK_OF_HEALER))
				{
					htmltext = "30120-16.htm"; // fnLowLevel11
				}
				else
				{
					htmltext = "30120-17.htm"; // fnLowLevelNoProof11
				}
			}
			else if (hasQuestItems(player, MARK_OF_PILGRIM, MARK_OF_TRUST, MARK_OF_HEALER))
			{
				takeItems(player, -1, MARK_OF_PILGRIM, MARK_OF_TRUST, MARK_OF_HEALER);
				player.setPlayerClass(BISHOP);
				player.setBaseClass(BISHOP);
				// SystemMessage and cast skill is done by setClassId
				player.broadcastUserInfo();
				giveItems(player, SHADOW_ITEM_EXCHANGE_COUPON_C_GRADE, 15);
				htmltext = "30120-18.htm"; // fnAfterClassChange11
			}
			else
			{
				htmltext = "30120-19.htm"; // fnNoProof11
			}
		}
		else if ((classId == PROPHET) && (player.getPlayerClass() == PlayerClass.CLERIC))
		{
			if (player.getLevel() < 40)
			{
				if (hasQuestItems(player, MARK_OF_PILGRIM, MARK_OF_TRUST, MARK_OF_REFORMER))
				{
					htmltext = "30120-20.htm"; // fnLowLevel12
				}
				else
				{
					htmltext = "30120-21.htm"; // fnLowLevelNoProof12
				}
			}
			else if (hasQuestItems(player, MARK_OF_PILGRIM, MARK_OF_TRUST, MARK_OF_REFORMER))
			{
				takeItems(player, -1, MARK_OF_PILGRIM, MARK_OF_TRUST, MARK_OF_REFORMER);
				player.setPlayerClass(PROPHET);
				player.setBaseClass(PROPHET);
				// SystemMessage and cast skill is done by setClassId
				player.broadcastUserInfo();
				giveItems(player, SHADOW_ITEM_EXCHANGE_COUPON_C_GRADE, 15);
				htmltext = "30120-22.htm"; // fnAfterClassChange12
			}
			else
			{
				htmltext = "30120-23.htm"; // fnNoProof12
			}
		}
		else if ((classId == ELDER) && (player.getPlayerClass() == PlayerClass.ORACLE))
		{
			if (player.getLevel() < 40)
			{
				if (hasQuestItems(player, MARK_OF_PILGRIM, MARK_OF_LIFE, MARK_OF_HEALER))
				{
					htmltext = "30120-24.htm"; // fnLowLevel21
				}
				else
				{
					htmltext = "30120-25.htm"; // fnLowLevelNoProof21
				}
			}
			else if (hasQuestItems(player, MARK_OF_PILGRIM, MARK_OF_LIFE, MARK_OF_HEALER))
			{
				takeItems(player, -1, MARK_OF_PILGRIM, MARK_OF_LIFE, MARK_OF_HEALER);
				player.setPlayerClass(ELDER);
				player.setBaseClass(ELDER);
				// SystemMessage and cast skill is done by setClassId
				player.broadcastUserInfo();
				giveItems(player, SHADOW_ITEM_EXCHANGE_COUPON_C_GRADE, 15);
				htmltext = "30120-26.htm"; // fnAfterClassChange21
			}
			else
			{
				htmltext = "30120-27.htm"; // fnNoProof21
			}
		}
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		String htmltext = null;
		if (player.isInCategory(CategoryType.CLERIC_GROUP) && player.isInCategory(CategoryType.FOURTH_CLASS_GROUP) && (player.isInCategory(CategoryType.HUMAN_CALL_CLASS) || player.isInCategory(CategoryType.ELF_CALL_CLASS)))
		{
			htmltext = "30120-01.htm"; // fnYouAreFourthClass
		}
		else if (player.isInCategory(CategoryType.CLERIC_GROUP) && (player.isInCategory(CategoryType.HUMAN_CALL_CLASS) || player.isInCategory(CategoryType.ELF_CALL_CLASS)))
		{
			final PlayerClass classId = player.getPlayerClass();
			if ((classId == PlayerClass.CLERIC) || (classId == PlayerClass.BISHOP) || (classId == PlayerClass.PROPHET))
			{
				htmltext = "30120-02.htm"; // fnClassList1
			}
			else if ((classId == PlayerClass.ORACLE) || (classId == PlayerClass.ELDER))
			{
				htmltext = "30120-09.htm"; // fnClassList2
			}
			else
			{
				htmltext = "30120-13.htm"; // fnYouAreFirstClass
			}
		}
		else
		{
			htmltext = "30120-14.htm"; // fnClassMismatch
		}
		return htmltext;
	}
	
	public static void main(String[] args)
	{
		new ElfHumanClericChange2();
	}
}
