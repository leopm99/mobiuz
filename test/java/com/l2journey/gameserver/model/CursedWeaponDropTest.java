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
 */
package com.l2journey.gameserver.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.l2journey.commons.threads.ThreadPool;
import com.l2journey.gameserver.data.xml.ItemData;
import com.l2journey.gameserver.data.xml.SkillData;
import com.l2journey.gameserver.managers.MapRegionManager;
import com.l2journey.gameserver.model.actor.Attackable;
import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.item.ItemTemplate;
import com.l2journey.gameserver.model.item.instance.Item;
import com.l2journey.gameserver.util.Broadcast;

/**
 * Covers cursed-weapon monster drops when the killer has no map region (issue #11).
 */
class CursedWeaponDropTest
{
	private static final int ITEM_ID = 8689;
	private static final int SKILL_ID = 3629;
	private static final String WEAPON_NAME = "Blood Sword Akamanah";
	private static final long REMOVE_PERIOD_MS = 3L * 12000L;
	
	/** Krateis Cube coords that map to square 17,17. */
	private static final int KRATEIS_X = -81990;
	private static final int KRATEIS_Y = -12407;
	private static final int KRATEIS_Z = -8320;
	
	@Test
	void checkDropCompletesWhenMapRegionIsNull()
	{
		assertDropLogsTown(null, "Aden Castle Town");
	}
	
	@Test
	void checkDropLogsMappedTownWhenMapRegionExists()
	{
		assertDropLogsTown(new MapRegion("giran_castle_town", "Giran Castle Town", 910, 3, 9), "Giran Castle Town");
	}
	
	private static void assertDropLogsTown(MapRegion region, String expectedTown)
	{
		final List<LogRecord> records = new ArrayList<>();
		final Handler handler = recordingHandler(records);
		final Logger logger = Logger.getLogger(CursedWeapon.class.getName());
		final Level previous = logger.getLevel();
		logger.setLevel(Level.INFO);
		logger.addHandler(handler);
		try
		{
			assertDoesNotThrow(() -> runCheckDrop(region));
			assertTrue(records.stream().anyMatch(r ->
			{
				final String message = r.getMessage();
				return (message != null) && message.contains(WEAPON_NAME) && message.contains(expectedTown);
			}));
		}
		finally
		{
			logger.removeHandler(handler);
			logger.setLevel(previous);
		}
	}
	
	private static void runCheckDrop(MapRegion region)
	{
		final Item dropped = mock(Item.class);
		final Attackable attackable = mock(Attackable.class);
		when(attackable.getName()).thenReturn("Test Monster");
		when(attackable.dropItem(any(Creature.class), anyInt(), eq(1L))).thenReturn(dropped);
		
		final Player player = mock(Player.class);
		when(player.getX()).thenReturn(KRATEIS_X);
		when(player.getY()).thenReturn(KRATEIS_Y);
		when(player.getZ()).thenReturn(KRATEIS_Z);
		when(player.getName()).thenReturn("Killer");
		
		final ItemTemplate template = mock(ItemTemplate.class);
		when(template.getDisplayId()).thenReturn(ITEM_ID);
		
		final SkillData skillData = mock(SkillData.class);
		when(skillData.getMaxLevel(SKILL_ID)).thenReturn(1);
		
		final MapRegionManager mapRegionManager = mock(MapRegionManager.class);
		when(mapRegionManager.getMapRegion(any(WorldObject.class))).thenReturn(region);
		when(mapRegionManager.getClosestTownName(any(Creature.class))).thenCallRealMethod();
		
		@SuppressWarnings("unchecked")
		final ScheduledFuture<?> scheduled = mock(ScheduledFuture.class);
		
		try (MockedStatic<SkillData> skills = mockStatic(SkillData.class);
			MockedStatic<ItemData> items = mockStatic(ItemData.class);
			MockedStatic<MapRegionManager> mapRegions = mockStatic(MapRegionManager.class);
			MockedStatic<Broadcast> broadcast = mockStatic(Broadcast.class);
			MockedStatic<ThreadPool> threads = mockStatic(ThreadPool.class))
		{
			skills.when(SkillData::getInstance).thenReturn(skillData);
			
			final ItemData itemData = mock(ItemData.class);
			when(itemData.getTemplate(ITEM_ID)).thenReturn(template);
			items.when(ItemData::getInstance).thenReturn(itemData);
			
			mapRegions.when(MapRegionManager::getInstance).thenReturn(mapRegionManager);
			
			threads.when(() -> ThreadPool.scheduleAtFixedRate(any(Runnable.class), anyLong(), anyLong())).thenReturn(scheduled);
			
			final CursedWeapon weapon = new CursedWeapon(ITEM_ID, SKILL_ID, WEAPON_NAME);
			weapon.setDropRate(100000);
			weapon.setDuration(300);
			weapon.setDurationLost(3);
			
			assertTrue(weapon.checkDrop(attackable, player));
			assertTrue(weapon.isDropped());
			assertTrue(weapon.getEndTime() > System.currentTimeMillis());
			verify(dropped).setDropTime(0);
			threads.verify(() -> ThreadPool.scheduleAtFixedRate(any(Runnable.class), eq(REMOVE_PERIOD_MS), eq(REMOVE_PERIOD_MS)));
		}
	}
	
	private static Handler recordingHandler(List<LogRecord> records)
	{
		return new Handler()
		{
			@Override
			public void publish(LogRecord record)
			{
				records.add(record);
			}
			
			@Override
			public void flush()
			{
			}
			
			@Override
			public void close()
			{
			}
		};
	}
}
