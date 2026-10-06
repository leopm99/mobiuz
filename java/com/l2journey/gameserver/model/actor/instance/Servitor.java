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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.l2journey.Config;
import com.l2journey.commons.database.DatabaseFactory;
import com.l2journey.commons.threads.ThreadPool;
import com.l2journey.gameserver.data.sql.CharSummonTable;
import com.l2journey.gameserver.data.sql.SummonEffectTable;
import com.l2journey.gameserver.data.xml.SkillData;
import com.l2journey.gameserver.model.WorldObject;
import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.Summon;
import com.l2journey.gameserver.model.actor.enums.creature.InstanceType;
import com.l2journey.gameserver.model.actor.templates.NpcTemplate;
import com.l2journey.gameserver.model.item.enums.ItemProcessType;
import com.l2journey.gameserver.model.item.holders.ItemHolder;
import com.l2journey.gameserver.model.skill.AbnormalType;
import com.l2journey.gameserver.model.skill.BuffInfo;
import com.l2journey.gameserver.model.skill.EffectScope;
import com.l2journey.gameserver.model.skill.Skill;
import com.l2journey.gameserver.model.skill.enums.SkillFinishType;
import com.l2journey.gameserver.model.stats.Stat;
import com.l2journey.gameserver.network.SystemMessageId;
import com.l2journey.gameserver.network.serverpackets.SetSummonRemainTime;
import com.l2journey.gameserver.network.serverpackets.SystemMessage;

/**
 * @author UnAfraid
 */
public class Servitor extends Summon implements Runnable
{
	protected static final Logger log = Logger.getLogger(Servitor.class.getName());
	
	private static final String ADD_SKILL_SAVE = "REPLACE INTO character_summon_skills_save (ownerId,ownerClassIndex,summonSkillId,skill_id,skill_level,remaining_time,buff_index) VALUES (?,?,?,?,?,?,?)";
	private static final String RESTORE_SKILL_SAVE = "SELECT skill_id,skill_level,remaining_time,buff_index FROM character_summon_skills_save WHERE ownerId=? AND ownerClassIndex=? AND summonSkillId=? ORDER BY buff_index ASC";
	private static final String DELETE_SKILL_SAVE = "DELETE FROM character_summon_skills_save WHERE ownerId=? AND ownerClassIndex=? AND summonSkillId=?";
	
	private float _expMultiplier = 0;
	private ItemHolder _itemConsume;
	private int _lifeTime;
	private int _lifeTimeRemaining;
	private int _consumeItemInterval;
	private int _consumeItemIntervalRemaining;
	protected Future<?> _summonLifeTask;
	
	private int _referenceSkill;
	
	public Servitor(NpcTemplate template, Player owner)
	{
		super(template, owner);
		setInstanceType(InstanceType.Servitor);
		setShowSummonAnimation(true);
	}
	
	@Override
	public void onSpawn()
	{
		super.onSpawn();
		if (_summonLifeTask == null)
		{
			_summonLifeTask = ThreadPool.scheduleAtFixedRate(this, 0, 5000);
		}
		
		// Enable servitor actions and update shortcuts
		sendPetActionList();
	}
	
	@Override
	public int getLevel()
	{
		return getTemplate() != null ? getTemplate().getLevel() : 0;
	}
	
	@Override
	public int getSummonType()
	{
		return 1;
	}
	
	public void setExpMultiplier(float expMultiplier)
	{
		_expMultiplier = expMultiplier;
	}
	
	public float getExpMultiplier()
	{
		return _expMultiplier;
	}
	
	public void setItemConsume(ItemHolder item)
	{
		_itemConsume = item;
	}
	
	public ItemHolder getItemConsume()
	{
		return _itemConsume;
	}
	
	public void setItemConsumeInterval(int interval)
	{
		_consumeItemInterval = interval;
		_consumeItemIntervalRemaining = interval;
	}
	
	public int getItemConsumeInterval()
	{
		return _consumeItemInterval;
	}
	
	public void setLifeTime(int lifeTime)
	{
		_lifeTime = lifeTime;
		_lifeTimeRemaining = lifeTime;
	}
	
	public int getLifeTime()
	{
		return _lifeTime;
	}
	
	public void setLifeTimeRemaining(int time)
	{
		_lifeTimeRemaining = time;
	}
	
	public int getLifeTimeRemaining()
	{
		return _lifeTimeRemaining;
	}
	
	public void setReferenceSkill(int skillId)
	{
		_referenceSkill = skillId;
	}
	
	public int getReferenceSkill()
	{
		return _referenceSkill;
	}
	
	@Override
	public boolean doDie(Creature killer)
	{
		if (!super.doDie(killer))
		{
			return false;
		}
		
		if (_summonLifeTask != null)
		{
			_summonLifeTask.cancel(false);
		}
		
		CharSummonTable.getInstance().removeServitor(getOwner());
		return true;
	}
	
	@Override
	public void doPickupItem(WorldObject object)
	{
	}
	
	@Override
	public void setRestoreSummon(boolean value)
	{
		_restoreSummon = value;
	}
	
	@Override
	public void stopSkillEffects(SkillFinishType type, int skillId)
	{
		super.stopSkillEffects(type, skillId);
		SummonEffectTable.getInstance().removeServitorEffects(getOwner(), getReferenceSkill(), skillId);
	}
	
	@Override
	public void storeMe()
	{
		if ((_referenceSkill == 0) || isDead())
		{
			return;
		}
		
		if (Config.RESTORE_SERVITOR_ON_RECONNECT)
		{
			CharSummonTable.getInstance().saveSummon(this);
		}
	}
	
	@Override
	public void storeEffect(boolean storeEffects)
	{
		if (!Config.SUMMON_STORE_SKILL_COOLTIME || (getOwner() == null) || getOwner().isInOlympiadMode())
		{
			return;
		}
		
		// Clear list for overwrite
		SummonEffectTable.getInstance().clearServitorEffects(getOwner(), getReferenceSkill());
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(DELETE_SKILL_SAVE))
		{
			// Delete all current stored effects for summon to avoid dupe
			ps.setInt(1, getOwner().getObjectId());
			ps.setInt(2, getOwner().getClassIndex());
			ps.setInt(3, _referenceSkill);
			ps.execute();
			
			int buffIndex = 0;
			
			final List<Integer> storedSkills = new LinkedList<>();
			
			// Store all effect data along with calculated remaining
			if (storeEffects)
			{
				try (PreparedStatement ps2 = con.prepareStatement(ADD_SKILL_SAVE))
				{
					ps2.setInt(1, getOwner().getObjectId());
					ps2.setInt(2, getOwner().getClassIndex());
					ps2.setInt(3, _referenceSkill);
					for (BuffInfo info : getEffectList().getEffects())
					{
						if (info == null)
						{
							continue;
						}
						
						final Skill skill = info.getSkill();
						// Do not save heals.
						// Dances and songs are not kept in retail.
						if ((skill.getAbnormalType() == AbnormalType.LIFE_FORCE_OTHERS) || skill.isToggle() || (skill.isDance() && !Config.ALT_STORE_DANCES))
						{
							continue;
						}
						
						if (storedSkills.contains(skill.getReuseHashCode()))
						{
							continue;
						}
						
						storedSkills.add(skill.getReuseHashCode());
						
						ps2.setInt(4, skill.getId());
						ps2.setInt(5, skill.getLevel());
						ps2.setInt(6, info.getTime());
						ps2.setInt(7, ++buffIndex);
						ps2.addBatch();
						
						SummonEffectTable.getInstance().addServitorEffect(getOwner(), getReferenceSkill(), skill, info.getTime());
					}
					ps2.executeBatch();
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Could not store summon effect data: ", e);
		}
	}
	
	@Override
	public void restoreEffects()
	{
		if (getOwner().isInOlympiadMode())
		{
			return;
		}
		
		try (Connection con = DatabaseFactory.getConnection())
		{
			if (!SummonEffectTable.getInstance().containsSkill(getOwner(), getReferenceSkill()))
			{
				try (PreparedStatement ps = con.prepareStatement(RESTORE_SKILL_SAVE))
				{
					ps.setInt(1, getOwner().getObjectId());
					ps.setInt(2, getOwner().getClassIndex());
					ps.setInt(3, _referenceSkill);
					try (ResultSet rs = ps.executeQuery())
					{
						while (rs.next())
						{
							final int effectCurTime = rs.getInt("remaining_time");
							final Skill skill = SkillData.getInstance().getSkill(rs.getInt("skill_id"), rs.getInt("skill_level"));
							if (skill == null)
							{
								continue;
							}
							
							if (skill.hasEffects(EffectScope.GENERAL))
							{
								SummonEffectTable.getInstance().addServitorEffect(getOwner(), getReferenceSkill(), skill, effectCurTime);
							}
						}
					}
				}
			}
			
			try (PreparedStatement statement = con.prepareStatement(DELETE_SKILL_SAVE))
			{
				statement.setInt(1, getOwner().getObjectId());
				statement.setInt(2, getOwner().getClassIndex());
				statement.setInt(3, _referenceSkill);
				statement.executeUpdate();
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Could not restore " + this + " active effect data: " + e.getMessage(), e);
		}
		finally
		{
			SummonEffectTable.getInstance().applyServitorEffects(this, getOwner(), getReferenceSkill());
		}
	}
	
	@Override
	public void unSummon(Player owner)
	{
		if (_summonLifeTask != null)
		{
			_summonLifeTask.cancel(false);
		}
		
		super.unSummon(owner);
		
		if (!_restoreSummon)
		{
			CharSummonTable.getInstance().removeServitor(owner);
		}
	}
	
	@Override
	public boolean destroyItem(ItemProcessType process, int objectId, long count, WorldObject reference, boolean sendMessage)
	{
		return getOwner().destroyItem(process, objectId, count, reference, sendMessage);
	}
	
	@Override
	public boolean destroyItemByItemId(ItemProcessType process, int itemId, long count, WorldObject reference, boolean sendMessage)
	{
		return getOwner().destroyItemByItemId(process, itemId, count, reference, sendMessage);
	}
	
	@Override
	public byte getAttackElement()
	{
		return getOwner() != null ? getOwner().getAttackElement() : super.getAttackElement();
	}
	
	@Override
	public int getAttackElementValue(byte attackAttribute)
	{
		return getOwner() != null ? getOwner().getAttackElementValue(attackAttribute) : super.getAttackElementValue(attackAttribute);
	}
	
	@Override
	public int getDefenseElementValue(byte defenseAttribute)
	{
		return getOwner() != null ? getOwner().getDefenseElementValue(defenseAttribute) : super.getDefenseElementValue(defenseAttribute);
	}
	
	@Override
	public boolean isServitor()
	{
		return true;
	}
	
	@Override
	public Servitor asServitor()
	{
		return this;
	}
	
	@Override
	public void run()
	{
		final int usedtime = 5000;
		_lifeTimeRemaining -= usedtime;
		if (isDead() || !isSpawned())
		{
			if (_summonLifeTask != null)
			{
				_summonLifeTask.cancel(false);
			}
			return;
		}
		
		// check if the summon's lifetime has ran out
		if (_lifeTimeRemaining < 0)
		{
			sendPacket(SystemMessageId.YOUR_SERVITOR_PASSED_AWAY);
			unSummon(getOwner());
			return;
		}
		
		if (_consumeItemInterval > 0)
		{
			_consumeItemIntervalRemaining -= usedtime;
			
			// check if it is time to consume another item
			if ((_consumeItemIntervalRemaining <= 0) && (_itemConsume.getCount() > 0) && (_itemConsume.getId() > 0) && !isDead())
			{
				if (destroyItemByItemId(null, _itemConsume.getId(), _itemConsume.getCount(), this, false))
				{
					final SystemMessage msg = new SystemMessage(SystemMessageId.A_SUMMONED_MONSTER_USES_S1);
					msg.addItemName(_itemConsume.getId());
					sendPacket(msg);
					
					// Reset
					_consumeItemIntervalRemaining = _consumeItemInterval;
				}
				else
				{
					sendPacket(SystemMessageId.SINCE_YOU_DO_NOT_HAVE_ENOUGH_ITEMS_TO_MAINTAIN_THE_SERVITOR_S_STAY_THE_SERVITOR_HAS_DISAPPEARED);
					unSummon(getOwner());
				}
			}
		}
		
		sendPacket(new SetSummonRemainTime(_lifeTime, _lifeTimeRemaining));
		updateEffectIcons();
	}
	
	@Override
	public double getMAtk(Creature target, Skill skill)
	{
		final Player owner = getOwner();
		if (owner == null)
		{
			return super.getMAtk(target, skill);
		}
		
		return super.getMAtk(target, skill) + (owner.getMAtk(target, skill) * (owner.getServitorShareBonus(Stat.MAGIC_ATTACK) - 1.0));
	}
	
	@Override
	public double getMDef(Creature target, Skill skill)
	{
		final Player owner = getOwner();
		if (owner == null)
		{
			return super.getMDef(target, skill);
		}
		
		return super.getMDef(target, skill) + (owner.getMDef(target, skill) * (owner.getServitorShareBonus(Stat.MAGIC_DEFENCE) - 1.0));
	}
	
	@Override
	public double getPAtk(Creature target)
	{
		final Player owner = getOwner();
		if (owner == null)
		{
			return super.getPAtk(target);
		}
		
		return super.getPAtk(target) + (owner.getPAtk(target) * (owner.getServitorShareBonus(Stat.POWER_ATTACK) - 1.0));
	}
	
	@Override
	public double getPDef(Creature target)
	{
		final Player owner = getOwner();
		if (owner == null)
		{
			return super.getPDef(target);
		}
		
		return super.getPDef(target) + (owner.getPDef(target) * (owner.getServitorShareBonus(Stat.POWER_DEFENCE) - 1.0));
	}
	
	@Override
	public int getMAtkSpd()
	{
		final Player owner = getOwner();
		if (owner == null)
		{
			return super.getMAtkSpd();
		}
		
		return (int) (super.getMAtkSpd() + (owner.getMAtkSpd() * (owner.getServitorShareBonus(Stat.MAGIC_ATTACK_SPEED) - 1.0)));
	}
	
	@Override
	public int getMaxHp()
	{
		final Player owner = getOwner();
		if (owner == null)
		{
			return super.getMaxHp();
		}
		
		return (int) (super.getMaxHp() + (owner.getMaxHp() * (owner.getServitorShareBonus(Stat.MAX_HP) - 1.0)));
	}
	
	@Override
	public int getMaxMp()
	{
		final Player owner = getOwner();
		if (owner == null)
		{
			return super.getMaxMp();
		}
		
		return (int) (super.getMaxMp() + (owner.getMaxMp() * (owner.getServitorShareBonus(Stat.MAX_MP) - 1.0)));
	}
	
	@Override
	public int getCriticalHit(Creature target, Skill skill)
	{
		final Player owner = getOwner();
		if (owner == null)
		{
			return super.getCriticalHit(target, skill);
		}
		
		return (int) (super.getCriticalHit(target, skill) + ((owner.getCriticalHit(target, skill)) * (owner.getServitorShareBonus(Stat.CRITICAL_RATE) - 1.0)));
	}
	
	@Override
	public double getPAtkSpd()
	{
		final Player owner = getOwner();
		if (owner == null)
		{
			return super.getPAtkSpd();
		}
		
		return super.getPAtkSpd() + (owner.getPAtkSpd() * (owner.getServitorShareBonus(Stat.POWER_ATTACK_SPEED) - 1.0));
	}
}