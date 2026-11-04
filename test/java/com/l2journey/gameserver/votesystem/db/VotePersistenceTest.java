package com.l2journey.gameserver.votesystem.db;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.Test;

import com.l2journey.Config;
import com.l2journey.commons.database.DatabaseFactory;
import com.l2journey.gameserver.votesystem.model.IndividualVote;

class VotePersistenceTest
{
	@Test
	void loaderKeepsDifferentIpsInIndependentArrays() throws Exception
	{
		try (var database = mockStatic(DatabaseFactory.class))
		{
			final Connection con = mock(Connection.class);
			final PreparedStatement ps = mock(PreparedStatement.class);
			final ResultSet rows = mock(ResultSet.class);
			database.when(DatabaseFactory::getConnection).thenReturn(con);
			when(con.prepareStatement(anyString())).thenReturn(ps);
			when(ps.executeQuery()).thenReturn(rows);
			when(rows.next()).thenReturn(true, true, false);
			when(rows.getString("voterIp")).thenReturn("192.0.2.1", "192.0.2.2");
			final var constructor = IndividualVoteDB.class.getDeclaredConstructor();
			constructor.setAccessible(true);
			final var data = constructor.newInstance();
			final var votes = data.getVotesDB();
			assertNotSame(votes.get("192.0.2.1"), votes.get("192.0.2.2"));
			assertEquals("192.0.2.1", votes.get("192.0.2.1")[0].getVoterIp());
			assertEquals("192.0.2.2", votes.get("192.0.2.2")[0].getVoterIp());
		}
	}

	@Test
	void globalLoaderReadsTheFirstRow() throws Exception
	{
		try (var database = mockStatic(DatabaseFactory.class))
		{
			final Connection con = mock(Connection.class);
			final PreparedStatement ps = mock(PreparedStatement.class);
			final ResultSet rows = mock(ResultSet.class);
			database.when(DatabaseFactory::getConnection).thenReturn(con);
			when(con.prepareStatement(anyString())).thenReturn(ps);
			when(ps.executeQuery()).thenReturn(rows);
			when(rows.next()).thenReturn(true, false);
			when(rows.getInt("voteSite")).thenReturn(3);
			when(rows.getInt("lastRewardVotes")).thenReturn(250);
			final var constructor = GlobalVoteDB.class.getDeclaredConstructor();
			constructor.setAccessible(true);
			final var data = constructor.newInstance();
			assertTrue(data.isLoaded());
			assertEquals(250, data.getGlobalVotes()[3].getVotesLastReward());
		}
	}

	@Test
	void durableClaimCommitsAndDatabaseFailureRollsBack() throws Exception
	{
		try (var database = mockStatic(DatabaseFactory.class))
		{
			final Connection con = mock(Connection.class);
			final PreparedStatement ps = mock(PreparedStatement.class);
			final ResultSet rows = mock(ResultSet.class);
			database.when(DatabaseFactory::getConnection).thenReturn(con);
			when(con.prepareStatement(anyString())).thenReturn(ps);
			when(ps.executeQuery()).thenReturn(rows);
			final var constructor = IndividualVoteDB.class.getDeclaredConstructor();
			constructor.setAccessible(true);
			final var data = constructor.newInstance();
			final var vote = new IndividualVote("192.0.2.1", 0, System.currentTimeMillis(), 0, true);
			assertTrue(data.claimVote(vote));
			verify(con).commit();
			when(ps.executeUpdate()).thenThrow(new SQLException("Expected test failure"));
			assertFalse(data.claimVote(vote));
			verify(con).rollback();
		}
	}

	@Test
	void alreadyRewardedActiveDatabaseRowCannotBeClaimedAgain() throws Exception
	{
		final long interval = Config.INTERVAL_TO_NEXT_VOTE;
		Config.INTERVAL_TO_NEXT_VOTE = 43200000;
		try (var database = mockStatic(DatabaseFactory.class))
		{
			final Connection con = mock(Connection.class);
			final PreparedStatement ps = mock(PreparedStatement.class);
			final ResultSet rows = mock(ResultSet.class);
			database.when(DatabaseFactory::getConnection).thenReturn(con);
			when(con.prepareStatement(anyString())).thenReturn(ps);
			when(ps.executeQuery()).thenReturn(rows);
			final var constructor = IndividualVoteDB.class.getDeclaredConstructor();
			constructor.setAccessible(true);
			final var data = constructor.newInstance();
			when(rows.next()).thenReturn(true);
			when(rows.getBoolean("alreadyRewarded")).thenReturn(true);
			when(rows.getLong("votingTimeSite")).thenReturn(System.currentTimeMillis());
			assertFalse(data.claimVote(new IndividualVote("192.0.2.1", 0, System.currentTimeMillis(), 0, true)));
			verify(con).rollback();
			verify(ps, never()).executeUpdate();
		}
		finally { Config.INTERVAL_TO_NEXT_VOTE = interval; }
	}
}
