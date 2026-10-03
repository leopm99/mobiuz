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
package village_master.OrcChange1;

import com.l2journey.gameserver.data.enums.CategoryType;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.enums.creature.Race;
import com.l2journey.gameserver.model.actor.enums.player.PlayerClass;

import ai.AbstractNpcAI;

/**
 * Orc class transfer AI.
 * @author Adry_85
 */
public class OrcChange1 extends AbstractNpcAI
{
	// NPCs
	private static int[] NPCS =
	{
		30500, // Osborn
		30505, // Drikus
		30508, // Castor
		32097, // Finker
	};
	
	// Items
	private static final int SHADOW_ITEM_EXCHANGE_COUPON_D_GRADE = 8869;
	private static final int MARK_OF_RAIDER = 1592;
	private static final int KHAVATARI_TOTEM = 1615;
	private static final int MASK_OF_MEDIUM = 1631;
	
	private OrcChange1()
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
			case "30500-01.htm", "30500-02.htm", "30500-03.htm", "30500-04.htm", "30500-05.htm", "30500-06.htm", "30500-07.htm", "30500-08.htm", "30505-01.htm", "30505-02.htm", "30505-03.htm", "30505-04.htm", "30505-05.htm", "30505-06.htm", "30505-07.htm", "30505-08.htm", "30508-01.htm", "30508-02.htm", "30508-03.htm", "30508-04.htm", "30508-05.htm", "30508-06.htm", "30508-07.htm", "30508-08.htm", "32097-01.htm", "32097-02.htm", "32097-03.htm", "32097-04.htm", "32097-05.htm", "32097-06.htm", "32097-07.htm", "32097-08.htm": // high_prefect_osborn003f; high_prefect_osborn006fa; high_prefect_osborn007fa; high_prefect_osborn006fb; high_prefect_osborn007fb; high_prefect_osborn003m; high_prefect_osborn006ma; high_prefect_osborn007ma; high_prefect_drikus003f; high_prefect_drikus006fa; high_prefect_drikus007fa; high_prefect_drikus006fb; high_prefect_drikus007fb; high_prefect_drikus003m; high_prefect_drikus006ma; high_prefect_drikus007ma; high_prefect_cional003f; high_prefect_cional006fa; high_prefect_cional007fa; high_prefect_cional006fb; high_prefect_cional007fb; high_prefect_cional003m; high_prefect_cional006ma; high_prefect_cional007ma; high_prefect_finker003f; high_prefect_finker006fa; high_prefect_finker007fa; high_prefect_finker006fb; high_prefect_finker007fb; high_prefect_finker003m; high_prefect_finker006ma; high_prefect_finker007ma
			{
				htmltext = event;
				break;
			}
			case "45", "47", "50":
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
			htmltext = npc.getId() + "-09.htm"; // fnYouAreSecondClass
		}
		else if (player.isInCategory(CategoryType.THIRD_CLASS_GROUP))
		{
			htmltext = npc.getId() + "-10.htm"; // fnYouAreThirdClass
		}
		else if (player.isInCategory(CategoryType.FOURTH_CLASS_GROUP))
		{
			htmltext = "30500-24.htm"; // fnYouAreFourthClass
		}
		else if ((classId == 45) && (player.getPlayerClass() == PlayerClass.ORC_FIGHTER))
		{
			if (player.getLevel() < 20)
			{
				if (hasQuestItems(player, MARK_OF_RAIDER))
				{
					htmltext = npc.getId() + "-11.htm"; // fnLowLevel11
				}
				else
				{
					htmltext = npc.getId() + "-12.htm"; // fnLowLevelNoProof11
				}
			}
			else if (hasQuestItems(player, MARK_OF_RAIDER))
			{
				takeItems(player, MARK_OF_RAIDER, -1);
				player.setPlayerClass(45);
				player.setBaseClass(45);
				// SystemMessage and cast skill is done by setClassId
				player.broadcastUserInfo();
				giveItems(player, SHADOW_ITEM_EXCHANGE_COUPON_D_GRADE, 15);
				htmltext = npc.getId() + "-14.htm"; // fnAfterClassChange11
			}
			else
			{
				htmltext = npc.getId() + "-13.htm"; // fnNoProof11
			}
		}
		else if ((classId == 47) && (player.getPlayerClass() == PlayerClass.ORC_FIGHTER))
		{
			if (player.getLevel() < 20)
			{
				if (hasQuestItems(player, KHAVATARI_TOTEM))
				{
					htmltext = npc.getId() + "-15.htm"; // fnLowLevel12
				}
				else
				{
					htmltext = npc.getId() + "-16.htm"; // fnLowLevelNoProof12
				}
			}
			else if (hasQuestItems(player, KHAVATARI_TOTEM))
			{
				takeItems(player, KHAVATARI_TOTEM, -1);
				player.setPlayerClass(47);
				player.setBaseClass(47);
				// SystemMessage and cast skill is done by setClassId
				player.broadcastUserInfo();
				giveItems(player, SHADOW_ITEM_EXCHANGE_COUPON_D_GRADE, 15);
				htmltext = npc.getId() + "-18.htm"; // fnAfterClassChange12
			}
			else
			{
				htmltext = npc.getId() + "-17.htm"; // fnNoProof12
			}
		}
		else if ((classId == 50) && (player.getPlayerClass() == PlayerClass.ORC_MAGE))
		{
			if (player.getLevel() < 20)
			{
				if (hasQuestItems(player, MASK_OF_MEDIUM))
				{
					htmltext = npc.getId() + "-19.htm"; // fnLowLevel21
				}
				else
				{
					htmltext = npc.getId() + "-20.htm"; // fnLowLevelNoProof21
				}
			}
			else if (hasQuestItems(player, MASK_OF_MEDIUM))
			{
				takeItems(player, MASK_OF_MEDIUM, -1);
				player.setPlayerClass(50);
				player.setBaseClass(50);
				// SystemMessage and cast skill is done by setClassId
				player.broadcastUserInfo();
				giveItems(player, SHADOW_ITEM_EXCHANGE_COUPON_D_GRADE, 15);
				htmltext = npc.getId() + "-22.htm"; // fnAfterClassChange21
			}
			else
			{
				htmltext = npc.getId() + "-21.htm"; // fnNoProof21
			}
		}
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		String htmltext = null;
		if (player.getRace() == Race.ORC)
		{
			if (player.isInCategory(CategoryType.FIGHTER_GROUP))
			{
				htmltext = npc.getId() + "-01.htm"; // fnClassList1
			}
			else if (player.isInCategory(CategoryType.MAGE_GROUP))
			{
				htmltext = npc.getId() + "-06.htm"; // fnClassList2
			}
		}
		else
		{
			htmltext = npc.getId() + "-23.htm"; // fnClassMismatch
		}
		return htmltext;
	}
	
	public static void main(String[] args)
	{
		new OrcChange1();
	}
}
