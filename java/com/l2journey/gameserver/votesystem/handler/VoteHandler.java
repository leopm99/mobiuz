package com.l2journey.gameserver.votesystem.handler;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import com.l2journey.Config;
import com.l2journey.gameserver.votesystem.Enum.VoteSite;
import com.l2journey.gameserver.votesystem.model.IndividualVoteResponse;
import com.l2journey.gameserver.votesystem.util.VoteJson;
import com.l2journey.gameserver.votesystem.util.VoteSiteData;
import com.l2journey.gameserver.votesystem.util.VoteUtil;

/**
 * Provider adapters. HTTP and parse failures never reset counts or confirm a reward.
 * @author l2.topgameserver.net
 */
public class VoteHandler
{
	public static final Logger LOGGER = Logger.getLogger(VoteHandler.class.getName());
	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss").withResolverStyle(ResolverStyle.STRICT);
	
	private record VoteLink(long id, String url, long expires)
	{
	}
	
	private static final Map<String, VoteLink> LINKS = new ConcurrentHashMap<>();
	
	public static String encode(String value)
	{
		return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
	}
	
	private static boolean present(String... values)
	{
		for (String value : values)
		{
			if ((value == null) || value.isBlank())
			{
				return false;
			}
		}
		return true;
	}
	
	public static boolean isConfigured(int site)
	{
		return switch (site)
		{
			case 0 -> present(Config.VOTE_LINK_TGS, Config.TGS_API_KEY);
			case 1 -> present(Config.VOTE_LINK_ITOPZ, Config.ITOPZ_API_KEY, Config.ITOPZ_SRV_ID);
			case 2 -> present(Config.VOTE_LINK_TOP_CO, Config.TOP_CO_SRV_ID);
			case 3 -> present(Config.VOTE_LINK_VTS, Config.VTS_API_KEY);
			case 4 -> present(Config.VOTE_LINK_HZ, Config.HZ_API_KEY);
			case 5 -> present(Config.VOTE_NETWORK_LINK, Config.VOTE_NETWORK_API_KEY, Config.VOTE_NETWORK_USER_NAME);
			case 6 -> present(Config.VOTE_LINK_TSS, Config.TSS_API_TOKEN, Config.TS_SRV_ID, Config.TS_DOMAIN_NAME);
			case 7 -> present(Config.BRASIL_VOTE_LINK, Config.BRASIL_USER_NAME);
			case 8 -> present(Config.VOTE_LINK_MMOTOP, Config.MMOTOP_API_KEY);
			case 9 -> present(Config.VOTE_LINK_TZ, Config.TZ_API_KEY);
			case 10 -> present(Config.VOTE_LINK_SERVERS, Config.SERVERS_HASH_CODE, Config.SERVERS_SRV_ID);
			case 12 -> present(Config.HOPZONE_EU_API_KEY, Config.HOPZONE_EU_SERVER_ID);
			case 13 -> present(Config.HOTSERVERS_API_KEY, Config.HOTSERVERS_SERVER_ID);
			case 14 -> present(Config.L2RANKZONE_API_KEY);
			case 15 -> present(Config.TOP4TEAMBR_USER_NAME);
			default -> false;
		};
	}
	
	private static String request(String url, String body, int site)
	{
		HttpURLConnection connection = null;
		try
		{
			final URI uri = URI.create(url);
			if (!"https".equalsIgnoreCase(uri.getScheme()) && !"http".equalsIgnoreCase(uri.getScheme()))
			{
				return "";
			}
			connection = (HttpURLConnection) uri.toURL().openConnection();
			connection.setInstanceFollowRedirects(false);
			connection.setConnectTimeout(Math.clamp(Config.VOTE_CONNECT_TIMEOUT, 1000, 30000));
			connection.setReadTimeout(Math.clamp(Config.VOTE_READ_TIMEOUT, 1000, 30000));
			connection.setRequestProperty("User-Agent", "L2Journey Vote System");
			connection.setRequestProperty("Accept", "application/json, text/plain, text/html");
			if (body != null)
			{
				final byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
				connection.setRequestMethod("POST");
				connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
				connection.setDoOutput(true);
				connection.setFixedLengthStreamingMode(bytes.length);
				try (var output = connection.getOutputStream())
				{
					output.write(bytes);
				}
			}
			final int code = connection.getResponseCode();
			if (code != HttpURLConnection.HTTP_OK)
			{
				LOGGER.warning("Vote site " + site + " returned HTTP " + code + ".");
				return "";
			}
			try (var input = connection.getInputStream())
			{
				final byte[] bytes = input.readNBytes(1048577);
				if (bytes.length > 1048576)
				{
					throw new IOException("Response too large");
				}
				return new String(bytes, StandardCharsets.UTF_8).trim();
			}
		}
		catch (IOException | RuntimeException e)
		{
			// Do not log URLs, response bodies, credentials or player identifiers.
			LOGGER.warning("Vote request failed for site " + site + " (" + e.getClass().getSimpleName() + ").");
			return "";
		}
		finally
		{
			if (connection != null)
			{
				connection.disconnect();
			}
		}
	}
	
	protected static String getResponse(String url, int site)
	{
		return request(url, null, site);
	}
	
	protected static String getNetWorkResponse(String body, int site)
	{
		return request(Config.VOTE_NETWORK_LINK, body, site);
	}
	
	public static IndividualVoteResponse getIndividualVoteResponse(int site, String ip, String account)
	{
		if (!isConfigured(site) || !VoteSiteData.getInstance().isIndividualEnabled(site))
		{
			return null;
		}
		final String queryIp = present(Config.TEST_IP) && !"off".equalsIgnoreCase(Config.TEST_IP) ? Config.TEST_IP : ip;
		final String response;
		if (site == 5)
		{
			response = getNetWorkResponse("apiKey=" + encode(Config.VOTE_NETWORK_API_KEY) + "&type=2&player=" + encode(queryIp), site);
		}
		else
		{
			final VoteLink link = LINKS.get(ip + ":" + account);
			final String url = (site == 12) && (link != null) && (link.expires() > System.currentTimeMillis()) ? "https://api.hopzone.eu/v1/?api_key=" + encode(Config.HOPZONE_EU_API_KEY) + "&vote_id=" + link.id() + "&type=json" : getIndividualUrl(site, queryIp, account);
			response = getResponse(url, site);
		}
		try
		{
			return parseIndividual(site, response, System.currentTimeMillis());
		}
		catch (RuntimeException e)
		{
			LOGGER.warning("Invalid individual vote response for site " + site + ".");
			return null;
		}
	}
	
	/**
	 * Public for fixture-based verification; performs no network or database operations.
	 * @param site
	 * @param response
	 * @param now
	 * @return
	 */
	public static IndividualVoteResponse parseIndividual(int site, String response, long now)
	{
		if ((VoteSite.fromId(site) == null) || (response == null) || response.isBlank())
		{
			return null;
		}
		final Object data = VoteJson.parse(response);
		final String error = VoteJson.text(VoteJson.field(data, "error"));
		if (!error.isEmpty() && !"OK".equalsIgnoreCase(error) && !"NONE".equalsIgnoreCase(error) && !"false".equalsIgnoreCase(error))
		{
			throw new IllegalArgumentException("Provider error");
		}
		final IndividualVoteResponse result = new IndividualVoteResponse();
		final Object status = switch (site)
		{
			case 0 -> VoteJson.field(data, "already_voted");
			case 1, 9 -> VoteJson.field(data, "isVoted");
			case 3, 7, 12 -> VoteJson.field(data, "status");
			case 8 -> VoteJson.field(data, "is_voted");
			case 13 -> VoteJson.field(data, "has_voted");
			case 14 -> VoteJson.field(data, "voted");
			case 2, 5, 10 -> data;
			default -> VoteJson.field(data, "voted");
		};
		if (!VoteJson.confirmed(status))
		{
			return result;
		}
		final Object time = VoteJson.field(data, "vote_time", "voteTime", "vote_date", "date");
		final Object server = VoteJson.field(data, "server_time", "serverTime", "hopzoneServerTime");
		final long voteTime;
		final long difference;
		if ((site == 2) || (site == 5) || (site == 10))
		{
			// These legacy providers return only a boolean, without a timestamp.
			voteTime = now;
			difference = 0;
		}
		else
		{
			voteTime = timestamp(time, site);
			difference = server == null ? 0 : Math.subtractExact(now, timestamp(server, site));
		}
		if ((voteTime <= 0) || (Math.addExact(voteTime, difference) > (now + 60000)))
		{
			throw new IllegalArgumentException("Invalid vote time");
		}
		result.setIsVoted(true);
		result.setVoteSiteTime(voteTime);
		result.setDiffTime(difference);
		return result;
	}
	
	private static long timestamp(Object value, int site)
	{
		final String text = VoteJson.text(value);
		if (text.matches("[0-9]+"))
		{
			final long number = Long.parseLong(text);
			return number < 100000000000L ? Math.multiplyExact(number, 1000) : number;
		}
		try
		{
			return Instant.parse(text).toEpochMilli();
		}
		catch (java.time.DateTimeException e)
		{
			return LocalDateTime.parse(text, DATE).atZone(VoteSiteData.getInstance().getSettings(site).zone()).toInstant().toEpochMilli();
		}
	}
	
	public int getGlobalVotesResponse(int site)
	{
		if (!isConfigured(site) || !VoteSiteData.getInstance().isGlobalEnabled(site))
		{
			return -1;
		}
		final String response = site == 5 ? getNetWorkResponse(getGlobalUrl(site), site) : getResponse(getGlobalUrl(site), site);
		try
		{
			return parseGlobal(site, response);
		}
		catch (RuntimeException e)
		{
			LOGGER.warning("Invalid global vote response for site " + site + ".");
			return -1;
		}
	}
	
	public static int parseGlobal(int site, String response)
	{
		if ((VoteSite.fromId(site) == null) || (response == null) || response.isBlank())
		{
			return -1;
		}
		if ((site == 6) && response.startsWith("<"))
		{
			response = VoteUtil.between("VOTE <span>", response, "</span>").trim();
		}
		final Object data = VoteJson.parse(response);
		final String error = VoteJson.text(VoteJson.field(data, "error"));
		if (!error.isEmpty() && !"OK".equalsIgnoreCase(error) && !"NONE".equalsIgnoreCase(error) && !"false".equalsIgnoreCase(error))
		{
			throw new IllegalArgumentException("Provider error");
		}
		final Object count = switch (site)
		{
			case 0 -> VoteJson.field(data, "getVotes");
			case 1 -> VoteJson.field(data, "server_votes");
			case 15 -> VoteJson.field(data, "votes", "server_votes");
			case 3 -> VoteJson.field(data, "votes");
			case 4 -> VoteJson.field(data, "totalvotes");
			case 7 -> VoteJson.field(data, "votes", "totalvotes");
			case 8 -> VoteJson.field(data, "monthly_votes");
			case 9 -> VoteJson.field(data, "totalVotes");
			case 12 -> VoteJson.field(data, "total_monthly_votes");
			case 13 -> VoteJson.field(data, "votes");
			case 14 -> VoteJson.field(data, "currentVotes");
			default -> data;
		};
		final int total = Math.toIntExact(VoteJson.integer(count));
		return total < 0 ? -1 : total;
	}
	
	/**
	 * Generates a private link; never accepts a player-supplied vote ID.
	 * @param ip
	 * @param account
	 * @return
	 */
	public static String createHopzoneLink(String ip, String account)
	{
		if (!isConfigured(12) || !VoteSiteData.getInstance().isIndividualEnabled(12))
		{
			return "";
		}
		final long now = System.currentTimeMillis();
		LINKS.entrySet().removeIf(entry -> entry.getValue().expires() <= now);
		final VoteLink existing = LINKS.get(ip + ":" + account);
		if (existing != null)
		{
			return existing.url();
		}
		if (LINKS.size() >= 10000)
		{
			return "";
		}
		try
		{
			final Object response = VoteJson.parse(getResponse("https://api.hopzone.eu/v1/?api_key=" + encode(Config.HOPZONE_EU_API_KEY) + "&generate=" + encode(Config.HOPZONE_EU_SERVER_ID) + "&type=json", 12));
			final long id = VoteJson.integer(VoteJson.field(response, "vote_id"));
			final URI url = URI.create(VoteJson.text(VoteJson.field(response, "url")));
			if ((id <= 0) || !"https".equals(url.getScheme()) || !"hopzone.eu".equalsIgnoreCase(url.getHost()) || (url.getUserInfo() != null) || ((url.getPort() != -1) && (url.getPort() != 443)) || !url.getPath().equals("/vote/" + Config.HOPZONE_EU_SERVER_ID + "/" + id))
			{
				return "";
			}
			final VoteLink created = new VoteLink(id, url.toASCIIString(), now + 43200000L);
			final VoteLink previous = LINKS.putIfAbsent(ip + ":" + account, created);
			return previous == null ? created.url() : previous.url();
		}
		catch (RuntimeException e)
		{
			LOGGER.warning("Could not generate Hopzone.eu vote link.");
			return "";
		}
	}
	
	public static String getIndividualUrl(int site, String ip, String account)
	{
		ip = encode(present(Config.TEST_IP) && !"off".equalsIgnoreCase(Config.TEST_IP) ? Config.TEST_IP : ip);
		return switch (site)
		{
			case 0 -> Config.VOTE_LINK_TGS + "API_KEY=" + encode(Config.TGS_API_KEY) + "/getData/" + ip;
			case 1 -> Config.VOTE_LINK_ITOPZ + encode(Config.ITOPZ_API_KEY) + "/" + encode(Config.ITOPZ_SRV_ID) + "/" + ip;
			case 2 -> Config.VOTE_LINK_TOP_CO + "VoteCheck.php?id=" + encode(Config.TOP_CO_SRV_ID) + "&ip=" + ip;
			case 3 -> Config.VOTE_LINK_VTS + "api.php?apiKey=" + encode(Config.VTS_API_KEY) + "&ip=" + ip;
			case 4 -> Config.VOTE_LINK_HZ + "vote?token=" + encode(Config.HZ_API_KEY) + "&ip_address=" + ip;
			case 5 -> Config.VOTE_NETWORK_LINK;
			case 6 -> Config.VOTE_LINK_TSS + "token=" + encode(Config.TSS_API_TOKEN) + "&ip=" + ip;
			case 7 -> Config.BRASIL_VOTE_LINK + "username=" + encode(Config.BRASIL_USER_NAME) + "&ip=" + ip + "&type=json";
			case 8 -> Config.VOTE_LINK_MMOTOP + encode(Config.MMOTOP_API_KEY) + "/ip/" + ip;
			case 9 -> Config.VOTE_LINK_TZ + "vote?token=" + encode(Config.TZ_API_KEY) + "&ip=" + ip;
			case 10 -> Config.VOTE_LINK_SERVERS + "checkip.php?hash=" + encode(Config.SERVERS_HASH_CODE) + "&server_id=" + encode(Config.SERVERS_SRV_ID) + "&ip=" + ip;
			case 12 -> "https://api.hopzone.eu/v1/?api_key=" + encode(Config.HOPZONE_EU_API_KEY) + "&ip=" + ip + "&type=json";
			case 13 -> "https://hotservers.org/api/servers/" + encode(Config.HOTSERVERS_SERVER_ID) + "/voteCheck?api_token=" + encode(Config.HOTSERVERS_API_KEY) + "&ip_address=" + ip;
			case 14 -> "https://l2rankzone.com/api/vote-reward?apiKey=" + encode(Config.L2RANKZONE_API_KEY) + "&ip=" + ip;
			case 15 -> "https://top.4teambr.com/api.php?name=" + encode(Config.TOP4TEAMBR_USER_NAME) + "&ip=" + ip;
			default -> "";
		};
	}
	
	public String getGlobalUrl(int site)
	{
		return switch (site)
		{
			case 0 -> Config.VOTE_LINK_TGS + "API_KEY=" + encode(Config.TGS_API_KEY) + "/getData/";
			case 1 -> Config.VOTE_LINK_ITOPZ + encode(Config.ITOPZ_API_KEY) + "/" + encode(Config.ITOPZ_SRV_ID) + "/";
			case 2 -> Config.VOTE_LINK_TOP_CO + "VoteCheck_Total.php?id=" + encode(Config.TOP_CO_SRV_ID);
			case 3 -> Config.VOTE_LINK_VTS + "api.php?apiKey=" + encode(Config.VTS_API_KEY);
			case 4 -> Config.VOTE_LINK_HZ + "votes?token=" + encode(Config.HZ_API_KEY);
			case 5 -> "apiKey=" + encode(Config.VOTE_NETWORK_API_KEY) + "&type=1";
			case 6 -> "https://l2topservers.com/l2top/" + encode(Config.TS_SRV_ID) + "/" + encode(Config.TS_DOMAIN_NAME);
			case 7 -> Config.BRASIL_VOTE_LINK + "username=" + encode(Config.BRASIL_USER_NAME) + "&type=json";
			case 8 -> Config.VOTE_LINK_MMOTOP + encode(Config.MMOTOP_API_KEY) + "/info/";
			case 9 -> Config.VOTE_LINK_TZ + "server_" + encode(Config.TZ_API_KEY) + "/getServerData";
			case 10 -> Config.VOTE_LINK_SERVERS + "yearlyvotes.php?server_id=" + encode(Config.SERVERS_SRV_ID);
			case 12 -> "https://api.hopzone.eu/v1/?api_key=" + encode(Config.HOPZONE_EU_API_KEY) + "&server_id=" + encode(Config.HOPZONE_EU_SERVER_ID) + "&type=json";
			case 13 -> "https://hotservers.org/api/servers/" + encode(Config.HOTSERVERS_SERVER_ID) + "/votes?api_token=" + encode(Config.HOTSERVERS_API_KEY);
			case 14 -> "https://l2rankzone.com/api/server-stats?apiKey=" + encode(Config.L2RANKZONE_API_KEY);
			case 15 -> "https://top.4teambr.com/api.php?name=" + encode(Config.TOP4TEAMBR_USER_NAME);
			default -> "";
		};
	}
}
