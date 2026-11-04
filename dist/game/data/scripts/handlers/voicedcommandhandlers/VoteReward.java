package handlers.voicedcommandhandlers;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.l2journey.Config;
import com.l2journey.commons.threads.ThreadPool;
import com.l2journey.gameserver.handler.IVoicedCommandHandler;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.votesystem.Enum.VoteSite;
import com.l2journey.gameserver.votesystem.handler.VoteManager;
import com.l2journey.gameserver.votesystem.handler.VoteHandler;
import com.l2journey.gameserver.votesystem.util.VoteSiteData;

/** Checks individual vote rewards without blocking the client's packet thread. */
public class VoteReward implements IVoicedCommandHandler
{
	private static final Logger LOGGER = Logger.getLogger(VoteReward.class.getName());
	private final Set<Integer> _pendingPlayers = ConcurrentHashMap.newKeySet();

	@Override
	public String[] getVoicedCommandList()
	{
		return new String[] { Config.VOTING_COMMAND };
	}

	@Override
	public boolean useVoicedCommand(String command, Player player, String params)
	{
		if ((player == null) || !Config.VOTING_COMMAND.equalsIgnoreCase(command))
		{
			return false;
		}
		if (!Config.ENABLE_VOTE_SYSTEM || !Config.ENABLE_INDIVIDUAL_VOTE || !Config.ENABLE_VOTING_COMMAND)
		{
			player.sendMessage("Voting command reward is disabled.");
			return false;
		}
		if (player.isJailed())
		{
			player.sendMessage("You cannot use this function while you are jailed.");
			return false;
		}
		if (!_pendingPlayers.add(player.getObjectId()))
		{
			player.sendMessage("Your votes are already being checked. Please wait.");
			return true;
		}
		try
		{
			ThreadPool.execute(() ->
			{
				try
				{
					if ("link".equalsIgnoreCase(params == null ? "" : params.trim()))
					{
						final String ip = VoteManager.getInstance().existIp(player);
						if (ip != null)
						{
							final String link = VoteManager.getInstance().createVoteLink(player);
							player.sendMessage(link.isEmpty() ? "Could not generate a Hopzone.eu vote link. Please wait and check the site configuration." : "Vote at: " + link);
						}
						return;
					}
					for (VoteSite site : VoteSite.values())
					{
						if (!player.isOnline() || player.isJailed() || !Config.ENABLE_VOTE_SYSTEM || !Config.ENABLE_INDIVIDUAL_VOTE || !Config.ENABLE_VOTING_COMMAND)
						{
							break;
						}
						if (VoteSiteData.getInstance().isIndividualEnabled(site.getId()) && VoteHandler.isConfigured(site.getId()) && !VoteSiteData.getInstance().getRewards(site.getId()).isEmpty())
						{
							VoteManager.getInstance().getReward(player, site.getId());
						}
					}
				}
				catch (RuntimeException e)
				{
					LOGGER.log(Level.WARNING, "Could not process voiced vote rewards.", e);
					player.sendMessage("Could not finish checking your votes. Please try again later.");
				}
				finally
				{
					_pendingPlayers.remove(player.getObjectId());
				}
			});
		}
		catch (RuntimeException e)
		{
			_pendingPlayers.remove(player.getObjectId());
			LOGGER.log(Level.WARNING, "Could not schedule voiced vote rewards.", e);
			player.sendMessage("Could not start checking your votes. Please try again later.");
			return false;
		}
		return true;
	}
}
