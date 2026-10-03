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
package village_master.DwarfBlacksmithChange1;

import com.l2journey.gameserver.data.enums.CategoryType;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.enums.player.PlayerClass;

import ai.AbstractNpcAI;

/**
 * Dwarf class transfer AI.
 * @author Adry_85
 */
public class DwarfBlacksmithChange1 extends AbstractNpcAI
{
	// NPCs
	private static int[] NPCS =
	{
		30499, // Tapoy
		30504, // Mendio
		30595, // Opix
		32093, // Bolin
	};
	
	// Items
	private static final int SHADOW_ITEM_EXCHANGE_COUPON_D_GRADE = 8869;
	private static final int FINAL_PASS_CERTIFICATE = 1635;
	// Class
	private static final int ARTISAN = 56;
	
	private DwarfBlacksmithChange1()
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
			case "30499-01.htm", "30499-02.htm", "30499-03.htm", "30499-04.htm", "30504-01.htm", "30504-02.htm", "30504-03.htm", "30504-04.htm", "30595-01.htm", "30595-02.htm", "30595-03.htm", "30595-04.htm", "32093-01.htm", "32093-02.htm", "32093-03.htm", "32093-04.htm": // head_blacksmith_tapoy003f; head_blacksmith_tapoy006fa; head_blacksmith_tapoy007fa; head_blacksmith_tapoy006fb; head_blacksmith_mendio003f; head_blacksmith_mendio006fa; head_blacksmith_mendio007fa; head_blacksmith_mendio006fb; head_blacksmith_opix003f; head_blacksmith_opix006fa; head_blacksmith_opix007fa; head_blacksmith_opix006fb; head_blacksmith_boillin003f; head_blacksmith_boillin006fa; head_blacksmith_boillin007fa; head_blacksmith_boillin006fb
			{
				htmltext = event;
				break;
			}
			case "56":
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
			htmltext = "30499-12.htm"; // fnYouAreFourthClass
		}
		else if ((classId == ARTISAN) && (player.getPlayerClass() == PlayerClass.DWARVEN_FIGHTER))
		{
			if (player.getLevel() < 20)
			{
				if (hasQuestItems(player, FINAL_PASS_CERTIFICATE))
				{
					htmltext = npc.getId() + "-08.htm"; // fnLowLevel11
				}
				else
				{
					htmltext = npc.getId() + "-09.htm"; // fnLowLevelNoProof11
				}
			}
			else if (hasQuestItems(player, FINAL_PASS_CERTIFICATE))
			{
				takeItems(player, FINAL_PASS_CERTIFICATE, -1);
				player.setPlayerClass(ARTISAN);
				player.setBaseClass(ARTISAN);
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
		if (player.isInCategory(CategoryType.WARSMITH_GROUP))
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
		new DwarfBlacksmithChange1();
	}
}
