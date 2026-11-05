package com.l2journey.gameserver.model.phantom;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.l2journey.commons.threads.ThreadPool;
import com.l2journey.commons.util.Rnd;
import com.l2journey.gameserver.model.World;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.network.enums.ChatType;
import com.l2journey.gameserver.network.serverpackets.CreatureSay;
import com.l2journey.gameserver.util.LocationUtil;

public class PhantomChat
{
	private static final Map<Integer, Long> LAST_TOWN_CHAT_AT = new ConcurrentHashMap<>();
	private static final long TOWN_CHAT_COOLDOWN_MS = 45000L;
	private static final int TOWN_CHAT_XY = 800;
	private static final int TOWN_CHAT_Z = 300;

	private static final String[] GREETINGS =
	{
		"hola",
		"que tal",
		"buenas",
		"ey",
		"como va"
	};
	private static final String[] TOWN_REPLIES =
	{
		"todo bien",
		"aqui ando",
		"que tal",
		"buenas",
		"ey"
	};
	// Mensagens que dizem quando morrem
	public static final String[] DEATH_MESSAGES =
	{
		"lag...",
		"wtf de donde salio ese",
		"omg mi equipo",
		"rez pls",
		"gg",
		"la proxima te mato",
		"altos petes"
	};
	
	// Mensagens casuais que dizem ao ar enquanto farmam ou caminham
	private static final String[] CASUAL_MESSAGES =
	{
		"alguien para rb?",
		"vendo mats Pm",
		"necesito buff",
		"cuanta adena cae aca...",
		"que aburrido",
		"alguien clan?",
		"esta muy bueno el server",
		"compro ss gr C, pm",
		"compro ss B gr, pm",
		"compro ss grado A, pm",
		"Sale Party?? GOgo",
		"altos petes"
	};
	// Mensagens que usam para RESPONDER quando um jogador real fala com eles (por geral ou PM)
	private static final String[] RESPONSES =
	{
		"jaja mal",
		"no me molestes toy farmeando",
		"se",
		"tu mama en tanga",
		"na",
		"despues te digo",
		"tu papa en zunga",
		"?",
		"toy afk, no jodas",
		"xd",
		"xD",
		"buen drop por aca",
		"voy a probar otro spot despues",
		"estos mobs rinden bastante",
		"me falta un poco para subir",
		"ando juntando herbs para no parar",
		"si cae algo bueno aviso",
		"esta zona esta tranquila",
		"necesito mejorar el arma pronto"
	};
	private PhantomChat()
	{
	}

	public static void botReply(Player bot, Player sender, String text, boolean isPrivate)
	{
		if (isPrivate && (sender != null))
		{
			// Responde por Whisper (PM)
			sender.sendPacket(new CreatureSay(bot, ChatType.WHISPER, bot.getName(), text));
		}
		else
		{
			// Fala pelo chat Geral
			bot.broadcastPacket(new CreatureSay(bot, ChatType.GENERAL, bot.getName(), text));
		}
	}
	
	/**
	 * Schedules a random Spanish reply after a short typing delay.
	 * @param bot the phantom
	 * @param sender the player being answered, or {@code null} for casual chat
	 * @param isPrivate {@code true} for whisper replies
	 */
	public static void requestChatResponse(Player bot, Player sender, boolean isPrivate)
	{
		handleCustomChat(bot, sender, isPrivate);
	}
	
	private static void handleCustomChat(Player bot, Player sender, boolean isPrivate)
	{
		if ((bot == null) || bot.isDead())
		{
			return;
		}
		
		String replyText;
		
		// Se e uma mensagem privada, ou se e por geral mas decidimos responder ao target
		if (isPrivate || ((sender != null) && (Rnd.get(100) < 50)))
		{
			replyText = RESPONSES[Rnd.get(RESPONSES.length)];
		}
		else
		{
			// Soltar uma frase casual ao ar
			replyText = CASUAL_MESSAGES[Rnd.get(CASUAL_MESSAGES.length)];
		}
		
		int typingDelay = calculateTypingTime(replyText, Rnd.get(500, 1500));
		ThreadPool.schedule(() ->
		{
			if (bot.isOnline() && !bot.isDead())
			{
				botReply(bot, sender, replyText, isPrivate);
			}
		}, typingDelay);
	}

	/**
	 * @param text reply text
	 * @param jitterMs random extra delay in ms
	 * @return total typing delay in ms
	 */
	static int calculateTypingTime(String text, int jitterMs)
	{
		int length = (text == null) ? 0 : text.length();
		return 1500 + (length * 120) + Math.max(0, jitterMs);
	}
	
	/**
	 * Metodo extra caso outros scripts precisem forcar a mensagem de morte
	 * @param bot
	 */
	public static void sayDeathMessage(Player bot)
	{
		if (bot == null)
		{
			return;
		}
		
		String text = Rnd.pick(DEATH_MESSAGES);
		bot.broadcastPacket(new CreatureSay(bot, ChatType.GENERAL, bot.getName(), text));
	}

	/**
	 * While resting in a peace zone, occasionally greets a nearby phantom and schedules a reply.
	 * @param bot the resting phantom
	 */
	public static void attemptBotConversation(Player bot)
	{
		if (!isEligibleForTownChat(bot))
		{
			return;
		}

		long now = System.currentTimeMillis();
		long last = LAST_TOWN_CHAT_AT.getOrDefault(bot.getObjectId(), 0L);
		if (!isTownChatCooldownReady(now, last))
		{
			return;
		}

		if (!passesTownChatRoll(Rnd.get(100)))
		{
			return;
		}

		Player partner = findNearbyPhantom(bot);
		if (partner == null)
		{
			return;
		}

		LAST_TOWN_CHAT_AT.put(bot.getObjectId(), now);
		LAST_TOWN_CHAT_AT.put(partner.getObjectId(), now);

		String greeting = GREETINGS[Rnd.get(GREETINGS.length)];
		bot.broadcastPacket(new CreatureSay(bot, ChatType.GENERAL, bot.getName(), greeting));
		scheduleTownReply(partner);
	}

	/**
	 * Schedules a short greeting reply. Town chat does not use the farm or trade lines.
	 * @param partner the phantom who answers
	 */
	static void scheduleTownReply(Player partner)
	{
		if ((partner == null) || partner.isDead())
		{
			return;
		}

		String replyText = TOWN_REPLIES[Rnd.get(TOWN_REPLIES.length)];
		int typingDelay = calculateTypingTime(replyText, Rnd.get(500, 1500));
		ThreadPool.schedule(() ->
		{
			if (partner.isOnline() && !partner.isDead())
			{
				partner.broadcastPacket(new CreatureSay(partner, ChatType.GENERAL, partner.getName(), replyText));
			}
		}, typingDelay);
	}

	static boolean isEligibleForTownChat(Player bot)
	{
		return (bot != null) && !bot.isDead() && bot.isOnline();
	}

	static boolean isTownChatCooldownReady(long now, long lastAt)
	{
		return (now - lastAt) >= TOWN_CHAT_COOLDOWN_MS;
	}

	static boolean passesTownChatRoll(int roll)
	{
		return roll < 10;
	}

	private static Player findNearbyPhantom(Player bot)
	{
		Player best = null;
		double bestDist = Double.MAX_VALUE;
		for (Player other : World.getInstance().getVisibleObjects(bot, Player.class))
		{
			if ((other == null) || (other == bot) || other.isDead() || (other.getClient() != null))
			{
				continue;
			}
			if (Math.abs(bot.getZ() - other.getZ()) > TOWN_CHAT_Z)
			{
				continue;
			}
			double dist = LocationUtil.calculateDistance(bot.getX(), bot.getY(), 0, other.getX(), other.getY(), 0, false, false);
			if ((dist <= TOWN_CHAT_XY) && (dist < bestDist))
			{
				bestDist = dist;
				best = other;
			}
		}
		return best;
	}

	public static void clearRuntimeState()
	{
		LAST_TOWN_CHAT_AT.clear();
	}
}
