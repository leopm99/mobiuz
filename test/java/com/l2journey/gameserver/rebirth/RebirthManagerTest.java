package com.l2journey.gameserver.rebirth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import com.l2journey.RebirthEngineConfigs;
import com.l2journey.gameserver.data.xml.ExperienceData;
import com.l2journey.gameserver.data.xml.ItemData;
import com.l2journey.gameserver.data.xml.SkillTreeData;
import com.l2journey.gameserver.model.SkillLearn;
import com.l2journey.gameserver.model.item.holders.ItemHolder;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.stat.PlayerStat;
import com.l2journey.gameserver.model.item.ItemTemplate;
import com.l2journey.gameserver.model.item.enums.ItemProcessType;
import com.l2journey.gameserver.model.item.instance.Item;
import com.l2journey.gameserver.model.itemcontainer.PlayerInventory;
import com.l2journey.gameserver.model.skill.Skill;
import com.l2journey.gameserver.rebirth.RebirthManager.Result;
import com.l2journey.gameserver.rebirth.RebirthRepository.Progress;

class RebirthManagerTest
{
	private RebirthRepository _repository;
	private RebirthManager _manager;
	private Player _player;
	private PlayerStat _stat;
	private PlayerInventory _inventory;
	private Skill _normal;
	private Skill _preserved;
	private Item _reward;
	private MockedStatic<ExperienceData> _experience;
	private MockedStatic<ItemData> _items;
	private MockedStatic<com.l2journey.gameserver.model.olympiad.OlympiadManager> _olympiad;
	private final java.util.Map<java.lang.reflect.Field, Object> _config = new java.util.HashMap<>();
	@BeforeEach
	void setup() throws Exception
	{
		for (var field : RebirthEngineConfigs.class.getFields()) { _config.put(field, field.get(null)); }
		RebirthEngineConfigs.REBIRTH_ENABLED = true;
		RebirthEngineConfigs.REBIRTH_MIN_LEVEL = 78;
		RebirthEngineConfigs.REBIRTH_MAX = 3;
		RebirthEngineConfigs.REBIRTH_RETURN_TO_LEVEL = 1;
		RebirthEngineConfigs.REBIRTH_SKILL_IDS = List.of(35200);
		RebirthEngineConfigs.REBIRTH_REWARDS = List.of(new RebirthEngineConfigs.Reward(2822, 1), new RebirthEngineConfigs.Reward(2822, 1), new RebirthEngineConfigs.Reward(2822, 1));
		_olympiad = mockStatic(com.l2journey.gameserver.model.olympiad.OlympiadManager.class);
		final var olympiad = mock(com.l2journey.gameserver.model.olympiad.OlympiadManager.class);
		_olympiad.when(com.l2journey.gameserver.model.olympiad.OlympiadManager::getInstance).thenReturn(olympiad);
		_repository = mock(RebirthRepository.class);
		_manager = new RebirthManager(_repository);
		when(_repository.read(42)).thenReturn(new Progress(0, "COMPLETE", 0, 0, 0, 0, List.of()));
		when(_repository.reserveReward(42, 1)).thenReturn(true);
		when(_repository.finish(42, 1)).thenReturn(true);
		_player = mock(Player.class);
		when(_player.getObjectId()).thenReturn(42);
		when(_player.isOnline()).thenReturn(true);
		when(_player.getLevel()).thenReturn(78);
		_stat = mock(PlayerStat.class);
		when(_player.getStat()).thenReturn(_stat);
		_inventory = mock(PlayerInventory.class);
		when(_player.getInventory()).thenReturn(_inventory);
		when(_inventory.validateCapacityByItemId(2822)).thenReturn(true);
		when(_inventory.validateWeight(anyLong())).thenReturn(true);
		when(_inventory.getInventoryItemCount(2822, -1)).thenReturn(0L, 1L);
		_normal = mock(Skill.class); when(_normal.getId()).thenReturn(1);
		_preserved = mock(Skill.class); when(_preserved.getId()).thenReturn(35200);
		when(_player.getAllSkills()).thenReturn(List.of(_normal, _preserved));
		when(_player.getAllShortcuts()).thenReturn(List.of());
		_reward = mock(Item.class);
		when(_player.addItem(ItemProcessType.REWARD, 2822, 1, _player, true)).thenReturn(_reward);
		_experience = mockStatic(ExperienceData.class);
		final ExperienceData experience = mock(ExperienceData.class);
		_experience.when(ExperienceData::getInstance).thenReturn(experience);
		when(experience.getMaxLevel()).thenReturn((byte) 86);
		when(experience.getExpForLevel(1)).thenReturn(0L);
		_items = mockStatic(ItemData.class);
		final ItemData data = mock(ItemData.class);
		_items.when(ItemData::getInstance).thenReturn(data);
		when(data.getTemplate(2822)).thenReturn(mock(ItemTemplate.class));
	}
	@AfterEach
	void cleanup() throws Exception
	{
		_items.close(); _experience.close(); _olympiad.close();
		for (var entry : _config.entrySet()) { entry.getKey().set(null, entry.getValue()); }
	}
	@Test
	void resetCommitPrecedesLiveChangesAndRewardReservationPrecedesDelivery() throws Exception
	{
		assertEquals(Result.SUCCESS, _manager.perform(_player));
		final var order = inOrder(_repository, _stat, _player, _reward);
		order.verify(_repository).begin(42, 0, 3, 1, 0L, 2822, 1L, List.of(35200));
		order.verify(_stat).setExp(0L);
		order.verify(_stat).setLevel((byte) 1);
		order.verify(_repository).reserveReward(42, 1);
		order.verify(_player).addItem(ItemProcessType.REWARD, 2822, 1L, _player, true);
		order.verify(_reward).updateDatabase(true);
		order.verify(_repository).verifyInventoryCount(42, 2822, 1);
		order.verify(_repository).finish(42, 1);
		verify(_player).regiveTemporarySkills();
		verify(_player).removeSkill(_normal, false, true);
		verify(_player, never()).removeSkill(_preserved, false, true);
	}
	@Test
	void failedDatabaseTransactionCannotResetOrReward() throws Exception
	{
		doThrow(new SQLException("fixture rollback")).when(_repository).begin(anyInt(), anyInt(), anyInt(), anyInt(), anyLong(), anyInt(), anyLong(), anyList());
		assertEquals(Result.FAILED, _manager.perform(_player));
		verifyNoInteractions(_stat);
		verify(_player, never()).removeSkill(any(Skill.class), anyBoolean(), anyBoolean());
		verify(_player, never()).addItem(any(), anyInt(), anyLong(), any(), anyBoolean());
	}
	@Test
	void failedCounterReadIsNotTreatedAsZero() throws Exception
	{
		when(_repository.read(42)).thenThrow(new SQLException("fixture unavailable"));
		assertEquals(Result.FAILED, _manager.perform(_player));
		verify(_repository, never()).begin(anyInt(), anyInt(), anyInt(), anyInt(), anyLong(), anyInt(), anyLong(), anyList());
		verifyNoInteractions(_stat);
	}
	@Test
	void maximumAndMinimumLevelPreventAllMutations() throws Exception
	{
		when(_repository.read(42)).thenReturn(new Progress(3, "COMPLETE", 0, 0, 0, 0, List.of()));
		assertEquals(Result.MAXIMUM, _manager.perform(_player));
		when(_repository.read(42)).thenReturn(new Progress(0, "COMPLETE", 0, 0, 0, 0, List.of()));
		when(_player.getLevel()).thenReturn(77);
		assertEquals(Result.LEVEL, _manager.perform(_player));
		verifyNoInteractions(_stat);
	}
	@Test
	void subclassCombatAndDisabledServiceCannotTouchDatabase()
	{
		when(_player.isSubClassActive()).thenReturn(true);
		assertEquals(Result.BLOCKED, _manager.perform(_player));
		when(_player.isSubClassActive()).thenReturn(false);
		when(_player.isInCombat()).thenReturn(true);
		assertEquals(Result.BLOCKED, _manager.perform(_player));
		RebirthEngineConfigs.REBIRTH_ENABLED = false;
		assertEquals(Result.BLOCKED, _manager.perform(_player));
		verifyNoInteractions(_repository);
	}
	@Test
	void pendingResetResumesWithoutIncrementingCounter() throws Exception
	{
		when(_repository.read(42)).thenReturn(new Progress(1, "RESET_PENDING", 1, 0, 2822, 1, List.of(35200)));
		assertEquals(Result.SUCCESS, _manager.perform(_player));
		verify(_repository, never()).begin(anyInt(), anyInt(), anyInt(), anyInt(), anyLong(), anyInt(), anyLong(), anyList());
		verify(_player).addItem(ItemProcessType.REWARD, 2822, 1, _player, true);
	}
	@Test
	void reservedRewardIsNeverReplayed() throws Exception
	{
		when(_repository.read(42)).thenReturn(new Progress(1, "REWARD_RESERVED", 1, 0, 2822, 1, List.of(35200)));
		assertEquals(Result.BLOCKED, _manager.perform(_player));
		_manager.recoverOnLogin(_player);
		verifyNoInteractions(_stat);
		verify(_player, never()).addItem(any(), anyInt(), anyLong(), any(), anyBoolean());
	}
	@Test
	void rewardReservationFailurePreventsDelivery() throws Exception
	{
		when(_repository.reserveReward(42, 1)).thenReturn(false);
		assertEquals(Result.FAILED, _manager.perform(_player));
		verify(_player, never()).addItem(any(), anyInt(), anyLong(), any(), anyBoolean());
		verify(_repository, never()).finish(anyInt(), anyInt());
	}
	@Test
	void loginCompletesCommittedResetWithoutAnotherRebirth() throws Exception
	{
		when(_repository.read(42)).thenReturn(new Progress(1, "RESET_PENDING", 1, 0, 2822, 1, List.of(35200)));
		_manager.recoverOnLogin(_player);
		verify(_player, never()).regiveTemporarySkills();
		verify(_repository, never()).begin(anyInt(), anyInt(), anyInt(), anyInt(), anyLong(), anyInt(), anyLong(), anyList());
		verify(_repository).finish(42, 1);
	}
	@Test
	void failedRewardCompletionRequiresReviewWithoutAutomaticRetry() throws Exception
	{
		when(_repository.finish(42, 1)).thenReturn(false);
		assertEquals(Result.FAILED, _manager.perform(_player));
		when(_repository.read(42)).thenReturn(new Progress(1, "REWARD_RESERVED", 1, 0, 2822, 1, List.of(35200)));
		assertEquals(Result.BLOCKED, _manager.perform(_player));
		verify(_player, times(1)).addItem(ItemProcessType.REWARD, 2822, 1, _player, true);
	}
	@Test
	void skillResetReturnsEveryRankAndPreservesTheRebirthCounterAndOrdinarySkills() throws Exception
	{
		RebirthEngineConfigs.REBIRTH_SKILL_RESET_PRICE = 10_000_000;
		when(_repository.read(42)).thenReturn(new Progress(3, "COMPLETE", 0, 0, 0, 0, List.of()));
		when(_player.getAdena()).thenReturn(12_000_000L);
		when(_player.reduceAdena(ItemProcessType.FEE, 10_000_000L, _player, true)).thenReturn(true);
		when(_preserved.getLevel()).thenReturn(3);
		when(_inventory.validateCapacity(3, false)).thenReturn(true);
		when(_inventory.getInventoryItemCount(2822, -1)).thenReturn(0L, 3L);
		when(_player.addItem(ItemProcessType.REFUND, 2822, 3L, _player, true)).thenReturn(_reward);
		when(_repository.transition(42, 3, "SKILL_REFUND_PENDING", "REFUND_RESERVED")).thenReturn(true);
		when(_repository.transition(42, 3, "REFUND_RESERVED", "COMPLETE")).thenReturn(true);
		try (var trees = mockStatic(SkillTreeData.class))
		{
			final SkillTreeData tree = mock(SkillTreeData.class);
			trees.when(SkillTreeData::getInstance).thenReturn(tree);
			final SkillLearn rank = mock(SkillLearn.class);
			when(rank.getRequiredItems()).thenReturn(Set.of(new ItemHolder(2822, 1)));
			when(tree.getRebirthSkill(eq(35200), intThat(level -> level >= 1 && level <= 3))).thenReturn(rank);
			assertEquals(Result.BLOCKED, _manager.resetSkills(_player, 1));
			verify(_player, never()).reduceAdena(any(), anyLong(), any(), anyBoolean());
			assertEquals(Result.SUCCESS, _manager.resetSkills(_player, 10_000_000));
		}
		final var order = inOrder(_repository, _player);
		order.verify(_repository).reserveSkillResetFee(42, 3, 10_000_000, Map.of(35200, 3), List.of(new RebirthEngineConfigs.Reward(2822, 3)));
		order.verify(_player).reduceAdena(ItemProcessType.FEE, 10_000_000L, _player, true);
		order.verify(_repository).verifyInventoryCount(42, 57, 2_000_000);
		order.verify(_repository).commitSkillReset(42, 3, Map.of(35200, 3));
		order.verify(_player).removeSkill(_preserved, false, true);
		order.verify(_repository).transition(42, 3, "SKILL_REFUND_PENDING", "REFUND_RESERVED");
		order.verify(_player).addItem(ItemProcessType.REFUND, 2822, 3L, _player, true);
		order.verify(_repository).verifyInventoryCount(42, 2822, 3);
		order.verify(_repository).transition(42, 3, "REFUND_RESERVED", "COMPLETE");
		verify(_player, never()).removeSkill(_normal, false, true);
		verifyNoInteractions(_stat);
	}

	@Test
	void pendingRefundResumesWithoutChargingAndReservedRefundCannotBeReplayed() throws Exception
	{
		when(_repository.read(42)).thenReturn(new Progress(2, "SKILL_REFUND_PENDING", 0, 0, 57, 10_000_000, List.of(35200)));
		when(_repository.readSkillRefunds(42, 2)).thenReturn(List.of(new RebirthEngineConfigs.Reward(2822, 1)));
		when(_inventory.validateCapacity(1, false)).thenReturn(true);
		when(_player.addItem(ItemProcessType.REFUND, 2822, 1L, _player, true)).thenReturn(_reward);
		when(_repository.transition(42, 2, "SKILL_REFUND_PENDING", "REFUND_RESERVED")).thenReturn(true);
		doThrow(new SQLException("inventory write failed")).when(_repository).verifyInventoryCount(42, 2822, 1);
		assertFalse(_manager.canLearnRebirthSkills(_player));
		assertEquals(Result.FAILED, _manager.resetSkills(_player, 0));
		when(_repository.read(42)).thenReturn(new Progress(2, "REFUND_RESERVED", 0, 0, 57, 10_000_000, List.of(35200)));
		assertEquals(Result.BLOCKED, _manager.resetSkills(_player, 0));
		verify(_player, never()).reduceAdena(any(), anyLong(), any(), anyBoolean());
		verify(_player).addItem(ItemProcessType.REFUND, 2822, 1L, _player, true);
		verify(_repository, never()).transition(42, 2, "REFUND_RESERVED", "COMPLETE");
	}

}
