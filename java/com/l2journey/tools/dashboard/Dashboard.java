package com.l2journey.tools.dashboard;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;

/**
 * Dashboard.
 * @author Aggretsuko
 */
public final class Dashboard
{
	private static final Color BACKGROUND = new Color(5, 25, 31);
	private static final Color CARD = new Color(6, 34, 42);
	private static final Color BORDER = new Color(11, 76, 87);
	private static final Color GREEN = new Color(101, 239, 43);
	private static final Color GOLD = new Color(227, 184, 72);
	private static final Color RED = new Color(238, 83, 76);
	private static final Color MUTED = new Color(129, 184, 189);
	private static final Color WHITE = new Color(232, 247, 241);
	private static final Path ROOT = findRoot();
	private static final Pattern PORT = Pattern.compile("(?m)^\\s*%s\\s*=\\s*(\\d+)");
	private static final Pattern XMX = Pattern.compile("-Xmx(\\d+(?:\\.\\d+)?)([kKmMgG])");
	private static final Pattern HEAP_INFO = Pattern.compile("(?i)used\\s+(\\d+(?:\\.\\d+)?)\\s*([kmg])");
	private static final long GAME_HEAP_LIMIT = readXmx(ROOT.resolve("game/startGameServer.bat"));
	private static final long LOGIN_HEAP_LIMIT = readXmx(ROOT.resolve("login/startLoginServer.bat"));
	private static final int PLAYERS_LIMIT = Integer.parseInt(readPort(ROOT.resolve("game/config/server.ini"), "MaximumOnlineUsers", "100"));
	private static final Map<String, ProcessHandle> PROCESS_CACHE = new ConcurrentHashMap<>();
	private static final String[] DELAYS =
	{
		"Imediatamente",
		"Em 5 segundos",
		"Em 30 segundos",
		"Em 60 segundos",
		"Em 10 minutos"
	};
	
	private final JTextArea _loginConsole = console();
	private final JTextArea _gameConsole = console();
	private final JLabel _loginState = new JLabel();
	private final JLabel _gameState = new JLabel();
	private final JLabel _uptime = new JLabel("--");
	private final JLabel _java = new JLabel(System.getProperty("java.version"));
	private final JLabel _clientPort = new JLabel(readPort(ROOT.resolve("game/config/server.ini"), "GameserverPort", "7777"));
	private final JLabel _gamePort = new JLabel(readPort(ROOT.resolve("login/config/loginserver.ini"), "LoginPort", "9014"));
	private final JLabel _loginThreads = new JLabel("--");
	private final JLabel _gameThreads = new JLabel("--");
	private final Gauge _players = new Gauge("PLAYERS ONLINE", "0", "Aguardando dados");
	private final Gauge _gameMemory = new Gauge("MEMORIA GAMESERVER", "--", "Aguardando JVM");
	private final Gauge _loginMemory = new Gauge("MEMORIA LOGINSERVER", "--", "Aguardando JVM");
	private volatile int _playersOnline;
	private final AtomicBoolean _queryingMetrics = new AtomicBoolean();
	private final AtomicBoolean _queryingStatus = new AtomicBoolean();
	private volatile ProcessMetrics _loginMetrics = ProcessMetrics.EMPTY;
	private volatile ProcessMetrics _gameMetrics = ProcessMetrics.EMPTY;
	private volatile int _refreshTick;
	private JTabbedPane _tabs;
	private final AtomicBoolean _closing = new AtomicBoolean();
	
	public static void main(String[] args)
	{
		SwingUtilities.invokeLater(() -> new Dashboard().show());
	}
	
	private void show()
	{
		final JFrame frame = new JFrame("L2Journey - Dashboard");
		frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		// Encerra processos orfaos de login/game mesmo se a JVM sair via Ctrl+C ou um sinal em vez do botao de fechar da janela.
		Runtime.getRuntime().addShutdownHook(new Thread(this::shutdownServers, "JourneyDash-ShutdownHook"));
		frame.addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent event)
			{
				if (!_closing.compareAndSet(false, true))
				{
					return;
				}
				frame.setTitle("L2Journey - Dashboard (encerrando servidores...)");
				// stop() bloqueia por alguns segundos por processo; executa fora da EDT para a janela nao travar e ser forcada a fechar.
				new Thread(() ->
				{
					shutdownServers();
					SwingUtilities.invokeLater(() ->
					{
						frame.dispose();
						System.exit(0);
					});
				}, "JourneyDash-Shutdown").start();
			}
		});
		frame.setMinimumSize(new Dimension(980, 680));
		frame.setSize(1160, 760);
		frame.setLocationByPlatform(true);
		frame.setContentPane(content());
		frame.setVisible(true);
		// Fonte so pode ser derivada apos setVisible(): antes disso o Frame (AWT pesado) ainda nao tem font resolvida (getFont() == null).
		resizeFont(frame, 12);
		restoreConsole("login", "loginserver", _loginConsole);
		restoreConsole("game", "gameserver", _gameConsole);
		new Timer(2000, event -> refresh()).start();
		refresh();
	}
	
	private void restoreConsole(String folder, String processName, JTextArea output)
	{
		find(processName).ifPresent(process ->
		{
			append(output, "[JourneyDash] Processo em execucao detectado.");
			append(output, "PID: " + process.pid());
			append(output, "CMD: " + process.info().commandLine().orElse("java ... " + processName));
			loadRecentLog(ROOT.resolve(folder).resolve("log"), output);
		});
	}
	
	private static void loadRecentLog(Path directory, JTextArea output)
	{
		try (var files = Files.list(directory))
		{
			final Path log = files.filter(Files::isRegularFile).filter(file -> file.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".log")).max(java.util.Comparator.comparingLong(file -> file.toFile().lastModified())).orElse(null);
			if (log != null)
			{
				// Posiciona no final do arquivo em vez de carregar o log inteiro (possivelmente varios MB) na memoria.
				try (RandomAccessFile file = new RandomAccessFile(log.toFile(), "r"))
				{
					final long offset = Math.max(0, file.length() - 12000);
					file.seek(offset);
					final byte[] data = new byte[(int) (file.length() - offset)];
					file.readFully(data);
					append(output, "---- " + log.getFileName() + " (ultimas linhas) ----");
					append(output, new String(data, StandardCharsets.UTF_8));
				}
			}
		}
		catch (IOException e)
		{
		}
	}
	
	private JPanel content()
	{
		final JPanel content = new JPanel(new BorderLayout(0, 12));
		content.setBackground(BACKGROUND);
		content.setBorder(new EmptyBorder(14, 18, 18, 18));
		content.add(header(), BorderLayout.NORTH);
		final JTabbedPane tabs = new JTabbedPane();
		tabs.setFont(tabs.getFont().deriveFont(Font.BOLD, 14f));
		tabs.addTab("  HOME  ", summary());
		tabs.addTab("  GAME SERVER  ", consoleTab("Console de inicializacao - GameServer", _gameConsole));
		tabs.addTab("  LOGIN SERVER  ", consoleTab("Console de inicializacao - LoginServer", _loginConsole));
		content.add(tabs, BorderLayout.CENTER);
		_tabs = tabs;
		return content;
	}
	
	/**
	 * Troca para a aba de console correspondente ao tipo de servico "login"/"game".
	 * @param type
	 */
	private void selectTab(String type)
	{
		if (_tabs != null)
		{
			_tabs.setSelectedIndex(type.equals("login") ? 2 : 1);
		}
	}
	
	private JPanel header()
	{
		final JPanel panel = new JPanel(new BorderLayout(12, 0));
		panel.setOpaque(false);
		final ImageIcon logo = logo();
		if (logo != null)
		{
			panel.add(new JLabel(logo), BorderLayout.WEST);
		}
		final JPanel text = new JPanel(new GridLayout(0, 1, 0, 3));
		text.setOpaque(false);
		final JLabel title = new JLabel("Dashboard");
		title.setForeground(GOLD);
		title.setFont(title.getFont().deriveFont(Font.BOLD, 23f));
		final JLabel subtitle = new JLabel("The most important thing is the journey.");
		subtitle.setForeground(MUTED);
		text.add(title);
		text.add(subtitle);
		panel.add(text, BorderLayout.CENTER);
		panel.add(tools(), BorderLayout.EAST);
		return panel;
	}
	
	private JPanel summary()
	{
		final JPanel page = new JPanel(new BorderLayout(0, 12));
		page.setBackground(BACKGROUND);
		page.setBorder(new EmptyBorder(14, 4, 4, 4));
		final JPanel center = new JPanel(new BorderLayout(0, 12));
		center.setOpaque(false);
		final JPanel facts = new JPanel(new GridLayout(2, 3, 10, 10));
		facts.setOpaque(false);
		facts.add(fact("TEMPO ATIVO", _uptime));
		facts.add(fact("JAVA", _java));
		facts.add(fact("THREADS DO LOGIN", _loginThreads));
		facts.add(fact("PORTA GAME SERVER", _gamePort));
		facts.add(fact("PORTA DO CLIENTE", _clientPort));
		facts.add(fact("THREADS DO GAME", _gameThreads));
		center.add(facts, BorderLayout.NORTH);
		final JPanel metrics = card(new GridLayout(1, 3, 18, 0));
		metrics.setBorder(new EmptyBorder(14, 20, 16, 20));
		metrics.add(_players);
		metrics.add(_gameMemory);
		metrics.add(_loginMemory);
		center.add(metrics, BorderLayout.CENTER);
		page.add(center, BorderLayout.CENTER);
		page.add(controls(), BorderLayout.SOUTH);
		return page;
	}
	
	private JPanel tools()
	{
		final JPanel bar = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 6, 0));
		bar.setOpaque(false);
		bar.add(toolButton("Reload", new String[]
		{
			"Configs",
			"Access",
			"HTML",
			"Multisells",
			"Buylists"
		}, item -> command("RELOAD " + item.toUpperCase(Locale.ROOT))));
		bar.add(toolButton("Announce", () -> announce("Normal")));
		bar.add(toolButton("Logs", new String[]
		{
			"View",
			"Delete"
		}, this::logs));
		return bar;
	}
	
	private JButton toolButton(String name, String[] items, java.util.function.Consumer<String> action)
	{
		final JButton button = new JButton(name + " ▾");
		button.setMargin(new java.awt.Insets(4, 8, 4, 8));
		button.setFont(button.getFont().deriveFont(Font.BOLD, 11f));
		button.setForeground(GOLD);
		button.setBackground(CARD);
		button.setFocusPainted(false);
		final JPopupMenu menu = new JPopupMenu();
		for (String item : items)
		{
			final javax.swing.JMenuItem entry = new javax.swing.JMenuItem(item);
			entry.addActionListener(event -> action.accept(item));
			menu.add(entry);
		}
		button.addActionListener(event -> menu.show(button, 0, button.getHeight()));
		return button;
	}
	
	private JButton toolButton(String name, Runnable action)
	{
		final JButton button = new JButton(name);
		button.setMargin(new java.awt.Insets(4, 8, 4, 8));
		button.setFont(button.getFont().deriveFont(Font.BOLD, 11f));
		button.setForeground(GOLD);
		button.setBackground(CARD);
		button.setFocusPainted(false);
		button.addActionListener(event -> action.run());
		return button;
	}
	
	private void announce(String type)
	{
		final String text = JOptionPane.showInputDialog(null, type + " announcement:");
		if ((text != null) && !text.isBlank())
		{
			command("ANNOUNCE " + type.toUpperCase(Locale.ROOT) + " " + text.trim());
		}
	}
	
	private void logs(String action)
	{
		final Path logs = ROOT.resolve("game/log");
		if (action.equals("View"))
		{
			openPath(logs);
			return;
		}
		if (JOptionPane.showConfirmDialog(null, "Excluir arquivos de log do GameServer?", "Logs", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
		{
			try (var files = Files.list(logs))
			{
				files.filter(Files::isRegularFile).forEach(file ->
				{
					try
					{
						Files.deleteIfExists(file);
					}
					catch (IOException e)
					{
					}
				});
			}
			catch (IOException e)
			{
			}
		}
	}
	
	private static void resizeFont(java.awt.Component component, int size)
	{
		component.setFont(component.getFont().deriveFont((float) size));
		if (component instanceof java.awt.Container container)
		{
			for (java.awt.Component child : container.getComponents())
			{
				resizeFont(child, size);
			}
		}
	}
	
	private void command(String value)
	{
		new Thread(() ->
		{
			try (Socket socket = new Socket("127.0.0.1", 15432);
				PrintWriter output = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
				BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8)))
			{
				output.println(value);
				final String response = input.readLine();
				append(_gameConsole, "[JourneyDash] " + value + ": " + response);
			}
			catch (IOException e)
			{
				append(_gameConsole, "[JourneyDash] GameServer nao esta disponivel.");
			}
		}, "JourneyDash-Command").start();
	}
	
	private JPanel controls()
	{
		final JPanel panel = new JPanel(new GridLayout(1, 2, 12, 0));
		panel.setOpaque(false);
		panel.add(serviceControls("GAME SERVER", _gameState, "game", _gameConsole));
		panel.add(serviceControls("LOGIN SERVER", _loginState, "login", _loginConsole));
		return panel;
	}
	
	private JPanel serviceControls(String title, JLabel state, String type, JTextArea console)
	{
		final JPanel panel = card(new BorderLayout(0, 8));
		panel.setBorder(new EmptyBorder(10, 12, 10, 12));
		panel.add(stateCard(title, state), BorderLayout.NORTH);
		final JPanel actions = new JPanel(new GridLayout(1, 3, 6, 0));
		actions.setOpaque(false);
		actions.add(button("INICIAR", GREEN, () ->
		{
			selectTab(type);
			launch(type, console);
		}));
		actions.add(button("PARAR", RED, () ->
		{
			selectTab(type);
			scheduleService("Parar " + title, type, console, false);
		}));
		actions.add(button("REINICIAR", GOLD, () ->
		{
			selectTab(type);
			scheduleService("Reiniciar " + title, type, console, true);
		}));
		panel.add(actions, BorderLayout.CENTER);
		return panel;
	}
	
	private void scheduleService(String label, String type, JTextArea console, boolean restart)
	{
		final String processName = type + "server";
		if (find(processName).isEmpty())
		{
			append(console, "[Dashboard] Nao ha sessao ativa para " + label.toLowerCase(Locale.ROOT) + ".");
			JOptionPane.showMessageDialog(null, "Nao ha sessao ativa no momento.", "Sessao inativa", JOptionPane.WARNING_MESSAGE);
			return;
		}
		final String selection = (String) JOptionPane.showInputDialog(null, "Quando deseja executar a acao?", label, JOptionPane.QUESTION_MESSAGE, null, DELAYS, DELAYS[0]);
		if (selection == null)
		{
			return;
		}
		final int seconds = delaySeconds(selection);
		final Timer timer = new Timer(seconds * 1000, event ->
		{
			((Timer) event.getSource()).stop();
			stop(processName, console);
			if (restart)
			{
				relaunchAfterDelay(type, console);
			}
		});
		timer.setRepeats(false);
		timer.start();
	}
	
	// Intervalo fixo entre parar e iniciar para o reinicio sempre completar visualmente em vez de ficar tentando e desistindo.
	private void relaunchAfterDelay(String type, JTextArea console)
	{
		append(console, "[Dashboard] Aguardando 5 segundos antes de iniciar novamente...");
		final Timer timer = new Timer(5000, event ->
		{
			((Timer) event.getSource()).stop();
			launch(type, console);
		});
		timer.setRepeats(false);
		timer.start();
	}
	
	private JPanel consoleTab(String title, JTextArea console)
	{
		final JPanel panel = new JPanel(new BorderLayout(0, 10));
		panel.setBackground(BACKGROUND);
		panel.setBorder(new EmptyBorder(16, 4, 4, 4));
		final JLabel label = new JLabel(title);
		label.setForeground(GOLD);
		label.setFont(label.getFont().deriveFont(Font.BOLD, 15f));
		panel.add(label, BorderLayout.NORTH);
		final JScrollPane scroll = new JScrollPane(console);
		scroll.setBorder(BorderFactory.createLineBorder(BORDER));
		panel.add(scroll, BorderLayout.CENTER);
		return panel;
	}
	
	private JPanel fact(String title, JLabel value)
	{
		final JPanel panel = card(new GridLayout(0, 1, 0, 6));
		panel.setBorder(new EmptyBorder(12, 13, 10, 13));
		final JLabel heading = new JLabel(title);
		heading.setForeground(MUTED);
		heading.setFont(heading.getFont().deriveFont(11f));
		value.setForeground(GREEN);
		value.setFont(value.getFont().deriveFont(Font.BOLD, 18f));
		panel.add(heading);
		panel.add(value);
		return panel;
	}
	
	private JPanel stateCard(String title, JLabel state)
	{
		final JPanel panel = card(new BorderLayout());
		panel.setBorder(new EmptyBorder(10, 13, 10, 13));
		final JLabel name = new JLabel(title);
		name.setForeground(MUTED);
		name.setFont(name.getFont().deriveFont(Font.BOLD, 11f));
		state.setHorizontalAlignment(SwingConstants.RIGHT);
		state.setFont(state.getFont().deriveFont(Font.BOLD, 14f));
		panel.add(name, BorderLayout.WEST);
		panel.add(state, BorderLayout.EAST);
		return panel;
	}
	
	private JButton button(String text, Color color, Runnable action)
	{
		final JButton button = new JButton(text);
		button.setBackground(CARD);
		button.setForeground(color);
		button.setBorder(BorderFactory.createLineBorder(color.darker()));
		button.setFocusPainted(false);
		button.setFont(button.getFont().deriveFont(Font.BOLD, 13f));
		button.addActionListener(event -> action.run());
		return button;
	}
	
	private void launch(String type, JTextArea output)
	{
		if (find(type + "server").isPresent())
		{
			append(output, "[Dashboard] Ja existe uma sessao ativa. Nao e possivel iniciar outra.");
			JOptionPane.showMessageDialog(null, "Ja existe uma sessao ativa. Encerre-a antes de iniciar outra.", "Sessao ja ativa", JOptionPane.WARNING_MESSAGE);
			return;
		}
		final String script = type.equals("login") ? "startLoginServer.bat" : "startGameServer.bat";
		final Path directory = ROOT.resolve(type);
		if (!Files.isRegularFile(directory.resolve(script)))
		{
			append(output, "[Dashboard] Arquivo nao encontrado: " + directory.resolve(script));
			return;
		}
		try
		{
			append(output, "[Dashboard] Iniciando " + script + "...");
			final Process process = new ProcessBuilder("cmd.exe", "/c", script).directory(directory.toFile()).redirectErrorStream(true).start();
			new Thread(() -> readConsole(type, process, output), "Dashboard-" + type).start();
		}
		catch (IOException e)
		{
			append(output, "[Dashboard] Falha ao iniciar: " + e.getMessage());
		}
	}
	
	private void readConsole(String type, Process process, JTextArea output)
	{
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)))
		{
			String line;
			while ((line = reader.readLine()) != null)
			{
				append(output, line);
			}
		}
		catch (IOException e)
		{
			append(output, "[Dashboard] Console encerrado.");
		}
	}
	
	private void stop(String name, JTextArea output)
	{
		find(name).ifPresent(process ->
		{
			append(output, "[Dashboard] Encerrando processo " + process.pid() + "...");
			// Encerrar apenas o processo filho java.exe deixa o cmd.exe (launcher) vivo; o startXServer.bat tem seu
			// proprio loop "if ERRORLEVEL 2 goto restart" que reinicia o servidor silenciosamente sem o dashboard saber.
			// Encerrar a arvore de processos inteira (launcher + filhos) evita essa condicao de corrida.
			killTree(shellWrapperOrSelf(process));
			try
			{
				if (!process.onExit().get(4, TimeUnit.SECONDS).isAlive())
				{
					return;
				}
			}
			catch (Exception e)
			{
			}
			try
			{
				process.destroyForcibly();
			}
			catch (Exception e)
			{
				append(output, "[Dashboard] Falha ao encerrar processo " + process.pid() + ": " + e.getMessage());
			}
		});
	}
	
	private static boolean isShellWrapper(ProcessHandle handle)
	{
		return handle.info().command().map(command -> command.toLowerCase(Locale.ROOT)).map(command -> command.endsWith("cmd.exe")).orElse(false);
	}
	
	// O cmd.exe pode nao ser o pai direto: quando "java" no PATH resolve para um shim (ex: javapath da Oracle),
	// ele fica entre o cmd.exe e a JVM real, entao sobe-se a arvore ate achar o cmd.exe em vez de checar so um nivel.
	private static long shellWrapperOrSelf(ProcessHandle process)
	{
		ProcessHandle current = process;
		for (int i = 0; i < 4; i++)
		{
			final Optional<ProcessHandle> parent = current.parent();
			if (parent.isEmpty())
			{
				break;
			}
			if (isShellWrapper(parent.get()))
			{
				return parent.get().pid();
			}
			current = parent.get();
		}
		return process.pid();
	}
	
	private static void killTree(long pid)
	{
		try
		{
			new ProcessBuilder("taskkill", "/F", "/T", "/PID", Long.toString(pid)).redirectErrorStream(true).start().waitFor(4, TimeUnit.SECONDS);
		}
		catch (Exception e)
		{
		}
	}
	
	// Garante que os dois servidores sejam encerrados quando a janela do dashboard fechar, evitando processos orfaos.
	private void shutdownServers()
	{
		try
		{
			stop("gameserver", _gameConsole);
		}
		catch (Exception e)
		{
		}
		try
		{
			stop("loginserver", _loginConsole);
		}
		catch (Exception e)
		{
		}
	}
	
	private void refresh()
	{
		final Optional<ProcessHandle> login = find("loginserver");
		final Optional<ProcessHandle> game = find("gameserver");
		status(_loginState, login.isPresent());
		status(_gameState, game.isPresent());
		final Optional<Instant> started = login.flatMap(p -> p.info().startInstant()).stream().findFirst().or(() -> game.flatMap(p -> p.info().startInstant()));
		_uptime.setText(started.map(Dashboard::formatDuration).orElse("--"));
		_players.set(percent(_playersOnline, PLAYERS_LIMIT), Integer.toString(_playersOnline), _playersOnline + " / " + PLAYERS_LIMIT);
		final long gameHeap = _gameMetrics.heap;
		_gameMemory.set(percent(gameHeap, GAME_HEAP_LIMIT), gameHeap > 0 ? human(gameHeap) : "--", gameHeap > 0 ? human(gameHeap) + " / " + human(GAME_HEAP_LIMIT) : "Aguardando JVM");
		final long loginHeap = _loginMetrics.heap;
		_loginMemory.set(percent(loginHeap, LOGIN_HEAP_LIMIT), loginHeap > 0 ? human(loginHeap) : "--", loginHeap > 0 ? human(loginHeap) + " / " + human(LOGIN_HEAP_LIMIT) : "Aguardando JVM");
		_loginThreads.setText(login.map(p -> Integer.toString(_loginMetrics.threads)).orElse("--"));
		_gameThreads.setText(game.map(p -> Integer.toString(_gameMetrics.threads)).orElse("--"));
		// As consultas via jcmd/powershell sao custosas de iniciar; executa-las a um terco da taxa de atualizacao.
		if ((_refreshTick++ % 3) == 0)
		{
			requestProcessMetrics(login, game);
		}
		requestPlayers();
	}
	
	private void requestPlayers()
	{
		if (!_queryingStatus.compareAndSet(false, true))
		{
			return;
		}
		new Thread(() ->
		{
			try (Socket socket = new Socket("127.0.0.1", 15432);
				PrintWriter output = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
				BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8)))
			{
				output.println("STATUS");
				final String response = input.readLine();
				if ((response != null) && response.startsWith("OK players="))
				{
					_playersOnline = Integer.parseInt(response.substring(11).trim());
				}
			}
			catch (Exception e)
			{
				_playersOnline = 0;
			}
			finally
			{
				_queryingStatus.set(false);
			}
		}, "JourneyDash-Status").start();
	}
	
	private void requestProcessMetrics(Optional<ProcessHandle> login, Optional<ProcessHandle> game)
	{
		if (!_queryingMetrics.compareAndSet(false, true))
		{
			return;
		}
		new Thread(() ->
		{
			try
			{
				_loginMetrics = login.map(this::queryMetrics).orElse(ProcessMetrics.EMPTY);
				_gameMetrics = game.map(this::queryMetrics).orElse(ProcessMetrics.EMPTY);
			}
			finally
			{
				_queryingMetrics.set(false);
			}
		}, "Dashboard-Metrics").start();
	}
	
	private ProcessMetrics queryMetrics(ProcessHandle process)
	{
		int threads = 0;
		try
		{
			final String command = "(Get-Process -Id " + process.pid() + " -ErrorAction Stop).Threads.Count";
			final Process probe = new ProcessBuilder("powershell.exe", "-NoProfile", "-Command", command).start();
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(probe.getInputStream(), StandardCharsets.UTF_8)))
			{
				threads = Integer.parseInt(reader.readLine().trim());
			}
			probe.waitFor(3, TimeUnit.SECONDS);
		}
		catch (Exception e)
		{
			// A consulta ao sistema operacional so fica indisponivel em uma sessao restrita do Windows.
		}
		return new ProcessMetrics(threads, heapUsed(process.pid()));
	}
	
	// O "java" simples no PATH costuma apontar para o shim javapath da Oracle, que so contem java(w).exe, nao o jcmd;
	// resolver o jcmd pelo diretorio de instalacao da propria JVM em execucao evita uma falha silenciosa de "nao foi possivel executar o programa".
	private static final Path JCMD = resolveJcmd();
	
	private static Path resolveJcmd()
	{
		for (String home : new String[]
		{
			System.getProperty("java.home"),
			System.getenv("JAVA_HOME")
		})
		{
			if (home != null)
			{
				final Path candidate = Paths.get(home, "bin", "jcmd.exe");
				if (Files.isRegularFile(candidate))
				{
					return candidate;
				}
			}
		}
		return Paths.get("jcmd");
	}
	
	private static long heapUsed(long pid)
	{
		try
		{
			final Process probe = new ProcessBuilder(JCMD.toString(), Long.toString(pid), "GC.heap_info").redirectErrorStream(true).start();
			final String output = new String(probe.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
			probe.waitFor(3, TimeUnit.SECONDS);
			final Matcher match = HEAP_INFO.matcher(output);
			if (match.find())
			{
				final double value = Double.parseDouble(match.group(1));
				return (long) (value * switch (match.group(2).toLowerCase(Locale.ROOT))
				{
					case "g" -> 1073741824d;
					case "m" -> 1048576d;
					default -> 1024d;
				});
			}
		}
		catch (Exception e)
		{
			// O jcmd e opcional em tempo de execucao; o monitoramento de memoria continua disponivel sem ele.
		}
		return 0;
	}
	
	private static Optional<ProcessHandle> find(String name)
	{
		final ProcessHandle cached = PROCESS_CACHE.get(name);
		if ((cached != null) && cached.isAlive())
		{
			return Optional.of(cached);
		}
		PROCESS_CACHE.remove(name);
		// Varrer todos os processos do SO e custoso; so paga-se esse custo quando o cache falha (primeira busca ou reinicio do processo).
		// Deve corresponder ao proprio processo java(w).exe, senao o wrapper cmd.exe (cuja linha de comando tambem
		// contem "...GameServer.bat"/"...LoginServer.bat") seria encontrado e encerrado no lugar, deixando o processo real orfao.
		final java.util.List<ProcessHandle> candidates = ProcessHandle.allProcesses().filter(p -> p.info().command().map(command -> command.toLowerCase(Locale.ROOT)).map(command -> command.endsWith("java.exe") || command.endsWith("javaw.exe")).orElse(false)).filter(p -> p.info().commandLine().orElse("").toLowerCase(Locale.ROOT).contains(name)).toList();
		// Quando "java" no PATH resolve para um shim (ex: javapath da Oracle), ele relanca um java.exe filho com a
		// mesma linha de comando; so o filho hospeda a JVM de verdade, entao ele e preferido sobre o shim pai.
		final Optional<ProcessHandle> direct = candidates.stream().filter(p -> p.parent().map(parent -> candidates.stream().anyMatch(c -> c.pid() == parent.pid())).orElse(false)).findFirst().or(() -> candidates.stream().findFirst());
		if (direct.isPresent())
		{
			direct.ifPresent(p -> PROCESS_CACHE.put(name, p));
			return direct;
		}
		// Em algumas politicas do Windows Server o Java nao consegue ler a linha de comando de outro processo via ProcessHandle.
		try
		{
			// Mesma preferencia pelo processo filho: evita pegar o shim (ex: javapath) em vez da JVM real.
			final String script = "$m=Get-CimInstance Win32_Process | Where-Object {$_.Name -eq 'java.exe' -and $_.CommandLine -match '" + name + "'}; $c=$m | Where-Object {$m.ProcessId -contains $_.ParentProcessId} | Select-Object -First 1; if ($c) { $c.ProcessId } else { $m | Select-Object -First 1 -ExpandProperty ProcessId }";
			final Process probe = new ProcessBuilder("powershell.exe", "-NoProfile", "-Command", script).redirectErrorStream(true).start();
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(probe.getInputStream(), StandardCharsets.UTF_8)))
			{
				final String pid = reader.readLine();
				if ((pid != null) && !pid.isBlank())
				{
					final Optional<ProcessHandle> resolved = ProcessHandle.of(Long.parseLong(pid.trim()));
					resolved.ifPresent(p -> PROCESS_CACHE.put(name, p));
					return resolved;
				}
			}
		}
		catch (Exception e)
		{
		}
		return Optional.empty();
	}
	
	private static void status(JLabel label, boolean online)
	{
		label.setText(online ? "● ONLINE" : "● OFFLINE");
		label.setForeground(online ? GREEN : RED);
	}
	
	private static String formatDuration(Instant time)
	{
		long seconds = Duration.between(time, Instant.now()).toSeconds();
		return (seconds / 3600) + "h " + ((seconds % 3600) / 60) + "min";
	}
	
	private static int percent(long used, long total)
	{
		return total <= 0 ? 0 : (int) Math.min(100, (used * 100) / total);
	}
	
	private static String human(long bytes)
	{
		return bytes <= 0 ? "--" : String.format(Locale.US, "%.1f MB", bytes / 1048576d);
	}
	
	private static long readXmx(Path file)
	{
		try
		{
			final Matcher match = XMX.matcher(Files.readString(file));
			if (match.find())
			{
				return (long) (Double.parseDouble(match.group(1)) * switch (match.group(2).toLowerCase(Locale.ROOT))
				{
					case "g" -> 1073741824d;
					case "m" -> 1048576d;
					default -> 1024d;
				});
			}
		}
		catch (IOException e)
		{
		}
		return 0;
	}
	
	private static JTextArea console()
	{
		final JTextArea text = new JTextArea();
		text.setEditable(false);
		text.setBackground(new Color(2, 17, 21));
		text.setForeground(new Color(146, 231, 191));
		text.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
		text.setBorder(new EmptyBorder(12, 12, 12, 12));
		return text;
	}
	
	private static JPanel card(java.awt.LayoutManager layout)
	{
		final JPanel panel = new JPanel(layout);
		panel.setBackground(CARD);
		panel.setBorder(BorderFactory.createLineBorder(BORDER));
		return panel;
	}
	
	private static void append(JTextArea area, String text)
	{
		SwingUtilities.invokeLater(() ->
		{
			area.append(text + System.lineSeparator());
			area.setCaretPosition(area.getDocument().getLength());
		});
	}
	
	private static int delaySeconds(String value)
	{
		return switch (value)
		{
			case "Em 5 segundos" -> 5;
			case "Em 30 segundos" -> 30;
			case "Em 60 segundos" -> 60;
			case "Em 10 minutos" -> 600;
			default -> 0;
		};
	}
	
	private static String readPort(Path file, String key, String fallback)
	{
		try
		{
			final Matcher match = Pattern.compile(String.format(PORT.pattern(), key)).matcher(Files.readString(file));
			return match.find() ? match.group(1) : fallback;
		}
		catch (IOException e)
		{
			return fallback;
		}
	}
	
	private static void openPath(Path path)
	{
		try
		{
			if (Files.exists(path))
			{
				java.awt.Desktop.getDesktop().open(path.toFile());
			}
		}
		catch (IOException | UnsupportedOperationException e)
		{
		}
	}
	
	private static Path findRoot()
	{
		try
		{
			final Path jar = Paths.get(Dashboard.class.getProtectionDomain().getCodeSource().getLocation().toURI());
			return jar.getParent().toAbsolutePath().normalize();
		}
		catch (Exception e)
		{
			return Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
		}
	}
	
	private static ImageIcon logo()
	{
		try
		{
			Image image = ImageIO.read(ROOT.resolve("images/splash.png").toFile());
			return new ImageIcon(image.getScaledInstance(86, 86, Image.SCALE_SMOOTH));
		}
		catch (IOException e)
		{
			return null;
		}
	}
	
	private static final class Gauge extends JPanel
	{
		private final String _title;
		private String _value;
		private String _detail;
		private int _percent;
		
		Gauge(String title, String value, String detail)
		{
			_title = title;
			_value = value;
			_detail = detail;
			setOpaque(false);
			setPreferredSize(new Dimension(205, 220));
		}
		
		void set(int percent, String value, String detail)
		{
			_percent = percent;
			_value = value;
			_detail = detail;
			repaint();
		}
		
		@Override
		protected void paintComponent(Graphics graphics)
		{
			super.paintComponent(graphics);
			Graphics2D g = (Graphics2D) graphics.create();
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			int size = Math.min(getWidth() - 30, getHeight() - 50);
			int x = (getWidth() - size) / 2;
			int y = 8;
			g.setStroke(new BasicStroke(18, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.setColor(new Color(25, 71, 79));
			g.drawArc(x, y, size, size, 90, -360);
			g.setColor(GREEN);
			g.drawArc(x, y, size, size, 90, -Math.max(1, (_percent * 360) / 100));
			g.setColor(MUTED);
			g.setFont(getFont().deriveFont(13f));
			drawCentered(g, _title, getWidth() / 2, (y + (size / 2)) - 25);
			g.setColor(WHITE);
			g.setFont(getFont().deriveFont(Font.BOLD, 32f));
			drawCentered(g, _value, getWidth() / 2, y + (size / 2) + 24);
			g.setColor(MUTED);
			g.setFont(getFont().deriveFont(11f));
			drawCentered(g, _detail, getWidth() / 2, y + size + 25);
			g.dispose();
		}
		
		private static void drawCentered(Graphics2D g, String text, int x, int y)
		{
			g.drawString(text, x - (g.getFontMetrics().stringWidth(text) / 2), y);
		}
	}
	
	private record ProcessMetrics(int threads, long heap)
	{
		private static final ProcessMetrics EMPTY = new ProcessMetrics(0, 0);
	}
}
