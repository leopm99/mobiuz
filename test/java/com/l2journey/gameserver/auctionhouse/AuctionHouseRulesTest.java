package com.l2journey.gameserver.auctionhouse;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class AuctionHouseRulesTest
{
	@Test void percentageIsNotTruncatedAndIsChargedPerDay()
	{
		assertEquals(150000, AuctionHouseRules.fee(100, 100000, 3, 0.5, 10000));
		assertEquals(10000, AuctionHouseRules.fee(1, 100, 1, 0.5, 10000));
		assertEquals(1, AuctionHouseRules.fee(1, 1, 1, 0.5, 0));
		assertEquals(0, AuctionHouseRules.fee(1, 1, 1, 0, 0));
	}
	@Test void invalidCountsPricesAndDurationAreRejected()
	{
		for (long n : new long[] {-1, 0, Long.MAX_VALUE})
		{
			assertThrows(IllegalArgumentException.class, () -> AuctionHouseRules.total(n, 1));
			assertThrows(IllegalArgumentException.class, () -> AuctionHouseRules.total(1, n));
		}
		for (int days : new int[] {-1, 0, 10, Integer.MAX_VALUE})
		{
			assertThrows(IllegalArgumentException.class, () -> AuctionHouseRules.fee(1, 1, days, 0.5, 10000));
		}
		for (double fee : new double[] {-1, Double.NaN, Double.POSITIVE_INFINITY, 101})
		{
			assertThrows(IllegalArgumentException.class, () -> AuctionHouseRules.fee(1, 1, 1, fee, 0));
		}
	}
	@Test void limitsUseLongArithmetic()
	{
		assertEquals(10_000_000_000_000_000L, AuctionHouseRules.total(1_000_000, 10_000_000_000L));
		assertEquals(90_000_000_000_000_000L, AuctionHouseRules.fee(1_000_000, 10_000_000_000L, 9, 100, 0));
	}
	@Test void searchCannotInjectMarkupOrBypassParameters()
	{
		assertEquals("Earth Crystal", AuctionHouseRules.search(" Earth Crystal "));
		assertEquals("+4 Sword-1", AuctionHouseRules.search("+4 Sword-1"));
		for (String value : new String[] {"a;b", "<html>", "\"", "$quantity", "x".repeat(33), "a\nb", "a\\b"})
		{
			assertThrows(IllegalArgumentException.class, () -> AuctionHouseRules.search(value));
		}
	}
}
