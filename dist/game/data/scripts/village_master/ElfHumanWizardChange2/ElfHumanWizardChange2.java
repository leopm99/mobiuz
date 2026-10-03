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
package village_master.ElfHumanWizardChange2;

import com.l2journey.gameserver.data.enums.CategoryType;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.enums.player.PlayerClass;

import ai.AbstractNpcAI;

/**
 * Elf Human class transfer AI.
 * @author Adry_85
 */
public class ElfHumanWizardChange2 extends AbstractNpcAI
{
	// NPCs
	private static int[] NPCS =
	{
		30115, // Jurek
		30174, // Arkenias
		30176, // Valleria
		30694, // Scraide
		30854, // Drikiyan
		31331, // Valdis
		31755, // Halaster
		31996, // Javier
	};
	
	// Items
	private static final int SHADOW_ITEM_EXCHANGE_COUPON_C_GRADE = 8870;
	private static final int MARK_OF_SCHOLAR = 2674; // proof11x, proof12x, proof13x, proof21x, proof22x
	private static final int MARK_OF_TRUST = 2734; // proof11y, proof12y, proof13y
	private static final int MARK_OF_MAGUS = 2840; // proof11z, proof21z
	private static final int MARK_OF_WITCHCRAFT = 3307; // proof12z
	private static final int MARK_OF_SUMMONER = 3336; // proof13z, proof22z
	private static final int MARK_OF_LIFE = 3140; // proof21y, proof22y
	// Classes
	private static final int SORCERER = 12;
	private static final int NECROMANCER = 13;
	private static final int WARLOCK = 14;
	private static final int SPELLSINGER = 27;
	private static final int ELEMENTAL_SUMMONER = 28;
	
	private ElfHumanWizardChange2()
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
			case "30115-02.htm", "30115-03.htm", "30115-04.htm", "30115-05.htm", "30115-06.htm", "30115-07.htm", "30115-08.htm", "30115-09.htm", "30115-10.htm", "30115-11.htm", "30115-12.htm", "30115-13.htm", "30115-14.htm", "30115-15.htm", "30115-16.htm", "30115-17.htm", "30115-18.htm": // master_lv3_hew003h; master_lv3_hew006ha; master_lv3_hew007ha; master_lv3_hew007hat; master_lv3_hew006hb; master_lv3_hew007hb; master_lv3_hew007hbt; master_lv3_hew006hc; master_lv3_hew007hc; master_lv3_hew007hct; master_lv3_hew003e; master_lv3_hew006ea; master_lv3_hew007ea; master_lv3_hew007eat; master_lv3_hew006eb; master_lv3_hew007eb; master_lv3_hew007ebt
			{
				htmltext = event;
				break;
			}
			case "12", "13", "14", "27", "28":
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
			htmltext = "30115-21.htm"; // fnYouAreThirdClass
		}
		else if ((classId == SORCERER) && (player.getPlayerClass() == PlayerClass.WIZARD))
		{
			if (player.getLevel() < 40)
			{
				if (hasQuestItems(player, MARK_OF_SCHOLAR, MARK_OF_TRUST, MARK_OF_MAGUS))
				{
					htmltext = "30115-22.htm"; // fnLowLevel11
				}
				else
				{
					htmltext = "30115-23.htm"; // fnLowLevelNoProof11
				}
			}
			else if (hasQuestItems(player, MARK_OF_SCHOLAR, MARK_OF_TRUST, MARK_OF_MAGUS))
			{
				takeItems(player, -1, MARK_OF_SCHOLAR, MARK_OF_TRUST, MARK_OF_MAGUS);
				player.setPlayerClass(SORCERER);
				player.setBaseClass(SORCERER);
				// SystemMessage and cast skill is done by setClassId
				player.broadcastUserInfo();
				giveItems(player, SHADOW_ITEM_EXCHANGE_COUPON_C_GRADE, 15);
				htmltext = "30115-24.htm"; // fnAfterClassChange11
			}
			else
			{
				htmltext = "30115-25.htm"; // fnNoProof11
			}
		}
		else if ((classId == NECROMANCER) && (player.getPlayerClass() == PlayerClass.WIZARD))
		{
			if (player.getLevel() < 40)
			{
				if (hasQuestItems(player, MARK_OF_SCHOLAR, MARK_OF_TRUST, MARK_OF_WITCHCRAFT))
				{
					htmltext = "30115-26.htm"; // fnLowLevel12
				}
				else
				{
					htmltext = "30115-27.htm"; // fnLowLevelNoProof12
				}
			}
			else if (hasQuestItems(player, MARK_OF_SCHOLAR, MARK_OF_TRUST, MARK_OF_WITCHCRAFT))
			{
				takeItems(player, -1, MARK_OF_SCHOLAR, MARK_OF_TRUST, MARK_OF_WITCHCRAFT);
				player.setPlayerClass(NECROMANCER);
				player.setBaseClass(NECROMANCER);
				// SystemMessage and cast skill is done by setClassId
				player.broadcastUserInfo();
				giveItems(player, SHADOW_ITEM_EXCHANGE_COUPON_C_GRADE, 15);
				htmltext = "30115-28.htm"; // fnAfterClassChange12
			}
			else
			{
				htmltext = "30115-29.htm"; // fnNoProof12
			}
		}
		else if ((classId == WARLOCK) && (player.getPlayerClass() == PlayerClass.WIZARD))
		{
			if (player.getLevel() < 40)
			{
				if (hasQuestItems(player, MARK_OF_SCHOLAR, MARK_OF_TRUST, MARK_OF_SUMMONER))
				{
					htmltext = "30115-30.htm"; // fnLowLevel13
				}
				else
				{
					htmltext = "30115-31.htm"; // fnLowLevelNoProof13
				}
			}
			else if (hasQuestItems(player, MARK_OF_SCHOLAR, MARK_OF_TRUST, MARK_OF_SUMMONER))
			{
				takeItems(player, -1, MARK_OF_SCHOLAR, MARK_OF_TRUST, MARK_OF_SUMMONER);
				player.setPlayerClass(WARLOCK);
				player.setBaseClass(WARLOCK);
				// SystemMessage and cast skill is done by setClassId
				player.broadcastUserInfo();
				giveItems(player, SHADOW_ITEM_EXCHANGE_COUPON_C_GRADE, 15);
				htmltext = "30115-32.htm"; // fnAfterClassChange13
			}
			else
			{
				htmltext = "30115-33.htm"; // fnNoProof13
			}
		}
		else if ((classId == SPELLSINGER) && (player.getPlayerClass() == PlayerClass.ELVEN_WIZARD))
		{
			if (player.getLevel() < 40)
			{
				if (hasQuestItems(player, MARK_OF_SCHOLAR, MARK_OF_LIFE, MARK_OF_MAGUS))
				{
					htmltext = "30115-34.htm"; // fnLowLevel21
				}
				else
				{
					htmltext = "30115-35.htm"; // fnLowLevelNoProof21
				}
			}
			else if (hasQuestItems(player, MARK_OF_SCHOLAR, MARK_OF_LIFE, MARK_OF_MAGUS))
			{
				takeItems(player, -1, MARK_OF_SCHOLAR, MARK_OF_LIFE, MARK_OF_MAGUS);
				player.setPlayerClass(SPELLSINGER);
				player.setBaseClass(SPELLSINGER);
				// SystemMessage and cast skill is done by setClassId
				player.broadcastUserInfo();
				giveItems(player, SHADOW_ITEM_EXCHANGE_COUPON_C_GRADE, 15);
				htmltext = "30115-36.htm"; // fnAfterClassChange21
			}
			else
			{
				htmltext = "30115-37.htm"; // fnNoProof21
			}
		}
		else if ((classId == ELEMENTAL_SUMMONER) && (player.getPlayerClass() == PlayerClass.ELVEN_WIZARD))
		{
			if (player.getLevel() < 40)
			{
				if (hasQuestItems(player, MARK_OF_SCHOLAR, MARK_OF_LIFE, MARK_OF_SUMMONER))
				{
					htmltext = "30115-38.htm"; // fnLowLevel22
				}
				else
				{
					htmltext = "30115-39.htm"; // fnLowLevelNoProof22
				}
			}
			else if (hasQuestItems(player, MARK_OF_SCHOLAR, MARK_OF_LIFE, MARK_OF_SUMMONER))
			{
				takeItems(player, -1, MARK_OF_SCHOLAR, MARK_OF_LIFE, MARK_OF_SUMMONER);
				player.setPlayerClass(ELEMENTAL_SUMMONER);
				player.setBaseClass(ELEMENTAL_SUMMONER);
				// SystemMessage and cast skill is done by setClassId
				player.broadcastUserInfo();
				giveItems(player, SHADOW_ITEM_EXCHANGE_COUPON_C_GRADE, 15);
				htmltext = "30115-40.htm"; // fnAfterClassChange22
			}
			else
			{
				htmltext = "30115-41.htm"; // fnNoProof22
			}
		}
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		String htmltext = null;
		if (player.isInCategory(CategoryType.WIZARD_GROUP) && player.isInCategory(CategoryType.FOURTH_CLASS_GROUP) && (player.isInCategory(CategoryType.HUMAN_MALL_CLASS) || player.isInCategory(CategoryType.ELF_MALL_CLASS)))
		{
			htmltext = "30115-01.htm"; // fnYouAreFourthClass
		}
		else if (player.isInCategory(CategoryType.WIZARD_GROUP) && (player.isInCategory(CategoryType.HUMAN_MALL_CLASS) || player.isInCategory(CategoryType.ELF_MALL_CLASS)))
		{
			final PlayerClass classId = player.getPlayerClass();
			if ((classId == PlayerClass.WIZARD) || (classId == PlayerClass.SORCERER) || (classId == PlayerClass.NECROMANCER) || (classId == PlayerClass.WARLOCK))
			{
				htmltext = "30115-02.htm"; // fnClassList1
			}
			else if ((classId == PlayerClass.ELVEN_WIZARD) || (classId == PlayerClass.SPELLSINGER) || (classId == PlayerClass.ELEMENTAL_SUMMONER))
			{
				htmltext = "30115-12.htm"; // fnClassList2
			}
			else
			{
				htmltext = "30115-19.htm"; // fnYouAreFirstClass
			}
		}
		else
		{
			htmltext = "30115-20.htm"; // fnClassMismatch
		}
		return htmltext;
	}
	
	public static void main(String[] args)
	{
		new ElfHumanWizardChange2();
	}
}
