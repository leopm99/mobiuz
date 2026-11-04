package com.l2journey.gameserver.auctionhouse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.sql.*;
import org.junit.jupiter.api.Test;
import com.l2journey.commons.database.DatabaseFactory;
import com.l2journey.gameserver.auctionhouse.AuctionHouseStore.Reservation;
import com.l2journey.gameserver.model.item.instance.Item;

class AuctionHouseStoreTest
{
	@Test void reservationAndJournalCommitTogetherOrRollBack() throws Exception
	{
		try(var database=mockStatic(DatabaseFactory.class))
		{
			Connection con=mock(Connection.class); database.when(DatabaseFactory::getConnection).thenReturn(con);
			PreparedStatement ps=mock(PreparedStatement.class); when(con.prepareStatement(anyString())).thenReturn(ps); when(ps.executeUpdate()).thenReturn(1);
			Item source=mock(Item.class); when(source.getObjectId()).thenReturn(100); when(source.getCount()).thenReturn(40L);
			AuctionHouseStore store=new AuctionHouseStore(); var op=store.reserve(1,"BUY",20,source,5,500,10000);
			assertEquals(1,op.listingId()); verify(con).setAutoCommit(false); verify(con).commit();
			reset(con); when(con.prepareStatement(anyString())).thenReturn(ps); when(ps.executeUpdate()).thenReturn(1,0);
			assertThrows(SQLException.class,() -> store.reserve(1,"BUY",20,source,5,500,10000));
			verify(con).rollback(); verify(con,never()).commit();
		}
	}
	@Test void finalListingAndJournalCannotCommitIndependently() throws Exception
	{
		try(var database=mockStatic(DatabaseFactory.class))
		{
			Connection con=mock(Connection.class); database.when(DatabaseFactory::getConnection).thenReturn(con);
			PreparedStatement ps=mock(PreparedStatement.class); when(con.prepareStatement(anyString())).thenReturn(ps); when(ps.executeUpdate()).thenReturn(1,0);
			assertThrows(SQLException.class,() -> new AuctionHouseStore().finish(new Reservation(1,"op"),100,5,"ACTIVE"));
			verify(con).rollback(); verify(con,never()).commit();
		}
	}
	@Test void staleReservationDoesNotWriteJournal() throws Exception
	{
		try(var database=mockStatic(DatabaseFactory.class))
		{
			Connection con=mock(Connection.class); database.when(DatabaseFactory::getConnection).thenReturn(con);
			PreparedStatement ps=mock(PreparedStatement.class); when(con.prepareStatement(anyString())).thenReturn(ps); when(ps.executeUpdate()).thenReturn(0);
			assertThrows(SQLException.class,() -> new AuctionHouseStore().reserve(1,"BUY",20,mock(Item.class),1,100,1000));
			verify(con,times(1)).prepareStatement(anyString()); verify(con).rollback();
		}
	}
}
