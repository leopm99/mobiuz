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
package village_master.DwarfWarehouseChange1;

import com.l2journey.gameserver.data.enums.CategoryType;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.enums.player.PlayerClass;

import ai.AbstractNpcAI;

/**
 * Dwarf class transfer AI.
 * @author Adry_85
 */
public class DwarfWarehouseChange1 extends AbstractNpcAI
{
	// NPCs
	private static int[] NPCS =
	{
		30498, // Moke
		30503, // Rikadio
		30594, // Ranspo
		32092, // Alder
	};
	
	// Items
	private static final int SHADOW_ITEM_EXCHANGE_COUPON_D_GRADE = 8869;
	private static final int RING_OF_RAVEN = 1642;
	// Class
	private static final int SCAVENGER = 54;
	
	private DwarfWarehouseChange1()
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
			case "30498-01.htm", "30498-02.htm", "30498-03.htm", "30498-04.htm", "30503-01.htm", "30503-02.htm", "30503-03.htm", "30503-04.htm", "30594-01.htm", "30594-02.htm", "30594-03.htm", "30594-04.htm", "32092-01.htm", "32092-02.htm", "32092-03.htm", "32092-04.htm": // warehouse_chief_moke003f; warehouse_chief_moke006fa; warehouse_chief_moke007fa; warehouse_chief_moke006fb; warehouse_chief_rikadio003f; warehouse_chief_rikadio006fa; warehouse_chief_rikadio007fa; warehouse_chief_rikadio006fb; warehouse_chief_ranspo003f; warehouse_chief_ranspo006fa; warehouse_chief_ranspo007fa; warehouse_chief_ranspo006fb; warehouse_chief_older003f; warehouse_chief_older006fa; warehouse_chief_older007fa; warehouse_chief_older006fb
			{
				htmltext = event;
				break;
			}
			case "54":
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
			htmltext = npc.getId() + "-06.htm"; // fnYouAreSecondClass
		}
		else if (player.isInCategory(CategoryType.THIRD_CLASS_GROUP))
		{
			htmltext = npc.getId() + "-07.htm"; // fnYouAreThirdClass
		}
		else if (player.isInCategory(CategoryType.FOURTH_CLASS_GROUP))
		{
			htmltext = "30498-12.htm"; // fnYouAreFourthClass
		}
		else if ((classId == SCAVENGER) && (player.getPlayerClass() == PlayerClass.DWARVEN_FIGHTER))
		{
			if (player.getLevel() < 20)
			{
				if (hasQuestItems(player, RING_OF_RAVEN))
				{
					htmltext = npc.getId() + "-08.htm"; // fnLowLevel11
				}
				else
				{
					htmltext = npc.getId() + "-09.htm"; // fnLowLevelNoProof11
				}
			}
			else if (hasQuestItems(player, RING_OF_RAVEN))
			{
				takeItems(player, RING_OF_RAVEN, -1);
				player.setPlayerClass(SCAVENGER);
				player.setBaseClass(SCAVENGER);
				// SystemMessage and cast skill is done by setClassId
				player.broadcastUserInfo();
				giveItems(player, SHADOW_ITEM_EXCHANGE_COUPON_D_GRADE, 15);
				htmltext = npc.getId() + "-10.htm"; // fnAfterClassChange11
			}
			else
			{
				htmltext = npc.getId() + "-11.htm"; // fnNoProof11
			}
		}
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		String htmltext = null;
		if (player.isInCategory(CategoryType.BOUNTY_HUNTER_GROUP))
		{
			htmltext = npc.getId() + "-01.htm"; // fnClassList1
		}
		else
		{
			htmltext = npc.getId() + "-05.htm"; // fnClassMismatch
		}
		return htmltext;
	}
	
	public static void main(String[] args)
	{
		new DwarfWarehouseChange1();
	}
}
