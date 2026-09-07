package com.l2journey.gameserver.model.phantom;

import com.l2journey.commons.util.Rnd;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.network.enums.ChatType;
import com.l2journey.gameserver.network.serverpackets.CreatureSay;

public class PhantomChat
{
	// Mensagens que falam quando morrem
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
	
	// Mensagens casuais ditas no ar enquanto farmam ou caminham
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
	// Mensagens usadas para RESPONDER quando um jogador real fala com eles (por geral ou PM)
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
	
	public static void botReply(Player bot, Player sender, String text, boolean isPrivate)
	{
		if (isPrivate && (sender != null))
		{
			// Responde por sussurro (PM)
			sender.sendPacket(new CreatureSay(bot, ChatType.WHISPER, bot.getName(), text));
		}
		else
		{
			// Fala pelo chat Geral
			bot.broadcastPacket(new CreatureSay(bot, ChatType.GENERAL, bot.getName(), text));
		}
	}
	
	/**
	 * Dispara uma resposta do bot usando o sistema de frases aleatorias.
	 * @param bot
	 * @param sender
	 * @param prompt
	 * @param isPrivate
	 */
	public static void requestChatResponse(Player bot, Player sender, String prompt, boolean isPrivate)
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
		
		// Se for mensagem privada, ou se for por geral mas decidimos responder ao alvo
		if (isPrivate || ((sender != null) && (Rnd.get(100) < 50)))
		{
			replyText = RESPONSES[Rnd.get(RESPONSES.length)];
		}
		else
		{
			// Solta uma frase casual no ar
			replyText = CASUAL_MESSAGES[Rnd.get(CASUAL_MESSAGES.length)];
		}
		
		botReply(bot, sender, replyText, isPrivate);
	}
	
	/**
	 * Método extra por si otros scripts necesitan forzar el mensaje de muerte
	 * @param bot
	 */
	public static void sayDeathMessage(Player bot)
	{
		if (bot == null)
		{
			return;
		}
		
		String text = DEATH_MESSAGES[Rnd.get(DEATH_MESSAGES.length)];
		bot.broadcastPacket(new CreatureSay(bot, ChatType.GENERAL, bot.getName(), text));
	}
}