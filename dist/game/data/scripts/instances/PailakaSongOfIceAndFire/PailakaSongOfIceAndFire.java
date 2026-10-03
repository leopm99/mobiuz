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
package instances.PailakaSongOfIceAndFire;

import com.l2journey.gameserver.managers.InstanceManager;
import com.l2journey.gameserver.model.Location;
import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.instancezone.InstanceWorld;
import com.l2journey.gameserver.model.zone.ZoneType;
import com.l2journey.gameserver.network.NpcStringId;
import com.l2journey.gameserver.network.enums.ChatType;

import instances.AbstractInstance;

/**
 * Pailaka Song of Ice and Fire Instance zone.
 * @author Gnacik, St3eT
 */
public class PailakaSongOfIceAndFire extends AbstractInstance
{
	// NPCs
	private static final int ADLER1 = 32497;
	private static final int GARGOS = 18607;
	private static final int BLOOM = 18616;
	private static final int BOTTLE = 32492;
	private static final int BRAZIER = 32493;
	// Items
	private static final int FIRE_ENHANCER = 13040;
	private static final int WATER_ENHANCER = 13041;
	private static final int SHIELD_POTION = 13032;
	private static final int HEAL_POTION = 13033;
	// Location
	private static final Location TELEPORT = new Location(-52875, 188232, -4696);
	// Misc
	private static final int TEMPLATE_ID = 43;
	private static final int ZONE = 20108;
	
	private PailakaSongOfIceAndFire()
	{
		addStartNpc(ADLER1);
		addTalkId(ADLER1);
		addAttackId(BOTTLE, BRAZIER);
		addExitZoneId(ZONE);
		addSpawnId(BLOOM);
		addKillId(BLOOM);
		addCreatureSeeId(GARGOS);
	}
	
	@Override
	public void onEnterInstance(Player player, InstanceWorld world, boolean firstEntrance)
	{
		if (firstEntrance)
		{
			world.addAllowed(player);
		}
		teleportPlayer(player, TELEPORT, world.getInstanceId());
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "enter":
			{
				enterInstance(player, TEMPLATE_ID);
				break;
			}
			case "GARGOS_LAUGH":
			{
				npc.broadcastSay(ChatType.NPC_SHOUT, NpcStringId.OHH_OH_OH);
				break;
			}
			case "TELEPORT":
			{
				teleportPlayer(player, TELEPORT, player.getInstanceId());
				break;
			}
			case "DELETE":
			{
				if (npc != null)
				{
					npc.deleteMe();
				}
				break;
			}
			case "BLOOM_TIMER":
			{
				startQuestTimer("BLOOM_TIMER2", getRandom(2, 4) * 60 * 1000, npc, null);
				break;
			}
			case "BLOOM_TIMER2":
			{
				npc.setInvisible(!npc.isInvisible());
				startQuestTimer("BLOOM_TIMER", 5000, npc, null);
				break;
			}
		}
		return super.onEvent(event, npc, player);
	}
	
	@Override
	public void onAttack(Npc npc, Player player, int damage, boolean isSummon)
	{
		if ((damage > 0) && npc.isScriptValue(0))
		{
			switch (getRandom(6))
			{
				case 0:
				{
					if (npc.getId() == BOTTLE)
					{
						npc.dropItem(player, WATER_ENHANCER, getRandom(1, 6));
					}
					break;
				}
				case 1:
				{
					if (npc.getId() == BRAZIER)
					{
						npc.dropItem(player, FIRE_ENHANCER, getRandom(1, 6));
					}
					break;
				}
				case 2, 3:
				{
					npc.dropItem(player, SHIELD_POTION, getRandom(1, 10));
					break;
				}
				case 4, 5:
				{
					npc.dropItem(player, HEAL_POTION, getRandom(1, 10));
					break;
				}
			}
			npc.setScriptValue(1);
			startQuestTimer("DELETE", 3000, npc, null);
		}
	}
	
	@Override
	public void onKill(Npc npc, Player player, boolean isSummon)
	{
		npc.dropItem(player, getRandomBoolean() ? SHIELD_POTION : HEAL_POTION, getRandom(1, 7));
	}
	
	@Override
	public void onExitZone(Creature creature, ZoneType zone)
	{
		if ((creature.isPlayer()) && !creature.isDead() && !creature.isTeleporting() && creature.asPlayer().isOnline())
		{
			final InstanceWorld world = InstanceManager.getInstance().getWorld(creature);
			if ((world != null) && (world.getTemplateId() == TEMPLATE_ID))
			{
				startQuestTimer("TELEPORT", 1000, null, creature.asPlayer());
			}
		}
	}
	
	@Override
	public void onCreatureSee(Npc npc, Creature creature)
	{
		if (creature.isPlayer() && npc.isScriptValue(0))
		{
			npc.setScriptValue(1);
			startQuestTimer("GARGOS_LAUGH", 1000, npc, creature.asPlayer());
		}
	}
	
	@Override
	public void onSpawn(Npc npc)
	{
		npc.setInvisible(true);
		startQuestTimer("BLOOM_TIMER", 1000, npc, null);
	}
	
	public static void main(String[] args)
	{
		new PailakaSongOfIceAndFire();
	}
}
