package com.l2journey.gameserver.auctionhouse;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.l2journey.commons.database.DatabaseFactory;
import com.l2journey.gameserver.model.item.enums.ItemLocation;
import com.l2journey.gameserver.model.item.instance.Item;

/** Durable reservations. Inventory/mail use their own connections, so uncertain work is never replayed. */
class AuctionHouseStore
{
	record Row(int id, int ownerId, int objectId, int itemId, long quantity, long price, long expiresAt, String state) { }
	record Reservation(int listingId, String operationId) { }

	void validateSchema() throws SQLException
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			for (String table : List.of("community_auction_listings", "community_auction_operations", "items", "messages"))
			{
				try (PreparedStatement ps = statement(con, "SELECT ENGINE FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=?", table);
					ResultSet rs = ps.executeQuery())
				{
					if (!rs.next() || !"InnoDB".equalsIgnoreCase(rs.getString(1)))
					{
						throw new SQLException("Missing InnoDB table: " + table);
					}
				}
			}
		}
	}

	List<Row> load() throws SQLException
	{
		final List<Row> rows = new ArrayList<>();
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT id,owner_id,object_id,item_id,quantity,unit_price,expires_at,state FROM community_auction_listings WHERE state IN ('ACTIVE','BUSY','REVIEW') ORDER BY id");
			ResultSet rs = ps.executeQuery())
		{
			while (rs.next())
			{
				rows.add(new Row(rs.getInt(1), rs.getInt(2), rs.getInt(3), rs.getInt(4), rs.getLong(5), rs.getLong(6), rs.getLong(7), rs.getString(8)));
			}
		}
		return rows;
	}

	Reservation create(int ownerId, Item source, long quantity, long price, long expiresAt, long fee, long adenaBefore) throws SQLException
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			con.setAutoCommit(false);
			try
			{
				final int id;
				try (PreparedStatement ps = con.prepareStatement("INSERT INTO community_auction_listings (owner_id,object_id,item_id,quantity,unit_price,expires_at,state) VALUES (?,?,?,?,?,?,'BUSY')", Statement.RETURN_GENERATED_KEYS))
				{
					bind(ps, ownerId, source.getObjectId(), source.getId(), quantity, price, expiresAt);
					ps.executeUpdate();
					try (ResultSet rs = ps.getGeneratedKeys())
					{
						if (!rs.next()) { throw new SQLException("No listing ID"); }
						id = Math.toIntExact(rs.getLong(1));
					}
				}
				final String operation = UUID.randomUUID().toString();
				insertOperation(con, operation, id, "CREATE", ownerId, source, quantity, fee, adenaBefore);
				con.commit();
				return new Reservation(id, operation);
			}
			catch (SQLException | RuntimeException e) { con.rollback(); throw e; }
		}
	}

	Reservation reserve(int id, String kind, int actorId, Item source, long quantity, long amount, long adenaBefore) throws SQLException
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			con.setAutoCommit(false);
			try
			{
				change(con, "UPDATE community_auction_listings SET state='BUSY' WHERE id=? AND state='ACTIVE'", id);
				final String operation = UUID.randomUUID().toString();
				insertOperation(con, operation, id, kind, actorId, source, quantity, amount, adenaBefore);
				con.commit();
				return new Reservation(id, operation);
			}
			catch (SQLException | RuntimeException e) { con.rollback(); throw e; }
		}
	}

	private void insertOperation(Connection con, String id, int listingId, String kind, int actorId, Item source, long quantity, long amount, long adenaBefore) throws SQLException
	{
		change(con, "INSERT INTO community_auction_operations (id,listing_id,kind,actor_id,source_object_id,source_count,quantity,amount,adena_before,stage) VALUES (?,?,?,?,?,?,?,?,?,'RESERVED')",
			id, listingId, kind, actorId, source.getObjectId(), source.getCount(), quantity, amount, adenaBefore);
	}

	void checkpoint(Reservation op, String stage, int targetObjectId, int messageId) throws SQLException
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			change(con, "UPDATE community_auction_operations SET stage=?,target_object_id=IF(?=0,target_object_id,?),message_id=? WHERE id=? AND stage<>'DONE'", stage, targetObjectId, targetObjectId, messageId, op.operationId());
		}
	}

	void finish(Reservation op, int objectId, long remaining, String state) throws SQLException
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			con.setAutoCommit(false);
			try
			{
				change(con, "UPDATE community_auction_listings SET object_id=?,quantity=?,state=? WHERE id=? AND state='BUSY'", objectId, remaining, state, op.listingId());
				change(con, "UPDATE community_auction_operations SET stage='DONE' WHERE id=? AND stage<>'DONE'", op.operationId());
				con.commit();
			}
			catch (SQLException | RuntimeException e) { con.rollback(); throw e; }
		}
	}

	void review(int id) throws SQLException
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = statement(con, "UPDATE community_auction_listings SET state='REVIEW' WHERE id=? AND state IN ('ACTIVE','BUSY')", id))
		{
			ps.executeUpdate();
		}
	}

	Item restore(Row row) throws SQLException
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = statement(con, "SELECT object_id,item_id,count,enchant_level,loc,loc_data,custom_type1,custom_type2,mana_left,time,visual_item_id,agathion_energy FROM items WHERE object_id=? AND owner_id=? AND item_id=? AND count=? AND loc='AUCTION' AND loc_data=?", row.objectId(), row.ownerId(), row.itemId(), row.quantity(), row.id());
			ResultSet rs = ps.executeQuery())
		{
			return rs.next() ? Item.restoreFromDb(row.ownerId(), rs) : null;
		}
	}

	void verifyItem(int objectId, int ownerId, int itemId, long count, ItemLocation location, int locationData) throws SQLException
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = statement(con, "SELECT owner_id,item_id,count,loc,loc_data FROM items WHERE object_id=?", objectId);
			ResultSet rs = ps.executeQuery())
		{
			final boolean found = rs.next();
			if ((count == 0) ? found : (!found || (rs.getInt(1) != ownerId) || (rs.getInt(2) != itemId) || (rs.getLong(3) != count) || !location.name().equals(rs.getString(4)) || (rs.getInt(5) != locationData)))
			{
				throw new SQLException("Inventory persistence mismatch for object " + objectId);
			}
		}
	}

	void verifyAdena(int ownerId, long expected) throws SQLException
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = statement(con, "SELECT COALESCE(SUM(count),0) FROM items WHERE owner_id=? AND item_id=57 AND loc='INVENTORY'", ownerId);
			ResultSet rs = ps.executeQuery())
		{
			if (!rs.next() || (rs.getLong(1) != expected)) { throw new SQLException("Adena persistence mismatch for player " + ownerId); }
		}
	}

	void verifyMessage(int messageId, int receiverId) throws SQLException
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = statement(con, "SELECT receiverId,hasAttachments FROM messages WHERE messageId=?", messageId);
			ResultSet rs = ps.executeQuery())
		{
			if (!rs.next() || (rs.getInt(1) != receiverId) || !Boolean.parseBoolean(rs.getString(2))) { throw new SQLException("Mail persistence mismatch for message " + messageId); }
		}
	}

	private static PreparedStatement statement(Connection con, String sql, Object... values) throws SQLException
	{
		final PreparedStatement ps = con.prepareStatement(sql);
		try { bind(ps, values); return ps; }
		catch (SQLException | RuntimeException e) { ps.close(); throw e; }
	}

	private static void bind(PreparedStatement ps, Object... values) throws SQLException
	{
		for (int i = 0; i < values.length; i++) { ps.setObject(i + 1, values[i]); }
	}

	private static void change(Connection con, String sql, Object... values) throws SQLException
	{
		try (PreparedStatement ps = statement(con, sql, values))
		{
			if (ps.executeUpdate() != 1) { throw new SQLException("Auction reservation is no longer valid"); }
		}
	}
}
