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
package quests.TerritoryWarScripts;

import java.util.Calendar;
import java.util.List;

import com.l2journey.gameserver.managers.TerritoryWarManager;
import com.l2journey.gameserver.managers.TerritoryWarManager.TerritoryNPCSpawn;
import com.l2journey.gameserver.model.TerritoryWard;
import com.l2journey.gameserver.model.World;
import com.l2journey.gameserver.model.WorldObject;
import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.quest.Quest;
import com.l2journey.gameserver.model.quest.QuestState;
import com.l2journey.gameserver.model.quest.State;
import com.l2journey.gameserver.model.skill.Skill;
import com.l2journey.gameserver.network.NpcStringId;
import com.l2journey.gameserver.network.serverpackets.ExShowScreenMessage;
import com.l2journey.gameserver.util.ArrayUtil;
import com.l2journey.gameserver.util.LocationUtil;

import quests.Q00147_PathtoBecominganEliteMercenary.Q00147_PathtoBecominganEliteMercenary;
import quests.Q00148_PathtoBecominganExaltedMercenary.Q00148_PathtoBecominganExaltedMercenary;
import quests.Q00176_StepsForHonor.Q00176_StepsForHonor;

/**
 * Territory War quests superclass.
 * @author Gigiikun
 */
public class TerritoryWarSuperClass extends Quest
{
	private static final String KILLS = "kills";

	// "For the Sake of the Territory ..." quests variables
	public int catapultId;
	public int territoryId;
	public int[] leaderIds;
	public int[] guardIds;
	public NpcStringId[] npcString = {};
	// "Protect the ..." quests variables
	public int[] npcIds;
	// "Kill The ..."
	public int[] classIds;
	public int randomMin;
	public int randomMax;
	
	public TerritoryWarSuperClass(int questId)
	{
		super(questId);
		
		if (questId < 0)
		{
			// Outpost and Ward handled by the Super Class script.
			addSkillSeeId(36590);
			
			// Calculate next TW date.
			TerritoryWarManager.getInstance().setNextTWDate();
			LOGGER.info(getClass().getSimpleName() + ": Siege date: " + TerritoryWarManager.getInstance().getTWStart().getTime());
		}
	}
	
	public int getTerritoryIdForThisNPCId(int npcid)
	{
		return 0; // TODO: Implement this.
	}
	
	private void handleKillTheQuest(Player player)
	{
		final QuestState qs = getQuestState(player, true);
		int kill = 1;
		int max = 10;
		if (!qs.isCompleted())
		{
			if (!qs.isStarted())
			{
				qs.setState(State.STARTED);
				qs.setCond(1);
				qs.set("kill", "1");
				max = getRandom(randomMin, randomMax);
				qs.set("max", String.valueOf(max));
			}
			else
			{
				kill = qs.getInt("kill") + 1;
				max = qs.getInt("max");
			}
			if (kill >= max)
			{
				TerritoryWarManager.getInstance().giveTWQuestPoint(player);
				addExpAndSp(player, 534000, 51000);
				qs.set("doneDate", String.valueOf(Calendar.getInstance().get(Calendar.DAY_OF_YEAR)));
				qs.exitQuest(true);
				player.sendPacket(new ExShowScreenMessage(npcString[1], 2, 10000));
			}
			else
			{
				qs.set("kill", String.valueOf(kill));
				
				final ExShowScreenMessage message = new ExShowScreenMessage(npcString[0], 2, 10000);
				message.addStringParameter(String.valueOf(max));
				message.addStringParameter(String.valueOf(kill));
				player.sendPacket(message);
				
			}
		}
		else if (qs.getInt("doneDate") != Calendar.getInstance().get(Calendar.DAY_OF_YEAR))
		{
			qs.setState(State.STARTED);
			qs.setCond(1);
			qs.set("kill", "1");
			max = getRandom(randomMin, randomMax);
			qs.set("max", String.valueOf(max));
			
			final ExShowScreenMessage message = new ExShowScreenMessage(npcString[0], 2, 10000);
			message.addStringParameter(String.valueOf(max));
			message.addStringParameter(String.valueOf(kill));
			player.sendPacket(message);
		}
		else if (player.isGM())
		{
			// just for test
			player.sendMessage("Cleaning " + getName() + " Territory War quest by force!");
			qs.setState(State.STARTED);
			qs.setCond(1);
			qs.set("kill", "1");
			max = getRandom(randomMin, randomMax);
			qs.set("max", String.valueOf(max));
			
			final ExShowScreenMessage message = new ExShowScreenMessage(npcString[0], 2, 10000);
			message.addStringParameter(String.valueOf(max));
			message.addStringParameter(String.valueOf(kill));
			player.sendPacket(message);
		}
	}
	
	@Override
	public void onAttack(Npc npc, Player player, int damage, boolean isSummon)
	{
		if ((npc.getCurrentHp() == npc.getMaxHp()) && ArrayUtil.contains(npcIds, npc.getId()))
		{
			final int territoryId = getTerritoryIdForThisNPCId(npc.getId());
			if ((territoryId >= 81) && (territoryId <= 89))
			{
				for (Player pl : World.getInstance().getPlayers())
				{
					if (pl.getSiegeSide() == territoryId)
					{
						QuestState qs = pl.getQuestState(getName());
						if (qs == null)
						{
							qs = newQuestState(pl);
						}
						if (!qs.isStarted())
						{
							qs.setState(State.STARTED, false);
							qs.setCond(1);
						}
					}
				}
			}
		}
	}
	
	@Override
	public void onDeath(Creature killer, Creature victim, QuestState qs)
	{
		if ((killer == victim) || !victim.isPlayer() || (victim.getLevel() < 61))
		{
			return;
		}
		
		final Player actingPlayer = killer.asPlayer();
		if ((actingPlayer != null) && (qs.getPlayer() != null))
		{
			if (actingPlayer.getParty() != null)
			{
				for (Player pl : actingPlayer.getParty().getMembers())
				{
					if ((pl.getSiegeSide() == qs.getPlayer().getSiegeSide()) || (pl.getSiegeSide() == 0) || !LocationUtil.checkIfInRange(2000, killer, pl, false))
					{
						continue;
					}
					
					if (pl == actingPlayer)
					{
						handleStepsForHonor(actingPlayer);
						handleBecomeMercenaryQuest(actingPlayer, false);
					}
					handleKillTheQuest(pl);
				}
			}
			else if ((actingPlayer.getSiegeSide() != qs.getPlayer().getSiegeSide()) && (actingPlayer.getSiegeSide() > 0))
			{
				handleKillTheQuest(actingPlayer);
				handleStepsForHonor(actingPlayer);
				handleBecomeMercenaryQuest(actingPlayer, false);
			}
			
			TerritoryWarManager.getInstance().giveTWPoint(actingPlayer, qs.getPlayer().getSiegeSide(), 1);
		}
	}
	
	@Override
	public void onEnterWorld(Player player)
	{
		final int territoryId = TerritoryWarManager.getInstance().getRegisteredTerritoryId(player);
		if (territoryId > 0)
		{
			// register Territory Quest
			final TerritoryWarSuperClass territoryQuest = TerritoryWarSuperClassLoader.getForTheSakeScripts().get(territoryId);
			QuestState qs = player.getQuestState(territoryQuest.getName());
			if (qs == null)
			{
				qs = territoryQuest.newQuestState(player);
			}
			qs.setState(State.STARTED, false);
			qs.setCond(1);
			
			// register player on Death
			if (player.getLevel() >= 61)
			{
				final TerritoryWarSuperClass killthe = TerritoryWarSuperClassLoader.getKillTheScripts().get(player.getPlayerClass().getId());
				if (killthe != null)
				{
					qs = player.getQuestState(killthe.getName());
					if (qs == null)
					{
						qs = killthe.newQuestState(player);
					}
					player.addNotifyQuestOfDeath(qs);
				}
				else
				{
					LOGGER.warning(() -> "TerritoryWar: Missing Kill the quest for " + player + " whose class id: " + player.getPlayerClass().getId());
				}
			}
		}
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final TerritoryWarManager manager = TerritoryWarManager.getInstance();
		if (npc.getId() == catapultId)
		{
			manager.territoryCatapultDestroyed(territoryId - 80);
			manager.giveTWPoint(killer, territoryId, 4);
			manager.announceToParticipants(new ExShowScreenMessage(npcString[0], 2, 10000), 135000, 13500);
			handleBecomeMercenaryQuest(killer, true);
		}
		else if (ArrayUtil.contains(leaderIds, npc.getId()))
		{
			manager.giveTWPoint(killer, territoryId, 3);
		}
		
		if ((killer.getSiegeSide() != territoryId) && (TerritoryWarManager.getInstance().getTerritory(killer.getSiegeSide() - 80) != null))
		{
			manager.getTerritory(killer.getSiegeSide() - 80).getQuestDone()[0]++;
		}
	}
	
	@Override
	public void onSkillSee(Npc npc, Player caster, Skill skill, List<WorldObject> targets, boolean isSummon)
	{
		if (targets.contains(npc))
		{
			if (skill.getId() == 845)
			{
				if (TerritoryWarManager.getInstance().getHQForClan(caster.getClan()) != npc)
				{
					return;
				}
				
				npc.deleteMe();
				TerritoryWarManager.getInstance().setHQForClan(caster.getClan(), null);
			}
			else if (skill.getId() == 847)
			{
				if (TerritoryWarManager.getInstance().getHQForTerritory(caster.getSiegeSide()) != npc)
				{
					return;
				}
				
				final TerritoryWard ward = TerritoryWarManager.getInstance().getTerritoryWard(caster);
				if (ward == null)
				{
					return;
				}
				
				if ((caster.getSiegeSide() - 80) == ward.getOwnerCastleId())
				{
					for (TerritoryNPCSpawn wardSpawn : TerritoryWarManager.getInstance().getTerritory(ward.getOwnerCastleId()).getOwnedWard())
					{
						if (wardSpawn.getId() == ward.getTerritoryId())
						{
							wardSpawn.setNPC(wardSpawn.getNpc().getSpawn().doSpawn(false));
							ward.unSpawnMe();
							ward.setNpc(wardSpawn.getNpc());
						}
					}
				}
				else
				{
					ward.unSpawnMe();
					ward.setNpc(TerritoryWarManager.getInstance().addTerritoryWard(ward.getTerritoryId(), caster.getSiegeSide() - 80, ward.getOwnerCastleId(), true));
					ward.setOwnerCastleId(caster.getSiegeSide() - 80);
					TerritoryWarManager.getInstance().getTerritory(caster.getSiegeSide() - 80).getQuestDone()[1]++;
				}
			}
		}
	}
	
	// Used to register NPCs "For the Sake of the Territory ..." quests
	public void registerKillIds()
	{
		addKillId(catapultId);
		for (int mobid : leaderIds)
		{
			addKillId(mobid);
		}
		for (int mobid : guardIds)
		{
			addKillId(mobid);
		}
	}
	
	@Override
	public void setOnEnterWorld(boolean value)
	{
		super.setOnEnterWorld(value);
		
		for (Player player : World.getInstance().getPlayers())
		{
			if (player.getSiegeSide() > 0)
			{
				final TerritoryWarSuperClass territoryQuest = TerritoryWarSuperClassLoader.getForTheSakeScripts().get(player.getSiegeSide());
				if (territoryQuest == null)
				{
					continue;
				}
				
				QuestState qs = player.hasQuestState(territoryQuest.getName()) ? player.getQuestState(territoryQuest.getName()) : territoryQuest.newQuestState(player);
				if (value)
				{
					qs.setState(State.STARTED, false);
					qs.setCond(1);
					// register player on Death
					if (player.getLevel() >= 61)
					{
						final TerritoryWarSuperClass killthe = TerritoryWarSuperClassLoader.getKillTheScripts().get(player.getPlayerClass().getId());
						if (killthe != null)
						{
							qs = player.getQuestState(killthe.getName());
							if (qs == null)
							{
								qs = killthe.newQuestState(player);
							}
							player.addNotifyQuestOfDeath(qs);
						}
						else
						{
							LOGGER.warning(() -> "TerritoryWar: Missing Kill the quest for " + player + " whose class id: " + player.getPlayerClass().getId());
						}
					}
				}
				else
				{
					qs.exitQuest(false);
					for (Quest q : TerritoryWarSuperClassLoader.getProtectTheScripts().values())
					{
						qs = player.getQuestState(q.getName());
						if (qs != null)
						{
							qs.exitQuest(false);
						}
					}
					// unregister player on Death
					final TerritoryWarSuperClass killthe = TerritoryWarSuperClassLoader.getKillTheScripts().get(player.getClassIndex());
					if (killthe != null)
					{
						qs = player.getQuestState(killthe.getName());
						if (qs != null)
						{
							player.removeNotifyQuestOfDeath(qs);
						}
					}
				}
			}
		}
	}
	
	private void handleBecomeMercenaryQuest(Player player, boolean catapult)
	{
		int enemyCount = 10;
		int catapultCount = 1;
		QuestState qs = player.getQuestState(Q00147_PathtoBecominganEliteMercenary.class.getSimpleName());
		if ((qs != null) && qs.isCompleted())
		{
			qs = player.getQuestState(Q00148_PathtoBecominganExaltedMercenary.class.getSimpleName());
			enemyCount = 30;
			catapultCount = 2;
		}
		
		if ((qs != null) && qs.isStarted())
		{
			final int cond = qs.getCond();
			if (catapult)
			{
				if ((cond == 1) || (cond == 2))
				{
					final int count = qs.getInt("catapult") + 1;
					qs.set("catapult", String.valueOf(count));
					if (count >= catapultCount)
					{
						qs.setCond((cond == 1) ? 3 : 4);
					}
				}
			}
			else if ((cond == 1) || (cond == 3))
			{
				final int kills = qs.getInt(KILLS) + 1;
				qs.set(KILLS, Integer.toString(kills));
				if (kills >= enemyCount)
				{
					qs.setCond((cond == 1) ? 2 : 4);
				}
			}
		}
	}
	
	private void handleStepsForHonor(Player player)
	{
		final QuestState q176 = player.getQuestState(Q00176_StepsForHonor.class.getSimpleName());
		if ((q176 != null) && q176.isStarted())
		{
			final int cond = q176.getCond();
			if ((cond == 1) || (cond == 3) || (cond == 5) || (cond == 7))
			{
				final int kills = q176.getInt(KILLS) + 1;
				q176.set(KILLS, kills);
				if ((cond == 1) && (kills >= 9))
				{
					q176.setCond(2);
					q176.set(KILLS, "0");
				}
				else if ((cond == 3) && (kills >= 18))
				{
					q176.setCond(4);
					q176.set(KILLS, "0");
				}
				else if ((cond == 5) && (kills >= 27))
				{
					q176.setCond(6);
					q176.set(KILLS, "0");
				}
				else if ((cond == 7) && (kills >= 36))
				{
					q176.setCond(8);
					q176.unset(KILLS);
				}
			}
		}
	}
}
