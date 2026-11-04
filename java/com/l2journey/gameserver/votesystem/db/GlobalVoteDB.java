package com.l2journey.gameserver.votesystem.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.l2journey.commons.database.DatabaseFactory;
import com.l2journey.gameserver.votesystem.Enum.VoteSite;
import com.l2journey.gameserver.votesystem.model.GlobalVote;

public class GlobalVoteDB
{
	private static final Logger LOGGER = Logger.getLogger(GlobalVoteDB.class.getName());
	private volatile boolean _loaded;
	private final GlobalVote[] _globalVotes = new GlobalVote[VoteSite.slotCount()];
	private GlobalVoteDB() { loadGlobalVotes(); }

	public synchronized void loadGlobalVotes()
	{
		_loaded = false;
		for (VoteSite site : VoteSite.values()) { _globalVotes[site.getId()] = new GlobalVote(site.getId(), 0); }
		try (Connection con = DatabaseFactory.getConnection(); PreparedStatement ps = con.prepareStatement("SELECT voteSite,lastRewardVotes FROM globalvotes"); ResultSet rs = ps.executeQuery())
		{
			while (rs.next())
			{
				final int site = rs.getInt("voteSite");
				if (VoteSite.fromId(site) != null) { _globalVotes[site] = new GlobalVote(site, Math.max(0, rs.getInt("lastRewardVotes"))); }
			}
			_loaded = true;
		}
		catch (Exception e) { LOGGER.log(Level.WARNING, "Could not load global votes", e); }
	}

	public boolean isLoaded() { return _loaded; }

	public boolean saveGlobalVoteChecked(GlobalVote vote)
	{
		try (Connection con = DatabaseFactory.getConnection(); PreparedStatement ps = con.prepareStatement("INSERT INTO globalvotes(voteSite,lastRewardVotes) VALUES(?,?) ON DUPLICATE KEY UPDATE lastRewardVotes=VALUES(lastRewardVotes)"))
		{
			ps.setInt(1, vote.getVoyeSite());
			ps.setInt(2, vote.getVotesLastReward());
			ps.executeUpdate();
			return true;
		}
		catch (Exception e) { LOGGER.log(Level.WARNING, "Could not save global vote checkpoint", e); return false; }
	}

	public void saveGlobalVote(GlobalVote vote) { saveGlobalVoteChecked(vote); }
	public void saveGlobalVotes(GlobalVote[] votes) { for (GlobalVote vote : votes) { if (vote != null) { saveGlobalVote(vote); } } }
	public synchronized GlobalVote[] getGlobalVotes()
	{
		final GlobalVote[] copy = new GlobalVote[_globalVotes.length];
		for (int i = 0; i < copy.length; i++) { copy[i] = _globalVotes[i] == null ? null : new GlobalVote(i, _globalVotes[i].getVotesLastReward()); }
		return copy;
	}
	public static GlobalVoteDB getInstance() { return SingleHolder.INSTANCE; }
	private static class SingleHolder { protected static final GlobalVoteDB INSTANCE = new GlobalVoteDB(); }
}
