package com.l2journey.gameserver.rebirth;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.l2journey.RebirthEngineConfigs.Reward;
import com.l2journey.commons.database.DatabaseFactory;

/** Durable rebirth journal. All reset mutations must use transactional tables. */
public class RebirthRepository
{
	public record Progress(int count, String state, int level, long exp, int itemId, long itemCount, List<Integer> preserved)
	{
		public boolean pending() { return state.equals("RESET_PENDING"); }
		public boolean complete() { return state.equals("COMPLETE"); }
	}
	public Progress read(int playerId) throws SQLException
	{
		try (Connection con = DatabaseFactory.getConnection(); PreparedStatement ps = con.prepareStatement("SELECT rebirthCount,state,resetLevel,resetExp,rewardItemId,rewardItemCount,preservedSkills FROM character_rebirths WHERE playerId=?"))
		{
			ps.setInt(1, playerId);
			try (ResultSet rs = ps.executeQuery())
			{
				if (!rs.next()) { return new Progress(0, "COMPLETE", 0, 0, 0, 0, List.of()); }
				final String skills = rs.getString("preservedSkills");
				final List<Integer> preserved = skills == null || skills.isBlank() ? List.of() : java.util.Arrays.stream(skills.split(",")).map(Integer::parseInt).toList();
				return new Progress(rs.getInt("rebirthCount"), rs.getString("state"), rs.getInt("resetLevel"), rs.getLong("resetExp"), rs.getInt("rewardItemId"), rs.getLong("rewardItemCount"), preserved);
			}
		}
	}
	private static void requireTransactionalTables(Connection con) throws SQLException
	{
		try (PreparedStatement ps = con.prepareStatement("SELECT TABLE_NAME,ENGINE FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN ('characters','character_skills','character_shortcuts','character_rebirths')"); ResultSet rs = ps.executeQuery())
		{
			int count = 0;
			while (rs.next())
			{
				if (!"InnoDB".equalsIgnoreCase(rs.getString("ENGINE"))) { throw new SQLException("Rebirth requires InnoDB: " + rs.getString("TABLE_NAME")); }
				count++;
			}
			if (count != 4) { throw new SQLException("Rebirth tables missing; import the rebirth SQL update"); }
		}
	}
	public void begin(int playerId, int expectedCount, int maximum, int level, long exp, int itemId, long itemCount, List<Integer> preserved) throws SQLException
	{
		if (expectedCount < 0 || expectedCount >= maximum || level < 1 || exp < 0 || itemId <= 0 || itemCount <= 0 || preserved.stream().anyMatch(id -> id <= 0))
		{ throw new IllegalArgumentException("Invalid rebirth reset"); }
		try (Connection con = DatabaseFactory.getConnection())
		{
			requireTransactionalTables(con);
			con.setAutoCommit(false);
			try
			{
				try (PreparedStatement ps = con.prepareStatement("SELECT charId FROM characters WHERE charId=? FOR UPDATE"))
				{
					ps.setInt(1, playerId);
					try (ResultSet rs = ps.executeQuery()) { if (!rs.next()) { throw new SQLException("Character not found"); } }
				}
				try (PreparedStatement ps = con.prepareStatement("INSERT IGNORE INTO character_rebirths(playerId,rebirthCount) VALUES(?,0)"))
				{ ps.setInt(1, playerId); ps.executeUpdate(); }
				try (PreparedStatement ps = con.prepareStatement("SELECT rebirthCount,state FROM character_rebirths WHERE playerId=? FOR UPDATE"))
				{
					ps.setInt(1, playerId);
					try (ResultSet rs = ps.executeQuery())
					{
						if (!rs.next() || rs.getInt("rebirthCount") != expectedCount || !"COMPLETE".equals(rs.getString("state")))
						{ throw new SQLException("Rebirth state changed or a previous operation is unresolved"); }
					}
				}
				try (PreparedStatement ps = con.prepareStatement("UPDATE characters SET level=?,exp=?,expBeforeDeath=0,curHp=1,curMp=1,curCp=1 WHERE charId=?"))
				{ ps.setInt(1, level); ps.setLong(2, exp); ps.setInt(3, playerId); if (ps.executeUpdate() != 1) { throw new SQLException("Character reset failed"); } }
				final String placeholders = preserved.stream().map(id -> "?").collect(Collectors.joining(","));
				deleteSkills(con, "DELETE FROM character_skills WHERE charId=? AND class_index=0", "skill_id", playerId, placeholders, preserved);
				deleteSkills(con, "DELETE FROM character_shortcuts WHERE charId=? AND class_index=0 AND type=2", "shortcut_id", playerId, placeholders, preserved);
				try (PreparedStatement ps = con.prepareStatement("UPDATE character_rebirths SET rebirthCount=?,state='RESET_PENDING',resetLevel=?,resetExp=?,rewardItemId=?,rewardItemCount=?,preservedSkills=? WHERE playerId=?"))
				{
					ps.setInt(1, expectedCount + 1); ps.setInt(2, level); ps.setLong(3, exp); ps.setInt(4, itemId); ps.setLong(5, itemCount);
					ps.setString(6, preserved.stream().map(String::valueOf).collect(Collectors.joining(","))); ps.setInt(7, playerId);
					if (ps.executeUpdate() != 1) { throw new SQLException("Rebirth journal update failed"); }
				}
				con.commit();
			}
			catch (SQLException | RuntimeException e)
			{
				try { con.rollback(); } catch (SQLException rollback) { e.addSuppressed(rollback); }
				throw e;
			}
		}
	}
	private static void deleteSkills(Connection con, String sql, String column, int playerId, String placeholders, List<Integer> preserved) throws SQLException
	{
		try (PreparedStatement ps = con.prepareStatement(sql + (preserved.isEmpty() ? "" : " AND " + column + " NOT IN (" + placeholders + ")")))
		{
			ps.setInt(1, playerId);
			for (int i = 0; i < preserved.size(); i++) { ps.setInt(i + 2, preserved.get(i)); }
			ps.executeUpdate();
		}
	}
	// These states share the rebirth journal so neither operation can overlap the other.
	static final String SKILL_FEE_RESERVED = "SKILL_FEE_RESERVED";
	static final String SKILL_REFUND_PENDING = "SKILL_REFUND_PENDING";
	static final String REFUND_RESERVED = "REFUND_RESERVED";

	void reserveSkillResetFee(int playerId, int count, long fee, Map<Integer, Integer> ranks, List<Reward> refunds) throws SQLException
	{
		final String skills = ranks.keySet().stream().map(String::valueOf).collect(Collectors.joining(","));
		final String items = refunds.stream().map(reward -> reward.id() + ":" + reward.count()).collect(Collectors.joining(","));
		try (Connection con = DatabaseFactory.getConnection())
		{
			requireTransactionalTables(con);
			con.setAutoCommit(false);
			try
			{
				try (PreparedStatement ps = con.prepareStatement("UPDATE character_rebirths SET state=?,rewardItemId=57,rewardItemCount=?,preservedSkills=?,skillResetRefunds=? WHERE playerId=? AND rebirthCount=? AND state='COMPLETE'"))
				{
					ps.setString(1, SKILL_FEE_RESERVED);
					ps.setLong(2, fee);
					ps.setString(3, skills);
					ps.setString(4, items);
					ps.setInt(5, playerId);
					ps.setInt(6, count);
					if (ps.executeUpdate() != 1) { throw new SQLException("Could not reserve rebirth skill reset"); }
				}
				try (PreparedStatement ps = con.prepareStatement("SELECT skill_level FROM character_skills WHERE charId=? AND class_index=0 AND skill_id=? FOR UPDATE"))
				{
					ps.setInt(1, playerId);
					for (var rank : ranks.entrySet())
					{
						ps.setInt(2, rank.getKey());
						try (ResultSet rs = ps.executeQuery())
						{
							if (!rs.next() || rs.getInt(1) != rank.getValue()) { throw new SQLException("Saved rebirth skill rank differs from the quote"); }
						}
					}
				}
				con.commit();
			}
			catch (SQLException | RuntimeException e)
			{
				try { con.rollback(); } catch (SQLException rollback) { e.addSuppressed(rollback); }
				throw e;
			}
		}
	}

	void commitSkillReset(int playerId, int count, Map<Integer, Integer> ranks) throws SQLException
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			con.setAutoCommit(false);
			try
			{
				// Update the journal under the same transaction as the skill and shortcut deletions.
				if (!transition(con, playerId, count, SKILL_FEE_RESERVED, SKILL_REFUND_PENDING))
				{
					throw new SQLException("Rebirth skill reset reservation is missing");
				}
				try (PreparedStatement skills = con.prepareStatement("DELETE FROM character_skills WHERE charId=? AND class_index=0 AND skill_id=? AND skill_level=?");
					PreparedStatement shortcuts = con.prepareStatement("DELETE FROM character_shortcuts WHERE charId=? AND class_index=0 AND type=2 AND shortcut_id=?"))
				{
					skills.setInt(1, playerId);
					shortcuts.setInt(1, playerId);
					for (var rank : ranks.entrySet())
					{
						skills.setInt(2, rank.getKey());
						skills.setInt(3, rank.getValue());
						if (skills.executeUpdate() != 1) { throw new SQLException("Rebirth skill changed during reset"); }
						shortcuts.setInt(2, rank.getKey());
						shortcuts.executeUpdate();
					}
				}
				con.commit();
			}
			catch (SQLException | RuntimeException e)
			{
				try { con.rollback(); } catch (SQLException rollback) { e.addSuppressed(rollback); }
				throw e;
			}
		}
	}

	List<Reward> readSkillRefunds(int playerId, int count) throws SQLException
	{
		try (Connection con = DatabaseFactory.getConnection(); PreparedStatement ps = con.prepareStatement("SELECT skillResetRefunds FROM character_rebirths WHERE playerId=? AND rebirthCount=? AND state=?"))
		{
			ps.setInt(1, playerId);
			ps.setInt(2, count);
			ps.setString(3, SKILL_REFUND_PENDING);
			try (ResultSet rs = ps.executeQuery())
			{
				if (!rs.next()) { throw new SQLException("Rebirth skill refund is missing"); }
				final String saved = rs.getString(1);
				if (saved.isBlank()) { return List.of(); }
				final List<Reward> rewards = new ArrayList<>();
				for (String pair : saved.split(","))
				{
					final String[] values = pair.split(":");
					rewards.add(new Reward(Integer.parseInt(values[0]), Long.parseLong(values[1])));
				}
				return rewards;
			}
		}
	}

	void verifyInventoryCount(int playerId, int itemId, long expected) throws SQLException
	{
		try (Connection con = DatabaseFactory.getConnection(); PreparedStatement ps = con.prepareStatement("SELECT COALESCE(SUM(count),0) FROM items WHERE owner_id=? AND item_id=? AND loc IN ('INVENTORY','PAPERDOLL')"))
		{
			ps.setInt(1, playerId);
			ps.setInt(2, itemId);
			try (ResultSet rs = ps.executeQuery())
			{
				if (!rs.next() || rs.getLong(1) != expected) { throw new SQLException("Inventory persistence could not be confirmed for item " + itemId); }
			}
		}
	}

	public boolean reserveReward(int playerId, int count) throws SQLException { return transition(playerId, count, "RESET_PENDING", "REWARD_RESERVED"); }
	public boolean finish(int playerId, int count) throws SQLException { return transition(playerId, count, "REWARD_RESERVED", "COMPLETE"); }
	boolean transition(int playerId, int count, String before, String after) throws SQLException
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			return transition(con, playerId, count, before, after);
		}
	}

	private static boolean transition(Connection con, int playerId, int count, String before, String after) throws SQLException
	{
		try (PreparedStatement ps = con.prepareStatement("UPDATE character_rebirths SET state=? WHERE playerId=? AND rebirthCount=? AND state=?"))
		{
			ps.setString(1, after);
			ps.setInt(2, playerId);
			ps.setInt(3, count);
			ps.setString(4, before);
			return ps.executeUpdate() == 1;
		}
	}
}
