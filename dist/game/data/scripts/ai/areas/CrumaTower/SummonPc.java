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
package ai.areas.CrumaTower;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import com.l2journey.gameserver.model.actor.Attackable;
import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.zone.ZoneId;
import com.l2journey.gameserver.network.enums.ChatType;
import com.l2journey.gameserver.network.serverpackets.MagicSkillUse;
import com.l2journey.gameserver.network.serverpackets.NpcSay;

import ai.AbstractNpcAI;

/**
 * IA de Summon Pc.<br>
 * Puxa o jogador para o NPC ao atacar.
 * @author Zoey76, Mafias, KingHanker
 */
public class SummonPc extends AbstractNpcAI
{
	private static final int PORTA = 20213;
	private static final int PERUM = 20221;
	
	private static final String CHECK_TIMER = "CHECK_TARGET";
	private static final String PULL_TIMER = "PULL_TARGET";
	private static final String REATTACK_TIMER = "REATTACK_AFTER_PULL";
	
	private static final String LAST_TARGET = "LAST_TARGET";
	private static final String PULL_TARGET = "PULL_TARGET_OBJ";
	private static final String LAST_SUMMON_TIME = "LAST_SUMMON_TIME";
	private static final String SUMMON_PHRASE = "SUMMON_PHRASE";
	
	private static final int VISUAL_SKILL = 1086;
	private static final int VISUAL_LEVEL = 2;
	private static final int TELEPORT_VISUAL = 2036;
	private static final int CAST_TIME = 2000;
	
	private static final int CHECK_INTERVAL = 1000;
	private static final int MELEE_DISTANCE = 200;
	private static final int SUMMON_DISTANCE = 600;
	
	private static final int SUMMON_COOLDOWN_MS = 10000;
	private static final int REATTACK_DELAY = 300;
	private static final int TELEPORT_RADIUS = 80;
	private static final int ASSIST_REFRESH_RADIUS = 1200;
	
	private static final Set<Npc> ACTIVE_MOBS = ConcurrentHashMap.newKeySet();
	
	private static final String[] SUMMON_PHRASES =
	{
		"You cannot escape!",
		"Get over here!"
	};
	
	private SummonPc()
	{
		addSpawnId(PORTA, PERUM);
		addAttackId(PORTA, PERUM);
		addKillId(PORTA, PERUM);
	}
	
	@Override
	public void onSpawn(Npc npc)
	{
		if (npc == null)
		{
			return;
		}
		
		ACTIVE_MOBS.add(npc);
		
		npc.setScriptValue(0);
		npc.getVariables().remove(LAST_TARGET);
		npc.getVariables().remove(PULL_TARGET);
		npc.getVariables().remove(LAST_SUMMON_TIME);
		npc.getVariables().remove(SUMMON_PHRASE);
		
		cancelQuestTimer(CHECK_TIMER, npc, null);
		cancelQuestTimer(PULL_TIMER, npc, null);
		cancelQuestTimer(REATTACK_TIMER, npc, null);
		
		startQuestTimer(CHECK_TIMER, CHECK_INTERVAL, npc, null, true);
	}
	
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		if ((npc == null) || (attacker == null) || npc.isDead() || attacker.isDead())
		{
			return;
		}
		
		if (npc.getScriptValue() == 1)
		{
			return;
		}
		
		npc.getVariables().set(LAST_TARGET, attacker);
		
		final Attackable monster = npc.asAttackable();
		if (monster != null)
		{
			monster.addDamageHate(attacker, Math.max(1, damage), 5000);
		}
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		if (npc != null)
		{
			ACTIVE_MOBS.remove(npc);
			
			cancelQuestTimer(CHECK_TIMER, npc, null);
			cancelQuestTimer(PULL_TIMER, npc, null);
			cancelQuestTimer(REATTACK_TIMER, npc, null);
			
			npc.getVariables().remove(LAST_TARGET);
			npc.getVariables().remove(PULL_TARGET);
			npc.getVariables().remove(LAST_SUMMON_TIME);
			npc.getVariables().remove(SUMMON_PHRASE);
			
			npc.setScriptValue(0);
		}
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if ((npc == null) || npc.isDead())
		{
			return null;
		}
		
		switch (event)
		{
			case CHECK_TIMER:
			{
				handleCheckTarget(npc);
				break;
			}
			
			case PULL_TIMER:
			{
				handlePullTarget(npc);
				break;
			}
			
			case REATTACK_TIMER:
			{
				handleReattack(npc, player);
				break;
			}
		}
		
		return null;
	}
	
	private void handleCheckTarget(Npc npc)
	{
		if (npc.getScriptValue() == 1)
		{
			return;
		}
		
		final Attackable monster = npc.asAttackable();
		if (monster == null)
		{
			return;
		}
		
		final Player target = resolveTarget(npc, monster);
		
		if (!isValidSummonTarget(target))
		{
			npc.getVariables().remove(LAST_TARGET);
			return;
		}
		
		npc.setTarget(target);
		
		final double distance = npc.calculateDistance3D(target);
		
		monster.addDamageHate(target, 1000, 10000);
		
		if (distance > SUMMON_DISTANCE)
		{
			final long now = System.currentTimeMillis();
			final long lastSummon = npc.getVariables().getLong(LAST_SUMMON_TIME, 0);
			
			if ((now - lastSummon) >= SUMMON_COOLDOWN_MS)
			{
				castSummonAndPull(npc, target);
			}
			
			return;
		}
		
		if (distance > MELEE_DISTANCE)
		{
			return;
		}
		
		npc.doAttack(target);
	}
	
	private void handlePullTarget(Npc npc)
	{
		try
		{
			final Player target = npc.getVariables().getObject(PULL_TARGET, Player.class);
			
			if (!isValidSummonTarget(target))
			{
				npc.getVariables().remove(PULL_TARGET);
				npc.getVariables().remove(LAST_TARGET);
				return;
			}
			
			final Attackable monster = npc.asAttackable();
			if (monster == null)
			{
				return;
			}
			
			npc.getVariables().set(LAST_SUMMON_TIME, System.currentTimeMillis());
			
			npc.stopMove(null);
			npc.abortAttack();
			
			final String phrase = npc.getVariables().getString(SUMMON_PHRASE, SUMMON_PHRASES[0]);
			
			npc.broadcastPacket(new NpcSay(npc.getObjectId(), ChatType.NPC_SHOUT, npc.getId(), phrase));
			
			npc.broadcastPacket(new MagicSkillUse(npc, npc, VISUAL_SKILL, VISUAL_LEVEL, CAST_TIME, 0));
			
			target.teleToLocation(npc.getX() + ThreadLocalRandom.current().nextInt(-TELEPORT_RADIUS, TELEPORT_RADIUS + 1), npc.getY() + ThreadLocalRandom.current().nextInt(-TELEPORT_RADIUS, TELEPORT_RADIUS + 1), npc.getZ());
			
			target.broadcastPacket(new MagicSkillUse(target, target, TELEPORT_VISUAL, 1, 0, 0));
			
			npc.setTarget(target);
			monster.addDamageHate(target, 10000, 10000);
			
			refreshAssistMobs(npc, target);
			
			startQuestTimer(REATTACK_TIMER, REATTACK_DELAY, npc, target);
		}
		finally
		{
			npc.setScriptValue(0);
			npc.getVariables().remove(PULL_TARGET);
			npc.getVariables().remove(SUMMON_PHRASE);
		}
	}
	
	private void handleReattack(Npc npc, Player target)
	{
		if (!isValidSummonTarget(target) || npc.isDead())
		{
			return;
		}
		
		final Attackable monster = npc.asAttackable();
		if (monster == null)
		{
			return;
		}
		
		npc.setTarget(target);
		monster.addDamageHate(target, 10000, 10000);
		
		npc.doAttack(target);
	}
	
	private Player resolveTarget(Npc npc, Attackable monster)
	{
		final Creature hated = monster.getMostHated();
		
		if (hated instanceof Player player)
		{
			
			npc.getVariables().set(LAST_TARGET, player);
			
			return player;
		}
		
		return npc.getVariables().getObject(LAST_TARGET, Player.class);
	}
	
	private boolean isValidSummonTarget(Player target)
	{
		// Evita pulls desde ciudad o zonas peace.
		if ((target == null) || target.isDead() || !target.isOnline() || target.isInsideZone(ZoneId.PEACE))
		{
			return false;
		}
		
		return true;
	}
	
	private void castSummonAndPull(Npc npc, Player target)
	{
		npc.setScriptValue(1);
		
		npc.getVariables().set(LAST_TARGET, target);
		npc.getVariables().set(PULL_TARGET, target);
		
		final String phrase = SUMMON_PHRASES[ThreadLocalRandom.current().nextInt(SUMMON_PHRASES.length)];
		
		npc.getVariables().set(SUMMON_PHRASE, phrase);
		
		npc.stopMove(null);
		npc.abortAttack();
		
		npc.broadcastPacket(new NpcSay(npc.getObjectId(), ChatType.NPC_SHOUT, npc.getId(), phrase));
		
		npc.broadcastPacket(new MagicSkillUse(npc, npc, VISUAL_SKILL, VISUAL_LEVEL, CAST_TIME, 0));
		
		startQuestTimer(PULL_TIMER, CAST_TIME, npc, null);
	}
	
	private void refreshAssistMobs(Npc npc, Player target)
	{
		for (Npc assist : ACTIVE_MOBS)
		{
			if ((assist == null) || assist.isDead() || (assist == npc) || (assist.getInstanceId() != npc.getInstanceId()))
			{
				continue;
			}
			
			if (npc.calculateDistance3D(assist) > ASSIST_REFRESH_RADIUS)
			{
				continue;
			}
			
			final Object currentTarget = assist.getTarget();
			final Object hated = assist.asAttackable() != null ? assist.asAttackable().getMostHated() : null;
			
			if ((currentTarget == target) || (hated == target))
			{
				final Attackable monster = assist.asAttackable();
				
				if (monster != null)
				{
					assist.setTarget(target);
					monster.addDamageHate(target, 10000, 10000);
				}
			}
		}
	}
	
	public static void main(String[] args)
	{
		new SummonPc();
	}
}