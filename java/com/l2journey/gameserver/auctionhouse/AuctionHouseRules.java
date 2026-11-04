package com.l2journey.gameserver.auctionhouse;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Shared validation for untrusted board parameters and monetary arithmetic. */
public final class AuctionHouseRules
{
	public static final long MAX_QUANTITY = 1_000_000;
	public static final long MAX_PRICE = 10_000_000_000L;
	private AuctionHouseRules() { }

	public static long total(long quantity, long unitPrice)
	{
		if ((quantity < 1) || (quantity > MAX_QUANTITY) || (unitPrice < 1) || (unitPrice > MAX_PRICE))
		{
			throw new IllegalArgumentException("Invalid quantity or unit price.");
		}
		return Math.multiplyExact(quantity, unitPrice);
	}

	public static long fee(long quantity, long price, int days, double percent, long minimum)
	{
		if ((days < 1) || (days > 9) || !Double.isFinite(percent) || (percent < 0) || (percent > 100) || (minimum < 0))
		{
			throw new IllegalArgumentException("Invalid listing fee or duration.");
		}
		return Math.max(minimum, BigDecimal.valueOf(total(quantity, price)).multiply(BigDecimal.valueOf(days))
			.multiply(BigDecimal.valueOf(percent)).movePointLeft(2).setScale(0, RoundingMode.CEILING).longValueExact());
	}

	public static String search(String value)
	{
		final String text = value == null ? "" : value.trim();
		if ((text.length() > 32) || !text.matches("[a-zA-Z0-9 +\\-]*"))
		{
			throw new IllegalArgumentException("Use up to 32 letters, numbers, spaces, + or - in searches.");
		}
		return text;
	}
}
