package handlers.voicedcommandhandlers;

import com.l2journey.Config;
import com.l2journey.gameserver.handler.IVoicedCommandHandler;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.item.enums.ItemProcessType;
import com.l2journey.gameserver.model.stats.Formulas;

public class ResVCmd implements IVoicedCommandHandler
{
	private static final String[] COMMANDS = { "res" };

	@Override
	public boolean useVoicedCommand(String command, Player player, String target)
	{
		if ((player == null) || !"res".equalsIgnoreCase(command) || !Config.COMMAND_RES) { return false; }
		synchronized (player)
		{
			if (!player.isDead() || player.isFakeDeath())
			{
				player.sendMessage("You can only use this command while dead.");
				return false;
			}
			if (player.isInOlympiadMode() || player.isResurrectionBlocked() || player.isTeleporting())
			{
				player.sendMessage("You cannot be revived right now.");
				return false;
			}
			final int price = Config.PRICE_RESS;
			if ((price < 0) || ((price > 0) && ((Config.ITEM_ID_RESS <= 0) || !player.destroyItemByItemId(ItemProcessType.FEE, Config.ITEM_ID_RESS, price, player, true))))
			{
				player.sendMessage("You do not have enough currency for resurrection.");
				return false;
			}
			player.doRevive(Formulas.calculateSkillResurrectRestorePercent(100, player));
			player.setCurrentCp(player.getMaxCp());
			player.setCurrentHp(player.getMaxHp());
			player.setCurrentMp(player.getMaxMp());
			player.sendMessage("You have been resurrected.");
			return true;
		}
	}

	@Override
	public String[] getVoicedCommandList() { return COMMANDS; }
}
