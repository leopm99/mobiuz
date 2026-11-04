package com.l2journey.gameserver.rebirth;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.l2journey.RebirthEngineConfigs;
import com.l2journey.RebirthEngineConfigs.Reward;
import com.l2journey.gameserver.data.xml.ExperienceData;
import com.l2journey.gameserver.data.xml.ItemData;
import com.l2journey.gameserver.data.xml.SkillTreeData;
import com.l2journey.gameserver.model.SkillLearn;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.item.instance.Item;
import com.l2journey.gameserver.model.itemcontainer.Inventory;
import com.l2journey.gameserver.model.actor.enums.player.ShortcutType;
import com.l2journey.gameserver.model.item.ItemTemplate;
import com.l2journey.gameserver.model.olympiad.OlympiadManager;
import com.l2journey.gameserver.model.zone.ZoneId;
import com.l2journey.gameserver.model.item.enums.ItemProcessType;
import com.l2journey.gameserver.rebirth.RebirthRepository.Progress;

import static com.l2journey.gameserver.rebirth.RebirthRepository.REFUND_RESERVED;
import static com.l2journey.gameserver.rebirth.RebirthRepository.SKILL_FEE_RESERVED;
import static com.l2journey.gameserver.rebirth.RebirthRepository.SKILL_REFUND_PENDING;

/** Performs a main-class reset only after its durable database transaction succeeds. */
public class RebirthManager
{
	public enum Result { SUCCESS, MAXIMUM, LEVEL, BLOCKED, FAILED }
	private static final Logger LOGGER = Logger.getLogger(RebirthManager.class.getName());
	private final RebirthRepository _repository;
	private volatile long _nextLoginWarning;
	public RebirthManager(RebirthRepository repository) { _repository = repository; }
	public static RebirthManager getInstance() { return Holder.INSTANCE; }
	private static class Holder { private static final RebirthManager INSTANCE = new RebirthManager(new RebirthRepository()); }

	public Result perform(Player player)
	{
		synchronized (player)
		{
			if (!RebirthEngineConfigs.REBIRTH_ENABLED || !eligible(player))
			{
				player.sendMessage("Rebirth requires your main class, outside combat, events, transformations and trades.");
				return Result.BLOCKED;
			}
			try
			{
				final Progress progress = _repository.read(player.getObjectId());
				if (!progress.complete()) { return resume(player, progress, true); }
				if (progress.count() >= RebirthEngineConfigs.REBIRTH_MAX) { return Result.MAXIMUM; }
				if (player.getLevel() < RebirthEngineConfigs.REBIRTH_MIN_LEVEL) { return Result.LEVEL; }
				final int level = RebirthEngineConfigs.REBIRTH_RETURN_TO_LEVEL;
				if (level < 1 || level >= player.getLevel() || level >= ExperienceData.getInstance().getMaxLevel() ||
					RebirthEngineConfigs.REBIRTH_REWARDS.size() != RebirthEngineConfigs.REBIRTH_MAX)
				{ throw new IllegalStateException("Invalid rebirth levels or rewards"); }
				final var reward = RebirthEngineConfigs.REBIRTH_REWARDS.get(progress.count());
				final ItemTemplate template = ItemData.getInstance().getTemplate(reward.id());
				if (template == null || !player.getInventory().validateCapacityByItemId(reward.id()) ||
					!player.getInventory().validateWeight(Math.multiplyExact((long) template.getWeight(), reward.count())))
				{ player.sendMessage("Invalid reward or insufficient inventory capacity/weight."); return Result.BLOCKED; }
				final long exp = ExperienceData.getInstance().getExpForLevel(level);
				_repository.begin(player.getObjectId(), progress.count(), RebirthEngineConfigs.REBIRTH_MAX, level, exp, reward.id(), reward.count(), RebirthEngineConfigs.REBIRTH_SKILL_IDS);
				return resume(player, new Progress(progress.count() + 1, "RESET_PENDING", level, exp, reward.id(), reward.count(), List.copyOf(RebirthEngineConfigs.REBIRTH_SKILL_IDS)), true);
			}
			catch (SQLException | RuntimeException e)
			{
				LOGGER.log(Level.SEVERE, "Rebirth failed for character " + player.getObjectId() + ". Check the rebirth journal before retrying.", e);
				player.sendMessage("Rebirth could not be completed. Contact a GM before retrying.");
				return Result.FAILED;
			}
		}
	}
	public static boolean eligible(Player player)
	{
		return player.isOnline() && !player.isSubClassActive() && !player.isChangingClass() && !player.isAlikeDead() &&
			!player.isInCombat() && !player.isInDuel() && !player.isCastingNow() && !player.isCastingSimultaneouslyNow() &&
			!player.isTransformed() && !player.isMounted() && !player.isTeleporting() && !player.isInStoreMode() &&
			!player.isProcessingTransaction() && !player.isInOlympiadMode() && !player.isOnEvent() &&
			!player.isRegisteredOnEvent() && !player.isOnSoloEvent() && !player.isJailed() && !player.isCursedWeaponEquipped() && !player.inObserverMode() &&
			player.getPvpFlag() == 0 && !player.isInsideZone(ZoneId.SIEGE) && !OlympiadManager.getInstance().isRegisteredInComp(player);
	}
	private Result resume(Player player, Progress progress, boolean liveReset) throws SQLException
	{
		if (!progress.pending())
		{
			player.sendMessage(SKILL_REFUND_PENDING.equals(progress.state()) ? "Finish your pending skill refund at the Rebirth Manager." : "A previous rebirth operation needs GM review.");
			return Result.BLOCKED;
		}
		if (progress.count() <= 0 || progress.level() < 1 || progress.level() >= ExperienceData.getInstance().getMaxLevel() || progress.exp() < 0 || progress.itemCount() <= 0 || ItemData.getInstance().getTemplate(progress.itemId()) == null)
		{ throw new IllegalStateException("Invalid pending rebirth journal"); }
		if (!player.getInventory().validateCapacityByItemId(progress.itemId()) ||
			!player.getInventory().validateWeight(Math.multiplyExact((long) ItemData.getInstance().getTemplate(progress.itemId()).getWeight(), progress.itemCount())))
		{ player.sendMessage("Free inventory space and weight, then visit the Rebirth Manager to finish your pending rebirth."); return Result.BLOCKED; }
		// A committed reset can be applied again after a crash without incrementing the counter.
		player.getStat().setExp(progress.exp());
		player.getStat().setLevel((byte) progress.level());
		player.setExpBeforeDeath(0);
		for (var skill : List.copyOf(player.getAllSkills()))
		{
			if (!progress.preserved().contains(skill.getId())) { player.removeSkill(skill, false, true); }
		}
		for (var shortcut : List.copyOf(player.getAllShortcuts()))
		{
			if (shortcut.getType() == ShortcutType.SKILL && !progress.preserved().contains(shortcut.getId()))
			{ player.deleteShortcut(shortcut.getSlot(), shortcut.getPage()); }
		}
		player.giveAvailableAutoGetSkills();
		if (liveReset)
		{
			player.regiveTemporarySkills();
		}
		player.setCurrentHp(player.getMaxHp());
		player.setCurrentMp(player.getMaxMp());
		player.setCurrentCp(player.getMaxCp());
		player.storeMe();
		player.sendSkillList();
		player.broadcastStatusUpdate();
		player.broadcastUserInfo();
		// Reserve before item delivery. A crash in this narrow window requires review, never automatic replay.
		if (!_repository.reserveReward(player.getObjectId(), progress.count())) { throw new SQLException("Reward reservation failed"); }
		deliver(player, new Reward(progress.itemId(), progress.itemCount()), ItemProcessType.REWARD);
		if (!_repository.finish(player.getObjectId(), progress.count())) { throw new SQLException("Reward completion failed; do not replay"); }
		return Result.SUCCESS;
	}
	public record SkillResetQuote(Map<Integer, Integer> ranks, List<Reward> refunds)
	{
	}

	public Progress getProgress(Player player) throws SQLException
	{
		return _repository.read(player.getObjectId());
	}

	public boolean canLearnRebirthSkills(Player player)
	{
		try
		{
			return getProgress(player).complete();
		}
		catch (SQLException | RuntimeException e)
		{
			LOGGER.log(Level.WARNING, "Could not check rebirth skill acquisition for " + player.getName(), e);
			return false;
		}
	}

	public SkillResetQuote quoteSkillReset(Player player)
	{
		final Map<Integer, Integer> ranks = new TreeMap<>();
		final Map<Integer, Long> refunds = new TreeMap<>();
		final var tree = SkillTreeData.getInstance();
		for (var skill : player.getAllSkills())
		{
			final int skillId = skill.getId();
			final int learnedRank = skill.getLevel();
			if (learnedRank <= 0 || tree.getRebirthSkill(skillId, 1) == null)
			{
				continue;
			}
			for (int rank = 1; rank <= learnedRank; rank++)
			{
				final SkillLearn learn = tree.getRebirthSkill(skillId, rank);
				if (learn == null)
				{
					throw new IllegalStateException("Missing rebirth catalog entry for skill " + skillId + " rank " + rank);
				}
				for (var item : learn.getRequiredItems())
				{
					if (item.getId() <= 0 || item.getCount() <= 0)
					{
						throw new IllegalStateException("Invalid rebirth skill cost");
					}
					refunds.merge(item.getId(), item.getCount(), Math::addExact);
				}
			}
			ranks.put(skillId, learnedRank);
		}
		return new SkillResetQuote(Map.copyOf(ranks), refunds.entrySet().stream().map(e -> new Reward(e.getKey(), e.getValue())).toList());
	}

	public Result resetSkills(Player player, long confirmedFee)
	{
		synchronized (player)
		{
			if (!RebirthEngineConfigs.REBIRTH_ENABLED || !eligible(player))
			{
				player.sendMessage("Reset rebirth skills on your main class, outside combat and trades.");
				return Result.BLOCKED;
			}
			try
			{
				final Progress progress = getProgress(player);
				if (SKILL_REFUND_PENDING.equals(progress.state()))
				{
					return finishSkillRefund(player, progress, _repository.readSkillRefunds(player.getObjectId(), progress.count()));
				}
				if (!progress.complete())
				{
					player.sendMessage("Resolve your previous rebirth operation before resetting skills.");
					return Result.BLOCKED;
				}
				final long fee = RebirthEngineConfigs.REBIRTH_SKILL_RESET_PRICE;
				if (progress.count() < 1 || fee <= 0 || fee != confirmedFee)
				{
					player.sendMessage("This skill reset is unavailable. Open the confirmation again.");
					return Result.BLOCKED;
				}
				final SkillResetQuote quote = quoteSkillReset(player);
				if (quote.ranks().isEmpty())
				{
					player.sendMessage("You have no learned rebirth skills to reset.");
					return Result.BLOCKED;
				}
				if (!canReceive(player, quote.refunds()))
				{
					player.sendMessage("Free inventory space and weight for the returned items.");
					return Result.BLOCKED;
				}
				final long balance = player.getAdena();
				if (balance < fee)
				{
					player.sendMessage("You do not have enough Adena for the rebirth skill reset.");
					return Result.BLOCKED;
				}
				final int playerId = player.getObjectId();
				_repository.reserveSkillResetFee(playerId, progress.count(), fee, quote.ranks(), quote.refunds());
				final Item adena = player.getInventory().getAdenaInstance();
				if (!player.reduceAdena(ItemProcessType.FEE, fee, player, true))
				{
					if (!_repository.transition(playerId, progress.count(), SKILL_FEE_RESERVED, "COMPLETE"))
					{
						throw new SQLException("Could not release unpaid skill reset");
					}
					return Result.BLOCKED;
				}
				if (adena != null)
				{
					adena.updateDatabase(true);
				}
				_repository.verifyInventoryCount(playerId, Inventory.ADENA_ID, balance - fee);
				_repository.commitSkillReset(playerId, progress.count(), quote.ranks());
				return finishSkillRefund(player, new Progress(progress.count(), SKILL_REFUND_PENDING, 0, 0, 0, 0, List.copyOf(quote.ranks().keySet())), quote.refunds());
			}
			catch (SQLException | RuntimeException e)
			{
				LOGGER.log(Level.SEVERE, "Rebirth skill reset failed for " + player.getName() + ". Check the rebirth journal.", e);
				player.sendMessage("The rebirth skill reset could not be completed. Contact a GM before retrying.");
				return Result.FAILED;
			}
		}
	}

	private Result finishSkillRefund(Player player, Progress progress, List<Reward> refunds) throws SQLException
	{
		if (!SKILL_REFUND_PENDING.equals(progress.state()) || progress.preserved().isEmpty())
		{
			throw new SQLException("Invalid pending rebirth skill refund");
		}
		final int playerId = player.getObjectId();
		for (var skill : List.copyOf(player.getAllSkills()))
		{
			if (progress.preserved().contains(skill.getId()))
			{
				player.removeSkill(skill, false, true);
			}
		}
		for (var shortcut : List.copyOf(player.getAllShortcuts()))
		{
			if (shortcut.getType() == ShortcutType.SKILL && progress.preserved().contains(shortcut.getId()))
			{
				player.deleteShortcut(shortcut.getSlot(), shortcut.getPage());
			}
		}
		player.sendSkillList();
		player.broadcastUserInfo();
		if (!canReceive(player, refunds))
		{
			player.sendMessage("Free inventory space and weight, then return to the Rebirth Manager for your refund.");
			return Result.BLOCKED;
		}
		if (!_repository.transition(playerId, progress.count(), SKILL_REFUND_PENDING, REFUND_RESERVED))
		{
			throw new SQLException("Skill refund reservation failed");
		}
		for (Reward refund : refunds)
		{
			deliver(player, refund, ItemProcessType.REFUND);
		}
		if (!_repository.transition(playerId, progress.count(), REFUND_RESERVED, "COMPLETE"))
		{
			throw new SQLException("Skill refund completion failed; do not replay");
		}
		player.sendMessage("Your rebirth skills have been reset. You can choose new skills at the trainer.");
		return Result.SUCCESS;
	}

	private static boolean canReceive(Player player, List<Reward> items)
	{
		long weight = 0;
		long slots = 0;
		long questSlots = 0;
		for (Reward item : items)
		{
			final ItemTemplate template = ItemData.getInstance().getTemplate(item.id());
			if (template == null || item.count() <= 0 || template.hasExImmediateEffect())
			{
				return false;
			}
			final long needed = template.isStackable() ? (player.getInventory().getItemByItemId(item.id()) == null ? 1 : 0) : item.count();
			if (template.isQuestItem()) { questSlots = Math.addExact(questSlots, needed); }
			else { slots = Math.addExact(slots, needed); }
			weight = Math.addExact(weight, Math.multiplyExact((long) template.getWeight(), item.count()));
		}
		return (slots == 0 || player.getInventory().validateCapacity(slots, false)) &&
			(questSlots == 0 || player.getInventory().validateCapacity(questSlots, true)) && player.getInventory().validateWeight(weight);
	}

	private void deliver(Player player, Reward reward, ItemProcessType process) throws SQLException
	{
		final var inventory = player.getInventory();
		final long expected = Math.addExact(inventory.getInventoryItemCount(reward.id(), -1), reward.count());
		final Item delivered = player.addItem(process, reward.id(), reward.count(), player, true);
		if (delivered == null || inventory.getInventoryItemCount(reward.id(), -1) != expected)
		{
			throw new SQLException("Rebirth item delivery could not be confirmed");
		}
		delivered.updateDatabase(true);
		if (!delivered.isStackable())
		{
			for (Item item : inventory.getAllItemsByItemId(reward.id()))
			{
				if (item != delivered) { item.updateDatabase(true); }
			}
		}
		_repository.verifyInventoryCount(player.getObjectId(), reward.id(), expected);
	}

	public void recoverOnLogin(Player player)
	{
		if (!RebirthEngineConfigs.REBIRTH_ENABLED) { return; }
		synchronized (player)
		{
			try
			{
				final Progress progress = _repository.read(player.getObjectId());
				if (progress.complete()) { return; }
				if (player.isSubClassActive()) { player.sendMessage("A pending rebirth requires your main class. Visit the Rebirth Manager."); return; }
				if (SKILL_REFUND_PENDING.equals(progress.state()))
				{
					finishSkillRefund(player, progress, _repository.readSkillRefunds(player.getObjectId(), progress.count()));
				}
				else if (resume(player, progress, false) == Result.SUCCESS)
				{
					player.sendMessage("Your pending rebirth has been completed.");
				}
			}
			catch (SQLException | RuntimeException e)
			{
				if (_nextLoginWarning < System.currentTimeMillis())
				{
					_nextLoginWarning = System.currentTimeMillis() + 60000;
					LOGGER.log(Level.SEVERE, "Could not check pending rebirths. Verify the rebirth SQL update.", e);
				}
			}
		}
	}
}
