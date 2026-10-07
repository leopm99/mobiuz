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
package com.l2journey.gameserver.data.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import com.l2journey.Config;
import com.l2journey.commons.database.DatabaseFactory;
import com.l2journey.gameserver.data.xml.NpcData;
import com.l2journey.gameserver.data.xml.PetDataTable;
import com.l2journey.gameserver.data.xml.SkillData;
import com.l2journey.gameserver.model.PetData;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.instance.Pet;
import com.l2journey.gameserver.model.actor.instance.Servitor;
import com.l2journey.gameserver.model.actor.templates.NpcTemplate;
import com.l2journey.gameserver.model.item.instance.Item;
import com.l2journey.gameserver.model.skill.Skill;
import com.l2journey.gameserver.network.serverpackets.PetItemList;

/**
 * @author Nyaran
 */
public class CharSummonTable
{
	private static final Logger LOGGER = Logger.getLogger(CharSummonTable.class.getName());
	private static final Map<Integer, Integer> _pets = new ConcurrentHashMap<>();
	private static final Map<Integer, Integer> _servitors = new ConcurrentHashMap<>();
	
	// SQL
	private static final String INIT_PET = "SELECT ownerId, item_obj_id FROM pets WHERE restore = 'true'";
	private static final String INIT_SUMMONS = "SELECT ownerId, summonSkillId FROM character_summons";
	private static final String LOAD_SUMMON = "SELECT curHp, curMp, time FROM character_summons WHERE ownerId = ? AND summonSkillId = ?";
	private static final String REMOVE_SUMMON = "DELETE FROM character_summons WHERE ownerId = ?";
	private static final String SAVE_SUMMON = "REPLACE INTO character_summons (ownerId,summonSkillId,curHp,curMp,time) VALUES (?,?,?,?,?)";
	
	public Map<Integer, Integer> getPets()
	{
		return _pets;
	}
	
	public Map<Integer, Integer> getServitors()
	{
		return _servitors;
	}
	
	public void init()
	{
		if (Config.RESTORE_SERVITOR_ON_RECONNECT)
		{
			try (Connection con = DatabaseFactory.getConnection();
				Statement s = con.createStatement();
				ResultSet rs = s.executeQuery(INIT_SUMMONS))
			{
				while (rs.next())
				{
					_servitors.put(rs.getInt("ownerId"), rs.getInt("summonSkillId"));
				}
			}
			catch (Exception e)
			{
				LOGGER.warning(getClass().getSimpleName() + ": Error while loading saved servitor: " + e);
			}
		}
		
		if (Config.RESTORE_PET_ON_RECONNECT)
		{
			try (Connection con = DatabaseFactory.getConnection();
				Statement s = con.createStatement();
				ResultSet rs = s.executeQuery(INIT_PET))
			{
				while (rs.next())
				{
					_pets.put(rs.getInt("ownerId"), rs.getInt("item_obj_id"));
				}
			}
			catch (Exception e)
			{
				LOGGER.warning(getClass().getSimpleName() + ": Error while loading saved pet: " + e);
			}
		}
	}
	
	public void removeServitor(Player player)
	{
		_servitors.remove(player.getObjectId());
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(REMOVE_SUMMON))
		{
			ps.setInt(1, player.getObjectId());
			ps.execute();
		}
		catch (SQLException e)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Summon cannot be removed: " + e);
		}
	}
	
	public void restorePet(Player player)
	{
		final Item item = player.getInventory().getItemByObjectId(_pets.get(player.getObjectId()));
		if (item == null)
		{
			LOGGER.warning(() -> getClass().getSimpleName() + ": Null pet summoning item for: " + player);
			return;
		}
		final PetData petData = PetDataTable.getInstance().getPetDataByItemId(item.getId());
		if (petData == null)
		{
			LOGGER.warning(() -> getClass().getSimpleName() + ": Null pet data for: " + player + " and summoning item: " + item);
			return;
		}
		final NpcTemplate npcTemplate = NpcData.getInstance().getTemplate(petData.getNpcId());
		if (npcTemplate == null)
		{
			LOGGER.warning(() -> getClass().getSimpleName() + ": Null pet NPC template for: " + player + " and pet Id:" + petData.getNpcId());
			return;
		}
		
		final Pet pet = Pet.spawnPet(npcTemplate, player, item);
		if (pet == null)
		{
			LOGGER.warning(() -> getClass().getSimpleName() + ": Null pet instance for: " + player + " and pet NPC template:" + npcTemplate);
			return;
		}
		
		pet.setShowSummonAnimation(true);
		pet.setTitle(player.getName());
		
		if (!pet.isRespawned())
		{
			pet.setCurrentHp(pet.getMaxHp());
			pet.setCurrentMp(pet.getMaxMp());
			pet.getStat().setExp(pet.getExpForThisLevel());
			pet.setCurrentFed(pet.getMaxFed());
		}
		
		pet.setRunning();
		
		if (!pet.isRespawned())
		{
			pet.storeMe();
		}
		
		item.setEnchantLevel(pet.getLevel());
		player.setPet(pet);
		pet.spawnMe(player.getX() + 50, player.getY() + 100, player.getZ());
		pet.startFeed();
		pet.setFollowStatus(true);
		pet.getOwner().sendPacket(new PetItemList(pet.getInventory().getItems()));
		pet.broadcastStatusUpdate();
	}
	
	public void restoreServitor(Player player)
	{
		final int skillId = _servitors.get(player.getObjectId());
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(LOAD_SUMMON))
		{
			ps.setInt(1, player.getObjectId());
			ps.setInt(2, skillId);
			try (ResultSet rs = ps.executeQuery())
			{
				Skill skill;
				while (rs.next())
				{
					final int curHp = rs.getInt("curHp");
					final int curMp = rs.getInt("curMp");
					final int time = rs.getInt("time");
					
					skill = SkillData.getInstance().getSkill(skillId, player.getSkillLevel(skillId));
					if (skill == null)
					{
						removeServitor(player);
						return;
					}
					
					skill.applyEffects(player, player);
					
					if (player.hasServitor())
					{
						final Servitor summon = player.getSummon().asServitor();
						summon.setCurrentHp(curHp);
						summon.setCurrentMp(curMp);
						summon.setLifeTimeRemaining(time);
					}
				}
			}
		}
		catch (SQLException e)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Servitor cannot be restored: " + e);
		}
	}
	
	public void saveSummon(Servitor summon)
	{
		if ((summon == null) || (summon.getLifeTimeRemaining() <= 0))
		{
			return;
		}
		
		_servitors.put(summon.getOwner().getObjectId(), summon.getReferenceSkill());
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(SAVE_SUMMON))
		{
			ps.setInt(1, summon.getOwner().getObjectId());
			ps.setInt(2, summon.getReferenceSkill());
			ps.setInt(3, (int) Math.round(summon.getCurrentHp()));
			ps.setInt(4, (int) Math.round(summon.getCurrentMp()));
			ps.setInt(5, summon.getLifeTimeRemaining());
			ps.execute();
		}
		catch (Exception e)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Failed to store summon: " + summon + " from " + summon.getOwner() + ", error: " + e);
		}
	}
	
	public static CharSummonTable getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final CharSummonTable INSTANCE = new CharSummonTable();
	}
}
