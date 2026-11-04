package com.l2journey.gameserver.votesystem.handler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;

import com.l2journey.Config;
import com.l2journey.commons.threads.ThreadPool;
import com.l2journey.gameserver.data.xml.ItemData;
import com.l2journey.gameserver.model.World;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.item.enums.ItemProcessType;
import com.l2journey.gameserver.network.GameClient;
import com.l2journey.gameserver.util.Broadcast;
import com.l2journey.gameserver.votesystem.Enum.VoteSite;
import com.l2journey.gameserver.votesystem.db.GlobalVoteDB;
import com.l2journey.gameserver.votesystem.db.IndividualVoteDB;
import com.l2journey.gameserver.votesystem.model.GlobalVote;
import com.l2journey.gameserver.votesystem.model.IndividualVote;
import com.l2journey.gameserver.votesystem.model.IndividualVoteResponse;
import com.l2journey.gameserver.votesystem.model.Reward;
import com.l2journey.gameserver.votesystem.util.VoteQueryLimiter;
import com.l2journey.gameserver.votesystem.util.VoteSiteData;

/** Coordinates vote verification, durable reward claims and managed background tasks. */
public final class VoteManager extends VoteHandler
{
	private ScheduledFuture<?> _saveGlobalVotes;
	private ScheduledFuture<?> _updateIndividualVotes;
	private ScheduledFuture<?> _autoGlobalVotesReward;
	private final Map<String, IndividualVote[]> _foundVoters = new ConcurrentHashMap<>();
	private final GlobalVote[] _globalVotes = new GlobalVote[VoteSite.slotCount()];
	private final Object[] _ipLocks = new Object[64];
	private final Object _globalLock = new Object();
	private volatile boolean _stopped;
	private volatile boolean _globalReady;
	private final VoteQueryLimiter _queries = new VoteQueryLimiter();
	private final Object[] _pollLocks = new Object[VoteSite.slotCount()];
	private final long[] _nextGlobalPoll = new long[VoteSite.slotCount()];
	private final boolean[] _validGlobalCounts = new boolean[VoteSite.slotCount()];
	
	private record ItemReward(int id, int count)
	{
	}
	
	private VoteManager()
	{
		for (int i = 0; i < _ipLocks.length; i++)
		{
			_ipLocks[i] = new Object();
		}
		for (VoteSite site : VoteSite.values())
		{
			_globalVotes[site.getId()] = new GlobalVote(site.getId(), 0);
			_pollLocks[site.getId()] = new Object();
		}
		if (!Config.ENABLE_VOTE_SYSTEM)
		{
			return;
		}
		if (Config.ENABLE_INDIVIDUAL_VOTE)
		{
			loadVotes();
			_updateIndividualVotes = periodic(this::AutoUpdateIndividualVotes, 30000, Config.NEXT_TIME_TO_AUTO_UPDATE_INDIVIDUAL_VOTES);
		}
		if (Config.ENABLE_GLOBAL_VOTE)
		{
			loadGlobalVotes();
			checkAllResponseGlobalVotes();
			_autoGlobalVotesReward = periodic(this::AutoGlobalVoteReward, 10000, globalTaskInterval());
			_saveGlobalVotes = periodic(this::AutoSaveGlobalVotes, 30000, Config.NEXT_TIME_TO_AUTO_UPDATE_TOTAL_VOTE);
		}
	}
	
	private static long globalTaskInterval()
	{
		long interval = Config.NEXT_TIME_TO_CHECK_AUTO_GLOBAL_VOTES_REWARD;
		for (VoteSite site : VoteSite.values())
		{
			final long configured = VoteSiteData.getInstance().getSettings(site.getId()).checkInterval();
			if (VoteSiteData.getInstance().isGlobalEnabled(site.getId()) && (configured > 0))
			{
				interval = interval > 0 ? Math.min(interval, configured) : configured;
			}
		}
		return interval;
	}
	
	private ScheduledFuture<?> periodic(Runnable task, long initialDelay, long interval)
	{
		if (interval <= 0)
		{
			LOGGER.warning("Vote task disabled: interval must be positive.");
			return null;
		}
		return ThreadPool.scheduleAtFixedRate(() ->
		{
			if (_stopped)
			{
				return;
			}
			try
			{
				task.run();
			}
			catch (RuntimeException e)
			{
				LOGGER.log(Level.WARNING, "Vote task failed", e);
			}
		}, initialDelay, interval);
	}
	
	private Object ipLock(String ip)
	{
		return _ipLocks[(ip.hashCode() & Integer.MAX_VALUE) % _ipLocks.length];
	}
	
	private static boolean validSite(int site)
	{
		return VoteSite.fromId(site) != null;
	}
	
	public void getReward(Player player, int site)
	{
		if (_stopped || !Config.ENABLE_VOTE_SYSTEM || !Config.ENABLE_INDIVIDUAL_VOTE || !validSite(site) || !VoteSiteData.getInstance().isIndividualEnabled(site))
		{
			return;
		}
		final String ip = existIp(player);
		if (ip == null)
		{
			return;
		}
		final List<ItemReward> rewards = rewards(site);
		if (rewards.isEmpty())
		{
			player.sendMessage("No valid vote rewards configured for this site.");
			return;
		}
		synchronized (ipLock(ip))
		{
			if (!available(ip, site))
			{
				player.sendMessage("You can vote again in " + getTimeRemainingWithSampleFormat(player, site));
				return;
			}
		}
		if (!_queries.acquire(ip, site, System.currentTimeMillis(), Config.VOTE_QUERY_COOLDOWN))
		{
			player.sendMessage("Please wait before checking this vote site again.");
			return;
		}
		final IndividualVoteResponse response;
		try
		{
			response = getIndividualVoteResponse(site, ip, player.getAccountName());
		}
		catch (RuntimeException e)
		{
			LOGGER.warning("Could not verify vote for site " + site + " (" + e.getClass().getSimpleName() + ").");
			player.sendMessage("Could not verify your vote. Please try again later.");
			return;
		}
		finally
		{
			_queries.release(ip, site);
		}
		if (response == null)
		{
			player.sendMessage("Could not verify your vote. Please try again later.");
			return;
		}
		if (!response.getIsVoted())
		{
			player.sendMessage("No confirmed vote found for " + VoteSiteData.getInstance().getSiteName(site) + ".");
			return;
		}
		final IndividualVote vote = new IndividualVote(ip, response.getDiffTime(), response.getVoteSiteTime(), site, true);
		if ((vote.getVotingTimeSite() <= 0) || (getTimeRemaining(vote) <= 0) || (Config.INTERVAL_TO_NEXT_VOTE <= 0))
		{
			player.sendMessage("The confirmed vote has expired or the vote interval is invalid.");
			return;
		}
		synchronized (ipLock(ip))
		{
			if (_stopped || !Config.ENABLE_VOTE_SYSTEM || !Config.ENABLE_INDIVIDUAL_VOTE || !ip.equals(existIp(player)))
			{
				return;
			}
			if (!available(ip, site))
			{
				player.sendMessage("You can vote again in " + getTimeRemainingWithSampleFormat(player, site));
				return;
			}
			if (!IndividualVoteDB.getInstance().claimVote(vote))
			{
				player.sendMessage("Reward already claimed or its reservation could not be saved. Please try later.");
				return;
			}
			_foundVoters.computeIfAbsent(ip, key -> new IndividualVote[VoteSite.slotCount()])[site] = vote;
			if (deliver(player, rewards, "individual site " + site))
			{
				player.sendMessage(VoteSiteData.getInstance().getSiteName(site) + ": Thank you for voting! Your reward was delivered.");
			}
		}
	}
	
	/**
	 * Generate an account-bound Hopzone.eu link using the same IP/site request limiter.
	 * @param player
	 * @return
	 */
	public String createVoteLink(Player player)
	{
		if (_stopped || !Config.ENABLE_VOTE_SYSTEM || !Config.ENABLE_INDIVIDUAL_VOTE || !VoteSiteData.getInstance().isIndividualEnabled(12))
		{
			return "";
		}
		final String ip = existIp(player);
		if ((ip == null) || !_queries.acquire(ip, 12, System.currentTimeMillis(), Config.VOTE_QUERY_COOLDOWN))
		{
			return "";
		}
		try
		{
			return VoteHandler.createHopzoneLink(ip, player.getAccountName());
		}
		finally
		{
			_queries.release(ip, 12);
		}
	}
	
	private boolean available(String ip, int site)
	{
		final IndividualVote[] votes = _foundVoters.get(ip);
		final IndividualVote vote = votes == null ? null : votes[site];
		return (vote == null) || !vote.getAlreadyRewarded() || (getTimeRemaining(vote) <= 0);
	}
	
	public boolean checkIndividualAvailableVote(Player player, int site)
	{
		if (_stopped || !Config.ENABLE_VOTE_SYSTEM || !Config.ENABLE_INDIVIDUAL_VOTE || !validSite(site) || !VoteSiteData.getInstance().isIndividualEnabled(site))
		{
			return false;
		}
		final String ip = existIp(player);
		if (ip == null)
		{
			return false;
		}
		synchronized (ipLock(ip))
		{
			return available(ip, site);
		}
	}
	
	public long getTimeRemaining(IndividualVote vote)
	{
		// diffTime = local time minus provider time; add it to the provider's vote timestamp.
		return (vote.getVotingTimeSite() + vote.getDiffTime() + Config.INTERVAL_TO_NEXT_VOTE) - System.currentTimeMillis();
	}
	
	public String getTimeRemainingWithSampleFormat(Player player, int site)
	{
		if (!validSite(site))
		{
			return "";
		}
		final String ip = existIp(player);
		if (ip == null)
		{
			return "";
		}
		synchronized (ipLock(ip))
		{
			final IndividualVote[] votes = _foundVoters.get(ip);
			return (votes == null) || (votes[site] == null) ? "" : CalculateTimeRemainingWithSampleDateFormat(getTimeRemaining(votes[site]));
		}
	}
	
	public String CalculateTimeRemainingWithSampleDateFormat(long remaining)
	{
		final long seconds = Math.max(0, remaining) / 1000;
		return String.format(java.util.Locale.ROOT, "%dH:%02dm:%02ds", seconds / 3600, (seconds % 3600) / 60, seconds % 60);
	}
	
	public String existIp(Player player)
	{
		if ((player == null) || !player.isOnline())
		{
			return null;
		}
		final GameClient client = player.getClient();
		if ((client == null) || client.isDetached() || (client.getPlayer() != player))
		{
			return null;
		}
		final String ip = client.getHostAddress();
		return (ip == null) || ip.isBlank() ? null : ip;
	}
	
	public void loadVotes()
	{
		IndividualVoteDB.getInstance().getVotesDB().forEach((ip, votes) ->
		{
			if ((ip == null) || ip.isBlank())
			{
				return;
			}
			synchronized (ipLock(ip))
			{
				final IndividualVote[] copy = new IndividualVote[VoteSite.slotCount()];
				System.arraycopy(votes, 0, copy, 0, Math.min(copy.length, votes.length));
				_foundVoters.putIfAbsent(ip, copy);
			}
		});
	}
	
	protected void loadGlobalVotes()
	{
		synchronized (_globalLock)
		{
			final GlobalVoteDB database = GlobalVoteDB.getInstance();
			if (!database.isLoaded())
			{
				_globalReady = false;
				return;
			}
			final GlobalVote[] loaded = database.getGlobalVotes();
			for (VoteSite site : VoteSite.values())
			{
				final int i = site.getId();
				_globalVotes[i] = (i < loaded.length) && (loaded[i] != null) ? new GlobalVote(i, loaded[i].getVotesLastReward()) : new GlobalVote(i, 0);
			}
			_globalReady = true;
		}
	}
	
	/** All individual claims are saved immediately; never overwrite them from a stale periodic snapshot. */
	public void saveVotes()
	{
		AutoCleanInnecesaryIndividualVotes();
	}
	
	private static List<ItemReward> rewards(int site)
	{
		final List<ItemReward> rewards = new ArrayList<>();
		for (Reward reward : VoteSiteData.getInstance().getRewards(site))
		{
			if ((reward.getItemCount() <= 0) || (ItemData.getInstance().getTemplate(reward.getItemId()) == null))
			{
				LOGGER.warning("Invalid reward configuration for vote site " + site);
				return List.of();
			}
			rewards.add(new ItemReward(reward.getItemId(), reward.getItemCount()));
		}
		return rewards;
	}
	
	private static List<ItemReward> globalRewards(int site)
	{
		final List<ItemReward> result = new ArrayList<>();
		for (Reward reward : VoteSiteData.getInstance().getGlobalRewards(site))
		{
			if ((reward.getItemCount() <= 0) || (ItemData.getInstance().getTemplate(reward.getItemId()) == null))
			{
				return List.of();
			}
			result.add(new ItemReward(reward.getItemId(), reward.getItemCount()));
		}
		return result;
	}
	
	private static boolean deliver(Player player, List<ItemReward> rewards, String context)
	{
		try
		{
			for (ItemReward reward : rewards)
			{
				if (player.addItem(ItemProcessType.REWARD, reward.id(), reward.count(), player, true) == null)
				{
					throw new IllegalStateException("Item was not created");
				}
			}
			return true;
		}
		catch (RuntimeException e)
		{
			LOGGER.log(Level.SEVERE, "Vote reward requires manual review: character " + player.getObjectId() + ", " + context + ". Claim retained to prevent duplicate items.", e);
			player.sendMessage("Your vote reward requires GM review. Please contact a GM.");
			return false;
		}
	}
	
	protected void AutoGlobalVoteReward()
	{
		if (_stopped || !Config.ENABLE_VOTE_SYSTEM || !Config.ENABLE_GLOBAL_VOTE)
		{
			return;
		}
		if (!_globalReady)
		{
			GlobalVoteDB.getInstance().loadGlobalVotes();
			loadGlobalVotes();
			if (!_globalReady)
			{
				return;
			}
		}
		for (VoteSite site : VoteSite.values())
		{
			if (!VoteSiteData.getInstance().isGlobalEnabled(site.getId()))
			{
				continue;
			}
			checkNewUpdate(site.getId());
			if (!_validGlobalCounts[site.getId()])
			{
				continue;
			}
			synchronized (_globalLock)
			{
				if (_stopped || !Config.ENABLE_VOTE_SYSTEM || !Config.ENABLE_GLOBAL_VOTE)
				{
					return;
				}
				final int configuredStep = VoteSiteData.getInstance().getSettings(site.getId()).globalStep();
				final long step = configuredStep > 0 ? configuredStep : (long) Config.GLOBAL_VOTES_AMOUNT_TO_NEXT_REWARD * (site == VoteSite.L2SERVERS ? 25 : 1);
				if ((step <= 0) || (step > Integer.MAX_VALUE))
				{
					LOGGER.warning("Invalid global vote reward threshold.");
					return;
				}
				final GlobalVote vote = _globalVotes[site.getId()];
				if (vote.getCurrentVotes() < (vote.getVotesLastReward() + step))
				{
					continue;
				}
				final List<ItemReward> rewards = globalRewards(site.getId());
				if (rewards.isEmpty())
				{
					continue;
				}
				final Set<String> ips = new HashSet<>();
				final List<Player> recipients = new ArrayList<>();
				for (Player player : World.getInstance().getPlayers())
				{
					final String ip = existIp(player);
					if ((ip != null) && ips.add(ip))
					{
						recipients.add(player);
					}
				}
				if (recipients.isEmpty())
				{
					continue;
				}
				final int next = (int) (vote.getVotesLastReward() + step);
				// Reserve this round durably before sending items. Delivery failures must not replay a partial round.
				if (!GlobalVoteDB.getInstance().saveGlobalVoteChecked(new GlobalVote(site.getId(), next)))
				{
					continue;
				}
				vote.setVotesLastReward(next);
				boolean delivered = true;
				for (Player player : recipients)
				{
					delivered &= deliver(player, rewards, "global site " + site.getId() + ", round " + next);
				}
				if (delivered)
				{
					Broadcast.toAllOnlinePlayers(VoteSiteData.getInstance().getSiteName(site.getId()) + ": Vote rewards delivered. Thank you!", true);
				}
			}
		}
	}
	
	protected void AutoSaveGlobalVotes()
	{
		synchronized (_globalLock)
		{
			if (_globalReady)
			{
				GlobalVoteDB.getInstance().saveGlobalVotes(_globalVotes);
			}
		}
	}
	
	protected void AutoUpdateIndividualVotes()
	{
		AutoCleanInnecesaryIndividualVotes();
	}
	
	protected void AutoCleanInnecesaryIndividualVotes()
	{
		final HashSet<IndividualVote> expired = new HashSet<>();
		_foundVoters.forEach((ip, votes) ->
		{
			synchronized (ipLock(ip))
			{
				for (int i = 0; i < votes.length; i++)
				{
					if ((votes[i] != null) && (getTimeRemaining(votes[i]) <= 0))
					{
						expired.add(votes[i]);
						votes[i] = null;
					}
				}
				boolean empty = true;
				for (IndividualVote vote : votes)
				{
					if (vote != null)
					{
						empty = false;
						break;
					}
				}
				if (empty)
				{
					_foundVoters.remove(ip, votes);
				}
			}
		});
		IndividualVoteDB.getInstance().DeleteVotes(expired);
	}
	
	public void checkAllResponseGlobalVotes()
	{
		if (_stopped || !Config.ENABLE_VOTE_SYSTEM || !Config.ENABLE_GLOBAL_VOTE)
		{
			return;
		}
		ThreadPool.execute(() ->
		{
			for (VoteSite site : VoteSite.values())
			{
				checkNewUpdate(site.getId());
			}
		});
	}
	
	public void checkNewUpdate(int site)
	{
		if (!validSite(site))
		{
			return;
		}
		synchronized (_pollLocks[site])
		{
			pollSite(site);
		}
	}
	
	private void pollSite(int site)
	{
		if (_stopped || !_globalReady || !validSite(site) || !Config.ENABLE_VOTE_SYSTEM || !Config.ENABLE_GLOBAL_VOTE || !VoteSiteData.getInstance().isGlobalEnabled(site))
		{
			return;
		}
		synchronized (_globalLock)
		{
			final long now = System.currentTimeMillis();
			if (_nextGlobalPoll[site] > now)
			{
				return;
			}
			final long configured = VoteSiteData.getInstance().getSettings(site).checkInterval();
			final long interval = configured > 0 ? configured : Config.NEXT_TIME_TO_CHECK_AUTO_GLOBAL_VOTES_REWARD;
			_nextGlobalPoll[site] = now + Math.max(1000, interval);
			_validGlobalCounts[site] = false;
		}
		final int total;
		try
		{
			total = getGlobalVotesResponse(site);
		}
		catch (RuntimeException e)
		{
			LOGGER.warning("Could not update global vote count for site " + site + " (" + e.getClass().getSimpleName() + ").");
			return;
		}
		if (total < 0)
		{
			return;
		}
		synchronized (_globalLock)
		{
			if (_stopped)
			{
				return;
			}
			final GlobalVote vote = _globalVotes[site];
			if ((total < vote.getVotesLastReward()) && GlobalVoteDB.getInstance().saveGlobalVoteChecked(new GlobalVote(site, 0)))
			{
				vote.setVotesLastReward(0);
			}
			vote.setCurrentVotes(total);
			_validGlobalCounts[site] = true;
		}
	}
	
	public void shutdown()
	{
		_stopped = true;
		stopAutoTasks();
		synchronized (_globalLock)
		{
			AutoSaveGlobalVotes();
		}
		saveVotes();
	}
	
	private void stopAutoTasks()
	{
		if (_saveGlobalVotes != null)
		{
			_saveGlobalVotes.cancel(false);
			_saveGlobalVotes = null;
		}
		if (_updateIndividualVotes != null)
		{
			_updateIndividualVotes.cancel(false);
			_updateIndividualVotes = null;
		}
		if (_autoGlobalVotesReward != null)
		{
			_autoGlobalVotesReward.cancel(false);
			_autoGlobalVotesReward = null;
		}
	}
	
	public void Shutdown()
	{
		shutdown();
	}
	
	public static VoteManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final VoteManager INSTANCE = new VoteManager();
	}
}
