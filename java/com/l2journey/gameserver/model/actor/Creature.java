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
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.StampedLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.l2journey.Config;
import com.l2journey.EventsConfig;
import com.l2journey.commons.threads.ThreadPool;
import com.l2journey.commons.util.Rnd;
import com.l2journey.gameserver.GeoData;
import com.l2journey.gameserver.ai.Action;
import com.l2journey.gameserver.ai.AttackableAI;
import com.l2journey.gameserver.ai.CreatureAI;
import com.l2journey.gameserver.ai.CreatureAI.IntentionCommand;
import com.l2journey.gameserver.ai.Intention;
import com.l2journey.gameserver.cache.RelationCache;
import com.l2journey.gameserver.data.enums.CategoryType;
import com.l2journey.gameserver.data.xml.CategoryData;
import com.l2journey.gameserver.data.xml.DoorData;
import com.l2journey.gameserver.data.xml.FenceData;
import com.l2journey.gameserver.data.xml.ItemData;
import com.l2journey.gameserver.data.xml.NpcData;
import com.l2journey.gameserver.data.xml.SendMessageLocalisationData;
import com.l2journey.gameserver.managers.CaptchaManager;
import com.l2journey.gameserver.managers.IdManager;
import com.l2journey.gameserver.managers.InstanceManager;
import com.l2journey.gameserver.managers.MapRegionManager;
import com.l2journey.gameserver.managers.TerritoryWarManager;
import com.l2journey.gameserver.managers.ZoneManager;
import com.l2journey.gameserver.model.AccessLevel;
import com.l2journey.gameserver.model.EffectList;
import com.l2journey.gameserver.model.Location;
import com.l2journey.gameserver.model.Spawn;
import com.l2journey.gameserver.model.TimeStamp;
import com.l2journey.gameserver.model.World;
import com.l2journey.gameserver.model.WorldObject;
import com.l2journey.gameserver.model.WorldRegion;
import com.l2journey.gameserver.model.actor.enums.creature.InstanceType;
import com.l2journey.gameserver.model.actor.enums.creature.Race;
import com.l2journey.gameserver.model.actor.enums.creature.Team;
import com.l2journey.gameserver.model.actor.enums.player.TeleportWhereType;
import com.l2journey.gameserver.model.actor.holders.creature.InvulSkillHolder;
import com.l2journey.gameserver.model.actor.instance.GrandBoss;
import com.l2journey.gameserver.model.actor.instance.QuestGuard;
import com.l2journey.gameserver.model.actor.stat.CreatureStat;
import com.l2journey.gameserver.model.actor.status.CreatureStatus;
import com.l2journey.gameserver.model.actor.tasks.creature.HitTask;
import com.l2journey.gameserver.model.actor.tasks.creature.MagicUseTask;
import com.l2journey.gameserver.model.actor.tasks.creature.NotifyAITask;
import com.l2journey.gameserver.model.actor.tasks.creature.QueuedMagicUseTask;
import com.l2journey.gameserver.model.actor.templates.CreatureTemplate;
import com.l2journey.gameserver.model.actor.templates.NpcTemplate;
import com.l2journey.gameserver.model.actor.transform.Transform;
import com.l2journey.gameserver.model.actor.transform.TransformTemplate;
import com.l2journey.gameserver.model.clan.Clan;
import com.l2journey.gameserver.model.effects.EffectFlag;
import com.l2journey.gameserver.model.effects.EffectType;
import com.l2journey.gameserver.model.events.Containers;
import com.l2journey.gameserver.model.events.EventDispatcher;
import com.l2journey.gameserver.model.events.EventType;
import com.l2journey.gameserver.model.events.holders.actor.creature.OnCreatureAttack;
import com.l2journey.gameserver.model.events.holders.actor.creature.OnCreatureAttackAvoid;
import com.l2journey.gameserver.model.events.holders.actor.creature.OnCreatureAttacked;
import com.l2journey.gameserver.model.events.holders.actor.creature.OnCreatureDamageDealt;
import com.l2journey.gameserver.model.events.holders.actor.creature.OnCreatureDamageReceived;
import com.l2journey.gameserver.model.events.holders.actor.creature.OnCreatureDeath;
import com.l2journey.gameserver.model.events.holders.actor.creature.OnCreatureKilled;
import com.l2journey.gameserver.model.events.holders.actor.creature.OnCreatureSee;
import com.l2journey.gameserver.model.events.holders.actor.creature.OnCreatureSkillUse;
import com.l2journey.gameserver.model.events.holders.actor.creature.OnCreatureTeleported;
import com.l2journey.gameserver.model.events.holders.actor.npc.OnNpcSkillSee;
import com.l2journey.gameserver.model.events.holders.actor.npc.attackable.OnAttackableFactionCall;
import com.l2journey.gameserver.model.events.listeners.AbstractEventListener;
import com.l2journey.gameserver.model.events.returns.TerminateReturn;
import com.l2journey.gameserver.model.groups.Party;
import com.l2journey.gameserver.model.instancezone.Instance;
import com.l2journey.gameserver.model.interfaces.ILocational;
import com.l2journey.gameserver.model.item.ItemTemplate;
import com.l2journey.gameserver.model.item.Weapon;
import com.l2journey.gameserver.model.item.enums.ItemProcessType;
import com.l2journey.gameserver.model.item.enums.ShotType;
import com.l2journey.gameserver.model.item.instance.Item;
import com.l2journey.gameserver.model.item.type.WeaponType;
import com.l2journey.gameserver.model.itemcontainer.Inventory;
import com.l2journey.gameserver.model.options.OptionSkillHolder;
import com.l2journey.gameserver.model.options.OptionSkillType;
import com.l2journey.gameserver.model.skill.AbnormalType;
import com.l2journey.gameserver.model.skill.AbnormalVisualEffect;
import com.l2journey.gameserver.model.skill.BuffFinishTask;
import com.l2journey.gameserver.model.skill.BuffInfo;
import com.l2journey.gameserver.model.skill.CommonSkill;
import com.l2journey.gameserver.model.skill.EffectScope;
import com.l2journey.gameserver.model.skill.Skill;
import com.l2journey.gameserver.model.skill.SkillChannelized;
import com.l2journey.gameserver.model.skill.SkillChannelizer;
import com.l2journey.gameserver.model.skill.enums.FlyType;
import com.l2journey.gameserver.model.skill.enums.SkillFinishType;
import com.l2journey.gameserver.model.skill.holders.SkillHolder;
import com.l2journey.gameserver.model.skill.holders.SkillUseHolder;
import com.l2journey.gameserver.model.skill.targets.TargetType;
import com.l2journey.gameserver.model.stats.BaseStat;
import com.l2journey.gameserver.model.stats.Calculator;
import com.l2journey.gameserver.model.stats.Formulas;
import com.l2journey.gameserver.model.stats.Stat;
import com.l2journey.gameserver.model.stats.functions.AbstractFunction;
import com.l2journey.gameserver.model.zone.ZoneId;
import com.l2journey.gameserver.model.zone.ZoneRegion;
import com.l2journey.gameserver.network.Disconnection;
import com.l2journey.gameserver.network.GameClient;
import com.l2journey.gameserver.network.SystemMessageId;
import com.l2journey.gameserver.network.serverpackets.AbstractNpcInfo;
import com.l2journey.gameserver.network.serverpackets.ActionFailed;
import com.l2journey.gameserver.network.serverpackets.Attack;
import com.l2journey.gameserver.network.serverpackets.ChangeMoveType;
import com.l2journey.gameserver.network.serverpackets.ChangeWaitType;
import com.l2journey.gameserver.network.serverpackets.ExBrAgathionEnergyInfo;
import com.l2journey.gameserver.network.serverpackets.ExRotation;
import com.l2journey.gameserver.network.serverpackets.FlyToLocation;
import com.l2journey.gameserver.network.serverpackets.MagicSkillCanceled;
import com.l2journey.gameserver.network.serverpackets.MagicSkillLaunched;
import com.l2journey.gameserver.network.serverpackets.MagicSkillUse;
import com.l2journey.gameserver.network.serverpackets.MoveToLocation;
import com.l2journey.gameserver.network.serverpackets.MoveToPawn;
import com.l2journey.gameserver.network.serverpackets.Revive;
import com.l2journey.gameserver.network.serverpackets.ServerObjectInfo;
import com.l2journey.gameserver.network.serverpackets.ServerPacket;
import com.l2journey.gameserver.network.serverpackets.SetupGauge;
import com.l2journey.gameserver.network.serverpackets.SocialAction;
import com.l2journey.gameserver.network.serverpackets.StatusUpdate;
import com.l2journey.gameserver.network.serverpackets.StopMove;
import com.l2journey.gameserver.network.serverpackets.SystemMessage;
import com.l2journey.gameserver.network.serverpackets.TeleportToLocation;
import com.l2journey.gameserver.pathfinding.AbstractNodeLoc;
import com.l2journey.gameserver.pathfinding.PathFinding;
import com.l2journey.gameserver.taskmanagers.AttackStanceTaskManager;
import com.l2journey.gameserver.taskmanagers.CreatureSeeTaskManager;
import com.l2journey.gameserver.taskmanagers.GameTimeTaskManager;
import com.l2journey.gameserver.taskmanagers.MovementTaskManager;
import com.l2journey.gameserver.util.Broadcast;
import com.l2journey.gameserver.util.LocationUtil;

/**
 * Classe mae de todos os objetos de personagem do mundo (PC, NPC...)<br>
 * Creature:<br>
 * <ul>
 * <li>Door</li>
 * <li>Playable</li>
 * <li>Npc</li>
 * <li>StaticObject</li>
 * <li>Trap</li>
 * <li>Vehicle</li>
 * </ul>
 * <b>Conceito de CreatureTemplate:</b><br>
 * Cada Creature possui propriedades genericas e estaticas (ex: todos os Keltir tem o mesmo numero de HP...).<br>
 * Todas essas propriedades sao armazenadas em um template diferente para cada tipo de Creature.<br>
 * Cada template e carregado uma vez na memoria cache do servidor (reduz uso de memoria).<br>
 * Quando uma nova instancia de Creature e criada, o servidor apenas cria um link entre a instancia e o template.<br>
 * Esse link e armazenado em {@link #_template}
 * @version $Revision: 1.53.2.45.2.34 $ $Date: 2005/04/11 10:06:08 $
 */
public abstract class Creature extends WorldObject
{
	public static final Logger LOGGER = Logger.getLogger(Creature.class.getName());
	
	private final Set<Creature> _attackByList = ConcurrentHashMap.newKeySet(1);
	private volatile boolean _isCastingNow = false;
	private volatile boolean _isCastingSimultaneouslyNow = false;
	private Skill _lastSkillCast;
	private Skill _lastSimultaneousSkillCast;
	
	private boolean _isDead = false;
	private boolean _isImmobilized = false;
	private boolean _isOverloaded = false; // o personagem esta carregando peso demais
	private boolean _isParalyzed = false;
	private boolean _isPendingRevive = false;
	private boolean _isRunning = false;
	protected boolean _showSummonAnimation = false;
	protected boolean _isTeleporting = false;
	private boolean _isInvul = false;
	private boolean _isMortal = true; // Personagem morre quando HP chega a 0
	private boolean _isFlying = false;
	
	private CreatureStat _stat;
	private CreatureStatus _status;
	private CreatureTemplate _template; // O link para o objeto CreatureTemplate contendo propriedades genericas e estaticas deste tipo de Creature (ex: Max HP, Speed...)
	private String _title;
	
	public static final double MAX_HP_BAR_PX = 352.0;
	
	private double _hpUpdateIncCheck = .0;
	private double _hpUpdateDecCheck = .0;
	private double _hpUpdateInterval = .0;
	
	private int _karma = 0;
	
	/** Tabela de Calculators contendo todos os calculadores utilizados */
	private Calculator[] _calculators;
	/** Map contendo todas as skills deste personagem. */
	private final Map<Integer, Skill> _skills = new ConcurrentHashMap<>();
	/** Map contendo os timestamps de reuso de skills. */
	private final Map<Integer, TimeStamp> _reuseTimeStampsSkills = new ConcurrentHashMap<>();
	/** Map contendo os timestamps de reuso de itens. */
	private final Map<Integer, TimeStamp> _reuseTimeStampsItems = new ConcurrentHashMap<>();
	/** Map contendo todas as skills desabilitadas. */
	private final Map<Integer, Long> _disabledSkills = new ConcurrentHashMap<>();
	private boolean _allSkillsDisabled;
	
	private final byte[] _zones = new byte[ZoneId.getZoneCount()];
	protected final Location _lastZoneValidateLocation = new Location(getX(), getY(), getZ());
	
	private boolean _isInTownWar;
	
	private final StampedLock _attackLock = new StampedLock();
	
	private Creature _debugger = null;
	
	private Team _team = Team.NONE;
	
	private boolean _lethalable = true;
	
	private final Map<Integer, OptionSkillHolder> _triggerSkills = new ConcurrentHashMap<>(1);
	
	private final Map<Integer, InvulSkillHolder> _invulAgainst = new ConcurrentHashMap<>(1);
	/** Lista de efeitos da Creature. */
	private final EffectList _effectList = new EffectList(this);
	/** A criatura que invocou este personagem. */
	private Creature _summoner = null;
	
	private SkillChannelizer _channelizer = null;
	
	private SkillChannelized _channelized = null;
	
	private final BuffFinishTask _buffFinishTask = new BuffFinishTask();
	
	/** Mapa de 32 bits, contendo todos os efeitos visuais anormais em andamento. */
	private int _abnormalVisualEffects;
	/** Mapa de 32 bits, contendo todos os efeitos visuais anormais especiais em andamento. */
	private int _abnormalVisualEffectsSpecial;
	/** Mapa de 32 bits, contendo todos os efeitos visuais anormais de evento em andamento. */
	private int _abnormalVisualEffectsEvent;
	
	/** Dados de movimento desta Creature */
	protected MoveData _move;
	private boolean _cursorKeyMovement = false;
	
	/** O alvo desta criatura. */
	private WorldObject _target;
	/** Representa o momento em que o ataque deve terminar, em nanossegundos. */
	private volatile long _attackEndTime;
	private int _disableBowAttackEndTime;
	
	private int _castInterruptTime;
	
	/** Tabela de calculadores contendo todos os calculadores padrao de NPC (ex: ACCURACY_COMBAT, EVASION_RATE) */
	private static final Calculator[] NPC_STD_CALCULATOR = Formulas.getStdNPCCalculators();
	
	private volatile CreatureAI _ai = null;
	
	/** Futuro do Cast de Skill */
	protected Future<?> _skillCast;
	protected Future<?> _skillCast2;
	
	private final Map<Integer, RelationCache> _knownRelations = new ConcurrentHashMap<>();
	
	private Set<Creature> _seenCreatures = null;
	private int _seenCreatureRange = Config.ALT_PARTY_RANGE;
	
	/** Uma lista contendo os itens dropados deste fake player. */
	
	private OnCreatureAttack _onCreatureAttack = null;
	private OnCreatureAttacked _onCreatureAttacked = null;
	private OnCreatureDamageDealt _onCreatureDamageDealt = null;
	private OnCreatureDamageReceived _onCreatureDamageReceived = null;
	private OnCreatureAttackAvoid _onCreatureAttackAvoid = null;
	private OnCreatureSkillUse _onCreatureSkillUse = null;
	
	/**
	 * Cria uma criatura.
	 * @param template o template da criatura
	 */
	public Creature(CreatureTemplate template)
	{
		this(IdManager.getInstance().getNextId(), template);
	}
	
	/**
	 * Construtor de Creature.<br>
	 * <br>
	 * <b><u>Conceito</u>:</b><br>
	 * <br>
	 * Cada Creature possui propriedades genericas e estaticas (ex: todos os Keltir tem o mesmo numero de HP...).<br>
	 * Todas essas propriedades sao armazenadas em um template diferente para cada tipo de Creature. Cada template e carregado uma vez na memoria cache do servidor (reduz uso de memoria).<br>
	 * Quando uma nova instancia de Creature e criada, o servidor apenas cria um link entre a instancia e o template. Esse link e armazenado em <b>_template</b><br>
	 * <br>
	 * <b><u>Acoes</u>:</b>
	 * <ul>
	 * <li>Define o _template da Creature</li>
	 * <li>Define _overloaded como false (o personagem pode carregar mais itens)</li>
	 * <li>Se a Creature for um Npc, copia as skills do template para o objeto</li>
	 * <li>Se a Creature for um Npc, vincula _calculators ao NPC_STD_CALCULATOR</li>
	 * <li>Se a Creature NAO for um Npc, cria um slot vazio de _skills</li>
	 * <li>Se a Creature for um Player ou Summon, copia o conjunto basico de Calculator para o objeto</li>
	 * </ul>
	 * @param objectId Identificador do objeto a ser inicializado
	 * @param template O CreatureTemplate a ser aplicado ao objeto
	 */
	public Creature(int objectId, CreatureTemplate template)
	{
		super(objectId);
		if (template == null)
		{
			throw new NullPointerException("Template is null!");
		}
		
		setInstanceType(InstanceType.Creature);
		initCharStat();
		initCharStatus();
		
		// Define seu template para a nova Creature
		_template = template;
		if (isDoor())
		{
			_calculators = Formulas.getStdDoorCalculators();
		}
		else if (isNpc())
		{
			// Copia os Calculators padrao do Npc em _calculators
			_calculators = NPC_STD_CALCULATOR;
			
			// Copia as skills do Npc de seu template para a instancia de Creature
			// A lista de skills pode ser afetada por efeitos de magia, entao e necessario fazer uma copia
			// para evitar que uma magia afetando um Npc afete outros Npc do mesmo tipo tambem.
			for (Skill skill : template.getSkills().values())
			{
				addSkill(skill);
			}
		}
		else
		{
			// Se a Creature for um Player ou Summon, cria o conjunto basico de calculadores
			_calculators = new Calculator[Stat.NUM_STATS];
			if (isSummon())
			{
				// Copia as skills do Summon de seu template para a instancia de Creature
				// A lista de skills pode ser afetada por efeitos de magia, entao e necessario fazer uma copia
				// para evitar que uma magia afetando um Summon afete outros Summon do mesmo tipo tambem.
				for (Skill skill : template.getSkills().values())
				{
					addSkill(skill);
				}
			}
			
			Formulas.addFuncsToNewCharacter(this);
		}
		
		setInvul(true);
	}
	
	public EffectList getEffectList()
	{
		return _effectList;
	}
	
	/**
	 * @return inventario do personagem, padrao null, sobrescrito em tipos Playable e em Npc
	 */
	public Inventory getInventory()
	{
		return null;
	}
	
	public boolean destroyItemByItemId(ItemProcessType process, int itemId, long count, WorldObject reference, boolean sendMessage)
	{
		// Padrao: NPCs consomem itens virtuais para suas skills
		// TODO: deveria ser registrado em log se acontecer.. deveria ser false
		return true;
	}
	
	public boolean destroyItem(ItemProcessType process, int objectId, long count, WorldObject reference, boolean sendMessage)
	{
		// Padrao: NPCs consomem itens virtuais para suas skills
		// TODO: deveria ser registrado em log se acontecer.. deveria ser false
		return true;
	}
	
	/**
	 * Verifica se o personagem esta na zona com o Id informado.
	 * @param zone o Id da zona a verificar
	 * @return {code true} se o personagem estiver nessa zona
	 */
	@Override
	public boolean isInsideZone(ZoneId zone)
	{
		final Instance instance = InstanceManager.getInstance().getInstance(getInstanceId());
		switch (zone)
		{
			case PVP:
			{
				if ((instance != null) && instance.isPvP())
				{
					return true;
				}
				
				return (_zones[ZoneId.PVP.ordinal()] > 0) && (_zones[ZoneId.PEACE.ordinal()] == 0) && (_zones[ZoneId.NO_PVP.ordinal()] == 0);
			}
			case PEACE:
			{
				if ((instance != null) && instance.isPvP())
				{
					return false;
				}
			}
		}
		
		return _zones[zone.ordinal()] > 0;
	}
	
	/**
	 * @param zone
	 * @param state
	 */
	public void setInsideZone(ZoneId zone, boolean state)
	{
		synchronized (_zones)
		{
			if (state)
			{
				_zones[zone.ordinal()]++;
			}
			else if (_zones[zone.ordinal()] > 0)
			{
				_zones[zone.ordinal()]--;
			}
		}
	}
	
	/**
	 * Isso retornara true se o jogador estiver transformado,<br>
	 * mas se o jogador nao estiver transformado retornara false.
	 * @return status de transformacao
	 */
	public boolean isTransformed()
	{
		return false;
	}
	
	public Transform getTransformation()
	{
		return null;
	}
	
	/**
	 * Isso ira destransformar um jogador se ele for uma instancia de Player e estiver transformado.
	 */
	public void untransform()
	{
		// Apenas um placeholder
	}
	
	/**
	 * Isso retornara true se o jogador for GM,<br>
	 * mas se o jogador nao for GM retornara false.
	 * @return status de GM
	 */
	public boolean isGM()
	{
		return false;
	}
	
	/**
	 * Sobrescrito em Player.
	 * @return o nivel de acesso.
	 */
	public AccessLevel getAccessLevel()
	{
		return null;
	}
	
	protected void initCharStatusUpdateValues()
	{
		_hpUpdateIncCheck = _stat.getMaxHp();
		_hpUpdateInterval = _hpUpdateIncCheck / MAX_HP_BAR_PX;
		_hpUpdateDecCheck = _hpUpdateIncCheck - _hpUpdateInterval;
	}
	
	/**
	 * Remove a Creature do mundo quando a tarefa de decay e executada.<br>
	 * <font color=#FF0000><b><u>Cuidado</u>: Este metodo NAO REMOVE o objeto de _allObjects do World </b></font><br>
	 * <font color=#FF0000><b><u>Cuidado</u>: Este metodo NAO ENVIA pacotes Server->Client para jogadores</b></font>
	 */
	public void onDecay()
	{
		if (Config.DISCONNECT_AFTER_DEATH && isPlayer())
		{
			final Player player = asPlayer();
			if (player.isOnline())
			{
				Disconnection.of(player).storeAndDeleteWith(new SystemMessage(SendMessageLocalisationData.getLocalisation(player, "60 min. have passed after the death of your character, so you were disconnected from the game.")));
			}
		}
		else
		{
			decayMe();
			final ZoneRegion region = ZoneManager.getInstance().getRegion(this);
			if (region != null)
			{
				region.removeFromZones(this);
			}
		}
	}
	
	@Override
	public void onSpawn()
	{
		super.onSpawn();
		
		_buffFinishTask.start();
		
		revalidateZone(true);
		
		// Configuracao customizada de anuncios de boss.
		if (this instanceof GrandBoss)
		{
			if (Config.GRANDBOSS_SPAWN_ANNOUNCEMENTS && ((getInstanceId() == 0) || Config.GRANDBOSS_INSTANCE_ANNOUNCEMENTS) && !isMinion() && !isRaidMinion())
			{
				final String name = NpcData.getInstance().getTemplate(getId()).getName();
				if ((name != null) && !Config.RAIDBOSSES_EXCLUDED_FROM_SPAWN_ANNOUNCEMENTS.contains(getId()))
				{
					Broadcast.toAllOnlinePlayers(name + " has spawned!");
					Broadcast.toAllOnlinePlayersOnScreen(name + " has spawned!");
				}
			}
		}
		else if (isRaid() && Config.RAIDBOSS_SPAWN_ANNOUNCEMENTS && ((getInstanceId() == 0) || Config.RAIDBOSS_INSTANCE_ANNOUNCEMENTS) && !isMinion() && !isRaidMinion())
		{
			final String name = NpcData.getInstance().getTemplate(getId()).getName();
			if ((name != null) && !Config.RAIDBOSSES_EXCLUDED_FROM_SPAWN_ANNOUNCEMENTS.contains(getId()))
			{
				Broadcast.toAllOnlinePlayers(name + " has spawned!");
				Broadcast.toAllOnlinePlayersOnScreen(name + " has spawned!");
			}
		}
	}
	
	public synchronized void onTeleported()
	{
		if (!_isTeleporting)
		{
			return;
		}
		
		spawnMe(getX(), getY(), getZ());
		setTeleporting(false);
		
		if (EventDispatcher.getInstance().hasListener(EventType.ON_CREATURE_TELEPORTED, this))
		{
			EventDispatcher.getInstance().notifyEventAsync(new OnCreatureTeleported(this), this);
		}
	}
	
	/**
	 * Adiciona a instancia de Creature que esta atacando a lista de atacantes.
	 * @param creature A Creature que ataca esta
	 */
	public void addAttackerToAttackByList(Creature creature)
	{
		// DS: movido para Attackable
	}
	
	/**
	 * Envia um pacote para a Creature E para todos os Player em _KnownPlayers da Creature.<br>
	 * <br>
	 * <b><u>Conceito</u>:</b><br>
	 * <br>
	 * Players na area de deteccao da Creature sao identificados em <b>_knownPlayers</b>.<br>
	 * Para informar outros jogadores sobre modificacao de estado na Creature, o servidor apenas precisa percorrer _knownPlayers para enviar pacotes Server->Client
	 * @param packet
	 */
	public void broadcastPacket(ServerPacket packet)
	{
		// TODO: Talvez adicionar alguma logica de contagem de jogadores proximos aqui.
		packet.sendInBroadcast();
		
		World.getInstance().forEachVisibleObject(this, Player.class, player ->
		{
			if (isVisibleFor(player))
			{
				player.sendPacket(packet);
			}
		});
	}
	
	/**
	 * Envia um pacote para a Creature E para todos os Player no raio (raio maximo da knownlist) a partir da Creature.<br>
	 * <br>
	 * <b><u>Conceito</u>:</b><br>
	 * <br>
	 * Players na area de deteccao da Creature sao identificados em <b>_knownPlayers</b>.<br>
	 * Para informar outros jogadores sobre modificacao de estado na Creature, o servidor apenas precisa percorrer _knownPlayers para enviar pacotes Server->Client
	 * @param packet
	 * @param radiusInKnownlist
	 */
	public void broadcastPacket(ServerPacket packet, int radiusInKnownlist)
	{
		packet.sendInBroadcast();
		
		World.getInstance().forEachVisibleObjectInRange(this, Player.class, radiusInKnownlist, player ->
		{
			if (isVisibleFor(player))
			{
				player.sendPacket(packet);
			}
		});
	}
	
	public void broadcastMoveToLocation()
	{
		broadcastMoveToLocation(false);
	}
	
	public void broadcastMoveToLocation(boolean force)
	{
		final MoveData move = _move;
		if (move == null)
		{
			return;
		}
		
		// Broadcast MoveToLocation quando forcado ou uma vez por segundo.
		final int gameTicks = GameTimeTaskManager.getInstance().getGameTicks();
		if (!force && (move.moveTimestamp > 0) && ((gameTicks - move.lastBroadcastTime) < 10))
		{
			return;
		}
		
		move.lastBroadcastTime = gameTicks;
		
		if (isPlayable())
		{
			broadcastPacket(new MoveToLocation(this));
		}
		else
		{
			final CreatureAI ai = hasAI() ? getAI() : null;
			final Intention intention = ai != null ? ai.getIntention() : null;
			final WorldObject target = ((intention == Intention.ATTACK) || (intention == Intention.FOLLOW)) ? _target : null;
			if (target != null)
			{
				if ((target != this) && !isOnGeodataPath(move))
				{
					broadcastPacket(new MoveToPawn(this, target, getAI().getClientMovingToPawnOffset()));
				}
				else
				{
					broadcastPacket(new MoveToLocation(this));
				}
			}
			else
			{
				final WorldRegion region = getWorldRegion();
				if (((region != null) && region.areNeighborsActive()))
				{
					broadcastPacket(new MoveToLocation(this));
				}
			}
		}
	}
	
	public void broadcastSocialAction(int id)
	{
		if (isPlayable())
		{
			broadcastPacket(new SocialAction(getObjectId(), id));
		}
		else
		{
			final WorldRegion region = getWorldRegion();
			if ((region != null) && region.areNeighborsActive())
			{
				broadcastPacket(new SocialAction(getObjectId(), id));
			}
		}
	}
	
	/**
	 * @return true se a atualizacao de hp deve ser feita, false se nao.
	 */
	protected boolean needHpUpdate()
	{
		final double currentHp = _status.getCurrentHp();
		final double maxHp = _stat.getMaxHp();
		if ((currentHp <= 1.0) || (maxHp < MAX_HP_BAR_PX))
		{
			return true;
		}
		
		if ((currentHp <= _hpUpdateDecCheck) || (currentHp >= _hpUpdateIncCheck))
		{
			if (currentHp == maxHp)
			{
				_hpUpdateIncCheck = currentHp + 1;
				_hpUpdateDecCheck = currentHp - _hpUpdateInterval;
			}
			else
			{
				final double doubleMulti = currentHp / _hpUpdateInterval;
				int intMulti = (int) doubleMulti;
				_hpUpdateDecCheck = _hpUpdateInterval * (doubleMulti < intMulti ? intMulti - 1 : intMulti);
				_hpUpdateIncCheck = _hpUpdateDecCheck + _hpUpdateInterval;
			}
			
			return true;
		}
		
		return false;
	}
	
	/**
	 * Envia o pacote Server->Client StatusUpdate com HP e MP atuais para todos os outros Players para informar.<br>
	 * <br>
	 * <b><u>Acoes</u>:</b>
	 * <ul>
	 * <li>Cria o pacote Server->Client StatusUpdate com HP e MP atuais</li>
	 * <li>Envia o pacote Server->Client StatusUpdate com HP e MP atuais para todas as Creatures chamadas _statusListener que devem ser informadas sobre atualizacoes de HP/MP desta Creature</li>
	 * </ul>
	 * <font color=#FF0000><b><u>Cuidado</u>: Este metodo NAO ENVIA informacao de CP</b></font>
	 */
	public void broadcastStatusUpdate()
	{
		if (_status.getStatusListener().isEmpty() || !needHpUpdate())
		{
			return;
		}
		
		// Cria o pacote Server->Client StatusUpdate com HP atual
		final StatusUpdate su = new StatusUpdate(this);
		su.addAttribute(StatusUpdate.MAX_HP, _stat.getMaxHp());
		su.addAttribute(StatusUpdate.CUR_HP, (int) _status.getCurrentHp());
		
		// Percorre os StatusListener
		// Envia o pacote Server->Client StatusUpdate com HP e MP atuais
		for (Creature temp : _status.getStatusListener())
		{
			if (temp != null)
			{
				temp.sendPacket(su);
			}
		}
	}
	
	/**
	 * @param text
	 */
	public void sendMessage(String text)
	{
		// implementacao padrao
	}
	
	/**
	 * Teleporta uma Creature e seu pet se necessario.<br>
	 * <br>
	 * <b><u>Acoes</u>:</b>
	 * <ul>
	 * <li>Para o movimento da Creature</li>
	 * <li>Define a posicao x,y,z do WorldObject e se necessario modifica sua _worldRegion</li>
	 * <li>Envia um pacote Server->Client TeleportToLocation para a Creature E para todos os Players em seus _KnownPlayers</li>
	 * <li>Modifica a posicao do pet se necessario</li>
	 * </ul>
	 * @param xValue
	 * @param yValue
	 * @param zValue
	 * @param headingValue
	 * @param instanceId
	 * @param randomOffset
	 */
	public void teleToLocation(int xValue, int yValue, int zValue, int headingValue, int instanceId, int randomOffset)
	{
		// Previne teleporte para jogadores que desconectaram inesperadamente.
		if (isPlayer() && !asPlayer().isOnline())
		{
			return;
		}
		
		int x = xValue;
		int y = yValue;
		int z = _isFlying ? zValue : GeoData.getInstance().getHeight(x, y, zValue);
		int heading = headingValue;
		
		// Prepara a criatura para teleporte.
		if (_isPendingRevive)
		{
			doRevive();
		}
		
		// Aborta acoes do cliente, cast e remove alvo.
		stopMove(null);
		abortAttack();
		abortCast();
		setTarget(null);
		
		setTeleporting(true);
		
		getAI().setIntention(Intention.ACTIVE);
		
		// Remove o objeto de sua localizacao antiga.
		decayMe();
		
		// Ajusta a posicao um pouco.
		if (Config.OFFSET_ON_TELEPORT_ENABLED && (randomOffset > 0))
		{
			x += Rnd.get(-randomOffset, randomOffset);
			y += Rnd.get(-randomOffset, randomOffset);
		}
		z += 5;
		
		// Envia pacote de teleporte onde necessario.
		broadcastPacket(new TeleportToLocation(this, x, y, z, heading));
		
		// Muda o id da instancia.
		setInstanceId(instanceId);
		
		// Define a posicao x,y,z do WorldObject e se necessario modifica sua _worldRegion.
		setXYZ(x, y, z);
		
		// Tambem ajusta o heading.
		if (heading != 0)
		{
			setHeading(heading);
		}
		
		// Permite recall de personagens desconectados.
		if (isPlayer())
		{
			final Player player = asPlayer();
			final GameClient client = player.getClient();
			if ((client != null) && client.isDetached())
			{
				onTeleported();
			}
		}
		else
		{
			onTeleported();
		}
		
		revalidateZone(true);
	}
	
	public void teleToLocation(int x, int y, int z, int heading, int instanceId, boolean randomOffset)
	{
		teleToLocation(x, y, z, heading, instanceId, (randomOffset) ? Config.MAX_OFFSET_ON_TELEPORT : 0);
	}
	
	public void teleToLocation(int x, int y, int z, int heading, int instanceId)
	{
		teleToLocation(x, y, z, heading, instanceId, 0);
	}
	
	public void teleToLocation(int x, int y, int z, int heading, boolean randomOffset)
	{
		if (EventsConfig.TW_DISABLE_GK && isInTownWarEvent() && !isPendingRevive())
		{
			sendMessage("You can't teleport during Town War Event.");
			return;
		}
		
		teleToLocation(x, y, z, heading, -1, (randomOffset) ? Config.MAX_OFFSET_ON_TELEPORT : 0);
	}
	
	public void teleToLocation(int x, int y, int z, int heading)
	{
		teleToLocation(x, y, z, heading, -1, 0);
	}
	
	public void teleToLocation(int x, int y, int z, boolean randomOffset)
	{
		teleToLocation(x, y, z, 0, -1, (randomOffset) ? Config.MAX_OFFSET_ON_TELEPORT : 0);
	}
	
	public void teleToLocation(int x, int y, int z)
	{
		teleToLocation(x, y, z, 0, -1, 0);
	}
	
	public void teleToLocation(ILocational loc, int randomOffset)
	{
		teleToLocation(loc.getX(), loc.getY(), loc.getZ(), loc.getHeading(), loc.getInstanceId(), randomOffset);
	}
	
	public void teleToLocation(ILocational loc, int instanceId, int randomOffset)
	{
		teleToLocation(loc.getX(), loc.getY(), loc.getZ(), loc.getHeading(), instanceId, randomOffset);
	}
	
	public void teleToLocation(ILocational loc, boolean randomOffset)
	{
		teleToLocation(loc.getX(), loc.getY(), loc.getZ(), loc.getHeading(), loc.getInstanceId(), (randomOffset) ? Config.MAX_OFFSET_ON_TELEPORT : 0);
	}
	
	public void teleToLocation(ILocational loc)
	{
		teleToLocation(loc.getX(), loc.getY(), loc.getZ(), loc.getHeading(), loc.getInstanceId(), 0);
	}
	
	public void teleToLocation(TeleportWhereType teleportWhere)
	{
		teleToLocation(MapRegionManager.getInstance().getTeleToLocation(this, teleportWhere), true);
	}
	
	private boolean canUseRangeWeapon()
	{
		if (isTransformed())
		{
			return true;
		}
		
		// Verifica flechas e MP
		if (isPlayer())
		{
			final Weapon weaponItem = getActiveWeaponItem();
			if ((weaponItem == null) || !weaponItem.isRange())
			{
				return false;
			}
			
			// Equipa flechas necessarias na mao esquerda e envia um pacote Server->Client ItemList ao Player, depois retorna True
			if (!checkAndEquipArrows())
			{
				// Cancela a acao porque o Player nao tem flechas
				getAI().setIntention(Intention.IDLE);
				sendPacket(ActionFailed.STATIC_PACKET);
				sendPacket(weaponItem.isBow() ? SystemMessageId.YOU_HAVE_RUN_OUT_OF_ARROWS : SystemMessageId.NOT_ENOUGH_BOLTS);
				return false;
			}
			
			// Verifica se o arco pode ser usado
			if (_disableBowAttackEndTime <= GameTimeTaskManager.getInstance().getGameTicks())
			{
				// Verifica se o Player possui MP suficiente
				int mpConsume = weaponItem.getMpConsume();
				if ((weaponItem.getReducedMpConsume() > 0) && (Rnd.get(100) < weaponItem.getReducedMpConsumeChance()))
				{
					mpConsume = weaponItem.getReducedMpConsume();
				}
				
				mpConsume = (int) calcStat(Stat.BOW_MP_CONSUME_RATE, mpConsume, null, null);
				if (_status.getCurrentMp() < mpConsume)
				{
					// Se o Player nao tiver MP suficiente, para o ataque
					ThreadPool.schedule(new NotifyAITask(this, Action.READY_TO_ACT), 1000);
					sendPacket(SystemMessageId.NOT_ENOUGH_MP);
					sendPacket(ActionFailed.STATIC_PACKET);
					return false;
				}
				
				// Se o Player tiver MP suficiente, o arco consome
				if (mpConsume > 0)
				{
					_status.reduceMp(mpConsume);
				}
				
				// Define o periodo de nao reutilizacao do arco
				_disableBowAttackEndTime = (5 * GameTimeTaskManager.TICKS_PER_SECOND) + GameTimeTaskManager.getInstance().getGameTicks();
			}
			else
			{
				// Cancela a acao porque o arco nao pode ser reutilizado neste momento
				ThreadPool.schedule(new NotifyAITask(this, Action.READY_TO_ACT), 1000);
				sendPacket(ActionFailed.STATIC_PACKET);
				return false;
			}
		}
		else if (isNpc())
		{
			if (_disableBowAttackEndTime > GameTimeTaskManager.getInstance().getGameTicks())
			{
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Executa um ataque fisico contra um alvo (Simples, Arco, Lanca ou Dual).<br>
	 * <br>
	 * <b><u>Acoes</u>:</b>
	 * <ul>
	 * <li>Obtem a arma ativa (sempre equipada na mao direita)</li>
	 * <li>Se a arma for um arco, verifica flechas, MP e delay de reutilizacao do arco (se necessario, equipa o Player com flechas na mao esquerda)</li>
	 * <li>Se a arma for um arco, consome MP e define o novo periodo de nao reutilizacao do arco</li>
	 * <li>Obtem a Velocidade de Ataque da Creature (delay em milissegundos antes do proximo ataque)</li>
	 * <li>Seleciona o tipo de ataque a iniciar (Simples, Arco, Lanca ou Dual) e verifica se SoulShots estao carregados, entao inicia o calculo</li>
	 * <li>Se o pacote Server->Client Attack contiver pelo menos 1 hit, envia o pacote Server->Client Attack para a Creature E para todos os Players em _KnownPlayers da Creature</li>
	 * <li>Notifica a AI com READY_TO_ACT</li>
	 * </ul>
	 * @param target A Creature alvo
	 */
	public void doAttack(Creature target)
	{
		final long stamp = _attackLock.tryWriteLock();
		if (stamp == 0)
		{
			return;
		}
		
		try
		{
			if ((target == null) || isAttackDisabled() || !target.isTargetable())
			{
				return;
			}
			
			// Notifica scripts
			if (EventDispatcher.getInstance().hasListener(EventType.ON_CREATURE_ATTACK, this))
			{
				if (_onCreatureAttack == null)
				{
					_onCreatureAttack = new OnCreatureAttack();
				}
				
				_onCreatureAttack.setAttacker(this);
				_onCreatureAttack.setTarget(target);
				final TerminateReturn attackReturn = EventDispatcher.getInstance().notifyEvent(_onCreatureAttack, this, TerminateReturn.class);
				if ((attackReturn != null) && attackReturn.terminate())
				{
					getAI().setIntention(Intention.ACTIVE);
					sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
			}
			
			if (EventDispatcher.getInstance().hasListener(EventType.ON_CREATURE_ATTACKED, target))
			{
				if (_onCreatureAttacked == null)
				{
					_onCreatureAttacked = new OnCreatureAttacked();
				}
				
				_onCreatureAttacked.setAttacker(this);
				_onCreatureAttacked.setTarget(target);
				final TerminateReturn attackedReturn = EventDispatcher.getInstance().notifyEvent(_onCreatureAttacked, target, TerminateReturn.class);
				if ((attackedReturn != null) && attackedReturn.terminate())
				{
					getAI().setIntention(Intention.ACTIVE);
					sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
			}
			
			if (!isAlikeDead())
			{
				if ((isNpc() && target.isAlikeDead()) || !isInSurroundingRegion(target))
				{
					getAI().setIntention(Intention.ACTIVE);
					sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
				else if (isPlayer())
				{
					if (target.isDead())
					{
						getAI().setIntention(Intention.ACTIVE);
						sendPacket(ActionFailed.STATIC_PACKET);
						return;
					}
					
					if (isTransformed() && !getTransformation().canAttack())
					{
						sendPacket(ActionFailed.STATIC_PACKET);
						return;
					}
				}
			}
			
			// Verifica se a arma do atacante pode atacar
			if (getActiveWeaponItem() != null)
			{
				final Weapon wpn = getActiveWeaponItem();
				if (!wpn.isAttackWeapon() && !isGM())
				{
					if (wpn.getItemType() == WeaponType.FISHINGROD)
					{
						sendPacket(SystemMessageId.YOU_LOOK_ODDLY_AT_THE_FISHING_POLE_IN_DISBELIEF_AND_REALIZE_THAT_YOU_CAN_T_ATTACK_ANYTHING_WITH_THIS);
					}
					else
					{
						sendPacket(SystemMessageId.THAT_WEAPON_CANNOT_PERFORM_ANY_ATTACKS);
					}
					
					sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
			}
			
			final Player player = asPlayer();
			if (player != null)
			{
				if (player.inObserverMode())
				{
					sendPacket(SystemMessageId.OBSERVERS_CANNOT_PARTICIPATE);
					sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
				else if (player.isSiegeFriend(target))
				{
					if (TerritoryWarManager.getInstance().isTWInProgress())
					{
						sendPacket(SystemMessageId.YOU_CANNOT_FORCE_ATTACK_A_MEMBER_OF_THE_SAME_TERRITORY);
					}
					else
					{
						sendPacket(SystemMessageId.FORCE_ATTACK_IS_IMPOSSIBLE_AGAINST_A_TEMPORARY_ALLIED_MEMBER_DURING_A_SIEGE);
					}
					
					sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
				// Verificando se o alvo se moveu para zona de paz
				else if (target.isInsidePeaceZone(player) && !isInTownWarEvent())
				{
					getAI().setIntention(Intention.ACTIVE);
					sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
			}
			else if (isInsidePeaceZone(this, target) && !isInTownWarEvent())
			{
				getAI().setIntention(Intention.ACTIVE);
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			
			stopEffectsOnAction();
			
			// Verificacao GeoData de Linha de Visao aqui (ou dz > 1000)
			if (!GeoData.getInstance().canSeeTarget(this, target))
			{
				sendPacket(SystemMessageId.CANNOT_SEE_TARGET);
				getAI().setIntention(Intention.ACTIVE);
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			
			// Mobius: Nao se mover quando o ataque e lancado.
			if (isMoving())
			{
				stopMove(getLocation());
			}
			
			final Weapon weaponItem = getActiveWeaponItem();
			final int timeAtk = calculateTimeBetweenAttacks();
			final int timeToHit = timeAtk / 2;
			final Attack attack = new Attack(this, target, isChargedShot(ShotType.SOULSHOTS), (weaponItem != null) ? weaponItem.getCrystalTypePlus().getLevel() : 0);
			setHeading(LocationUtil.calculateHeadingFrom(this, target));
			final int reuse = calculateReuseTime(weaponItem);
			final long currentTime = System.nanoTime();
			boolean hitted = false;
			switch (getAttackType())
			{
				case BOW:
				{
					if (!canUseRangeWeapon())
					{
						return;
					}
					
					_attackEndTime = currentTime + TimeUnit.MILLISECONDS.toNanos(timeToHit + (reuse / 2));
					hitted = doAttackHitByBow(attack, target, timeAtk, reuse);
					break;
				}
				case CROSSBOW:
				{
					if (!canUseRangeWeapon())
					{
						return;
					}
					
					_attackEndTime = currentTime + TimeUnit.MILLISECONDS.toNanos(timeToHit + (reuse / 2));
					hitted = doAttackHitByCrossBow(attack, target, timeAtk, reuse);
					break;
				}
				case POLE:
				{
					_attackEndTime = currentTime + TimeUnit.MILLISECONDS.toNanos(timeAtk);
					hitted = doAttackHitByPole(attack, target, timeToHit);
					break;
				}
				case FIST:
				{
					if (!isPlayer())
					{
						_attackEndTime = currentTime + TimeUnit.MILLISECONDS.toNanos(timeAtk);
						hitted = doAttackHitSimple(attack, target, timeToHit);
						break;
					}
					// Fallthrough.
				}
				case DUAL:
				case DUALFIST:
				case DUALDAGGER:
				{
					_attackEndTime = currentTime + TimeUnit.MILLISECONDS.toNanos(timeAtk);
					hitted = doAttackHitByDual(attack, target, timeToHit);
					break;
				}
				default:
				{
					_attackEndTime = currentTime + TimeUnit.MILLISECONDS.toNanos(timeAtk);
					hitted = doAttackHitSimple(attack, target, timeToHit);
					break;
				}
			}
			
			// Precaucao. Ja aconteceu no passado. Provavelmente impossivel de acontecer agora, mas nao vamos arriscar.
			if (_attackEndTime < currentTime)
			{
				_attackEndTime = currentTime + TimeUnit.MILLISECONDS.toNanos(Integer.MAX_VALUE);
			}
			
			// Marca o atacante se for um Player fora de area PvP
			if ((player != null) && !player.isInsideZone(ZoneId.PVP) && (player != target)) // Previne jogadores de serem marcados em Zonas PvP.
			{
				AttackStanceTaskManager.getInstance().addAttackStanceTask(player);
				if (player.getSummon() != target)
				{
					player.updatePvPStatus(target);
				}
			}
			
			// Verifica se o hit nao errou
			if (!hitted)
			{
				abortAttack(); // Aborta o ataque da Creature e envia pacote Server->Client ActionFailed
			}
			else
			{
				// Se nao erramos o hit, descarrega os soulshots, se houver
				setChargedShot(ShotType.SOULSHOTS, false);
				if (player != null)
				{
					if (player.isCursedWeaponEquipped())
					{
						// Se atingido por uma arma amaldicoada, CP e reduzido a 0
						if (!target.isInvul())
						{
							target.setCurrentCp(0);
						}
					}
					// Se uma arma amaldicoada e atingida por um Hero, CP e reduzido a 0
					else if (player.isHero() && target.isPlayer() && target.asPlayer().isCursedWeaponEquipped())
					{
						target.setCurrentCp(0);
					}
				}
			}
			
			// Se o pacote Server->Client Attack contiver pelo menos 1 hit, envia o pacote Server->Client Attack
			// para a Creature E para todos os Players em _KnownPlayers da Creature
			if (attack.hasHits())
			{
				broadcastPacket(attack);
			}
			
			// Notifica AI com READY_TO_ACT
			ThreadPool.schedule(new NotifyAITask(this, Action.READY_TO_ACT), timeAtk + reuse);
		}
		finally
		{
			_attackLock.unlockWrite(stamp);
		}
	}
	
	/**
	 * Executa um ataque de Arco.<br>
	 * <br>
	 * <b><u>Acoes</u>:</b>
	 * <ul>
	 * <li>Calcula se o hit errou ou nao</li>
	 * <li>Consome flechas</li>
	 * <li>Se o hit nao errou, calcula se a defesa de escudo e eficiente</li>
	 * <li>Se o hit nao errou, calcula se o hit e critico</li>
	 * <li>Se o hit nao errou, calcula danos fisicos</li>
	 * <li>Se a Creature for um Player, envia um pacote Server->Client SetupGauge</li>
	 * <li>Cria uma nova tarefa de hit com prioridade Media</li>
	 * <li>Calcula e define o delay de desabilitacao do arco em funcao da Velocidade de Ataque</li>
	 * <li>Adiciona este hit ao pacote Server-Client Attack</li>
	 * </ul>
	 * @param attack Pacote Server->Client Attack no qual o hit sera adicionado
	 * @param target A Creature alvo
	 * @param sAtk A Velocidade de Ataque do atacante
	 * @param reuse
	 * @return True se o hit nao errou
	 */
	private boolean doAttackHitByBow(Attack attack, Creature target, int sAtk, int reuse)
	{
		int damage1 = 0;
		byte shld1 = 0;
		boolean crit1 = false;
		
		// Calcula se o hit errou ou nao.
		final boolean miss1 = Formulas.calcHitMiss(this, target);
		
		// Consome flechas.
		reduceArrowCount(false);
		
		_move = null;
		
		// Verifica se o hit nao errou.
		if (!miss1)
		{
			// Calcula se a defesa de escudo e eficiente.
			shld1 = Formulas.calcShldUse(this, target);
			
			// Calcula se o hit e critico.
			crit1 = Formulas.calcCrit(this, target);
			
			// Calcula danos fisicos.
			damage1 = (int) Formulas.calcPhysDam(this, target, null, shld1, crit1, attack.hasSoulshot());
			
			// Formula de Dano a Distancia de Arcos (Dano diminui gradualmente quando 60% ou menos do alcance maximo, e aumenta quando 60% ou mais).
			// O alcance maximo e 500 que e o alcance base do arco, e 60% disso e 800.
			if (Config.CALCULATE_DISTANCE_BOW_DAMAGE)
			{
				damage1 *= (calculateDistance3D(target) / 4000) + 0.8;
			}
		}
		
		// Verifica se a Creature e um Player.
		if (isPlayer())
		{
			sendPacket(new SetupGauge(getObjectId(), SetupGauge.RED, sAtk + reuse));
		}
		
		// Cria uma nova tarefa de hit com prioridade Media.
		ThreadPool.schedule(new HitTask(this, target, damage1, crit1, miss1, shld1, attack.hasSoulshot(), true), sAtk);
		
		// Calcula e define o delay de desabilitacao do arco em funcao da Velocidade de Ataque.
		final int gameTime = GameTimeTaskManager.getInstance().getGameTicks();
		_disableBowAttackEndTime = gameTime + ((sAtk + reuse) / GameTimeTaskManager.MILLIS_IN_TICK);
		
		// Precaucao. Aconteceu no passado com _attackEndTime. Nao vamos arriscar.
		if (_disableBowAttackEndTime < gameTime)
		{
			_disableBowAttackEndTime = Integer.MAX_VALUE;
		}
		
		// Adiciona este hit ao pacote Server-Client Attack.
		attack.addHit(target, damage1, miss1, crit1, shld1);
		
		// Retorna true se o hit nao errou.
		return !miss1;
	}
	
	/**
	 * Executa um ataque de Besta.<br>
	 * <br>
	 * <b><u>Acoes</u>:</b>
	 * <ul>
	 * <li>Calcula se o hit errou ou nao</li>
	 * <li>Consome virotes</li>
	 * <li>Se o hit nao errou, calcula se a defesa de escudo e eficiente</li>
	 * <li>Se o hit nao errou, calcula se o hit e critico</li>
	 * <li>Se o hit nao errou, calcula danos fisicos</li>
	 * <li>Se a Creature for um Player, envia um pacote Server->Client SetupGauge</li>
	 * <li>Cria uma nova tarefa de hit com prioridade Media</li>
	 * <li>Calcula e define o delay de desabilitacao da besta em funcao da Velocidade de Ataque</li>
	 * <li>Adiciona este hit ao pacote Server-Client Attack</li><br>
	 * @param attack Pacote Server->Client Attack no qual o hit sera adicionado
	 * @param target A Creature alvo
	 * @param sAtk A Velocidade de Ataque do atacante
	 * @param reuse
	 * @return True se o hit nao errou
	 */
	private boolean doAttackHitByCrossBow(Attack attack, Creature target, int sAtk, int reuse)
	{
		int damage1 = 0;
		byte shld1 = 0;
		boolean crit1 = false;
		
		// Calcula se o hit errou ou nao.
		final boolean miss1 = Formulas.calcHitMiss(this, target);
		
		// Consome virotes.
		reduceArrowCount(true);
		
		_move = null;
		
		// Verifica se o hit nao errou.
		if (!miss1)
		{
			// Calcula se a defesa de escudo e eficiente.
			shld1 = Formulas.calcShldUse(this, target);
			
			// Calcula se o hit e critico.
			crit1 = Formulas.calcCrit(this, target);
			
			// Calcula danos fisicos.
			damage1 = (int) Formulas.calcPhysDam(this, target, null, shld1, crit1, attack.hasSoulshot());
		}
		
		// Verifica se a Creature e um Player.
		if (isPlayer())
		{
			// Envia uma mensagem de sistema.
			sendPacket(SystemMessageId.YOUR_CROSSBOW_IS_PREPARING_TO_FIRE);
			
			// Envia um pacote Server->Client SetupGauge.
			sendPacket(new SetupGauge(getObjectId(), SetupGauge.RED, sAtk + reuse));
		}
		
		// Cria uma nova tarefa de hit com prioridade Media.
		ThreadPool.schedule(new HitTask(this, target, damage1, crit1, miss1, shld1, attack.hasSoulshot(), true), sAtk);
		
		// Calcula e define o delay de desabilitacao da besta em funcao da Velocidade de Ataque.
		final int gameTime = GameTimeTaskManager.getInstance().getGameTicks();
		_disableBowAttackEndTime = gameTime + ((sAtk + reuse) / GameTimeTaskManager.MILLIS_IN_TICK);
		
		// Precaucao. Aconteceu no passado com _attackEndTime. Nao vamos arriscar.
		if (_disableBowAttackEndTime < gameTime)
		{
			_disableBowAttackEndTime = Integer.MAX_VALUE;
		}
		
		// Adiciona este hit ao pacote Server-Client Attack.
		attack.addHit(target, damage1, miss1, crit1, shld1);
		
		// Retorna true se o hit nao errou.
		return !miss1;
	}
	
	/**
	 * Executa um ataque Dual.<br>
	 * <br>
	 * <b><u>Acoes</u>:</b>
	 * <ul>
	 * <li>Calcula se os hits erraram ou nao</li>
	 * <li>Se os hits nao erraram, calcula se a defesa de escudo e eficiente</li>
	 * <li>Se os hits nao erraram, calcula se o hit e critico</li>
	 * <li>Se os hits nao erraram, calcula danos fisicos</li>
	 * <li>Cria 2 novas tarefas de hit com prioridade Media</li>
	 * <li>Adiciona esses hits ao pacote Server-Client Attack</li>
	 * </ul>
	 * @param attack Pacote Server->Client Attack no qual o hit sera adicionado
	 * @param target A Creature alvo
	 * @param sAtk
	 * @return True se o hit 1 ou hit 2 nao errou
	 */
	private boolean doAttackHitByDual(Attack attack, Creature target, int sAtk)
	{
		int damage1 = 0;
		int damage2 = 0;
		byte shld1 = 0;
		byte shld2 = 0;
		boolean crit1 = false;
		boolean crit2 = false;
		
		// Calcula se os hits erraram ou nao
		final boolean miss1 = Formulas.calcHitMiss(this, target);
		final boolean miss2 = Formulas.calcHitMiss(this, target);
		
		// Verifica se o hit 1 nao errou
		if (!miss1)
		{
			// Calcula se a defesa de escudo e eficiente contra o hit 1
			shld1 = Formulas.calcShldUse(this, target);
			
			// Calcula se o hit 1 e critico
			crit1 = Formulas.calcCrit(this, target);
			
			// Calcula danos fisicos do hit 1
			damage1 = (int) Formulas.calcPhysDam(this, target, null, shld1, crit1, attack.hasSoulshot());
			damage1 /= 2;
		}
		
		// Verifica se o hit 2 nao errou
		if (!miss2)
		{
			// Calcula se a defesa de escudo e eficiente contra o hit 2
			shld2 = Formulas.calcShldUse(this, target);
			
			// Calcula se o hit 2 e critico
			crit2 = Formulas.calcCrit(this, target);
			
			// Calcula danos fisicos do hit 2
			damage2 = (int) Formulas.calcPhysDam(this, target, null, shld2, crit2, attack.hasSoulshot());
			damage2 /= 2;
		}
		
		// Cria uma nova tarefa de hit com prioridade Media para o hit 1
		ThreadPool.schedule(new HitTask(this, target, damage1, crit1, miss1, shld1, attack.hasSoulshot(), true), sAtk / 2);
		
		// Cria uma nova tarefa de hit com prioridade Media para o hit 2 com delay maior
		ThreadPool.schedule(new HitTask(this, target, damage2, crit2, miss2, shld2, attack.hasSoulshot(), false), sAtk);
		
		// Adiciona estes hits ao pacote Server-Client Attack
		attack.addHit(target, damage1, miss1, crit1, shld1);
		attack.addHit(target, damage2, miss2, crit2, shld2);
		
		// Retorna true se o hit 1 ou hit 2 nao errou
		return !miss1 || !miss2;
	}
	
	/**
	 * Executa um ataque de Lanca.<br>
	 * <br>
	 * <b><u>Acoes</u>:</b>
	 * <ul>
	 * <li>Obtem todos os objetos visiveis em uma area esferica perto da Creature para obter alvos possiveis</li>
	 * <li>Se o alvo possivel e a Creature alvo, executa um ataque simples contra ela</li>
	 * <li>Se o alvo possivel nao e a Creature alvo mas e atacavel, executa um ataque simples contra ele</li>
	 * </ul>
	 * @param attack Pacote Server->Client Attack no qual o hit sera adicionado
	 * @param target
	 * @param sAtk
	 * @return True se um hit nao errou
	 */
	private boolean doAttackHitByPole(Attack attack, Creature target, int sAtk)
	{
		// Executa o hit no alvo principal.
		boolean hitted = doAttackHitSimple(attack, target, 100, sAtk, true);
		
		if (!isAffected(EffectFlag.POLEARM_SINGLE_TARGET))
		{
			// Sem Polearm Mastery (skill 216) o maximo de ataques simultaneos e 3 (1 por padrao + 2 na skill 3599).
			int attackCountMax = (int) _stat.calcStat(Stat.ATTACK_COUNT_MAX, 1, null, null);
			if (attackCountMax > 1)
			{
				final double headingAngle = LocationUtil.convertHeadingToDegree(getHeading());
				final int maxRadius = _stat.getPhysicalAttackRange();
				final int physicalAttackAngle = _stat.getPhysicalAttackAngle();
				double attackpercent = 85;
				for (Creature obj : World.getInstance().getVisibleObjectsInRange(this, Creature.class, maxRadius))
				{
					// Pula o alvo principal.
					
					// Pula alvo morto ou fingindo de morto.
					
					// Verifica se o alvo e auto atacavel.
					// Verifica se o alvo esta dentro do angulo de ataque.
					if ((obj == target) || obj.isAlikeDead() || !obj.isAutoAttackable(this) || (Math.abs(calculateDirectionTo(obj) - headingAngle) > physicalAttackAngle))
					{
						continue;
					}
					
					if (obj.isPet() && isPlayer() && (obj.asPet().getOwner() == asPlayer()))
					{
						continue;
					}
					
					if (isAttackable() && obj.isPlayer() && _target.isAttackable())
					{
						continue;
					}
					
					if (isAttackable() && obj.isAttackable() && !asAttackable().isChaos())
					{
						continue;
					}
					
					// Executa um ataque simples contra o alvo adicional.
					hitted |= doAttackHitSimple(attack, obj, attackpercent, sAtk, false);
					attackpercent /= 1.15;
					if (--attackCountMax <= 0)
					{
						break;
					}
				}
			}
		}
		
		// Retorna true se um hit nao errou
		return hitted;
	}
	
	/**
	 * Executa um ataque simples.<br>
	 * <br>
	 * <b><u>Acoes</u>:</b>
	 * <ul>
	 * <li>Calcula se o hit errou ou nao</li>
	 * <li>Se o hit nao errou, calcula se a defesa de escudo e eficiente</li>
	 * <li>Se o hit nao errou, calcula se o hit e critico</li>
	 * <li>Se o hit nao errou, calcula danos fisicos</li>
	 * <li>Cria uma nova tarefa de hit com prioridade Media</li>
	 * <li>Adiciona este hit ao pacote Server-Client Attack</li>
	 * </ul>
	 * @param attack Pacote Server->Client Attack no qual o hit sera adicionado
	 * @param target A Creature alvo
	 * @param sAtk
	 * @return True se o hit nao errou
	 */
	private boolean doAttackHitSimple(Attack attack, Creature target, int sAtk)
	{
		return doAttackHitSimple(attack, target, 100, sAtk, true);
	}
	
	private boolean doAttackHitSimple(Attack attack, Creature target, double attackpercent, int sAtk, boolean rechargeShots)
	{
		int damage1 = 0;
		byte shld1 = 0;
		boolean crit1 = false;
		
		// Calcula se o hit errou ou nao
		final boolean miss1 = Formulas.calcHitMiss(this, target);
		
		// Verifica se o hit nao errou
		if (!miss1)
		{
			// Calcula se a defesa de escudo e eficiente
			shld1 = Formulas.calcShldUse(this, target);
			
			// Calcula se o hit e critico
			crit1 = Formulas.calcCrit(this, target);
			
			// Calcula danos fisicos
			damage1 = (int) Formulas.calcPhysDam(this, target, null, shld1, crit1, attack.hasSoulshot());
			if (attackpercent != 100)
			{
				damage1 = (int) ((damage1 * attackpercent) / 100);
			}
		}
		
		// Cria uma nova tarefa de hit com prioridade Media
		ThreadPool.schedule(new HitTask(this, target, damage1, crit1, miss1, shld1, attack.hasSoulshot(), rechargeShots), sAtk);
		
		// Adiciona este hit ao pacote Server-Client Attack
		attack.addHit(target, damage1, miss1, crit1, shld1);
		
		// Retorna true se o hit nao errou
		return !miss1;
	}
	
	/**
	 * Gerencia a tarefa de conjuracao (tempo de conjuracao e interrupcao, delay de reuso...) e exibe a barra de conjuracao e animacao no cliente.<br>
	 * <br>
	 * <b><u>Acoes</u>:</b>
	 * <ul>
	 * <li>Verifica a possibilidade da conjuracao: skill e uma magia, conjurador nao esta silenciado...</li>
	 * <li>Obtem a lista de todos os alvos (ex: efeitos em area) e define a Creature alvo (suas estatisticas serao usadas no calculo)</li>
	 * <li>Calcula o tempo de conjuracao (base + modificador de MAtkSpd), tempo de interrupcao e delay de reuso</li>
	 * <li>Envia um pacote Server->Client MagicSkillUser (para exibir animacao de conjuracao), um pacote SetupGauge (para exibir barra de conjuracao) e uma mensagem de sistema</li>
	 * <li>Desabilita todas as skills durante o tempo de conjuracao (cria uma tarefa EnableAllSkills)</li>
	 * <li>Desabilita a skill durante o delay de reuso (cria uma tarefa EnableSkill)</li>
	 * <li>Cria uma tarefa MagicUseTask (que chamara o metodo onMagicUseTimer) para executar a Skill Magica ao final do tempo de conjuracao</li>
	 * </ul>
	 * @param skill A Skill a ser usada
	 */
	public void doCast(Skill skill)
	{
		beginCast(skill, false);
	}
	
	public void doSimultaneousCast(Skill skill)
	{
		beginCast(skill, true);
	}
	
	public void doCast(Skill skill, Creature target, List<WorldObject> targets)
	{
		if (!checkDoCastConditions(skill))
		{
			setCastingNow(false);
			return;
		}
		
		// Override casting type
		if (skill.isSimultaneousCast())
		{
			doSimultaneousCast(skill, target, targets);
			return;
		}
		
		stopEffectsOnAction();
		
		// Recharge AutoSoulShot
		// this method should not used with Playable
		beginCast(skill, false, target, targets);
	}
	
	public void doSimultaneousCast(Skill skill, Creature target, List<WorldObject> targets)
	{
		if (!checkDoCastConditions(skill))
		{
			setCastingSimultaneouslyNow(false);
			return;
		}
		
		stopEffectsOnAction();
		
		beginCast(skill, true, target, targets);
	}
	
	private void beginCast(Skill skill, boolean isSimultaneous)
	{
		// Atacaveis nao podem conjurar enquanto se movem.
		if (isAttackable() && isMoving())
		{
			return;
		}
		
		if (!checkDoCastConditions(skill))
		{
			if (isSimultaneous)
			{
				setCastingSimultaneouslyNow(false);
			}
			else
			{
				setCastingNow(false);
			}
			
			if (isPlayer())
			{
				getAI().setIntention(Intention.ACTIVE);
			}
			return;
		}
		
		// Sobrescreve o tipo de conjuracao
		boolean simultaneously = isSimultaneous;
		if (skill.isSimultaneousCast() && !simultaneously)
		{
			simultaneously = true;
		}
		
		stopEffectsOnAction();
		
		// Define o alvo da skill em funcao do Tipo de Skill e Tipo de Alvo
		Creature target = null;
		
		// Obtem todos os alvos possiveis da skill em uma tabela em funcao do tipo de alvo da skill
		final List<WorldObject> targets = skill.getTargetList(this);
		boolean doit = false;
		
		// Skills AURA devem sempre usar o conjurador como alvo
		switch (skill.getTargetType())
		{
			case AREA_SUMMON: // Precisamos disso para corrigir a direcao
			{
				target = isPlayer() ? asPlayer().getSummon() : null;
				break;
			}
			case AURA:
			case AURA_CORPSE_MOB:
			case FRONT_AURA:
			case BEHIND_AURA:
			case GROUND:
			case AURA_FRIENDLY:
			{
				target = this;
				break;
			}
			case SELF:
			case PET:
			case SERVITOR:
			case SUMMON:
			case OWNER_PET:
			case PARTY:
			case CLAN:
			case PARTY_CLAN:
			case COMMAND_CHANNEL:
			{
				doit = true;
				// Fallthrough.
			}
			default:
			{
				if (targets.isEmpty())
				{
					if (simultaneously)
					{
						setCastingSimultaneouslyNow(false);
					}
					else
					{
						setCastingNow(false);
					}
					
					// Envia um pacote Server->Client ActionFailed para o Player
					if (isPlayer())
					{
						sendPacket(ActionFailed.STATIC_PACKET);
						getAI().setIntention(Intention.ACTIVE);
					}
					
					return;
				}
				
				if ((skill.isContinuous() && !skill.isDebuff()) || skill.hasEffectType(EffectType.CPHEAL, EffectType.HEAL))
				{
					doit = true;
				}
				
				if (doit)
				{
					target = targets.get(0).asCreature();
				}
				else
				{
					target = _target != null ? _target.asCreature() : null;
				}
			}
		}
		
		beginCast(skill, simultaneously, target, targets);
	}
	
	private void beginCast(Skill skill, boolean simultaneously, Creature target, List<WorldObject> targets)
	{
		if (target == null)
		{
			if (simultaneously)
			{
				setCastingSimultaneouslyNow(false);
			}
			else
			{
				setCastingNow(false);
			}
			
			if (isPlayer())
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				getAI().setIntention(Intention.ACTIVE);
			}
			
			return;
		}
		
		if (EventDispatcher.getInstance().hasListener(EventType.ON_CREATURE_SKILL_USE, this))
		{
			if (_onCreatureSkillUse == null)
			{
				_onCreatureSkillUse = new OnCreatureSkillUse();
			}
			
			_onCreatureSkillUse.setCaster(this);
			_onCreatureSkillUse.setSkill(skill);
			_onCreatureSkillUse.setSimultaneously(simultaneously);
			_onCreatureSkillUse.setTarget(target);
			_onCreatureSkillUse.setTargets(targets);
			final TerminateReturn term = EventDispatcher.getInstance().notifyEvent(_onCreatureSkillUse, this, TerminateReturn.class);
			if ((term != null) && term.terminate())
			{
				if (simultaneously)
				{
					setCastingSimultaneouslyNow(false);
				}
				else
				{
					setCastingNow(false);
				}
				
				if (isPlayer())
				{
					sendPacket(ActionFailed.STATIC_PACKET);
					getAI().setIntention(Intention.ACTIVE);
				}
				
				return;
			}
		}
		
		// TODO: Remover hardcode usando event listeners!
		if (skill.hasEffectType(EffectType.RESURRECTION) && (isResurrectionBlocked() || target.isResurrectionBlocked()))
		{
			sendPacket(SystemMessageId.REJECT_RESURRECTION); // Rejeitar ressurreicao
			target.sendPacket(SystemMessageId.REJECT_RESURRECTION); // Rejeitar ressurreicao
			if (simultaneously)
			{
				setCastingSimultaneouslyNow(false);
			}
			else
			{
				setCastingNow(false);
			}
			
			if (isPlayer())
			{
				getAI().setIntention(Intention.ACTIVE);
				sendPacket(ActionFailed.STATIC_PACKET);
			}
			
			return;
		}
		
		// Obtem o Tempo Base de Conjuracao das Skills.
		int skillTime = (skill.getHitTime() + skill.getCoolTime());
		if (!skill.isChanneling() || (skill.getChannelingSkillId() == 0))
		{
			// Calcula o Tempo de Conjuracao das Skills "Nao-Estaticas" (com PAtk/MAtkSpd do conjurador).
			if (!skill.isStatic())
			{
				skillTime = Formulas.calcAtkSpd(this, skill, skillTime);
			}
			
			// Calcula o Tempo de Conjuracao de Skills Magicas (reduzido em 40% se usando SPS/BSPS)
			if (skill.isMagic() && (isChargedShot(ShotType.SPIRITSHOTS) || isChargedShot(ShotType.BLESSED_SPIRITSHOTS)))
			{
				skillTime = (int) (0.6 * skillTime);
			}
		}
		
		// Evita Animacao de Conjuracao quebrada.
		// Cliente nao consegue lidar com menos de 550ms de Animacao de Conjuracao em Skills Magicas com mais de 550ms de base.
		if (skill.isMagic() && ((skill.getHitTime() + skill.getCoolTime()) > 550) && (skillTime < 550))
		{
			skillTime = 550;
		}
		// Cliente nao consegue lidar com menos de 500ms de Animacao de Conjuracao em Skills Fisicas com 500ms de base ou mais.
		else if (!skill.isStatic() && ((skill.getHitTime() + skill.getCoolTime()) >= 500) && (skillTime < 500))
		{
			skillTime = 500;
		}
		
		// enfileira ervas e pocoes
		if (_isCastingSimultaneouslyNow && simultaneously)
		{
			ThreadPool.schedule(() -> beginCast(skill, simultaneously, target, targets), 100);
			return;
		}
		
		// Define o _castInterruptTime e status de conjuracao (Player ja tem isso como true)
		if (simultaneously)
		{
			setCastingSimultaneouslyNow(true);
		}
		else
		{
			setCastingNow(true);
		}
		
		if (!simultaneously)
		{
			_castInterruptTime = -2 + GameTimeTaskManager.getInstance().getGameTicks() + (skillTime / GameTimeTaskManager.MILLIS_IN_TICK);
			setLastSkillCast(skill);
		}
		else
		{
			setLastSimultaneousSkillCast(skill);
		}
		
		// Calcula o Tempo de Reuso da Skill
		int reuseDelay;
		if (skill.isStaticReuse() || skill.isStatic())
		{
			reuseDelay = (skill.getReuseDelay());
		}
		else if (skill.isMagic())
		{
			reuseDelay = (int) (skill.getReuseDelay() * calcStat(Stat.MAGIC_REUSE_RATE, 1, null, null));
		}
		else if (skill.isPhysical())
		{
			reuseDelay = (int) (skill.getReuseDelay() * calcStat(Stat.P_REUSE, 1, null, null));
		}
		else
		{
			reuseDelay = (int) (skill.getReuseDelay() * calcStat(Stat.DANCE_REUSE, 1, null, null));
		}
		
		// Verifica se esta skill consome mp ao iniciar a conjuracao
		final int initmpcons = _stat.getMpInitialConsume(skill);
		if (initmpcons > 0)
		{
			_status.reduceMp(initmpcons);
			final StatusUpdate su = new StatusUpdate(this);
			su.addAttribute(StatusUpdate.CUR_MP, (int) _status.getCurrentMp());
			sendPacket(su);
		}
		
		// Desabilita a skill durante o delay de reuso e cria uma tarefa EnableSkill com prioridade Media para habilita-la ao final do delay de reuso
		if (reuseDelay > 10)
		{
			if (Formulas.calcSkillMastery(this, skill))
			{
				reuseDelay = 100;
				if (isPlayable())
				{
					sendPacket(SystemMessageId.A_SKILL_IS_READY_TO_BE_USED_AGAIN);
				}
			}
			
			if (reuseDelay > 1000)
			{
				addTimeStamp(skill, reuseDelay);
			}
			else
			{
				disableSkill(skill, reuseDelay);
			}
		}
		
		// Garante que o personagem esta virado para o alvo selecionado
		if (target != this)
		{
			setHeading(LocationUtil.calculateHeadingFrom(this, target));
			broadcastPacket(new ExRotation(getObjectId(), getHeading()));
		}
		
		if (isPlayable())
		{
			if ((skill.getItemConsumeId() > 0) && !destroyItemByItemId(null, skill.getItemConsumeId(), skill.getItemConsumeCount(), null, true))
			{
				sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT_2);
				abortCast();
				return;
			}
			
			// Reduz mana do talisma ao usar skill.
			if ((skill.getReferenceItemId() > 0) && (ItemData.getInstance().getTemplate(skill.getReferenceItemId()).getBodyPart() == ItemTemplate.SLOT_DECO))
			{
				for (Item item : getInventory().getAllItemsByItemId(skill.getReferenceItemId()))
				{
					if (item.isEquipped())
					{
						if (item.getMana() < item.useSkillDisTime())
						{
							abortCast();
							return;
						}
						
						item.decreaseMana(false, item.useSkillDisTime());
						break;
					}
				}
			}
		}
		
		// Transmite efeito de voo se necessario.
		if (skill.getFlyType() != null)
		{
			broadcastPacket(new FlyToLocation(this, target, skill.getFlyType()));
			setXYZ(target.getX(), target.getY(), target.getZ());
		}
		
		if (!skill.isToggle())
		{
			// Envia um pacote Server->Client MagicSkillUser com alvo, displayId, level, skillTime, reuseDelay
			// para a Creature E para todos os Players em _KnownPlayers da Creature
			broadcastPacket(new MagicSkillUse(this, target, skill.getDisplayId(), skill.getDisplayLevel(), skillTime, reuseDelay));
			broadcastPacket(new MagicSkillLaunched(this, skill.getDisplayId(), skill.getDisplayLevel(), targets));
		}
		
		// Envia uma mensagem de sistema para o jogador.
		if (isPlayer() && !skill.isAbnormalInstant())
		{
			SystemMessage sm = null;
			switch (skill.getId())
			{
				case 1312: // Pesca
				{
					// Feito em Player.startFishing()
					break;
				}
				case 2046: // Coleira de Lobo
				{
					sm = new SystemMessage(SystemMessageId.SUMMONING_YOUR_PET);
					break;
				}
				default:
				{
					sm = new SystemMessage(SystemMessageId.USE_S1);
					sm.addSkillName(skill);
					break;
				}
			}
			
			if (sm != null)
			{
				sendPacket(sm);
			}
		}
		
		if (skill.hasEffects(EffectScope.START))
		{
			skill.applyEffectScope(EffectScope.START, new BuffInfo(this, target, skill), true, false);
		}
		
		final MagicUseTask mut = new MagicUseTask(this, targets, skill, skillTime, simultaneously);
		
		// executa a magia em skillTime milissegundos
		if (skillTime > 0)
		{
			// Envia um pacote Server->Client SetupGauge com a cor do medidor e o tempo de conjuracao
			if (isPlayer() && !simultaneously)
			{
				sendPacket(new SetupGauge(getObjectId(), SetupGauge.BLUE, skillTime));
			}
			
			if (skill.isChanneling() && (skill.getChannelingSkillId() > 0))
			{
				getSkillChannelizer().startChanneling(skill);
			}
			
			if (simultaneously)
			{
				final Future<?> future = _skillCast2;
				if (future != null)
				{
					future.cancel(true);
					_skillCast2 = null;
				}
				
				// Cria uma tarefa MagicUseTask para executar a MagicSkill ao final do tempo de conjuracao (skillTime)
				// Por razoes de animacao do cliente (buffs de grupo especialmente) 400 ms antes!
				_skillCast2 = ThreadPool.schedule(mut, Math.max(0, skillTime - 400));
			}
			else
			{
				final Future<?> future = _skillCast;
				if (future != null)
				{
					future.cancel(true);
					_skillCast = null;
				}
				
				// Cria uma tarefa MagicUseTask para executar a MagicSkill ao final do tempo de conjuracao (skillTime)
				// Por razoes de animacao do cliente (buffs de grupo especialmente) 400 ms antes!
				_skillCast = ThreadPool.schedule(mut, Math.max(0, skillTime - 400));
			}
		}
		else
		{
			mut.setSkillTime(0);
			onMagicLaunchedTimer(mut);
		}
	}
	
	/**
	 * Verifica se a conjuracao da skill e possivel
	 * @param skill
	 * @return True se a conjuracao e possivel
	 */
	public boolean checkDoCastConditions(Skill skill)
	{
		if ((skill == null) || isSkillDisabled(skill) || ((skill.getFlyType() == FlyType.CHARGE) && isMovementDisabled()))
		{
			// Envia um pacote Server->Client ActionFailed para o Player
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		// Verifica se o conjurador tem MP suficiente
		if (_status.getCurrentMp() < (_stat.getMpConsume(skill) + _stat.getMpInitialConsume(skill)))
		{
			// Envia uma Mensagem de Sistema para o conjurador
			sendPacket(SystemMessageId.NOT_ENOUGH_MP);
			
			// Envia um pacote Server->Client ActionFailed para o Player
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		// Verifica se o conjurador tem HP suficiente
		if (_status.getCurrentHp() <= skill.getHpConsume())
		{
			// Envia uma Mensagem de Sistema para o conjurador
			sendPacket(SystemMessageId.NOT_ENOUGH_HP);
			
			// Envia um pacote Server->Client ActionFailed para o Player
			sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		// Verificacoes de silenciamento de skill.
		if (!skill.isStatic())
		{
			// Verifica se a skill e uma magia e se a Creature nao esta silenciada
			if (skill.isMagic())
			{
				if (isMuted())
				{
					// Envia um pacote Server->Client ActionFailed para o Player
					sendPacket(ActionFailed.STATIC_PACKET);
					return false;
				}
			}
			else
			{
				// Verifica se a skill e fisica e se a Creature nao esta silenciada fisicamente
				if (isPhysicalMuted())
				{
					// Envia um pacote Server->Client ActionFailed para o Player
					sendPacket(ActionFailed.STATIC_PACKET);
					return false;
				}
			}
		}
		
		// previne conjuracao de signets em zona de paz
		if (skill.isChanneling() && (skill.getChannelingSkillId() > 0) && (getInstanceId() == 0) && !isInTownWarEvent())
		{
			final ZoneRegion zoneRegion = ZoneManager.getInstance().getRegion(this);
			boolean canCast = true;
			if ((skill.getTargetType() == TargetType.GROUND) && isPlayer())
			{
				final Location wp = asPlayer().getCurrentSkillWorldPosition();
				if (!zoneRegion.checkEffectRangeInsidePeaceZone(skill, wp.getX(), wp.getY(), wp.getZ()))
				{
					canCast = false;
				}
			}
			else if (!zoneRegion.checkEffectRangeInsidePeaceZone(skill, getX(), getY(), getZ()))
			{
				canCast = false;
			}
			
			if (!canCast)
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED_DUE_TO_UNSUITABLE_TERMS);
				sm.addSkillName(skill);
				sendPacket(sm);
				return false;
			}
		}
		
		// Verifica se a arma do conjurador e limitada a usar apenas suas proprias skills
		if (getActiveWeaponItem() != null)
		{
			final Weapon wep = getActiveWeaponItem();
			if (wep.useWeaponSkillsOnly() && !isGM() && wep.hasSkills())
			{
				boolean found = false;
				for (SkillHolder sh : wep.getSkills())
				{
					if (sh.getSkillId() == skill.getId())
					{
						found = true;
					}
				}
				
				if (!found)
				{
					if (asPlayer() != null)
					{
						sendPacket(SystemMessageId.THAT_WEAPON_CANNOT_USE_ANY_OTHER_SKILL_EXCEPT_THE_WEAPON_S_SKILL);
					}
					
					return false;
				}
			}
		}
		
		// Verifica se a magia consome um Item
		// TODO: combinar verificacao e consumo
		if ((skill.getItemConsumeId() > 0) && (getInventory() != null))
		{
			// Obtem o Item consumido pela magia
			final Item requiredItems = getInventory().getItemByItemId(skill.getItemConsumeId());
			
			// Verifica se o conjurador possui Items consumidos suficientes para conjurar
			if ((requiredItems == null) || (requiredItems.getCount() < skill.getItemConsumeCount()))
			{
				// Verificado: quando uma skill de summon falha, servidor mostra a quantidade de item consumido necessaria
				if (skill.hasEffectType(EffectType.SUMMON))
				{
					final SystemMessage sm = new SystemMessage(SystemMessageId.SUMMONING_A_SERVITOR_COSTS_S2_S1);
					sm.addItemName(skill.getItemConsumeId());
					sm.addInt(skill.getItemConsumeCount());
					sendPacket(sm);
				}
				else
				{
					// Envia uma Mensagem de Sistema para o conjurador
					sendPacket(SystemMessageId.THERE_ARE_NOT_ENOUGH_NECESSARY_ITEMS_TO_USE_THE_SKILL);
				}
				
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Obtem o mapa de timestamps de reuso de itens.
	 * @return o mapa de timestamps de reuso de itens
	 */
	public Map<Integer, TimeStamp> getItemReuseTimeStamps()
	{
		return _reuseTimeStampsItems;
	}
	
	/**
	 * Adiciona um timestamp de reuso de item.
	 * @param item o item
	 * @param reuse o reuso
	 */
	public void addTimeStampItem(Item item, long reuse)
	{
		addTimeStampItem(item, reuse, -1);
	}
	
	/**
	 * Adiciona um timestamp de reuso de item.<br>
	 * Usado para propositos de restauracao.
	 * @param item o item
	 * @param reuse o reuso
	 * @param systime o tempo do sistema
	 */
	public void addTimeStampItem(Item item, long reuse, long systime)
	{
		_reuseTimeStampsItems.put(item.getObjectId(), new TimeStamp(item, reuse, systime));
	}
	
	/**
	 * Obtem o tempo de reuso restante do item para um dado ID de objeto de item.
	 * @param itemObjId o ID do objeto do item
	 * @return se o item tiver um timestamp de reuso, o tempo restante, caso contrario -1
	 */
	public long getItemRemainingReuseTime(int itemObjId)
	{
		final TimeStamp reuseStamp = _reuseTimeStampsItems.get(itemObjId);
		return reuseStamp != null ? reuseStamp.getRemaining() : -1;
	}
	
	/**
	 * Obtem o delay de reuso restante para um dado grupo de reuso compartilhado de itens.
	 * @param group o grupo de reuso compartilhado de itens
	 * @return se o grupo de reuso compartilhado tiver um timestamp de reuso, o tempo restante, caso contrario -1
	 */
	public long getReuseDelayOnGroup(int group)
	{
		if ((group > 0) && !_reuseTimeStampsItems.isEmpty())
		{
			final long currentTime = System.currentTimeMillis();
			for (TimeStamp ts : _reuseTimeStampsItems.values())
			{
				if (ts.getSharedReuseGroup() == group)
				{
					final long stamp = ts.getStamp();
					if (currentTime < stamp)
					{
						return Math.max(stamp - currentTime, 0);
					}
				}
			}
		}
		
		return -1;
	}
	
	/**
	 * Obtem o mapa de timestamps de reuso de skills.
	 * @return o mapa de timestamps de reuso de skills
	 */
	public Map<Integer, TimeStamp> getSkillReuseTimeStamps()
	{
		return _reuseTimeStampsSkills;
	}
	
	/**
	 * Adiciona o timestamp de reuso da skill.
	 * @param skill a skill
	 * @param reuse o delay
	 */
	public void addTimeStamp(Skill skill, long reuse)
	{
		addTimeStamp(skill, reuse, -1);
	}
	
	/**
	 * Adiciona o timestamp de reuso da skill.<br>
	 * Usado para propositos de restauracao.
	 * @param skill a skill
	 * @param reuse o reuso
	 * @param systime o tempo do sistema
	 */
	public void addTimeStamp(Skill skill, long reuse, long systime)
	{
		_reuseTimeStampsSkills.put(skill.getReuseHashCode(), new TimeStamp(skill, reuse, systime));
	}
	
	/**
	 * Remove um timestamp de reuso de skill.
	 * @param skill a skill a ser removida
	 */
	public void removeTimeStamp(Skill skill)
	{
		_reuseTimeStampsSkills.remove(skill.getReuseHashCode());
	}
	
	/**
	 * Remove todos os timestamps de reuso de skills.
	 */
	public void resetTimeStamps()
	{
		_reuseTimeStampsSkills.clear();
	}
	
	/**
	 * Obtem o tempo de reuso restante da skill para um dado hash code de skill.
	 * @param hashCode o hash code da skill
	 * @return se a skill tiver um timestamp de reuso, o tempo restante, caso contrario -1
	 */
	public long getSkillRemainingReuseTime(int hashCode)
	{
		final TimeStamp reuseStamp = _reuseTimeStampsSkills.get(hashCode);
		return reuseStamp != null ? reuseStamp.getRemaining() : -1;
	}
	
	/**
	 * Verifica se a skill esta sob tempo de reuso.
	 * @param hashCode o hash code da skill
	 * @return {@code true} se a skill esta sob tempo de reuso, {@code false} caso contrario
	 */
	public boolean hasSkillReuse(int hashCode)
	{
		final TimeStamp reuseStamp = _reuseTimeStampsSkills.get(hashCode);
		return (reuseStamp != null) && reuseStamp.hasNotPassed();
	}
	
	/**
	 * Obtem o timestamp de reuso da skill.
	 * @param hashCode o hash code da skill
	 * @return se a skill tiver um timestamp de reuso, o timestamp de reuso da skill, caso contrario {@code null}
	 */
	public TimeStamp getSkillReuseTimeStamp(int hashCode)
	{
		return _reuseTimeStampsSkills.get(hashCode);
	}
	
	/**
	 * Obtem o mapa de skills desabilitadas.
	 * @return o mapa de skills desabilitadas
	 */
	public Map<Integer, Long> getDisabledSkills()
	{
		return _disabledSkills;
	}
	
	/**
	 * Habilita uma skill.
	 * @param skill a skill a ser habilitada
	 */
	public void enableSkill(Skill skill)
	{
		if (skill == null)
		{
			return;
		}
		
		_disabledSkills.remove(skill.getReuseHashCode());
	}
	
	/**
	 * Desabilita uma skill por um tempo determinado.<br>
	 * Se o delay for menor ou igual a zero, a skill sera desabilitada "para sempre".
	 * @param skill a skill a ser desabilitada
	 * @param delay delay em milissegundos
	 */
	public void disableSkill(Skill skill, long delay)
	{
		if (skill == null)
		{
			return;
		}
		
		_disabledSkills.put(skill.getReuseHashCode(), delay > 0 ? System.currentTimeMillis() + delay : Long.MAX_VALUE);
	}
	
	/**
	 * Remove todas as skills desabilitadas.
	 */
	public void resetDisabledSkills()
	{
		_disabledSkills.clear();
	}
	
	/**
	 * Verifica se a skill esta desabilitada.
	 * @param skill a skill
	 * @return {@code true} se a skill esta desabilitada, {@code false} caso contrario
	 */
	public boolean isSkillDisabled(Skill skill)
	{
		if (skill == null)
		{
			return false;
		}
		
		if (isAllSkillsDisabled())
		{
			return true;
		}
		
		final int hashCode = skill.getReuseHashCode();
		if (hasSkillReuse(hashCode))
		{
			return true;
		}
		
		if (_disabledSkills.isEmpty())
		{
			return false;
		}
		
		final Long stamp = _disabledSkills.get(hashCode);
		if (stamp == null)
		{
			return false;
		}
		
		if (stamp < System.currentTimeMillis())
		{
			_disabledSkills.remove(hashCode);
			return false;
		}
		
		return true;
	}
	
	/**
	 * Desabilita todas as skills.
	 */
	public void disableAllSkills()
	{
		_allSkillsDisabled = true;
	}
	
	/**
	 * Habilita todas as skills, exceto aquelas sob tempo de reuso ou previamente desabilitadas.
	 */
	public void enableAllSkills()
	{
		_allSkillsDisabled = false;
	}
	
	/**
	 * Mata a Creature.<br>
	 * <br>
	 * <b><u>Acoes</u>:</b>
	 * <ul>
	 * <li>Define alvo como null e cancela Ataque ou Conjuracao</li>
	 * <li>Para o movimento</li>
	 * <li>Para a tarefa de Regeneracao de HP/MP/CP</li>
	 * <li>Para todos os efeitos de skills ativos em progresso na Creature</li>
	 * <li>Envia o pacote Server->Client StatusUpdate com HP e MP atuais para todos os outros Players para informar</li>
	 * <li>Notifica a AI da Creature</li>
	 * </ul>
	 * @param killer A Creature que a matou
	 * @return false se o jogador ja estiver morto.
	 */
	public boolean doDie(Creature killer)
	{
		if (Config.ENABLE_CAPTCHA)
		{
			CaptchaManager.getInstance().updateCounter(killer, this);
		}
		
		// matar so e possivel uma vez
		synchronized (this)
		{
			if (_isDead)
			{
				return false;
			}
			
			// agora reseta currentHp para zero
			setCurrentHp(0);
			setDead(true);
		}
		
		if (EventDispatcher.getInstance().hasListener(EventType.ON_CREATURE_DEATH, this))
		{
			EventDispatcher.getInstance().notifyEvent(new OnCreatureDeath(killer, this), this);
		}
		
		if (EventDispatcher.getInstance().hasListener(EventType.ON_CREATURE_KILLED, this))
		{
			final TerminateReturn returnBack = EventDispatcher.getInstance().notifyEvent(new OnCreatureKilled(killer, this), this, TerminateReturn.class);
			if ((returnBack != null) && returnBack.terminate())
			{
				return false;
			}
		}
		
		// Calcula recompensas para o causador de dano principal.
		final Creature mainDamageDealer = isMonster() ? asMonster().getMainDamageDealer() : null;
		calculateRewards(mainDamageDealer != null ? mainDamageDealer : killer);
		
		// Define alvo como null e cancela Ataque ou Conjuracao
		setTarget(null);
		
		// Para o movimento
		stopMove(null);
		
		// Para a tarefa de Regeneracao de HP/MP/CP
		_status.stopHpMpRegeneration();
		
		if (isAttackable())
		{
			final Spawn spawn = asNpc().getSpawn();
			if ((spawn != null) && spawn.isRespawnEnabled())
			{
				stopAllEffects();
			}
			else
			{
				_effectList.stopAllEffectsWithoutExclusions(true, true);
			}
			
			// Ajuda de cla por alcance de aggro ao matar.
			if ((killer != null) && killer.isPlayable() && !killer.asPlayer().isGM())
			{
				final NpcTemplate template = asAttackable().getTemplate();
				final Set<Integer> clans = template.getClans();
				if ((clans != null) && !clans.isEmpty())
				{
					World.getInstance().forEachVisibleObjectInRange(this, Attackable.class, template.getClanHelpRange(), called ->
					{
						// Nao chama npcs mortos, npcs sem ai ou npcs que estao muito longe.
						if (called.isDead() || !called.hasAI() || (Math.abs(killer.getZ() - called.getZ()) > 600))
						{
							return;
						}
						
						// Nao chama npcs que ja estao realizando alguma acao (ex: atacando, conjurando).
						// Nao chama npcs que nao sao do mesmo cla.
						if (((called.getAI().getIntention() != Intention.IDLE) && (called.getAI().getIntention() != Intention.ACTIVE)) || !template.isClan(called.getTemplate().getClans()))
						{
							return;
						}
						
						// Por padrao, quando um membro da faccao pede ajuda, ataca o atacante do que chamou.
						called.getAI().notifyAction(Action.AGGRESSION, killer, 1);
						
						if (EventDispatcher.getInstance().hasListener(EventType.ON_ATTACKABLE_FACTION_CALL, called))
						{
							EventDispatcher.getInstance().notifyEventAsync(new OnAttackableFactionCall(called, asAttackable(), killer.asPlayer(), killer.isSummon()), called);
						}
					});
				}
			}
		}
		else
		{
			stopAllEffectsExceptThoseThatLastThroughDeath();
		}
		
		// Envia o pacote Server->Client StatusUpdate com HP e MP atuais para todos os outros Players para informar
		broadcastStatusUpdate();
		
		// Notifica a AI da Creature
		if (hasAI())
		{
			getAI().notifyAction(Action.DEATH);
		}
		
		ZoneManager.getInstance().getRegion(this).onDeath(this);
		
		getAttackByList().clear();
		
		if (isChannelized())
		{
			getSkillChannelized().abortChannelization();
		}
		
		// Custom boss announcements configuration.
		if (this instanceof GrandBoss)
		{
			if (Config.GRANDBOSS_DEFEAT_ANNOUNCEMENTS && ((getInstanceId() == 0) || Config.GRANDBOSS_INSTANCE_ANNOUNCEMENTS) && !isMinion() && !isRaidMinion())
			{
				final String name = NpcData.getInstance().getTemplate(getId()).getName();
				if ((name != null) && !Config.RAIDBOSSES_EXCLUDED_FROM_DEFEAT_ANNOUNCEMENTS.contains(getId()))
				{
					Broadcast.toAllOnlinePlayers(name + " has been defeated!");
					Broadcast.toAllOnlinePlayersOnScreen(name + " has been defeated!");
				}
			}
		}
		else if (isRaid() && Config.RAIDBOSS_DEFEAT_ANNOUNCEMENTS && ((getInstanceId() == 0) || Config.RAIDBOSS_INSTANCE_ANNOUNCEMENTS) && !isMinion() && !isRaidMinion())
		{
			final String name = NpcData.getInstance().getTemplate(getId()).getName();
			if ((name != null) && !Config.RAIDBOSSES_EXCLUDED_FROM_DEFEAT_ANNOUNCEMENTS.contains(getId()))
			{
				Broadcast.toAllOnlinePlayers(name + " has been defeated!");
				Broadcast.toAllOnlinePlayersOnScreen(name + " has been defeated!");
			}
		}
		
		return true;
	}
	
	@Override
	public boolean decayMe()
	{
		if (hasAI())
		{
			if (isAttackable())
			{
				getAttackByList().clear();
				asAttackable().clearAggroList();
				getAI().setIntention(Intention.IDLE);
			}
			
			getAI().stopAITask();
		}
		
		// Habilita AI.
		_disabledAI = false;
		
		_onCreatureAttack = null;
		_onCreatureAttacked = null;
		_onCreatureDamageDealt = null;
		_onCreatureDamageReceived = null;
		_onCreatureAttackAvoid = null;
		_onCreatureSkillUse = null;
		
		return super.decayMe();
	}
	
	public boolean deleteMe()
	{
		if (hasAI())
		{
			getAI().stopAITask();
		}
		
		// Remove todos os efeitos, nao transmite mudancas.
		_effectList.stopAllEffectsWithoutExclusions(false, false);
		
		// Esquece todas as criaturas vistas.
		if (_seenCreatures != null)
		{
			CreatureSeeTaskManager.getInstance().remove(this);
			_seenCreatures.clear();
		}
		
		// Cancela a BuffFinishTask relacionada a esta criatura.
		_buffFinishTask.stop();
		
		// Define world region como null.
		setWorldRegion(null);
		
		return true;
	}
	
	public void detachAI()
	{
		if (isWalker())
		{
			return;
		}
		
		setAI(null);
	}
	
	protected void calculateRewards(Creature killer)
	{
	}
	
	/** Define HP, MP e CP e revive a Creature. */
	public void doRevive()
	{
		if (!_isDead)
		{
			return;
		}
		
		if (!_isTeleporting)
		{
			setIsPendingRevive(false);
			setDead(false);
			
			if ((Config.RESPAWN_RESTORE_CP > 0) && (_status.getCurrentCp() < (_stat.getMaxCp() * Config.RESPAWN_RESTORE_CP)))
			{
				_status.setCurrentCp(_stat.getMaxCp() * Config.RESPAWN_RESTORE_CP);
			}
			
			if ((Config.RESPAWN_RESTORE_HP > 0) && (_status.getCurrentHp() < (_stat.getMaxHp() * Config.RESPAWN_RESTORE_HP)))
			{
				_status.setCurrentHp(_stat.getMaxHp() * Config.RESPAWN_RESTORE_HP);
			}
			
			if ((Config.RESPAWN_RESTORE_MP > 0) && (_status.getCurrentMp() < (_stat.getMaxMp() * Config.RESPAWN_RESTORE_MP)))
			{
				_status.setCurrentMp(_stat.getMaxMp() * Config.RESPAWN_RESTORE_MP);
			}
			
			// Inicia transmissao de status
			broadcastPacket(new Revive(this));
			ZoneManager.getInstance().getRegion(this).onRevive(this);
		}
		else
		{
			setIsPendingRevive(true);
		}
	}
	
	/**
	 * Revive a Creature usando skill.
	 * @param revivePower
	 */
	public void doRevive(double revivePower)
	{
		doRevive();
	}
	
	/**
	 * Obtem a AI desta criatura.
	 * @return a AI
	 */
	public CreatureAI getAI()
	{
		CreatureAI ai = _ai;
		if (ai == null)
		{
			synchronized (this)
			{
				ai = _ai;
				if (ai == null)
				{
					_ai = ai = initAI();
				}
			}
		}
		
		return ai;
	}
	
	/**
	 * Inicializa a AI desta criatura.<br>
	 * Abordagem OOP para ser sobrescrita em classes filhas.
	 * @return a nova AI
	 */
	protected CreatureAI initAI()
	{
		return new CreatureAI(this);
	}
	
	public void setAI(CreatureAI newAI)
	{
		final CreatureAI oldAI = _ai;
		if ((oldAI != null) && (oldAI != newAI) && (oldAI instanceof AttackableAI))
		{
			oldAI.stopAITask();
		}
		
		_ai = newAI;
	}
	
	/**
	 * Verifica se esta criatura tem uma AI.
	 * @return {@code true} se esta criatura tem uma AI, {@code false} caso contrario
	 */
	public boolean hasAI()
	{
		return _ai != null;
	}
	
	/**
	 * @return True se a Creature e RaidBoss ou seu lacaio.
	 */
	public boolean isRaid()
	{
		return false;
	}
	
	/**
	 * @return True se a Creature e lacaio.
	 */
	public boolean isMinion()
	{
		return false;
	}
	
	/**
	 * @return True se a Creature e lacaio de RaidBoss.
	 */
	public boolean isRaidMinion()
	{
		return false;
	}
	
	/**
	 * @return uma lista de Creatures que atacaram.
	 */
	public Set<Creature> getAttackByList()
	{
		return _attackByList;
	}
	
	public Skill getLastSimultaneousSkillCast()
	{
		return _lastSimultaneousSkillCast;
	}
	
	public void setLastSimultaneousSkillCast(Skill skill)
	{
		_lastSimultaneousSkillCast = skill;
	}
	
	public Skill getLastSkillCast()
	{
		return _lastSkillCast;
	}
	
	public void setLastSkillCast(Skill skill)
	{
		_lastSkillCast = skill;
	}
	
	public boolean isAfraid()
	{
		return isAffected(EffectFlag.FEAR);
	}
	
	/**
	 * @return True se a Creature nao pode usar suas skills (ex: stun, sleep...).
	 */
	public boolean isAllSkillsDisabled()
	{
		return _allSkillsDisabled || isStunned() || isSleeping() || isParalyzed();
	}
	
	/**
	 * @return True se a Creature nao pode atacar (attackEndTime, attackMute, fake death, stun, sleep, paralyze).
	 */
	public boolean isAttackDisabled()
	{
		return isAttackingNow() || isDisabled();
	}
	
	/**
	 * @return True se a Creature esta desabilitada (attackMute, fake death, stun, sleep, paralyze).
	 */
	public boolean isDisabled()
	{
		return _disabledAI || isAlikeDead() || isPhysicalAttackMuted() || isStunned() || isSleeping() || isParalyzed();
	}
	
	public Calculator[] getCalculators()
	{
		return _calculators;
	}
	
	public boolean isConfused()
	{
		return isAffected(EffectFlag.CONFUSED);
	}
	
	/**
	 * @return True se a Creature esta morta ou usando fake death.
	 */
	public boolean isAlikeDead()
	{
		return _isDead;
	}
	
	/**
	 * @return True se a Creature esta morta.
	 */
	public boolean isDead()
	{
		return _isDead;
	}
	
	public void setDead(boolean value)
	{
		_isDead = value;
	}
	
	public boolean isImmobilized()
	{
		return _isImmobilized;
	}
	
	public void setImmobilized(boolean value)
	{
		_isImmobilized = value;
	}
	
	public boolean isMuted()
	{
		return isAffected(EffectFlag.MUTED);
	}
	
	public boolean isPhysicalMuted()
	{
		return isAffected(EffectFlag.PSYCHICAL_MUTED);
	}
	
	public boolean isPhysicalAttackMuted()
	{
		return isAffected(EffectFlag.PSYCHICAL_ATTACK_MUTED);
	}
	
	/**
	 * @return True se a Creature nao pode se mover (stun, root, sleep, overload, paralyzed).
	 */
	public boolean isMovementDisabled()
	{
		// check for isTeleporting to prevent teleport cheating (if appear packet not received)
		return isStunned() || isRooted() || isSleeping() || _isOverloaded || isParalyzed() || _isImmobilized || isAlikeDead() || _isTeleporting;
	}
	
	/**
	 * @return True se a Creature nao pode ser controlada pelo jogador (confused, afraid).
	 */
	public boolean isOutOfControl()
	{
		return isConfused() || isAfraid();
	}
	
	public boolean isOverloaded()
	{
		return _isOverloaded;
	}
	
	/**
	 * Define o status de sobrecarregado da Creature (se True, o Player nao pode pegar mais itens).
	 * @param value
	 */
	public void setOverloaded(boolean value)
	{
		_isOverloaded = value;
	}
	
	public boolean isParalyzed()
	{
		return _isParalyzed || isAffected(EffectFlag.PARALYZED);
	}
	
	public void setParalyzed(boolean value)
	{
		_isParalyzed = value;
	}
	
	public boolean isPendingRevive()
	{
		return _isDead && _isPendingRevive;
	}
	
	public void setIsPendingRevive(boolean value)
	{
		_isPendingRevive = value;
	}
	
	public boolean isDisarmed()
	{
		return isAffected(EffectFlag.DISARMED);
	}
	
	public boolean isRooted()
	{
		return isAffected(EffectFlag.ROOTED);
	}
	
	/**
	 * @return True se a Creature esta correndo.
	 */
	public boolean isRunning()
	{
		return _isRunning;
	}
	
	private void setRunning(boolean value)
	{
		if (_isRunning == value)
		{
			return;
		}
		
		_isRunning = value;
		if (_stat.getRunSpeed() != 0)
		{
			broadcastPacket(new ChangeMoveType(this));
		}
		
		if (isPlayer())
		{
			asPlayer().broadcastUserInfo();
		}
		else if (isSummon())
		{
			broadcastStatusUpdate();
		}
		else if (isNpc())
		{
			World.getInstance().forEachVisibleObject(this, Player.class, player ->
			{
				if (!isVisibleFor(player))
				{
					return;
				}
				
				if (_stat.getRunSpeed() == 0)
				{
					player.sendPacket(new ServerObjectInfo(asNpc(), player));
				}
				else
				{
					player.sendPacket(new AbstractNpcInfo.NpcInfo(asNpc(), player));
				}
			});
		}
	}
	
	/** Define o tipo de movimento da Creature para corrida e envia pacote Server->Client ChangeMoveType para todos os outros Players. */
	public void setRunning()
	{
		setRunning(true);
	}
	
	public boolean isSleeping()
	{
		return isAffected(EffectFlag.SLEEP);
	}
	
	public boolean isStunned()
	{
		return isAffected(EffectFlag.STUNNED);
	}
	
	public boolean isBetrayed()
	{
		return isAffected(EffectFlag.BETRAYED);
	}
	
	public boolean isTeleporting()
	{
		return _isTeleporting;
	}
	
	public void setTeleporting(boolean value)
	{
		_isTeleporting = value;
	}
	
	public void setInvul(boolean value)
	{
		_isInvul = value;
	}
	
	public boolean isInvul()
	{
		return _isInvul || _isTeleporting || isAffected(EffectFlag.INVUL);
	}
	
	public void setMortal(boolean value)
	{
		_isMortal = value;
	}
	
	public boolean isMortal()
	{
		return _isMortal;
	}
	
	public boolean isUndead()
	{
		return false;
	}
	
	public boolean isResurrectionBlocked()
	{
		return isAffected(EffectFlag.BLOCK_RESURRECTION);
	}
	
	public boolean isFlying()
	{
		return _isFlying;
	}
	
	public void setFlying(boolean mode)
	{
		_isFlying = mode;
	}
	
	public CreatureStat getStat()
	{
		return _stat;
	}
	
	/**
	 * Inicializa a classe CharStat do WorldObject, e sobrescrita em classes que requerem um tipo CharStat diferente.<br>
	 * Remove a necessidade de verificacoes instanceof.
	 */
	public void initCharStat()
	{
		_stat = new CreatureStat(this);
	}
	
	public void setStat(CreatureStat value)
	{
		_stat = value;
	}
	
	public CreatureStatus getStatus()
	{
		return _status;
	}
	
	/**
	 * Inicializa a classe CharStatus do WorldObject, e sobrescrita em classes que requerem um tipo CharStatus diferente.<br>
	 * Remove a necessidade de verificacoes instanceof.
	 */
	public void initCharStatus()
	{
		_status = new CreatureStatus(this);
	}
	
	public void setStatus(CreatureStatus value)
	{
		_status = value;
	}
	
	public CreatureTemplate getTemplate()
	{
		return _template;
	}
	
	/**
	 * Define o template da Creature.<br>
	 * <br>
	 * <b><u>Conceito</u>:</b><br>
	 * <br>
	 * Cada Creature possui propriedades genericas e estaticas (ex: todos os Keltir tem o mesmo numero de HP...).<br>
	 * Todas essas propriedades sao armazenadas em um template diferente para cada tipo de Creature.<br>
	 * Cada template e carregado uma vez na memoria cache do servidor (reduz uso de memoria).<br>
	 * Quando uma nova instancia de Creature e criada, o servidor apenas cria um link entre a instancia e o template. Esse link e armazenado em <b>_template</b>.
	 * @param template
	 */
	protected void setTemplate(CreatureTemplate template)
	{
		_template = template;
	}
	
	/**
	 * @return o Titulo da Creature.
	 */
	public String getTitle()
	{
		return _title;
	}
	
	/**
	 * Define o Titulo da Creature.
	 * @param value
	 */
	public void setTitle(String value)
	{
		if (value == null)
		{
			_title = "";
		}
		else
		{
			_title = isPlayer() && (value.length() > 21) ? value.substring(0, 20) : value;
		}
	}
	
	/**
	 * Define o tipo de movimento da Creature para andar e envia pacote Server->Client ChangeMoveType para todos os outros Players.
	 */
	public void setWalking()
	{
		setRunning(false);
	}
	
	/**
	 * Obtem os efeitos visuais anormais que afetam este personagem.
	 * @return um mapa de 32 bits contendo todos os efeitos visuais anormais em progresso para este personagem
	 */
	public int getAbnormalVisualEffects()
	{
		return _abnormalVisualEffects;
	}
	
	/**
	 * Obtem os efeitos visuais anormais especiais que afetam este personagem.
	 * @return um mapa de 32 bits contendo todos os efeitos especiais em progresso para este personagem
	 */
	public int getAbnormalVisualEffectSpecial()
	{
		return _abnormalVisualEffectsSpecial;
	}
	
	/**
	 * Obtem os efeitos visuais anormais de evento que afetam este personagem.
	 * @return um mapa de 32 bits contendo todos os efeitos visuais anormais de evento em progresso para este personagem
	 */
	public int getAbnormalVisualEffectEvent()
	{
		return _abnormalVisualEffectsEvent;
	}
	
	/**
	 * Verifica se esta criatura e afetada pelo efeito visual anormal dado.
	 * @param ave o efeito visual anormal
	 * @return {@code true} se a criatura e afetada pelo efeito visual anormal, {@code false} caso contrario
	 */
	public boolean hasAbnormalVisualEffect(AbnormalVisualEffect ave)
	{
		if (ave.isEvent())
		{
			return (_abnormalVisualEffectsEvent & ave.getMask()) == ave.getMask();
		}
		
		if (ave.isSpecial())
		{
			return (_abnormalVisualEffectsSpecial & ave.getMask()) == ave.getMask();
		}
		
		return (_abnormalVisualEffects & ave.getMask()) == ave.getMask();
	}
	
	/**
	 * Adiciona as flags de efeito visual anormal na mascara binaria e envia pacote Server->Client UserInfo/CharInfo.
	 * @param update se {@code true} pacotes de atualizacao serao enviados
	 * @param aves os efeitos visuais anormais
	 */
	public void startAbnormalVisualEffect(boolean update, AbnormalVisualEffect... aves)
	{
		for (AbnormalVisualEffect ave : aves)
		{
			if (ave.isEvent())
			{
				_abnormalVisualEffectsEvent |= ave.getMask();
			}
			else if (ave.isSpecial())
			{
				_abnormalVisualEffectsSpecial |= ave.getMask();
			}
			else
			{
				_abnormalVisualEffects |= ave.getMask();
			}
		}
		
		if (update)
		{
			updateAbnormalEffect();
		}
	}
	
	/**
	 * Remove as flags de efeito visual anormal da mascara binaria e envia pacote Server->Client UserInfo/CharInfo.
	 * @param update se {@code true} pacotes de atualizacao serao enviados
	 * @param aves os efeitos visuais anormais
	 */
	public void stopAbnormalVisualEffect(boolean update, AbnormalVisualEffect... aves)
	{
		for (AbnormalVisualEffect ave : aves)
		{
			if (ave.isEvent())
			{
				_abnormalVisualEffectsEvent &= ~ave.getMask();
			}
			else if (ave.isSpecial())
			{
				_abnormalVisualEffectsSpecial &= ~ave.getMask();
			}
			else
			{
				_abnormalVisualEffects &= ~ave.getMask();
			}
		}
		
		if (update)
		{
			updateAbnormalEffect();
		}
	}
	
	/**
	 * Ativa a flag de efeito anormal Fake Death, notifica a AI da Creature e envia pacote Server->Client UserInfo/CharInfo.
	 */
	public void startFakeDeath()
	{
		if (!isPlayer())
		{
			return;
		}
		
		asPlayer().setFakeDeath(true);
		
		// Aborta quaisquer ataques/conjuracoes se fingindo de morto
		abortAttack();
		abortCast();
		stopMove(null);
		getAI().notifyAction(Action.FAKE_DEATH);
		broadcastPacket(new ChangeWaitType(this, ChangeWaitType.WT_START_FAKEDEATH));
		
		// Remove alvo daqueles que tem a criatura nao alvo-avel no alvo.
		if (Config.FAKE_DEATH_UNTARGET)
		{
			World.getInstance().forEachVisibleObject(this, Creature.class, c ->
			{
				if (c.getTarget() == this)
				{
					c.setTarget(null);
				}
			});
		}
	}
	
	/**
	 * Executa um Efeito Anormal de Stun na Creature.<br>
	 * <br>
	 * <b><u>Acoes</u>:</b>
	 * <ul>
	 * <li>Calcula a taxa de sucesso do Efeito Anormal de Stun nesta Creature</li>
	 * <li>Se Stun tiver sucesso, ativa a flag de efeito anormal de Stun, notifica a AI da Creature e envia pacote Server->Client UserInfo/CharInfo</li>
	 * <li>Se Stun NAO tiver sucesso, envia uma mensagem de sistema Falhou para o Player atacante</li>
	 * </ul>
	 */
	public void startStunning()
	{
		// Aborta quaisquer ataques/conjuracoes se atordoado
		abortAttack();
		abortCast();
		stopMove(null);
		getAI().notifyAction(Action.STUNNED);
		if (!isSummon())
		{
			getAI().setIntention(Intention.IDLE);
		}
		
		updateAbnormalEffect();
	}
	
	public void startParalyze()
	{
		// Aborta quaisquer ataques/conjuracoes se paralisado
		abortAttack();
		abortCast();
		stopMove(null);
		getAI().notifyAction(Action.PARALYZED);
	}
	
	/**
	 * Para todos os efeitos de skills ativos em progresso na Creature.
	 */
	public void stopAllEffects()
	{
		_effectList.stopAllEffects();
	}
	
	/**
	 * Para todos os efeitos, exceto aqueles que persistem apos a morte.
	 */
	public void stopAllEffectsExceptThoseThatLastThroughDeath()
	{
		_effectList.stopAllEffectsExceptThoseThatLastThroughDeath();
	}
	
	/**
	 * Para e remove os efeitos correspondentes ao ID da skill.
	 * @param type determina a mensagem de sistema que sera enviada.
	 * @param skillId o Id da skill
	 */
	public void stopSkillEffects(SkillFinishType type, int skillId)
	{
		_effectList.stopSkillEffects(type, skillId);
	}
	
	public void stopEffects(EffectType type)
	{
		_effectList.stopEffects(type);
	}
	
	/**
	 * Encerra todos os efeitos de buff das skills com "removedOnAnyAction" definido.<br>
	 * Chamado em qualquer acao exceto movimento (ataque, conjuracao).
	 */
	public void stopEffectsOnAction()
	{
		_effectList.stopEffectsOnAction();
	}
	
	/**
	 * Encerra todos os efeitos de buff das skills com "removedOnDamage" definido.<br>
	 * Chamado ao diminuir HP e queima de mana.
	 * @param awake
	 */
	public void stopEffectsOnDamage(boolean awake)
	{
		_effectList.stopEffectsOnDamage(awake);
	}
	
	/**
	 * Para um/todos os Efeitos Anormais de Fake Death especificados.<br>
	 * <br>
	 * <b><u>Acoes</u>:</b>
	 * <ul>
	 * <li>Deleta um/todos (se effect=null) Efeitos Anormais de Fake Death da Creature e atualiza icone magico do cliente</li>
	 * <li>Define a flag de efeito anormal _fake_death para False</li>
	 * <li>Notifica a AI da Creature</li>
	 * </ul>
	 * @param removeEffects
	 */
	public void stopFakeDeath(boolean removeEffects)
	{
		if (removeEffects)
		{
			stopEffects(EffectType.FAKE_DEATH);
		}
		
		// se esta for uma instancia de player, inicia o periodo de graca para este personagem (graca apenas de mobs)!
		if (isPlayer())
		{
			final Player player = asPlayer();
			player.setFakeDeath(false);
			player.setRecentFakeDeath(true);
		}
		
		broadcastPacket(new ChangeWaitType(this, ChangeWaitType.WT_STOP_FAKEDEATH));
		
		// TODO: Hack temporario: jogadores veem FD em pessoas que estao se movendo: Teleporte para alguem que usa FD - se ele se levantar ele caira novamente para aquele cliente -
		// mesmo que ele esteja realmente de pe... Provavelmente informacao errada no pacote CharInfo?
		broadcastPacket(new Revive(this));
	}
	
	/**
	 * Para um/todos os Efeitos Anormais de Stun especificados.<br>
	 * <br>
	 * <b><u>Acoes</u>:</b>
	 * <ul>
	 * <li>Deleta um/todos (se effect=null) Efeitos Anormais de Stun da Creature e atualiza icone magico do cliente</li>
	 * <li>Define a flag de efeito anormal _stuned para False</li>
	 * <li>Notifica a AI da Creature</li>
	 * <li>Envia pacote Server->Client UserInfo/CharInfo</li>
	 * </ul>
	 * @param removeEffects
	 */
	public void stopStunning(boolean removeEffects)
	{
		if (removeEffects)
		{
			stopEffects(EffectType.STUN);
		}
		
		if (!isPlayer())
		{
			getAI().notifyAction(Action.THINK);
		}
		
		updateAbnormalEffect();
	}
	
	/**
	 * Para Efeito: Transformacao.<br>
	 * <br>
	 * <b><u>Acoes</u>:</b>
	 * <ul>
	 * <li>Remove Efeito de Transformacao</li>
	 * <li>Notifica a AI da Creature</li>
	 * <li>Envia pacote Server->Client UserInfo/CharInfo</li>
	 * </ul>
	 * @param removeEffects
	 */
	public void stopTransformation(boolean removeEffects)
	{
		if (removeEffects)
		{
			_effectList.stopSkillEffects(SkillFinishType.NORMAL, AbnormalType.TRANSFORM);
		}
		
		// se esta for uma instancia de player, entao destransforma, tambem define a coluna transform_id igual a 0 se nao for amaldicoado.
		if (isPlayer() && (getTransformation() != null))
		{
			untransform();
		}
		
		if (!isPlayer())
		{
			getAI().notifyAction(Action.THINK);
		}
		
		updateAbnormalEffect();
	}
	
	public abstract void updateAbnormalEffect();
	
	/**
	 * Atualiza icones de skills ativas em progresso (Em Uso e Nao Em Uso por estarem empilhadas) no cliente.<br>
	 * <br>
	 * <b><u>Conceito</u>:</b><br>
	 * <br>
	 * Todos os efeitos de skills ativas em progresso (Em Uso e Nao Em Uso por estarem empilhadas) sao representados por um icone no cliente.<br>
	 * <font color=#FF0000><b><u>Cuidado</u>: Este metodo APENAS ATUALIZA o cliente do jogador e nao clientes de todos os jogadores no grupo.</b></font>
	 */
	public void updateEffectIcons()
	{
		updateEffectIcons(false);
	}
	
	/**
	 * Atualiza Icones de Efeitos para este personagem(player/summon) e seu grupo se houver.
	 * @param partyOnly
	 */
	public void updateEffectIcons(boolean partyOnly)
	{
		// sobrescrito
	}
	
	public boolean isAffectedBySkill(int skillId)
	{
		return _effectList.isAffectedBySkill(skillId);
	}
	
	/**
	 * Esta classe agrupa todos os dados de movimento.
	 */
	public static class MoveData
	{
		// Quando recuperamos x/y/z usamos GameTimeControl.getGameTicks()
		// Se estamos nos movendo, mas move timestamp==gameticks, nao precisamos recalcular a posicao.
		int moveStartTime;
		int moveTimestamp; // Ultima atualizacao de movimento.
		int xDestination;
		int yDestination;
		int zDestination;
		double xAccurate; // Caso contrario haveria erros de arredondamento.
		double yAccurate;
		double zAccurate;
		int heading;
		
		boolean disregardingGeodata;
		int onGeodataPathIndex;
		List<AbstractNodeLoc> geoPath;
		int geoPathAccurateTx;
		int geoPathAccurateTy;
		int geoPathGtx;
		int geoPathGty;
		
		int lastBroadcastTime;
	}
	
	/**
	 * Adiciona uma Func ao conjunto de Calculators da Creature.<br>
	 * <br>
	 * <b><u>Conceito</u>:</b> Uma Creature possui uma tabela de Calculators chamada <b>_calculators</b>.<br>
	 * Cada Calculator (um calculator por estado) possui uma tabela de objetos Func.<br>
	 * Um objeto Func e uma funcao matematica que permite calcular o modificador de um estado (ex: REGENERATE_HP_RATE...).<br>
	 * Para reduzir uso de memoria cache, Npcs que nao tem skills compartilham o mesmo conjunto de Calculators chamado <b>NPC_STD_CALCULATOR</b>.<br>
	 * Por isso, se um Npc esta sob um efeito de skill/magia que modifica um de seus estados, uma copia do NPC_STD_CALCULATOR deve ser criada em seus _calculators antes de adicionar novos objetos Func.<br>
	 * <br>
	 * <b><u>Acoes</u>:</b>
	 * <ul>
	 * <li>Se _calculators esta vinculado ao NPC_STD_CALCULATOR, cria uma copia do NPC_STD_CALCULATOR em _calculators</li>
	 * <li>Adiciona o objeto Func a _calculators</li>
	 * </ul>
	 * @param function O objeto Func a ser adicionado ao Calculator correspondente ao estado afetado
	 */
	public void addStatFunc(AbstractFunction function)
	{
		if (function == null)
		{
			return;
		}
		
		synchronized (this)
		{
			// Verifica se o conjunto de Calculators esta vinculado ao conjunto padrao de NPC
			if (_calculators == NPC_STD_CALCULATOR)
			{
				// Cria uma copia do conjunto padrao de NPC Calculator
				_calculators = new Calculator[Stat.NUM_STATS];
				for (int i = 0; i < Stat.NUM_STATS; i++)
				{
					if (NPC_STD_CALCULATOR[i] != null)
					{
						_calculators[i] = new Calculator(NPC_STD_CALCULATOR[i]);
					}
				}
			}
			
			// Seleciona o Calculator do estado afetado no conjunto de Calculators
			final int stat = function.getStat().ordinal();
			if (_calculators[stat] == null)
			{
				_calculators[stat] = new Calculator();
			}
			
			// Adiciona a Func ao calculator correspondente ao estado
			_calculators[stat].addFunc(function);
		}
	}
	
	/**
	 * Adiciona uma lista de Funcs ao conjunto de Calculators da Creature.<br>
	 * <br>
	 * <b><u>Conceito</u>:</b><br>
	 * <br>
	 * Uma Creature possui uma tabela de Calculators chamada <b>_calculators</b>.<br>
	 * Cada Calculator (um calculator por estado) possui uma tabela de objetos Func.<br>
	 * Um objeto Func e uma funcao matematica que permite calcular o modificador de um estado (ex: REGENERATE_HP_RATE...).<br>
	 * <font color=#FF0000><b><u>Cuidado</u>: Este metodo e APENAS para Player</b></font><br>
	 * <br>
	 * <b><u>Exemplo de uso</u>:</b>
	 * <ul>
	 * <li>Equipar um item do inventario</li>
	 * <li>Aprender uma nova skill passiva</li>
	 * <li>Usar uma skill ativa</li>
	 * </ul>
	 * @param functions A lista de objetos Func a serem adicionados ao Calculator correspondente ao estado afetado
	 */
	public void addStatFuncs(List<AbstractFunction> functions)
	{
		final List<Stat> modifiedStats = new ArrayList<>();
		for (AbstractFunction f : functions)
		{
			modifiedStats.add(f.getStat());
			addStatFunc(f);
		}
		
		broadcastModifiedStats(modifiedStats);
	}
	
	/**
	 * Remove a Func from the Calculator set of the Creature.<br>
	 * <br>
	 * <b><u>Concept</u>:</b><br>
	 * <br>
	 * A Creature owns a table of Calculators called <b>_calculators</b>.<br>
	 * Each Calculator (a calculator per state) own a table of Func object.<br>
	 * A Func object is a mathematic function that permit to calculate the modifier of a state (ex : REGENERATE_HP_RATE...).<br>
	 * To reduce cache memory use, Npcs who don't have skills share the same Calculator set called <b>NPC_STD_CALCULATOR</b>.<br>
	 * Por isso, se um Npc esta sob um efeito de skill/magia que modifica um de seus estados, uma copia do NPC_STD_CALCULATOR deve ser criada em seus _calculators antes de adicionar novos objetos Func.<br>
	 * <br>
	 * <b><u>Acoes</u>:</b>
	 * <ul>
	 * <li>Remove o objeto Func de _calculators</li>
	 * <li>Se a Creature for um Npc e _calculators for igual a NPC_STD_CALCULATOR, libera memoria cache e apenas cria um link para NPC_STD_CALCULATOR em _calculators</li>
	 * </ul>
	 * @param function O objeto Func a ser removido do Calculator correspondente ao estado afetado
	 */
	public void removeStatFunc(AbstractFunction function)
	{
		if (function == null)
		{
			return;
		}
		
		// Seleciona o Calculator do estado afetado no conjunto de Calculators
		final int stat = function.getStat().ordinal();
		
		synchronized (this)
		{
			if (_calculators[stat] == null)
			{
				return;
			}
			
			// Remove o objeto Func do Calculator
			_calculators[stat].removeFunc(function);
			
			if (_calculators[stat].size() == 0)
			{
				_calculators[stat] = null;
			}
			
			// Se possivel, libera a memoria e apenas cria um link para NPC_STD_CALCULATOR
			if (isNpc())
			{
				int i = 0;
				for (; i < Stat.NUM_STATS; i++)
				{
					if (!Calculator.equalsCals(_calculators[i], NPC_STD_CALCULATOR[i]))
					{
						break;
					}
				}
				
				if (i >= Stat.NUM_STATS)
				{
					_calculators = NPC_STD_CALCULATOR;
				}
			}
		}
	}
	
	/**
	 * Remove uma lista de Funcs do conjunto de Calculators do Player.<br>
	 * <br>
	 * <b><u>Conceito</u>:</b><br>
	 * <br>
	 * Uma Creature possui uma tabela de Calculators chamada <b>_calculators</b>.<br>
	 * Cada Calculator (um calculator por estado) possui uma tabela de objetos Func.<br>
	 * Um objeto Func e uma funcao matematica que permite calcular o modificador de um estado (ex: REGENERATE_HP_RATE...).<br>
	 * <font color=#FF0000><b><u>Cuidado</u>: Este metodo e APENAS para Player</b></font><br>
	 * <br>
	 * <b><u>Exemplo de uso</u>:</b>
	 * <ul>
	 * <li>Desequipar um item do inventario</li>
	 * <li>Parar uma skill ativa</li>
	 * </ul>
	 * @param functions A lista de objetos Func a serem adicionados ao Calculator correspondente ao estado afetado
	 */
	public void removeStatFuncs(AbstractFunction[] functions)
	{
		final List<Stat> modifiedStats = new ArrayList<>();
		for (AbstractFunction f : functions)
		{
			modifiedStats.add(f.getStat());
			removeStatFunc(f);
		}
		
		broadcastModifiedStats(modifiedStats);
	}
	
	/**
	 * Remove todos os objetos Func com o proprietario selecionado do conjunto de Calculators da Creature.<br>
	 * <br>
	 * <b><u>Conceito</u>:</b><br>
	 * <br>
	 * Uma Creature possui uma tabela de Calculators chamada <b>_calculators</b>.<br>
	 * Cada Calculator (um calculator por estado) possui uma tabela de objetos Func.<br>
	 * Um objeto Func e uma funcao matematica que permite calcular o modificador de um estado (ex: REGENERATE_HP_RATE...).<br>
	 * Para reduzir uso de memoria cache, Npcs que nao tem skills compartilham o mesmo conjunto de Calculators chamado <b>NPC_STD_CALCULATOR</b>.<br>
	 * Por isso, se um Npc esta sob um efeito de skill/magia que modifica um de seus estados, uma copia do NPC_STD_CALCULATOR deve ser criada em seus _calculators antes de adicionar novos objetos Func.<br>
	 * <br>
	 * <b><u>Acoes</u>:</b>
	 * <ul>
	 * <li>Remove todos os objetos Func do proprietario selecionado de _calculators</li>
	 * <li>Se a Creature for um Npc e _calculators for igual a NPC_STD_CALCULATOR, libera memoria cache e apenas cria um link para NPC_STD_CALCULATOR em _calculators</li>
	 * </ul>
	 * <br>
	 * <b><u>Exemplo de uso</u>:</b>
	 * <ul>
	 * <li>Desequipar um item do inventario</li>
	 * <li>Parar uma skill ativa</li>
	 * </ul>
	 * @param owner O Objeto(Skill, Item...) que criou o efeito
	 */
	public void removeStatsOwner(Object owner)
	{
		List<Stat> modifiedStats = null;
		int i = 0;
		
		// Percorre o conjunto de Calculators
		synchronized (this)
		{
			for (Calculator calc : _calculators)
			{
				if (calc != null)
				{
					// Deleta todos os objetos Func do proprietario selecionado
					if (modifiedStats != null)
					{
						modifiedStats.addAll(calc.removeOwner(owner));
					}
					else
					{
						modifiedStats = calc.removeOwner(owner);
					}
					
					if (calc.size() == 0)
					{
						_calculators[i] = null;
					}
				}
				
				i++;
			}
			
			// Se possivel, libera a memoria e apenas cria um link para NPC_STD_CALCULATOR
			if (isNpc())
			{
				i = 0;
				for (; i < Stat.NUM_STATS; i++)
				{
					if (!Calculator.equalsCals(_calculators[i], NPC_STD_CALCULATOR[i]))
					{
						break;
					}
				}
				
				if (i >= Stat.NUM_STATS)
				{
					_calculators = NPC_STD_CALCULATOR;
				}
			}
			
			if (isSummon())
			{
				if (getCurrentHp() > getMaxHp())
				{
					setCurrentHp(getMaxHp());
				}
				if (getCurrentMp() > getMaxMp())
				{
					setCurrentMp(getMaxMp());
				}
			}
			
			broadcastModifiedStats(modifiedStats);
		}
	}
	
	protected void broadcastModifiedStats(List<Stat> stats)
	{
		if (!isSpawned() || (stats == null) || stats.isEmpty())
		{
			return;
		}
		
		if (isSummon())
		{
			final Summon summon = asSummon();
			if (summon.getOwner() != null)
			{
				summon.updateAndBroadcastStatus(1);
			}
		}
		else
		{
			boolean broadcastFull = false;
			final StatusUpdate su = new StatusUpdate(this);
			for (Stat stat : stats)
			{
				if (stat == Stat.POWER_ATTACK_SPEED)
				{
					su.addAttribute(StatusUpdate.ATK_SPD, (int) _stat.getPAtkSpd());
				}
				else if (stat == Stat.MAGIC_ATTACK_SPEED)
				{
					su.addAttribute(StatusUpdate.CAST_SPD, _stat.getMAtkSpd());
				}
				else if (stat == Stat.MOVE_SPEED)
				{
					broadcastFull = true;
				}
			}
			
			if (isPlayer())
			{
				final Player player = asPlayer();
				if (broadcastFull)
				{
					player.broadcastUserInfo();
				}
				else
				{
					player.updateUserInfo();
					if (su.hasAttributes())
					{
						broadcastPacket(su);
					}
				}
				
				final Summon summon = player.getSummon();
				if ((summon != null) && isAffected(EffectFlag.SERVITOR_SHARE))
				{
					summon.broadcastStatusUpdate();
				}
			}
			else if (isNpc())
			{
				if (broadcastFull)
				{
					World.getInstance().forEachVisibleObject(this, Player.class, player ->
					{
						if (!isVisibleFor(player))
						{
							return;
						}
						
						if (_stat.getRunSpeed() == 0)
						{
							player.sendPacket(new ServerObjectInfo(asNpc(), player));
						}
						else
						{
							player.sendPacket(new AbstractNpcInfo.NpcInfo(asNpc(), player));
						}
					});
				}
				else if (su.hasAttributes())
				{
					broadcastPacket(su);
				}
			}
			else if (su.hasAttributes())
			{
				broadcastPacket(su);
			}
		}
	}
	
	/**
	 * @return o destino X da Creature ou a posicao X se nao estiver em movimento.
	 */
	public int getXdestination()
	{
		final MoveData move = _move;
		if (move != null)
		{
			return move.xDestination;
		}
		
		return getX();
	}
	
	/**
	 * @return o destino Y da Creature ou a posicao Y se nao estiver em movimento.
	 */
	public int getYdestination()
	{
		final MoveData move = _move;
		if (move != null)
		{
			return move.yDestination;
		}
		
		return getY();
	}
	
	/**
	 * @return o destino Z da Creature ou a posicao Z se nao estiver em movimento.
	 */
	public int getZdestination()
	{
		final MoveData move = _move;
		if (move != null)
		{
			return move.zDestination;
		}
		
		return getZ();
	}
	
	/**
	 * @return True se a Creature esta em combate.
	 */
	public boolean isInCombat()
	{
		return hasAI() && ((getAI().getAttackTarget() != null) || getAI().isAutoAttacking());
	}
	
	/**
	 * @return True se a Creature esta se movendo.
	 */
	public boolean isMoving()
	{
		return _move != null;
	}
	
	/**
	 * @return True se a Creature esta viajando por um caminho calculado.
	 */
	public boolean isOnGeodataPath()
	{
		final MoveData move = _move;
		if (move == null)
		{
			return false;
		}
		
		return isOnGeodataPath(move);
	}
	
	/**
	 * @param move o MoveData a verificar (nao deve ser null).
	 * @return True se a Creature esta viajando por um caminho calculado.
	 */
	public boolean isOnGeodataPath(MoveData move)
	{
		if ((move.onGeodataPathIndex == -1) || (move.onGeodataPathIndex == (move.geoPath.size() - 1)))
		{
			return false;
		}
		
		return true;
	}
	
	/**
	 * Este metodo retorna uma lista de nos de pathfinding representando o caminho de movimento.<br>
	 * Se a operacao de movimento estiver definida (nao null), retorna o caminho do campo 'geoPath' do movimento.<br>
	 * Caso contrario, retorna null.
	 * @return Lista de AbstractNodeLoc representando o caminho, ou null se indefinido.
	 */
	public List<AbstractNodeLoc> getGeoPath()
	{
		final MoveData move = _move;
		if ((move != null) && (move.geoPath != null))
		{
			return new ArrayList<>(move.geoPath);
		}
		return null;
	}
	
	/**
	 * @return True se a Creature esta conjurando.
	 */
	public boolean isCastingNow()
	{
		return _isCastingNow;
	}
	
	public void setCastingNow(boolean value)
	{
		_isCastingNow = value;
	}
	
	public boolean isCastingSimultaneouslyNow()
	{
		return _isCastingSimultaneouslyNow;
	}
	
	public void setCastingSimultaneouslyNow(boolean value)
	{
		_isCastingSimultaneouslyNow = value;
	}
	
	/**
	 * @return True se a conjuracao da Creature pode ser abortada.
	 */
	public boolean canAbortCast()
	{
		return _castInterruptTime > GameTimeTaskManager.getInstance().getGameTicks();
	}
	
	public int getCastInterruptTime()
	{
		return _castInterruptTime;
	}
	
	/**
	 * Verifica se a criatura esta atacando ou conjurando agora.
	 * @return {@code true} se a criatura esta atacando ou conjurando agora, {@code false} caso contrario
	 */
	public boolean isAttackingOrCastingNow()
	{
		return isAttackingNow() || isRangeAttackingNow() || isCastingNow() || isCastingSimultaneouslyNow();
	}
	
	/**
	 * Verifica se a criatura esta atacando agora.
	 * @return {@code true} se a criatura esta atacando agora, {@code false} caso contrario
	 */
	public boolean isAttackingNow()
	{
		return _attackEndTime > System.nanoTime();
	}
	
	/**
	 * @return True se a Creature esta atacando com uma arma de longo alcance.
	 */
	public final boolean isRangeAttackingNow()
	{
		return _disableBowAttackEndTime > GameTimeTaskManager.getInstance().getGameTicks();
	}
	
	/**
	 * Aborta o ataque da Creature e envia pacote Server->Client ActionFailed.
	 */
	public void abortAttack()
	{
		if (isAttackingNow())
		{
			sendPacket(ActionFailed.STATIC_PACKET);
		}
	}
	
	/**
	 * Aborta a conjuracao da Creature e envia pacote Server->Client MagicSkillCanceled/ActionFailed.
	 */
	public void abortCast()
	{
		if (_isCastingNow || _isCastingSimultaneouslyNow)
		{
			Future<?> future = _skillCast;
			
			// cancela a tarefa agendada de hit da skill
			if (future != null)
			{
				future.cancel(true);
				_skillCast = null;
			}
			
			future = _skillCast2;
			if (future != null)
			{
				future.cancel(true);
				_skillCast2 = null;
			}
			
			// TODO: Lidar com remocao de npc spawnado.
			if (isChanneling())
			{
				getSkillChannelizer().stopChanneling();
			}
			
			if (_allSkillsDisabled)
			{
				enableAllSkills(); // isto permanece para uso forcado de skill, ex: scroll of escape
			}
			
			setCastingNow(false);
			setCastingSimultaneouslyNow(false);
			
			// protecao para nao poder ser interrompido mais
			_castInterruptTime = 0;
			if (isPlayer())
			{
				getAI().notifyAction(Action.FINISH_CASTING); // restaurando a intencao anterior
			}
			
			broadcastPacket(new MagicSkillCanceled(getObjectId())); // envia pacote para parar animacoes no lado do cliente
			sendPacket(ActionFailed.STATIC_PACKET); // envia um pacote de "acao falhou" para o conjurador
		}
	}
	
	/**
	 * Atualiza a posicao da Creature durante um movimento e retorna True se o movimento terminou.<br>
	 * <br>
	 * <b><u>Conceito</u>:</b><br>
	 * <br>
	 * No inicio da acao de movimento, todas as propriedades do movimento sao armazenadas no objeto MoveData chamado <b>_move</b> da Creature.<br>
	 * A posicao do ponto inicial e do destino permitem estimar em funcao da velocidade de movimento o tempo para alcancar o destino.<br>
	 * Quando o movimento e iniciado (ex: por MovetoLocation), este metodo sera chamado a cada 0.1 seg para estimar e atualizar a posicao da Creature no servidor.<br>
	 * Note que a posicao atual do servidor pode diferir da posicao atual do cliente mesmo se cada movimento for em linha reta.<br>
	 * Por isso, o cliente envia regularmente um pacote Client->Server ValidatePosition para eventualmente corrigir a diferenca no servidor.<br>
	 * Mas, e sempre a posicao do servidor que e usada no calculo de alcance. Ao final do tempo estimado de movimento,<br>
	 * a posicao da Creature e automaticamente definida para a posicao de destino mesmo se o movimento nao terminou.<br>
	 * <font color=#FF0000><b><u>Cuidado</u>: A posicao Z atual e obtida DO CLIENTE pelo pacote Client->Server ValidatePosition.<br>
	 * Mas as posicoes x e y devem ser calculadas para evitar que jogadores tentem modificar sua velocidade de movimento.</b></font>
	 * @return True se o movimento terminou
	 */
	public boolean updatePosition()
	{
		if (!isSpawned())
		{
			_move = null;
			return true;
		}
		
		// Obtem dados de movimento
		final MoveData move = _move;
		if (move == null)
		{
			return true;
		}
		
		// Verifica se esta e a primeira atualizacao
		if (move.moveTimestamp == 0)
		{
			move.moveTimestamp = move.moveStartTime;
			move.xAccurate = getX();
			move.yAccurate = getY();
		}
		
		// Verifica se a posicao ja foi calculada
		final int gameTicks = GameTimeTaskManager.getInstance().getGameTicks();
		if (move.moveTimestamp == gameTicks)
		{
			return false;
		}
		
		final int xPrev = getX();
		final int yPrev = getY();
		final int zPrev = getZ(); // a coordenada z pode ser modificada por sincronizacoes de coordenadas
		double dx = move.xDestination - move.xAccurate;
		double dy = move.yDestination - move.yAccurate;
		double dz = move.zDestination - zPrev; // Coordenada Z seguira os valores do cliente
		
		if (isPlayer() && !_isFlying)
		{
			// No caso de movimento por cursor, evitar mover atraves de obstaculos.
			if (_cursorKeyMovement)
			{
				final double angle = LocationUtil.convertHeadingToDegree(getHeading());
				final double radian = Math.toRadians(angle);
				final double course = Math.toRadians(180);
				final double frontDistance = 10 * (_stat.getMoveSpeed() / 100);
				final int x1 = (int) (Math.cos(Math.PI + radian + course) * frontDistance);
				final int y1 = (int) (Math.sin(Math.PI + radian + course) * frontDistance);
				final int x = xPrev + x1;
				final int y = yPrev + y1;
				if (!GeoData.getInstance().canMove(xPrev, yPrev, zPrev, x, y, zPrev, getInstanceId()))
				{
					_move.onGeodataPathIndex = -1;
					stopMove(asPlayer().getLastServerPosition());
					return true;
				}
			}
			else // Movimento por clique do mouse.
			{
				// Para o movimento quando o jogador clicou longe e intersectou com um obstaculo.
				final double distance = Math.hypot(dx, dy);
				if (distance > 3000)
				{
					final double angle = LocationUtil.convertHeadingToDegree(getHeading());
					final double radian = Math.toRadians(angle);
					final double course = Math.toRadians(180);
					final double frontDistance = 10 * (_stat.getMoveSpeed() / 100);
					final int x1 = (int) (Math.cos(Math.PI + radian + course) * frontDistance);
					final int y1 = (int) (Math.sin(Math.PI + radian + course) * frontDistance);
					final int x = xPrev + x1;
					final int y = yPrev + y1;
					if (!GeoData.getInstance().canMove(xPrev, yPrev, zPrev, x, y, zPrev, getInstanceId()))
					{
						_move.onGeodataPathIndex = -1;
						if (hasAI())
						{
							if (getAI().isFollowing())
							{
								getAI().stopFollow();
							}
							
							getAI().setIntention(Intention.IDLE);
						}
						
						return true;
					}
				}
				else // Verifica portas ou cercas proximas.
				{
					if (hasAI() && (getAI().getIntention() == Intention.ATTACK)) // Suporte para ataque do jogador com movimento direto. Testado no retail em 11 de maio de 2023.
					{
						final double angle = LocationUtil.convertHeadingToDegree(getHeading());
						final double radian = Math.toRadians(angle);
						final double course = Math.toRadians(180);
						final double frontDistance = 10 * (_stat.getMoveSpeed() / 100);
						final int x1 = (int) (Math.cos(Math.PI + radian + course) * frontDistance);
						final int y1 = (int) (Math.sin(Math.PI + radian + course) * frontDistance);
						final int x = xPrev + x1;
						final int y = yPrev + y1;
						if (!GeoData.getInstance().canMove(xPrev, yPrev, zPrev, x, y, zPrev, getInstanceId()))
						{
							_move.onGeodataPathIndex = -1;
							broadcastPacket(new StopMove(this));
							return true;
						}
					}
					else // Verifica portas ou cercas proximas.
					{
						final WorldRegion region = getWorldRegion();
						if (region != null)
						{
							final boolean hasDoors = !region.getDoors().isEmpty();
							final boolean hasFences = !region.getFences().isEmpty();
							if (hasDoors || hasFences)
							{
								final double angle = LocationUtil.convertHeadingToDegree(getHeading());
								final double radian = Math.toRadians(angle);
								final double course = Math.toRadians(180);
								final double frontDistance = 10 * (_stat.getMoveSpeed() / 100);
								final int x1 = (int) (Math.cos(Math.PI + radian + course) * frontDistance);
								final int y1 = (int) (Math.sin(Math.PI + radian + course) * frontDistance);
								final int x = xPrev + x1;
								final int y = yPrev + y1;
								if ((hasDoors && DoorData.getInstance().checkIfDoorsBetween(xPrev, yPrev, zPrev, x, y, zPrev, getInstanceId(), false)) //
									|| (hasFences && FenceData.getInstance().checkIfFenceBetween(xPrev, yPrev, zPrev, x, y, zPrev, getInstanceId())))
								{
									_move.onGeodataPathIndex = -1;
									if (hasAI())
									{
										if (getAI().isFollowing())
										{
											getAI().stopFollow();
										}
										
										getAI().setIntention(Intention.IDLE);
									}
									
									stopMove(null);
									return true;
								}
							}
						}
					}
				}
			}
		}
		
		// Distancia do destino.
		double delta = (dx * dx) + (dy * dy);
		final boolean isFloating = _isFlying || (isInsideZone(ZoneId.WATER) && !isInsideZone(ZoneId.CASTLE));
		if (!isFloating && (delta < 10000) && ((dz * dz) > 2500)) // Perto o suficiente, permite erro entre geodata do cliente e servidor se nao puder ser evitado.
		{
			delta = Math.sqrt(delta);
		}
		else
		{
			delta = Math.sqrt(delta + (dz * dz));
		}
		
		// Previne que nao-jogaveis se teleportem para outra camada de terreno durante o movimento.
		// Aplica apenas se o Z de destino e significativamente diferente E a criatura ficaria flutuando no ar.
		// Isso permite movimento natural em rampas/inclinacoes enquanto previne teleporte de camada.
		if (!isPlayer() && !isFloating && (Math.abs(move.zDestination - zPrev) > 300))
		{
			// Verifica se o Z de destino e nivel de solo valido naquela posicao
			final int groundZ = GeoData.getInstance().getHeight(move.xDestination, move.yDestination, move.zDestination);
			// Se o Z de destino esta proximo do nivel do solo, e provavelmente uma inclinacao valida - permitir
			// Se o Z de destino esta longe do nivel do solo, e provavelmente um problema de camada - prevenir
			if (Math.abs(groundZ - move.zDestination) > 100)
			{
				move.zDestination = zPrev;
			}
		}
		
		// Colisao do alvo deve ser subtraida da distancia atual.
		final double collision;
		final WorldObject target = _target;
		if ((target != null) && target.isCreature() && hasAI() && (getAI().getIntention() == Intention.ATTACK))
		{
			collision = target.asCreature().getTemplate().getCollisionRadius();
		}
		else
		{
			collision = getTemplate().getCollisionRadius();
		}
		
		delta = Math.max(0.00001, delta - collision);
		
		double distFraction = Double.MAX_VALUE;
		if (delta > 1)
		{
			final double distPassed = (_stat.getMoveSpeed() * (gameTicks - move.moveTimestamp)) / GameTimeTaskManager.TICKS_PER_SECOND;
			distFraction = distPassed / delta;
		}
		
		final boolean arrived = distFraction > 1;
		if (arrived)
		{
			// Define a posicao da Creature para o destino.
			super.setXYZ(move.xDestination, move.yDestination, move.zDestination);
		}
		else
		{
			move.xAccurate += dx * distFraction;
			move.yAccurate += dy * distFraction;
			
			// Define a posicao da Creature para estimativa apos movimento parcial.
			super.setXYZ((int) move.xAccurate, (int) move.yAccurate, zPrev + (int) ((dz * distFraction) + 0.5));
		}
		
		revalidateZone(false);
		
		// Define o timer da ultima atualizacao de posicao para agora.
		move.moveTimestamp = gameTicks;
		
		// Envia MoveToLocation ao chegar.
		if (arrived && !isOnGeodataPath())
		{
			broadcastMoveToLocation(true);
		}
		else if (isAttackable() && (target != null)) // Attackable com alvo.
		{
			broadcastMoveToLocation();
		}
		
		return arrived;
	}
	
	public void revalidateZone(boolean force)
	{
		// Esta funcao e chamada com muita frequencia pelo codigo de movimento.
		if (!force && (calculateDistance3D(_lastZoneValidateLocation) < (isNpc() && !isInCombat() ? Config.MAX_DRIFT_RANGE : 100)))
		{
			return;
		}
		
		_lastZoneValidateLocation.setXYZ(this);
		
		final ZoneRegion region = ZoneManager.getInstance().getRegion(this);
		if (region != null)
		{
			region.revalidateZones(this);
		}
		else // Precaucao. Moveu para regiao invalida?
		{
			World.getInstance().disposeOutOfBoundsObject(this);
		}
	}
	
	/**
	 * Para o movimento da Creature (Chamado apenas pelo AI Accessor).<br>
	 * <br>
	 * <b><u>Acoes</u>:</b>
	 * <ul>
	 * <li>Deleta dados de movimento da Creature</li>
	 * <li>Define a posicao atual (x,y,z), sua WorldRegion atual se necessario e sua direcao</li>
	 * <li>Remove o objeto WorldObject do _gmList do GmListTable</li>
	 * <li>Remove o objeto do _knownObjects e _knownPlayer de todas as WorldRegion Creatures ao redor</li>
	 * </ul>
	 * <font color=#FF0000><b><u>Cuidado</u>: Este metodo NAO envia pacote Server->Client StopMove/StopRotation</b></font>
	 * @param loc
	 */
	public void stopMove(Location loc)
	{
		// Deleta dados de movimento da Creature.
		_move = null;
		_cursorKeyMovement = false;
		
		// Todos os dados estao contidos em um objeto Location.
		if (loc != null)
		{
			setXYZ(loc.getX(), loc.getY(), loc.getZ());
			setHeading(loc.getHeading());
			revalidateZone(true);
		}
		
		broadcastPacket(new StopMove(this));
	}
	
	/**
	 * @return Retorna o showSummonAnimation.
	 */
	public boolean isShowSummonAnimation()
	{
		return _showSummonAnimation;
	}
	
	/**
	 * @param showSummonAnimation O showSummonAnimation a definir.
	 */
	public void setShowSummonAnimation(boolean showSummonAnimation)
	{
		_showSummonAnimation = showSummonAnimation;
	}
	
	/**
	 * Define um WorldObject como alvo (adiciona o alvo ao _target, _knownObject da Creature e Creature ao _KnownObject do WorldObject).<br>
	 * <br>
	 * <b><u>Conceito</u>:</b><br>
	 * <br>
	 * O WorldObject (incluindo Creature) alvo e identificado em <b>_target</b> da Creature.<br>
	 * <br>
	 * <b><u>Acoes</u>:</b>
	 * <ul>
	 * <li>Define o _target da Creature para WorldObject</li>
	 * <li>Se necessario, adiciona WorldObject ao _knownObject da Creature</li>
	 * <li>Se necessario, adiciona Creature ao _KnownObject do WorldObject</li>
	 * <li>Se object==null, cancela Ataque ou Conjuracao</li>
	 * </ul>
	 * @param object L2object para definir como alvo
	 */
	public void setTarget(WorldObject object)
	{
		if ((object != null) && !object.isSpawned())
		{
			_target = null;
			return;
		}
		
		_target = object;
	}
	
	/**
	 * @return o identificador do WorldObject alvo ou -1.
	 */
	public int getTargetId()
	{
		if (_target != null)
		{
			return _target.getObjectId();
		}
		
		return 0;
	}
	
	/**
	 * @return o WorldObject alvo ou null.
	 */
	public WorldObject getTarget()
	{
		return _target;
	}
	
	/**
	 * Calcula dados de movimento para uma acao de mover para local e adiciona a Creature a MOVING_OBJECTS do MovementTaskManager (chamado apenas pelo AI Accessor).<br>
	 * <br>
	 * <b><u>Conceito</u>:</b><br>
	 * <br>
	 * No inicio da acao de movimento, todas as propriedades do movimento sao armazenadas no objeto MoveData chamado <b>_move</b> da Creature.<br>
	 * A posicao do ponto inicial e do destino permitem estimar em funcao da velocidade de movimento o tempo para alcancar o destino.<br>
	 * Todas as Creature em movimento sao identificadas em <b>MOVING_OBJECTS</b> do MovementTaskManager que chamara o metodo updatePosition dessas Creature a cada 0.1s.<br>
	 * <br>
	 * <b><u>Acoes</u>:</b>
	 * <ul>
	 * <li>Obtem a posicao atual da Creature</li>
	 * <li>Calcula a distancia (dx,dy) entre a posicao atual e o destino incluindo offset</li>
	 * <li>Cria e inicializa um objeto MoveData</li>
	 * <li>Define o objeto _move da Creature para o objeto MoveData</li>
	 * <li>Adiciona a Creature a MOVING_OBJECTS do MovementTaskManager</li>
	 * <li>Cria uma tarefa para notificar a AI que a Creature chegou a um ponto de verificacao do movimento</li>
	 * </ul>
	 * <font color=#FF0000><b><u>Cuidado</u>: Este metodo NAO envia pacote Server->Client MoveToPawn/MoveToLocation.</b></font><br>
	 * <br>
	 * <b><u>Exemplo de uso</u>:</b>
	 * <ul>
	 * <li>AI : onIntentionMoveTo(Location), onIntentionPickUp(WorldObject), onIntentionInteract(WorldObject)</li>
	 * <li>FollowTask</li>
	 * </ul>
	 * @param xValue A posicao X do destino
	 * @param yValue A posicao Y do destino
	 * @param zValue A posicao Z do destino
	 * @param offsetValue O tamanho da area de interacao da Creature alvo
	 */
	public void moveToLocation(int xValue, int yValue, int zValue, int offsetValue)
	{
		// Get the Move Speed of the Creature
		final double speed = _stat.getMoveSpeed();
		if ((speed <= 0) || isMovementDisabled())
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		int x = xValue;
		int y = yValue;
		int z = zValue;
		int offset = offsetValue;
		
		// Get current position of the Creature
		final int curX = getX();
		final int curY = getY();
		final int curZ = getZ();
		
		// Calcula a distancia (dx,dy) entre a posicao atual e o destino
		// TODO: melhorar suporte de movimento/seguimento no eixo Z quando dx,dy sao pequenos comparados a dz
		double dx = (x - curX);
		double dy = (y - curY);
		double dz = (z - curZ);
		double distance = Math.hypot(dx, dy);
		
		final boolean verticalMovementOnly = _isFlying && (distance == 0) && (dz != 0);
		if (verticalMovementOnly)
		{
			distance = Math.abs(dz);
		}
		
		// Faz movimento na agua curto e nao usa verificacoes de geodata para personagens nadando, distancia em um clique pode facilmente ter mais de 3000.
		final boolean isInWater = isInsideZone(ZoneId.WATER) && !isInsideZone(ZoneId.CASTLE);
		if (isInWater && (distance > 700))
		{
			final double divider = 700 / distance;
			x = curX + (int) (divider * dx);
			y = curY + (int) (divider * dy);
			z = curZ + (int) (divider * dz);
			dx = (x - curX);
			dy = (y - curY);
			dz = (z - curZ);
			distance = Math.hypot(dx, dy);
		}
		
		// @formatter:off
		// Define angulos de movimento necessarios
		// ^
		// |    X (x,y)
		// |   /
		// |  / distance
		// | /
		// |/ angle
		// X ---------->
		// (curx,cury)
		// @formatter:on
		
		double cos;
		double sin;
		
		// Verifica se um offset de movimento esta definido ou nao ha distancia a percorrer
		if ((offset > 0) || (distance < 1))
		{
			// aproximacao para mover mais perto quando coordenadas z sao diferentes
			// TODO: lidar melhor com movimento no eixo Z
			offset -= Math.abs(dz);
			if (offset < 5)
			{
				offset = 5;
			}
			
			// Se nao ha distancia a percorrer, o movimento e cancelado
			if ((distance < 1) || ((distance - offset) <= 0))
			{
				// Notifica a AI que a Creature chegou ao destino
				getAI().notifyAction(Action.ARRIVED);
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			
			// Calcula angulos de movimento necessarios
			sin = dy / distance;
			cos = dx / distance;
			distance -= (offset - 5); // devido a erro de arredondamento, temos que mover um pouco mais perto para estar no alcance
			
			// Calcula o novo destino com offset incluido
			x = curX + (int) (distance * cos);
			y = curY + (int) (distance * sin);
		}
		else
		{
			// Calcula angulos de movimento necessarios
			sin = dy / distance;
			cos = dx / distance;
		}
		
		// Cria e inicializa um objeto MoveData
		final MoveData move = new MoveData();
		
		// VERIFICACOES DE MOVIMENTO GEODATA E PATHFINDING
		final WorldRegion region = getWorldRegion();
		move.disregardingGeodata = (region == null) || !region.areNeighborsActive();
		move.onGeodataPathIndex = -1; // Inicializa nao no caminho geodata
		if (!move.disregardingGeodata && !_isFlying && !isInWater && !isVehicle() && !_cursorKeyMovement)
		{
			final boolean isInVehicle = isPlayer() && (asPlayer().getVehicle() != null);
			if (isInVehicle)
			{
				move.disregardingGeodata = true;
			}
			
			// Verificacoes de movimento.
			if ((Config.PATHFINDING > 0) && !(this instanceof QuestGuard))
			{
				int originalX = x;
				int originalY = y;
				final int originalZ = z;
				final double originalDistance = distance;
				final int gtx = (originalX - World.WORLD_X_MIN) >> 4;
				final int gty = (originalY - World.WORLD_Y_MIN) >> 4;
				if (isOnGeodataPath())
				{
					try
					{
						// Player seguindo uma rota de contorno para atacar: o destino ajustado pelo
						// perto do destino pedido (alvo nao se moveu muito), mantemos a rota.
						final boolean playerAttackPath = isPlayer() && hasAI() && (getAI().getIntention() == Intention.ATTACK);
						if ((playerAttackPath && (Math.hypot(originalX - _move.geoPathAccurateTx, originalY - _move.geoPathAccurateTy) < 200)) //
							|| ((gtx == _move.geoPathGtx) && (gty == _move.geoPathGty)))
						{
							sendPacket(ActionFailed.STATIC_PACKET);
							return;
						}
						
						_move.onGeodataPathIndex = -1; // Define nao no caminho geodata.
					}
					catch (NullPointerException e)
					{
					}
				}
				
				// Suporte para ataque do jogador com movimento direto. Testado no retail em 11 de maio de 2023.
				final boolean directMove = isPlayer() && hasAI() && (getAI().getIntention() == Intention.ATTACK);
				
				final boolean needGeoCheck = !directMove && !isInVehicle && // Nao no veiculo.
					!(isPlayer() && (distance > 3000)) && // Deve poder clicar longe e mover.
					!(isMonster() && (Math.abs(dz) > 500)) && // Monstros podem mover em saliencias.
					!(((curZ - z) > 300) && (distance < 300)); // Proibe corrigir destino se personagem quer cair.
				
				if (directMove || needGeoCheck)
				{
					if (directMove || (distance > 70))
					{
						// localizacao diferente se destino nao foi alcancado (ou apenas coordenada z e diferente)
						final Location destiny = GeoData.getInstance().moveCheck(curX, curY, curZ, x, y, z, getInstanceId());
						
						x = destiny.getX();
						y = destiny.getY();
						
						if (!isPlayer())
						{
							z = destiny.getZ();
						}
					}
					
					dx = x - curX;
					dy = y - curY;
					dz = z - curZ;
					
					distance = verticalMovementOnly ? Math.pow(dz, 2) : Math.hypot(dx, dy);
				}
				
				final int pathfindingThreshold = isPlayer() ? 30 : 15;
				final boolean dangerousFall = isMonster() && (Math.abs(dz) > 100) && (distance < 500);
				final boolean blockedInCombat = isAttackable() && isInCombat() && ((originalDistance - distance) > 5);
				
				// Player atacando alvo: com directMove o engine move em linha reta e PULA o pathfinding.
				// segue a rota (via moveToNextRoutePoint disparado pelo ARRIVED), sem travar.
				final Creature attackTarget = directMove ? getAI().getFollowTarget() : null;
				final boolean attackingMonster = (attackTarget != null) && attackTarget.isMonster();
				final int attackClearance = getTemplate().getCollisionRadius() + 50;
				final boolean blockedPlayerAttack = directMove && attackingMonster && !isOnGeodataPath() && !GeoData.getInstance().canMoveCorridor(curX, curY, curZ, originalX, originalY, originalZ, getInstanceId(), attackClearance);
				// Verificacao de Pathfinding.
				if ((blockedPlayerAttack || (!directMove && (((originalDistance - distance) > pathfindingThreshold) || dangerousFall || blockedInCombat))) && !isAfraid() && !isInVehicle)
				{
					// Calculo de caminho -- sobrescreve verificacao de movimento anterior
					move.geoPath = PathFinding.getInstance().findPath(curX, curY, curZ, originalX, originalY, originalZ, getInstanceId(), isPlayer());
					boolean found = (move.geoPath != null) && (move.geoPath.size() > 1);
					
					// Se caminho nao encontrado e este e um Attackable, tenta encontrar caminho mais proximo ao destino.
					if (!found && isAttackable())
					{
						int xMin = Math.min(curX, originalX);
						int xMax = Math.max(curX, originalX);
						int yMin = Math.min(curY, originalY);
						int yMax = Math.max(curY, originalY);
						final int maxDiff = Math.min(Math.max(xMax - xMin, yMax - yMin), 500);
						xMin -= maxDiff;
						xMax += maxDiff;
						yMin -= maxDiff;
						yMax += maxDiff;
						int destinationX = 0;
						int destinationY = 0;
						double shortDistance = Double.MAX_VALUE;
						double tempDistance;
						List<AbstractNodeLoc> tempPath;
						for (int sX = xMin; sX < xMax; sX += 500)
						{
							for (int sY = yMin; sY < yMax; sY += 500)
							{
								tempDistance = Math.hypot(sX - originalX, sY - originalY);
								if (tempDistance < shortDistance)
								{
									tempPath = PathFinding.getInstance().findPath(curX, curY, curZ, sX, sY, originalZ, getInstanceId(), false);
									found = (tempPath != null) && (tempPath.size() > 1);
									if (found)
									{
										shortDistance = tempDistance;
										move.geoPath = tempPath;
										destinationX = sX;
										destinationY = sY;
									}
								}
							}
						}
						
						found = (move.geoPath != null) && (move.geoPath.size() > 1);
						if (found)
						{
							originalX = destinationX;
							originalY = destinationY;
						}
					}
					
					if (found)
					{
						move.onGeodataPathIndex = 0; // No primeiro segmento.
						move.geoPathGtx = gtx;
						move.geoPathGty = gty;
						move.geoPathAccurateTx = originalX;
						move.geoPathAccurateTy = originalY;
						x = move.geoPath.get(move.onGeodataPathIndex).getX();
						y = move.geoPath.get(move.onGeodataPathIndex).getY();
						z = move.geoPath.get(move.onGeodataPathIndex).getZ();
						dx = x - curX;
						dy = y - curY;
						dz = z - curZ;
						distance = verticalMovementOnly ? Math.pow(dz, 2) : Math.hypot(dx, dy);
						sin = dy / distance;
						cos = dx / distance;
					}
					else // Nenhum caminho encontrado.
					{
						// Quando nenhum caminho de movimento foi encontrado, usa movimento direto. Testado no retail em 21 de outubro de 2024.
						// if (isPlayer() && !_isFlying && !isInWater)
						// {
						// sendPacket(ActionFailed.STATIC_PACKET);
						// return;
						// }
						
						move.disregardingGeodata = true;
						x = originalX;
						y = originalY;
						z = originalZ;
						distance = originalDistance;
					}
				}
				
				// Verifica destino ao usar movimento com mouse e nenhum caminho e encontrado.
				if (isPlayable() && !_cursorKeyMovement && (move.geoPath == null))
				{
					final Location destiny = GeoData.getInstance().moveCheck(curX, curY, curZ, x, y, z, getInstanceId());
					x = destiny.getX();
					y = destiny.getY();
					z = destiny.getZ();
					dx = x - curX;
					dy = y - curY;
					dz = z - curZ;
					distance = verticalMovementOnly ? Math.pow(dz, 2) : Math.hypot(dx, dy);
				}
			}
			
			// Se nao ha distancia a percorrer, o movimento e cancelado
			if ((distance < 1) && ((Config.PATHFINDING > 0) || isPlayable()))
			{
				if (isSummon())
				{
					// Nao interromper seguindo o dono.
					if (getAI().getFollowTarget() != asPlayer())
					{
						asSummon().setFollowStatus(false);
						getAI().setIntention(Intention.IDLE);
					}
				}
				else
				{
					getAI().setIntention(Intention.IDLE);
				}
				
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
		}
		
		// Aplica distancia Z para voar ou nadar para calculos de tempo corretos
		if ((_isFlying || isInWater) && !verticalMovementOnly)
		{
			distance = Math.hypot(distance, dz);
		}
		
		// Calcula o numero de ticks entre a posicao atual e o destino.
		final int ticksToMove = (int) ((GameTimeTaskManager.TICKS_PER_SECOND * distance) / speed);
		move.xDestination = x;
		move.yDestination = y;
		move.zDestination = z; // isso e o que foi solicitado pelo cliente
		
		// Calcula e define a direcao da Creature
		move.heading = 0; // valor inicial para sincronizacao de coordenadas
		
		// Nao quebra a direcao em movimentos verticais
		if (!verticalMovementOnly)
		{
			setHeading(LocationUtil.calculateHeadingFrom(cos, sin));
		}
		
		move.moveStartTime = GameTimeTaskManager.getInstance().getGameTicks();
		
		// Define o objeto _move da Creature para o objeto MoveData
		_move = move;
		
		// Adiciona a Creature aos objetos em movimento do MovementTaskManager.
		// O MovementTaskManager gerencia o movimento de objetos.
		MovementTaskManager.getInstance().registerMovingObject(this);
		
		// Create a task to notify the AI that Creature arrives at a check point of the movement
		if ((ticksToMove * GameTimeTaskManager.MILLIS_IN_TICK) > 3000)
		{
			ThreadPool.schedule(new NotifyAITask(this, Action.ARRIVED_REVALIDATE), 2000);
		}
		
		// the Event.ARRIVED will be sent when the character will actually arrive to destination by MovementTaskManager
	}
	
	/**
	 * Mover para o proximo ponto da rota.
	 * @return true, se bem sucedido
	 */
	public boolean moveToNextRoutePoint()
	{
		final MoveData move = _move;
		if (move == null)
		{
			return false;
		}
		
		if (!isOnGeodataPath(move))
		{
			// Cancela a acao de movimento
			_move = null;
			return false;
		}
		
		// Obtem a Velocidade de Movimento da Creature
		final double speed = _stat.getMoveSpeed();
		if ((speed <= 0) || isMovementDisabled())
		{
			// Cancela a acao de movimento
			_move = null;
			return false;
		}
		
		// Obtem a posicao atual da Creature
		final int curX = getX();
		final int curY = getY();
		
		// Cria e inicializa um objeto MoveData
		final MoveData newMove = new MoveData();
		
		// Atualiza o objeto MoveData
		newMove.onGeodataPathIndex = move.onGeodataPathIndex + 1; // proximo segmento
		newMove.geoPath = move.geoPath;
		newMove.geoPathGtx = move.geoPathGtx;
		newMove.geoPathGty = move.geoPathGty;
		newMove.geoPathAccurateTx = move.geoPathAccurateTx;
		newMove.geoPathAccurateTy = move.geoPathAccurateTy;
		if (move.onGeodataPathIndex == (move.geoPath.size() - 2))
		{
			newMove.xDestination = move.geoPathAccurateTx;
			newMove.yDestination = move.geoPathAccurateTy;
			newMove.zDestination = move.geoPath.get(newMove.onGeodataPathIndex).getZ();
		}
		else
		{
			newMove.xDestination = move.geoPath.get(newMove.onGeodataPathIndex).getX();
			newMove.yDestination = move.geoPath.get(newMove.onGeodataPathIndex).getY();
			newMove.zDestination = move.geoPath.get(newMove.onGeodataPathIndex).getZ();
		}
		
		// Calcula e define a direcao da Creature.
		final double distance = Math.hypot(newMove.xDestination - curX, newMove.yDestination - curY);
		if (distance != 0)
		{
			setHeading(LocationUtil.calculateHeadingFrom(curX, curY, newMove.xDestination, newMove.yDestination));
		}
		
		// Calcula o numero de ticks entre a posicao atual e o destino.
		final int ticksToMove = (int) ((GameTimeTaskManager.TICKS_PER_SECOND * distance) / speed);
		newMove.heading = 0; // valor inicial para sincronizacao de coordenadas
		newMove.moveStartTime = GameTimeTaskManager.getInstance().getGameTicks();
		
		// Define o objeto _move da Creature para o objeto MoveData
		_move = newMove;
		
		// Adiciona a Creature aos objetos em movimento do MovementTaskManager.
		// O MovementTaskManager gerencia o movimento de objetos.
		MovementTaskManager.getInstance().registerMovingObject(this);
		
		// Cria uma tarefa para notificar a AI que a Creature chegou a um ponto de verificacao do movimento
		if ((ticksToMove * GameTimeTaskManager.MILLIS_IN_TICK) > 3000)
		{
			ThreadPool.schedule(new NotifyAITask(this, Action.ARRIVED_REVALIDATE), 2000);
		}
		
		// o Event.ARRIVED sera enviado quando o personagem realmente chegar ao destino pelo MovementTaskManager
		
		// Envia um pacote Server->Client MoveToLocation para o ator e todos os Player em seus _knownPlayers
		broadcastMoveToLocation(true);
		return true;
	}
	
	/**
	 * Valida a direcao do movimento.
	 * @param heading a direcao
	 * @return true, se bem sucedido
	 */
	public boolean validateMovementHeading(int heading)
	{
		final MoveData move = _move;
		if (move == null)
		{
			return true;
		}
		
		boolean result = true;
		if (move.heading != heading)
		{
			result = (move.heading == 0); // valor inicial ou falso
			move.heading = heading;
		}
		
		return result;
	}
	
	/**
	 * Verifica se este objeto esta dentro do raio 2D dado ao redor do ponto dado.
	 * @param loc Localizacao do alvo
	 * @param radius o raio ao redor do alvo
	 * @return true se a Creature esta dentro do raio.
	 */
	public boolean isInsideRadius2D(ILocational loc, int radius)
	{
		return isInsideRadius2D(loc.getX(), loc.getY(), loc.getZ(), radius);
	}
	
	/**
	 * Verifica se este objeto esta dentro do raio 2D dado ao redor do ponto dado.
	 * @param x Posicao X do alvo
	 * @param y Posicao Y do alvo
	 * @param z Posicao Z do alvo
	 * @param radius o raio ao redor do alvo
	 * @return true se a Creature esta dentro do raio.
	 */
	public boolean isInsideRadius2D(int x, int y, int z, int radius)
	{
		return calculateDistance2D(x, y, z) < radius;
	}
	
	/**
	 * Verifica se este objeto esta dentro do raio 3D dado ao redor do ponto dado.
	 * @param loc Localizacao do alvo
	 * @param radius o raio ao redor do alvo
	 * @return true se a Creature esta dentro do raio.
	 */
	public boolean isInsideRadius3D(ILocational loc, int radius)
	{
		return isInsideRadius3D(loc.getX(), loc.getY(), loc.getZ(), radius);
	}
	
	/**
	 * Verifica se este objeto esta dentro do raio 3D dado ao redor do ponto dado.
	 * @param x Posicao X do alvo
	 * @param y Posicao Y do alvo
	 * @param z Posicao Z do alvo
	 * @param radius o raio ao redor do alvo
	 * @return true se a Creature esta dentro do raio.
	 */
	public boolean isInsideRadius3D(int x, int y, int z, int radius)
	{
		return calculateDistance3D(x, y, z) < radius;
	}
	
	/**
	 * <b><u>Sobrescrito em</u>:</b>
	 * <li>Player</li>
	 * @return True se flechas estao disponiveis.
	 */
	protected boolean checkAndEquipArrows()
	{
		return true;
	}
	
	/**
	 * <b><u>Sobrescrito em</u>:</b>
	 * <li>Player</li>
	 * @return True se virotes estao disponiveis.
	 */
	protected boolean checkAndEquipBolts()
	{
		return true;
	}
	
	/**
	 * Adiciona Exp e Sp a Creature.<br>
	 * <br>
	 * <b><u>Sobrescrito em</u>:</b>
	 * <li>Player</li>
	 * <li>Pet</li><br>
	 * @param addToExp
	 * @param addToSp
	 */
	public synchronized void addExpAndSp(double addToExp, double addToSp)
	{
		// Metodo vazio (sobrescrito por jogadores e pets)
	}
	
	/**
	 * <b><u>Sobrescrito em</u>:</b>
	 * <li>Player</li>
	 * @return a instancia da arma ativa (sempre equipada na mao direita).
	 */
	public abstract Item getActiveWeaponInstance();
	
	/**
	 * <b><u>Sobrescrito em</u>:</b>
	 * <li>Player</li>
	 * @return o item da arma ativa (sempre equipado na mao direita).
	 */
	public abstract Weapon getActiveWeaponItem();
	
	/**
	 * <b><u>Sobrescrito em</u>:</b>
	 * <li>Player</li>
	 * @return a instancia da arma secundaria (sempre equipada na mao esquerda).
	 */
	public abstract Item getSecondaryWeaponInstance();
	
	/**
	 * <b><u>Sobrescrito em</u>:</b>
	 * <li>Player</li>
	 * @return o item secundario {@link ItemTemplate} (sempre equipado na mao esquerda).
	 */
	public abstract ItemTemplate getSecondaryWeaponItem();
	
	/**
	 * Gerencia o processo de hit (chamado pela Hit Task).<br>
	 * <br>
	 * <b><u>Acoes</u>:</b>
	 * <ul>
	 * <li>Se o atacante/alvo esta morto ou usa fake death, notifica a AI com CANCEL e envia um pacote Server->Client ActionFailed (se o atacante for um Player)</li>
	 * <li>Se o ataque nao foi abortado, envia uma mensagem de sistema (golpe critico, errou...) para atacante/alvo se forem Player</li>
	 * <li>Se o ataque nao foi abortado e o golpe nao errou, reduz HP do alvo e calcula dano refletido para reduzir HP do atacante se necessario</li>
	 * <li>Se o ataque nao foi abortado e o golpe nao errou, gerencia interrupcao de ataque ou conjuracao do alvo (calculando taxa, enviando mensagem...)</li>
	 * </ul>
	 * @param target A Creature alvo
	 * @param damageValue Numero de HP a reduzir
	 * @param crit True se o golpe e critico
	 * @param miss True se o golpe errou
	 * @param shld True se o escudo e eficiente
	 * @param soulshot True se SoulShot estao carregados
	 * @param rechargeShots True se SoulShots sao recarregados
	 */
	public void onHitTimer(Creature target, int damageValue, boolean crit, boolean miss, byte shld, boolean soulshot, boolean rechargeShots)
	{
		// Se o atacante/alvo esta morto ou usa fake death, notifica a AI com CANCEL
		// e envia um pacote Server->Client ActionFailed (se o atacante for um Player)
		if ((target == null) || isAlikeDead())
		{
			getAI().notifyAction(Action.CANCEL);
			return;
		}
		
		// Verifica se fake players devem agredir uns aos outros.
		if ((isNpc() && target.isAlikeDead()) || target.isDead() || (!isInSurroundingRegion(target) && !isDoor()))
		{
			// getAI().setIntention(Intention.ACTIVE, null);
			// As vezes o ataque e processado mas o alvo morre antes do hit
			// Entao precisamos recarregar shots para o proximo ataque
			if (rechargeShots)
			{
				rechargeShots(true, false);
			}
			
			getAI().notifyAction(Action.CANCEL);
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (miss)
		{
			// Notify target AI
			if (target.hasAI())
			{
				target.getAI().notifyAction(Action.EVADED, this);
			}
			
			notifyAttackAvoid(target, false);
		}
		// else
		// {
		// Se nao erramos o hit, descarrega os soulshots, se houver
		// setChargedShot(ShotType.SOULSHOTS, false);
		// }
		
		// Envia mensagem sobre dano/critico ou erro
		int damage = damageValue;
		sendDamageMessage(target, damage, false, crit, miss);
		
		// Verifica Raidboss ataque Creature sera petrificada se atacar um raid que tem mais de 8 niveis abaixo
		if (target.isRaid() && target.giveRaidCurse() && !Config.RAID_DISABLE_CURSE && (getLevel() > (target.getLevel() + 8)))
		{
			final Skill skill = CommonSkill.RAID_CURSE2.getSkill();
			if (skill != null)
			{
				abortAttack();
				abortCast();
				getAI().setIntention(Intention.IDLE);
				skill.applyEffects(target, this);
			}
			else
			{
				LOGGER.warning("Skill 4515 at level 1 is missing in DP.");
			}
			
			damage = 0; // previne estragar o calculo de drop
		}
		
		// Se o alvo da Creature e um Player, envia uma mensagem de sistema
		if (target.isPlayer())
		{
			final Player enemy = target.asPlayer();
			enemy.getAI().clientStartAutoAttack();
		}
		
		if (!miss && (damage > 0))
		{
			final Weapon weapon = getActiveWeaponItem();
			final boolean isBow = ((weapon != null) && ((weapon.getItemType() == WeaponType.BOW) || (weapon.getItemType() == WeaponType.CROSSBOW)));
			int reflectedDamage = 0;
			if (!isBow && !target.isInvul()) // Nao reflete se a arma for do tipo arco ou alvo for invulneravel
			{
				// correcao rapida para sem drop de raid se boss ataca personagem de alto nivel com reflexao de dano
				if (!target.isRaid() || (asPlayer() == null) || (asPlayer().getLevel() <= (target.getLevel() + 8)))
				{
					// Reduz HP do alvo e calcula dano refletido para reduzir HP do atacante se necessario
					final double reflectPercent = target.getStat().calcStat(Stat.REFLECT_DAMAGE_PERCENT, 0, null, null);
					if (reflectPercent > 0)
					{
						reflectedDamage = (int) ((reflectPercent / 100.) * damage);
						if (reflectedDamage > target.getMaxHp())
						{
							reflectedDamage = target.getMaxHp();
						}
					}
				}
			}
			
			// reduz HP do alvo
			target.reduceCurrentHp(damage, this, null);
			target.notifyDamageReceived(damage, this, null, crit, false);
			if (reflectedDamage > 0)
			{
				reduceCurrentHp(reflectedDamage, target, true, false, null);
				notifyDamageReceived(reflectedDamage, target, null, crit, false);
			}
			
			if (!isBow) // Nao absorve se a arma for do tipo arco
			{
				// Absorve HP do dano infligido
				double absorbPercent = _stat.calcStat(Stat.ABSORB_DAMAGE_PERCENT, 0, null, null);
				if (absorbPercent > 0)
				{
					final int maxCanAbsorb = (int) (_stat.getMaxRecoverableHp() - _status.getCurrentHp());
					int absorbDamage = (int) ((absorbPercent / 100.) * damage);
					if (absorbDamage > maxCanAbsorb)
					{
						absorbDamage = maxCanAbsorb; // Nao pode absorver mais que o hp maximo
					}
					
					if (absorbDamage > 0)
					{
						setCurrentHp(_status.getCurrentHp() + absorbDamage);
					}
				}
				
				// Absorve MP do dano infligido
				absorbPercent = _stat.calcStat(Stat.ABSORB_MANA_DAMAGE_PERCENT, 0, null, null);
				if (absorbPercent > 0)
				{
					final int maxCanAbsorb = (int) (_stat.getMaxRecoverableMp() - _status.getCurrentMp());
					int absorbDamage = (int) ((absorbPercent / 100.) * damage);
					if (absorbDamage > maxCanAbsorb)
					{
						absorbDamage = maxCanAbsorb; // Nao pode absorver mais que o mp maximo
					}
					
					if (absorbDamage > 0)
					{
						setCurrentMp(_status.getCurrentMp() + absorbDamage);
					}
				}
			}
			
			// Notifica a AI com ATTACKED
			if (target.hasAI())
			{
				target.getAI().notifyAction(Action.ATTACKED, this);
			}
			
			getAI().clientStartAutoAttack();
			
			if (isSummon())
			{
				final Player owner = asSummon().getOwner();
				if (owner != null)
				{
					owner.getAI().clientStartAutoAttack();
				}
			}
			
			// Gerencia interrupcao de ataque ou conjuracao do alvo (calculando taxa, enviando mensagem...)
			if (!target.isRaid() && Formulas.calcAtkBreak(target, damage))
			{
				target.breakAttack();
				target.breakCast();
			}
			
			if (!_triggerSkills.isEmpty())
			{
				for (OptionSkillHolder holder : _triggerSkills.values())
				{
					if (((!crit && (holder.getSkillType() == OptionSkillType.ATTACK)) || ((holder.getSkillType() == OptionSkillType.CRITICAL) && crit)) && (Rnd.get(100) < holder.getChance()))
					{
						makeTriggerCast(holder.getSkill(), target);
					}
				}
			}
			
			// Executa efeito de habilidade especial onCritical da arma se disponivel
			if (crit && (weapon != null))
			{
				weapon.castOnCriticalSkill(this, target);
			}
		}
		
		// Recarrega quaisquer tarefas ativas de auto-soulshot para a criatura atual.
		if (rechargeShots)
		{
			rechargeShots(true, false);
		}
	}
	
	/**
	 * Interrompe um ataque e envia pacote Server->Client ActionFailed e uma Mensagem de Sistema para a Creature.
	 */
	public void breakAttack()
	{
		if (isAttackingNow())
		{
			// Aborta o ataque da Creature e envia pacote Server->Client ActionFailed
			abortAttack();
			
			if (isPlayer())
			{
				// Envia uma mensagem de sistema
				sendPacket(SystemMessageId.YOUR_ATTACK_HAS_FAILED);
			}
		}
	}
	
	/**
	 * Interrompe uma conjuracao e envia pacote Server->Client ActionFailed e uma Mensagem de Sistema para a Creature.
	 */
	public void breakCast()
	{
		// dano so pode cancelar skills magicas e estaticas
		if (_isCastingNow && canAbortCast() && (_lastSkillCast != null) && (_lastSkillCast.isMagic() || _lastSkillCast.isStatic()))
		{
			// Aborta a conjuracao da Creature e envia pacote Server->Client MagicSkillCanceled/ActionFailed.
			abortCast();
			
			if (isPlayer())
			{
				// Envia uma mensagem de sistema
				sendPacket(SystemMessageId.YOUR_CASTING_HAS_BEEN_INTERRUPTED);
			}
		}
	}
	
	/**
	 * Reduz o numero de flechas da Creature.<br>
	 * <br>
	 * <b><u>Sobrescrito em</u>:</b>
	 * <li>Player</li><br>
	 * @param bolts
	 */
	protected void reduceArrowCount(boolean bolts)
	{
		// padrao e nao fazer nada
	}
	
	/**
	 * Gerencia ataque Forcado (shift + selecionar alvo).<br>
	 * <br>
	 * <b><u>Acoes</u>:</b>
	 * <ul>
	 * <li>Se a Creature ou alvo esta em area de cidade, envia mensagem de sistema TARGET_IN_PEACEZONE e pacote Server->Client ActionFailed</li>
	 * <li>Se o alvo esta confuso, envia pacote Server->Client ActionFailed</li>
	 * <li>Se a Creature e um Artefato, envia pacote Server->Client ActionFailed</li>
	 * <li>Envia pacote Server->Client MyTargetSelected para iniciar ataque e Notifica AI com ATTACK</li>
	 * </ul>
	 * @param player O Player para atacar
	 */
	@Override
	public void onForcedAttack(Player player)
	{
		if (isInsidePeaceZone(player) && !isInTownWarEvent())
		{
			// Se a Creature ou alvo esta em zona de paz, envia mensagem de sistema TARGET_IN_PEACEZONE e pacote Server->Client ActionFailed
			player.sendPacket(SystemMessageId.YOU_MAY_NOT_ATTACK_THIS_TARGET_IN_A_PEACEFUL_ZONE);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (player.isInOlympiadMode() && (player.getTarget() != null) && player.getTarget().isPlayable())
		{
			Player target = null;
			final WorldObject object = player.getTarget();
			if ((object != null) && object.isPlayable())
			{
				target = object.asPlayer();
			}
			
			if ((target == null) || (target.isInOlympiadMode() && (!player.isOlympiadStart() || (player.getOlympiadGameId() != target.getOlympiadGameId()))))
			{
				// se o Player esta na Olimpiada e a partida ainda nao comecou, envia pacote Server->Client ActionFailed
				player.sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
		}
		
		if ((player.getTarget() != null) && !player.getTarget().canBeAttacked() && !player.getAccessLevel().allowPeaceAttack() && !player.isInTownWarEvent())
		{
			// Se o alvo nao e atacavel, envia pacote Server->Client ActionFailed
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (player.isConfused())
		{
			// Se o alvo esta confuso, envia pacote Server->Client ActionFailed
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		// Verificacao de Linha de Visao GeoData ou dz > 1000
		if (!GeoData.getInstance().canSeeTarget(player, this))
		{
			player.sendPacket(SystemMessageId.CANNOT_SEE_TARGET);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (player.getBlockCheckerArena() != -1)
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		// Notifica AI com ATTACK
		player.getAI().setIntention(Intention.ATTACK, this);
	}
	
	/**
	 * @param attacker
	 * @return True se dentro de zona de paz.
	 */
	public boolean isInsidePeaceZone(Player attacker)
	{
		if (attacker.isPlayer() && attacker.isInTownWarEvent())
		{
			return false;
		}
		
		return isInsidePeaceZone(attacker, this);
	}
	
	public boolean isInsidePeaceZone(Player attacker, WorldObject target)
	{
		if (isInTownWarEvent())
		{
			return false;
		}
		
		return (!attacker.getAccessLevel().allowPeaceAttack() && isInsidePeaceZone((WorldObject) attacker, target));
	}
	
	public boolean isInsidePeaceZone(WorldObject attacker, WorldObject target)
	{
		if ((target == null) || !(target.isPlayable() && attacker.isPlayable()) || isInTownWarEvent() || InstanceManager.getInstance().getInstance(getInstanceId()).isPvP())
		{
			return false;
		}
		
		if (TerritoryWarManager.PLAYER_WITH_WARD_CAN_BE_KILLED_IN_PEACEZONE && TerritoryWarManager.getInstance().isTWInProgress() && target.isPlayer() && target.asPlayer().isCombatFlagEquipped())
		{
			return false;
		}
		
		if (Config.ALT_GAME_KARMA_PLAYER_CAN_BE_KILLED_IN_PEACEZONE)
		{
			// Permite red ser atacado e red atacar jogadores flagged.
			final Player player = asPlayer();
			if ((player != null) && (player.getKarma() > 0))
			{
				return false;
			}
			
			final Player attackerPlayer = attacker.asPlayer();
			if ((attackerPlayer != null) && (attackerPlayer.getKarma() > 0) && (player != null) && (player.getPvpFlag() > 0))
			{
				return false;
			}
		}
		
		return (target.isInsideZone(ZoneId.PEACE) || attacker.isInsideZone(ZoneId.PEACE) || target.isInsideZone(ZoneId.NO_PVP) || attacker.isInsideZone(ZoneId.NO_PVP));
	}
	
	/**
	 * @return true se este personagem esta dentro de uma grid ativa.
	 */
	public boolean isInActiveRegion()
	{
		final WorldRegion region = getWorldRegion();
		return ((region != null) && (region.isActive()));
	}
	
	/**
	 * @return True se a Creature tem um Grupo em andamento.
	 */
	public boolean isInParty()
	{
		return false;
	}
	
	/**
	 * @return o objeto Party da Creature.
	 */
	public Party getParty()
	{
		return null;
	}
	
	/**
	 * @return a Velocidade de Ataque da Creature (atraso (em milissegundos) antes do proximo ataque).
	 */
	public int calculateTimeBetweenAttacks()
	{
		return (int) (500000 / _stat.getPAtkSpd());
	}
	
	/**
	 * @param weapon
	 * @return o Tempo de Reutilizacao do Ataque (usado para atraso de arco)
	 */
	public int calculateReuseTime(Weapon weapon)
	{
		if (isTransformed())
		{
			switch (getAttackType())
			{
				case BOW:
				{
					return (int) ((1500 * 333 * _stat.getWeaponReuseModifier(null)) / _stat.getPAtkSpd());
				}
				case CROSSBOW:
				{
					return (int) ((1200 * 333 * _stat.getWeaponReuseModifier(null)) / _stat.getPAtkSpd());
				}
			}
		}
		
		if ((weapon == null) || (weapon.getReuseDelay() == 0))
		{
			return 0;
		}
		
		return (int) ((weapon.getReuseDelay() * 333) / _stat.getPAtkSpd());
	}
	
	/**
	 * Adiciona uma skill ao _skills da Creature e seus objetos Func ao conjunto de calculadores da Creature.<br>
	 * <br>
	 * <b><u>Conceito</u>:</b><br>
	 * <br>
	 * Todas as skills pertencentes a uma Creature sao identificadas em <b>_skills</b><br>
	 * <br>
	 * <b><u>Acoes</u>:</b>
	 * <ul>
	 * <li>Substitui oldSkill por newSkill ou Adiciona a newSkill</li>
	 * <li>Se uma skill antiga foi substituida, remove todos seus objetos Func do conjunto de calculadores da Creature</li>
	 * <li>Adiciona objetos Func da newSkill ao conjunto de calculadores da Creature</li>
	 * </ul>
	 * <br>
	 * <b><u>Sobrescrito em</u>:</b>
	 * <ul>
	 * <li>Player : Salva atualizacao na tabela character_skills do banco de dados</li>
	 * </ul>
	 * @param newSkill A Skill para adicionar a Creature
	 * @return A Skill substituida ou null se apenas adicionou uma nova Skill
	 */
	public Skill addSkill(Skill newSkill)
	{
		Skill oldSkill = null;
		if (newSkill != null)
		{
			// Substitui oldSkill por newSkill ou Adiciona a newSkill
			oldSkill = _skills.put(newSkill.getId(), newSkill);
			
			// Se uma skill antiga foi substituida, remove todos seus objetos Func
			if (oldSkill != null)
			{
				removeStatsOwner(oldSkill);
				
				if (oldSkill.isPassive())
				{
					stopSkillEffects(SkillFinishType.NORMAL, oldSkill.getId());
				}
			}
			
			// Adiciona objetos Func da newSkill ao conjunto de calculadores da Creature
			addStatFuncs(newSkill.getStatFuncs(null, this));
			if (newSkill.isPassive())
			{
				newSkill.applyEffects(this, this, false, true, false, 0);
			}
		}
		
		return oldSkill;
	}
	
	public Skill removeSkill(Skill skill, boolean cancelEffect)
	{
		return (skill != null) ? removeSkill(skill.getId(), cancelEffect) : null;
	}
	
	public Skill removeSkill(int skillId)
	{
		return removeSkill(skillId, true);
	}
	
	public Skill removeSkill(int skillId, boolean cancelEffect)
	{
		// Remove a skill do _skills da Creature
		final Skill oldSkill = _skills.remove(skillId);
		
		// Remove todos seus objetos Func do conjunto de calculadores da Creature
		if (oldSkill != null)
		{
			// Para a conjuracao se esta skill esta sendo usada agora
			if ((_lastSkillCast != null) && _isCastingNow && (oldSkill.getId() == _lastSkillCast.getId()))
			{
				abortCast();
			}
			
			if ((_lastSimultaneousSkillCast != null) && _isCastingSimultaneouslyNow && (oldSkill.getId() == _lastSimultaneousSkillCast.getId()))
			{
				abortCast();
			}
			
			// Para os efeitos.
			if (cancelEffect || oldSkill.isToggle() || oldSkill.isPassive())
			{
				removeStatsOwner(oldSkill);
				stopSkillEffects(SkillFinishType.REMOVED, oldSkill.getId());
			}
		}
		
		return oldSkill;
	}
	
	/**
	 * <b><u>Conceito</u>:</b><br>
	 * <br>
	 * Todas as skills pertencentes a uma Creature sao identificadas em <b>_skills</b> da Creature
	 * @return todas as skills da Creature em uma tabela de Skill.
	 */
	public Collection<Skill> getAllSkills()
	{
		return _skills.values();
	}
	
	/**
	 * @return o mapa contendo as skills deste personagem.
	 */
	public Map<Integer, Skill> getSkills()
	{
		return _skills;
	}
	
	/**
	 * Retorna o nivel de uma skill pertencente a Creature.
	 * @param skillId O identificador da Skill cujo nivel deve ser retornado
	 * @return O nivel da Skill identificada por skillId
	 */
	public int getSkillLevel(int skillId)
	{
		final Skill skill = getKnownSkill(skillId);
		return (skill == null) ? 0 : skill.getLevel();
	}
	
	/**
	 * @param skillId O identificador da Skill para verificar o conhecimento
	 * @return a skill a partir da skill conhecida.
	 */
	public Skill getKnownSkill(int skillId)
	{
		return _skills.get(skillId);
	}
	
	/**
	 * Retorna o numero de buffs afetando esta Creature.
	 * @return O numero de Buffs afetando esta Creature
	 */
	public int getBuffCount()
	{
		return _effectList.getBuffCount();
	}
	
	public int getDanceCount()
	{
		return _effectList.getDanceCount();
	}
	
	/**
	 * Gerencia a tarefa de lancamento de skill magica (consumo de MP, HP, Item...) e exibe a animacao da skill magica no cliente.<br>
	 * <br>
	 * <b><u>Acoes</u>:</b>
	 * <ul>
	 * <li>Envia pacote Server->Client MagicSkillLaunched (para exibir animacao de skill magica) para todos os Player de _knownPlayers da Creature</li>
	 * <li>Consome MP, HP e Item se necessario</li>
	 * <li>Envia pacote Server->Client StatusUpdate com modificacao de MP para o Player</li>
	 * <li>Executa a skill magica para calcular seus efeitos</li>
	 * <li>Se o tipo da skill e PDAM, notifica a AI do alvo com ATTACK</li>
	 * <li>Notifica a AI da Creature com FINISH_CASTING</li>
	 * </ul>
	 * <font color=#FF0000><b><u>Cuidado</u>: Uma conjuracao de skill magica DEVE estar em andamento</b></font>
	 * @param mut
	 */
	public void onMagicLaunchedTimer(MagicUseTask mut)
	{
		final Skill skill = mut.getSkill();
		final List<WorldObject> targets = mut.getTargets();
		if ((skill == null) || (targets == null))
		{
			abortCast();
			return;
		}
		
		if (targets.isEmpty())
		{
			switch (skill.getTargetType())
			{
				// apenas skills do tipo AURA podem ser conjuradas sem alvo
				case AURA:
				case FRONT_AURA:
				case BEHIND_AURA:
				case AURA_CORPSE_MOB:
				case AURA_FRIENDLY:
				{
					break;
				}
				default:
				{
					abortCast();
					return;
				}
			}
		}
		
		// Verificacao de escape do raio da skill e zona de paz. Primeira versao, nao perfeita em skills AoE.
		int escapeRange = 0;
		if (skill.getEffectRange() > escapeRange)
		{
			escapeRange = skill.getEffectRange();
		}
		else if ((skill.getCastRange() < 0) && (skill.getAffectRange() > 80))
		{
			escapeRange = skill.getAffectRange();
		}
		
		if (!targets.isEmpty() && (escapeRange > 0))
		{
			int skipRange = 0;
			int skipLOS = 0;
			int skipPeaceZone = 0;
			final List<WorldObject> targetList = new LinkedList<>();
			for (WorldObject target : targets)
			{
				if (target.isCreature())
				{
					if (!isInsideRadius3D(target.getX(), target.getY(), target.getZ(), escapeRange + _template.getCollisionRadius()))
					{
						skipRange++;
						continue;
					}
					
					// Curar membros do grupo deve ignorar Linha de Visao.
					if (((skill.getTargetType() != TargetType.PARTY) || !skill.hasEffectType(EffectType.HEAL)) //
						&& (mut.getSkillTime() > 550) && !GeoData.getInstance().canSeeTarget(this, target))
					{
						skipLOS++;
						continue;
					}
					
					if (skill.isBad())
					{
						if (isPlayer())
						{
							if (target.asCreature().isInsidePeaceZone(asPlayer()))
							{
								skipPeaceZone++;
								continue;
							}
						}
						else
						{
							if (target.asCreature().isInsidePeaceZone(this, target))
							{
								skipPeaceZone++;
								continue;
							}
						}
					}
					
					targetList.add(target);
				}
			}
			
			if (targetList.isEmpty())
			{
				if (isPlayer())
				{
					if (skipRange > 0)
					{
						sendPacket(SystemMessageId.THE_DISTANCE_IS_TOO_FAR_AND_SO_THE_CASTING_HAS_BEEN_STOPPED);
					}
					else if (skipLOS > 0)
					{
						sendPacket(SystemMessageId.CANNOT_SEE_TARGET);
					}
					else if (skipPeaceZone > 0)
					{
						sendPacket(SystemMessageId.A_MALICIOUS_SKILL_CANNOT_BE_USED_IN_A_PEACE_ZONE);
					}
				}
				
				abortCast();
				return;
			}
			
			mut.setTargets(targetList);
		}
		
		// Garante que uma conjuracao esta em andamento
		// Verifica se o jogador esta usando fake death.
		// Skills estaticas podem ser usadas enquanto finge morte.
		if ((mut.isSimultaneous() && !_isCastingSimultaneouslyNow) || (!mut.isSimultaneous() && !_isCastingNow) || (isAlikeDead() && !skill.isStatic()))
		{
			// agora cancela ambos, simultaneo e normal
			getAI().notifyAction(Action.CANCEL);
			return;
		}
		
		mut.setPhase(2);
		if (mut.getSkillTime() == 0)
		{
			onMagicHitTimer(mut);
		}
		else
		{
			_skillCast = ThreadPool.schedule(mut, 400);
		}
	}
	
	// Executa no final da conjuracao de skill
	public void onMagicHitTimer(MagicUseTask mut)
	{
		final Skill skill = mut.getSkill();
		final List<WorldObject> targets = mut.getTargets();
		if ((skill == null) || (targets == null))
		{
			abortCast();
			return;
		}
		
		try
		{
			// Percorre a tabela de alvos
			for (WorldObject tgt : targets)
			{
				if (tgt.isPlayable())
				{
					if (isPlayer() && tgt.isSummon())
					{
						tgt.asSummon().updateAndBroadcastStatus(1);
					}
				}
				else if (isPlayable() && tgt.isAttackable())
				{
					final Creature target = tgt.asCreature();
					if (skill.getEffectPoint() > 0)
					{
						target.asAttackable().reduceHate(this, skill.getEffectPoint());
					}
					else if (skill.getEffectPoint() < 0)
					{
						target.asAttackable().addDamageHate(this, 0, -skill.getEffectPoint());
					}
				}
			}
			
			rechargeShots(skill.useSoulShot(), skill.useSpiritShot());
			
			final StatusUpdate su = new StatusUpdate(this);
			boolean isSendStatus = false;
			
			// Consome MP da Creature e Envia pacote Server->Client StatusUpdate com HP e MP atuais para todos os outros Player para informar
			final double mpConsume = _stat.getMpConsume(skill);
			if (mpConsume > 0)
			{
				if (mpConsume > _status.getCurrentMp())
				{
					sendPacket(SystemMessageId.NOT_ENOUGH_MP);
					abortCast();
					return;
				}
				
				_status.reduceMp(mpConsume);
				su.addAttribute(StatusUpdate.CUR_MP, (int) _status.getCurrentMp());
				isSendStatus = true;
			}
			
			// Consome HP se necessario e Envia pacote Server->Client StatusUpdate com HP e MP atuais para todos os outros Player para informar
			if (skill.getHpConsume() > 0)
			{
				final double consumeHp = skill.getHpConsume();
				if (consumeHp >= _status.getCurrentHp())
				{
					sendPacket(SystemMessageId.NOT_ENOUGH_HP);
					abortCast();
					return;
				}
				
				_status.reduceHp(consumeHp, this, true);
				su.addAttribute(StatusUpdate.CUR_HP, (int) _status.getCurrentHp());
				isSendStatus = true;
			}
			
			// Envia pacote Server->Client StatusUpdate com modificacao de MP para o Player
			if (isSendStatus)
			{
				sendPacket(su);
			}
			
			if (isPlayer())
			{
				// Consome Cargas.
				final Player player = asPlayer();
				if (skill.getChargeConsumeCount() > 0)
				{
					player.decreaseCharges(skill.getChargeConsumeCount());
				}
				
				// Consome Almas se necessario.
				if ((skill.getMaxSoulConsumeCount() > 0) && !player.decreaseSouls(skill.getMaxSoulConsumeCount()))
				{
					abortCast();
					return;
				}
				
				// Consome Energia de Agathion se necessario.
				if (skill.getEnergyConsume() > 0)
				{
					final Item bracelet = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LBRACELET);
					if ((bracelet != null) && bracelet.isAgathionItem())
					{
						if (bracelet.getAgathionEnergy() < skill.getEnergyConsume())
						{
							player.sendPacket(SystemMessageId.THE_SKILL_WAS_CANCELED_DUE_TO_INSUFFICIENT_ENERGY);
							abortCast();
							return;
						}
						bracelet.setAgathionEnergy(bracelet.getAgathionEnergy() - skill.getEnergyConsume());
						bracelet.updateDatabase();
						player.sendPacket(new ExBrAgathionEnergyInfo(Collections.singletonList(bracelet)));
					}
				}
			}
			
			// Executa a skill magica para calcular seus efeitos
			callSkill(mut.getSkill(), mut.getTargets());
		}
		catch (NullPointerException e)
		{
			LOGGER.log(Level.WARNING, "", e);
		}
		
		if (mut.getSkillTime() > 0)
		{
			mut.setCount(mut.getCount() + 1);
		}
		
		mut.setPhase(3);
		if (mut.getSkillTime() == 0)
		{
			onMagicFinalizer(mut);
		}
		else
		{
			if (mut.isSimultaneous())
			{
				_skillCast2 = ThreadPool.schedule(mut, 0);
			}
			else
			{
				_skillCast = ThreadPool.schedule(mut, 0);
			}
		}
	}
	
	// Executa apos skillTime
	public void onMagicFinalizer(MagicUseTask mut)
	{
		if (mut.isSimultaneous())
		{
			_skillCast2 = null;
			setCastingSimultaneouslyNow(false);
			return;
		}
		
		// Limpeza
		_skillCast = null;
		_castInterruptTime = 0;
		
		// Em cada repeticao recarrega shots antes de conjurar.
		if (mut.getCount() > 0)
		{
			rechargeShots(mut.getSkill().useSoulShot(), mut.getSkill().useSpiritShot());
		}
		
		// Para a conjuracao
		setCastingNow(false);
		setCastingSimultaneouslyNow(false);
		
		final Skill skill = mut.getSkill();
		final WorldObject target = !mut.getTargets().isEmpty() ? mut.getTargets().get(0) : null;
		
		// Ataca o alvo apos uso de skill.
		if (skill.nextActionIsAttack() && (_target != this) && (target != null) && (_target == target) && _target.isCreature() && target.canBeAttacked() && (!isPlayer() || !asPlayer().isAutoPlaying()))
		{
			final IntentionCommand nextIntention = getAI().getNextIntention();
			if ((nextIntention == null) || (nextIntention.getIntention() != Intention.MOVE_TO))
			{
				if (isPlayer())
				{
					final SkillUseHolder currSkill = asPlayer().getCurrentSkill();
					if ((currSkill == null) || !currSkill.isShiftPressed())
					{
						ThreadPool.schedule(() ->
						{
							if (!isDisabled() && !isAttackingOrCastingNow())
							{
								getAI().setIntention(Intention.ATTACK, target);
							}
						}, 333); // Wait for skill land animation.
					}
				}
				else
				{
					getAI().setIntention(Intention.ATTACK, target);
				}
			}
			else if (isPlayer()) // Player is moving.
			{
				ThreadPool.schedule(() -> completeMagicFinalizer(skill, target), 333); // Wait for skill land animation.
				return;
			}
		}
		
		completeMagicFinalizer(skill, target);
	}
	
	private void completeMagicFinalizer(Skill skill, WorldObject target)
	{
		if (skill.isBad() && (skill.getTargetType() != TargetType.UNLOCKABLE))
		{
			getAI().clientStartAutoAttack();
		}
		
		// Notifica a AI da Creature com FINISH_CASTING
		getAI().notifyAction(Action.FINISH_CASTING);
		
		// Notifica Scripts DP
		notifyQuestEventSkillFinished(skill, target);
		
		// Se o personagem e um jogador, limpa seu estado de conjuracao atual e verifica se uma skill esta na fila.
		// Se ha uma skill na fila, executa-a e limpa a fila.
		if (isPlayer())
		{
			final Player player = asPlayer();
			final SkillUseHolder queuedSkill = player.getQueuedSkill();
			player.setCurrentSkill(null, false, false);
			if (queuedSkill != null)
			{
				player.setQueuedSkill(null, false, false);
				
				// NAO USE: Chamada recursiva ao metodo useMagic().
				// player.useMagic(queuedSkill.getSkill(), queuedSkill.isCtrlPressed(), queuedSkill.isShiftPressed());
				ThreadPool.execute(new QueuedMagicUseTask(player, queuedSkill.getSkill(), queuedSkill.isCtrlPressed(), queuedSkill.isShiftPressed()));
			}
		}
		
		if (isChanneling())
		{
			getSkillChannelizer().stopChanneling();
		}
	}
	
	// Evento de quest ON_SPELL_FNISHED
	protected void notifyQuestEventSkillFinished(Skill skill, WorldObject target)
	{
	}
	
	/**
	 * Executa a skill magica e calcula seus efeitos em cada alvo contido na tabela de alvos.
	 * @param skill A Skill a usar
	 * @param targets A tabela de alvos WorldObject
	 */
	public void callSkill(Skill skill, List<WorldObject> targets)
	{
		try
		{
			// Verifica se os efeitos de skill toggle ja estao em andamento na Creature
			if (skill.isToggle() && isAffectedBySkill(skill.getId()))
			{
				return;
			}
			
			// Verificacoes iniciais
			for (WorldObject obj : targets)
			{
				if ((obj == null) || !obj.isCreature())
				{
					continue;
				}
				
				final Creature target = obj.asCreature();
				
				// Verifica ataque de monstro raid e verifica buff em personagens que atacam monstros raid.
				Creature targetsAttackTarget = null;
				Creature targetsCastTarget = null;
				if (target.hasAI())
				{
					targetsAttackTarget = target.getAI().getAttackTarget();
					targetsCastTarget = target.getAI().getCastTarget();
				}
				
				if (!Config.RAID_DISABLE_CURSE && ((target.isRaid() && target.giveRaidCurse() && (getLevel() > (target.getLevel() + 8))) || (!skill.isBad() && (targetsAttackTarget != null) && targetsAttackTarget.isRaid() && targetsAttackTarget.giveRaidCurse() && targetsAttackTarget.getAttackByList().contains(target) && (getLevel() > (targetsAttackTarget.getLevel() + 8))) || (!skill.isBad() && (targetsCastTarget != null) && targetsCastTarget.isRaid() && targetsCastTarget.giveRaidCurse() && targetsCastTarget.getAttackByList().contains(target) && (getLevel() > (targetsCastTarget.getLevel() + 8)))))
				{
					final CommonSkill curse = skill.isMagic() ? CommonSkill.RAID_CURSE : CommonSkill.RAID_CURSE2;
					final Skill curseSkill = curse.getSkill();
					if (curseSkill != null)
					{
						abortAttack();
						abortCast();
						getAI().setIntention(Intention.IDLE);
						curseSkill.applyEffects(target, this);
					}
					else
					{
						LOGGER.warning("Skill ID " + curse.getId() + " level " + curse.getLevel() + " is missing in DP!");
					}
					
					return;
				}
				
				// Verifica se over-hit e possivel
				if (skill.isOverhit() && target.isAttackable())
				{
					target.asAttackable().overhitEnabled(true);
				}
				
				// Skills estaticas nao ativam nenhuma skill de chance
				if (!skill.isStatic())
				{
					// Executa efeito de habilidade especial da arma se disponivel
					final Weapon activeWeapon = getActiveWeaponItem();
					if ((activeWeapon != null) && !target.isDead())
					{
						activeWeapon.castOnMagicSkill(this, target, skill);
					}
					
					if (!_triggerSkills.isEmpty())
					{
						for (OptionSkillHolder holder : _triggerSkills.values())
						{
							if (((skill.isMagic() && (holder.getSkillType() == OptionSkillType.MAGIC)) || (skill.isPhysical() && (holder.getSkillType() == OptionSkillType.ATTACK))) && (Rnd.get(100) < holder.getChance()))
							{
								makeTriggerCast(holder.getSkill(), target);
							}
						}
					}
				}
			}
			
			// Executa a skill magica e calcula seus efeitos
			skill.activateSkill(this, targets);
			
			final Player player = asPlayer();
			if (player != null)
			{
				for (WorldObject target : targets)
				{
					// ATTACKED e PvPStatus
					if (target.isCreature())
					{
						if (skill.getEffectPoint() <= 0)
						{
							if ((target.isPlayable() || target.isTrap()) && skill.isBad())
							{
								// Conjurado no target_self mas nao prejudica a si mesmo
								if (!target.equals(this))
								{
									// Verificacao de modo de combate
									if (target.isPlayer())
									{
										target.asPlayer().getAI().clientStartAutoAttack();
									}
									else if (target.isSummon() && target.asCreature().hasAI())
									{
										final Player owner = target.asSummon().getOwner();
										if (owner != null)
										{
											owner.getAI().clientStartAutoAttack();
										}
									}
									
									// ataque do proprio pet nao flagra jogador
									// ativar armadilha nao flagra dono da armadilha
									if ((player.getSummon() != target) && !isTrap() && !((skill.getEffectPoint() == 0) && (skill.getAffectRange() > 0)))
									{
										player.updatePvPStatus(target.asCreature());
									}
								}
							}
							else if (target.isAttackable())
							{
								switch (skill.getId())
								{
									case 51: // Lure
									case 511: // Temptation
									{
										break;
									}
									default:
									{
										// adiciona atacante na lista
										target.asCreature().addAttackerToAttackByList(this);
									}
								}
							}
							
							// notifica a AI do alvo sobre o ataque
							if (target.asCreature().hasAI() && skill.isBad() && !skill.hasEffectType(EffectType.HATE) && (skill.getAbnormalType() != AbnormalType.TURN_PASSIVE))
							{
								target.asCreature().getAI().notifyAction(Action.ATTACKED, this);
							}
						}
						else
						{
							if (target.isPlayer())
							{
								// Conjurando skill nao ofensiva em jogador com flag pvp ou com karma
								if (!(target.equals(this) || target.equals(player)) && ((target.asPlayer().getPvpFlag() > 0) || (target.asPlayer().getKarma() > 0)))
								{
									player.updatePvPStatus();
								}
							}
							else if (target.isAttackable())
							{
								player.updatePvPStatus();
							}
						}
					}
				}
				
				// Mobs no alcance 1000 veem a magia
				World.getInstance().forEachVisibleObjectInRange(player, Npc.class, 1000, npcMob ->
				{
					if (EventDispatcher.getInstance().hasListener(EventType.ON_NPC_SKILL_SEE, npcMob))
					{
						EventDispatcher.getInstance().notifyEventAsync(new OnNpcSkillSee(npcMob, player, skill, targets, isSummon()), npcMob);
					}
					
					// Logica On Skill See
					if (npcMob.isAttackable())
					{
						final Attackable attackable = npcMob.asAttackable();
						int skillEffectPoint = skill.getEffectPoint();
						if (player.hasSummon() && (targets.size() == 1) && targets.contains(player.getSummon()))
						{
							skillEffectPoint = 0;
						}
						
						if ((skillEffectPoint > 0) && attackable.hasAI() && (attackable.getAI().getIntention() == Intention.ATTACK))
						{
							final WorldObject npcTarget = attackable.getTarget();
							for (WorldObject skillTarget : targets)
							{
								if ((npcTarget == skillTarget) || (npcMob == skillTarget))
								{
									final Creature originalCaster = isSummon() ? this : player;
									attackable.addDamageHate(originalCaster, 0, (skillEffectPoint * 150) / (attackable.getLevel() + 7));
								}
							}
						}
					}
				});
			}
			
			// Notifica a AI
			if (skill.isBad() && !skill.hasEffectType(EffectType.HATE))
			{
				for (WorldObject target : targets)
				{
					if (target.isCreature())
					{
						final Creature creature = target.asCreature();
						if (creature.hasAI())
						{
							// Notifica a AI do alvo sobre o ataque
							creature.getAI().notifyAction(Action.ATTACKED, this);
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": callSkill() failed.", e);
		}
	}
	
	/**
	 * @return o Modificador de Nivel ((level + 89) / 100).
	 */
	public double getLevelMod()
	{
		return ((getLevel() + 89) / 100d);
	}
	
	public void setSkillCast(Future<?> newSkillCast)
	{
		_skillCast = newSkillCast;
	}
	
	/**
	 * Define _isCastingNow como true e _castInterruptTime e calculado a partir do tempo final (ticks)
	 * @param newSkillCastEndTick
	 */
	public void forceIsCasting(int newSkillCastEndTick)
	{
		setCastingNow(true);
		
		// para interrupcao -400 ms
		_castInterruptTime = newSkillCastEndTick - 4;
	}
	
	private boolean _disabledAI = false;
	
	public void updatePvPFlag(int value)
	{
		// Overridden in Player
	}
	
	/**
	 * @return a multiplier based on weapon random damage
	 */
	public double getRandomDamageMultiplier()
	{
		final Weapon activeWeapon = getActiveWeaponItem();
		int random;
		if (activeWeapon != null)
		{
			random = activeWeapon.getRandomDamage();
		}
		else
		{
			random = 5 + (int) Math.sqrt(getLevel());
		}
		
		return (1 + ((double) Rnd.get(0 - random, random) / 100));
	}
	
	public long getAttackEndTime()
	{
		return _attackEndTime;
	}
	
	public int getBowAttackEndTime()
	{
		return _disableBowAttackEndTime;
	}
	
	/**
	 * Not Implemented.
	 * @return
	 */
	public abstract int getLevel();
	
	public double calcStat(Stat stat, double init)
	{
		return _stat.calcStat(stat, init, null, null);
	}
	
	// Stat - NEED TO REMOVE ONCE CREATURESTATUS IS COMPLETE
	public double calcStat(Stat stat, double init, Creature target, Skill skill)
	{
		return _stat.calcStat(stat, init, target, skill);
	}
	
	public int getAccuracy()
	{
		return _stat.getAccuracy();
	}
	
	public float getAttackSpeedMultiplier()
	{
		return _stat.getAttackSpeedMultiplier();
	}
	
	public double getCriticalDmg(Creature target, double init)
	{
		return _stat.getCriticalDmg(target, init);
	}
	
	public int getCriticalHit(Creature target, Skill skill)
	{
		return _stat.getCriticalHit(target, skill);
	}
	
	public int getEvasionRate(Creature target)
	{
		return _stat.getEvasionRate(target);
	}
	
	public int getMagicalAttackRange(Skill skill)
	{
		return _stat.getMagicalAttackRange(skill);
	}
	
	public int getMaxCp()
	{
		return _stat.getMaxCp();
	}
	
	public int getMaxRecoverableCp()
	{
		return _stat.getMaxRecoverableCp();
	}
	
	public double getMAtk(Creature target, Skill skill)
	{
		return _stat.getMAtk(target, skill);
	}
	
	public int getMAtkSpd()
	{
		return _stat.getMAtkSpd();
	}
	
	public int getMaxMp()
	{
		return _stat.getMaxMp();
	}
	
	public int getMaxRecoverableMp()
	{
		return _stat.getMaxRecoverableMp();
	}
	
	public int getMaxHp()
	{
		return _stat.getMaxHp();
	}
	
	public int getMaxRecoverableHp()
	{
		return _stat.getMaxRecoverableHp();
	}
	
	public int getMCriticalHit(Creature target, Skill skill)
	{
		return _stat.getMCriticalHit(target, skill);
	}
	
	public double getMDef(Creature target, Skill skill)
	{
		return _stat.getMDef(target, skill);
	}
	
	public double getMReuseRate(Skill skill)
	{
		return _stat.getMReuseRate(skill);
	}
	
	public double getPAtk(Creature target)
	{
		return _stat.getPAtk(target);
	}
	
	public double getPAtkSpd()
	{
		return _stat.getPAtkSpd();
	}
	
	public double getPDef(Creature target)
	{
		return _stat.getPDef(target);
	}
	
	public int getPhysicalAttackRange()
	{
		return _stat.getPhysicalAttackRange();
	}
	
	public double getMovementSpeedMultiplier()
	{
		return _stat.getMovementSpeedMultiplier();
	}
	
	public double getRunSpeed()
	{
		return _stat.getRunSpeed();
	}
	
	public double getWalkSpeed()
	{
		return _stat.getWalkSpeed();
	}
	
	public double getSwimRunSpeed()
	{
		return _stat.getSwimRunSpeed();
	}
	
	public double getSwimWalkSpeed()
	{
		return _stat.getSwimWalkSpeed();
	}
	
	public double getMoveSpeed()
	{
		return _stat.getMoveSpeed();
	}
	
	public int getShldDef()
	{
		return _stat.getShldDef();
	}
	
	public int getSTR()
	{
		return _stat.getSTR();
	}
	
	public int getDEX()
	{
		return _stat.getDEX();
	}
	
	public int getCON()
	{
		return _stat.getCON();
	}
	
	public int getINT()
	{
		return _stat.getINT();
	}
	
	public int getWIT()
	{
		return _stat.getWIT();
	}
	
	public int getMEN()
	{
		return _stat.getMEN();
	}
	
	// Status - NEED TO REMOVE ONCE CREATURESTATUS IS COMPLETE
	public void addStatusListener(Creature object)
	{
		_status.addStatusListener(object);
	}
	
	public void reduceCurrentHp(double amount, Creature attacker, Skill skill)
	{
		reduceCurrentHp(amount, attacker, true, false, skill);
	}
	
	public void reduceCurrentHpByDOT(double amount, Creature attacker, Skill skill)
	{
		reduceCurrentHp(amount, attacker, !skill.isToggle(), true, skill);
	}
	
	public void reduceCurrentHp(double amount, Creature attacker, boolean awake, boolean isDOT, Skill skill)
	{
		if (Config.CHAMPION_ENABLE && isChampion() && (Config.CHAMPION_HP != 0))
		{
			_status.reduceHp(amount / Config.CHAMPION_HP, attacker, awake, isDOT, false);
		}
		else
		{
			if (isPlayer() && !isDOT && (skill != null) && (skill.getCastRange() > 0) && (attacker != null) && !GeoData.getInstance().canSeeTarget(attacker, this))
			{
				amount = 0;
			}
			
			_status.reduceHp(amount, attacker, awake, isDOT, false);
		}
	}
	
	public void reduceCurrentMp(double amount)
	{
		_status.reduceMp(amount);
	}
	
	@Override
	public void removeStatusListener(Creature object)
	{
		_status.removeStatusListener(object);
	}
	
	protected void stopHpMpRegeneration()
	{
		_status.stopHpMpRegeneration();
	}
	
	public double getCurrentCp()
	{
		return _status.getCurrentCp();
	}
	
	public int getCurrentCpPercent()
	{
		return (int) ((_status.getCurrentCp() * 100) / _stat.getMaxCp());
	}
	
	public void setCurrentCp(double newCp)
	{
		_status.setCurrentCp(newCp);
	}
	
	public double getCurrentHp()
	{
		return _status.getCurrentHp();
	}
	
	public int getCurrentHpPercent()
	{
		return (int) ((_status.getCurrentHp() * 100) / _stat.getMaxHp());
	}
	
	public void setCurrentHp(double newHp)
	{
		_status.setCurrentHp(newHp);
	}
	
	public void setCurrentHpMp(double newHp, double newMp)
	{
		_status.setCurrentHpMp(newHp, newMp);
	}
	
	public double getCurrentMp()
	{
		return _status.getCurrentMp();
	}
	
	public int getCurrentMpPercent()
	{
		return (int) ((_status.getCurrentMp() * 100) / _stat.getMaxMp());
	}
	
	public void setCurrentMp(double newMp)
	{
		_status.setCurrentMp(newMp);
	}
	
	/**
	 * Fully restores the creature's HP and MP to their maximum values.
	 */
	public void fullRestore()
	{
		_status.setCurrentHp(getMaxHp());
		_status.setCurrentMp(getMaxMp(), isPlayable());
	}
	
	/**
	 * @return the max weight that the Creature can load.
	 */
	public int getMaxLoad()
	{
		if (isPlayer() || isPet())
		{
			// Weight Limit = (CON Modifier*69000) * Skills
			// Source http://l2p.bravehost.com/weightlimit.html (May 2007)
			final double baseLoad = Math.floor(BaseStat.CON.calcBonus(this) * 69000);
			return (int) calcStat(Stat.WEIGHT_LIMIT, (baseLoad * Config.ALT_WEIGHT_LIMIT), this, null);
		}
		
		return 0;
	}
	
	public int getBonusWeightPenalty()
	{
		if (isPlayer() || isPet())
		{
			return (int) calcStat(Stat.WEIGHT_PENALTY, 1, this, null);
		}
		
		return 0;
	}
	
	/**
	 * @return the current weight of the Creature.
	 */
	public int getCurrentLoad()
	{
		if ((isPlayer() || isPet()) && (getInventory() != null))
		{
			return (int) (getInventory().getTotalWeight() / Config.ALT_WEIGHT_LIMIT);
		}
		
		return 0;
	}
	
	public boolean isChampion()
	{
		return false;
	}
	
	/**
	 * Send system message about damage.
	 * @param target
	 * @param damage
	 * @param mcrit
	 * @param pcrit
	 * @param miss
	 */
	public void sendDamageMessage(Creature target, int damage, boolean mcrit, boolean pcrit, boolean miss)
	{
		if (miss && target.isPlayer())
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.C1_HAS_EVADED_C2_S_ATTACK);
			sm.addPcName(target.asPlayer());
			sm.addString(getName());
			target.sendPacket(sm);
		}
	}
	
	public byte getAttackElement()
	{
		return _stat.getAttackElement();
	}
	
	public int getAttackElementValue(byte attackAttribute)
	{
		return _stat.getAttackElementValue(attackAttribute);
	}
	
	public int getDefenseElementValue(byte defenseAttribute)
	{
		return _stat.getDefenseElementValue(defenseAttribute);
	}
	
	public void disableCoreAI(boolean value)
	{
		_disabledAI = value;
	}
	
	public boolean isCoreAIDisabled()
	{
		return _disabledAI;
	}
	
	/**
	 * @return true
	 */
	public boolean giveRaidCurse()
	{
		return true;
	}
	
	/**
	 * Check if target is affected with special buff
	 * @see EffectList#isAffected(EffectFlag)
	 * @param flag int
	 * @return boolean
	 */
	public boolean isAffected(EffectFlag flag)
	{
		return _effectList.isAffected(flag);
	}
	
	/**
	 * Check if target is affected by AbnormalType.
	 * @param type the AbnormalType
	 * @return boolean true if affected
	 */
	public boolean isAffectedByAbnormalType(AbnormalType type)
	{
		return _effectList.isAffectedByAbnormalType(type);
	}
	
	public Team getTeam()
	{
		return _team;
	}
	
	public void setTeam(Team team)
	{
		_team = team;
	}
	
	public void setLethalable(boolean value)
	{
		_lethalable = value;
	}
	
	public boolean isLethalable()
	{
		return _lethalable;
	}
	
	public Map<Integer, OptionSkillHolder> getTriggerSkills()
	{
		return _triggerSkills;
	}
	
	public void addTriggerSkill(OptionSkillHolder holder)
	{
		getTriggerSkills().put(holder.getSkill().getId(), holder);
	}
	
	public void removeTriggerSkill(OptionSkillHolder holder)
	{
		getTriggerSkills().remove(holder.getSkill().getId());
	}
	
	public void makeTriggerCast(Skill skill, Creature target, boolean ignoreTargetType)
	{
		try
		{
			if ((skill == null))
			{
				return;
			}
			
			if (skill.checkCondition(this, target, false))
			{
				if (isSkillDisabled(skill))
				{
					return;
				}
				
				if (skill.getReuseDelay() > 0)
				{
					disableSkill(skill, skill.getReuseDelay());
				}
				
				final List<WorldObject> targets = !ignoreTargetType ? skill.getTargetList(this, false, target) : Collections.singletonList(target);
				if (targets.isEmpty())
				{
					return;
				}
				
				Creature skillTarget = target;
				for (WorldObject obj : targets)
				{
					if ((obj != null) && obj.isCreature())
					{
						skillTarget = obj.asCreature();
						break;
					}
				}
				
				if (Config.ALT_VALIDATE_TRIGGER_SKILLS && isPlayable() && (skillTarget != null) && skillTarget.isPlayable())
				{
					final Player player = asPlayer();
					if (!player.checkPvpSkill(skillTarget, skill))
					{
						return;
					}
				}
				
				broadcastPacket(new MagicSkillUse(this, skillTarget, skill.getDisplayId(), skill.getLevel(), 0, 0));
				broadcastPacket(new MagicSkillLaunched(this, skill.getDisplayId(), skill.getLevel(), targets));
				
				// Launch the magic skill and calculate its effects
				skill.activateSkill(this, targets);
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "", e);
		}
	}
	
	public void makeTriggerCast(Skill skill, Creature target)
	{
		makeTriggerCast(skill, target, false);
	}
	
	/**
	 * Dummy method overriden in {@link Player}
	 * @return {@code true} if current player can revive and shows 'To Village' button upon death, {@code false} otherwise.
	 */
	public boolean canRevive()
	{
		return true;
	}
	
	/**
	 * Dummy method overriden in {@link Player}
	 * @param value
	 */
	public void setCanRevive(boolean value)
	{
	}
	
	/**
	 * Dummy method overriden in {@link Attackable}
	 * @return {@code true} if there is a loot to sweep, {@code false} otherwise.
	 */
	public boolean isSweepActive()
	{
		return false;
	}
	
	/**
	 * Dummy method overriden in {@link Player}
	 * @return the clan id of current character.
	 */
	public int getClanId()
	{
		return 0;
	}
	
	/**
	 * Dummy method overriden in {@link Player}
	 * @return the clan of current character.
	 */
	public Clan getClan()
	{
		return null;
	}
	
	/**
	 * Dummy method overriden in {@link Player}
	 * @return {@code true} if player is in academy, {@code false} otherwise.
	 */
	public boolean isAcademyMember()
	{
		return false;
	}
	
	/**
	 * Dummy method overriden in {@link Player}
	 * @return the pledge type of current character.
	 */
	public int getPledgeType()
	{
		return 0;
	}
	
	/**
	 * Dummy method overriden in {@link Player}
	 * @return the alliance id of current character.
	 */
	public int getAllyId()
	{
		return 0;
	}
	
	/**
	 * Notifies to listeners that current character received damage.
	 * @param damage
	 * @param attacker
	 * @param skill
	 * @param critical
	 * @param damageOverTime
	 */
	public void notifyDamageReceived(double damage, Creature attacker, Skill skill, boolean critical, boolean damageOverTime)
	{
		// Auto attacks make you stand up.
		if (isPlayer() && asPlayer().isFakeDeath() && Config.FAKE_DEATH_DAMAGE_STAND && (damage > 0))
		{
			stopFakeDeath(true);
		}
		
		if ((attacker != null) && EventDispatcher.getInstance().hasListener(EventType.ON_CREATURE_DAMAGE_DEALT, attacker))
		{
			if (_onCreatureDamageDealt == null)
			{
				_onCreatureDamageDealt = new OnCreatureDamageDealt();
			}
			
			_onCreatureDamageDealt.setAttacker(attacker);
			_onCreatureDamageDealt.setTarget(this);
			_onCreatureDamageDealt.setDamage(damage);
			_onCreatureDamageDealt.setSkill(skill);
			_onCreatureDamageDealt.setCritical(critical);
			_onCreatureDamageDealt.setDamageOverTime(damageOverTime);
			
			EventDispatcher.getInstance().notifyEvent(_onCreatureDamageDealt, attacker);
		}
		
		if (EventDispatcher.getInstance().hasListener(EventType.ON_CREATURE_DAMAGE_RECEIVED, this))
		{
			if (_onCreatureDamageReceived == null)
			{
				_onCreatureDamageReceived = new OnCreatureDamageReceived();
			}
			
			_onCreatureDamageReceived.setAttacker(attacker);
			_onCreatureDamageReceived.setTarget(this);
			_onCreatureDamageReceived.setDamage(damage);
			_onCreatureDamageReceived.setSkill(skill);
			_onCreatureDamageReceived.setCritical(critical);
			_onCreatureDamageReceived.setDamageOverTime(damageOverTime);
			
			EventDispatcher.getInstance().notifyEventAsync(_onCreatureDamageReceived, this);
		}
	}
	
	/**
	 * Notifies to listeners that current character avoid attack.
	 * @param target
	 * @param isDot
	 */
	public void notifyAttackAvoid(Creature target, boolean isDot)
	{
		if (EventDispatcher.getInstance().hasListener(EventType.ON_CREATURE_ATTACK_AVOID, target))
		{
			if (_onCreatureAttackAvoid == null)
			{
				_onCreatureAttackAvoid = new OnCreatureAttackAvoid();
			}
			
			_onCreatureAttackAvoid.setAttacker(this);
			_onCreatureAttackAvoid.setTarget(target);
			_onCreatureAttackAvoid.setDamageOverTime(isDot);
			
			EventDispatcher.getInstance().notifyEvent(_onCreatureAttackAvoid, target);
		}
	}
	
	/**
	 * @return {@link WeaponType} of current character's weapon or basic weapon type.
	 */
	public WeaponType getAttackType()
	{
		if (isTransformed())
		{
			final TransformTemplate template = getTransformation().getTemplate(asPlayer());
			if (template != null)
			{
				return template.getBaseAttackType();
			}
		}
		
		final Weapon weapon = getActiveWeaponItem();
		if (weapon != null)
		{
			return weapon.getItemType();
		}
		
		return _template.getBaseAttackType();
	}
	
	public boolean isInCategory(CategoryType type)
	{
		return CategoryData.getInstance().isInCategory(type, getId());
	}
	
	/**
	 * @return the character that summoned this NPC.
	 */
	public Creature getSummoner()
	{
		return _summoner;
	}
	
	/**
	 * @param summoner the summoner of this NPC.
	 */
	public void setSummoner(Creature summoner)
	{
		_summoner = summoner;
	}
	
	@Override
	public boolean isCreature()
	{
		return true;
	}
	
	@Override
	public Creature asCreature()
	{
		return this;
	}
	
	/**
	 * @return {@code true} if current character is casting channeling skill, {@code false} otherwise.
	 */
	public boolean isChanneling()
	{
		return (_channelizer != null) && _channelizer.isChanneling();
	}
	
	public SkillChannelizer getSkillChannelizer()
	{
		if (_channelizer == null)
		{
			_channelizer = new SkillChannelizer(this);
		}
		
		return _channelizer;
	}
	
	/**
	 * @return {@code true} if current character is affected by channeling skill, {@code false} otherwise.
	 */
	public boolean isChannelized()
	{
		return (_channelized != null) && !_channelized.isChannelized();
	}
	
	public SkillChannelized getSkillChannelized()
	{
		if (_channelized == null)
		{
			_channelized = new SkillChannelized();
		}
		
		return _channelized;
	}
	
	public void addInvulAgainst(SkillHolder holder)
	{
		final InvulSkillHolder invulHolder = getInvulAgainstSkills().get(holder.getSkillId());
		if (invulHolder != null)
		{
			invulHolder.increaseInstances();
			return;
		}
		
		getInvulAgainstSkills().put(holder.getSkillId(), new InvulSkillHolder(holder));
	}
	
	public void removeInvulAgainst(SkillHolder holder)
	{
		final InvulSkillHolder invulHolder = getInvulAgainstSkills().get(holder.getSkillId());
		if ((invulHolder != null) && (invulHolder.decreaseInstances() < 1))
		{
			getInvulAgainstSkills().remove(holder.getSkillId());
		}
	}
	
	public boolean isInvulAgainst(int skillId, int skillLevel)
	{
		if (!_invulAgainst.isEmpty())
		{
			final SkillHolder holder = getInvulAgainstSkills().get(skillId);
			return ((holder != null) && ((holder.getSkillLevel() < 1) || (holder.getSkillLevel() == skillLevel)));
		}
		
		return false;
	}
	
	private Map<Integer, InvulSkillHolder> getInvulAgainstSkills()
	{
		return _invulAgainst;
	}
	
	@Override
	public Collection<AbstractEventListener> getListeners(EventType type)
	{
		final Collection<AbstractEventListener> objectListeners = super.getListeners(type);
		final Collection<AbstractEventListener> templateListeners = _template.getListeners(type);
		final Collection<AbstractEventListener> globalListeners = isMonster() ? Containers.Monsters().getListeners(type) : isNpc() ? Containers.Npcs().getListeners(type) : isPlayer() ? Containers.Players().getListeners(type) : Collections.emptyList();
		
		// Avoid creating a new object.
		if (objectListeners.isEmpty() && templateListeners.isEmpty() && globalListeners.isEmpty())
		{
			return Collections.emptyList();
		}
		else if (!objectListeners.isEmpty() && templateListeners.isEmpty() && globalListeners.isEmpty())
		{
			return objectListeners;
		}
		else if (!templateListeners.isEmpty() && objectListeners.isEmpty() && globalListeners.isEmpty())
		{
			return templateListeners;
		}
		else if (!globalListeners.isEmpty() && objectListeners.isEmpty() && templateListeners.isEmpty())
		{
			return globalListeners;
		}
		
		final Collection<AbstractEventListener> allListeners = new ArrayList<>(objectListeners.size() + templateListeners.size() + globalListeners.size());
		allListeners.addAll(objectListeners);
		allListeners.addAll(templateListeners);
		allListeners.addAll(globalListeners);
		return allListeners;
	}
	
	public Race getRace()
	{
		return _template.getRace();
	}
	
	@Override
	public void setXYZ(int newX, int newY, int newZ)
	{
		// 0, 0 is not a valid location.
		if ((newX == 0) && (newY == 0))
		{
			return;
		}
		
		final ZoneRegion oldZoneRegion = ZoneManager.getInstance().getRegion(this);
		final ZoneRegion newZoneRegion = ZoneManager.getInstance().getRegion(newX, newY);
		
		// Mobius: Prevent moving to nonexistent regions.
		if (newZoneRegion == null)
		{
			return;
		}
		
		if (oldZoneRegion != newZoneRegion)
		{
			oldZoneRegion.removeFromZones(this);
			newZoneRegion.revalidateZones(this);
		}
		
		super.setXYZ(newX, newY, newZ);
	}
	
	public boolean isInDuel()
	{
		return false;
	}
	
	public int getDuelId()
	{
		return 0;
	}
	
	public byte getSiegeState()
	{
		return 0;
	}
	
	public int getSiegeSide()
	{
		return 0;
	}
	
	public Map<Integer, RelationCache> getKnownRelations()
	{
		return _knownRelations;
	}
	
	protected void initSeenCreatures()
	{
		if (_seenCreatures == null)
		{
			synchronized (this)
			{
				if (_seenCreatures == null)
				{
					if (isNpc())
					{
						final NpcTemplate template = asNpc().getTemplate();
						if ((template != null) && (template.getAggroRange() > 0))
						{
							_seenCreatureRange = template.getAggroRange();
						}
					}
					
					_seenCreatures = ConcurrentHashMap.newKeySet(1);
				}
			}
		}
		
		CreatureSeeTaskManager.getInstance().add(this);
	}
	
	public void updateSeenCreatures()
	{
		if ((_seenCreatures == null) || _isDead || !isSpawned())
		{
			return;
		}
		
		// Check if region and its neighbors are active.
		final WorldRegion region = getWorldRegion();
		if ((region == null) || !region.areNeighborsActive())
		{
			return;
		}
		
		World.getInstance().forEachVisibleObjectInRange(this, Creature.class, _seenCreatureRange, creature ->
		{
			if (!creature.isInvisible() && _seenCreatures.add(creature) && EventDispatcher.getInstance().hasListener(EventType.ON_CREATURE_SEE, this))
			{
				EventDispatcher.getInstance().notifyEventAsync(new OnCreatureSee(this, creature), this);
			}
		});
	}
	
	public void removeSeenCreature(WorldObject worldObject)
	{
		if (_seenCreatures == null)
		{
			return;
		}
		
		_seenCreatures.remove(worldObject);
	}
	
	public int getKarma()
	{
		return _karma;
	}
	
	public void setKarma(int karma)
	{
		_karma = karma;
	}
	
	public int getMinShopDistance()
	{
		return 0;
	}
	
	public void setCursorKeyMovement(boolean value)
	{
		_cursorKeyMovement = value;
	}
	
	public void addBuffInfoTime(BuffInfo info)
	{
		_buffFinishTask.addBuffInfo(info);
	}
	
	public void removeBuffInfoTime(BuffInfo info)
	{
		_buffFinishTask.removeBuffInfo(info);
	}
	
	public boolean isInTownWarEvent()
	{
		return _isInTownWar;
	}
	
	public void setInTownWarEvent(boolean value)
	{
		_isInTownWar = value;
	}
	
	/**
	 * @return True if debugging is enabled for this L2Character
	 */
	public boolean isDebug()
	{
		return _debugger != null;
	}
	
	/**
	 * Sets L2Character instance, to which debug packets will be send
	 * @param d
	 */
	public void setDebug(Creature d)
	{
		_debugger = d;
	}
}
