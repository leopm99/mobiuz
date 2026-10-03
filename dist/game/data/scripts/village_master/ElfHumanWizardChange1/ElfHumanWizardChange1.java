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
package village_master.ElfHumanWizardChange1;

import com.l2journey.gameserver.data.enums.CategoryType;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.enums.creature.Race;
import com.l2journey.gameserver.model.actor.enums.player.PlayerClass;

import ai.AbstractNpcAI;

/**
 * Elf Human class transfer AI
 * @author Adry_85
 */
public class ElfHumanWizardChange1 extends AbstractNpcAI
{
	// NPCs
	private static int[] NPCS =
	{
		30037, // Levian
		30070, // Sylvain
		30289, // Raymond
		32095, // Marie
		32098, // Celes
	};
	
	// Items
	private static final int SHADOW_ITEM_EXCHANGE_COUPON_D_GRADE = 8869;
	private static final int MARK_OF_FAITH = 1201;
	private static final int ETERNITY_DIAMOND = 1230;
	private static final int LEAF_OF_ORACLE = 1235;
	private static final int BEAD_OF_SEASON = 1292;
	// Classes
	private static final int WIZARD = 11;
	private static final int CLERIC = 15;
	private static final int ELVEN_WIZARD = 26;
	private static final int ORACLE = 29;
	
	private ElfHumanWizardChange1()
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
			case "30037-01.htm", "30037-02.htm", "30037-03.htm", "30037-04.htm", "30037-05.htm", "30037-06.htm", "30037-07.htm", "30037-08.htm", "30037-09.htm", "30037-10.htm", "30037-11.htm", "30037-12.htm", "30037-13.htm", "30037-14.htm", "30070-01.htm", "30070-02.htm", "30070-03.htm", "30070-04.htm", "30070-05.htm", "30070-06.htm", "30070-07.htm", "30070-08.htm", "30070-09.htm", "30070-10.htm", "30070-11.htm", "30070-12.htm", "30070-13.htm", "30070-14.htm", "30289-01.htm", "30289-02.htm", "30289-03.htm", "30289-04.htm", "30289-05.htm", "30289-06.htm", "30289-07.htm", "30289-08.htm", "30289-09.htm", "30289-10.htm", "30289-11.htm", "30289-12.htm", "30289-13.htm", "30289-14.htm", "32095-01.htm", "32095-02.htm", "32095-03.htm", "32095-04.htm", "32095-05.htm", "32095-06.htm", "32095-07.htm", "32095-08.htm", "32095-09.htm", "32095-10.htm", "32095-11.htm", "32095-12.htm", "32095-13.htm", "32095-14.htm", "32098-01.htm", "32098-02.htm", "32098-03.htm", "32098-04.htm", "32098-05.htm", "32098-06.htm", "32098-07.htm", "32098-08.htm", "32098-09.htm", "32098-10.htm", "32098-11.htm", "32098-12.htm", "32098-13.htm", "32098-14.htm": // levian003h; levian006ha; levian007ha; levian007hat; levian006hb; levian007hb; levian007hbt; levian003e; levian006ea; levian007ea; levian007eat; levian006eb; levian007eb; levian007ebt; sylvain003h; sylvain006ha; sylvain007ha; sylvain007hat; sylvain006hb; sylvain007hb; sylvain007hbt; sylvain003e; sylvain006ea; sylvain007ea; sylvain007eat; sylvain006eb; sylvain007eb; sylvain007ebt; bishop_raimund003h; bishop_raimund006ha; bishop_raimund007ha; bishop_raimund007hat; bishop_raimund006hb; bishop_raimund007hb; bishop_raimund007hbt; bishop_raimund003e; bishop_raimund006ea; bishop_raimund007ea; bishop_raimund007eat; bishop_raimund006eb; bishop_raimund007eb; bishop_raimund007ebt; highpriest_mattew003h; highpriest_mattew006ha; highpriest_mattew007ha; highpriest_mattew007hat; highpriest_mattew006hb; highpriest_mattew007hb; highpriest_mattew007hbt; highpriest_mattew003e; highpriest_mattew006ea; highpriest_mattew007ea; highpriest_mattew007eat; highpriest_mattew006eb; highpriest_mattew007eb; highpriest_mattew007ebt; grandmagister_celes003h; grandmagister_celes006ha; grandmagister_celes007ha; grandmagister_celes007hat; grandmagister_celes006hb; grandmagister_celes007hb; grandmagister_celes007hbt; grandmagister_celes003e; grandmagister_celes006ea; grandmagister_celes007ea; grandmagister_celes007eat; grandmagister_celes006eb; grandmagister_celes007eb; grandmagister_celes007ebt
			{
				htmltext = event;
				break;
			}
			case "11", "15", "26", "29":
			{
				htmltext = ClassChangeRequested(player, npc, Integer.parseInt(event));
				break;
			}
		}
		return htmltext;
	}
	
	private String ClassChangeRequested(Player player, Npc npc, int classId)
	{
		String htmltext = null;
		if (player.isInCategory(CategoryType.SECOND_CLASS_GROUP))
		{
			htmltext = npc.getId() + "-16.htm"; // fnYouAreSecondClass
		}
		else if (player.isInCategory(CategoryType.THIRD_CLASS_GROUP))
		{
			htmltext = npc.getId() + "-17.htm"; // fnYouAreThirdClass
		}
		else if (player.isInCategory(CategoryType.FOURTH_CLASS_GROUP))
		{
			htmltext = "30037-34.htm"; // fnYouAreFourthClass
		}
		else if ((classId == WIZARD) && (player.getPlayerClass() == PlayerClass.MAGE))
		{
			if (player.getLevel() < 20)
			{
				if (hasQuestItems(player, BEAD_OF_SEASON))
				{
					htmltext = npc.getId() + "-18.htm"; // fnLowLevel11
				}
				else
				{
					htmltext = npc.getId() + "-19.htm"; // fnLowLevelNoProof11
				}
			}
			else if (hasQuestItems(player, BEAD_OF_SEASON))
			{
				takeItems(player, BEAD_OF_SEASON, -1);
				player.setPlayerClass(WIZARD);
				player.setBaseClass(WIZARD);
				// SystemMessage and cast skill is done by setClassId
				player.broadcastUserInfo();
				giveItems(player, SHADOW_ITEM_EXCHANGE_COUPON_D_GRADE, 15);
				htmltext = npc.getId() + "-20.htm"; // fnAfterClassChange11
			}
			else
			{
				htmltext = npc.getId() + "-21.htm"; // fnNoProof11
			}
		}
		else if ((classId == CLERIC) && (player.getPlayerClass() == PlayerClass.MAGE))
		{
			if (player.getLevel() < 20)
			{
				if (hasQuestItems(player, MARK_OF_FAITH))
				{
					htmltext = npc.getId() + "-22.htm"; // fnLowLevel12
				}
				else
				{
					htmltext = npc.getId() + "-23.htm"; // fnLowLevelNoProof12
				}
			}
			else if (hasQuestItems(player, MARK_OF_FAITH))
			{
				takeItems(player, MARK_OF_FAITH, -1);
				player.setPlayerClass(CLERIC);
				player.setBaseClass(CLERIC);
				// SystemMessage and cast skill is done by setClassId
				player.broadcastUserInfo();
				giveItems(player, SHADOW_ITEM_EXCHANGE_COUPON_D_GRADE, 15);
				htmltext = npc.getId() + "-24.htm"; // fnAfterClassChange12
			}
			else
			{
				htmltext = npc.getId() + "-25.htm"; // fnNoProof12
			}
		}
		else if ((classId == ELVEN_WIZARD) && (player.getPlayerClass() == PlayerClass.ELVEN_MAGE))
		{
			if (player.getLevel() < 20)
			{
				if (hasQuestItems(player, ETERNITY_DIAMOND))
				{
					htmltext = npc.getId() + "-26.htm"; // fnLowLevel21
				}
				else
				{
					htmltext = npc.getId() + "-27.htm"; // fnLowLevelNoProof21
				}
			}
			else if (hasQuestItems(player, ETERNITY_DIAMOND))
			{
				takeItems(player, ETERNITY_DIAMOND, -1);
				player.setPlayerClass(ELVEN_WIZARD);
				player.setBaseClass(ELVEN_WIZARD);
				// SystemMessage and cast skill is done by setClassId
				player.broadcastUserInfo();
				giveItems(player, SHADOW_ITEM_EXCHANGE_COUPON_D_GRADE, 15);
				htmltext = npc.getId() + "-28.htm"; // fnAfterClassChange21
			}
			else
			{
				htmltext = npc.getId() + "-29.htm"; // fnNoProof21
			}
		}
		else if ((classId == ORACLE) && (player.getPlayerClass() == PlayerClass.ELVEN_MAGE))
		{
			if (player.getLevel() < 20)
			{
				if (hasQuestItems(player, LEAF_OF_ORACLE))
				{
					htmltext = npc.getId() + "-30.htm"; // fnLowLevel22
				}
				else
				{
					htmltext = npc.getId() + "-31.htm"; // fnLowLevelNoProof22
				}
			}
			else if (hasQuestItems(player, LEAF_OF_ORACLE))
			{
				takeItems(player, LEAF_OF_ORACLE, -1);
				player.setPlayerClass(ORACLE);
				player.setBaseClass(ORACLE);
				// SystemMessage and cast skill is done by setClassId
				player.broadcastUserInfo();
				giveItems(player, SHADOW_ITEM_EXCHANGE_COUPON_D_GRADE, 15);
				htmltext = npc.getId() + "-32.htm"; // fnAfterClassChange22
			}
			else
			{
				htmltext = npc.getId() + "-33.htm"; // fnNoProof22
			}
		}
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		String htmltext = null;
		final Race playerRace = player.getRace();
		if (player.isInCategory(CategoryType.MAGE_GROUP) && ((playerRace == Race.HUMAN) || (playerRace == Race.ELF)))
		{
			if (playerRace == Race.HUMAN)
			{
				htmltext = npc.getId() + "-01.htm"; // fnClassList1
			}
			else
			{
				htmltext = npc.getId() + "-08.htm"; // fnClassList2
			}
		}
		else
		{
			htmltext = npc.getId() + "-15.htm"; // fnClassMismatch
		}
		return htmltext;
	}
	
	public static void main(String[] args)
	{
		new ElfHumanWizardChange1();
	}
}
