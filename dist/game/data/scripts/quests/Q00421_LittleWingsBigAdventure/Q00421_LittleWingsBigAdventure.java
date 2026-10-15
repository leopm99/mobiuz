/*
 * Copyright (c) 2013 L2jMobius
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package quests.Q00421_LittleWingsBigAdventure;

import java.util.HashMap;
import java.util.Map;

import com.l2journey.Config;
import com.l2journey.gameserver.ai.Intention;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.Summon;
import com.l2journey.gameserver.model.item.instance.Item;
import com.l2journey.gameserver.model.quest.Quest;
import com.l2journey.gameserver.model.quest.QuestSound;
import com.l2journey.gameserver.model.quest.QuestState;
import com.l2journey.gameserver.model.quest.State;
import com.l2journey.gameserver.model.skill.holders.SkillHolder;
import com.l2journey.gameserver.network.NpcStringId;
import com.l2journey.gameserver.network.enums.ChatType;
import com.l2journey.gameserver.network.serverpackets.NpcSay;
import com.l2journey.gameserver.util.LocationUtil;

/**
 * Little Wing's Big Adventure (421)
 * @author Mobius, jurchiks
 */
public class Q00421_LittleWingsBigAdventure extends Quest
{
	private static final String FLUTEOBJECTID = "fluteObjectId";

	// NPCs
	private static final int CRONOS = 30610;
	private static final int MIMYU = 30747;
	// Items
	private static final int DRAGONFLUTE_OF_WIND = 3500;
	private static final int DRAGONFLUTE_OF_STAR = 3501;
	private static final int DRAGONFLUTE_OF_TWILIGHT = 3502;
	private static final int FAIRY_LEAF = 4325;
	// Monsters
	private static final int TREE_OF_WIND = 27185;
	private static final int TREE_OF_STAR = 27186;
	private static final int TREE_OF_TWILIGHT = 27187;
	private static final int TREE_OF_ABYSS = 27188;
	private static final int SOUL_OF_TREE_GUARDIAN = 27189;
	// Skills
	private static final SkillHolder CURSE_OF_MIMYU = new SkillHolder(4167, 1);
	private static final SkillHolder DRYAD_ROOT = new SkillHolder(1201, 33);
	private static final SkillHolder VICIOUS_POISON = new SkillHolder(4243, 1);
	// Rewards
	private static final int DRAGON_BUGLE_OF_WIND = 4422;
	private static final int DRAGON_BUGLE_OF_STAR = 4423;
	private static final int DRAGON_BUGLE_OF_TWILIGHT = 4424;
	// Misc
	private static final int MIN_PLAYER_LEVEL = 45;
	private static final int MIN_HACHLING_LEVEL = 55;
	// Memo states
	// Tree progress is a bit field; each tree is completed once, in any order.
	/** Tree of Wind bit. */
	private static final int TREE_OF_WIND_FLAG = 1;
	/** Tree of Star bit. */
	private static final int TREE_OF_STAR_FLAG = 2;
	/** Tree of Twilight bit. */
	private static final int TREE_OF_TWILIGHT_FLAG = 4;
	/** Tree of the Abyss bit. */
	private static final int TREE_OF_ABYSS_FLAG = 8;
	/** Wind, Star, Twilight, and Abyss are all done. */
	private static final int MEMO_STATE_ALL_TREES = TREE_OF_WIND_FLAG | TREE_OF_STAR_FLAG | TREE_OF_TWILIGHT_FLAG | TREE_OF_ABYSS_FLAG;
	/** Empty mask; Mimyu only offers the explanation menu. */
	private static final int MEMO_STATE_NO_TREES = 0;
	/** Cronos has started the quest; the player has not spoken to Mimyu. */
	private static final int MEMO_STATE_SENT_TO_MIMYU = 100;
	/** Mimyu is waiting for the hatchling whose flute object id was stored at the start. */
	private static final int MEMO_STATE_AWAITING_HATCHLING = 200;
	/** Mimyu has accepted that hatchling and is waiting for it to be unsummoned and for its flute. */
	private static final int MEMO_STATE_AWAITING_FLUTE = 16;
	private static final Map<Integer, NpcData> NPC_DATA = new HashMap<>();
	static
	{
		NPC_DATA.put(TREE_OF_WIND, new NpcData(NpcStringId.HEY_YOU_VE_ALREADY_DRUNK_THE_ESSENCE_OF_WIND, TREE_OF_WIND_FLAG, 27)); // retail min hits is 270
		NPC_DATA.put(TREE_OF_STAR, new NpcData(NpcStringId.HEY_YOU_VE_ALREADY_DRUNK_THE_ESSENCE_OF_A_STAR, TREE_OF_STAR_FLAG, 40)); // retail min hits is 400
		NPC_DATA.put(TREE_OF_TWILIGHT, new NpcData(NpcStringId.HEY_YOU_VE_ALREADY_DRUNK_THE_ESSENCE_OF_DUSK, TREE_OF_TWILIGHT_FLAG, 15)); // retail min hits is 150
		NPC_DATA.put(TREE_OF_ABYSS, new NpcData(NpcStringId.HEY_YOU_VE_ALREADY_DRUNK_THE_ESSENCE_OF_THE_ABYSS, TREE_OF_ABYSS_FLAG, 27)); // retail min hits is 270
	}
	
	public Q00421_LittleWingsBigAdventure()
	{
		super(421);
		addStartNpc(CRONOS);
		addTalkId(CRONOS, MIMYU);
		addAttackId(NPC_DATA.keySet());
		addKillId(NPC_DATA.keySet());
		registerQuestItems(FAIRY_LEAF);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if ("DESPAWN_GUARDIAN".equals(event))
		{
			if (npc != null)
			{
				npc.deleteMe();
			}
			return super.onEvent(event, npc, player);
		}
		
		final QuestState qs = getQuestState(player, false);
		String htmltext = null;
		if (qs == null)
		{
			return htmltext;
		}
		
		switch (event)
		{
			case "30610-05.htm":
			{
				if (qs.isCreated())
				{
					if (getQuestItemsCount(player, DRAGONFLUTE_OF_WIND, DRAGONFLUTE_OF_STAR, DRAGONFLUTE_OF_TWILIGHT) == 1)
					{
						final Item flute = getFlute(player);
						if (flute.getEnchantLevel() < MIN_HACHLING_LEVEL)
						{
							htmltext = "30610-06.html";
						}
						else
						{
							qs.startQuest();
							qs.setMemoState(MEMO_STATE_SENT_TO_MIMYU);
							qs.set(FLUTEOBJECTID, flute.getObjectId());
							htmltext = event;
						}
					}
					else
					{
						htmltext = "30610-06.html";
					}
				}
				break;
			}
			case "30747-04.html":
			{
				final Summon summon = player.getSummon();
				if (summon == null)
				{
					htmltext = "30747-02.html";
				}
				else if (summon.getControlObjectId() != qs.getInt(FLUTEOBJECTID))
				{
					htmltext = "30747-03.html";
				}
				else
				{
					htmltext = event;
				}
				break;
			}
			case "30747-05.html":
			{
				final Summon summon = player.getSummon();
				if (summon == null)
				{
					htmltext = "30747-06.html";
				}
				else if (summon.getControlObjectId() != qs.getInt(FLUTEOBJECTID))
				{
					htmltext = "30747-06.html";
				}
				else
				{
					giveItems(player, FAIRY_LEAF, 4);
					qs.setCond(2, true);
					qs.setMemoState(MEMO_STATE_NO_TREES);
					htmltext = event;
				}
				break;
			}
			case "30747-07.html", "30747-08.html", "30747-09.html", "30747-10.html":
			{
				htmltext = event;
				break;
			}
			default:
			{
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, Player talker)
	{
		final QuestState qs = getQuestState(talker, true);
		String htmltext = getNoQuestMsg(talker);
		
		switch (npc.getId())
		{
			case CRONOS:
			{
				switch (qs.getState())
				{
					case State.CREATED:
					{
						final long fluteCount = getQuestItemsCount(talker, DRAGONFLUTE_OF_WIND, DRAGONFLUTE_OF_STAR, DRAGONFLUTE_OF_TWILIGHT);
						if (fluteCount == 0)
						{
							break; // this quest does not show up if no flute in inventory
						}
						
						if (talker.getLevel() < MIN_PLAYER_LEVEL)
						{
							htmltext = "30610-01.htm";
						}
						else if (fluteCount > 1)
						{
							htmltext = "30610-02.htm";
						}
						else if (getFlute(talker).getEnchantLevel() < MIN_HACHLING_LEVEL)
						{
							htmltext = "30610-03.html";
						}
						else
						{
							htmltext = "30610-04.htm";
						}
						break;
					}
					case State.STARTED:
					{
						htmltext = "30610-07.html";
						break;
					}
					case State.COMPLETED:
					{
						htmltext = getAlreadyCompletedMsg(talker);
						break;
					}
					default:
					{
						break;
					}
				}
				break;
			}
			case MIMYU:
			{
				switch (qs.getMemoState())
				{
					case MEMO_STATE_SENT_TO_MIMYU:
					{
						qs.setMemoState(MEMO_STATE_AWAITING_HATCHLING);
						htmltext = "30747-01.html";
						break;
					}
					case MEMO_STATE_AWAITING_HATCHLING:
					{
						final Summon summon = talker.getSummon();
						if (summon == null)
						{
							htmltext = "30747-02.html";
						}
						else if (summon.getControlObjectId() != qs.getInt(FLUTEOBJECTID))
						{
							htmltext = "30747-03.html";
						}
						else
						{
							htmltext = "30747-04.html";
						}
						break;
					}
					case MEMO_STATE_NO_TREES:
					{
						htmltext = "30747-07.html";
						break;
					}
					case MEMO_STATE_ALL_TREES:
					{
						if (!hasQuestItems(talker, FAIRY_LEAF))
						{
							final Summon summon = talker.getSummon();
							if (summon == null)
							{
								htmltext = "30747-12.html";
							}
							else if (summon.getControlObjectId() == qs.getInt(FLUTEOBJECTID))
							{
								qs.setMemoState(MEMO_STATE_AWAITING_FLUTE);
								htmltext = "30747-13.html";
							}
							else
							{
								htmltext = "30747-14.html";
							}
						}
						break;
					}
					case MEMO_STATE_AWAITING_FLUTE:
					{
						if (!hasQuestItems(talker, FAIRY_LEAF))
						{
							if (talker.hasSummon())
							{
								htmltext = "30747-15.html";
							}
							else
							{
								final long fluteCount = getQuestItemsCount(talker, DRAGONFLUTE_OF_WIND, DRAGONFLUTE_OF_STAR, DRAGONFLUTE_OF_TWILIGHT);
								if (fluteCount > 1)
								{
									htmltext = "30747-17.html";
								}
								else if (fluteCount == 1)
								{
									final Item flute = getFlute(talker);
									if (flute.getObjectId() == qs.getInt(FLUTEOBJECTID))
									{
										// TODO what if the hatchling has items in his inventory?
										// Should they be transfered to the strider or given to the player?
										switch (flute.getId())
										{
											case DRAGONFLUTE_OF_WIND:
											{
												takeItems(talker, DRAGONFLUTE_OF_WIND, -1);
												giveItems(talker, DRAGON_BUGLE_OF_WIND, 1);
												break;
											}
											case DRAGONFLUTE_OF_STAR:
											{
												takeItems(talker, DRAGONFLUTE_OF_STAR, -1);
												giveItems(talker, DRAGON_BUGLE_OF_STAR, 1);
												break;
											}
											case DRAGONFLUTE_OF_TWILIGHT:
											{
												takeItems(talker, DRAGONFLUTE_OF_TWILIGHT, -1);
												giveItems(talker, DRAGON_BUGLE_OF_TWILIGHT, 1);
												break;
											}
											default:
											{
												break;
											}
										}
										
										qs.exitQuest(true, true);
										htmltext = "30747-16.html";
									}
									else
									{
										npc.setTarget(talker);
										npc.doCast(CURSE_OF_MIMYU.getSkill());
										htmltext = "30747-18.html";
									}
								}
							}
						}
						break;
					}
					default:
					{
						// Any incomplete set of trees.
						final int memoState = qs.getMemoState();
						if ((memoState > MEMO_STATE_NO_TREES) && (memoState < MEMO_STATE_ALL_TREES) && hasQuestItems(talker, FAIRY_LEAF))
						{
							htmltext = "30747-11.html";
						}
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
		return htmltext;
	}
	
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		final QuestState qs = getQuestState(attacker, false);
		if ((qs != null) && qs.isCond(2))
		{
			if (isSummon)
			{
				final NpcData data = NPC_DATA.get(npc.getId());
				if ((qs.getMemoState() & data.memoStateValue) == 0)
				{
					if (attacker.getSummon().getControlObjectId() == qs.getInt(FLUTEOBJECTID))
					{
						final int hits = qs.getInt("hits") + 1;
						qs.set("hits", hits);
						if (hits < data.minHits)
						{
							if ((npc.getId() == TREE_OF_ABYSS) && (getRandom(100) < 2))
							{
								npc.setTarget(attacker);
								npc.doCast(DRYAD_ROOT.getSkill());
							}
						}
						else if ((getRandom(100) < 6) && (hasQuestItems(attacker, FAIRY_LEAF))) // Retail is < 2
						{
							npc.broadcastPacket(new NpcSay(npc, ChatType.NPC_GENERAL, NpcStringId.GIVE_ME_A_FAIRY_LEAF));
							takeItems(attacker, FAIRY_LEAF, 1);
							qs.setMemoState(qs.getMemoState() + data.memoStateValue);
							qs.unset("hits");
							playSound(attacker, QuestSound.ITEMSOUND_QUEST_MIDDLE);
							if (qs.getMemoState() == MEMO_STATE_ALL_TREES)
							{
								qs.setCond(3);
							}
						}
					}
				}
				else
				{
					switch (getRandom(3))
					{
						case 0:
						{
							npc.broadcastPacket(new NpcSay(npc, ChatType.NPC_GENERAL, NpcStringId.WHY_DO_YOU_BOTHER_ME_AGAIN));
							break;
						}
						case 1:
						{
							npc.broadcastPacket(new NpcSay(npc, ChatType.NPC_GENERAL, data.message));
							break;
						}
						case 2:
						{
							npc.broadcastPacket(new NpcSay(npc, ChatType.NPC_GENERAL, NpcStringId.LEAVE_NOW_BEFORE_YOU_INCUR_THE_WRATH_OF_THE_GUARDIAN_GHOST));
							break;
						}
						default:
						{
							break;
						}
					}
				}
			}
			else if (getRandom(100) < 30)
			{
				npc.setTarget(attacker);
				npc.doCast(VICIOUS_POISON.getSkill());
			}
		}
		else if ((npc.getCurrentHp() < (npc.getMaxHp() * 0.67)) && (getRandom(100) < 30))
		{
			npc.setTarget(attacker);
			npc.doCast(VICIOUS_POISON.getSkill());
		}
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		if (LocationUtil.checkIfInRange(Config.ALT_PARTY_RANGE, killer, npc, true))
		{
			for (int i = 0; i < 20; i++)
			{
				final Npc guardian = addSpawn(SOUL_OF_TREE_GUARDIAN, npc);
				startQuestTimer("DESPAWN_GUARDIAN", 300000, guardian, null);
				if (i == 0)
				{
					npc.setTarget(killer);
					npc.doCast(VICIOUS_POISON.getSkill());
				}
				
				npc.getAI().setIntention(Intention.ATTACK, killer);
			}
		}
	}
	
	private static Item getFlute(Player player)
	{
		final int fluteItemId;
		if (hasQuestItems(player, DRAGONFLUTE_OF_WIND))
		{
			fluteItemId = DRAGONFLUTE_OF_WIND;
		}
		else if (hasQuestItems(player, DRAGONFLUTE_OF_STAR))
		{
			fluteItemId = DRAGONFLUTE_OF_STAR;
		}
		else
		{
			fluteItemId = DRAGONFLUTE_OF_TWILIGHT;
		}
		return player.getInventory().getItemByItemId(fluteItemId);
	}
	
	private static class NpcData
	{
		public NpcStringId message;
		public int memoStateValue;
		public int minHits;
		
		public NpcData(NpcStringId message, int memoStateValue, int minHits)
		{
			this.message = message;
			this.memoStateValue = memoStateValue;
			this.minHits = minHits;
		}
	}
}