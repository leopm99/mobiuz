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
package com.l2journey.gameserver.model.actor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

import com.l2journey.Config;
import com.l2journey.commons.threads.ThreadPool;
import com.l2journey.gameserver.ai.Action;
import com.l2journey.gameserver.ai.AttackableAI;
import com.l2journey.gameserver.ai.CreatureAI;
import com.l2journey.gameserver.ai.FortSiegeGuardAI;
import com.l2journey.gameserver.ai.Intention;
import com.l2journey.gameserver.ai.SiegeGuardAI;
import com.l2journey.gameserver.data.xml.ItemData;
import com.l2journey.gameserver.managers.CursedWeaponsManager;
import com.l2journey.gameserver.managers.EventDropManager;
import com.l2journey.gameserver.managers.PcCafePointsManager;
import com.l2journey.gameserver.managers.WalkingManager;
import com.l2journey.gameserver.model.AbsorberInfo;
import com.l2journey.gameserver.model.AggroInfo;
import com.l2journey.gameserver.model.DamageDoneInfo;
import com.l2journey.gameserver.model.Seed;
import com.l2journey.gameserver.model.WorldObject;
import com.l2journey.gameserver.model.actor.enums.creature.InstanceType;
import com.l2journey.gameserver.model.actor.enums.creature.Team;
import com.l2journey.gameserver.model.actor.enums.npc.DropType;
import com.l2journey.gameserver.model.actor.instance.GrandBoss;
import com.l2journey.gameserver.model.actor.instance.Monster;
import com.l2journey.gameserver.model.actor.status.AttackableStatus;
import com.l2journey.gameserver.model.actor.tasks.attackable.CommandChannelTimer;
import com.l2journey.gameserver.model.actor.templates.NpcTemplate;
import com.l2journey.gameserver.model.events.EventDispatcher;
import com.l2journey.gameserver.model.events.EventType;
import com.l2journey.gameserver.model.events.holders.actor.npc.attackable.OnAttackableAggroRangeEnter;
import com.l2journey.gameserver.model.events.holders.actor.npc.attackable.OnAttackableAttack;
import com.l2journey.gameserver.model.events.holders.actor.npc.attackable.OnAttackableKill;
import com.l2journey.gameserver.model.groups.CommandChannel;
import com.l2journey.gameserver.model.groups.Party;
import com.l2journey.gameserver.model.item.ItemTemplate;
import com.l2journey.gameserver.model.item.holders.ItemHolder;
import com.l2journey.gameserver.model.item.instance.Item;
import com.l2journey.gameserver.model.skill.Skill;
import com.l2journey.gameserver.model.stats.Stat;
import com.l2journey.gameserver.network.SystemMessageId;
import com.l2journey.gameserver.network.enums.ChatType;
import com.l2journey.gameserver.network.serverpackets.CreatureSay;
import com.l2journey.gameserver.network.serverpackets.SystemMessage;
import com.l2journey.gameserver.taskmanagers.DecayTaskManager;

public class Attackable extends Npc
{
	// Raide
	private boolean _isRaid = false;
	private boolean _isRaidMinion = false;
	//
	private boolean _champion = false;
	private final Map<Creature, AggroInfo> _aggroList = new ConcurrentHashMap<>();
	private boolean _canReturnToSpawnPoint = true;
	private boolean _seeThroughSilentMove = false;
	// Plantacao
	private boolean _seeded = false;
	private Seed _seed = null;
	private int _seederObjId = 0;
	private final AtomicReference<ItemHolder> _harvestItem = new AtomicReference<>();
	// Saque
	private int _spoilerObjectId;
	private final AtomicReference<Collection<ItemHolder>> _sweepItems = new AtomicReference<>();
	// Dano excessivo
	private boolean _overhit;
	private double _overhitDamage;
	private Creature _overhitAttacker;
	// Canal de comando
	private CommandChannel _firstCommandChannelAttacked = null;
	private CommandChannelTimer _commandChannelTimer = null;
	private long _commandChannelLastAttack = 0;
	// Cristal de alma
	private boolean _absorbed;
	private final Map<Integer, AbsorberInfo> _absorbersList = new ConcurrentHashMap<>();
	// Diversos
	private boolean _mustGiveExpSp;
	protected int _onKillDelay = 2500; // L2J usa 5000
	
	/**
	 * Cria um NPC atacavel.
	 * @param template o template do NPC atacavel
	 */
	public Attackable(NpcTemplate template)
	{
		super(template);
		setInstanceType(InstanceType.Attackable);
		setInvul(false);
		_mustGiveExpSp = true;
	}
	
	@Override
	public AttackableStatus getStatus()
	{
		return (AttackableStatus) super.getStatus();
	}
	
	@Override
	public void initCharStatus()
	{
		setStatus(new AttackableStatus(this));
	}
	
	@Override
	protected CreatureAI initAI()
	{
		return new AttackableAI(this);
	}
	
	public Map<Creature, AggroInfo> getAggroList()
	{
		return _aggroList;
	}
	
	public boolean canReturnToSpawnPoint()
	{
		return _canReturnToSpawnPoint;
	}
	
	public void setCanReturnToSpawnPoint(boolean value)
	{
		_canReturnToSpawnPoint = value;
	}
	
	public boolean canSeeThroughSilentMove()
	{
		return _seeThroughSilentMove;
	}
	
	public void setSeeThroughSilentMove(boolean value)
	{
		_seeThroughSilentMove = value;
	}
	
	/**
	 * Usa a habilidade se as verificacoes minimas forem aprovadas.
	 * @param skill a habilidade
	 */
	public void useMagic(Skill skill)
	{
		if ((skill == null) || isAlikeDead() || skill.isPassive() || isCastingNow() || isSkillDisabled(skill))
		{
			return;
		}
		
		if ((getCurrentMp() < (getStat().getMpConsume(skill) + getStat().getMpInitialConsume(skill))) || (getCurrentHp() <= skill.getHpConsume()))
		{
			return;
		}
		
		if (!skill.isStatic())
		{
			if (skill.isMagic())
			{
				if (isMuted())
				{
					return;
				}
			}
			else if (isPhysicalMuted())
			{
				return;
			}
		}
		
		final WorldObject target = skill.getFirstOfTargetList(this);
		if (target != null)
		{
			getAI().setIntention(Intention.CAST, skill, target);
		}
	}
	
	/**
	 * Reduz o HP atual do Atacavel.
	 * @param damage O valor de reducao de HP
	 * @param attacker A Criatura que ataca
	 */
	@Override
	public void reduceCurrentHp(double damage, Creature attacker, Skill skill)
	{
		reduceCurrentHp(damage, attacker, true, false, skill);
	}
	
	/**
	 * Reduz o HP atual do Atacavel, atualiza sua _aggroList e lanca a tarefa doDie se necessario.
	 * @param damage O valor de reducao de HP
	 * @param attacker A Criatura que ataca
	 * @param awake O estado de despertar (Se True: para de dormir)
	 * @param isDOT
	 * @param skill
	 */
	@Override
	public void reduceCurrentHp(double damage, Creature attacker, boolean awake, boolean isDOT, Skill skill)
	{
		if (_isRaid && !isMinion() && (attacker != null) && (attacker.getParty() != null) && attacker.getParty().isInCommandChannel() && attacker.getParty().getCommandChannel().meetRaidWarCondition(this))
		{
			if (_firstCommandChannelAttacked == null) // direito de saque nao definido
			{
				synchronized (this)
				{
					if (_firstCommandChannelAttacked == null)
					{
						_firstCommandChannelAttacked = attacker.getParty().getCommandChannel();
						if (_firstCommandChannelAttacked != null)
						{
							_commandChannelTimer = new CommandChannelTimer(this);
							_commandChannelLastAttack = System.currentTimeMillis();
							ThreadPool.schedule(_commandChannelTimer, 10000); // verifica ultimo ataque
							_firstCommandChannelAttacked.broadcastPacket(new CreatureSay(null, ChatType.PARTYROOM_ALL, "", "You have looting rights!")); // TODO: retail msg
						}
					}
				}
			}
			else if (attacker.getParty().getCommandChannel().equals(_firstCommandChannelAttacked)) // esta no mesmo canal
			{
				_commandChannelLastAttack = System.currentTimeMillis(); // atualiza tempo do ultimo ataque
			}
		}
		
		// Adiciona dano e odio ao AggroInfo do atacante na _aggroList do Atacavel
		if (attacker != null)
		{
			addDamage(attacker, (int) damage, skill);
		}
		
		// Se este Atacavel e um Monstro e gerou lacaios, chama seus lacaios para a batalha
		if (isMonster())
		{
			Monster master = asMonster();
			
			if (master.hasMinions())
			{
				master.getMinionList().onAssist(this, attacker);
			}
			
			master = master.getLeader();
			if ((master != null) && master.hasMinions())
			{
				master.getMinionList().onAssist(this, attacker);
			}
		}
		
		// Reduz o HP atual do Atacavel e lanca a tarefa doDie se necessario
		super.reduceCurrentHp(damage, attacker, awake, isDOT, skill);
	}
	
	public synchronized void setMustRewardExpSp(boolean value)
	{
		_mustGiveExpSp = value;
	}
	
	public synchronized boolean getMustRewardExpSP()
	{
		return _mustGiveExpSp;
	}
	
	/**
	 * Mata o Atacavel (o corpo desaparece apos 7 segundos), distribui recompensas (EXP, SP, Drops...) e notifica o Motor de Quests.<br>
	 * Acoes:<br>
	 * Distribui recompensas de Exp e SP ao jogador (incluindo dono do Summon) que atingiu o Atacavel e aos membros do grupo.<br>
	 * Notifica o Motor de Quests sobre a morte do Atacavel se necessario.<br>
	 * Mata o Npc (o corpo desaparece apos 7 segundos)<br>
	 * Cuidado: Este metodo NAO DA recompensas ao Pet.
	 * @param killer A Criatura que matou o Atacavel
	 */
	@Override
	public boolean doDie(Creature killer)
	{
		// Mata o Npc (o corpo desaparece apos 7 segundos)
		if (!super.doDie(killer))
		{
			return false;
		}
		
		// Notificacao atrasada.
		if (killer != null)
		{
			final Player player = killer.asPlayer();
			if ((player != null) && EventDispatcher.getInstance().hasListener(EventType.ON_ATTACKABLE_KILL, this))
			{
				EventDispatcher.getInstance().notifyEventAsyncDelayed(new OnAttackableKill(player, this, killer.isSummon()), this, _onKillDelay);
			}
		}
		
		// Notifica os lacaios se houver.
		if (isMonster())
		{
			final Monster mob = asMonster();
			final Monster leader = mob.getLeader();
			if ((leader != null) && leader.hasMinions())
			{
				final int respawnTime = Config.MINIONS_RESPAWN_TIME.containsKey(getId()) ? Config.MINIONS_RESPAWN_TIME.get(getId()) * 1000 : -1;
				leader.getMinionList().onMinionDie(mob, respawnTime);
			}
			
			if (mob.hasMinions())
			{
				mob.getMinionList().onMasterDie(false);
			}
		}
		
		try
		{
			final Player playerKiller = (killer != null) ? killer.asPlayer() : null;
			if ((playerKiller != null) && isRaid())
			{
				playerKiller.getCounters().onRaidKill();
			}
		}
		catch (Exception e)
		{
			/* silencioso */ }
			
		return true;
	}
	
	private static class PartyContainer
	{
		public Party party;
		public long damage;
		
		public PartyContainer(Party party, long damage)
		{
			this.party = party;
			this.damage = damage;
		}
	}
	
	/**
	 * Distribui recompensas de Exp e SP ao jogador (incluindo dono do Summon) que atingiu o Atacavel e aos membros do grupo.<br>
	 * Acoes:<br>
	 * Obtem o jogador dono do Servitor (se necessario) e o Grupo em andamento.<br>
	 * Calcula as recompensas de Experiencia e SP em funcao da diferenca de nivel.<br>
	 * Adiciona recompensas de Exp e SP ao jogador (incluindo penalidade do Summon) e aos membros do grupo na area conhecida do ultimo atacante.<br>
	 * Cuidado: Este metodo NAO DA recompensas ao Pet.
	 * @param lastAttacker A Criatura que matou o Atacavel
	 */
	@Override
	protected void calculateRewards(Creature lastAttacker)
	{
		try
		{
			if (_aggroList.isEmpty())
			{
				return;
			}
			
			// NOTA: Mapa thread-safe e usado porque durante a iteracao para verificar todas as condicoes, as vezes uma entrada deve ser removida.
			final Map<Player, DamageDoneInfo> rewards = new ConcurrentHashMap<>();
			
			Player maxDealer = null;
			long maxDamage = 0;
			long totalDamage = 0;
			
			// Enquanto Iterando sobre Este Mapa, Remover Objeto Nao e Permitido
			// Percorre a _aggroList do Atacavel
			for (AggroInfo info : _aggroList.values())
			{
				if (info == null)
				{
					continue;
				}
				
				// Obtem a Criatura correspondente a este atacante
				final Player attacker = info.getAttacker().asPlayer();
				if (attacker == null)
				{
					continue;
				}
				
				// Obtem danos causados por este atacante
				final long damage = info.getDamage();
				
				// Previne comportamento indesejado
				if (damage > 1)
				{
					// Verifica se o causador de dano nao esta muito longe deste (monstro morto)
					if (calculateDistance3D(attacker) > Config.ALT_PARTY_RANGE)
					{
						continue;
					}
					
					totalDamage += damage;
					
					// Calcula danos reais (Invocadores devem receber seu proprio dano mais o dano do invocado)
					final DamageDoneInfo reward = rewards.computeIfAbsent(attacker, DamageDoneInfo::new);
					reward.addDamage(damage);
					
					if (reward.getDamage() > maxDamage)
					{
						maxDealer = attacker;
						maxDamage = reward.getDamage();
					}
				}
			}
			
			final List<PartyContainer> damagingParties = new ArrayList<>();
			for (AggroInfo info : _aggroList.values())
			{
				final Creature attacker = info.getAttacker();
				if (attacker == null)
				{
					continue;
				}
				
				long totalMemberDamage = 0;
				final Party party = attacker.getParty();
				if (party == null)
				{
					continue;
				}
				
				Optional<PartyContainer> partyContainerStream = Optional.empty();
				for (int i = 0, damagingPartiesSize = damagingParties.size(); i < damagingPartiesSize; i++)
				{
					final PartyContainer p = damagingParties.get(i);
					if (p.party == party)
					{
						partyContainerStream = Optional.of(p);
						break;
					}
				}
				
				final PartyContainer container = partyContainerStream.orElse(new PartyContainer(party, 0L));
				final List<Player> members = party.getMembers();
				for (Player e : members)
				{
					final AggroInfo memberAggro = _aggroList.get(e);
					if (memberAggro == null)
					{
						continue;
					}
					
					if (memberAggro.getDamage() > 1)
					{
						totalMemberDamage += memberAggro.getDamage();
					}
				}
				
				container.damage = totalMemberDamage;
				
				if (!partyContainerStream.isPresent())
				{
					damagingParties.add(container);
				}
			}
			
			final PartyContainer mostDamageParty;
			damagingParties.sort(Comparator.comparingLong(c -> c.damage));
			mostDamageParty = !damagingParties.isEmpty() ? damagingParties.get(0) : null;
			
			// Gerencia drops de Base, Quests e Varredura do Atacavel
			if ((mostDamageParty != null) && (mostDamageParty.damage > maxDamage))
			{
				Player leader = mostDamageParty.party.getLeader();
				doItemDrop(leader);
				EventDropManager.getInstance().doEventDrop(leader, this);
			}
			else
			{
				doItemDrop((maxDealer != null) && maxDealer.isOnline() ? maxDealer : lastAttacker);
				EventDropManager.getInstance().doEventDrop(lastAttacker, this);
			}
			
			if (!getMustRewardExpSP())
			{
				return;
			}
			
			if (!rewards.isEmpty())
			{
				for (DamageDoneInfo reward : rewards.values())
				{
					if (reward == null)
					{
						continue;
					}
					
					// Atacante a ser recompensado
					final Player attacker = reward.getAttacker();
					
					// Quantidade total de dano causado
					final long damage = reward.getDamage();
					
					// Obtem grupo
					final Party attackerParty = attacker.getParty();
					
					// Penalidade aplicada ao XP do atacante
					// Se este atacante tiver servitor, obtem a Penalidade de Exp aplicada ao servitor.
					final float penalty = attacker.hasServitor() ? attacker.getSummon().asServitor().getExpMultiplier() : 1;
					
					// Se NAO ha grupo em andamento
					if (attackerParty == null)
					{
						// Calcula recompensas de Exp e SP
						if (isInSurroundingRegion(attacker))
						{
							// Calcula a diferenca de nivel entre este atacante (jogador ou dono do servitor) e o Atacavel
							// mob = 24, atk = 10, diff = -14 (xp completo)
							// mob = 24, atk = 28, diff = 4 (algum xp)
							// mob = 24, atk = 50, diff = 26 (sem xp)
							final double[] expSp = calculateExpAndSp(attacker.getLevel(), damage, totalDamage);
							double exp = expSp[0];
							double sp = expSp[1];
							
							if (Config.CHAMPION_ENABLE && _champion)
							{
								exp *= Config.CHAMPION_REWARDS_EXP_SP;
								sp *= Config.CHAMPION_REWARDS_EXP_SP;
							}
							
							exp *= penalty;
							
							// Verifica se houve um golpe com dano excessivo habilitado
							final Creature overhitAttacker = _overhitAttacker;
							if (_overhit && (overhitAttacker != null))
							{
								final Player player = overhitAttacker.asPlayer();
								if ((player != null) && (attacker == player))
								{
									attacker.sendPacket(SystemMessageId.OVER_HIT);
									exp += calculateOverhitExp(exp);
								}
							}
							
							// Distribui o Exp e SP entre o Jogador e seu Summon
							if (!attacker.isDead())
							{
								long addExp = Math.round(attacker.calcStat(Stat.EXPSP_RATE, exp, null, null));
								int addSp = (int) attacker.calcStat(Stat.EXPSP_RATE, sp, null, null);
								
								// Taxas premium
								if (attacker.hasPremiumStatus())
								{
									addExp *= Config.PREMIUM_RATE_XP;
									addSp *= Config.PREMIUM_RATE_SP;
								}
								
								attacker.addExpAndSp(addExp, addSp, useVitalityRate());
								if ((addExp > 0) && useVitalityRate())
								{
									attacker.updateVitalityPoints(getVitalityPoints(attacker.getLevel(), damage), true, false);
									PcCafePointsManager.getInstance().givePcCafePoint(attacker, exp);
								}
							}
						}
					}
					else
					{
						// compartilhar com membros do grupo
						long partyDmg = 0;
						double partyMul = 1;
						int partyLvl = 0;
						
						// Obtem todas as Criaturas que podem ser recompensadas no grupo
						final List<Player> rewardedMembers = new ArrayList<>();
						// Percorre todos os jogadores no grupo
						final List<Player> groupMembers = attackerParty.isInCommandChannel() ? attackerParty.getCommandChannel().getMembers() : attackerParty.getMembers();
						for (Player partyPlayer : groupMembers)
						{
							if ((partyPlayer == null) || partyPlayer.isDead())
							{
								continue;
							}
							
							// Obtem o RewardInfo deste jogador das recompensas do Atacavel
							final DamageDoneInfo reward2 = rewards.get(partyPlayer);
							
							// Se o jogador esta nas recompensas do Atacavel adiciona seus danos aos danos do grupo
							if (reward2 != null)
							{
								if (calculateDistance3D(partyPlayer) < Config.ALT_PARTY_RANGE)
								{
									partyDmg += reward2.getDamage(); // Add Player damages to party damages
									rewardedMembers.add(partyPlayer);
									
									if (partyPlayer.getLevel() > partyLvl)
									{
										if (attackerParty.isInCommandChannel())
										{
											partyLvl = attackerParty.getCommandChannel().getLevel();
										}
										else
										{
											partyLvl = partyPlayer.getLevel();
										}
									}
								}
								
								rewards.remove(partyPlayer); // Remove o jogador das recompensas do Atacavel
							}
							else if (calculateDistance3D(partyPlayer) < Config.ALT_PARTY_RANGE)
							{
								rewardedMembers.add(partyPlayer);
								if (partyPlayer.getLevel() > partyLvl)
								{
									if (attackerParty.isInCommandChannel())
									{
										partyLvl = attackerParty.getCommandChannel().getLevel();
									}
									else
									{
										partyLvl = partyPlayer.getLevel();
									}
								}
							}
						}
						
						// Se o grupo nao matou este Atacavel sozinho
						if (partyDmg < totalDamage)
						{
							partyMul = ((double) partyDmg / totalDamage);
						}
						
						// Calcula recompensas de Exp e SP
						final double[] expSp = calculateExpAndSp(partyLvl, partyDmg, totalDamage);
						double exp = expSp[0];
						double sp = expSp[1];
						
						if (Config.CHAMPION_ENABLE && _champion)
						{
							exp *= Config.CHAMPION_REWARDS_EXP_SP;
							sp *= Config.CHAMPION_REWARDS_EXP_SP;
						}
						
						exp *= partyMul;
						sp *= partyMul;
						
						// Verifica se houve um golpe com dano excessivo habilitado
						// (Quando em grupo, o bonus de exp por dano excessivo e dado ao grupo inteiro e dividido proporcionalmente entre os membros)
						final Creature overhitAttacker = _overhitAttacker;
						if (_overhit && (overhitAttacker != null))
						{
							final Player player = overhitAttacker.asPlayer();
							if ((player != null) && (attacker == player))
							{
								attacker.sendPacket(SystemMessageId.OVER_HIT);
								exp += calculateOverhitExp(exp);
							}
						}
						
						// Distribui recompensas de Experiencia e SP aos membros do grupo do jogador na area conhecida do ultimo atacante
						if (partyDmg > 0)
						{
							attackerParty.distributeXpAndSp(exp, sp, rewardedMembers, partyLvl, partyDmg, this);
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "", e);
		}
	}
	
	@Override
	public void addAttackerToAttackByList(Creature creature)
	{
		if ((creature == null) || (creature == this) || getAttackByList().contains(creature))
		{
			return;
		}
		
		getAttackByList().add(creature);
	}
	
	public Creature getMainDamageDealer()
	{
		if (_aggroList.isEmpty())
		{
			return null;
		}
		
		long damage = 0;
		Creature damageDealer = null;
		for (AggroInfo info : _aggroList.values())
		{
			if ((info != null) && (info.getDamage() > damage) && (calculateDistance3D(info.getAttacker()) < Config.ALT_PARTY_RANGE))
			{
				damage = info.getDamage();
				damageDealer = info.getAttacker();
			}
		}
		
		return damageDealer;
	}
	
	/**
	 * Adiciona dano e odio ao AggroInfo do atacante na _aggroList do Atacavel.
	 * @param attacker A Criatura que causou danos a este Atacavel
	 * @param damage O numero de danos causados pela Criatura atacante
	 * @param skill
	 */
	public void addDamage(Creature attacker, int damage, Skill skill)
	{
		if (attacker == null)
		{
			return;
		}
		
		// Notifica a IA do Atacavel com ATTACKED
		if (!isDead())
		{
			try
			{
				// Se o monstro esta andando - para-o
				if (isWalker() && !isCoreAIDisabled() && WalkingManager.getInstance().isOnWalk(this))
				{
					WalkingManager.getInstance().stopMoving(this, false, true);
				}
				
				getAI().notifyAction(Action.ATTACKED, attacker);
				
				// Calcula a quantidade de odio que este atacavel recebe deste ataque.
				final long hateValue = ((long) damage * 100) / (getLevel() + 7);
				addDamageHate(attacker, damage, (int) hateValue);
				
				final Player player = attacker.asPlayer();
				if ((player != null) && EventDispatcher.getInstance().hasListener(EventType.ON_ATTACKABLE_ATTACK, this))
				{
					EventDispatcher.getInstance().notifyEventAsync(new OnAttackableAttack(player, this, damage, skill, attacker.isSummon()), this);
				}
			}
			catch (Exception e)
			{
				LOGGER.log(Level.SEVERE, "", e);
			}
		}
	}
	
	/**
	 * Adiciona dano e odio a lista de agressao do atacante para este personagem.
	 * @param attacker A Criatura que causou danos a este Atacavel
	 * @param damage O numero de danos causados pela Criatura atacante
	 * @param aggroValue O odio (=dano) causado pela Criatura atacante
	 */
	public void addDamageHate(Creature attacker, long damage, long aggroValue)
	{
		if ((attacker == null) || (attacker == this))
		{
			return;
		}
		
		// Obtem o AggroInfo da Criatura atacante da _aggroList do Atacavel
		final AggroInfo ai = _aggroList.computeIfAbsent(attacker, AggroInfo::new);
		ai.addDamage(damage);
		
		// Armadilhas nao causam agressao
		// fazendo esse hack porque nao e possivel determinar se o dano foi feito por armadilha
		// entao apenas verifica por armadilha ativada aqui
		long aggro = aggroValue;
		final Player targetPlayer = attacker.asPlayer();
		if ((targetPlayer == null) || (targetPlayer.getTrap() == null) || !targetPlayer.getTrap().isTriggered())
		{
			ai.addHate(aggro);
		}
		
		if ((targetPlayer != null) && (aggro == 0))
		{
			addDamageHate(attacker, 0, 1);
			
			// Define a intencao do Atacavel para ACTIVE
			if (getAI().getIntention() == Intention.IDLE)
			{
				getAI().setIntention(Intention.ACTIVE);
			}
			
			// Notifica os scripts
			if (EventDispatcher.getInstance().hasListener(EventType.ON_ATTACKABLE_AGGRO_RANGE_ENTER, this))
			{
				EventDispatcher.getInstance().notifyEventAsync(new OnAttackableAggroRangeEnter(this, targetPlayer, attacker.isSummon()), this);
			}
		}
		else if ((targetPlayer == null) && (aggro == 0))
		{
			aggro = 1;
			ai.addHate(1);
		}
		
		// Define a intencao do Atacavel para ACTIVE
		if ((aggro != 0) && (getAI().getIntention() == Intention.IDLE))
		{
			getAI().setIntention(Intention.ACTIVE);
		}
	}
	
	public void reduceHate(Creature target, long amount)
	{
		if ((getAI() instanceof SiegeGuardAI) || (getAI() instanceof FortSiegeGuardAI))
		{
			// TODO: isso apenas previne erro ate que os guardas de cerco sejam tratados corretamente
			stopHating(target);
			setTarget(null);
			getAI().setIntention(Intention.IDLE);
			return;
		}
		
		if (target == null) // lista de agressao inteira
		{
			final Creature mostHated = getMostHated();
			if (mostHated == null) // torna o alvo passivo por mais um momento
			{
				((AttackableAI) getAI()).setGlobalAggro(-25);
				return;
			}
			
			for (AggroInfo ai : _aggroList.values())
			{
				ai.addHate(amount);
			}
			
			if (getHating(mostHated) >= 0)
			{
				((AttackableAI) getAI()).setGlobalAggro(-25);
				clearAggroList();
				getAI().setIntention(Intention.ACTIVE);
				setWalking();
			}
			
			return;
		}
		
		final AggroInfo ai = _aggroList.get(target);
		if (ai == null)
		{
			return;
		}
		
		ai.addHate(amount);
		if ((ai.getHate() >= 0) && (getMostHated() == null))
		{
			((AttackableAI) getAI()).setGlobalAggro(-25);
			clearAggroList();
			getAI().setIntention(Intention.ACTIVE);
			setWalking();
		}
	}
	
	/**
	 * Limpa o odio da _aggroList da Criatura sem remover da lista.
	 * @param target
	 */
	public void stopHating(Creature target)
	{
		if (target == null)
		{
			return;
		}
		
		final AggroInfo ai = _aggroList.get(target);
		if (ai != null)
		{
			ai.stopHate();
		}
	}
	
	/**
	 * @return a Criatura mais odiada da _aggroList do Atacavel.
	 */
	public Creature getMostHated()
	{
		if (_aggroList.isEmpty() || isAlikeDead())
		{
			return null;
		}
		
		Creature mostHated = null;
		long maxHate = 0;
		
		// Enquanto Interagindo sobre Este Mapa, Remover Objeto Nao e Permitido
		// Percorre a aggroList do Atacavel
		for (AggroInfo ai : _aggroList.values())
		{
			if (ai == null)
			{
				continue;
			}
			
			if (ai.checkHate(this) > maxHate)
			{
				mostHated = ai.getAttacker();
				maxHate = ai.getHate();
			}
		}
		
		return mostHated;
	}
	
	/**
	 * @return as 2 Criaturas mais odiadas da _aggroList do Atacavel.
	 */
	public List<Creature> get2MostHated()
	{
		if (_aggroList.isEmpty() || isAlikeDead())
		{
			return null;
		}
		
		Creature mostHated = null;
		Creature secondMostHated = null;
		long maxHate = 0;
		final List<Creature> result = new ArrayList<>();
		
		// Enquanto iterando sobre este mapa, remover objetos nao e permitido
		// Percorre a aggroList do Atacavel
		for (AggroInfo ai : _aggroList.values())
		{
			if (ai == null)
			{
				continue;
			}
			
			if (ai.checkHate(this) > maxHate)
			{
				secondMostHated = mostHated;
				mostHated = ai.getAttacker();
				maxHate = ai.getHate();
			}
		}
		
		result.add(mostHated);
		
		if (getAttackByList().contains(secondMostHated))
		{
			result.add(secondMostHated);
		}
		else
		{
			result.add(null);
		}
		
		return result;
	}
	
	public List<Creature> getHateList()
	{
		if (_aggroList.isEmpty() || isAlikeDead())
		{
			return null;
		}
		
		final List<Creature> result = new ArrayList<>();
		for (AggroInfo ai : _aggroList.values())
		{
			if (ai == null)
			{
				continue;
			}
			ai.checkHate(this);
			
			result.add(ai.getAttacker());
		}
		
		return result;
	}
	
	/**
	 * @param target A Criatura cujo nivel de odio deve ser retornado
	 * @return o nivel de odio do Atacavel contra esta Criatura contido na _aggroList.
	 */
	public long getHating(Creature target)
	{
		if (_aggroList.isEmpty() || (target == null))
		{
			return 0;
		}
		
		final AggroInfo ai = _aggroList.get(target);
		if (ai == null)
		{
			return 0;
		}
		
		if (ai.getAttacker().isPlayer())
		{
			final Player act = ai.getAttacker().asPlayer();
			if (act.isInvisible() || act.isInvul() || act.isSpawnProtected())
			{
				// Remover Objeto Deveria Usar Este Metodo e Pode Ser Bloqueado Enquanto Interage
				_aggroList.remove(target);
				return 0;
			}
		}
		
		if (!ai.getAttacker().isSpawned() || ai.getAttacker().isInvisible())
		{
			_aggroList.remove(target);
			return 0;
		}
		
		if (ai.getAttacker().isAlikeDead())
		{
			ai.stopHate();
			return 0;
		}
		
		return ai.getHate();
	}
	
	public void doItemDrop(Creature mainDamageDealer)
	{
		doItemDrop(getTemplate(), mainDamageDealer);
	}
	
	/**
	 * Gerencia drops de Base, Quests e Eventos Especiais do Atacavel (chamado por calculateRewards).<br>
	 * Conceito:<br>
	 * Durante um Evento Especial todos os Atacaveis podem dropar Itens extras.<br>
	 * Esses Itens extras sao definidos na tabela allNpcDateDrops do EventDroplist.<br>
	 * Cada Evento Especial tem uma data de inicio e fim para parar de dropar Itens extras automaticamente.<br>
	 * Acoes:<br>
	 * Gerencia drop de Eventos Especiais criados pelo GM por um periodo definido.<br>
	 * Obtem todos os drops possiveis deste Atacavel do NpcTemplate e adiciona drops de Quest.<br>
	 * Para cada drop possivel (base + quests), calcula qual deve ser dropado (aleatorio).<br>
	 * Obtem a quantidade de cada Item dropado (aleatorio).<br>
	 * Cria este ou estes Itens correspondentes a cada Identificador de Item dropado.<br>
	 * Se o modo autoLoot estiver ativo e se a Criatura que matou o Atacavel for um jogador, da o(s) item(ns) ao jogador que matou o Atacavel.<br>
	 * Se o modo autoLoot nao estiver ativo ou se a Criatura que matou o Atacavel nao for um jogador, adiciona este ou estes item(ns) no mundo como objeto visivel na posicao onde o mob estava por ultimo.
	 * @param npcTemplate
	 * @param mainDamageDealer
	 */
	public void doItemDrop(NpcTemplate npcTemplate, Creature mainDamageDealer)
	{
		if (mainDamageDealer == null)
		{
			return;
		}
		
		final Player player = mainDamageDealer.asPlayer();
		
		// Nao dropa nada se o ultimo atacante ou dono nao for jogador
		if (player == null)
		{
			return;
		}
		
		CursedWeaponsManager.getInstance().checkDrop(this, player);
		
		if (isSpoiled())
		{
			_sweepItems.set(npcTemplate.calculateDrops(DropType.SPOIL, this, player));
		}
		
		final Collection<ItemHolder> deathItems = npcTemplate.calculateDrops(DropType.DROP, this, player);
		if (deathItems != null)
		{
			for (ItemHolder drop : deathItems)
			{
				final ItemTemplate item = ItemData.getInstance().getTemplate(drop.getId());
				// Verifica se o modo autoLoot esta ativo
				if (Config.AUTO_LOOT_ITEM_IDS.contains(item.getId()) || isFlying() || (!item.hasExImmediateEffect() && ((!_isRaid && Config.AUTO_LOOT) || (_isRaid && Config.AUTO_LOOT_RAIDS))) || (item.hasExImmediateEffect() && Config.AUTO_LOOT_HERBS))
				{
					player.doAutoLoot(this, drop); // Da o(s) item(ns) ao jogador que matou o Atacavel
				}
				else
				{
					dropItem(player, drop); // dropa o item no chao
				}
				
				// Transmite mensagem se o RaidBoss foi derrotado
				if (_isRaid && !_isRaidMinion && (drop.getCount() > 0))
				{
					final SystemMessage sm = new SystemMessage(SystemMessageId.C1_DIED_AND_DROPPED_S3_S2);
					sm.addString(getName());
					sm.addItemName(item);
					sm.addLong(drop.getCount());
					broadcastPacket(sm);
				}
			}
			
			deathItems.clear();
		}
	}
	
	/**
	 * @return a arma ativa deste Atacavel (= nulo).
	 */
	public Item getActiveWeapon()
	{
		return null;
	}
	
	/**
	 * Verifica se a criatura esta na lista de agressao.
	 * @param creature a criatura
	 * @return {@code true} se a criatura estiver na lista de agressao, {@code false} caso contrario
	 */
	public boolean isInAggroList(Creature creature)
	{
		return (creature != null) && _aggroList.containsKey(creature);
	}
	
	/**
	 * Limpa a _aggroList do Atacavel.
	 */
	public void clearAggroList()
	{
		_aggroList.clear();
		
		// limpa valores de dano excessivo
		_overhit = false;
		_overhitDamage = 0;
		_overhitAttacker = null;
	}
	
	/**
	 * @return {@code true} se houver saque para varrer, {@code false} caso contrario.
	 */
	@Override
	public boolean isSweepActive()
	{
		return _sweepItems.get() != null;
	}
	
	/**
	 * @return uma copia de itens ficticios para o saque de varredura.
	 */
	public List<ItemTemplate> getSpoilLootItems()
	{
		final Collection<ItemHolder> sweepItems = _sweepItems.get();
		final List<ItemTemplate> lootItems = new LinkedList<>();
		if (sweepItems != null)
		{
			for (ItemHolder item : sweepItems)
			{
				lootItems.add(ItemData.getInstance().getTemplate(item.getId()));
			}
		}
		
		return lootItems;
	}
	
	/**
	 * @return tabela contendo todos os Itens que podem ser saqueados.
	 */
	public Collection<ItemHolder> takeSweep()
	{
		return _sweepItems.getAndSet(null);
	}
	
	/**
	 * @return tabela contendo todos os Itens que podem ser colhidos.
	 */
	public ItemHolder takeHarvest()
	{
		return _harvestItem.getAndSet(null);
	}
	
	/**
	 * Verifica se o corpo esta muito velho.
	 * @param attacker o jogador a validar
	 * @param remainingTime o tempo a verificar
	 * @param sendMessage se {@code true} enviara uma mensagem de corpo muito velho
	 * @return {@code true} se o corpo estiver muito velho
	 */
	public boolean isOldCorpse(Player attacker, int remainingTime, boolean sendMessage)
	{
		if (!isDead() || (DecayTaskManager.getInstance().getRemainingTime(this) >= remainingTime))
		{
			return false;
		}
		if (sendMessage && (attacker != null))
		{
			attacker.sendPacket(SystemMessageId.THE_CORPSE_IS_TOO_OLD_THE_SKILL_CANNOT_BE_USED);
		}
		
		return true;
	}
	
	/**
	 * @param sweeper o jogador a validar.
	 * @param sendMessage sendMessage se {@code true} enviara uma mensagem de varredura nao permitida.
	 * @return {@code true} se for o saqueador ou estiver no grupo do saqueador.
	 */
	public boolean checkSpoilOwner(Player sweeper, boolean sendMessage)
	{
		if ((sweeper.getObjectId() == _spoilerObjectId) || sweeper.isInLooterParty(_spoilerObjectId))
		{
			return true;
		}
		if (sendMessage)
		{
			sweeper.sendPacket(SystemMessageId.THERE_ARE_NO_PRIORITY_RIGHTS_ON_A_SWEEPER);
		}
		
		return false;
	}
	
	/**
	 * Define a flag de dano excessivo no Atacavel.
	 * @param status O status da flag de dano excessivo
	 */
	public void overhitEnabled(boolean status)
	{
		_overhit = status;
	}
	
	/**
	 * Define os valores de dano excessivo como o atacante que desferiu o golpe e a quantidade de dano causada pela habilidade.
	 * @param attacker A Criatura que atingiu o Atacavel usando a habilidade de dano excessivo habilitada
	 * @param damage A quantidade de dano causada pela habilidade de dano excessivo habilitada no Atacavel
	 */
	public void setOverhitValues(Creature attacker, double damage)
	{
		// Calcula o dano excessivo
		// Ex: mob tinha 10 HP restantes, habilidade de dano excessivo causou 50 de dano total, dano excessivo e 40
		final double overhitDmg = -(getCurrentHp() - damage);
		if (overhitDmg < 0)
		{
			// nao matamos o mob com o golpe de dano excessivo. (nao foi realmente um golpe de dano excessivo)
			// vamos apenas limpar todos os valores relacionados ao dano excessivo
			overhitEnabled(false);
			_overhitDamage = 0;
			_overhitAttacker = null;
			return;
		}
		
		overhitEnabled(true);
		_overhitDamage = overhitDmg;
		_overhitAttacker = attacker;
	}
	
	/**
	 * Retorna a Criatura que atingiu o Atacavel usando uma habilidade de dano excessivo habilitada.
	 * @return Criatura atacante
	 */
	public Creature getOverhitAttacker()
	{
		return _overhitAttacker;
	}
	
	/**
	 * Retorna a quantidade de dano causada no Atacavel usando uma habilidade de dano excessivo habilitada.
	 * @return double dano
	 */
	public double getOverhitDamage()
	{
		return _overhitDamage;
	}
	
	/**
	 * @return True se o Atacavel foi atingido por uma habilidade de dano excessivo habilitada.
	 */
	public boolean isOverhit()
	{
		return _overhit;
	}
	
	/**
	 * Ativa a condicao de alma absorvida no Atacavel.
	 */
	public void absorbSoul()
	{
		_absorbed = true;
	}
	
	/**
	 * @return True se o Atacavel teve sua alma absorvida.
	 */
	public boolean isAbsorbed()
	{
		return _absorbed;
	}
	
	/**
	 * Adiciona um atacante que absorveu com sucesso a alma deste Atacavel na _absorbersList.
	 * @param attacker
	 */
	public void addAbsorber(Player attacker)
	{
		// Se nao temos _absorbersList iniciada, faca-o
		final AbsorberInfo ai = _absorbersList.get(attacker.getObjectId());
		
		// Se a Criatura atacante ainda nao esta na _absorbersList deste Atacavel, adicione-a
		if (ai == null)
		{
			_absorbersList.put(attacker.getObjectId(), new AbsorberInfo(attacker.getObjectId(), getCurrentHp()));
		}
		else
		{
			ai.setAbsorbedHp(getCurrentHp());
		}
		
		// Define este Atacavel como absorvido
		absorbSoul();
	}
	
	public void resetAbsorbList()
	{
		_absorbed = false;
		_absorbersList.clear();
	}
	
	public Map<Integer, AbsorberInfo> getAbsorbersList()
	{
		return _absorbersList;
	}
	
	/**
	 * Calcula a Experiencia e SP a distribuir ao atacante (Jogador, Servitor ou Grupo) do Atacavel.
	 * @param charLevel O nivel do matador
	 * @param damage Os danos causados pelo atacante (Jogador, Servitor ou Grupo)
	 * @param totalDamage O dano total causado
	 * @return
	 */
	private double[] calculateExpAndSp(int charLevel, long damage, long totalDamage)
	{
		final int levelDiff = charLevel - getLevel();
		double xp = 0;
		double sp = 0;
		
		if (levelDiff < Config.MONSTER_EXP_MAX_LEVEL_DIFFERENCE)
		{
			xp = Math.max(0, (getExpReward(charLevel) * damage) / totalDamage);
			sp = Math.max(0, (getSpReward(charLevel) * damage) / totalDamage);
			
			if ((charLevel > 84) && (levelDiff <= -3))
			{
				double mul;
				switch (levelDiff)
				{
					case -3:
					{
						mul = 0.97;
						break;
					}
					case -4:
					{
						mul = 0.67;
						break;
					}
					case -5:
					{
						mul = 0.42;
						break;
					}
					case -6:
					{
						mul = 0.25;
						break;
					}
					case -7:
					{
						mul = 0.15;
						break;
					}
					case -8:
					{
						mul = 0.09;
						break;
					}
					case -9:
					{
						mul = 0.05;
						break;
					}
					case -10:
					{
						mul = 0.03;
						break;
					}
					default:
					{
						mul = 1;
						break;
					}
				}
				
				xp *= mul;
				sp *= mul;
			}
		}
		
		return new double[]
		{
			xp,
			sp
		};
	}
	
	public double calculateOverhitExp(double exp)
	{
		// Obtem a porcentagem baseada no total de dano extra (dano excessivo) causado em relacao a quantidade total (maxima) de HP do Atacavel
		double overhitPercentage = ((_overhitDamage * 100) / getMaxHp());
		
		// Porcentagens de dano excessivo sao limitadas a 25% no maximo
		if (overhitPercentage > 25)
		{
			overhitPercentage = 25;
		}
		
		// Obtem o bonus de exp por dano excessivo de acordo com a porcentagem de dano excessivo acima
		// (base 1/1 - 13% de dano excessivo, 13% de exp extra e dado, e assim por diante...)
		return (overhitPercentage / 100) * exp;
	}
	
	/**
	 * Retorna True.
	 */
	@Override
	public boolean canBeAttacked()
	{
		return true;
	}
	
	@Override
	public void onSpawn()
	{
		super.onSpawn();
		
		// Limpa saque e semente do mob
		setSpoilerObjectId(0);
		
		// Limpa toda lista de agressao
		clearAggroList();
		
		// Limpa recompensa do Colhedor
		_harvestItem.set(null);
		
		setWalking();
		
		// Limpa estado de Semeado modificado
		_seeded = false;
		_seed = null;
		_seederObjId = 0;
		// Limpa valor de dano excessivo
		overhitEnabled(false);
		
		_sweepItems.set(null);
		resetAbsorbList();
		
		setWalking();
		
		// Verifica a regiao onde este mob esta, nao ativa a IA se a regiao estiver inativa.
		if (hasAI())
		{
			// Define a intencao do Atacavel para ACTIVE
			getAI().setIntention(Intention.ACTIVE);
			
			// Verifica a regiao onde este mob esta, nao ativa a IA se a regiao estiver inativa.
			if (!isInActiveRegion())
			{
				getAI().stopAITask();
			}
		}
	}
	
	/**
	 * Verifica se esta saqueado.
	 * @return {@code true} se estiver saqueado, {@code false} caso contrario
	 */
	public boolean isSpoiled()
	{
		return _spoilerObjectId != 0;
	}
	
	/**
	 * Obtem o ID do objeto do saqueador.
	 * @return o ID do objeto do saqueador se estiver saqueado, 0 caso contrario
	 */
	public int getSpoilerObjectId()
	{
		return _spoilerObjectId;
	}
	
	/**
	 * Define o ID do objeto do saqueador.
	 * @param spoilerObjectId spoilerObjectId o ID do objeto do saqueador
	 */
	public void setSpoilerObjectId(int spoilerObjectId)
	{
		_spoilerObjectId = spoilerObjectId;
	}
	
	/**
	 * Define o estado do mob como semeado. Parametros precisam ser definidos antes.
	 * @param seeder
	 */
	public void setSeeded(Player seeder)
	{
		if ((_seed == null) || (_seederObjId != seeder.getObjectId()))
		{
			return;
		}
		
		_seeded = true;
		int count = 1;
		for (int skillId : getTemplate().getSkills().keySet())
		{
			switch (skillId)
			{
				case 4303: // Strong type x2
				{
					count *= 2;
					break;
				}
				case 4304: // Strong type x3
				{
					count *= 3;
					break;
				}
				case 4305: // Strong type x4
				{
					count *= 4;
					break;
				}
				case 4306: // Strong type x5
				{
					count *= 5;
					break;
				}
				case 4307: // Strong type x6
				{
					count *= 6;
					break;
				}
				case 4308: // Strong type x7
				{
					count *= 7;
					break;
				}
				case 4309: // Strong type x8
				{
					count *= 8;
					break;
				}
				case 4310: // Strong type x9
				{
					count *= 9;
					break;
				}
			}
		}
		
		// bonus de mobs de alto nivel
		final int diff = getLevel() - _seed.getLevel() - 5;
		if (diff > 0)
		{
			count += diff;
		}
		
		_harvestItem.set(new ItemHolder(_seed.getCropId(), count * Config.RATE_DROP_MANOR));
	}
	
	/**
	 * Define os parametros da semente, mas nao o estado de semeado
	 * @param seed - instancia {@link Seed} da semente usada
	 * @param seeder - jogador que semeia
	 */
	public void setSeeded(Seed seed, Player seeder)
	{
		if (_seeded)
		{
			return;
		}
		
		_seed = seed;
		_seederObjId = seeder.getObjectId();
	}
	
	public int getSeederId()
	{
		return _seederObjId;
	}
	
	public Seed getSeed()
	{
		return _seed;
	}
	
	public boolean isSeeded()
	{
		return _seeded;
	}
	
	/**
	 * Define atraso para chamada onKill(), em ms Padrao: 5000 ms
	 * @param delay
	 */
	public void setOnKillDelay(int delay)
	{
		_onKillDelay = delay;
	}
	
	public int getOnKillDelay()
	{
		return _onKillDelay;
	}
	
	/**
	 * Verifica se o servidor permite Animacao Aleatoria.
	 */
	// Isso esta localizado aqui porque Monster e FriendlyMob ambos estendem esta classe. As outras instancias nao-pc estendem ou Npc ou Monster.
	@Override
	public boolean hasRandomAnimation()
	{
		return ((Config.MAX_MONSTER_ANIMATION > 0) && isRandomAnimationEnabled() && !(this instanceof GrandBoss));
	}
	
	public void setCommandChannelTimer(CommandChannelTimer commandChannelTimer)
	{
		_commandChannelTimer = commandChannelTimer;
	}
	
	public CommandChannelTimer getCommandChannelTimer()
	{
		return _commandChannelTimer;
	}
	
	public CommandChannel getFirstCommandChannelAttacked()
	{
		return _firstCommandChannelAttacked;
	}
	
	public void setFirstCommandChannelAttacked(CommandChannel firstCommandChannelAttacked)
	{
		_firstCommandChannelAttacked = firstCommandChannelAttacked;
	}
	
	/**
	 * @return o _commandChannelLastAttack
	 */
	public long getCommandChannelLastAttack()
	{
		return _commandChannelLastAttack;
	}
	
	/**
	 * @param channelLastAttack o _commandChannelLastAttack a definir
	 */
	public void setCommandChannelLastAttack(long channelLastAttack)
	{
		_commandChannelLastAttack = channelLastAttack;
	}
	
	public void returnHome()
	{
		clearAggroList();
		
		if (hasAI() && (getSpawn() != null))
		{
			getAI().setIntention(Intention.MOVE_TO, getSpawn().getLocation());
		}
	}
	
	/*
	 * Retorna diminuicao (se positivo) ou aumento (se negativo) de pontos de vitalidade baseado no dano. Maximo para dano = maxHp.
	 */
	public float getVitalityPoints(int level, long damage)
	{
		// verificacao de sanidade
		if (damage <= 0)
		{
			return 0;
		}
		
		final long expReward = getExpReward(level);
		final float divider = (getLevel() > 0) && (expReward > 0) ? (getTemplate().getBaseHpMax() * 9 * getLevel() * getLevel()) / (100 * expReward) : 0;
		if (divider == 0)
		{
			return 0;
		}
		
		// valor negativo - vitalidade sera consumida
		return -Math.min(damage, getMaxHp()) / divider;
	}
	
	/*
	 * True se a taxa de vitalidade para exp e sp deve ser aplicada
	 */
	public boolean useVitalityRate()
	{
		return !_champion || Config.CHAMPION_ENABLE_VITALITY;
	}
	
	/** Retorna True se a Criatura for RaidBoss ou seu lacaio. */
	@Override
	public boolean isRaid()
	{
		return _isRaid;
	}
	
	/**
	 * Define este Npc como uma instancia de Raide.
	 * @param isRaid
	 */
	public void setIsRaid(boolean isRaid)
	{
		_isRaid = isRaid;
	}
	
	/**
	 * Define este Npc como uma instancia de Lacaio.
	 * @param value
	 */
	public void setIsRaidMinion(boolean value)
	{
		_isRaid = value;
		_isRaidMinion = value;
	}
	
	@Override
	public boolean isRaidMinion()
	{
		return _isRaidMinion;
	}
	
	@Override
	public boolean isMinion()
	{
		return getLeader() != null;
	}
	
	/**
	 * @return lider deste lacaio ou nulo.
	 */
	public Attackable getLeader()
	{
		return null;
	}
	
	public void setChampion(boolean champ)
	{
		_champion = champ;
		if (Config.SHOW_CHAMPION_AURA)
		{
			setTeam(champ ? Team.RED : Team.NONE);
		}
	}
	
	@Override
	public boolean isChampion()
	{
		return _champion;
	}
	
	@Override
	public boolean isAttackable()
	{
		return true;
	}
	
	@Override
	public Attackable asAttackable()
	{
		return this;
	}
}
