package com.l2journey.gameserver.votesystem.handler;

import static org.junit.jupiter.api.Assertions.*;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import com.sun.net.httpserver.HttpServer;
import com.l2journey.gameserver.votesystem.Enum.VoteSite;
import com.l2journey.gameserver.votesystem.util.VoteJson;
import com.l2journey.gameserver.votesystem.util.VoteQueryLimiter;

class VoteProviderTest
{
	@Test
	void stableIdsPreserveLegacySitesAndReserveGlobalEntry()
	{
		assertEquals(10, VoteSite.L2SERVERS.getId());
		assertNull(VoteSite.fromId(11));
		assertEquals(VoteSite.HOPZONE_EU, VoteSite.fromId(12));
		assertEquals(VoteSite.TOP4TEAMBR, VoteSite.fromId(15));
	}

	@Test
	void jsonHandlesOrderWhitespaceWrappersAndEscapedDelimiters()
	{
		final Object json = VoteJson.parse("{\"message\":\"commas, colons: and \\\"quotes\\\"\",\"data\":{\"server_votes\":23}}");
		assertEquals(23, VoteJson.integer(VoteJson.field(json, "server_votes")));
		assertEquals("commas, colons: and \"quotes\"", VoteJson.field(json, "message"));
		assertEquals("A", VoteJson.parse("\"\\u0041\""));
	}

	@Test
	void malformedJsonCannotProduceVotes()
	{
		for (String text : new String[] { "{\"votes\":1,}", "{\"votes\":1,\"votes\":9}", "01", "true junk", "{\"votes\":NaN}", "{\"a\":", "[1,]" })
		{
			assertThrows(IllegalArgumentException.class, () -> VoteJson.parse(text), text);
		}
		assertThrows(IllegalArgumentException.class, () -> VoteJson.parse("[".repeat(40) + "0" + "]".repeat(40)));
	}

	@Test
	void globalFailuresStayDistinctFromLegitimateZero()
	{
		assertEquals(-1, VoteHandler.parseGlobal(12, ""));
		assertEquals(0, VoteHandler.parseGlobal(12, "{\"total_monthly_votes\":0}"));
		assertThrows(RuntimeException.class, () -> VoteHandler.parseGlobal(12, "{\"error\":\"INVALID_KEY\",\"total_monthly_votes\":0}"));
		assertThrows(RuntimeException.class, () -> VoteHandler.parseGlobal(12, "{\"error\":\"OK\"}"));
		assertEquals(-1, VoteHandler.parseGlobal(5, "-1"));
		assertThrows(RuntimeException.class, () -> VoteHandler.parseGlobal(12, "{\"total_monthly_votes\":2147483648}"));
		assertThrows(RuntimeException.class, () -> VoteHandler.parseGlobal(12, "{\"total_monthly_votes\":1.5}"));
	}

	@Test
	void globalAdaptersReadNewAndLegacyProviderFields()
	{
		assertEquals(7, VoteHandler.parseGlobal(7, "{\"votes\":\"7\"}"));
		assertEquals(8, VoteHandler.parseGlobal(13, "{\"votes\":8}"));
		assertEquals(9, VoteHandler.parseGlobal(14, "{\"currentVotes\":9}"));
		assertEquals(10, VoteHandler.parseGlobal(15, "{\"votes\":10}"));
		assertEquals(11, VoteHandler.parseGlobal(9, "{\"result\":{\"totalVotes\":\"11\"}}"));
		assertEquals(12, VoteHandler.parseGlobal(6, "<html>VOTE <span>12</span></html>"));
	}

	@Test
	void hopzonePendingIsNeverRewardedAndCompletedUsesProviderClock()
	{
		final long now = 1700000000000L;
		assertFalse(VoteHandler.parseIndividual(12, "{\"status\":\"pending\"}", now).getIsVoted());
		final var vote = VoteHandler.parseIndividual(12, "{\"status\":\"completed\",\"server_time\":1699999900,\"vote_time\":1699999800}", now);
		assertTrue(vote.getIsVoted());
		assertEquals(1699999800000L, vote.getVoteSiteTime());
		assertEquals(100000L, vote.getDiffTime());
		assertThrows(RuntimeException.class, () -> VoteHandler.parseIndividual(12, "{\"status\":\"completed\"}", now));
		assertThrows(RuntimeException.class, () -> VoteHandler.parseIndividual(12, "{\"status\":\"completed\",\"vote_time\":1700001000}", now));
	}

	@Test
	void epochValuesSupportNestedCaseInsensitiveResponses()
	{
		final long now = 1700000000000L;
		assertTrue(VoteHandler.parseIndividual(1, "{\"isVoted\":true,\"voteTime\":1699999900,\"serverTime\":1700000000}", now).getIsVoted());
		assertTrue(VoteHandler.parseIndividual(14, "{\"data\":{\"voted\":true,\"voteTime\":1699999900000,\"serverTime\":1700000000000}}", now).getIsVoted());
		assertFalse(VoteHandler.parseIndividual(14, "{\"voted\":false}", now).getIsVoted());
		assertNull(VoteHandler.parseIndividual(11, "true", now));
	}

	@Test
	void legacyBooleanProvidersRequireExplicitConfirmation()
	{
		assertTrue(VoteHandler.parseIndividual(5, "1", 1700000000000L).getIsVoted());
		assertFalse(VoteHandler.parseIndividual(5, "0", 1700000000000L).getIsVoted());
		assertTrue(VoteHandler.parseIndividual(2, "true", 1700000000000L).getIsVoted());
		assertFalse(VoteHandler.parseIndividual(2, "false", 1700000000000L).getIsVoted());
	}

	@Test
	void queryLimiterIsSharedByIpAndSiteAndKeepsRunningRequestsBlocked()
	{
		final VoteQueryLimiter limiter = new VoteQueryLimiter();
		assertTrue(limiter.acquire("ip", 0, 0, 15000));
		assertFalse(limiter.acquire("ip", 0, 20000, 15000));
		assertTrue(limiter.acquire("ip", 1, 1000, 15000));
		assertTrue(limiter.acquire("another", 0, 1000, 15000));
		limiter.release("ip", 0);
		assertFalse(limiter.acquire("ip", 0, 10000, 15000));
		assertTrue(limiter.acquire("ip", 0, 20000, 15000));
	}

	@Test
	void requestsUseRealStatusCodesAndDoNotFollowRedirects() throws Exception
	{
		final HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/ok", exchange ->
		{
			final byte[] response = "{\"votes\":3}".getBytes(StandardCharsets.UTF_8);
			exchange.sendResponseHeaders(200, response.length);
			try (var out = exchange.getResponseBody()) { out.write(response); }
		});
		server.createContext("/redirect", exchange ->
		{
			exchange.getResponseHeaders().set("Location", "/ok");
			exchange.sendResponseHeaders(302, -1); exchange.close();
		});
		server.createContext("/limited", exchange -> { exchange.sendResponseHeaders(429, -1); exchange.close(); });
		server.start();
		try
		{
			final String base = "http://127.0.0.1:" + server.getAddress().getPort();
			assertEquals("{\"votes\":3}", VoteHandler.getResponse(base + "/ok", 0));
			assertEquals("", VoteHandler.getResponse(base + "/redirect", 0));
			assertEquals("", VoteHandler.getResponse(base + "/limited", 0));
			assertEquals("a%26b%3Dc%2F", VoteHandler.encode("a&b=c/"));
		}
		finally { server.stop(0); }
	}

	@Test
	void dateResponsesUse24HourClockAndTheConfiguredTimeZone()
	{
		try (var singleton = org.mockito.Mockito.mockStatic(com.l2journey.gameserver.votesystem.util.VoteSiteData.class))
		{
			final var settings = org.mockito.Mockito.mock(com.l2journey.gameserver.votesystem.util.VoteSiteData.class);
			singleton.when(com.l2journey.gameserver.votesystem.util.VoteSiteData::getInstance).thenReturn(settings);
			org.mockito.Mockito.when(settings.getSettings(org.mockito.ArgumentMatchers.anyInt())).thenReturn(new com.l2journey.gameserver.votesystem.util.VoteSiteData.Settings(true, true, 0, 0, java.time.ZoneId.of("America/Sao_Paulo")));
			final long now = Instant.parse("2026-10-24T23:00:00Z").toEpochMilli();
			final var vote = VoteHandler.parseIndividual(15, "{\"voted\":true,\"vote_date\":\"2026-10-24 19:30:00\",\"server_time\":\"2026-10-24 20:00:00\"}", now);
			assertTrue(vote.getIsVoted());
			assertEquals(Instant.parse("2026-10-24T22:30:00Z").toEpochMilli(), vote.getVoteSiteTime());
			assertEquals(0, vote.getDiffTime());
			assertThrows(RuntimeException.class, () -> VoteHandler.parseIndividual(15, "{\"voted\":true,\"vote_date\":\"2026-02-30 19:30:00\"}", now));
		}
	}

	@Test
	void generatedVoteIdIsBoundToAccountAndReusedOnlyForItsOwner()
	{
		final String previousKey = com.l2journey.Config.HOPZONE_EU_API_KEY;
		final String previousId = com.l2journey.Config.HOPZONE_EU_SERVER_ID;
		com.l2journey.Config.HOPZONE_EU_API_KEY = "fixture-key";
		com.l2journey.Config.HOPZONE_EU_SERVER_ID = "9";
		try (var singleton = org.mockito.Mockito.mockStatic(com.l2journey.gameserver.votesystem.util.VoteSiteData.class);
			 var provider = org.mockito.Mockito.mockStatic(VoteHandler.class, org.mockito.Mockito.CALLS_REAL_METHODS))
		{
			final var data = org.mockito.Mockito.mock(com.l2journey.gameserver.votesystem.util.VoteSiteData.class);
			singleton.when(com.l2journey.gameserver.votesystem.util.VoteSiteData::getInstance).thenReturn(data);
			org.mockito.Mockito.when(data.isIndividualEnabled(12)).thenReturn(true);
			provider.when(() -> VoteHandler.getResponse(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.eq(12))).thenAnswer(call ->
			{
				final String url = call.getArgument(0);
				return url.contains("generate=")
					? "{\"vote_id\":123,\"url\":\"https://hopzone.eu/vote/9/123\"}"
					: "{\"status\":\"pending\"}";
			});
			final String ip = "192.0.2.222";
			assertEquals("https://hopzone.eu/vote/9/123", VoteHandler.createHopzoneLink(ip, "fixture-owner"));
			assertFalse(VoteHandler.getIndividualVoteResponse(12, ip, "fixture-owner").getIsVoted());
			provider.verify(() -> VoteHandler.getResponse(org.mockito.ArgumentMatchers.contains("&vote_id=123"), org.mockito.ArgumentMatchers.eq(12)));
			assertFalse(VoteHandler.getIndividualVoteResponse(12, ip, "fixture-other").getIsVoted());
			provider.verify(() -> VoteHandler.getResponse(org.mockito.ArgumentMatchers.contains("&ip=192.0.2.222"), org.mockito.ArgumentMatchers.eq(12)));
		}
		finally
		{
			com.l2journey.Config.HOPZONE_EU_API_KEY = previousKey;
			com.l2journey.Config.HOPZONE_EU_SERVER_ID = previousId;
		}
	}
}
