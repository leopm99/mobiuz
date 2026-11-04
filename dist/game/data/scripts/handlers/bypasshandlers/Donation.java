package handlers.bypasshandlers;
import com.l2journey.gameserver.handler.IBypassHandler;
import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.donation.DonationManager;
public final class Donation implements IBypassHandler
{
 @Override public String[] getBypassList() { return new String[] { "donation" }; }
 @Override public boolean useBypass(String command, Player player, Creature target)
 {
  if (player == null || !command.startsWith("donation ") || player.validateHtmlAction(command) == -1) return false;
  DonationManager.getInstance().handleBypass(player,command.substring(9)); return true;
 }
}
