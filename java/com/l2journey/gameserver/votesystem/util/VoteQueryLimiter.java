package com.l2journey.gameserver.votesystem.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Shared IP/site limiter for both NPC and voiced entry points. */
public final class VoteQueryLimiter
{
	private record Key(String ip, int site) { }
	private final Map<Key, Long> _expires = new HashMap<>();
	private final Set<Key> _running = new HashSet<>();

	public synchronized boolean acquire(String ip, int site, long now, long cooldown)
	{
		_expires.entrySet().removeIf(entry -> entry.getValue() <= now && !_running.contains(entry.getKey()));
		final Key key = new Key(ip, site);
		if (_running.contains(key) || _expires.getOrDefault(key, 0L) > now || _expires.size() >= 10000) { return false; }
		_running.add(key);
		_expires.put(key, now + Math.max(1000, cooldown));
		return true;
	}
	public synchronized void release(String ip, int site) { _running.remove(new Key(ip, site)); }
}
