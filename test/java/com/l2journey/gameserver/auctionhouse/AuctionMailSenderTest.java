package com.l2journey.gameserver.auctionhouse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.junit.jupiter.api.Test;
import com.l2journey.gameserver.model.Message;
import com.l2journey.gameserver.network.enums.MessageSenderType;

class AuctionMailSenderTest
{
	@Test void customSenderSurvivesRestoreAndRemainsSystemMailOnServer() throws Exception
	{
		ResultSet row=mock(ResultSet.class);
		when(row.getInt("sendBySystem")).thenReturn(MessageSenderType.AUCTION_MANAGER.ordinal());
		Message message=new Message(row);
		assertEquals("Auction Manager",message.getSenderName());
		assertNotEquals(0,message.getSendBySystem(),"System attachments must not become rejectable player mail");
		assertEquals(MessageSenderType.PLAYER.ordinal(),message.getClientSenderType(),"Send a supported client type with the explicit sender name");
		PreparedStatement ps=mock(PreparedStatement.class);
		Message.applyInsertParameters(message,ps);
		verify(ps).setString(12,Integer.toString(MessageSenderType.AUCTION_MANAGER.ordinal()));
	}
	@Test void existingSenderTypesKeepTheirWireValues() throws Exception
	{
		assertEquals(0,MessageSenderType.PLAYER.ordinal()); assertEquals(1,MessageSenderType.NEWS.ordinal());
		assertEquals(2,MessageSenderType.NONE.ordinal()); assertEquals(3,MessageSenderType.ALEGRIA.ordinal());
		for(MessageSenderType type:new MessageSenderType[]{MessageSenderType.PLAYER,MessageSenderType.NEWS,MessageSenderType.NONE,MessageSenderType.ALEGRIA})
		{
			ResultSet row=mock(ResultSet.class); when(row.getInt("sendBySystem")).thenReturn(type.ordinal());
			Message message=new Message(row);
			assertEquals(type.ordinal(),message.getSendBySystem()); assertEquals(type.ordinal(),message.getClientSenderType());
		}
	}
}
