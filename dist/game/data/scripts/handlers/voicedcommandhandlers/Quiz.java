package handlers.voicedcommandhandlers;

import com.l2journey.gameserver.handler.IVoicedCommandHandler;
import com.l2journey.gameserver.model.actor.Player;

import events.QuizEvent.QuizEvent;

public class Quiz implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS =
	{
		"quiz",
		"1",
		"2",
		"3"
	};
	
	@Override
	public boolean useVoicedCommand(String command, Player activeChar, String params)
	{
		if (command.equalsIgnoreCase("quiz"))
		{
			activeChar.sendMessage(QuizEvent._quizRunning ? "Quiz Event in progress. Answer with .1, .2 or .3 in the chat." : "There is no Quiz Event running right now.");
			return true;
		}
		
		try
		{
			// A validação do estado do evento e do valor (1 a 3) é feita dentro do setAnswer.
			QuizEvent.setAnswer(activeChar, Integer.parseInt(command));
		}
		catch (NumberFormatException e)
		{
			return false;
		}
		return true;
	}
	
	@Override
	public String[] getVoicedCommandList()
	{
		return VOICED_COMMANDS;
	}
}