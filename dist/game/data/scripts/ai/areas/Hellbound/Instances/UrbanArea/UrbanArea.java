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
package ai.areas.Hellbound.Instances.UrbanArea;

import java.util.concurrent.ScheduledFuture;

import com.l2journey.commons.threads.ThreadPool;
import com.l2journey.gameserver.managers.InstanceManager;
import com.l2journey.gameserver.model.Location;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.instance.QuestGuard;
import com.l2journey.gameserver.model.groups.Party;
import com.l2journey.gameserver.model.instancezone.Instance;
import com.l2journey.gameserver.model.instancezone.InstanceWorld;
import com.l2journey.gameserver.model.item.enums.ItemProcessType;
import com.l2journey.gameserver.model.skill.Skill;
import com.l2journey.gameserver.model.skill.enums.SkillFinishType;
import com.l2journey.gameserver.model.skill.holders.SkillHolder;
import com.l2journey.gameserver.network.NpcStringId;
import com.l2journey.gameserver.network.SystemMessageId;
import com.l2journey.gameserver.network.enums.ChatType;
import com.l2journey.gameserver.network.serverpackets.NpcSay;
import com.l2journey.gameserver.network.serverpackets.SystemMessage;
import com.l2journey.gameserver.util.LocationUtil;

import ai.areas.Hellbound.HellboundEngine;
import instances.AbstractInstance;

/**
 * Urban Area instance zone.
 * @author GKR
 */
public class UrbanArea extends AbstractInstance
{
	private static final String ACTIVEAMASKARICALL = "activeAmaskariCall";
	private static final String SPAWNEDAMASKARI = "spawnedAmaskari";
	private static final String ISAMASKARIDEAD = "isAmaskariDead";
	private static final String REBUFF = "rebuff";

	// NPCs
	private static final int TOMBSTONE = 32343;
	private static final int KANAF = 32346;
	private static final int KEYMASTER = 22361;
	private static final int AMASKARI = 22449;
	private static final int DOWNTOWN_NATIVE = 32358;
	private static final int TOWN_GUARD = 22359;
	private static final int TOWN_PATROL = 22360;
	// Items
	private static final int KEY = 9714;
	// Skills
	private static final SkillHolder STONE = new SkillHolder(4616, 1);
	// Locations
	private static final Location AMASKARI_SPAWN_POINT = new Location(19424, 253360, -2032, 16860);
	private static final Location ENTRY_POINT = new Location(14117, 255434, -2016);
	protected static final Location EXIT_POINT = new Location(16262, 283651, -9700);
	// Misc
	private static final int MIN_LV = 78;
	private static final int TEMPLATE_ID = 2;
	
	private static final NpcStringId[] NPCSTRING_ID =
	{
		NpcStringId.INVADER,
		NpcStringId.YOU_HAVE_DONE_WELL_IN_FINDING_ME_BUT_I_CANNOT_JUST_HAND_YOU_THE_KEY
	};
	
	private static final NpcStringId[] NATIVES_NPCSTRING_ID =
	{
		NpcStringId.THANK_YOU_FOR_SAVING_ME,
		NpcStringId.GUARDS_ARE_COMING_RUN,
		NpcStringId.NOW_I_CAN_ESCAPE_ON_MY_OWN
	};
	
	public UrbanArea()
	{
		addFirstTalkId(DOWNTOWN_NATIVE);
		addStartNpc(KANAF, DOWNTOWN_NATIVE);
		addTalkId(KANAF, DOWNTOWN_NATIVE);
		addAttackId(TOWN_GUARD, KEYMASTER);
		addAggroRangeEnterId(TOWN_GUARD);
		addKillId(AMASKARI);
		addSpawnId(DOWNTOWN_NATIVE, TOWN_GUARD, TOWN_PATROL, KEYMASTER);
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		if (!npc.isAffectedBySkill(STONE.getSkillId()))
		{
			return "32358-02.htm";
		}
		return "32358-01.htm";
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		String htmltext = null;
		if (npc.getId() == KANAF)
		{
			if (!player.isGM())
			{
				if (HellboundEngine.getInstance().getLevel() < 10)
				{
					htmltext = "32346-lvl.htm";
				}
				
				if (player.getParty() == null)
				{
					htmltext = "32346-party.htm";
				}
			}
			
			if (htmltext == null)
			{
				enterInstance(player, TEMPLATE_ID);
			}
		}
		else if (npc.getId() == TOMBSTONE)
		{
			final InstanceWorld world = InstanceManager.getInstance().getWorld(npc);
			if (world != null)
			{
				final Party party = player.getParty();
				if (party == null)
				{
					htmltext = "32343-02.htm";
				}
				else if (npc.isBusy())
				{
					htmltext = "32343-02c.htm";
				}
				else if (player.getInventory().getInventoryItemCount(KEY, -1, false) >= 1)
				{
					for (Player partyMember : party.getMembers())
					{
						if (!LocationUtil.checkIfInRange(300, npc, partyMember, true))
						{
							return "32343-02b.htm";
						}
					}
					
					if (player.destroyItemByItemId(ItemProcessType.QUEST, KEY, 1, npc, true))
					{
						npc.setBusy(true);
						// destroy instance after 5 min
						final Instance inst = InstanceManager.getInstance().getInstance(world.getInstanceId());
						inst.setDuration(5 * 60000);
						inst.setEmptyDestroyTime(0);
						ThreadPool.schedule(new ExitInstance(party, world), 285000);
						htmltext = "32343-02d.htm";
					}
				}
				else
				{
					htmltext = "32343-02a.htm";
				}
			}
		}
		return htmltext;
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		final InstanceWorld world = InstanceManager.getInstance().getWorld(npc);
		if ((world != null) && (npc.getId() == DOWNTOWN_NATIVE))
		{
			if (event.equalsIgnoreCase(REBUFF) && !world.getParameters().getBoolean(ISAMASKARIDEAD, false))
			{
				STONE.getSkill().applyEffects(npc, npc);
			}
			else if (event.equalsIgnoreCase("break_chains"))
			{
				if (!npc.isAffectedBySkill(STONE.getSkillId()) || world.getParameters().getBoolean(ISAMASKARIDEAD, false))
				{
					npc.broadcastSay(ChatType.NPC_GENERAL, NATIVES_NPCSTRING_ID[0]);
					npc.broadcastSay(ChatType.NPC_GENERAL, NATIVES_NPCSTRING_ID[2]);
				}
				else
				{
					cancelQuestTimer(REBUFF, npc, null);
					if (npc.isAffectedBySkill(STONE.getSkillId()))
					{
						npc.stopSkillEffects(SkillFinishType.SILENT, STONE.getSkillId());
					}
					
					npc.broadcastSay(ChatType.NPC_GENERAL, NATIVES_NPCSTRING_ID[0]);
					npc.broadcastSay(ChatType.NPC_GENERAL, NATIVES_NPCSTRING_ID[1]);
					HellboundEngine.getInstance().updateTrust(10, true);
					npc.scheduleDespawn(3000);
					// Try to call Amaskari
					final Npc spawnedAmaskari = world.getParameters().getObject(SPAWNEDAMASKARI, Npc.class);
					if ((spawnedAmaskari != null) && !spawnedAmaskari.isDead() && (getRandom(1000) < 25) && LocationUtil.checkIfInRange(5000, npc, spawnedAmaskari, false))
					{
						final ScheduledFuture<?> activeAmaskariCall = world.getParameters().getObject(ACTIVEAMASKARICALL, ScheduledFuture.class);
						if (activeAmaskariCall != null)
						{
							activeAmaskariCall.cancel(true);
						}
						world.setParameter(ACTIVEAMASKARICALL, ThreadPool.schedule(new CallAmaskari(npc), 25000));
					}
				}
			}
		}
		return super.onEvent(event, npc, player);
	}
	
	@Override
	public void onSpawn(Npc npc)
	{
		if (npc.getId() == DOWNTOWN_NATIVE)
		{
			((QuestGuard) npc).setPassive(true);
			((QuestGuard) npc).setAutoAttackable(false);
			STONE.getSkill().applyEffects(npc, npc);
			startQuestTimer(REBUFF, 357000, npc, null);
		}
		else if ((npc.getId() == TOWN_GUARD) || (npc.getId() == KEYMASTER))
		{
			npc.setBusy(false);
			npc.setBusyMessage("");
		}
	}
	
	@Override
	public void onAggroRangeEnter(Npc npc, Player player, boolean isSummon)
	{
		final InstanceWorld world = InstanceManager.getInstance().getWorld(npc);
		if ((world != null) && !npc.isBusy())
		{
			npc.broadcastSay(ChatType.NPC_GENERAL, NPCSTRING_ID[0]);
			npc.setBusy(true);
			
			final Npc spawnedAmaskari = world.getParameters().getObject(SPAWNEDAMASKARI, Npc.class);
			if ((spawnedAmaskari != null) && !spawnedAmaskari.isDead() && (getRandom(1000) < 25) && LocationUtil.checkIfInRange(1000, npc, spawnedAmaskari, false))
			{
				final ScheduledFuture<?> activeAmaskariCall = world.getParameters().getObject(ACTIVEAMASKARICALL, ScheduledFuture.class);
				if (activeAmaskariCall != null)
				{
					activeAmaskariCall.cancel(true);
				}
				world.setParameter(ACTIVEAMASKARICALL, ThreadPool.schedule(new CallAmaskari(npc), 25000));
			}
		}
	}
	
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon, Skill skill)
	{
		final InstanceWorld world = InstanceManager.getInstance().getWorld(npc);
		if ((world != null) && !world.getParameters().getBoolean(ISAMASKARIDEAD, false) && !(npc.getBusyMessage().equalsIgnoreCase("atk") || npc.isBusy()))
		{
			int msgId;
			int range;
			switch (npc.getId())
			{
				case TOWN_GUARD:
				{
					msgId = 0;
					range = 1000;
					break;
				}
				case KEYMASTER:
				{
					msgId = 1;
					range = 5000;
					break;
				}
				default:
				{
					msgId = -1;
					range = 0;
				}
			}
			if (msgId >= 0)
			{
				npc.broadcastSay(ChatType.NPC_GENERAL, NPCSTRING_ID[msgId], range);
			}
			npc.setBusy(true);
			npc.setBusyMessage("atk");
			
			final Npc spawnedAmaskari = world.getParameters().getObject(SPAWNEDAMASKARI, Npc.class);
			if ((spawnedAmaskari != null) && !spawnedAmaskari.isDead() && (getRandom(1000) < 25) && LocationUtil.checkIfInRange(range, npc, spawnedAmaskari, false))
			{
				final ScheduledFuture<?> activeAmaskariCall = world.getParameters().getObject(ACTIVEAMASKARICALL, ScheduledFuture.class);
				if (activeAmaskariCall != null)
				{
					activeAmaskariCall.cancel(true);
				}
				world.setParameter(ACTIVEAMASKARICALL, ThreadPool.schedule(new CallAmaskari(npc), 25000));
			}
		}
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final InstanceWorld world = InstanceManager.getInstance().getWorld(npc);
		if (world != null)
		{
			world.setParameter(ISAMASKARIDEAD, true);
		}
	}
	
	@Override
	protected boolean checkConditions(Player player)
	{
		if (player.isGM())
		{
			return true;
		}
		
		final Party party = player.getParty();
		if ((party == null) || !party.isLeader(player))
		{
			player.sendPacket(SystemMessageId.ONLY_A_PARTY_LEADER_CAN_MAKE_THE_REQUEST_TO_ENTER);
			return false;
		}
		
		for (Player partyMember : party.getMembers())
		{
			if (partyMember.getLevel() < MIN_LV)
			{
				party.broadcastPacket(new SystemMessage(SystemMessageId.C1_S_LEVEL_DOES_NOT_CORRESPOND_TO_THE_REQUIREMENTS_FOR_ENTRY).addPcName(partyMember));
				return false;
			}
			
			if (!LocationUtil.checkIfInRange(1000, player, partyMember, true))
			{
				party.broadcastPacket(new SystemMessage(SystemMessageId.C1_IS_IN_A_LOCATION_WHICH_CANNOT_BE_ENTERED_THEREFORE_IT_CANNOT_BE_PROCESSED).addPcName(partyMember));
				return false;
			}
			
			if (InstanceManager.getInstance().getPlayerWorld(player) != null)
			{
				party.broadcastPacket(new SystemMessage(SystemMessageId.YOU_HAVE_ENTERED_ANOTHER_INSTANCE_ZONE_THEREFORE_YOU_CANNOT_ENTER_CORRESPONDING_DUNGEON).addPcName(partyMember));
				return false;
			}
		}
		return true;
	}
	
	@Override
	public void onEnterInstance(Player player, InstanceWorld world, boolean firstEntrance)
	{
		if (firstEntrance)
		{
			final Party party = player.getParty();
			if (party == null)
			{
				teleportPlayer(player, ENTRY_POINT, world.getInstanceId());
				world.addAllowed(player);
			}
			else
			{
				for (Player partyMember : party.getMembers())
				{
					teleportPlayer(partyMember, ENTRY_POINT, world.getInstanceId());
					world.addAllowed(partyMember);
				}
			}
			world.setParameter(SPAWNEDAMASKARI, addSpawn(AMASKARI, AMASKARI_SPAWN_POINT, false, 0, false, world.getInstanceId()));
		}
		else
		{
			teleportPlayer(player, ENTRY_POINT, world.getInstanceId());
		}
	}
	
	private static class CallAmaskari implements Runnable
	{
		private final Npc _caller;
		
		public CallAmaskari(Npc caller)
		{
			_caller = caller;
		}
		
		@Override
		public void run()
		{
			if ((_caller != null) && !_caller.isDead())
			{
				final InstanceWorld world = InstanceManager.getInstance().getWorld(_caller);
				if (world != null)
				{
					final Npc spawnedAmaskari = world.getParameters().getObject(SPAWNEDAMASKARI, Npc.class);
					if ((spawnedAmaskari != null) && !spawnedAmaskari.isDead())
					{
						spawnedAmaskari.teleToLocation(_caller.getLocation());
						spawnedAmaskari.broadcastPacket(new NpcSay(spawnedAmaskari.getObjectId(), ChatType.NPC_GENERAL, spawnedAmaskari.getId(), NpcStringId.I_LL_MAKE_YOU_FEEL_SUFFERING_LIKE_A_FLAME_THAT_IS_NEVER_EXTINGUISHED));
					}
				}
			}
		}
	}
	
	private class ExitInstance implements Runnable
	{
		private final Party _party;
		private final InstanceWorld _world;
		
		public ExitInstance(Party party, InstanceWorld world)
		{
			_party = party;
			_world = world;
		}
		
		@Override
		public void run()
		{
			if ((_party != null) && (_world != null))
			{
				for (Player partyMember : _party.getMembers())
				{
					if ((partyMember != null) && !partyMember.isDead())
					{
						_world.removeAllowed(partyMember);
						teleportPlayer(partyMember, EXIT_POINT, 0);
					}
				}
			}
		}
	}
}
