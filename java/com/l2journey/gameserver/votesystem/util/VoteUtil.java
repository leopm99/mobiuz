package com.l2journey.gameserver.votesystem.util;

/** Small helpers for the remaining legacy text response. */
public final class VoteUtil
{
	private VoteUtil() { }
	public static String between(String start, String text, String end)
	{
		final int first = text.indexOf(start);
		if (first < 0) { return ""; }
		final int from = first + start.length();
		final int last = text.indexOf(end, from);
		return last < 0 ? "" : text.substring(from, last);
	}
}
