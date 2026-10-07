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
package com.l2journey.gameserver.taskmanagers;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.l2journey.Config;
import com.l2journey.commons.threads.ThreadPool;
import com.l2journey.gameserver.data.xml.PetSkillData;
import com.l2journey.gameserver.handler.IItemHandler;
import com.l2journey.gameserver.handler.ItemHandler;
import com.l2journey.gameserver.model.WorldObject;
import com.l2journey.gameserver.model.actor.Playable;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.Summon;
import com.l2journey.gameserver.model.actor.instance.Guard;
import com.l2journey.gameserver.model.item.EtcItem;
import com.l2journey.gameserver.model.item.ItemTemplate;
import com.l2journey.gameserver.model.item.instance.Item;
import com.l2journey.gameserver.model.skill.AbnormalType;
import com.l2journey.gameserver.model.skill.BuffInfo;
import com.l2journey.gameserver.model.skill.Skill;
import com.l2journey.gameserver.model.skill.holders.SkillHolder;
import com.l2journey.gameserver.model.skill.targets.TargetType;
import com.l2journey.gameserver.model.zone.ZoneId;

/**
 * @author Mobius, KingHanker, Mafias
 */
public class AutoUseTaskManager
{
	private static final Set<Set<Player>> POOLS = ConcurrentHashMap.newKeySet();
	private static final int POOL_SIZE = 200;
	private static final int TASK_DELAY = 300;
	private static final int REUSE_MARGIN_TIME = 3;
	
	protected AutoUseTaskManager()
	{
	}
	
	private class AutoUse implements Runnable
	{
		private final Set<Player> _players;
		
		public AutoUse(Set<Player> players)
		{
			_players = players;
		}
		
		@Override
		public void run()
		{
			if (_players.isEmpty())
			{
				return;
			}
			
			for (Player player : _players)
			{
				if (!player.isOnline() || (player.isInOfflineMode() && !player.isOfflinePlay()) || player.isTransformed())
				{
					stopAutoUseTask(player);
					continue;
				}
				
				if (player.isSitting() || player.isStunned() || player.isSleeping() || player.isParalyzed() || player.isAfraid() || player.isAlikeDead() || player.isMounted() || (player.isTransformed() && player.getTransformation().isRiding()))
				{
					continue;
				}
				
				final boolean isInPeaceZone = player.isInsideZone(ZoneId.PEACE);
				
				if (Config.ENABLE_AUTO_ITEM && !isInPeaceZone)
				{
					ITEMS: for (Integer itemId : player.getAutoUseSettings().getAutoSupplyItems())
					{
						if (player.isTeleporting())
						{
							break ITEMS;
						}
						
						final Item item = player.getInventory().getItemByItemId(itemId.intValue());
						if (item == null)
						{
							player.getAutoUseSettings().getAutoSupplyItems().remove(itemId);
							continue ITEMS;
						}
						
						final ItemTemplate template = item.getTemplate();
						if ((template == null) || !template.checkCondition(player, player, false))
						{
							continue ITEMS;
						}
						
						final SkillHolder[] skills = item.getTemplate().getSkills();
						if (skills != null)
						{
							ITEM_SKILL: for (SkillHolder itemSkillHolder : skills)
							{
								final Skill skill = itemSkillHolder.getSkill();
								if (!skill.isActive())
								{
									continue ITEM_SKILL;
								}
								
								if (player.isAffectedBySkill(skill.getId()) || player.hasSkillReuse(skill.getReuseHashCode()) || !skill.checkCondition(player, player, false))
								{
									continue ITEMS;
								}
							}
						}
						
						final int reuseDelay = item.getReuseDelay();
						if ((reuseDelay <= 0) || (player.getItemRemainingReuseTime(item.getObjectId()) <= 0))
						{
							final IItemHandler handler = ItemHandler.getInstance().getHandler(item.getEtcItem());
							if ((handler != null) && handler.useItem(player, item, false) && (reuseDelay > 0))
							{
								player.addTimeStampItem(item, reuseDelay);
							}
						}
					}
				}
				
				// Checa HP
				if (Config.ENABLE_AUTO_POTION && !isInPeaceZone && (player.getCurrentHpPercent() < player.getAutoPlaySettings().getAutoPotionPercent()))
				{
					final int itemId = player.getAutoUseSettings().getAutoPotionItem();
					if (itemId > 0)
					{
						final Item item = player.getInventory().getItemByItemId(itemId);
						if (item == null)
						{
							player.getAutoUseSettings().setAutoPotionItem(0);
						}
						else
						{
							final int reuseDelay = item.getReuseDelay();
							if ((reuseDelay <= 0) || (player.getItemRemainingReuseTime(item.getObjectId()) <= 0))
							{
								final EtcItem etcItem = item.getEtcItem();
								final IItemHandler handler = ItemHandler.getInstance().getHandler(etcItem);
								if ((handler != null) && handler.useItem(player, item, false) && (reuseDelay > 0))
								{
									player.addTimeStampItem(item, reuseDelay);
								}
							}
						}
					}
				}
				
				// Checa MP (independente do HP)
				if (Config.ENABLE_AUTO_POTION && !isInPeaceZone && (player.getCurrentMpPercent() < player.getAutoPlaySettings().getAutoManaPotionPercent()))
				{
					final int itemId = player.getAutoUseSettings().getAutoManaPotionItem();
					
					if (itemId > 0)
					{
						final Item item = player.getInventory().getItemByItemId(itemId);
						if (item == null)
						{
							player.getAutoUseSettings().setAutoManaPotionItem(0);
						}
						else
						{
							final int reuseDelay = item.getReuseDelay();
							if ((reuseDelay <= 0) || (player.getItemRemainingReuseTime(item.getObjectId()) <= 0))
							{
								final EtcItem etcItem = item.getEtcItem();
								final IItemHandler handler = ItemHandler.getInstance().getHandler(etcItem);
								if ((handler != null) && handler.useItem(player, item, false) && (reuseDelay > 0))
								{
									player.addTimeStampItem(item, reuseDelay);
								}
							}
						}
					}
				}
				
				if (Config.ENABLE_AUTO_SKILL)
				{
					BUFFS: for (Integer skillId : player.getAutoUseSettings().getAutoBuffs())
					{
						// Fixes start area issue.
						// Already casting.
						// Attacking.
						// Player is teleporting.
						if (isInPeaceZone || player.isCastingNow() || player.isAttackingNow() || player.isTeleporting())
						{
							break BUFFS;
						}
						
						Playable pet = null;
						Skill skill = player.getKnownSkill(skillId);
						if (skill == null)
						{
							if (player.hasServitor() || player.hasPet())
							{
								final Summon summon = player.getSummon();
								skill = summon.getKnownSkill(skillId);
								if (skill == null)
								{
									skill = PetSkillData.getInstance().getKnownSkill(summon, skillId);
								}
								if (skill != null)
								{
									pet = summon;
								}
							}
							if (skill == null)
							{
								player.getAutoUseSettings().getAutoBuffs().remove(skillId);
								continue BUFFS;
							}
						}
						
						// Se estiver em party, aplicar o buff em todos os membros próximos.
						if (player.getParty() != null)
						{
							for (Player member : player.getParty().getMembers())
							{
								// Checa distância (usar mesmo range do buff)
								if ((member == null) || member.isDead() || member.isAlikeDead() || (player.calculateDistance3D(member) > skill.getCastRange()))
								{
									continue;
								}
								// Checa se precisa do buff
								if (!canCastBuff(player, member, skill))
								{
									continue;
								}
								// Não interrompe ação do player
								if (player.isCastingNow() || player.isAttackingNow() || player.isTeleporting())
								{
									break BUFFS;
								}
								// Aplica o buff
								final WorldObject savedTarget = player.getTarget();
								player.setTarget(member);
								player.doCast(skill);
								player.setTarget(savedTarget);
							}
						}
						else
						{
							// Buff use check.
							final WorldObject target = player.getTarget();
							if (!canCastBuff(player, target, skill))
							{
								continue BUFFS;
							}
							
							// Playable target cast.
							final Playable caster = pet != null ? pet : player;
							if ((target != null) && (target.isPlayable()))
							{
								final Player targetPlayer = target.asPlayer();
								if (((targetPlayer.getPvpFlag() == 0) && (targetPlayer.getKarma() <= 0)) || (targetPlayer.getParty() == caster.getParty()))
								{
									caster.doCast(skill);
								}
								else
								{
									if (!caster.getEffectList().isAffectedBySkill(skill.getId()))
									{
										final WorldObject savedTarget = target;
										caster.setTarget(caster);
										caster.doCast(skill);
										caster.setTarget(savedTarget);
									}
								}
							}
							else // Target self, cast and re-target.
							{
								final WorldObject savedTarget = target;
								caster.setTarget(caster);
								caster.doCast(skill);
								caster.setTarget(savedTarget);
							}
						}
					}
					
					// Continue when auto play is not enabled.
					if (!player.isAutoPlaying())
					{
						continue;
					}
					
					final int count = player.getAutoUseSettings().getAutoSkills().size();
					SKILLS: for (int i = 0; i < count; i++)
					{
						// Already casting.
						// Player is teleporting.
						if (player.isCastingNow() || player.isTeleporting())
						{
							break SKILLS;
						}
						
						// Acquire next skill.
						Playable pet = null;
						final WorldObject target = player.getTarget();
						final Integer skillId = player.getAutoUseSettings().getNextSkillId();
						Skill skill = player.getKnownSkill(skillId);
						if (skill == null)
						{
							if (player.hasServitor() || player.hasPet())
							{
								final Summon summon = player.getSummon();
								skill = summon.getKnownSkill(skillId);
								if (skill == null)
								{
									skill = PetSkillData.getInstance().getKnownSkill(summon, skillId);
								}
								if (skill != null)
								{
									pet = summon;
									pet.setTarget(target);
								}
							}
							if (skill == null)
							{
								player.getAutoUseSettings().getAutoSkills().remove(skillId);
								player.getAutoUseSettings().resetSkillOrder();
								break SKILLS;
							}
						}
						
						// Casting on self stops movement.
						if (target == player)
						{
							break SKILLS;
						}
						
						// Check negative effect skill target.
						if ((target == null) || target.asCreature().isDead())
						{
							// Remove queued skill.
							if (player.getQueuedSkill() != null)
							{
								player.setQueuedSkill(null, false, false);
							}
							break SKILLS;
						}
						
						// Peace zone and auto attackable checks.
						if (target.isInsideZone(ZoneId.PEACE) || !target.isAutoAttackable(player))
						{
							break SKILLS;
						}
						
						// Do not attack guards.
						if (target instanceof Guard)
						{
							final int targetMode = player.getAutoPlaySettings().getNextTargetMode();
							if ((targetMode != 3 /* NPC */) && (targetMode != 0 /* Any Target */))
							{
								break SKILLS;
							}
						}
						
						// Increment skill order.
						player.getAutoUseSettings().incrementSkillOrder();
						
						// Skill use check.
						final Playable caster = pet != null ? pet : player;
						if (!canUseMagic(caster, target, skill))
						{
							continue SKILLS;
						}
						
						// Use the skill.
						caster.useMagic(skill, true, false);
						
						break SKILLS;
					}
				}
			}
		}
		
		private boolean canCastBuff(Player player, WorldObject target, Skill skill)
		{
			if ((target != null) && target.isCreature() && target.asCreature().isAlikeDead() && (skill.getTargetType() != TargetType.SELF) && (skill.getTargetType() != TargetType.CORPSE) && (skill.getTargetType() != TargetType.PC_BODY))
			{
				return false;
			}
			
			final Playable playableTarget = (target == null) || !target.isPlayable() || (skill.getTargetType() == TargetType.SELF) ? player : target.asPlayable();
			if (((player != playableTarget) && (player.calculateDistance3D(playableTarget) > skill.getCastRange())) || !canUseMagic(player, playableTarget, skill))
			{
				return false;
			}
			
			final BuffInfo buffInfo = playableTarget.getEffectList().getBuffInfoBySkillId(skill.getId());
			final BuffInfo abnormalBuffInfo = playableTarget.getEffectList().getBuffInfoByAbnormalType(skill.getAbnormalType());
			if (abnormalBuffInfo != null)
			{
				if (buffInfo != null)
				{
					return (abnormalBuffInfo.getSkill().getId() == buffInfo.getSkill().getId()) && ((buffInfo.getTime() <= REUSE_MARGIN_TIME) || (buffInfo.getSkill().getLevel() < skill.getLevel()));
				}
				return (abnormalBuffInfo.getSkill().getAbnormalLevel() < skill.getAbnormalLevel()) || abnormalBuffInfo.isAbnormalType(AbnormalType.NONE);
			}
			return buffInfo == null;
		}
		
		private boolean canUseMagic(Playable playable, WorldObject target, Skill skill)
		{
			if (((skill.getItemConsumeCount() > 0) && (playable.getInventory().getInventoryItemCount(skill.getItemConsumeId(), -1) < skill.getItemConsumeCount())) || (playable.isPlayer() && (playable.asPlayer().getCharges() < skill.getChargeConsumeCount())))
			{
				return false;
			}
			
			final int mpConsume = skill.getMpInitialConsume() + skill.getMpConsume();
			if ((mpConsume > 0) && (playable.getCurrentMp() < mpConsume))
			{
				return false;
			}
			
			// Check if monster is spoiled to avoid Spoil (254/302) skill recast.
			if (((skill.getId() == 254) || (skill.getId() == 302)) && (target != null) && target.isMonster() && target.asMonster().isSpoiled())
			{
				return false;
			}
			
			return !playable.isSkillDisabled(skill) && skill.checkCondition(playable, target, false);
		}
	}
	
	public synchronized void startAutoUseTask(Player player)
	{
		for (Set<Player> pool : POOLS)
		{
			if (pool.contains(player))
			{
				return;
			}
		}
		
		for (Set<Player> pool : POOLS)
		{
			if (pool.size() < POOL_SIZE)
			{
				pool.add(player);
				return;
			}
		}
		
		final Set<Player> pool = ConcurrentHashMap.newKeySet(POOL_SIZE);
		pool.add(player);
		ThreadPool.schedulePriorityTaskAtFixedRate(new AutoUse(pool), TASK_DELAY, TASK_DELAY);
		POOLS.add(pool);
	}
	
	public void stopAutoUseTask(Player player)
	{
		player.getAutoUseSettings().resetSkillOrder();
		for (Set<Player> pool : POOLS)
		{
			if (pool.remove(player))
			{
				return;
			}
		}
	}
	
	public static AutoUseTaskManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final AutoUseTaskManager INSTANCE = new AutoUseTaskManager();
	}
}
