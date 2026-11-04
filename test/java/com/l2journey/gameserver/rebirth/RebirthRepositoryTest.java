package com.l2journey.gameserver.rebirth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import com.l2journey.commons.database.DatabaseFactory;

class RebirthRepositoryTest
{
	private MockedStatic<DatabaseFactory> _database;
	private Connection _connection;
	private ResultSet _engines;
	private ResultSet _journal;
	private final Map<String, PreparedStatement> _statements = new HashMap<>();
	private RebirthRepository _repository;
	@BeforeEach
	void setup() throws Exception
	{
		_database = mockStatic(DatabaseFactory.class);
		_connection = mock(Connection.class);
		_database.when(DatabaseFactory::getConnection).thenReturn(_connection);
		_engines = mock(ResultSet.class);
		when(_engines.next()).thenReturn(true, true, true, true, false);
		when(_engines.getString("ENGINE")).thenReturn("InnoDB");
		final ResultSet character = mock(ResultSet.class);
		when(character.next()).thenReturn(true);
		_journal = mock(ResultSet.class);
		when(_journal.next()).thenReturn(true);
		when(_journal.getString("state")).thenReturn("COMPLETE");
		when(_connection.prepareStatement(anyString())).thenAnswer(call ->
		{
			final String sql = call.getArgument(0);
			if (_statements.containsKey(sql)) { return _statements.get(sql); }
			final PreparedStatement ps = mock(PreparedStatement.class);
			when(ps.executeUpdate()).thenReturn(1);
			if (sql.contains("information_schema")) { when(ps.executeQuery()).thenReturn(_engines); }
			else if (sql.startsWith("SELECT charId")) { when(ps.executeQuery()).thenReturn(character); }
			else if (sql.startsWith("SELECT rebirthCount")) { when(ps.executeQuery()).thenReturn(_journal); }
			_statements.put(sql, ps);
			return ps;
		});
		_repository = new RebirthRepository();
	}
	@AfterEach
	void cleanup() { _database.close(); }
	private void begin() throws SQLException { _repository.begin(42, 0, 3, 1, 0, 2822, 1, List.of(35200)); }
	@Test
	void commitIncludesLevelSkillsShortcutsAndJournal() throws Exception
	{
		begin();
		verify(_connection).setAutoCommit(false);
		verify(_connection).commit();
		verify(_connection, never()).rollback();
		assertTrue(_statements.keySet().stream().anyMatch(sql -> sql.contains("state='RESET_PENDING'")));
		final var delete = _statements.entrySet().stream().filter(entry -> entry.getKey().startsWith("DELETE FROM character_skills ")).findFirst().orElseThrow();
		assertTrue(delete.getKey().contains("class_index=0"));
		assertTrue(delete.getKey().contains("skill_id NOT IN (?)"));
		verify(delete.getValue()).setInt(1, 42);
		verify(delete.getValue()).setInt(2, 35200);
		assertTrue(_statements.keySet().stream().anyMatch(sql -> sql.startsWith("DELETE FROM character_shortcuts") && sql.contains("type=2") && sql.contains("shortcut_id NOT IN (?)")));
	}
	@Test
	void nonTransactionalTablesFailBeforeMutatingAnything() throws Exception
	{
		when(_engines.getString("ENGINE")).thenReturn("MyISAM");
		assertThrows(SQLException.class, this::begin);
		verify(_connection, never()).setAutoCommit(false);
		verify(_connection, never()).commit();
		assertEquals(1, _statements.size());
	}
	@Test
	void staleCountRollsBackBeforeReset() throws Exception
	{
		when(_journal.getInt("rebirthCount")).thenReturn(1);
		assertThrows(SQLException.class, this::begin);
		verify(_connection).rollback();
		verify(_connection, never()).commit();
		assertFalse(_statements.keySet().stream().anyMatch(sql -> sql.startsWith("UPDATE characters")));
	}
	@Test
	void unresolvedOperationCannotStartAnotherReset() throws Exception
	{
		when(_journal.getString("state")).thenReturn("REWARD_RESERVED");
		assertThrows(SQLException.class, this::begin);
		verify(_connection).rollback();
		verify(_connection, never()).commit();
	}
	@Test
	void failedCommitIsReportedAndRolledBack() throws Exception
	{
		doThrow(new SQLException("fixture commit failed")).when(_connection).commit();
		assertThrows(SQLException.class, this::begin);
		verify(_connection).rollback();
	}
	@Test
	void failedResetStatementCannotCommitTheJournal() throws Exception
	{
		final PreparedStatement failed = mock(PreparedStatement.class);
		when(failed.executeUpdate()).thenThrow(new SQLException("fixture write failed"));
		_statements.put("UPDATE characters SET level=?,exp=?,expBeforeDeath=0,curHp=1,curMp=1,curCp=1 WHERE charId=?", failed);
		assertThrows(SQLException.class, this::begin);
		verify(_connection).rollback();
		verify(_connection, never()).commit();
	}
	@Test
	void rewardReservationUsesAConditionalStateTransition() throws Exception
	{
		assertTrue(_repository.reserveReward(42, 1));
		final var ps = _statements.values().iterator().next();
		verify(ps).setString(1, "REWARD_RESERVED");
		verify(ps).setInt(2, 42);
		verify(ps).setInt(3, 1);
		verify(ps).setString(4, "RESET_PENDING");
		when(ps.executeUpdate()).thenReturn(0);
		assertFalse(_repository.reserveReward(42, 1));
	}
	@Test
	void skillResetDeletesOnlyTheQuotedMainClassRanks() throws Exception
	{
		_repository.commitSkillReset(42, 2, Map.of(35200, 3));
		final var skill = _statements.get("DELETE FROM character_skills WHERE charId=? AND class_index=0 AND skill_id=? AND skill_level=?");
		verify(skill).setInt(1, 42);
		verify(skill).setInt(2, 35200);
		verify(skill).setInt(3, 3);
		assertTrue(_statements.keySet().stream().noneMatch(sql -> sql.startsWith("UPDATE characters") || sql.contains("SET rebirthCount")));
		verify(_connection).commit();
	}

	@Test
	void changedSkillRankRollsBackTheRefundStateAndSkillDeletion() throws Exception
	{
		final PreparedStatement missing = mock(PreparedStatement.class);
		_statements.put("DELETE FROM character_skills WHERE charId=? AND class_index=0 AND skill_id=? AND skill_level=?", missing);
		assertThrows(SQLException.class, () -> _repository.commitSkillReset(42, 2, Map.of(35200, 3)));
		verify(_connection).rollback();
		verify(_connection, never()).commit();
	}

}
