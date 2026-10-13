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
package village_master.KamaelChange2;

import com.l2journey.gameserver.data.enums.CategoryType;
import com.l2journey.gameserver.data.xml.CategoryData;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.enums.creature.Race;
import com.l2journey.gameserver.model.actor.enums.player.PlayerClass;
import com.l2journey.gameserver.model.quest.QuestState;
import com.l2journey.gameserver.util.ArrayUtil;

import ai.AbstractNpcAI;
import quests.Q00064_CertifiedBerserker.Q00064_CertifiedBerserker;
import quests.Q00065_CertifiedSoulBreaker.Q00065_CertifiedSoulBreaker;
import quests.Q00066_CertifiedArbalester.Q00066_CertifiedArbalester;

/**
 * Kamael class transfer AI.
 * @author Adry_85
 */
public class KamaelChange2 extends AbstractNpcAI
{
	private static final String HTML_N_32145_02 = "32145-02.htm";
	private static final String HTML_N_32145_03 = "32145-03.htm";
	private static final String HTML_N_32145_10 = "32145-10.htm";
	private static final String HTML_N_32145_04 = "32145-04.htm";

	// NPCs
	private static int[] NPCS_MALE =
	{
		32146, // Valpor
		32205, // Aetonic
		32209, // Ferdinand
		32213, // Vitus
		32217, // Barta
		32221, // Brome
		32225, // Taine
		32229, // Hagel
		32233, // Zoldart
	};
	private static int[] NPCS_FEMALE =
	{
		32145, // Maynard
		32206, // Pieche
		32210, // Eddy
		32214, // Meldina
		32218, // Miya
		32222, // Liane
		32226, // Raula
		32230, // Ceci
		32234, // Nizer
	};
	
	// Items
	private static final int SHADOW_ITEM_EXCHANGE_COUPON_C_GRADE = 8870;
	private static final int ORKURUS_RECOMMENDATION = 9760;
	private static final int KAMAEL_INQUISITOR_MARK = 9782;
	private static final int SOUL_BREAKER_CERTIFICATE = 9806;
	
	private KamaelChange2()
	{
		addStartNpc(NPCS_MALE);
		addStartNpc(NPCS_FEMALE);
		addTalkId(NPCS_MALE);
		addTalkId(NPCS_FEMALE);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		switch (event)
		{
			case "32145-05.htm", "32145-06.htm", "32145-07.htm", "32145-08.htm", "32145-09.htm", "32145-11.htm", "32145-12.htm", "32145-13.htm", "32145-14.htm", "32145-15.htm": // master_all_kamael003t; master_all_kamael006ta; master_all_kamael007ta; master_all_kamael006msa; master_all_kamael007msa; master_all_kamael003w; master_all_kamael006wa; master_all_kamael007wa; master_all_kamael006fsa; master_all_kamael007fsa
			{
				htmltext = event;
				break;
			}
			case "127", "128", "129", "130":
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
		if (CategoryData.getInstance().isInCategory(CategoryType.KAMAEL_THIRD_CLASS_GROUP, classId))
		{
			if (player.isInCategory(CategoryType.KAMAEL_FIRST_CLASS_GROUP))
			{
				if (ArrayUtil.contains(NPCS_MALE, npc.getId()))
				{
					htmltext = HTML_N_32145_02; // master_all_kamael012b
				}
				else
				{
					htmltext = HTML_N_32145_03; // master_all_kamael012c
				}
			}
			else if (player.isInCategory(CategoryType.KAMAEL_THIRD_CLASS_GROUP))
			{
				if (ArrayUtil.contains(NPCS_MALE, npc.getId()))
				{
					htmltext = "32145-16.htm"; // master_all_kamael005b
				}
				else
				{
					htmltext = "32145-17.htm"; // master_all_kamael005c
				}
			}
			else if (player.isInCategory(CategoryType.KAMAEL_FOURTH_CLASS_GROUP))
			{
				if (ArrayUtil.contains(NPCS_MALE, npc.getId()))
				{
					htmltext = "32145-18.htm"; // master_all_kamael100b
				}
				else
				{
					htmltext = "32145-19.htm"; // master_all_kamael100c
				}
			}
			else if (player.getPlayerClass() == PlayerClass.TROOPER)
			{
				if (ArrayUtil.contains(NPCS_MALE, npc.getId()))
				{
					if (classId == 127)
					{
						final QuestState qs = player.getQuestState(Q00064_CertifiedBerserker.class.getSimpleName());
						if (player.getLevel() < 40)
						{
							if ((qs != null) && qs.isCompleted())
							{
								htmltext = "32145-20.htm"; // master_all_kamael008ta
							}
							else
							{
								htmltext = "32145-21.htm"; // master_all_kamael009ta
							}
						}
						else if ((qs == null) || !qs.isCompleted())
						{
							htmltext = "32145-22.htm"; // master_all_kamael010ta
						}
						else
						{
							takeItems(player, ORKURUS_RECOMMENDATION, -1);
							player.setPlayerClass(127);
							player.setBaseClass(127);
							// SystemMessage and cast skill is done by setClassId
							player.broadcastUserInfo();
							giveItems(player, SHADOW_ITEM_EXCHANGE_COUPON_C_GRADE, 15);
							htmltext = "32145-23.htm"; // master_all_kamael011ta
						}
					}
					else if (classId == 128)
					{
						final QuestState qs = player.getQuestState(Q00065_CertifiedSoulBreaker.class.getSimpleName());
						if (player.getLevel() < 40)
						{
							if ((qs != null) && qs.isCompleted())
							{
								htmltext = "32145-24.htm"; // master_all_kamael008msa
							}
							else
							{
								htmltext = "32145-25.htm"; // master_all_kamael009msa
							}
						}
						else if ((qs == null) || !qs.isCompleted())
						{
							htmltext = "32145-26.htm"; // master_all_kamael010msa
						}
						else
						{
							takeItems(player, SOUL_BREAKER_CERTIFICATE, -1);
							player.setPlayerClass(128);
							player.setBaseClass(128);
							// SystemMessage and cast skill is done by setClassId
							player.broadcastUserInfo();
							giveItems(player, SHADOW_ITEM_EXCHANGE_COUPON_C_GRADE, 15);
							htmltext = "32145-27.htm"; // master_all_kamael011msa
						}
					}
				}
				else
				{
					htmltext = HTML_N_32145_10; // master_all_kamael002c
				}
			}
			else if (player.getPlayerClass() == PlayerClass.WARDER)
			{
				if (ArrayUtil.contains(NPCS_MALE, npc.getId()))
				{
					htmltext = HTML_N_32145_04; // master_all_kamael002b
				}
				else
				{
					if (classId == 129)
					{
						final QuestState qs = player.getQuestState(Q00065_CertifiedSoulBreaker.class.getSimpleName());
						if (player.getLevel() < 40)
						{
							if ((qs != null) && qs.isCompleted())
							{
								htmltext = "32145-28.htm"; // master_all_kamael008fsa
							}
							else
							{
								htmltext = "32145-29.htm"; // master_all_kamael009fsa
							}
						}
						else if ((qs == null) || !qs.isCompleted())
						{
							htmltext = "32145-30.htm"; // master_all_kamael010fsa
						}
						else
						{
							takeItems(player, SOUL_BREAKER_CERTIFICATE, -1);
							player.setPlayerClass(129);
							player.setBaseClass(129);
							// SystemMessage and cast skill is done by setClassId
							player.broadcastUserInfo();
							giveItems(player, SHADOW_ITEM_EXCHANGE_COUPON_C_GRADE, 15);
							htmltext = "32145-31.htm"; // master_all_kamael011fsa
						}
					}
					else if (classId == 130)
					{
						final QuestState qs = player.getQuestState(Q00066_CertifiedArbalester.class.getSimpleName());
						if (player.getLevel() < 40)
						{
							if ((qs != null) && qs.isCompleted())
							{
								htmltext = "32145-32.htm"; // master_all_kamael008wa
							}
							else
							{
								htmltext = "32145-33.htm"; // master_all_kamael009wa
							}
						}
						else if ((qs == null) || !qs.isCompleted())
						{
							htmltext = "32145-34.htm"; // master_all_kamael010wa
						}
						else
						{
							takeItems(player, KAMAEL_INQUISITOR_MARK, -1);
							player.setPlayerClass(130);
							player.setBaseClass(130);
							// SystemMessage and cast skill is done by setClassId
							player.broadcastUserInfo();
							giveItems(player, SHADOW_ITEM_EXCHANGE_COUPON_C_GRADE, 15);
							htmltext = "32145-35.htm"; // master_all_kamael011wa
						}
					}
				}
			}
		}
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		String htmltext = null;
		if (player.getRace() != Race.KAMAEL)
		{
			htmltext = "32145-01.htm"; // master_all_kamael002a
		}
		else if (player.isInCategory(CategoryType.KAMAEL_FIRST_CLASS_GROUP))
		{
			if (player.getPlayerClass() == PlayerClass.MALE_SOLDIER)
			{
				htmltext = HTML_N_32145_02; // master_all_kamael012b
			}
			else if (player.getPlayerClass() == PlayerClass.FEMALE_SOLDIER)
			{
				htmltext = HTML_N_32145_03; // master_all_kamael012c
			}
		}
		else if (player.isInCategory(CategoryType.KAMAEL_SECOND_CLASS_GROUP))
		{
			if (ArrayUtil.contains(NPCS_MALE, npc.getId()))
			{
				if (player.isInCategory(CategoryType.KAMAEL_FEMALE_MAIN_OCCUPATION))
				{
					htmltext = HTML_N_32145_04; // master_all_kamael002b
					return htmltext;
				}
				
				if (player.getPlayerClass() == PlayerClass.TROOPER)
				{
					htmltext = "32145-05.htm"; // master_all_kamael003t
				}
				else if (player.getPlayerClass() == PlayerClass.WARDER)
				{
					htmltext = HTML_N_32145_02; // master_all_kamael012b
				}
			}
			else
			{
				if (player.isInCategory(CategoryType.KAMAEL_MALE_MAIN_OCCUPATION))
				{
					htmltext = HTML_N_32145_10; // master_all_kamael002c
					return htmltext;
				}
				
				if (player.getPlayerClass() == PlayerClass.TROOPER)
				{
					htmltext = HTML_N_32145_03; // master_all_kamael012c
				}
				else if (player.getPlayerClass() == PlayerClass.WARDER)
				{
					htmltext = "32145-11.htm"; // master_all_kamael003w
				}
			}
		}
		else if (player.isInCategory(CategoryType.KAMAEL_THIRD_CLASS_GROUP))
		{
			if (ArrayUtil.contains(NPCS_MALE, npc.getId()))
			{
				if (player.isInCategory(CategoryType.KAMAEL_MALE_MAIN_OCCUPATION))
				{
					htmltext = "32145-16.htm"; // master_all_kamael005b
				}
				else
				{
					htmltext = HTML_N_32145_04; // master_all_kamael002b
				}
			}
			else
			{
				if (player.isInCategory(CategoryType.KAMAEL_FEMALE_MAIN_OCCUPATION))
				{
					htmltext = "32145-17.htm"; // master_all_kamael005c
				}
				else
				{
					htmltext = HTML_N_32145_10; // master_all_kamael002c
				}
			}
		}
		else if (player.isInCategory(CategoryType.KAMAEL_FOURTH_CLASS_GROUP))
		{
			if (ArrayUtil.contains(NPCS_MALE, npc.getId()))
			{
				if (player.isInCategory(CategoryType.KAMAEL_MALE_MAIN_OCCUPATION))
				{
					htmltext = "32145-18.htm"; // master_all_kamael100b
				}
				else
				{
					htmltext = HTML_N_32145_04; // master_all_kamael002b
				}
			}
			else
			{
				if (player.isInCategory(CategoryType.KAMAEL_FEMALE_MAIN_OCCUPATION))
				{
					htmltext = "32145-19.htm"; // master_all_kamael100c
				}
				else
				{
					htmltext = HTML_N_32145_10; // master_all_kamael002c
				}
			}
		}
		return htmltext;
	}
	
	public static void main(String[] args)
	{
		new KamaelChange2();
	}
}
