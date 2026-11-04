package com.l2journey.gameserver.donation.util;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Local history renderer; no changes to the server's shared HTML machinery.
 * @param <T>
 */
public final class Pagination<T> implements Iterable<T>
{
	private final List<T> entries;
	private final int page, size;
	private final StringBuilder html = new StringBuilder();
	
	public Pagination(Stream<T> source, int requested, int size, Predicate<T> filter, Comparator<T> order)
	{
		entries = source.filter(filter).sorted(order).toList();
		this.size = size;
		page = Math.max(1, Math.min(requested, Math.max(1, ((entries.size() + size) - 1) / size)));
	}
	
	@Override
	public Iterator<T> iterator()
	{
		return entries.subList((page - 1) * size, Math.min(page * size, entries.size())).iterator();
	}
	
	public void append(Object... values)
	{
		for (Object value : values)
		{
			html.append(value);
		}
	}
	
	public boolean isEmpty()
	{
		return entries.isEmpty();
	}
	
	public int getTotalEntries()
	{
		return entries.size();
	}
	
	public String getContent()
	{
		return html.toString();
	}
	
	public void generatePages(String action)
	{
		final int first = Math.max(1, page - 4);
		final int last = Math.min(((entries.size() + size) - 1) / size, page + 4);
		html.append("<br><center><table width=").append((last - first + 1) * 28).append(" cellpadding=0 cellspacing=0><tr>");
		for (int i = first; i <= last; i++)
		{
			html.append("<td width=28 align=center><a action=\"").append(action.replace("%page%", Integer.toString(i))).append("\">").append(i).append("</a></td>");
		}
		html.append("</tr></table></center>");
	}
}
