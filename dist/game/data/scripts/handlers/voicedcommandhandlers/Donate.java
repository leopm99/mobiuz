package handlers.voicedcommandhandlers;
import com.l2journey.gameserver.handler.IVoicedCommandHandler;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.donation.DonationConfig;
import com.l2journey.gameserver.donation.DonationManager;
public final class Donate implements IVoicedCommandHandler
{
 @Override public String[] getVoicedCommandList() { return new String[] { "donate" }; }
 @Override public boolean useVoicedCommand(String command, Player player, String params)
 {
  if (player == null || !"donate".equalsIgnoreCase(command)) return false;
  if (!DonationConfig.DONATION_ENABLED) { player.sendMessage("Donations are currently disabled."); return true; }
  if (player.isJailed()) return false;
  DonationManager.getInstance().showIndexWindow(player); return true;
 }
}
