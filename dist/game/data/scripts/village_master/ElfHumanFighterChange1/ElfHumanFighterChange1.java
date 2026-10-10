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
package village_master.ElfHumanFighterChange1;

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
public class ElfHumanFighterChange1 extends AbstractNpcAI
{
	// NPCs
	private static int[] NPCS =
	{
		30066, // Pabris
		30288, // Rains
		30373, // Ramos
		32094, // Schule
	};
	
	// Items
	private static final int SHADOW_ITEM_EXCHANGE_COUPON_D_GRADE = 8869;
	private static final int MEDALLION_OF_WARRIOR = 1145;
	private static final int SWORD_OF_RITUAL = 1161;
	private static final int BEZIQUES_RECOMMENDATION = 1190;
	private static final int ELVEN_KNIGHT_BROOCH = 1204;
	private static final int REISAS_RECOMMENDATION = 1217;
	// Classes
	private static final int WARRIOR = 1;
	private static final int KNIGHT = 4;
	private static final int ROGUE = 7;
	private static final int ELVEN_KNIGHT = 19;
	private static final int ELVEN_SCOUT = 22;
	
	private ElfHumanFighterChange1()
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
			case "30066-01.htm", "30066-02.htm", "30066-03.htm", "30066-04.htm", "30066-05.htm", "30066-06.htm", "30066-07.htm", "30066-08.htm", "30066-09.htm", "30066-10.htm", "30066-11.htm", "30066-12.htm", "30066-13.htm", "30066-14.htm", "30066-15.htm", "30066-16.htm", "30066-17.htm", "30288-01.htm", "30288-02.htm", "30288-03.htm", "30288-04.htm", "30288-05.htm", "30288-06.htm", "30288-07.htm", "30288-08.htm", "30288-09.htm", "30288-10.htm", "30288-11.htm", "30288-12.htm", "30288-13.htm", "30288-14.htm", "30288-15.htm", "30288-16.htm", "30288-17.htm", "30373-01.htm", "30373-02.htm", "30373-03.htm", "30373-04.htm", "30373-05.htm", "30373-06.htm", "30373-07.htm", "30373-08.htm", "30373-09.htm", "30373-10.htm", "30373-11.htm", "30373-12.htm", "30373-13.htm", "30373-14.htm", "30373-15.htm", "30373-16.htm", "30373-17.htm", "32094-01.htm", "32094-02.htm", "32094-03.htm", "32094-04.htm", "32094-05.htm", "32094-06.htm", "32094-07.htm", "32094-08.htm", "32094-09.htm", "32094-10.htm", "32094-11.htm", "32094-12.htm", "32094-13.htm", "32094-14.htm", "32094-15.htm", "32094-16.htm", "32094-17.htm": // pabris003h; pabris006ha; pabris007ha; pabris007hat; pabris006hb; pabris007hb; pabris007hbt; pabris006hc; pabris007hc; pabris007hct; pabris003e; pabris006ea; pabris007ea; pabris007eat; pabris006eb; pabris007eb; pabris007ebt; master_rains003h; master_rains006ha; master_rains007ha; master_rains007hat; master_rains006hb; master_rains007hb; master_rains007hbt; master_rains006hc; master_rains007hc; master_rains007hct; master_rains003e; master_rains006ea; master_rains007ea; master_rains007eat; master_rains006eb; master_rains007eb; master_rains007ebt; grandmaster_ramos003h; grandmaster_ramos006ha; grandmaster_ramos007ha; grandmaster_ramos007hat; grandmaster_ramos006hb; grandmaster_ramos007hb; grandmaster_ramos007hbt; grandmaster_ramos006hc; grandmaster_ramos007hc; grandmaster_ramos007hct; grandmaster_ramos003e; grandmaster_ramos006ea; grandmaster_ramos007ea; grandmaster_ramos007eat; grandmaster_ramos006eb; grandmaster_ramos007eb; grandmaster_ramos007ebt; grandmaster_shull003h; grandmaster_shull006ha; grandmaster_shull007ha; grandmaster_shull007hat; grandmaster_shull006hb; grandmaster_shull007hb; grandmaster_shull007hbt; grandmaster_shull006hc; grandmaster_shull007hc; grandmaster_shull007hct; grandmaster_shull003e; grandmaster_shull006ea; grandmaster_shull007ea; grandmaster_shull007eat; grandmaster_shull006eb; grandmaster_shull007eb; grandmaster_shull007ebt
			{
				htmltext = event;
				break;
			}
			case "1", "4", "7", "19", "22":
			{
				htmltext = ClassChangeRequested(player, npc, Integer.parseInt(event));
				break;
			}
			default:
			{
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
			htmltext = npc.getId() + "-19.htm"; // fnYouAreSecondClass
		}
		else if (player.isInCategory(CategoryType.THIRD_CLASS_GROUP))
		{
			htmltext = npc.getId() + "-20.htm"; // fnYouAreThirdClass
		}
		else if (player.isInCategory(CategoryType.FOURTH_CLASS_GROUP))
		{
			htmltext = "30066-41.htm"; // fnYouAreFourthClass
		}
		else if ((classId == WARRIOR) && (player.getPlayerClass() == PlayerClass.FIGHTER))
		{
			if (player.getLevel() < 20)
			{
				if (hasQuestItems(player, MEDALLION_OF_WARRIOR))
				{
					htmltext = npc.getId() + "-21.htm"; // fnLowLevel11
				}
				else
				{
					htmltext = npc.getId() + "-22.htm"; // fnLowLevelNoProof11
				}
			}
			else if (hasQuestItems(player, MEDALLION_OF_WARRIOR))
			{
				takeItems(player, MEDALLION_OF_WARRIOR, -1);
				player.setPlayerClass(WARRIOR);
				player.setBaseClass(WARRIOR);
				// SystemMessage and cast skill is done by setClassId
				player.broadcastUserInfo();
				giveItems(player, SHADOW_ITEM_EXCHANGE_COUPON_D_GRADE, 15);
				htmltext = npc.getId() + "-23.htm"; // fnAfterClassChange11
			}
			else
			{
				htmltext = npc.getId() + "-24.htm"; // fnNoProof11
			}
		}
		else if ((classId == KNIGHT) && (player.getPlayerClass() == PlayerClass.FIGHTER))
		{
			if (player.getLevel() < 20)
			{
				if (hasQuestItems(player, SWORD_OF_RITUAL))
				{
					htmltext = npc.getId() + "-25.htm"; // fnLowLevel12
				}
				else
				{
					htmltext = npc.getId() + "-26.htm"; // fnLowLevelNoProof12
				}
			}
			else if (hasQuestItems(player, SWORD_OF_RITUAL))
			{
				takeItems(player, SWORD_OF_RITUAL, -1);
				player.setPlayerClass(KNIGHT);
				player.setBaseClass(KNIGHT);
				// SystemMessage and cast skill is done by setClassId
				player.broadcastUserInfo();
				giveItems(player, SHADOW_ITEM_EXCHANGE_COUPON_D_GRADE, 15);
				htmltext = npc.getId() + "-27.htm"; // fnAfterClassChange12
			}
			else
			{
				htmltext = npc.getId() + "-28.htm"; // fnNoProof12
			}
		}
		else if ((classId == ROGUE) && (player.getPlayerClass() == PlayerClass.FIGHTER))
		{
			if (player.getLevel() < 20)
			{
				if (hasQuestItems(player, BEZIQUES_RECOMMENDATION))
				{
					htmltext = npc.getId() + "-29.htm"; // fnLowLevel13
				}
				else
				{
					htmltext = npc.getId() + "-30.htm"; // fnLowLevelNoProof13
				}
			}
			else if (hasQuestItems(player, BEZIQUES_RECOMMENDATION))
			{
				takeItems(player, BEZIQUES_RECOMMENDATION, -1);
				player.setPlayerClass(ROGUE);
				player.setBaseClass(ROGUE);
				// SystemMessage and cast skill is done by setClassId
				player.broadcastUserInfo();
				giveItems(player, SHADOW_ITEM_EXCHANGE_COUPON_D_GRADE, 15);
				htmltext = npc.getId() + "-31.htm"; // fnAfterClassChange13
			}
			else
			{
				htmltext = npc.getId() + "-32.htm"; // fnNoProof13
			}
		}
		else if ((classId == ELVEN_KNIGHT) && (player.getPlayerClass() == PlayerClass.ELVEN_FIGHTER))
		{
			if (player.getLevel() < 20)
			{
				if (hasQuestItems(player, ELVEN_KNIGHT_BROOCH))
				{
					htmltext = npc.getId() + "-33.htm"; // fnLowLevel21
				}
				else
				{
					htmltext = npc.getId() + "-34.htm"; // fnLowLevelNoProof21
				}
			}
			else if (hasQuestItems(player, ELVEN_KNIGHT_BROOCH))
			{
				takeItems(player, ELVEN_KNIGHT_BROOCH, -1);
				player.setPlayerClass(ELVEN_KNIGHT);
				player.setBaseClass(ELVEN_KNIGHT);
				// SystemMessage and cast skill is done by setClassId
				player.broadcastUserInfo();
				giveItems(player, SHADOW_ITEM_EXCHANGE_COUPON_D_GRADE, 15);
				htmltext = npc.getId() + "-35.htm"; // fnAfterClassChange21
			}
			else
			{
				htmltext = npc.getId() + "-36.htm"; // fnNoProof21
			}
		}
		else if ((classId == ELVEN_SCOUT) && (player.getPlayerClass() == PlayerClass.ELVEN_FIGHTER))
		{
			if (player.getLevel() < 20)
			{
				if (hasQuestItems(player, REISAS_RECOMMENDATION))
				{
					htmltext = npc.getId() + "-37.htm"; // fnLowLevel22
				}
				else
				{
					htmltext = npc.getId() + "-38.htm"; // fnLowLevelNoProof22
				}
			}
			else if (hasQuestItems(player, REISAS_RECOMMENDATION))
			{
				takeItems(player, REISAS_RECOMMENDATION, -1);
				player.setPlayerClass(ELVEN_SCOUT);
				player.setBaseClass(ELVEN_SCOUT);
				// SystemMessage and cast skill is done by setClassId
				player.broadcastUserInfo();
				giveItems(player, SHADOW_ITEM_EXCHANGE_COUPON_D_GRADE, 15);
				htmltext = npc.getId() + "-39.htm"; // fnAfterClassChange22
			}
			else
			{
				htmltext = npc.getId() + "-40.htm"; // fnNoProof22
			}
		}
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		String htmltext = null;
		final Race playerRace = player.getRace();
		if (player.isInCategory(CategoryType.FIGHTER_GROUP) && ((playerRace == Race.HUMAN) || (playerRace == Race.ELF)))
		{
			if (playerRace == Race.HUMAN)
			{
				htmltext = npc.getId() + "-01.htm"; // fnClassList1
			}
			else
			{
				htmltext = npc.getId() + "-11.htm"; // fnClassList2
			}
		}
		else
		{
			htmltext = npc.getId() + "-18.htm"; // fnClassMismatch
		}
		return htmltext;
	}
	
	public static void main(String[] args)
	{
		new ElfHumanFighterChange1();
	}
}
