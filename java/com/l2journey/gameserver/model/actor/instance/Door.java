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
package com.l2journey.gameserver.model.actor.instance;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.logging.Level;

import com.l2journey.commons.threads.ThreadPool;
import com.l2journey.commons.util.Rnd;
import com.l2journey.gameserver.ai.CreatureAI;
import com.l2journey.gameserver.ai.DoorAI;
import com.l2journey.gameserver.data.sql.ClanHallTable;
import com.l2journey.gameserver.data.xml.DoorData;
import com.l2journey.gameserver.managers.CastleManager;
import com.l2journey.gameserver.managers.FortManager;
import com.l2journey.gameserver.managers.InstanceManager;
import com.l2journey.gameserver.managers.TerritoryWarManager;
import com.l2journey.gameserver.model.Location;
import com.l2journey.gameserver.model.World;
import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.enums.creature.InstanceType;
import com.l2journey.gameserver.model.actor.enums.creature.Race;
import com.l2journey.gameserver.model.actor.stat.DoorStat;
import com.l2journey.gameserver.model.actor.status.DoorStatus;
import com.l2journey.gameserver.model.actor.templates.DoorTemplate;
import com.l2journey.gameserver.model.clan.Clan;
import com.l2journey.gameserver.model.instancezone.Instance;
import com.l2journey.gameserver.model.item.Weapon;
import com.l2journey.gameserver.model.item.instance.Item;
import com.l2journey.gameserver.model.residences.ClanHall;
import com.l2journey.gameserver.model.siege.Castle;
import com.l2journey.gameserver.model.siege.Fort;
import com.l2journey.gameserver.model.siege.clanhalls.SiegableHall;
import com.l2journey.gameserver.model.skill.Skill;
import com.l2journey.gameserver.network.SystemMessageId;
import com.l2journey.gameserver.network.serverpackets.DoorStatusUpdate;
import com.l2journey.gameserver.network.serverpackets.OnEventTrigger;
import com.l2journey.gameserver.network.serverpackets.StaticObjectInfo;
import com.l2journey.gameserver.network.serverpackets.SystemMessage;

public class Door extends Creature
{
	public static final byte OPEN_BY_CLICK = 1;
	public static final byte OPEN_BY_TIME = 2;
	public static final byte OPEN_BY_ITEM = 4;
	public static final byte OPEN_BY_SKILL = 8;
	public static final byte OPEN_BY_CYCLE = 16;
	
	/** The castle index in the array of Castle this Npc belongs to */
	private int _castleIndex = -2;
	/** The fort index in the array of Fort this Npc belongs to */
	private int _fortIndex = -2;
	private ClanHall _clanHall;
	boolean _open = false;
	private boolean _isAttackableDoor = false;
	private boolean _isTargetable;
	private int _meshindex = 1;
	// used for autoclose on open
	private Future<?> _autoCloseTask;
	
	/**
	 * Creates a door.
	 * @param template the door template
	 */
	public Door(DoorTemplate template)
	{
		super(template);
		setInstanceType(InstanceType.Door);
		setInvul(false);
		setLethalable(false);
		_open = template.isOpenByDefault();
		_isAttackableDoor = template.isAttackable();
		_isTargetable = template.isTargetable();
		if (getGroupName() != null)
		{
			DoorData.addDoorGroup(getGroupName(), getId());
		}
		
		if (isOpenableByTime())
		{
			startTimerOpen();
		}
		
		final int clanhallId = template.getClanHallId();
		if (clanhallId > 0)
		{
			final ClanHall hall = ClanHallTable.getAllClanHalls().get(clanhallId);
			if (hall != null)
			{
				setClanHall(hall);
				hall.getDoors().add(this);
			}
		}
	}
	
	@Override
	protected CreatureAI initAI()
	{
		return new DoorAI(this);
	}
	
	private void startTimerOpen()
	{
		int delay = _open ? getTemplate().getOpenTime() : getTemplate().getCloseTime();
		if (getTemplate().getRandomTime() > 0)
		{
			delay += Rnd.get(getTemplate().getRandomTime());
		}
		ThreadPool.schedule(new TimerOpen(), delay * 1000L);
	}
	
	@Override
	public DoorTemplate getTemplate()
	{
		return (DoorTemplate) super.getTemplate();
	}
	
	@Override
	public DoorStatus getStatus()
	{
		return (DoorStatus) super.getStatus();
	}
	
	@Override
	public void initCharStatus()
	{
		setStatus(new DoorStatus(this));
	}
	
	@Override
	public void initCharStat()
	{
		setStat(new DoorStat(this));
	}
	
	@Override
	public DoorStat getStat()
	{
		return (DoorStat) super.getStat();
	}
	
	/**
	 * @return {@code true} if door is open-able by skill.
	 */
	public boolean isOpenableBySkill()
	{
		return (getTemplate().getOpenType() & OPEN_BY_SKILL) == OPEN_BY_SKILL;
	}
	
	/**
	 * @return {@code true} if door is open-able by item.
	 */
	public boolean isOpenableByItem()
	{
		return (getTemplate().getOpenType() & OPEN_BY_ITEM) == OPEN_BY_ITEM;
	}
	
	/**
	 * @return {@code true} if door is open-able by double-click.
	 */
	public boolean isOpenableByClick()
	{
		return (getTemplate().getOpenType() & OPEN_BY_CLICK) == OPEN_BY_CLICK;
	}
	
	/**
	 * @return {@code true} if door is open-able by time.
	 */
	public boolean isOpenableByTime()
	{
		return (getTemplate().getOpenType() & OPEN_BY_TIME) == OPEN_BY_TIME;
	}
	
	/**
	 * @return {@code true} if door is open-able by Field Cycle system.
	 */
	public boolean isOpenableByCycle()
	{
		return (getTemplate().getOpenType() & OPEN_BY_CYCLE) == OPEN_BY_CYCLE;
	}
	
	@Override
	public int getLevel()
	{
		return getTemplate().getLevel();
	}
	
	/**
	 * Gets the door ID.
	 * @return the door ID
	 */
	@Override
	public int getId()
	{
		return getTemplate().getId();
	}
	
	/**
	 * @return Returns if the door is open.
	 */
	public boolean isOpen()
	{
		return _open;
	}
	
	/**
	 * @param open The door open status.
	 */
	public void setOpen(boolean open)
	{
		_open = open;
		if (getChildId() > 0)
		{
			final Door sibling = getSiblingDoor(getChildId());
			if (sibling != null)
			{
				sibling.notifyChildEvent(open);
			}
			else
			{
				LOGGER.log(Level.WARNING, () -> getClass().getSimpleName() + ": cannot find child id: " + getChildId());
			}
		}
	}
	
	public boolean isAttackableDoor()
	{
		return _isAttackableDoor;
	}
	
	public boolean isShowHp()
	{
		return getTemplate().isShowHp();
	}
	
	public void setIsAttackableDoor(boolean value)
	{
		_isAttackableDoor = value;
	}
	
	public int getDamage()
	{
		final int dmg = 6 - (int) Math.ceil((getCurrentHp() / getMaxHp()) * 6);
		if (dmg > 6)
		{
			return 6;
		}
		if (dmg < 0)
		{
			return 0;
		}
		return dmg;
	}
	
	// TODO: Replace index with the castle id itself.
	public Castle getCastle()
	{
		if (_castleIndex < 0)
		{
			_castleIndex = CastleManager.getInstance().getCastleIndex(this);
		}
		if (_castleIndex < 0)
		{
			return null;
		}
		return CastleManager.getInstance().getCastles().get(_castleIndex);
	}
	
	// TODO: Replace index with the fort id itself.
	public Fort getFort()
	{
		if (_fortIndex < 0)
		{
			_fortIndex = FortManager.getInstance().getFortIndex(this);
		}
		if (_fortIndex < 0)
		{
			return null;
		}
		return FortManager.getInstance().getForts().get(_fortIndex);
	}
	
	public void setClanHall(ClanHall clanhall)
	{
		_clanHall = clanhall;
	}
	
	public ClanHall getClanHall()
	{
		return _clanHall;
	}
	
	public boolean isEnemy()
	{
		if ((getCastle() != null) && (getCastle().getResidenceId() > 0) && getCastle().getZone().isActive() && isShowHp())
		{
			return true;
		}
		if ((getFort() != null) && (getFort().getResidenceId() > 0) && getFort().getZone().isActive() && isShowHp())
		{
			return true;
		}
		return (_clanHall != null) && _clanHall.isSiegableHall() && ((SiegableHall) _clanHall).getSiegeZone().isActive() && isShowHp();
	}
	
	@Override
	public boolean isAutoAttackable(Creature attacker)
	{
		// Doors can`t be attacked by NPCs
		if (!attacker.isPlayable())
		{
			return false;
		}
		
		if (_isAttackableDoor)
		{
			return true;
		}
		if (!isShowHp())
		{
			return false;
		}
		
		final Player actingPlayer = attacker.asPlayer();
		if ((_clanHall != null) && _clanHall.isSiegableHall())
		{
			final SiegableHall hall = (SiegableHall) _clanHall;
			if (!hall.isSiegableHall())
			{
				return false;
			}
			return hall.isInSiege() && hall.getSiege().doorIsAutoAttackable() && hall.getSiege().checkIsAttacker(actingPlayer.getClan());
		}
		// Attackable only during siege by everyone (not owner)
		final boolean isCastle = ((getCastle() != null) && (getCastle().getResidenceId() > 0) && getCastle().getZone().isActive());
		final boolean isFort = ((getFort() != null) && (getFort().getResidenceId() > 0) && getFort().getZone().isActive());
		final int activeSiegeId = (getFort() != null ? getFort().getResidenceId() : (getCastle() != null ? getCastle().getResidenceId() : 0));
		if (TerritoryWarManager.getInstance().isTWInProgress())
		{
			return !TerritoryWarManager.getInstance().isAllyField(actingPlayer, activeSiegeId);
		}
		else if (isFort)
		{
			final Clan clan = actingPlayer.getClan();
			if ((clan != null) && (clan == getFort().getOwnerClan()))
			{
				return false;
			}
		}
		else if (isCastle)
		{
			final Clan clan = actingPlayer.getClan();
			if ((clan != null) && (clan.getId() == getCastle().getOwnerId()))
			{
				return false;
			}
		}
		return (isCastle || isFort);
	}
	
	@Override
	public void updateAbnormalEffect()
	{
	}
	
	/**
	 * Return null.
	 */
	@Override
	public Item getActiveWeaponInstance()
	{
		return null;
	}
	
	@Override
	public Weapon getActiveWeaponItem()
	{
		return null;
	}
	
	@Override
	public Item getSecondaryWeaponInstance()
	{
		return null;
	}
	
	@Override
	public Weapon getSecondaryWeaponItem()
	{
		return null;
	}
	
	@Override
	public void broadcastStatusUpdate()
	{
		final Collection<Player> knownPlayers = World.getInstance().getVisibleObjects(this, Player.class);
		if ((knownPlayers == null) || knownPlayers.isEmpty())
		{
			return;
		}
		
		final StaticObjectInfo su = new StaticObjectInfo(this, false);
		final StaticObjectInfo targetableSu = new StaticObjectInfo(this, true);
		final DoorStatusUpdate dsu = new DoorStatusUpdate(this);
		OnEventTrigger oe = null;
		if (getEmitter() > 0)
		{
			oe = new OnEventTrigger(this, !_open);
		}
		
		for (Player player : knownPlayers)
		{
			if ((player == null) || !isVisibleFor(player))
			{
				continue;
			}
			
			if (player.isGM() || (((getCastle() != null) && (getCastle().getResidenceId() > 0)) || ((getFort() != null) && (getFort().getResidenceId() > 0))))
			{
				player.sendPacket(targetableSu);
			}
			else
			{
				player.sendPacket(su);
			}
			
			player.sendPacket(dsu);
			if (oe != null)
			{
				player.sendPacket(oe);
			}
		}
	}
	
	public void openMe()
	{
		if (getGroupName() != null)
		{
			manageGroupOpen(true, getGroupName());
			return;
		}
		
		if (!isOpen())
		{
			setOpen(true);
			broadcastStatusUpdate();
			startAutoCloseTask();
		}
	}
	
	public void closeMe()
	{
		// remove close task
		final Future<?> oldTask = _autoCloseTask;
		if (oldTask != null)
		{
			_autoCloseTask = null;
			oldTask.cancel(false);
		}
		if (getGroupName() != null)
		{
			manageGroupOpen(false, getGroupName());
			return;
		}
		
		if (isOpen())
		{
			setOpen(false);
			broadcastStatusUpdate();
		}
	}
	
	private void manageGroupOpen(boolean open, String groupName)
	{
		final Set<Integer> set = DoorData.getDoorsByGroup(groupName);
		Door first = null;
		for (Integer id : set)
		{
			final Door door = getSiblingDoor(id);
			if (first == null)
			{
				first = door;
			}
			
			if (door.isOpen() != open)
			{
				door.setOpen(open);
				door.broadcastStatusUpdate();
			}
		}
		if ((first != null) && open)
		{
			first.startAutoCloseTask(); // only one from group
		}
	}
	
	/**
	 * Door notify child about open state change
	 * @param open true if opened
	 */
	private void notifyChildEvent(boolean open)
	{
		final byte openThis = open ? getTemplate().getMasterDoorOpen() : getTemplate().getMasterDoorClose();
		if (openThis == 1)
		{
			openMe();
		}
		else if (openThis == -1)
		{
			closeMe();
		}
	}
	
	public String getDoorName()
	{
		return getTemplate().getName();
	}
	
	public int getX(int i)
	{
		return getTemplate().getNodeX()[i];
	}
	
	public int getY(int i)
	{
		return getTemplate().getNodeY()[i];
	}
	
	public int getZMin()
	{
		return getTemplate().getNodeZ();
	}
	
	public int getZMax()
	{
		return getTemplate().getNodeZ() + getTemplate().getHeight();
	}
	
	public void setMeshIndex(int mesh)
	{
		_meshindex = mesh;
	}
	
	public int getMeshIndex()
	{
		return _meshindex;
	}
	
	public int getEmitter()
	{
		return getTemplate().getEmmiter();
	}
	
	public boolean isWall()
	{
		return getTemplate().isWall();
	}
	
	public String getGroupName()
	{
		return getTemplate().getGroupName();
	}
	
	public int getChildId()
	{
		return getTemplate().getChildDoorId();
	}
	
	@Override
	public void reduceCurrentHp(double damage, Creature attacker, boolean awake, boolean isDOT, Skill skill)
	{
		if ((isWall() && (getInstanceId() == 0)) && (!attacker.isServitor() || (attacker.getTemplate().getRace() != Race.SIEGE_WEAPON)))
		{
			return;
		}
		
		super.reduceCurrentHp(damage, attacker, awake, isDOT, skill);
	}
	
	@Override
	public void reduceCurrentHpByDOT(double i, Creature attacker, Skill skill)
	{
		// doors can't be damaged by DOTs
	}
	
	@Override
	public boolean doDie(Creature killer)
	{
		if (!super.doDie(killer))
		{
			return false;
		}
		
		final boolean isFort = ((getFort() != null) && (getFort().getResidenceId() > 0) && getFort().getSiege().isInProgress());
		final boolean isCastle = ((getCastle() != null) && (getCastle().getResidenceId() > 0) && getCastle().getSiege().isInProgress());
		final boolean isHall = ((_clanHall != null) && _clanHall.isSiegableHall() && ((SiegableHall) _clanHall).isInSiege());
		if (isFort || isCastle || isHall)
		{
			broadcastPacket(new SystemMessage(SystemMessageId.THE_CASTLE_GATE_HAS_BEEN_DESTROYED));
		}
		
		return true;
	}
	
	@Override
	public void moveToLocation(int x, int y, int z, int offset)
	{
	}
	
	@Override
	public void stopMove(Location loc)
	{
	}
	
	@Override
	public void doAttack(Creature target)
	{
	}
	
	@Override
	public void doCast(Skill skill)
	{
	}
	
	@Override
	public void sendInfo(Player player)
	{
		if (isVisibleFor(player))
		{
			player.sendPacket(new StaticObjectInfo(this, player.isGM()));
			player.sendPacket(new DoorStatusUpdate(this));
			if (getEmitter() > 0)
			{
				player.sendPacket(new OnEventTrigger(this, !_open));
			}
		}
	}
	
	public void setTargetable(boolean value)
	{
		_isTargetable = value;
		broadcastStatusUpdate();
	}
	
	@Override
	public boolean isTargetable()
	{
		return _isTargetable;
	}
	
	public boolean checkCollision()
	{
		return getTemplate().isCheckCollision();
	}
	
	/**
	 * All doors are stored at DoorTable except instance doors
	 * @param doorId
	 * @return
	 */
	private Door getSiblingDoor(int doorId)
	{
		if (getInstanceId() == 0)
		{
			return DoorData.getInstance().getDoor(doorId);
		}
		
		final Instance inst = InstanceManager.getInstance().getInstance(getInstanceId());
		if (inst != null)
		{
			return inst.getDoor(doorId);
		}
		
		return null; // 2 late
	}
	
	private void startAutoCloseTask()
	{
		final DoorTemplate template = getTemplate();
		// Clanhall doors should not auto close.
		if ((template.getCloseTime() < 0) || isOpenableByTime() || (template.getClanHallId() > 0))
		{
			return;
		}
		
		final Future<?> oldTask = _autoCloseTask;
		if (oldTask != null)
		{
			_autoCloseTask = null;
			oldTask.cancel(false);
		}
		
		_autoCloseTask = ThreadPool.schedule(new AutoClose(), getTemplate().getCloseTime() * 1000L);
	}
	
	class AutoClose implements Runnable
	{
		@Override
		public void run()
		{
			if (_open)
			{
				closeMe();
			}
		}
	}
	
	class TimerOpen implements Runnable
	{
		@Override
		public void run()
		{
			if (_open)
			{
				closeMe();
			}
			else
			{
				openMe();
			}
			
			int delay = _open ? getTemplate().getCloseTime() : getTemplate().getOpenTime();
			if (getTemplate().getRandomTime() > 0)
			{
				delay += Rnd.get(getTemplate().getRandomTime());
			}
			ThreadPool.schedule(this, delay * 1000L);
		}
	}
	
	@Override
	public boolean isDoor()
	{
		return true;
	}
	
	@Override
	public Door asDoor()
	{
		return this;
	}
	
	@Override
	public String toString()
	{
		final StringBuilder sb = new StringBuilder();
		sb.append(getClass().getSimpleName());
		sb.append("[");
		sb.append(getTemplate().getId());
		sb.append("](");
		sb.append(getObjectId());
		sb.append(")");
		return sb.toString();
	}
}
