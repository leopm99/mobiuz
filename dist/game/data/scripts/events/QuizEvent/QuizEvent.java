package events.QuizEvent;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.l2journey.Config;
import com.l2journey.EventsConfig;
import com.l2journey.commons.threads.ThreadPool;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.item.enums.ItemProcessType;
import com.l2journey.gameserver.util.Broadcast;

/**
 * @author Bellatrix
 * @modified [Novak] - Sistema aprimorado de seleção de perguntas
 * @modified Revisão: concorrência, validação do XML, reagendamento seguro e sorteio por "baralho".
 */
public class QuizEvent
{
	private static final Logger LOGGER = Logger.getLogger(QuizEvent.class.getName());
	private static final String QUESTIONS_FILE = "data/scripts/events/QuizEvent/QuizEvent.xml";
	
	private static final int STATUS_NOT_IN_PROGRESS = 0;
	private static final int STATUS_ASK = 1;
	private static final int STATUS_ANSWER = 2;
	private static final int STATUS_END = 3;
	
	private static final long PAUSE_BETWEEN_QUESTIONS = 5000;
	private static final long PAUSE_BEFORE_END = 3000;
	private static final long DISABLED_RECHECK_DELAY = 60000;
	
	private record Question(String ask, String[] answers, int correct)
	{
	}
	
	/** Lido pela thread do chat para saber se deve aceitar respostas. */
	public static volatile boolean _quizRunning;
	
	// Estado compartilhado entre o scheduler e a thread do chat.
	private static final Map<Integer, Question> QUESTIONS = new ConcurrentHashMap<>();
	private static final Map<Integer, Integer> QUESTION_USAGE = new ConcurrentHashMap<>();
	private static final Map<Player, Integer> PLAYERS = new ConcurrentHashMap<>();
	private static final AtomicBoolean STARTED = new AtomicBoolean();
	private static volatile int _status = STATUS_NOT_IN_PROGRESS;
	private static volatile Question _current;
	
	// Usados apenas pela cadeia de tasks (uma por vez).
	private static final List<Integer> DECK = new ArrayList<>();
	private static int _lastQuestionId = -1;
	private static int _announced;
	
	// ----------------------------------------------------------------------------
	// ------------------------------ CONFIGS --------------------------------------
	// ----------------------------------------------------------------------------
	
	private static int _questionNumber;
	private static int _rewardID;
	private static long _rewardCount;
	private static int _initWait;
	private static int _answerTime;
	private static int _betweenTime;
	
	public QuizEvent()
	{
		// Se o construtor for chamado mais de uma vez (ex.: getVoicedCommandList), só a primeira chamada inicia o evento.
		if (!STARTED.compareAndSet(false, true))
		{
			return;
		}
		
		resetState();
		loadConfig();
		loadQuestions();
		
		schedule(_initWait * 1000L);
	}
	
	// ----------------------------------------------------------------------------
	// ------------------------------ AGENDAMENTO ----------------------------------
	// ----------------------------------------------------------------------------
	
	private static void schedule(long delay)
	{
		ThreadPool.schedule(QuizEvent::run, delay);
	}
	
	/**
	 * Cada etapa devolve o atraso até a próxima. Assim a cadeia sempre é reagendada, mesmo se uma etapa falhar.
	 */
	private static void run()
	{
		long nextDelay;
		try
		{
			nextDelay = step();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "QuizEvent: error during event step, restarting the cycle.", e);
			resetState();
			nextDelay = _betweenTime * 1000L;
		}
		
		schedule(nextDelay);
	}
	
	private static long step()
	{
		switch (_status)
		{
			case STATUS_NOT_IN_PROGRESS:
			{
				return startEvent();
			}
			case STATUS_ASK:
			{
				if (_announced < _questionNumber)
				{
					return askQuestion();
				}
				_status = STATUS_END;
				return PAUSE_BEFORE_END;
			}
			case STATUS_ANSWER:
			{
				return revealAnswer();
			}
			case STATUS_END:
			{
				return endEvent();
			}
			default:
			{
				resetState();
				return _betweenTime * 1000L;
			}
		}
	}
	
	// ----------------------------------------------------------------------------
	// ------------------------------ ETAPAS ---------------------------------------
	// ----------------------------------------------------------------------------
	
	private static long startEvent()
	{
		if (!EventsConfig.QUIZ_EVENT_ENABLED)
		{
			return DISABLED_RECHECK_DELAY; // Continua verificando, para o evento iniciar se for habilitado depois.
		}
		
		if (QUESTIONS.isEmpty())
		{
			LOGGER.warning("QuizEvent: no valid questions loaded, event not started.");
			return _betweenTime * 1000L;
		}
		
		_announced = 0;
		PLAYERS.clear();
		_quizRunning = true;
		
		Broadcast.toAllOnlinePlayers("Quiz Event begins! " + _questionNumber + " questions. " + _answerTime + " secs for answer each. ", true);
		Broadcast.toAllOnlinePlayers("Type . and the number of the correct answer to the chat. (Like: .1)", true);
		Broadcast.toAllOnlinePlayers("Get Ready!", true);
		
		_status = STATUS_ASK;
		return PAUSE_BETWEEN_QUESTIONS;
	}
	
	private static long askQuestion()
	{
		final Question question = selectQuestion();
		_current = question;
		PLAYERS.clear();
		
		Broadcast.toAllOnlinePlayers("-----------------", true);
		Broadcast.toAllOnlinePlayers("Question: " + question.ask(), true);
		Broadcast.toAllOnlinePlayers("-----------------", true);
		Broadcast.toAllOnlinePlayers("1: " + question.answers()[0], true);
		Broadcast.toAllOnlinePlayers("2: " + question.answers()[1], true);
		Broadcast.toAllOnlinePlayers("3: " + question.answers()[2], true);
		Broadcast.toAllOnlinePlayers("-----------------", true);
		
		_status = STATUS_ANSWER; // A partir daqui as respostas são aceitas.
		return _answerTime * 1000L;
	}
	
	private static long revealAnswer()
	{
		_status = STATUS_ASK; // Fecha a janela de respostas antes de contabilizar.
		_announced++;
		
		final Question question = _current;
		final int winners = giveReward(question);
		
		Broadcast.toAllOnlinePlayers("-----------------", true);
		Broadcast.toAllOnlinePlayers("The correct answer was: " + question.correct() + " (" + question.answers()[question.correct() - 1] + ")", true);
		Broadcast.toAllOnlinePlayers("Correct answers: " + winners, true);
		Broadcast.toAllOnlinePlayers("-----------------", true);
		
		return PAUSE_BETWEEN_QUESTIONS;
	}
	
	private static long endEvent()
	{
		Broadcast.toAllOnlinePlayers("The Quiz Event is over!", true);
		resetState();
		return _betweenTime * 1000L;
	}
	
	private static int giveReward(Question question)
	{
		int winners = 0;
		for (Map.Entry<Player, Integer> entry : PLAYERS.entrySet())
		{
			final Player player = entry.getKey();
			if ((player == null) || !player.isOnline())
			{
				continue;
			}
			
			try
			{
				if (entry.getValue().intValue() == question.correct())
				{
					player.sendMessage("Your answer was correct!");
					player.addItem(ItemProcessType.REWARD, _rewardID, _rewardCount, player, true);
					winners++;
				}
				else
				{
					player.sendMessage("Your answer was not correct!");
				}
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, "QuizEvent: failed to process the answer of " + player.getName() + ".", e);
			}
		}
		PLAYERS.clear();
		return winners;
	}
	
	// ----------------------------------------------------------------------------
	// ------------------------------ PERGUNTAS ------------------------------------
	// ----------------------------------------------------------------------------
	
	/**
	 * Sorteio por "baralho": todas as perguntas saem uma vez em ordem aleatória antes de qualquer repetição, e a última pergunta de um baralho nunca é a primeira do próximo.
	 */
	private static Question selectQuestion()
	{
		if (DECK.isEmpty())
		{
			DECK.addAll(QUESTIONS.keySet());
			Collections.shuffle(DECK);
			if ((DECK.size() > 1) && (DECK.get(DECK.size() - 1).intValue() == _lastQuestionId))
			{
				Collections.swap(DECK, 0, DECK.size() - 1);
			}
		}
		
		final int id = DECK.remove(DECK.size() - 1);
		_lastQuestionId = id;
		QUESTION_USAGE.merge(id, 1, Integer::sum);
		return QUESTIONS.get(id);
	}
	
	private static void loadQuestions()
	{
		QUESTIONS.clear();
		QUESTION_USAGE.clear();
		DECK.clear();
		_lastQuestionId = -1;
		
		final File file = new File(Config.DATAPACK_ROOT, QUESTIONS_FILE);
		if (!file.isFile())
		{
			LOGGER.severe("QuizEvent: questions file not found: " + file);
			return;
		}
		
		try
		{
			final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setIgnoringComments(true);
			factory.setValidating(false);
			final Document doc = factory.newDocumentBuilder().parse(file);
			
			for (Node root = doc.getFirstChild(); root != null; root = root.getNextSibling())
			{
				if (!"list".equalsIgnoreCase(root.getNodeName()))
				{
					continue;
				}
				for (Node child = root.getFirstChild(); child != null; child = child.getNextSibling())
				{
					if ("question".equalsIgnoreCase(child.getNodeName()))
					{
						parseQuestion(child);
					}
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "QuizEvent: error loading questions.", e);
		}
		
		LOGGER.info("QuizEvent: loaded " + QUESTIONS.size() + " questions.");
	}
	
	private static void parseQuestion(Node node)
	{
		final NamedNodeMap attrs = node.getAttributes();
		try
		{
			final int id = Integer.parseInt(attribute(attrs, "id"));
			final int correct = Integer.parseInt(attribute(attrs, "correct"));
			if ((correct < 1) || (correct > 3))
			{
				LOGGER.warning("QuizEvent: question " + id + " skipped, 'correct' must be 1, 2 or 3.");
				return;
			}
			if (QUESTIONS.containsKey(id))
			{
				LOGGER.warning("QuizEvent: question " + id + " skipped, duplicated id.");
				return;
			}
			
			final String[] answers =
			{
				attribute(attrs, "answer1"),
				attribute(attrs, "answer2"),
				attribute(attrs, "answer3")
			};
			QUESTIONS.put(id, new Question(attribute(attrs, "ask"), answers, correct));
			QUESTION_USAGE.put(id, 0);
		}
		catch (RuntimeException e)
		{
			LOGGER.warning("QuizEvent: invalid question skipped (" + e.getMessage() + ").");
		}
	}
	
	private static String attribute(NamedNodeMap attrs, String name)
	{
		final Node attr = attrs.getNamedItem(name);
		if (attr == null)
		{
			throw new IllegalArgumentException("missing attribute '" + name + "'");
		}
		return attr.getNodeValue();
	}
	
	// ----------------------------------------------------------------------------
	// ------------------------------ API PÚBLICA ----------------------------------
	// ----------------------------------------------------------------------------
	
	public static void setAnswer(Player player, int answer)
	{
		if ((player == null) || !_quizRunning)
		{
			return;
		}
		
		if (_status != STATUS_ANSWER)
		{
			player.sendMessage("There is no question open for answers right now.");
			return;
		}
		
		if ((answer < 1) || (answer > 3))
		{
			player.sendMessage("Choose 1, 2 or 3.");
			return;
		}
		
		final Integer previous = PLAYERS.putIfAbsent(player, answer);
		if (previous != null)
		{
			player.sendMessage("You already chose an answer: " + previous);
		}
		else
		{
			player.sendMessage("Your answer (" + answer + ") was registered.");
		}
	}
	
	public static String getQuestionStats()
	{
		final StringBuilder stats = new StringBuilder("Question Usage Statistics:\n");
		final List<Integer> ids = new ArrayList<>(QUESTIONS.keySet());
		Collections.sort(ids);
		for (int id : ids)
		{
			stats.append("Q").append(id).append(": ").append(QUESTION_USAGE.getOrDefault(id, 0)).append(" uses\n");
		}
		return stats.toString();
	}
	
	// ----------------------------------------------------------------------------
	// ------------------------------ AUXILIARES -----------------------------------
	// ----------------------------------------------------------------------------
	private static void loadConfig()
	{
		// Lidos aqui (e não em campos estáticos) para não depender da ordem de carregamento das classes.
		_questionNumber = Math.max(1, EventsConfig.QUIZ_EVENT_QUESTION_NUMBER);
		_rewardID = EventsConfig.QUIZ_EVENT_REWARD_ID;
		_rewardCount = EventsConfig.QUIZ_EVENT_REWARD_COUNT;
		_initWait = Math.max(1, EventsConfig.QUIZ_EVENT_STARTING);
		_answerTime = Math.max(1, EventsConfig.QUIZ_EVENT_ANSWER_TIME);
		_betweenTime = Math.max(1, EventsConfig.QUIZ_EVENT_INTERVAL);
	}
	
	private static void resetState()
	{
		_quizRunning = false;
		_status = STATUS_NOT_IN_PROGRESS;
		_announced = 0;
		_current = null;
		PLAYERS.clear();
	}
}
