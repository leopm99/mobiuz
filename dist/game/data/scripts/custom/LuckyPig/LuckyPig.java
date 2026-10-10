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
package custom.LuckyPig;

import java.util.ArrayList;
import java.util.List;

import com.l2journey.Config;
import com.l2journey.EventsConfig;
import com.l2journey.commons.util.Rnd;
import com.l2journey.gameserver.ai.Intention;
import com.l2journey.gameserver.managers.GlobalVariablesManager;
import com.l2journey.gameserver.managers.MapRegionManager;
import com.l2journey.gameserver.model.World;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.item.instance.Item;
import com.l2journey.gameserver.model.itemcontainer.Inventory;
import com.l2journey.gameserver.model.skill.holders.SkillHolder;
import com.l2journey.gameserver.network.NpcStringId;
import com.l2journey.gameserver.network.enums.ChatType;
import com.l2journey.gameserver.network.serverpackets.MagicSkillUse;
import com.l2journey.gameserver.network.serverpackets.NpcSay;
import com.l2journey.gameserver.util.ArrayUtil;

import ai.AbstractNpcAI;

/**
 * Lucky Pig AI.
 * @author St3eT, KingHanker, Zoinha
 */
public final class LuckyPig extends AbstractNpcAI
{
	// NPC
	private static final int LUCKY_PIG_WINGLESS = 2502;
	private static final int LUCKY_PIG_WINGLESS_GOLD = 2503;
	private static final int LUCKY_PIG_LOW = 18664;
	private static final int LUCKY_PIG_MEDIUM = 18665;
	private static final int LUCKY_PIG_TOP = 18666;
	//@formatter:off
	private static final int[] TRIGGER_MOBS_LOW =
	{
		// Enchanted Valley
		20589, 20590, 20591, 20592, 20593, 20594, 20595, 20596, 20597, 20598, 20599
	};
	private static final int[] TRIGGER_MOBS_MEDIUM =
	{
		// Forest of the Dead, Valley of Saints
		21520, 21521, 21522, 21523, 21524, 21525, 21526, 21527, 21528, 21529, 21530, 21531,
		21532, 21533, 21534, 21535, 21536, 21537, 21538, 21539, 21540, 21541, 21542, 21543,
		21544, 21545, 21546, 21547, 21548, 21549, 21550, 21551, 21552, 21553, 21554, 21555,
		21556, 21557, 21558, 21559, 21560, 21561, 21562, 21563, 21564, 21565, 21566, 21567,
		21568, 21569, 21570, 21571, 21572, 21573, 21574, 21575, 21576, 21577, 21578, 21579,
		21580, 21581, 21582, 21583, 21584, 21585, 21586, 21587, 21588, 21589, 21590, 21591,
		21592, 21593, 21594, 21595, 21596, 21597, 21598, 21599, 21600, 21601
	};
	private static final int[] TRIGGER_MOBS_TOP =
	{
		// Beast Farm, Plains of the Lizardmen, Sel Mahum Training Grounds, Field of Silence, Field of Whispers, Crypts of Disgrace, Den of Evil, Primeval Isle, Dragon Valley
		18873, 18874, 18875, 18876, 18877, 18878, 18879, 18880, 18881, 18882, 18883, 18884,
		18885, 18886, 18887, 18888, 18889, 18890, 18891, 18892, 18893, 18894, 18895, 18896,
		18897, 18898, 18899, 18900, 18901, 18902, 18903, 18904, 18905, 18906, 18907, 22196,
		22197, 22198, 22199, 22200, 22201, 22202, 22203, 22204, 22205, 22208, 22209, 22210,
		22211, 22212, 22213, 22214, 22215, 22216, 22217, 22218, 22219, 22220, 22221, 22222,
		22223, 22224, 22225, 22226, 22227, 22650, 22651, 22652, 22653, 22654, 22655, 22656,
		22657, 22658, 22659, 22691, 22692, 22693, 22694, 22695, 22696, 22697, 22698, 22699,
		22700, 22701, 22702, 22703, 22704, 22705, 22706, 22707, 22742, 22743, 22744, 22745,
		22768, 22769, 22770, 22771, 22772, 22773, 22774, 22775, 22776, 22777, 22778, 22779,
		22780, 22781, 22782, 22783, 22784, 22785, 22786, 22787, 22788, 22815, 22818, 22819,
		22820, 22821, 22858
	};
	// Items
	private static final int[][] DROPLIST =
	{
		{LUCKY_PIG_LOW, 8755, 1, 100}, // Top-Grade Life Stone - Level 52
		{LUCKY_PIG_LOW, 8756, 2, 25}, // Top-Grade Life Stone - Level 55
		{LUCKY_PIG_MEDIUM, 5577, 1, 33}, // Red Soul Crystal - Stage 11
		{LUCKY_PIG_MEDIUM, 5578, 1, 33}, // Green Soul Crystal - Stage 11
		{LUCKY_PIG_MEDIUM, 5579, 1, 100}, // Blue Soul Crystal - Stage 11
		{LUCKY_PIG_TOP, 9552, 1, 20}, // Fire Crystal
		{LUCKY_PIG_TOP, 9553, 1, 20}, // Water Crystal
		{LUCKY_PIG_TOP, 9554, 1, 20}, // Earth Crystal
		{LUCKY_PIG_TOP, 9555, 1, 20}, // Wind Crystal
		{LUCKY_PIG_TOP, 9556, 1, 20}, // Dark Crystal
		{LUCKY_PIG_TOP, 9557, 1, 100}, // Holy Crystal
	};
	private static final int[][] DROPLIST_GOLD =
	{
		{LUCKY_PIG_LOW, 14678, 1, 100}, // Neolithic Crystal - B
		{LUCKY_PIG_MEDIUM, 14679, 1, 100}, // Neolithic Crystal - A
		{LUCKY_PIG_TOP, 14680, 1, 100}, // Neolithic Crystal - S
	};
	//@formatter:on
	// Skills
	public static final SkillHolder ENLARGE = new SkillHolder(23325, 1); // Enlarge - Luckpy
	// NpcStrings
	private static final NpcStringId[] SPAM_TEXTS =
	{
		NpcStringId.LUCKY_IF_I_EAT_TOO_MUCH_ADENA_MY_WINGS_DISAPPEAR,
		NpcStringId.LUCKY_I_M_LUCKY_THE_SPIRIT_THAT_LOVES_ADENA,
		NpcStringId.LUCKY_I_WANT_TO_EAT_ADENA_GIVE_IT_TO_ME,
	};
	private static final NpcStringId[] EATING_TEXTS =
	{
		NpcStringId.GRRRR_YUCK,
		NpcStringId.LUCKY_IT_WASN_T_ENOUGH_ADENA_IT_S_GOTTA_BE_AT_LEAST_S,
		NpcStringId.YUMMY_THANKS_LUCKY,
		NpcStringId.LUCKY_THE_ADENA_IS_SO_YUMMY_I_M_GETTING_BIGGER,
		NpcStringId.LUCKY_NO_MORE_ADENA_OH_I_M_SO_HEAVY,
	};
	private static final NpcStringId[] TRANSFORM_TEXTS =
	{
		NpcStringId.OH_MY_WINGS_DISAPPEARED_ARE_YOU_GONNA_HIT_ME_IF_YOU_HIT_ME_I_LL_THROW_UP_EVERYTHING_THAT_I_ATE,
		NpcStringId.OH_MY_WINGS_ACK_ARE_YOU_GONNA_HIT_ME_SCARY_SCARY_IF_YOU_HIT_ME_SOMETHING_BAD_WILL_HAPPEN
	};
	
	//@formatter:off
	private static final int[] LUCKY_PIG_GOLD_BASE_CHANCE = 
	{
		0, 0, 0, 0, 3, 6, 9, 12, 15, 18, 20
	};
	//@formatter:on
	
	/**
	 * Global variable key name for NPC online status.
	 */
	private static final String IS_PIG_ONLINE = "isLuckyPigOnline";
	
	private LuckyPig()
	{
		super();
		setPigOnline(false);
		addStartNpc(LUCKY_PIG_LOW, LUCKY_PIG_MEDIUM, LUCKY_PIG_TOP);
		addTalkId(LUCKY_PIG_LOW, LUCKY_PIG_MEDIUM, LUCKY_PIG_TOP);
		addKillId(LUCKY_PIG_WINGLESS, LUCKY_PIG_WINGLESS_GOLD);
		addKillId(TRIGGER_MOBS_LOW);
		addKillId(TRIGGER_MOBS_MEDIUM);
		addKillId(TRIGGER_MOBS_TOP);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "DESPAWN_TIME":
			{
				setPigOnline(false);
				if (npc != null)
				{
					cancelQuestTimer("TEXT_SPAM", npc, null);
					cancelQuestTimer("CHECK_FOOD", npc, null);
					npc.deleteMe();
				}
				break;
			}
			case "DESPAWN_WINGLESS":
			{
				setPigOnline(false);
				if (npc != null)
				{
					npc.deleteMe();
				}
				break;
			}
			case "TEXT_SPAM":
			{
				final long lastEat = npc.getVariables().getLong("LUCKY_PIG_LAST_EAT", 0);
				
				if (System.currentTimeMillis() > (lastEat + 30000))
				{
					broadcastNpcSay(npc, ChatType.NPC_GENERAL, SPAM_TEXTS[getRandom(SPAM_TEXTS.length)]);
					startQuestTimer(event, (getRandom(20, 40) * 1000), npc, null);
				}
				else
				{
					startQuestTimer(event, (getRandom(10, 20) * 1000), npc, null);
				}
				break;
			}
			case "CHECK_FOOD":
			{
				final int foodState = npc.getVariables().getInt("LUCKY_PIG_FEED_STATE", 0);
				final Item foodItem = npc.getVariables().getObject("LUCKY_PIG_FOOD_ITEM", Item.class);
				
				switch (foodState)
				{
					case 0: // Looking for item
					{
						if (foodItem != null)
						{
							LOGGER.warning(LuckyPig.class.getSimpleName() + ": Lucky pig trying to find another food while currently isn't eaten yet!");
							break;
						}
						
						for (Item food : World.getInstance().getVisibleObjectsInRange(npc, Item.class, 300))
						{
							if (!food.isItem() || (food.getId() != Inventory.ADENA_ID))
							{
								continue;
							}
							
							World.getInstance().removeObject(food);
							npc.getVariables().set("LUCKY_PIG_FEED_STATE", 1);
							npc.getVariables().set("LUCKY_PIG_FOOD_ITEM", food);
						}
						break;
					}
					case 1: // Move to item loc
					{
						if (foodItem != null)
						{
							npc.getAI().setIntention(Intention.MOVE_TO, foodItem.getLocation());
							npc.getVariables().set("LUCKY_PIG_FEED_STATE", 2);
						}
						else
						{
							// Maybe someone else pickup it? :P
							npc.getVariables().set("LUCKY_PIG_FEED_STATE", 0);
						}
						break;
					}
					case 2:
					{
						if (foodItem != null)
						{
							final int eatCount = npc.getVariables().getInt("LUCKY_PIG_EAT_COUNT", 0) + 1;
							final int targetAdena = npc.getVariables().getInt("LUCKY_PIG_TARGET_ADENA", 0);
							final long countAdena = npc.getVariables().getInt("LUCKY_PIG_EAT_ADENA", 0) + foodItem.getCount();
							
							foodItem.decayMe();
							npc.getVariables().set("LUCKY_PIG_LAST_EAT", System.currentTimeMillis());
							npc.getVariables().set("LUCKY_PIG_EAT_ADENA", countAdena);
							npc.getVariables().set("LUCKY_PIG_FEED_COUNT", 0);
							npc.getVariables().set("LUCKY_PIG_EAT_COUNT", eatCount);
							npc.getVariables().set("LUCKY_PIG_FEED_STATE", 0);
							npc.getVariables().remove("LUCKY_PIG_FOOD_ITEM");
							
							if (countAdena >= targetAdena)
							{
								broadcastNpcSay(npc, ChatType.NPC_GENERAL, (getRandomBoolean() ? NpcStringId.LUCKY_I_M_FULL_THANKS_FOR_THE_YUMMY_ADENA_OH_I_M_SO_HEAVY : EATING_TEXTS[getRandom(EATING_TEXTS.length)]));
							}
							else
							{
								broadcastNpcSay(npc, ChatType.NPC_GENERAL, EATING_TEXTS[getRandom(EATING_TEXTS.length)]);
							}
							
							npc.broadcastPacket(new MagicSkillUse(npc, npc, ENLARGE.getSkillId(), 1, 1000, 1000));
							
							if (eatCount >= 10)
							{
								npc.getVariables().set("LUCKY_PIG_FEED_STATE", 3);
							}
						}
						else
						{
							npc.getVariables().set("LUCKY_PIG_FEED_STATE", 0);
						}
						break;
					}
					case 3:
					{
						transformLuckyPig(npc);
						break;
					}
					default:
					{
						break;
					}
				}
				break;
			}
			default:
			{
				break;
			}
		}
		return super.onEvent(event, npc, player);
	}
	
	private void transformLuckyPig(Npc luckyPig)
	{
		cancelQuestTimer("CHECK_FOOD", luckyPig, null);
		cancelQuestTimer("TEXT_SPAM", luckyPig, null);
		final Npc pig = addSpawn(calculateGoldChance(luckyPig) ? LUCKY_PIG_WINGLESS_GOLD : LUCKY_PIG_WINGLESS, luckyPig, true, 600000, true);
		startQuestTimer("DESPAWN_WINGLESS", 600000, pig, null);
		broadcastNpcSay(pig, ChatType.NPC_GENERAL, TRANSFORM_TEXTS[getRandom(TRANSFORM_TEXTS.length)]);
		pig.getVariables().set("LUCKY_PIG_SPAWN_ID", luckyPig.getId());
		luckyPig.deleteMe();
	}
	
	private boolean calculateGoldChance(Npc luckyPig)
	{
		final int eatCount = luckyPig.getVariables().getInt("LUCKY_PIG_EAT_COUNT", 0);
		final int targetAdena = luckyPig.getVariables().getInt("LUCKY_PIG_TARGET_ADENA", 0);
		final long countAdena = luckyPig.getVariables().getInt("LUCKY_PIG_EAT_ADENA", -1);
		
		double finalChance = LUCKY_PIG_GOLD_BASE_CHANCE[eatCount];
		
		if (countAdena >= targetAdena)
		{
			finalChance *= 1.46;
		}
		return getRandom(100) < finalChance;
	}
	
	/**
	 * @param npc
	 * @param type
	 * @param messageId
	 */
	public void broadcastNpcSay(Npc npc, ChatType type, NpcStringId messageId)
	{
		final NpcSay npcSay = new NpcSay(npc.getObjectId(), type, npc.getId(), messageId);
		final int sourceRegion = MapRegionManager.getInstance().getMapRegionLocId(npc);
		for (Player pc : World.getInstance().getPlayers())
		{
			if ((pc != null) && (MapRegionManager.getInstance().getMapRegionLocId(pc) == sourceRegion))
			{
				pc.sendPacket(npcSay);
			}
		}
	}
	
	/**
	 * Checks if there is already a spawned pig in the world.
	 * @return True if there is already a lucky pig wandering around. False otherwise.
	 */
	private static boolean isPigOnline()
	{
		return GlobalVariablesManager.getInstance().getBoolean(IS_PIG_ONLINE, false);
	}
	
	/**
	 * Sets the status of the Lucky Pig in the world.
	 * @param value Use true to set the Lucky Pig status to online. Use False otherwise.
	 */
	private static void setPigOnline(boolean value)
	{
		GlobalVariablesManager.getInstance().set(IS_PIG_ONLINE, value);
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		switch (npc.getId())
		{
			case LUCKY_PIG_WINGLESS:
			{
				setPigOnline(false);
				cancelQuestTimer("DESPAWN_WINGLESS", npc, null);
				manageDrop(npc, killer, false);
				break;
			}
			case LUCKY_PIG_WINGLESS_GOLD:
			{
				setPigOnline(false);
				cancelQuestTimer("DESPAWN_WINGLESS", npc, null);
				manageDrop(npc, killer, true);
				break;
			}
			default:
			{
				final boolean LuckyPidChance = Rnd.get(100) > (100 - EventsConfig.LUCKY_PID_CHANCE);
				if (isPigOnline() || !LuckyPidChance)
				{
					return;
				}
				setPigOnline(true);
				
				final int minAdena;
				final int luckyPigId;
				
				if (ArrayUtil.contains(TRIGGER_MOBS_LOW, npc.getId()))
				{
					minAdena = EventsConfig.LUCKY_PID_LOW_ADENA;
					luckyPigId = LUCKY_PIG_LOW;
				}
				else if (ArrayUtil.contains(TRIGGER_MOBS_MEDIUM, npc.getId()))
				{
					minAdena = EventsConfig.LUCKY_PID_MEDIUM_ADENA;
					luckyPigId = LUCKY_PIG_MEDIUM;
				}
				else
				{
					minAdena = EventsConfig.LUCKY_PID_TOP_ADENA;
					luckyPigId = LUCKY_PIG_TOP;
				}
				
				final int targetAdena = (int) (getRandom(minAdena, (minAdena * 10)) * Config.RATE_DEATH_DROP_AMOUNT_MULTIPLIER);
				final Npc luckyPig = addSpawn(luckyPigId, npc, true, 600000, true);
				startQuestTimer("DESPAWN_TIME", 600000, luckyPig, null);
				startQuestTimer("TEXT_SPAM", 5000, luckyPig, null);
				startQuestTimer("CHECK_FOOD", 2000, luckyPig, null, true);
				luckyPig.getVariables().set("LUCKY_PIG_TARGET_ADENA", targetAdena);
				break;
			}
		}
		
	}
	
	private void manageDrop(Npc luckyPig, Player player, boolean isGold)
	{
		final int npcId = luckyPig.getVariables().getInt("LUCKY_PIG_SPAWN_ID", 0);
		if (npcId <= 0)
		{
			return;
		}
		
		final List<Integer> catData = new ArrayList<>();
		for (int[] data : (isGold ? DROPLIST_GOLD : DROPLIST))
		{
			if ((data[0] == npcId) && (getRandom(100) < data[3]) && !catData.contains(data[2]))
			{
				luckyPig.dropItem(player, data[1], data[2]);
				catData.add(data[2]);
			}
		}
	}
	
	public static void main(String[] args)
	{
		if (EventsConfig.LUCKY_PID_SPAWN_ENABLED)
		{
			new LuckyPig();
		}
	}
}
