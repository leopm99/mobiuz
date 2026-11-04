package com.l2journey.gameserver.model.actor.instance;

import java.sql.SQLException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.l2journey.RebirthEngineConfigs;
import com.l2journey.RebirthEngineConfigs.Reward;
import com.l2journey.gameserver.data.xml.ItemData;
import com.l2journey.gameserver.data.xml.SkillTreeData;
import com.l2journey.gameserver.model.World;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.templates.NpcTemplate;
import com.l2journey.gameserver.network.serverpackets.ActionFailed;
import com.l2journey.gameserver.network.serverpackets.ExShowScreenMessage;
import com.l2journey.gameserver.network.serverpackets.MagicSkillUse;
import com.l2journey.gameserver.network.serverpackets.NpcHtmlMessage;
import com.l2journey.gameserver.network.serverpackets.SocialAction;
import com.l2journey.gameserver.rebirth.RebirthManager;

/** Rebirth dialogue and interaction. @author vGodFather, adapted for L2Journey */
public final class RebirthInstance extends Folk
{
	private static final Logger LOGGER = Logger.getLogger(RebirthInstance.class.getName());
	private static final String HTML_LOC = "data/html/rebirth/";
	public RebirthInstance(NpcTemplate template) { super(template); }

	@Override
	public void onBypassFeedback(Player player, String command)
	{
		if (player == null || command == null || !canInteract(player)) { return; }
		switch (command)
		{
			case "rebirthMenu" -> { showChatWindow(player); return; }
			case "confirmRebirth" -> { showProgress(player, "rebirth-confirm.htm"); return; }
			case "rebirthTrainer" -> { openTrainer(player); return; }
			case "previewSkillReset" -> { previewSkillReset(player); return; }
			case "performRebirth" -> { }
			default ->
			{
				if (command.startsWith("resetRebirthSkills "))
				{
					try
					{
						final long fee = Long.parseLong(command.substring("resetRebirthSkills ".length()));
						synchronized (player)
						{
							if (!canInteract(player)) { return; }
							if (RebirthManager.getInstance().resetSkills(player, fee) == RebirthManager.Result.SUCCESS)
							{
								show(player, "rebirth-skills-reset.htm");
							}
						}
					}
					catch (NumberFormatException e)
					{
						player.sendMessage("Open the rebirth skill reset confirmation again.");
					}
					return;
				}
				super.onBypassFeedback(player, command);
				return;
			}
		}
		final RebirthManager.Result result;
		synchronized (player)
		{
			if (!canInteract(player)) { return; }
			result = RebirthManager.getInstance().perform(player);
		}
		switch (result)
		{
			case MAXIMUM -> show(player, "rebirth-max.htm");
			case LEVEL -> show(player, "rebirth-level.htm");
			case SUCCESS ->
			{
				show(player, "rebirth-successfully.htm");
				player.broadcastPacket(new SocialAction(player.getObjectId(), 3));
				player.broadcastPacket(new MagicSkillUse(player, player, 2024, 1, 1, 0));
				player.sendPacket(new ExShowScreenMessage("Congratulations " + player.getName() + "! You have been Reborn!", 15000));
			}
			default -> player.sendPacket(ActionFailed.STATIC_PACKET);
		}
	}
	@Override
	public void showChatWindow(Player player) { showChatWindow(player, 0); }
	@Override
	public void showChatWindow(Player player, int value)
	{
		if (player != null && canInteract(player)) { showProgress(player, "rebirth.htm"); }
	}
	private void showProgress(Player player, String file)
	{
		if (!RebirthEngineConfigs.REBIRTH_ENABLED)
		{
			show(player, file, Map.of("%count%", "-", "%reward%", "Unavailable", "%available%", "-"));
			return;
		}
		try
		{
			final var progress = RebirthManager.getInstance().getProgress(player);
			final var rewards = RebirthEngineConfigs.REBIRTH_REWARDS;
			final String reward = progress.pending() ? rewardText(new Reward(progress.itemId(), progress.itemCount())) :
				progress.complete() && progress.count() >= 0 && progress.count() < rewards.size() ? rewardText(rewards.get(progress.count())) : "Unavailable";
			final int available = progress.complete() && !player.isSubClassActive() ? SkillTreeData.getInstance().getAvailableRebirthSkills(player).size() : 0;
			show(player, file, Map.of("%count%", Integer.toString(progress.count()), "%reward%", reward, "%available%", Integer.toString(available)));
		}
		catch (SQLException | RuntimeException e)
		{
			LOGGER.log(Level.WARNING, "Could not display rebirth progress for " + player.getName(), e);
			player.sendMessage("Rebirth progress is currently unavailable.");
		}
	}

	private void previewSkillReset(Player player)
	{
		if (!RebirthEngineConfigs.REBIRTH_ENABLED || !RebirthManager.eligible(player))
		{
			player.sendMessage("Reset rebirth skills on your main class, outside combat and trades.");
			return;
		}
		final RebirthManager manager = RebirthManager.getInstance();
		try
		{
			final var progress = manager.getProgress(player);
			if ("SKILL_REFUND_PENDING".equals(progress.state()))
			{
				show(player, "rebirth-skill-refund.htm");
				return;
			}
			if (!progress.complete() || progress.count() < 1)
			{
				player.sendMessage("You must complete your previous rebirth operation before resetting skills.");
				return;
			}
			final var quote = manager.quoteSkillReset(player);
			if (quote.ranks().isEmpty())
			{
				player.sendMessage("You have no learned rebirth skills to reset.");
				return;
			}
			final long fee = RebirthEngineConfigs.REBIRTH_SKILL_RESET_PRICE;
			final String items = quote.refunds().stream().map(RebirthInstance::rewardText).collect(Collectors.joining("<br1>"));
			show(player, "rebirth-skill-confirm.htm", Map.of(
				"%fee%", String.format(java.util.Locale.US, "%,d", fee),
				"%feeRaw%", Long.toString(fee),
				"%skillCount%", Integer.toString(quote.ranks().size()),
				"%refunds%", items.isEmpty() ? "None" : items));
		}
		catch (SQLException | RuntimeException e)
		{
			LOGGER.log(Level.WARNING, "Could not display rebirth skill reset for " + player.getName(), e);
			player.sendMessage("The skill reset is currently unavailable.");
		}
	}

	private void openTrainer(Player player)
	{
		for (Npc npc : World.getInstance().getVisibleObjectsInRange(player, Npc.class, Npc.INTERACTION_DISTANCE))
		{
			if (npc.getId() == RebirthEngineConfigs.REBIRTH_SKILL_SELLER_ID && npc.canInteract(player))
			{
				Folk.showRebirthSkillList(player, npc);
				return;
			}
		}
		player.sendMessage("Speak to the Rebirth Skill Trainer (NPC " + RebirthEngineConfigs.REBIRTH_SKILL_SELLER_ID + ") to learn skills.");
	}

	private static String rewardText(Reward reward)
	{
		final var template = ItemData.getInstance().getTemplate(reward.id());
		final String name = template == null ? "Item " + reward.id() : template.getName();
		return reward.count() + " x " + name.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	private void show(Player player, String file)
	{
		show(player, file, Map.of());
	}

	private void show(Player player, String file, Map<String, String> replacements)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(player, HTML_LOC + file);
		html.replace("%objectId%", getObjectId());
		html.replace("%level%", RebirthEngineConfigs.REBIRTH_RETURN_TO_LEVEL);
		html.replace("%minLevel%", RebirthEngineConfigs.REBIRTH_MIN_LEVEL);
		html.replace("%max%", RebirthEngineConfigs.REBIRTH_MAX);
		replacements.forEach(html::replace);
		player.sendPacket(html);
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
}
