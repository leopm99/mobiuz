package com.l2journey.gameserver.donation.util;
public final class DonationStrings
{
 public static boolean isValidString(String value, String pattern) { return value != null && value.matches(pattern); }
 public static String trimAndDress(String value, int limit) { return value.length() > limit ? value.substring(0, limit) + "..." : value; }
}
