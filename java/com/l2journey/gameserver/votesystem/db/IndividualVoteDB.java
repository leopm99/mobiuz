package com.l2journey.gameserver.votesystem.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.l2journey.Config;
import com.l2journey.commons.database.DatabaseFactory;
import com.l2journey.gameserver.votesystem.Enum.VoteSite;
import com.l2journey.gameserver.votesystem.model.IndividualVote;

/** Persistence of individual claims, keyed by IP and voting site. */
public class IndividualVoteDB
{
	private static final Logger LOGGER = Logger.getLogger(IndividualVoteDB.class.getName());
	private static final String UPSERT = "INSERT INTO individualvotes(voterIp,voteSite,diffTime,votingTimeSite,alreadyRewarded) VALUES(?,?,?,?,?) ON DUPLICATE KEY UPDATE diffTime=VALUES(diffTime), votingTimeSite=VALUES(votingTimeSite), alreadyRewarded=VALUES(alreadyRewarded)";
	private final Map<String, IndividualVote[]> _votes = new HashMap<>();
	
	private IndividualVoteDB()
	{
		loadVotes();
	}
	
	public synchronized void loadVotes()
	{
		final Map<String, IndividualVote[]> loaded = new HashMap<>();
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT voterIp,voteSite,diffTime,votingTimeSite,alreadyRewarded FROM individualvotes");
			ResultSet rs = ps.executeQuery())
		{
			while (rs.next())
			{
				final int site = rs.getInt("voteSite");
				if (VoteSite.fromId(site) == null)
				{
					LOGGER.warning("Ignoring invalid persisted vote site " + site);
					continue;
				}
				final String ip = rs.getString("voterIp");
				if ((ip == null) || ip.isBlank())
				{
					continue;
				}
				loaded.computeIfAbsent(ip, key -> new IndividualVote[VoteSite.slotCount()])[site] = new IndividualVote(ip, rs.getLong("diffTime"), rs.getLong("votingTimeSite"), site, rs.getBoolean("alreadyRewarded"));
			}
			_votes.clear();
			_votes.putAll(loaded);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Could not load individual votes", e);
		}
	}
	
	/**
	 * Reserve the reward durably before delivery. An active rewarded row cannot be claimed twice.
	 * @param vote
	 * @return
	 */
	public boolean claimVote(IndividualVote vote)
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			con.setAutoCommit(false);
			try
			{
				try (PreparedStatement ps = con.prepareStatement("SELECT diffTime,votingTimeSite,alreadyRewarded FROM individualvotes WHERE voterIp=? AND voteSite=? FOR UPDATE"))
				{
					ps.setString(1, vote.getVoterIp());
					ps.setInt(2, vote.getVoteSite());
					try (ResultSet rs = ps.executeQuery())
					{
						if (rs.next() && rs.getBoolean("alreadyRewarded") && ((rs.getLong("votingTimeSite") + rs.getLong("diffTime") + Config.INTERVAL_TO_NEXT_VOTE) > System.currentTimeMillis()))
						{
							con.rollback();
							return false;
						}
					}
				}
				try (PreparedStatement ps = con.prepareStatement(UPSERT))
				{
					bind(ps, vote);
					ps.executeUpdate();
				}
				con.commit();
				return true;
			}
			catch (Exception e)
			{
				con.rollback();
				throw e;
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Could not persist individual vote claim", e);
			return false;
		}
	}
	
	private static void bind(PreparedStatement ps, IndividualVote vote) throws SQLException
	{
		ps.setString(1, vote.getVoterIp());
		ps.setInt(2, vote.getVoteSite());
		ps.setLong(3, vote.getDiffTime());
		ps.setLong(4, vote.getVotingTimeSite());
		ps.setBoolean(5, vote.getAlreadyRewarded());
	}
	
	public void SaveVotes(Map<String, IndividualVote[]> votes)
	{
		if ((votes == null) || votes.isEmpty())
		{
			return;
		}
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(UPSERT))
		{
			for (IndividualVote[] entries : votes.values())
			{
				for (IndividualVote vote : entries)
				{
					if (vote != null)
					{
						bind(ps, vote);
						ps.addBatch();
					}
				}
			}
			ps.executeBatch();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Could not save individual votes", e);
		}
	}
	
	public void SaveVote(IndividualVote vote)
	{
		if (vote != null)
		{
			SaveVotes(Map.of(vote.getVoterIp(), new IndividualVote[]
			{
				vote
			}));
		}
	}
	
	public void DeleteVotes(HashSet<IndividualVote> votes)
	{
		if ((votes == null) || votes.isEmpty())
		{
			return;
		}
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("DELETE FROM individualvotes WHERE voterIp=? AND voteSite=? AND votingTimeSite=? AND diffTime=?"))
		{
			for (IndividualVote vote : votes)
			{
				ps.setString(1, vote.getVoterIp());
				ps.setInt(2, vote.getVoteSite());
				ps.setLong(3, vote.getVotingTimeSite());
				ps.setLong(4, vote.getDiffTime());
				ps.addBatch();
			}
			ps.executeBatch();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Could not delete expired votes", e);
		}
	}
	
	public synchronized Map<String, IndividualVote[]> getVotesDB()
	{
		final Map<String, IndividualVote[]> copy = new HashMap<>();
		_votes.forEach((ip, votes) -> copy.put(ip, votes.clone()));
		return copy;
	}
	
	public static IndividualVoteDB getInstance()
	{
		return SingleHolder.INSTANCE;
	}
	
	private static class SingleHolder
	{
		protected static final IndividualVoteDB INSTANCE = new IndividualVoteDB();
	}
}
