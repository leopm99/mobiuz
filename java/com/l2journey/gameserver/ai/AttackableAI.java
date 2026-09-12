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
package com.l2journey.gameserver.ai;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

import com.l2journey.Config;
import com.l2journey.EventsConfig;
import com.l2journey.commons.threads.ThreadPool;
import com.l2journey.commons.util.Rnd;
import com.l2journey.gameserver.GeoData;
import com.l2journey.gameserver.managers.DimensionalRiftManager;
import com.l2journey.gameserver.model.Location;
import com.l2journey.gameserver.model.Spawn;
import com.l2journey.gameserver.model.World;
import com.l2journey.gameserver.model.WorldObject;
import com.l2journey.gameserver.model.WorldRegion;
import com.l2journey.gameserver.model.actor.Attackable;
import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.enums.npc.AISkillScope;
import com.l2journey.gameserver.model.actor.enums.npc.AIType;
import com.l2journey.gameserver.model.actor.instance.FestivalMonster;
import com.l2journey.gameserver.model.actor.instance.FriendlyMob;
import com.l2journey.gameserver.model.actor.instance.GrandBoss;
import com.l2journey.gameserver.model.actor.instance.Guard;
import com.l2journey.gameserver.model.actor.instance.Monster;
import com.l2journey.gameserver.model.actor.instance.RaidBoss;
import com.l2journey.gameserver.model.actor.instance.RiftInvader;
import com.l2journey.gameserver.model.actor.instance.StaticObject;
import com.l2journey.gameserver.model.actor.templates.NpcTemplate;
import com.l2journey.gameserver.model.effects.EffectType;
import com.l2journey.gameserver.model.events.EventDispatcher;
import com.l2journey.gameserver.model.events.EventType;
import com.l2journey.gameserver.model.events.holders.actor.npc.attackable.OnAttackableFactionCall;
import com.l2journey.gameserver.model.events.holders.actor.npc.attackable.OnAttackableHate;
import com.l2journey.gameserver.model.events.returns.TerminateReturn;
import com.l2journey.gameserver.model.groups.Party;
import com.l2journey.gameserver.model.skill.AbnormalType;
import com.l2journey.gameserver.model.skill.AbnormalVisualEffect;
import com.l2journey.gameserver.model.skill.Skill;
import com.l2journey.gameserver.model.skill.targets.TargetType;
import com.l2journey.gameserver.model.zone.ZoneId;
import com.l2journey.gameserver.taskmanagers.AttackableThinkTaskManager;
import com.l2journey.gameserver.taskmanagers.GameTimeTaskManager;
import com.l2journey.gameserver.util.LocationUtil;

/**
 * Esta classe gerencia a IA de Attackable.
 * @author Zoey76
 */
public class AttackableAI extends CreatureAI
{
	/**
	 * Tarefa de medo.
	 * @author Zoey76
	 */
	public static class FearTask implements Runnable
	{
		private final AttackableAI _ai;
		private final Creature _effector;
		private boolean _start;
		
		public FearTask(AttackableAI ai, Creature effector, boolean start)
		{
			_ai = ai;
			_effector = effector;
			_start = start;
		}
		
		@Override
		public void run()
		{
			if (_effector != null)
			{
				final int fearTimeLeft = _ai.getFearTime() - FEAR_TICKS;
				_ai.setFearTime(fearTimeLeft);
				_ai.onActionAfraid(_effector, _start);
				_start = false;
			}
		}
	}
	
	protected static final int FEAR_TICKS = 5;
	private static final int RANDOM_WALK_RATE = 30; // confirmed
	private static final int MAX_ATTACK_TIMEOUT = 1200; // int ticks, i.e. 2min
	
	/** O atraso apos o qual o ataque e parado. */
	private int _attackTimeout;
	/** O contador de aggro do Attackable. */
	private int _globalAggro;
	/** Flag usada para indicar que uma acao de pensamento esta em progresso, para prevenir pensamento recursivo. */
	private boolean _thinking;
	private int _chaosTime = 0;
	
	// Parametros de medo
	private int _fearTime;
	private Future<?> _fearTask = null;
	
	/**
	 * Construtor da AttackableAI.
	 * @param creature a criatura
	 */
	public AttackableAI(Attackable creature)
	{
		super(creature);
		_attackTimeout = Integer.MAX_VALUE;
		_globalAggro = -10; // 10 segundos de timeout de ATTACK apos respawn
	}
	
	/**
	 * <b><u>Ator e um GuardInstance</u>:</b>
	 * <ul>
	 * <li>O alvo nao e um Folk ou uma Door</li>
	 * <li>O alvo nao esta morto, nao e invulneravel, nao esta em modo de movimento silencioso E muito longe (>100)</li>
	 * <li>O alvo esta no alcance de Aggro do ator e na mesma altura</li>
	 * <li>O alvo Player tem karma (=PK)</li>
	 * <li>O alvo Monster e agressivo</li>
	 * </ul>
	 * <br>
	 * <b><u>Ator e um SiegeGuard</u>:</b>
	 * <ul>
	 * <li>O alvo nao e um Folk ou uma Door</li>
	 * <li>O alvo nao esta morto, nao e invulneravel, nao esta em modo de movimento silencioso E muito longe (>100)</li>
	 * <li>O alvo esta no alcance de Aggro do ator e na mesma altura</li>
	 * <li>Um cerco esta em progresso</li>
	 * <li>O alvo Player nao e um Defender</li>
	 * </ul>
	 * <br>
	 * <b><u>Ator e um FriendlyMob</u>:</b>
	 * <ul>
	 * <li>O alvo nao e um Folk, uma Door ou outro Npc</li>
	 * <li>O alvo nao esta morto, nao e invulneravel, nao esta em modo de movimento silencioso E muito longe (>100)</li>
	 * <li>O alvo esta no alcance de Aggro do ator e na mesma altura</li>
	 * <li>O alvo Player tem karma (=PK)</li>
	 * </ul>
	 * <br>
	 * <b><u>Ator e um Monster</u>:</b>
	 * <ul>
	 * <li>O alvo nao e um Folk, uma Door ou outro Npc</li>
	 * <li>O alvo nao esta morto, nao e invulneravel, nao esta em modo de movimento silencioso E muito longe (>100)</li>
	 * <li>O alvo esta no alcance de Aggro do ator e na mesma altura</li>
	 * <li>O ator e Agressivo</li>
	 * </ul>
	 * @param target O WorldObject alvo
	 * @return {@code true} se o alvo pode ser atacado automaticamente devido a agressao.
	 */
	private boolean isAggressiveTowards(Creature target)
	{
		if ((target == null) || (getActiveChar() == null))
		{
			return false;
		}
		
		// Verifica se o alvo nao e invulneravel
		if (target.isInvul())
		{
			// Porem EffectInvincible requer verificar GMs especialmente
			if ((target.isPlayer() && target.isGM()) || (target.isSummon() && target.asSummon().getOwner().isGM()))
			{
				return false;
			}
		}
		
		// Verifica se o alvo nao e um Folk ou uma Door
		if (target.isDoor())
		{
			return false;
		}
		
		// Verifica se o alvo nao esta morto, esta no alcance de Aggro e na mesma altura
		final Attackable me = getActiveChar();
		if (target.isAlikeDead() || (target.isPlayable() && !me.isInsideRadius3D(target, me.getAggroRange())))
		{
			return false;
		}
		
		// Verifica se o alvo e um Playable e se a IA nao e um Raid Boss, pode ver jogadores em Movimento Silencioso e o alvo nao esta em modo silencioso
		if (target.isPlayable() && !(me.isRaid()) && !(me.canSeeThroughSilentMove()) && target.asPlayable().isSilentMovingAffected())
		{
			return false;
		}
		
		// Obtem o jogador se houver algum.
		final Player player = target.asPlayer();
		if (player != null)
		{
			// Nao pega aggro se o GM tem nivel de acesso abaixo ou igual a GM_DONT_TAKE_AGGRO
			// verifica se o alvo esta no periodo de graca de ter acabado de levantar de morte falsa
			if ((player.isGM() && !player.getAccessLevel().canTakeAggro()) || player.isRecentFakeDeath())
			{
				return false;
			}
			
			if (EventsConfig.FACTION_SYSTEM_ENABLED && EventsConfig.FACTION_GUARDS_ENABLED && ((player.isGood() && _actor.asNpc().getTemplate().isClan(EventsConfig.FACTION_EVIL_TEAM_NAME)) || (player.isEvil() && _actor.asNpc().getTemplate().isClan(EventsConfig.FACTION_GOOD_TEAM_NAME))))
			{
				return true;
			}
			
			// Verificacao de Dimensional Rift.
			if ((me instanceof RiftInvader) && player.isInParty())
			{
				final Party party = player.getParty();
				if (party.isInDimensionalRift())
				{
					final byte riftType = party.getDimensionalRift().getType();
					final byte riftRoom = party.getDimensionalRift().getCurrentRoom();
					if (!DimensionalRiftManager.getInstance().getRoom(riftType, riftRoom).checkIfInZone(me.getX(), me.getY(), me.getZ()))
					{
						return false;
					}
				}
			}
		}
		
		// Verifica se o ator e um GuardInstance
		if (me instanceof Guard)
		{
			// Verifica se o alvo Player tem karma (=PK)
			if ((player != null) && (player.getKarma() > 0))
			{
				return GeoData.getInstance().canSeeTarget(me, player); // Verificacao de Linha de Visao
			}
			
			// Verifica se o alvo Monster e agressivo
			if (target.isMonster() && Config.GUARD_ATTACK_AGGRO_MOB)
			{
				return (target.asMonster().isAggressive() && GeoData.getInstance().canSeeTarget(me, target));
			}
			
			return false;
		}
		else if (me instanceof FriendlyMob)
		{
			// Verifica se o alvo nao e outro Npc
			if (target instanceof Npc)
			{
				return false;
			}
			
			// Verifica se o alvo Player tem karma (=PK)
			if (target.isPlayer() && (target.asPlayer().getKarma() > 0))
			{
				return GeoData.getInstance().canSeeTarget(me, target); // Verificacao de Linha de Visao
			}
			
			return false;
		}
		else
		{
			if (target.isAttackable())
			{
				if (!target.isAutoAttackable(me))
				{
					return false;
				}
				
				if (me.isChaos() && me.isInsideRadius2D(target, me.getAggroRange()))
				{
					if (target.asAttackable().isInMyClan(me))
					{
						return false;
					}
					
					// Verificacao de Linha de Visao
					return GeoData.getInstance().canSeeTarget(me, target);
				}
			}
			
			if (target.isAttackable() || (target instanceof Npc))
			{
				return false;
			}
			
			// dependendo da config, nao permite que mobs ataquem _novos_ jogadores em zonas de paz,
			// a menos que ja estejam seguindo esses jogadores de fora da zona de paz.
			if (!Config.ALT_MOB_AGRO_IN_PEACEZONE && target.isInsideZone(ZoneId.PEACE) && target.isInsideZone(ZoneId.NO_PVP))
			{
				return false;
			}
			
			if (me.isChampion() && Config.CHAMPION_PASSIVE)
			{
				return false;
			}
			
			// Verifica se o ator e Agressivo.
			return me.isAggressive() && GeoData.getInstance().canSeeTarget(me, target);
		}
	}
	
	public void startAITask()
	{
		AttackableThinkTaskManager.getInstance().add(getActiveChar());
	}
	
	@Override
	public void stopAITask()
	{
		AttackableThinkTaskManager.getInstance().remove(getActiveChar());
		super.stopAITask();
	}
	
	/**
	 * Define a Intencao desta CreatureAI e cria uma Tarefa de IA executada a cada 1s (chama metodo onActionThink) para este Attackable.<br>
	 * <font color=#FF0000><b><u>Atencao</u>: Se _knowPlayer do ator nao estiver VAZIO, IDLE sera mudado para ACTIVE</b></font>
	 * @param newIntention A nova Intencao a ser definida para a IA
	 * @param arg0 O primeiro parametro da Intencao
	 * @param arg1 O segundo parametro da Intencao
	 */
	@Override
	synchronized void changeIntention(Intention newIntention, Object arg0, Object arg1)
	{
		Intention intention = newIntention;
		if ((intention == Intention.IDLE) || (intention == Intention.ACTIVE))
		{
			// Verifica se o ator nao esta morto
			final Attackable npc = getActiveChar();
			if (!npc.isAlikeDead())
			{
				// Se seu _knownPlayer nao estiver vazio, define a Intencao para ACTIVE
				if (!World.getInstance().getVisibleObjects(npc, Player.class).isEmpty())
				{
					intention = Intention.ACTIVE;
				}
				else if ((npc.getSpawn() != null) && !npc.isInsideRadius3D(npc.getSpawn().getLocation(), 2 * (npc.getSpawn().getRandomWalkRange() > 0 ? npc.getSpawn().getRandomWalkRange() : Config.MAX_DRIFT_RANGE)))
				{
					intention = Intention.ACTIVE;
				}
			}
			
			if (intention == Intention.IDLE)
			{
				// Define a Intencao desta AttackableAI para IDLE
				super.changeIntention(Intention.IDLE, null, null);
				
				// Para tarefa de IA e desanexa IA do NPC
				stopAITask();
				
				// Cancela a IA
				_actor.detachAI();
				return;
			}
		}
		
		// Define a Intencao desta AttackableAI para intention
		super.changeIntention(intention, arg0, arg1);
		
		// Se nao estiver idle - cria uma tarefa de IA (agenda onActionThink repetidamente)
		startAITask();
	}
	
	/**
	 * Gerencia a Intencao Attack: Para Ataque atual (se necessario), Calcula timeout de ataque, Inicia novo Ataque e Executa Acao Think.
	 * @param target A Creature a atacar
	 */
	@Override
	protected void onIntentionAttack(Creature target)
	{
		// Calcula o timeout de ataque
		_attackTimeout = MAX_ATTACK_TIMEOUT + GameTimeTaskManager.getInstance().getGameTicks();
		
		// Gerencia a Intencao Attack: Para Ataque atual (se necessario), Inicia novo Ataque e Executa Acao Think
		super.onIntentionAttack(target);
	}
	
	@Override
	protected void onActionAfraid(Creature effector, boolean start)
	{
		if ((_fearTime > 0) && (_fearTask == null))
		{
			_fearTask = ThreadPool.scheduleAtFixedRate(new FearTask(this, effector, start), 0, FEAR_TICKS * 1000); // seconds
			_actor.startAbnormalVisualEffect(true, AbnormalVisualEffect.TURN_FLEE);
		}
		else
		{
			super.onActionAfraid(effector, start);
			
			if ((_actor.isDead() || (_fearTime <= 0)) && (_fearTask != null))
			{
				_fearTask.cancel(true);
				_fearTask = null;
				_actor.stopAbnormalVisualEffect(true, AbnormalVisualEffect.TURN_FLEE);
				setIntention(Intention.IDLE);
			}
		}
	}
	
	protected void thinkCast()
	{
		if (checkTargetLost(getCastTarget()))
		{
			setCastTarget(null);
			return;
		}
		
		if (maybeMoveToPawn(getCastTarget(), _actor.getMagicalAttackRange(_skill)))
		{
			return;
		}
		
		clientStopMoving(null);
		setIntention(Intention.ACTIVE);
		_actor.doCast(_skill);
	}
	
	/**
	 * Gerencia pensamentos padrao de IA de um Attackable (chamado por onActionThink). <b><u>Acoes</u>:</b>
	 * <ul>
	 * <li>Atualiza a cada 1s o contador _globalAggro para se aproximar de 0</li>
	 * <li>Se o ator for Agressivo e puder atacar, adiciona todas as Creatures atacaveis automaticamente em seu Alcance de Aggro a sua _aggroList, escolhe um alvo e ordena atacar</li>
	 * <li>Se o ator for um GuardInstance que nao pode atacar, ordena retornar a sua localizacao base</li>
	 * <li>Se o ator for um Monster que nao pode atacar, ordena caminhar aleatoriamente (1/100)</li>
	 * </ul>
	 */
	protected void thinkActive()
	{
		// Verifica se a regiao e seus vizinhos estao ativos.
		final WorldRegion region = _actor.getWorldRegion();
		if ((region == null) || !region.areNeighborsActive())
		{
			return;
		}
		
		final Attackable npc = getActiveChar();
		
		// Atualiza a cada 1s o contador _globalAggro para se aproximar de 0
		if (_globalAggro != 0)
		{
			if (_globalAggro < 0)
			{
				_globalAggro++;
			}
			else
			{
				_globalAggro--;
			}
		}
		
		// Adiciona todas as Creatures atacaveis automaticamente no Alcance de Aggro do Attackable a sua _aggroList com 0 de dano e 1 de odio
		// Um Attackable nao e agressivo durante 10s apos seu spawn porque _globalAggro e definido como -10
		if (_globalAggro >= 0)
		{
			World.getInstance().forEachVisibleObject(npc, Creature.class, target ->
			{
				if ((target instanceof StaticObject))
				{
					return;
				}
				
				/*
				 * Verifica se este e um spawn de mob de festival. Se for, verifica se o gatilho de aggro e um participante do festival...se sim, move para atacar.
				 */
				if ((npc instanceof FestivalMonster) && target.isPlayer())
				{
					final Player targetPlayer = target.asPlayer();
					if (!(targetPlayer.isFestivalParticipant()))
					{
						return;
					}
				}
				
				// Para cada Creature verifica se o alvo e atacavel automaticamente
				if (isAggressiveTowards(target)) // verifica agressao
				{
					if (target.isPlayable() && EventDispatcher.getInstance().hasListener(EventType.ON_NPC_HATE, getActiveChar()))
					{
						final TerminateReturn term = EventDispatcher.getInstance().notifyEvent(new OnAttackableHate(getActiveChar(), target.asPlayer(), target.isSummon()), getActiveChar(), TerminateReturn.class);
						if ((term != null) && term.terminate())
						{
							return;
						}
					}
					
					if (npc.getHating(target) == 0)
					{
						npc.addDamageHate(target, 0, 0);
					}
				}
			});
			
			// Escolhe um alvo da sua aggroList
			final Creature hated = npc.isConfused() ? getAttackTarget() : npc.getMostHated();
			
			// Ordena ao Attackable atacar o alvo
			if ((hated != null) && !npc.isCoreAIDisabled())
			{
				// Obtem o nivel de odio do Attackable contra este alvo Creature contido em _aggroList
				final long aggro = npc.getHating(hated);
				if ((aggro + _globalAggro) > 0)
				{
					// Define o tipo de movimento da Creature para correr e envia pacote Servidor->Cliente ChangeMoveType para todos os outros Players
					if (!npc.isRunning())
					{
						npc.setRunning();
					}
					
					// Define a Intencao da IA para ATTACK
					setIntention(Intention.ATTACK, hated);
				}
				
				return;
			}
		}
		
		// Chance de esquecer atacantes apos algum tempo
		if ((npc.getCurrentHp() == npc.getMaxHp()) && (npc.getCurrentMp() == npc.getMaxMp()) && !npc.getAttackByList().isEmpty() && (Rnd.get(500) == 0))
		{
			npc.clearAggroList();
			npc.getAttackByList().clear();
		}
		
		// Se este e um monstro de festival, entao ele permanece no mesmo local.
		// if (npc instanceof FestivalMonster)
		// {
		// return;
		// }
		
		// Verifica se o mob nao deve retornar ao ponto de spawn
		if (!npc.canReturnToSpawnPoint()
		/* || npc.isReturningToSpawnPoint() */ ) // Commented because sometimes it stops movement.
		{
			return;
		}
		
		// Ordena este attackable retornar ao seu spawn porque nao ha alvo para atacar
		if (!npc.isWalker() && (npc.getSpawn() != null) && (npc.calculateDistance2D(npc.getSpawn()) > (npc.getSpawn().getRandomWalkRange() > 0 ? npc.getSpawn().getRandomWalkRange() : Config.MAX_DRIFT_RANGE)) && ((getTarget() == null) || getTarget().isInvisible() || (getTarget().isPlayer() && !Config.ATTACKABLES_CAMP_PLAYER_CORPSES && getTarget().asPlayer().isAlikeDead())))
		{
			npc.setWalking();
			npc.returnHome();
			return;
		}
		
		// Nao deixa jogador morto
		if ((getTarget() != null) && getTarget().isPlayer() && getTarget().asPlayer().isAlikeDead())
		{
			return;
		}
		
		// Minions seguindo o lider
		final Creature leader = npc.getLeader();
		if ((leader != null) && !leader.isAlikeDead())
		{
			final int offset;
			final int minRadius = 30;
			if (npc.isRaidMinion())
			{
				offset = 500; // para Raids - precisa correcao
			}
			else
			{
				offset = 200; // para minions normais - precisa correcao :)
			}
			
			if (leader.isRunning())
			{
				npc.setRunning();
			}
			else
			{
				npc.setWalking();
			}
			
			if (npc.calculateDistance2D(leader) > offset)
			{
				int x1 = Rnd.get(minRadius * 2, offset * 2); // x
				int y1 = Rnd.get(x1, offset * 2); // distance
				y1 = (int) Math.sqrt((y1 * y1) - (x1 * x1)); // y
				x1 = x1 > (offset + minRadius) ? (leader.getX() + x1) - offset : (leader.getX() - x1) + minRadius;
				y1 = y1 > (offset + minRadius) ? (leader.getY() + y1) - offset : (leader.getY() - y1) + minRadius;
				// Move o ator para Localizacao (x,y,z) no lado do servidor E do cliente enviando pacote Servidor->Cliente MoveToLocation (broadcast)
				moveTo(x1, y1, leader.getZ());
				return;
			}
			if (Rnd.get(RANDOM_WALK_RATE) == 0)
			{
				for (Skill sk : npc.getTemplate().getAISkills(AISkillScope.BUFF))
				{
					// Skip buffs that are already active.
					if (npc.isAffectedBySkill(sk.getId()))
					{
						continue;
					}
					
					// Only consider skills if we have enough MP.
					if (npc.getCurrentMp() <= sk.getMpConsume())
					{
						continue;
					}
					
					if (sk.getAbnormalType() == AbnormalType.LIFE_FORCE_OTHERS)
					{
						if (npc.getCurrentHp() >= npc.getMaxHp())
						{
							continue;
						}
					}
					
					npc.setTarget(npc);
					npc.doCast(sk);
					return;
				}
			}
		}
		// Ordena ao Monster caminhar aleatoriamente (1/100)
		else if ((npc.getSpawn() != null) && (Rnd.get(RANDOM_WALK_RATE) == 0) && npc.isRandomWalkingEnabled())
		{
			for (Skill sk : npc.getTemplate().getAISkills(AISkillScope.BUFF))
			{
				// Skip buffs that are already active.
				if (npc.isAffectedBySkill(sk.getId()))
				{
					continue;
				}
				
				// Only consider skills if we have enough MP.
				if (npc.getCurrentMp() <= sk.getMpConsume())
				{
					continue;
				}
				
				if (sk.getAbnormalType() == AbnormalType.LIFE_FORCE_OTHERS)
				{
					if (npc.getCurrentHp() >= npc.getMaxHp())
					{
						continue;
					}
				}
				
				npc.setTarget(npc);
				npc.doCast(sk);
				return;
			}
			
			int x1 = npc.getSpawn().getX();
			int y1 = npc.getSpawn().getY();
			int z1 = npc.getSpawn().getZ();
			final int driftRange = npc.getSpawn().getRandomWalkRange() > 0 ? npc.getSpawn().getRandomWalkRange() : Config.MAX_DRIFT_RANGE;
			if (npc.isInsideRadius2D(x1, y1, 0, driftRange))
			{
				final int deltaX = Rnd.get(driftRange * 2); // x
				int deltaY = Rnd.get(deltaX, driftRange * 2); // distance
				deltaY = (int) Math.sqrt((deltaY * deltaY) - (deltaX * deltaX)); // y
				x1 = (deltaX + x1) - driftRange;
				y1 = (deltaY + y1) - driftRange;
				z1 = npc.getZ();
			}
			
			// Move o ator para Localizacao (x,y,z) no lado do servidor E do cliente enviando pacote Servidor->Cliente MoveToLocation (broadcast)
			final Location moveLoc = _actor.isFlying() ? new Location(x1, y1, z1) : GeoData.getInstance().moveCheck(npc.getX(), npc.getY(), npc.getZ(), x1, y1, z1, npc.getInstanceId());
			if (LocationUtil.calculateDistance(npc.getSpawn(), moveLoc, false, false) <= driftRange)
			{
				moveTo(moveLoc.getX(), moveLoc.getY(), moveLoc.getZ());
			}
		}
	}
	
	/**
	 * Gerencia pensamentos de ataque de IA de um Attackable (chamado por onActionThink).<br>
	 * <br>
	 * <b><u>Acoes</u>:</b>
	 * <ul>
	 * <li>Atualiza o timeout de ataque se o ator estiver correndo</li>
	 * <li>Se o alvo estiver morto ou timeout expirar, para este ataque e define a Intencao para ACTIVE</li>
	 * <li>Chama todos os WorldObject de sua Facao dentro do Alcance de Facao</li>
	 * <li>Escolhe um alvo e ordena atacar com skill magica ou ataque fisico</li>
	 * </ul>
	 */
	protected void thinkAttack()
	{
		final Attackable npc = getActiveChar();
		if ((npc == null) || npc.isCastingNow())
		{
			return;
		}
		
		if (Config.AGGRO_DISTANCE_CHECK_ENABLED && npc.isMonster() && !npc.isWalker() && !(npc instanceof GrandBoss))
		{
			final Spawn spawn = npc.getSpawn();
			if ((spawn != null) && (npc.calculateDistance2D(spawn.getLocation()) > (spawn.getChaseRange() > 0 ? Math.max(Config.MAX_DRIFT_RANGE, spawn.getChaseRange()) : npc.isRaid() ? Config.AGGRO_DISTANCE_CHECK_RAID_RANGE : Config.AGGRO_DISTANCE_CHECK_RANGE)))
			{
				if ((Config.AGGRO_DISTANCE_CHECK_RAIDS || !npc.isRaid()) && (Config.AGGRO_DISTANCE_CHECK_INSTANCES || (npc.getInstanceId() == 0)))
				{
					if (Config.AGGRO_DISTANCE_CHECK_RESTORE_LIFE)
					{
						npc.setCurrentHp(npc.getMaxHp());
						npc.setCurrentMp(npc.getMaxMp());
					}
					
					npc.abortAttack();
					npc.clearAggroList();
					npc.getAttackByList().clear();
					
					if (npc.hasAI())
					{
						npc.getAI().setIntention(Intention.MOVE_TO, spawn.getLocation());
					}
					else
					{
						npc.teleToLocation(spawn.getLocation(), true);
					}
					
					// Minions devem retornar tambem.
					if (_actor.asMonster().hasMinions())
					{
						for (Monster minion : _actor.asMonster().getMinionList().getSpawnedMinions())
						{
							if (Config.AGGRO_DISTANCE_CHECK_RESTORE_LIFE)
							{
								minion.setCurrentHp(minion.getMaxHp());
								minion.setCurrentMp(minion.getMaxMp());
							}
							
							minion.abortAttack();
							minion.clearAggroList();
							minion.getAttackByList().clear();
							
							if (minion.hasAI())
							{
								minion.getAI().setIntention(Intention.MOVE_TO, spawn.getLocation());
							}
							else
							{
								minion.teleToLocation(spawn.getLocation(), true);
							}
						}
					}
					
					return;
				}
			}
		}
		
		if (npc.isCoreAIDisabled())
		{
			return;
		}
		
		final Creature mostHate = npc.getMostHated();
		if (mostHate == null)
		{
			setIntention(Intention.ACTIVE);
			return;
		}
		
		setAttackTarget(mostHate);
		npc.setTarget(mostHate);
		
		// Condicao de imobilizacao
		if (npc.isMovementDisabled())
		{
			movementDisable();
			return;
		}
		
		// Verifica se o alvo esta morto ou se o timeout expirou para parar este ataque
		final Creature originalAttackTarget = getAttackTarget();
		if ((originalAttackTarget == null) || originalAttackTarget.isAlikeDead())
		{
			// Para de odiar este alvo apos o timeout de ataque ou se o alvo estiver morto
			npc.stopHating(originalAttackTarget);
			return;
		}
		
		if (_attackTimeout < GameTimeTaskManager.getInstance().getGameTicks())
		{
			// Define a Intencao da IA para ACTIVE
			setIntention(Intention.ACTIVE);
			
			// Limpa alvo para o monstro poder retornar ao ponto de spawn.
			// Lista de aggro sera limpa naturalmente quando HP/MP regenerarem completamente.
			setTarget(null);
			
			npc.setWalking();
			
			// Monstro teleporta para spawn
			if (npc.isMonster() && (npc.getSpawn() != null) && (npc.getInstanceId() == 0) && (npc.isInCombat() || World.getInstance().getVisibleObjects(npc, Player.class).isEmpty()))
			{
				npc.teleToLocation(npc.getSpawn(), false);
			}
			
			return;
		}
		
		// O ator deve ser capaz de ver o alvo.
		if (!GeoData.getInstance().canSeeTarget(_actor, originalAttackTarget))
		{
			if (_actor.calculateDistance3D(originalAttackTarget) < 6000)
			{
				moveTo(originalAttackTarget);
			}
			
			return;
		}
		
		final NpcTemplate template = npc.getTemplate();
		final int collision = template.getCollisionRadius();
		
		// Trata todos os WorldObject de sua Facao dentro do Alcance de Facao
		
		final Set<Integer> clans = template.getClans();
		if ((clans != null) && !clans.isEmpty())
		{
			final int factionRange = template.getClanHelpRange() + collision;
			// Percorre todos os WorldObject que pertencem a sua facao
			try
			{
				final Creature finalTarget = originalAttackTarget;
				// Chama npcs amigaveis por ajuda apenas se este NPC foi atacado pela criatura alvo.
				boolean targetExistsInAttackByList = false;
				for (Creature reference : npc.getAttackByList())
				{
					if (reference == finalTarget)
					{
						targetExistsInAttackByList = true;
						break;
					}
				}
				
				if (targetExistsInAttackByList)
				{
					World.getInstance().forEachVisibleObjectInRange(npc, Attackable.class, factionRange, nearby ->
					{
						// Nao chama npcs mortos, npcs sem ia ou npcs que estao muito longe.
						if (nearby.isDead() || !nearby.hasAI() || (Math.abs(finalTarget.getZ() - nearby.getZ()) > 600))
						{
							return;
						}
						
						// Nao chama npcs que ja estao fazendo alguma acao (ex: atacando, conjurando).
						if ((nearby.getAI()._intention != Intention.IDLE) && (nearby.getAI()._intention != Intention.ACTIVE))
						{
							return;
						}
						
						// Nao chama npcs que nao estao no mesmo cla.
						final NpcTemplate nearbytemplate = nearby.getTemplate();
						if (!template.isClan(nearbytemplate.getClans()) || (nearbytemplate.hasIgnoreClanNpcIds() && nearbytemplate.getIgnoreClanNpcIds().contains(npc.getId())))
						{
							return;
						}
						
						if (finalTarget.isPlayable())
						{
							// Verificacao de Dimensional Rift.
							if (finalTarget.isInParty() && finalTarget.getParty().isInDimensionalRift())
							{
								final byte riftType = finalTarget.getParty().getDimensionalRift().getType();
								final byte riftRoom = finalTarget.getParty().getDimensionalRift().getCurrentRoom();
								if ((npc instanceof RiftInvader) && !DimensionalRiftManager.getInstance().getRoom(riftType, riftRoom).checkIfInZone(npc.getX(), npc.getY(), npc.getZ()))
								{
									return;
								}
							}
							
							// Por padrao, quando um membro da facao pede ajuda, ataca o atacante do chamador.
							// Notifica a IA com AGGRESSION
							nearby.getAI().notifyAction(Action.AGGRESSION, finalTarget, 1);
							
							if (EventDispatcher.getInstance().hasListener(EventType.ON_ATTACKABLE_FACTION_CALL, nearby))
							{
								EventDispatcher.getInstance().notifyEventAsync(new OnAttackableFactionCall(nearby, npc, finalTarget.asPlayer(), finalTarget.isSummon()), nearby);
							}
						}
						else if (nearby.getAI()._intention != Intention.ATTACK)
						{
							nearby.addDamageHate(finalTarget, 0, npc.getHating(finalTarget));
							nearby.getAI().setIntention(Intention.ATTACK, finalTarget);
						}
					});
				}
			}
			catch (NullPointerException e)
			{
				// LOGGER.warning(getClass().getSimpleName() + ": There has been a problem trying to think the attack!", e);
			}
		}
		
		// Inicializa dados
		final List<Skill> aiSuicideSkills = template.getAISkills(AISkillScope.SUICIDE);
		if (!aiSuicideSkills.isEmpty() && ((int) ((npc.getCurrentHp() / npc.getMaxHp()) * 100) < 30))
		{
			final Skill skill = aiSuicideSkills.get(Rnd.get(aiSuicideSkills.size()));
			if (LocationUtil.checkIfInRange(skill.getAffectRange(), npc, mostHate, false) && npc.hasSkillChance() && cast(skill))
			{
				return;
			}
		}
		
		// ------------------------------------------------------
		// Caso muitos mobs estejam tentando acertar do mesmo lugar, move um pouco, circulando ao redor do alvo.
		// Nota do Gnacik:
		// No l2js por causa disso as vezes mobs nao atacam o jogador, apenas correm ao redor do jogador sem sentido, entao diminui a chance por enquanto.
		final int combinedCollision = collision + mostHate.getTemplate().getCollisionRadius();
		if (!npc.isMovementDisabled() && (Rnd.get(100) <= 3))
		{
			for (Attackable nearby : World.getInstance().getVisibleObjects(npc, Attackable.class))
			{
				if (npc.isInsideRadius2D(nearby, collision) && (nearby != mostHate))
				{
					int newX = combinedCollision + Rnd.get(40);
					newX = Rnd.nextBoolean() ? mostHate.getX() + newX : mostHate.getX() - newX;
					int newY = combinedCollision + Rnd.get(40);
					newY = Rnd.nextBoolean() ? mostHate.getY() + newY : mostHate.getY() - newY;
					if (!npc.isInsideRadius2D(newX, newY, 0, collision))
					{
						final int newZ = npc.getZ() + 30;
						
						// Mobius: Verifica destino. Previne problemas de colisao com paredes e corrige monstros nao evitando obstaculos.
						moveTo(GeoData.getInstance().moveCheck(npc.getX(), npc.getY(), npc.getZ(), newX, newY, newZ, npc.getInstanceId()));
					}
					
					return;
				}
			}
		}
		
		// Calcula movimento de Arqueiro.
		if ((!npc.isMovementDisabled()) && (npc.getAiType() == AIType.ARCHER) && (Rnd.get(100) < 15))
		{
			final double distance = npc.calculateDistance2D(mostHate);
			if (distance <= (60 + combinedCollision))
			{
				int posX = npc.getX();
				int posY = npc.getY();
				final int posZ = npc.getZ() + 30;
				if (originalAttackTarget.getX() < posX)
				{
					posX += 300;
				}
				else
				{
					posX -= 300;
				}
				
				if (originalAttackTarget.getY() < posY)
				{
					posY += 300;
				}
				else
				{
					posY -= 300;
				}
				
				if (GeoData.getInstance().canMove(npc.getX(), npc.getY(), npc.getZ(), posX, posY, posZ, npc.getInstanceId()))
				{
					setIntention(Intention.MOVE_TO, new Location(posX, posY, posZ, 0));
				}
				
				return;
			}
		}
		
		// Reconsideracao de Alvo de BOSS/Minion de Raid
		if (npc.isRaid() || npc.isRaidMinion())
		{
			_chaosTime++;
			if (npc instanceof RaidBoss)
			{
				if (!npc.asMonster().hasMinions())
				{
					if ((_chaosTime > Config.RAID_CHAOS_TIME) && (Rnd.get(100) <= (100 - ((npc.getCurrentHp() * 100) / npc.getMaxHp()))))
					{
						aggroReconsider();
						_chaosTime = 0;
						return;
					}
				}
				else
				{
					if ((_chaosTime > Config.RAID_CHAOS_TIME) && (Rnd.get(100) <= (100 - ((npc.getCurrentHp() * 200) / npc.getMaxHp()))))
					{
						aggroReconsider();
						_chaosTime = 0;
						return;
					}
				}
			}
			else if (npc instanceof GrandBoss)
			{
				if (_chaosTime > Config.GRAND_CHAOS_TIME)
				{
					final double chaosRate = 100 - ((npc.getCurrentHp() * 300) / npc.getMaxHp());
					if (((chaosRate <= 10) && (Rnd.get(100) <= 10)) || ((chaosRate > 10) && (Rnd.get(100) <= chaosRate)))
					{
						aggroReconsider();
						_chaosTime = 0;
						return;
					}
				}
			}
			else
			{
				if ((_chaosTime > Config.MINION_CHAOS_TIME) && (Rnd.get(100) <= (100 - ((npc.getCurrentHp() * 200) / npc.getMaxHp()))))
				{
					aggroReconsider();
					_chaosTime = 0;
					return;
				}
			}
		}
		
		// Conjura skills.
		if (!npc.isMoving() || (npc.getAiType() == AIType.MAGE))
		{
			final List<Skill> generalSkills = template.getAISkills(AISkillScope.GENERAL);
			if (!generalSkills.isEmpty())
			{
				// Condicao de Cura
				final List<Skill> aiHealSkills = template.getAISkills(AISkillScope.HEAL);
				if (!aiHealSkills.isEmpty())
				{
					if (npc.isMinion())
					{
						final Creature leader = npc.getLeader();
						if ((leader != null) && !leader.isDead() && (Rnd.get(100) > ((leader.getCurrentHp() / leader.getMaxHp()) * 100)))
						{
							for (Skill healSkill : aiHealSkills)
							{
								if ((healSkill.getTargetType() == TargetType.SELF) || !checkSkillCastConditions(npc, healSkill))
								{
									continue;
								}
								
								if (!LocationUtil.checkIfInRange((healSkill.getCastRange() + collision + leader.getTemplate().getCollisionRadius()), npc, leader, false) && !isParty(healSkill) && !npc.isMovementDisabled())
								{
									moveToPawn(leader, healSkill.getCastRange() + collision + leader.getTemplate().getCollisionRadius());
									return;
								}
								
								if (GeoData.getInstance().canSeeTarget(npc, leader))
								{
									clientStopMoving(null);
									
									final WorldObject target = npc.getTarget();
									npc.setTarget(leader);
									npc.doCast(healSkill);
									npc.setTarget(target);
									// LOGGER.debug(this + " used heal skill " + healSkill + " on leader " + leader);
									return;
								}
							}
						}
					}
					
					double percentage = (npc.getCurrentHp() / npc.getMaxHp()) * 100;
					if (Rnd.get(100) < ((100 - percentage) / 3))
					{
						for (Skill sk : aiHealSkills)
						{
							if (!checkSkillCastConditions(npc, sk))
							{
								continue;
							}
							
							clientStopMoving(null);
							
							final WorldObject target = npc.getTarget();
							npc.setTarget(npc);
							npc.doCast(sk);
							npc.setTarget(target);
							// LOGGER.debug(this + " used heal skill " + sk + " on itself");
							
							return;
						}
					}
					
					for (Skill sk : aiHealSkills)
					{
						if (!checkSkillCastConditions(npc, sk))
						{
							continue;
						}
						
						if (sk.getTargetType() == TargetType.ONE)
						{
							for (Attackable obj : World.getInstance().getVisibleObjectsInRange(npc, Attackable.class, sk.getCastRange() + collision))
							{
								if (!obj.isDead() || !obj.isInMyClan(npc))
								{
									continue;
								}
								
								percentage = (obj.getCurrentHp() / obj.getMaxHp()) * 100;
								if ((Rnd.get(100) < ((100 - percentage) / 10)) && GeoData.getInstance().canSeeTarget(npc, obj))
								{
									clientStopMoving(null);
									
									final WorldObject target = npc.getTarget();
									npc.setTarget(obj);
									npc.doCast(sk);
									npc.setTarget(target);
									// LOGGER.debug(this + " used heal skill " + sk + " on " + obj);
									return;
								}
							}
						}
						
						if (isParty(sk))
						{
							clientStopMoving(null);
							npc.doCast(sk);
							return;
						}
					}
				}
				
				// Condicao de Skill de Ressurreicao
				final List<Skill> aiResSkills = template.getAISkills(AISkillScope.RES);
				if (!aiResSkills.isEmpty())
				{
					if (npc.isMinion())
					{
						final Creature leader = npc.getLeader();
						if ((leader != null) && leader.isDead())
						{
							for (Skill sk : aiResSkills)
							{
								if ((sk.getTargetType() == TargetType.SELF) || !checkSkillCastConditions(npc, sk))
								{
									continue;
								}
								
								if (!LocationUtil.checkIfInRange((sk.getCastRange() + collision + leader.getTemplate().getCollisionRadius()), npc, leader, false) && !isParty(sk) && !npc.isMovementDisabled())
								{
									moveToPawn(leader, sk.getCastRange() + collision + leader.getTemplate().getCollisionRadius());
									return;
								}
								
								if (GeoData.getInstance().canSeeTarget(npc, leader))
								{
									clientStopMoving(null);
									
									final WorldObject target = npc.getTarget();
									npc.setTarget(leader);
									npc.doCast(sk);
									npc.setTarget(target);
									// LOGGER.debug(this + " used resurrection skill " + sk + " on leader " + leader);
									return;
								}
							}
						}
					}
					
					for (Skill sk : aiResSkills)
					{
						if (!checkSkillCastConditions(npc, sk))
						{
							continue;
						}
						
						if (sk.getTargetType() == TargetType.ONE)
						{
							for (Attackable obj : World.getInstance().getVisibleObjectsInRange(npc, Attackable.class, sk.getCastRange() + collision))
							{
								if (!obj.isDead() || !npc.isInMyClan(obj))
								{
									continue;
								}
								
								if ((Rnd.get(100) < 10) && GeoData.getInstance().canSeeTarget(npc, obj))
								{
									clientStopMoving(null);
									
									final WorldObject target = npc.getTarget();
									npc.setTarget(obj);
									npc.doCast(sk);
									npc.setTarget(target);
									// LOGGER.debug(this + " used heal skill " + sk + " on clan member " + obj);
									return;
								}
							}
						}
						
						if (isParty(sk))
						{
							clientStopMoving(null);
							
							final WorldObject target = npc.getTarget();
							npc.setTarget(npc);
							npc.doCast(sk);
							npc.setTarget(target);
							// LOGGER.debug(this + " used heal skill " + sk + " on party");
							return;
						}
					}
				}
			}
			
			// Uso de skill de Longo/Curto Alcance.
			final WorldObject target = npc.getTarget();
			if (target != null)
			{
				final List<Skill> shortRangeSkills = npc.getShortRangeSkills();
				if (!shortRangeSkills.isEmpty() && npc.hasSkillChance() && (npc.calculateDistance2D(target) <= 150))
				{
					final Skill shortRangeSkill = shortRangeSkills.get(Rnd.get(shortRangeSkills.size()));
					final int castRange = shortRangeSkill.getCastRange();
					if (((castRange < 1) || (npc.calculateDistance3D(target) < castRange)) && checkSkillCastConditions(npc, shortRangeSkill))
					{
						clientStopMoving(null);
						npc.setTarget(target);
						npc.doCast(shortRangeSkill);
						// LOGGER.debug(this + " used short range skill " + shortRangeSkill + " on " + npc.getTarget());
						return;
					}
				}
				
				final List<Skill> longRangeSkills = npc.getLongRangeSkills();
				if (!longRangeSkills.isEmpty() && npc.hasSkillChance())
				{
					final Skill longRangeSkill = longRangeSkills.get(Rnd.get(longRangeSkills.size()));
					final int castRange = longRangeSkill.getCastRange();
					if (((castRange < 1) || (npc.calculateDistance3D(target) < castRange)) && checkSkillCastConditions(npc, longRangeSkill))
					{
						clientStopMoving(null);
						npc.setTarget(target);
						npc.doCast(longRangeSkill);
						// LOGGER.debug(this + " used long range skill " + longRangeSkill + " on " + npc.getTarget());
						return;
					}
				}
			}
		}
		
		final double dist = npc.calculateDistance2D(mostHate);
		final int dist2 = (int) dist - collision;
		int range = npc.getPhysicalAttackRange() + combinedCollision;
		if (npc.getAiType() == AIType.ARCHER)
		{
			range = 850 + combinedCollision; // Base bow range for NPCs.
		}
		
		if (mostHate.isMoving())
		{
			range += 50;
			if (npc.isMoving())
			{
				range += 50;
			}
		}
		
		// Inicia ataque corpo a corpo
		if ((dist2 > range) || !GeoData.getInstance().canSeeTarget(npc, mostHate))
		{
			if (npc.isMovementDisabled())
			{
				targetReconsider();
			}
			else
			{
				final Creature target = getAttackTarget();
				if (target != null)
				{
					if (target.isMoving())
					{
						range -= 100;
					}
					
					moveToPawn(target, Math.max(range, 5));
				}
			}
			
			return;
		}
		
		// Ataca alvo
		_actor.doAttack(getAttackTarget());
	}
	
	private boolean cast(Skill sk)
	{
		if (sk == null)
		{
			return false;
		}
		
		final Attackable caster = getActiveChar();
		if (!checkSkillCastConditions(caster, sk))
		{
			return false;
		}
		
		if ((getAttackTarget() == null) && (caster.getMostHated() != null))
		{
			setAttackTarget(caster.getMostHated());
		}
		
		final Creature attackTarget = getAttackTarget();
		if (attackTarget == null)
		{
			return false;
		}
		
		final double dist = caster.calculateDistance2D(attackTarget);
		double dist2 = dist - attackTarget.getTemplate().getCollisionRadius();
		final double srange = sk.getCastRange() + caster.getTemplate().getCollisionRadius();
		if (attackTarget.isMoving())
		{
			dist2 -= 30;
		}
		
		if (sk.isContinuous())
		{
			if (!sk.isDebuff())
			{
				if (!caster.isAffectedBySkill(sk.getId()))
				{
					clientStopMoving(null);
					caster.setTarget(caster);
					caster.doCast(sk);
					_actor.setTarget(attackTarget);
					return true;
				}
				
				// Se o ator ja tiver buff, comeca a procurar outros mobs da mesma facao para conjurar
				if (sk.getTargetType() == TargetType.SELF)
				{
					return false;
				}
				
				if (sk.getTargetType() == TargetType.ONE)
				{
					final Creature target = effectTargetReconsider(sk, true);
					if (target != null)
					{
						clientStopMoving(null);
						caster.setTarget(target);
						caster.doCast(sk);
						caster.setTarget(attackTarget);
						return true;
					}
				}
				
				if (canParty(sk))
				{
					clientStopMoving(null);
					caster.setTarget(caster);
					caster.doCast(sk);
					caster.setTarget(attackTarget);
					return true;
				}
			}
			else
			{
				if (GeoData.getInstance().canSeeTarget(caster, attackTarget) && !canAOE(sk) && !attackTarget.isDead() && (dist2 <= srange))
				{
					if (!attackTarget.isAffectedBySkill(sk.getId()))
					{
						clientStopMoving(null);
						caster.doCast(sk);
						return true;
					}
				}
				else if (canAOE(sk))
				{
					if ((sk.getTargetType() == TargetType.AURA) || (sk.getTargetType() == TargetType.BEHIND_AURA) || (sk.getTargetType() == TargetType.FRONT_AURA) || (sk.getTargetType() == TargetType.AURA_CORPSE_MOB))
					{
						clientStopMoving(null);
						caster.doCast(sk);
						return true;
					}
					
					if (((sk.getTargetType() == TargetType.AREA) || (sk.getTargetType() == TargetType.BEHIND_AREA) || (sk.getTargetType() == TargetType.FRONT_AREA)) && GeoData.getInstance().canSeeTarget(caster, attackTarget) && !attackTarget.isDead() && (dist2 <= srange))
					{
						clientStopMoving(null);
						caster.doCast(sk);
						return true;
					}
				}
				else if (sk.getTargetType() == TargetType.ONE)
				{
					final Creature target = effectTargetReconsider(sk, false);
					if (target != null)
					{
						clientStopMoving(null);
						caster.doCast(sk);
						return true;
					}
				}
			}
		}
		
		if (sk.hasEffectType(EffectType.DISPEL, EffectType.DISPEL_BY_SLOT))
		{
			if (sk.getTargetType() == TargetType.ONE)
			{
				if ((attackTarget.getEffectList().getFirstEffect(EffectType.BUFF) != null) && GeoData.getInstance().canSeeTarget(caster, attackTarget) && !attackTarget.isDead() && (dist2 <= srange))
				{
					clientStopMoving(null);
					caster.doCast(sk);
					return true;
				}
				
				final Creature target = effectTargetReconsider(sk, false);
				if (target != null)
				{
					clientStopMoving(null);
					caster.setTarget(target);
					caster.doCast(sk);
					caster.setTarget(attackTarget);
					return true;
				}
			}
			else if (canAOE(sk))
			{
				if (((sk.getTargetType() == TargetType.AURA) || (sk.getTargetType() == TargetType.BEHIND_AURA) || (sk.getTargetType() == TargetType.FRONT_AURA)) && GeoData.getInstance().canSeeTarget(caster, attackTarget))
				{
					clientStopMoving(null);
					caster.doCast(sk);
					return true;
				}
				else if (((sk.getTargetType() == TargetType.AREA) || (sk.getTargetType() == TargetType.BEHIND_AREA) || (sk.getTargetType() == TargetType.FRONT_AREA)) && GeoData.getInstance().canSeeTarget(caster, attackTarget) && !attackTarget.isDead() && (dist2 <= srange))
				{
					clientStopMoving(null);
					caster.doCast(sk);
					return true;
				}
			}
		}
		
		if (sk.hasEffectType(EffectType.HEAL))
		{
			if (caster.isMinion() && (sk.getTargetType() != TargetType.SELF))
			{
				final Creature leader = caster.getLeader();
				if ((leader != null) && !leader.isDead() && (Rnd.get(100) > ((leader.getCurrentHp() / leader.getMaxHp()) * 100)))
				{
					if (!LocationUtil.checkIfInRange((sk.getCastRange() + caster.getTemplate().getCollisionRadius() + leader.getTemplate().getCollisionRadius()), caster, leader, false) && !isParty(sk) && !caster.isMovementDisabled())
					{
						moveToPawn(leader, sk.getCastRange() + caster.getTemplate().getCollisionRadius() + leader.getTemplate().getCollisionRadius());
					}
					
					if (GeoData.getInstance().canSeeTarget(caster, leader))
					{
						clientStopMoving(null);
						caster.setTarget(leader);
						caster.doCast(sk);
						caster.setTarget(attackTarget);
						return true;
					}
				}
			}
			
			double percentage = (caster.getCurrentHp() / caster.getMaxHp()) * 100;
			if (Rnd.get(100) < ((100 - percentage) / 3))
			{
				clientStopMoving(null);
				caster.setTarget(caster);
				caster.doCast(sk);
				caster.setTarget(attackTarget);
				return true;
			}
			
			if (sk.getTargetType() == TargetType.ONE)
			{
				for (Attackable obj : World.getInstance().getVisibleObjectsInRange(caster, Attackable.class, sk.getCastRange() + caster.getTemplate().getCollisionRadius()))
				{
					if (obj.isDead() || !caster.isInMyClan(obj))
					{
						continue;
					}
					
					percentage = (obj.getCurrentHp() / obj.getMaxHp()) * 100;
					if ((Rnd.get(100) < ((100 - percentage) / 10)) && GeoData.getInstance().canSeeTarget(caster, obj))
					{
						clientStopMoving(null);
						caster.setTarget(obj);
						caster.doCast(sk);
						caster.setTarget(attackTarget);
						return true;
					}
				}
			}
			if (isParty(sk))
			{
				for (Attackable obj : World.getInstance().getVisibleObjectsInRange(caster, Attackable.class, sk.getAffectRange() + caster.getTemplate().getCollisionRadius()))
				{
					if (obj.isInMyClan(caster) && (obj.getCurrentHp() < obj.getMaxHp()) && (Rnd.get(100) <= 20))
					{
						clientStopMoving(null);
						caster.setTarget(caster);
						caster.doCast(sk);
						caster.setTarget(attackTarget);
						return true;
					}
				}
			}
		}
		
		if (sk.hasEffectType(EffectType.PHYSICAL_ATTACK, EffectType.PHYSICAL_ATTACK_HP_LINK, EffectType.MAGICAL_ATTACK, EffectType.DEATH_LINK, EffectType.HP_DRAIN))
		{
			if (!canAura(sk))
			{
				if (GeoData.getInstance().canSeeTarget(caster, attackTarget) && !attackTarget.isDead() && (dist2 <= srange))
				{
					clientStopMoving(null);
					caster.doCast(sk);
					return true;
				}
				
				final Creature target = skillTargetReconsider(sk);
				if (target != null)
				{
					clientStopMoving(null);
					caster.setTarget(target);
					caster.doCast(sk);
					caster.setTarget(attackTarget);
					return true;
				}
			}
			else
			{
				clientStopMoving(null);
				caster.doCast(sk);
				return true;
			}
		}
		
		if (sk.hasEffectType(EffectType.SLEEP))
		{
			if (sk.getTargetType() == TargetType.ONE)
			{
				final double range = caster.getPhysicalAttackRange() + caster.getTemplate().getCollisionRadius() + attackTarget.getTemplate().getCollisionRadius();
				if (!attackTarget.isDead() && (dist2 <= srange) && ((dist2 > range) || attackTarget.isMoving()) && !attackTarget.isAffectedBySkill(sk.getId()))
				{
					clientStopMoving(null);
					caster.doCast(sk);
					return true;
				}
				
				final Creature target = effectTargetReconsider(sk, false);
				if (target != null)
				{
					clientStopMoving(null);
					caster.doCast(sk);
					return true;
				}
			}
			else if (canAOE(sk))
			{
				if ((sk.getTargetType() == TargetType.AURA) || (sk.getTargetType() == TargetType.BEHIND_AURA) || (sk.getTargetType() == TargetType.FRONT_AURA))
				{
					clientStopMoving(null);
					caster.doCast(sk);
					return true;
				}
				
				if (((sk.getTargetType() == TargetType.AREA) || (sk.getTargetType() == TargetType.BEHIND_AREA) || (sk.getTargetType() == TargetType.FRONT_AREA)) && GeoData.getInstance().canSeeTarget(caster, attackTarget) && !attackTarget.isDead() && (dist2 <= srange))
				{
					clientStopMoving(null);
					caster.doCast(sk);
					return true;
				}
			}
		}
		
		if (sk.hasEffectType(EffectType.STUN, EffectType.ROOT, EffectType.PARALYZE, EffectType.MUTE, EffectType.FEAR))
		{
			if (GeoData.getInstance().canSeeTarget(caster, attackTarget) && !canAOE(sk) && (dist2 <= srange))
			{
				if (!attackTarget.isAffectedBySkill(sk.getId()))
				{
					clientStopMoving(null);
					caster.doCast(sk);
					return true;
				}
			}
			else if (canAOE(sk))
			{
				if ((sk.getTargetType() == TargetType.AURA) || (sk.getTargetType() == TargetType.BEHIND_AURA) || (sk.getTargetType() == TargetType.FRONT_AURA))
				{
					clientStopMoving(null);
					caster.doCast(sk);
					return true;
				}
				
				if (((sk.getTargetType() == TargetType.AREA) || (sk.getTargetType() == TargetType.BEHIND_AREA) || (sk.getTargetType() == TargetType.FRONT_AREA)) && GeoData.getInstance().canSeeTarget(caster, attackTarget) && !attackTarget.isDead() && (dist2 <= srange))
				{
					clientStopMoving(null);
					caster.doCast(sk);
					return true;
				}
			}
			else if (sk.getTargetType() == TargetType.ONE)
			{
				final Creature target = effectTargetReconsider(sk, false);
				if (target != null)
				{
					clientStopMoving(null);
					caster.doCast(sk);
					return true;
				}
			}
		}
		
		if (sk.hasEffectType(EffectType.DMG_OVER_TIME, EffectType.DMG_OVER_TIME_PERCENT))
		{
			if (GeoData.getInstance().canSeeTarget(caster, attackTarget) && !canAOE(sk) && !attackTarget.isDead() && (dist2 <= srange))
			{
				if (!attackTarget.isAffectedBySkill(sk.getId()))
				{
					clientStopMoving(null);
					caster.doCast(sk);
					return true;
				}
			}
			else if (canAOE(sk))
			{
				if ((sk.getTargetType() == TargetType.AURA) || (sk.getTargetType() == TargetType.BEHIND_AURA) || (sk.getTargetType() == TargetType.FRONT_AURA) || (sk.getTargetType() == TargetType.AURA_CORPSE_MOB))
				{
					clientStopMoving(null);
					caster.doCast(sk);
					return true;
				}
				
				if (((sk.getTargetType() == TargetType.AREA) || (sk.getTargetType() == TargetType.BEHIND_AREA) || (sk.getTargetType() == TargetType.FRONT_AREA)) && GeoData.getInstance().canSeeTarget(caster, attackTarget) && !attackTarget.isDead() && (dist2 <= srange))
				{
					clientStopMoving(null);
					caster.doCast(sk);
					return true;
				}
			}
			else if (sk.getTargetType() == TargetType.ONE)
			{
				final Creature target = effectTargetReconsider(sk, false);
				if (target != null)
				{
					clientStopMoving(null);
					caster.doCast(sk);
					return true;
				}
			}
		}
		
		if (sk.hasEffectType(EffectType.RESURRECTION))
		{
			if (!isParty(sk))
			{
				if (caster.isMinion() && (sk.getTargetType() != TargetType.SELF))
				{
					final Creature leader = caster.getLeader();
					if (leader != null)
					{
						if (leader.isDead() && !LocationUtil.checkIfInRange((sk.getCastRange() + caster.getTemplate().getCollisionRadius() + leader.getTemplate().getCollisionRadius()), caster, leader, false) && !isParty(sk) && !caster.isMovementDisabled())
						{
							moveToPawn(leader, sk.getCastRange() + caster.getTemplate().getCollisionRadius() + leader.getTemplate().getCollisionRadius());
						}
						
						if (GeoData.getInstance().canSeeTarget(caster, leader))
						{
							clientStopMoving(null);
							caster.setTarget(leader);
							caster.doCast(sk);
							caster.setTarget(attackTarget);
							return true;
						}
					}
				}
				
				for (Attackable obj : World.getInstance().getVisibleObjectsInRange(caster, Attackable.class, sk.getCastRange() + caster.getTemplate().getCollisionRadius()))
				{
					if (!obj.isDead() || !caster.isInMyClan(obj))
					{
						continue;
					}
					
					if ((Rnd.get(100) < 10) && GeoData.getInstance().canSeeTarget(caster, obj))
					{
						clientStopMoving(null);
						caster.setTarget(obj);
						caster.doCast(sk);
						caster.setTarget(attackTarget);
						return true;
					}
				}
			}
			else if (isParty(sk))
			{
				for (Npc obj : World.getInstance().getVisibleObjectsInRange(caster, Npc.class, sk.getAffectRange() + caster.getTemplate().getCollisionRadius()))
				{
					if (caster.isInMyClan(obj) && (obj.getCurrentHp() < obj.getMaxHp()) && (Rnd.get(100) <= 20))
					{
						clientStopMoving(null);
						caster.setTarget(caster);
						caster.doCast(sk);
						caster.setTarget(attackTarget);
						return true;
					}
				}
			}
		}
		
		if (!canAura(sk))
		{
			if (GeoData.getInstance().canSeeTarget(caster, attackTarget) && !attackTarget.isDead() && (dist2 <= srange))
			{
				clientStopMoving(null);
				caster.doCast(sk);
				return true;
			}
			
			final Creature target = skillTargetReconsider(sk);
			if (target != null)
			{
				clientStopMoving(null);
				caster.setTarget(target);
				caster.doCast(sk);
				caster.setTarget(attackTarget);
				return true;
			}
		}
		else
		{
			clientStopMoving(null);
			caster.doCast(sk);
			return true;
		}
		
		return false;
	}
	
	private void movementDisable()
	{
		final Creature target = getAttackTarget();
		if (target == null)
		{
			return;
		}
		
		final Attackable npc = getActiveChar();
		if (npc.getTarget() == null)
		{
			npc.setTarget(target);
		}
		
		final double dist = npc.calculateDistance2D(target);
		
		// TODO(Zoey76): Review this "magic changes".
		final int random = Rnd.get(100);
		if (!target.isImmobilized() && (random < 15) && tryCast(npc, target, AISkillScope.IMMOBILIZE, dist))
		{
			return;
		}
		
		if (((random < 20) && tryCast(npc, target, AISkillScope.COT, dist)) || ((random < 30) && tryCast(npc, target, AISkillScope.DEBUFF, dist)))
		{
			return;
		}
		
		if ((random < 40) && tryCast(npc, target, AISkillScope.NEGATIVE, dist))
		{
			return;
		}
		
		if ((npc.isMovementDisabled() || (npc.getAiType() == AIType.MAGE) || (npc.getAiType() == AIType.HEALER)) && tryCast(npc, target, AISkillScope.ATTACK, dist))
		{
			return;
		}
		
		if (tryCast(npc, target, AISkillScope.UNIVERSAL, dist))
		{
			return;
		}
		
		// Se nao puder conjurar, tenta atacar.
		final int range = npc.getPhysicalAttackRange() + npc.getTemplate().getCollisionRadius() + target.getTemplate().getCollisionRadius();
		if ((dist <= range) && GeoData.getInstance().canSeeTarget(npc, target))
		{
			_actor.doAttack(target);
			return;
		}
		
		// Se nao puder conjurar nem atacar, encontra um novo alvo.
		targetReconsider();
	}
	
	private boolean tryCast(Attackable npc, Creature target, AISkillScope aiSkillScope, double dist)
	{
		for (Skill sk : npc.getTemplate().getAISkills(aiSkillScope))
		{
			if (!checkSkillCastConditions(npc, sk) || (((sk.getCastRange() + target.getTemplate().getCollisionRadius()) <= dist) && !canAura(sk)) || !GeoData.getInstance().canSeeTarget(npc, target))
			{
				continue;
			}
			
			clientStopMoving(null);
			npc.doCast(sk);
			return true;
		}
		
		return false;
	}
	
	/**
	 * @param caster o conjurador
	 * @param skill a skill a verificar.
	 * @return {@code true} if the skill is available for casting {@code false} otherwise.
	 */
	private static boolean checkSkillCastConditions(Attackable caster, Skill skill)
	{
		// MP insuficiente.
		// Personagem esta no modo "skill desabilitada".
		if ((caster.isCastingNow() && !skill.isSimultaneousCast()) || (skill.getMpConsume() >= caster.getCurrentMp()) || caster.isSkillDisabled(skill))
		{
			return false;
		}
		
		// Se e uma skill estatica e skill magica e o personagem esta silenciado ou e uma skill fisica silenciada e o personagem esta fisicamente silenciado.
		if (!skill.isStatic() && ((skill.isMagic() && caster.isMuted()) || caster.isPhysicalMuted()))
		{
			return false;
		}
		
		return true;
	}
	
	private Creature effectTargetReconsider(Skill sk, boolean positive)
	{
		if (sk == null)
		{
			return null;
		}
		
		final Attackable actor = getActiveChar();
		if (!sk.hasEffectType(EffectType.DISPEL, EffectType.DISPEL_BY_SLOT))
		{
			if (!positive)
			{
				double dist = 0;
				double dist2 = 0;
				int range = 0;
				for (Creature obj : actor.getAttackByList())
				{
					if ((obj == null) || obj.isDead() || !GeoData.getInstance().canSeeTarget(actor, obj) || (obj == getAttackTarget()))
					{
						continue;
					}
					
					try
					{
						actor.setTarget(getAttackTarget());
						dist = actor.calculateDistance2D(obj);
						dist2 = dist - actor.getTemplate().getCollisionRadius();
						range = sk.getCastRange() + actor.getTemplate().getCollisionRadius() + obj.getTemplate().getCollisionRadius();
						if (obj.isMoving())
						{
							dist2 -= 70;
						}
					}
					catch (NullPointerException e)
					{
						continue;
					}
					
					if ((dist2 <= range) && !getAttackTarget().isAffectedBySkill(sk.getId()))
					{
						return obj;
					}
				}
				
				// ----------------------------------------------------------------------
				// Se houver Alvo proximo com aggro, comeca a ir atras de alvo aleatorio que e atacavel
				for (Creature obj : World.getInstance().getVisibleObjectsInRange(actor, Creature.class, range))
				{
					if (obj.isDead() || !GeoData.getInstance().canSeeTarget(actor, obj))
					{
						continue;
					}
					try
					{
						actor.setTarget(getAttackTarget());
						dist = actor.calculateDistance2D(obj);
						dist2 = dist;
						range = sk.getCastRange() + actor.getTemplate().getCollisionRadius() + obj.getTemplate().getCollisionRadius();
						if (obj.isMoving())
						{
							dist2 -= 70;
						}
					}
					catch (NullPointerException e)
					{
						continue;
					}
					
					if ((obj.isPlayer() || obj.isSummon()) && (dist2 <= range) && !getAttackTarget().isAffectedBySkill(sk.getId()))
					{
						return obj;
					}
				}
			}
			else if (positive)
			{
				double dist = 0;
				double dist2 = 0;
				int range = 0;
				for (Attackable targets : World.getInstance().getVisibleObjectsInRange(actor, Attackable.class, range))
				{
					if (targets.isDead() || !GeoData.getInstance().canSeeTarget(actor, targets) || targets.isInMyClan(actor))
					{
						continue;
					}
					
					try
					{
						actor.setTarget(getAttackTarget());
						dist = actor.calculateDistance2D(targets);
						dist2 = dist - actor.getTemplate().getCollisionRadius();
						range = sk.getCastRange() + actor.getTemplate().getCollisionRadius() + targets.getTemplate().getCollisionRadius();
						if (targets.isMoving())
						{
							dist2 -= 70;
						}
					}
					catch (NullPointerException e)
					{
						continue;
					}
					
					if ((dist2 <= range) && !targets.isAffectedBySkill(sk.getId()))
					{
						return targets;
					}
				}
			}
		}
		else
		{
			double dist = 0;
			double dist2 = 0;
			int range = sk.getCastRange() + actor.getTemplate().getCollisionRadius() + getAttackTarget().getTemplate().getCollisionRadius();
			for (Creature obj : World.getInstance().getVisibleObjectsInRange(actor, Creature.class, range))
			{
				if (obj.isDead() || !GeoData.getInstance().canSeeTarget(actor, obj))
				{
					continue;
				}
				
				try
				{
					actor.setTarget(getAttackTarget());
					dist = actor.calculateDistance2D(obj);
					dist2 = dist - actor.getTemplate().getCollisionRadius();
					range = sk.getCastRange() + actor.getTemplate().getCollisionRadius() + obj.getTemplate().getCollisionRadius();
					if (obj.isMoving())
					{
						dist2 -= 70;
					}
				}
				catch (NullPointerException e)
				{
					continue;
				}
				
				if ((obj.isPlayer() || obj.isSummon()) && (dist2 <= range) && (getAttackTarget().getEffectList().getFirstEffect(EffectType.BUFF) != null))
				{
					return obj;
				}
			}
		}
		
		return null;
	}
	
	private Creature skillTargetReconsider(Skill sk)
	{
		double dist = 0;
		double dist2 = 0;
		int range = 0;
		final Attackable actor = getActiveChar();
		if (actor.getHateList() != null)
		{
			for (Creature obj : actor.getHateList())
			{
				if ((obj == null) || !GeoData.getInstance().canSeeTarget(actor, obj) || obj.isDead())
				{
					continue;
				}
				
				try
				{
					actor.setTarget(getAttackTarget());
					dist = actor.calculateDistance2D(obj);
					dist2 = dist - actor.getTemplate().getCollisionRadius();
					range = sk.getCastRange() + actor.getTemplate().getCollisionRadius() + getAttackTarget().getTemplate().getCollisionRadius();
					// if(obj.isMoving())
					// dist2 = dist2 - 40;
				}
				catch (NullPointerException e)
				{
					continue;
				}
				
				if (dist2 <= range)
				{
					return obj;
				}
			}
		}
		
		if (!(actor instanceof Guard))
		{
			for (WorldObject target : World.getInstance().getVisibleObjects(actor, WorldObject.class))
			{
				try
				{
					actor.setTarget(getAttackTarget());
					dist = actor.calculateDistance2D(target);
					dist2 = dist;
					range = sk.getCastRange() + actor.getTemplate().getCollisionRadius() + getAttackTarget().getTemplate().getCollisionRadius();
					// if(obj.isMoving())
					// dist2 = dist2 - 40;
				}
				catch (NullPointerException e)
				{
					continue;
				}
				
				final Creature obj = target.isCreature() ? target.asCreature() : null;
				if ((obj == null) || !GeoData.getInstance().canSeeTarget(actor, obj) || (dist2 > range))
				{
					continue;
				}
				
				if (obj.isPlayer())
				{
					return obj;
				}
				
				if (obj.isAttackable() && actor.isChaos())
				{
					if (!obj.asAttackable().isInMyClan(actor))
					{
						return obj;
					}
					continue;
				}
				
				if (obj.isSummon())
				{
					return obj;
				}
			}
		}
		
		return null;
	}
	
	private void targetReconsider()
	{
		double dist = 0;
		double dist2 = 0;
		int range = 0;
		final Attackable actor = getActiveChar();
		final Creature mostHate = actor.getMostHated();
		if (actor.getHateList() != null)
		{
			for (Creature obj : actor.getHateList())
			{
				if ((obj == null) || !GeoData.getInstance().canSeeTarget(actor, obj) || obj.isDead() || (obj != mostHate) || (obj == actor))
				{
					continue;
				}
				
				try
				{
					dist = actor.calculateDistance2D(obj);
					dist2 = dist - actor.getTemplate().getCollisionRadius();
					range = actor.getPhysicalAttackRange() + actor.getTemplate().getCollisionRadius() + obj.getTemplate().getCollisionRadius();
					if (obj.isMoving())
					{
						dist2 -= 70;
					}
				}
				catch (NullPointerException e)
				{
					continue;
				}
				
				if (dist2 <= range)
				{
					actor.addDamageHate(obj, 0, mostHate != null ? actor.getHating(mostHate) : 2000);
					actor.setTarget(obj);
					setAttackTarget(obj);
					return;
				}
			}
		}
		if (!(actor instanceof Guard))
		{
			World.getInstance().forEachVisibleObject(actor, Creature.class, obj ->
			{
				if ((obj == null) || !GeoData.getInstance().canSeeTarget(actor, obj) || obj.isDead() || (obj != mostHate) || (obj == actor) || (obj == getAttackTarget()))
				{
					return;
				}
				
				if (obj.isPlayer())
				{
					actor.addDamageHate(obj, 0, mostHate != null ? actor.getHating(mostHate) : 2000);
					actor.setTarget(obj);
					setAttackTarget(obj);
				}
				else if (obj.isAttackable())
				{
					if (actor.isChaos())
					{
						if (obj.asAttackable().isInMyClan(actor))
						{
							return;
						}
						
						actor.addDamageHate(obj, 0, mostHate != null ? actor.getHating(mostHate) : 2000);
						actor.setTarget(obj);
						setAttackTarget(obj);
					}
				}
				else if (obj.isSummon())
				{
					actor.addDamageHate(obj, 0, mostHate != null ? actor.getHating(mostHate) : 2000);
					actor.setTarget(obj);
					setAttackTarget(obj);
				}
			});
		}
	}
	
	private void aggroReconsider()
	{
		final Attackable actor = getActiveChar();
		final Creature mostHate = actor.getMostHated();
		if (actor.getHateList() != null)
		{
			final int rand = Rnd.get(actor.getHateList().size());
			int count = 0;
			for (Creature obj : actor.getHateList())
			{
				if (count < rand)
				{
					count++;
					continue;
				}
				
				if ((obj == null) || !GeoData.getInstance().canSeeTarget(actor, obj) || obj.isDead() || (obj == getAttackTarget()) || (obj == actor))
				{
					continue;
				}
				
				try
				{
					actor.setTarget(getAttackTarget());
				}
				catch (NullPointerException e)
				{
					continue;
				}
				
				actor.addDamageHate(obj, 0, mostHate != null ? actor.getHating(mostHate) : 2000);
				actor.setTarget(obj);
				setAttackTarget(obj);
				return;
			}
		}
		
		if (!(actor instanceof Guard))
		{
			World.getInstance().forEachVisibleObject(actor, Creature.class, obj ->
			{
				if (!GeoData.getInstance().canSeeTarget(actor, obj) || obj.isDead() || (obj != mostHate) || (obj == actor))
				{
					return;
				}
				
				if (obj.isPlayer())
				{
					actor.addDamageHate(obj, 0, (mostHate != null) && !mostHate.isDead() ? actor.getHating(mostHate) : 2000);
					actor.setTarget(obj);
					setAttackTarget(obj);
				}
				else if (obj.isAttackable())
				{
					if (actor.isChaos())
					{
						if (obj.asAttackable().isInMyClan(actor))
						{
							return;
						}
						
						actor.addDamageHate(obj, 0, mostHate != null ? actor.getHating(mostHate) : 2000);
						actor.setTarget(obj);
						setAttackTarget(obj);
					}
				}
				else if (obj.isSummon())
				{
					actor.addDamageHate(obj, 0, mostHate != null ? actor.getHating(mostHate) : 2000);
					actor.setTarget(obj);
					setAttackTarget(obj);
				}
			});
		}
	}
	
	/**
	 * Gerencia acoes de pensamento de IA de um Attackable.
	 */
	@Override
	public void onActionThink()
	{
		// Verifica se uma acao de pensamento ja esta em progresso.
		if (_thinking)
		{
			return;
		}
		
		// Verifica se a regiao e seus vizinhos estao ativos.
		final WorldRegion region = _actor.getWorldRegion();
		// Verifica se o ator esta com todas as skills desabilitadas.
		if ((region == null) || !region.areNeighborsActive() || getActiveChar().isAllSkillsDisabled())
		{
			return;
		}
		
		// Inicia acao de pensamento
		_thinking = true;
		
		try
		{
			// Gerencia pensamentos de IA de um Attackable
			switch (getIntention())
			{
				case ACTIVE:
				{
					thinkActive();
					break;
				}
				case ATTACK:
				{
					thinkAttack();
					break;
				}
				case CAST:
				{
					thinkCast();
					break;
				}
			}
		}
		catch (Exception e)
		{
			// LOGGER.warning(getClass().getSimpleName() + ": " + getActor().getName() + " - onActionThink() failed!");
		}
		finally
		{
			// Para acao de pensamento
			_thinking = false;
		}
	}
	
	/**
	 * Executa acoes correspondentes a Action Attacked.<br>
	 * <br>
	 * <b><u>Acoes</u>:</b>
	 * <ul>
	 * <li>Inicia o ataque: Calcula o timeout de ataque, Define _globalAggro para 0, Adiciona o atacante a _aggroList do ator</li>
	 * <li>Define o tipo de movimento da Creature para correr e envia pacote Servidor->Cliente ChangeMoveType para todos os outros Players</li>
	 * <li>Define a Intencao para ATTACK</li>
	 * </ul>
	 * @param attacker A Creature que ataca o ator
	 */
	@Override
	protected void onActionAttacked(Creature attacker)
	{
		final Attackable me = getActiveChar();
		
		// Calcula o timeout de ataque
		_attackTimeout = MAX_ATTACK_TIMEOUT + GameTimeTaskManager.getInstance().getGameTicks();
		
		// Define _globalAggro para 0 para permitir ataque mesmo logo apos spawn
		if (_globalAggro < 0)
		{
			_globalAggro = 0;
		}
		
		// Adiciona o atacante a _aggroList do ator if not present.
		if (!me.isInAggroList(attacker))
		{
			me.addDamageHate(attacker, 0, 1);
		}
		
		// Define o tipo de movimento da Creature para correr e envia pacote Servidor->Cliente ChangeMoveType para todos os outros Players
		if (!me.isRunning())
		{
			me.setRunning();
		}
		
		// Define a Intencao para ATTACK
		if (getIntention() != Intention.ATTACK)
		{
			setIntention(Intention.ATTACK, attacker);
		}
		else if (me.getMostHated() != getAttackTarget())
		{
			setIntention(Intention.ATTACK, attacker);
		}
		
		if (me.isMonster())
		{
			Monster master = me.asMonster();
			if (master.hasMinions())
			{
				master.getMinionList().onAssist(me, attacker);
			}
			
			master = master.getLeader();
			if ((master != null) && master.hasMinions())
			{
				master.getMinionList().onAssist(me, attacker);
			}
		}
		
		super.onActionAttacked(attacker);
	}
	
	/**
	 * Executa acoes correspondentes a Action Aggression.<br>
	 * <br>
	 * <b><u>Acoes</u>:</b>
	 * <ul>
	 * <li>Adiciona o alvo a _aggroList do ator ou atualiza odio se ja presente</li>
	 * <li>Define a Intencao do ator para ATTACK (se o ator for GuardInstance verifica se nao esta muito longe de sua localizacao base)</li>
	 * </ul>
	 * @param target the Creature that attacks
	 * @param aggro O valor de odio a adicionar ao ator contra o alvo
	 */
	@Override
	protected void onActionAggression(Creature target, int aggro)
	{
		final Attackable me = getActiveChar();
		if (me.isDead() || (target == null))
		{
			return;
		}
		
		// Adiciona o alvo a _aggroList do ator ou atualiza odio se ja presente
		me.addDamageHate(target, 0, aggro);
		
		// Define a Intencao de IA do ator para ATTACK
		if (getIntention() != Intention.ATTACK)
		{
			// Define o tipo de movimento da Creature para correr e envia pacote Servidor->Cliente ChangeMoveType para todos os outros Players
			if (!me.isRunning())
			{
				me.setRunning();
			}
			
			setIntention(Intention.ATTACK, target);
		}
		
		if (me.isMonster())
		{
			Monster master = me.asMonster();
			if (master.hasMinions())
			{
				master.getMinionList().onAssist(me, target);
			}
			
			master = master.getLeader();
			if ((master != null) && master.hasMinions())
			{
				master.getMinionList().onAssist(me, target);
			}
		}
	}
	
	@Override
	protected void onIntentionActive()
	{
		// Cancela timeout de ataque
		_attackTimeout = Integer.MAX_VALUE;
		super.onIntentionActive();
	}
	
	public void setGlobalAggro(int value)
	{
		_globalAggro = value;
	}
	
	public Attackable getActiveChar()
	{
		return _actor.asAttackable();
	}
	
	public int getFearTime()
	{
		return _fearTime;
	}
	
	public void setFearTime(int fearTime)
	{
		_fearTime = fearTime;
	}
}
