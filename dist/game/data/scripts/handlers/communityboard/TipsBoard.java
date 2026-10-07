/*
 * Copyright (c) 2025 L2Journey Project
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package handlers.communityboard;

import java.io.File;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.l2journey.Config;
import com.l2journey.commons.threads.ThreadPool;
import com.l2journey.commons.util.IXmlReader;

/**
 * Carrega e fornece a dica exibida no banner "DICA DO DIA" do Community Board.
 * <p>
 * As dicas sao carregadas uma unica vez na inicializacao do servidor a partir de {@code ./config/CommunityTips.xml} e uma delas e sorteada como a "dica ativa" para todos os jogadores online (mesma dica para todos durante o uptime do servidor).
 * <p>
 * Para mudar a dica em runtime, chame {@link #load()} novamente (sorteia outra).
 * @author KingHanker
 */
public class TipsBoard implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(TipsBoard.class.getName());
	private static final String CONFIG_FILE = "./config/CommunityTips.xml";
	
	private static final String DEFAULT_TITLE = "DICA DO DIA";
	private static final String DEFAULT_ICON = "L2UI.MainStatus_large";
	private static final String DEFAULT_COLOR = "AE9977";
	private static final String DEFAULT_HIGHLIGHT = "CDB67F";
	private static final String TITLE_COLOR = "F2C75C";
	
	private final List<Tip> _tips = new ArrayList<>();
	
	/** HTML pre-renderizado da dica ativa (compartilhada entre todos os jogadores). */
	private volatile String _activeTipHtml = "";
	
	protected TipsBoard()
	{
		load();
		scheduleMidnightRotation();
	}
	
	/**
	 * Agenda a rotacao diaria da dica para 00:00 do dia seguinte e, dai em diante, a cada 24h.
	 */
	private void scheduleMidnightRotation()
	{
		final LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
		final LocalDateTime nextMidnight = LocalDateTime.of(LocalDate.now(ZoneId.systemDefault()).plusDays(1), LocalTime.MIDNIGHT);
		final long initialDelay = Math.max(1000L, Duration.between(now, nextMidnight).toMillis());
		ThreadPool.scheduleAtFixedRate(this::rotateTip, initialDelay, TimeUnit.DAYS.toMillis(1));
	}
	
	/**
	 * Sorteia uma nova dica ativa entre as ja carregadas (sem reler o XML).
	 */
	public void rotateTip()
	{
		if (_tips.isEmpty())
		{
			_activeTipHtml = "";
			return;
		}
		final Tip active = _tips.get(ThreadLocalRandom.current().nextInt(_tips.size()));
		_activeTipHtml = renderTip(active);
		LOGGER.info(getClass().getSimpleName() + ": Daily tip rotated.");
	}
	
	@Override
	public void load()
	{
		_tips.clear();
		_activeTipHtml = "";
		if (!Config.COMMUNITYBOARD_TIPS_ENABLED)
		{
			return;
		}
		
		final File file = new File(CONFIG_FILE);
		if (!file.exists())
		{
			LOGGER.warning(getClass().getSimpleName() + ": " + CONFIG_FILE + " not found.");
			return;
		}
		
		parseFile(file);
		
		if (!_tips.isEmpty())
		{
			final Tip active = _tips.get(ThreadLocalRandom.current().nextInt(_tips.size()));
			_activeTipHtml = renderTip(active);
		}
		
		LOGGER.info(() -> getClass().getSimpleName() + ": Loaded " + _tips.size() + " community board tips.");
	}
	
	@Override
	public boolean isValidating()
	{
		return false;
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if (!"list".equalsIgnoreCase(n.getNodeName()))
			{
				continue;
			}
			
			final NodeList children = n.getChildNodes();
			for (int i = 0; i < children.getLength(); i++)
			{
				final Node c = children.item(i);
				if (!"tip".equalsIgnoreCase(c.getNodeName()))
				{
					continue;
				}
				
				final Element el = (Element) c;
				final String text = el.getTextContent() != null ? el.getTextContent().trim() : "";
				if (text.isEmpty())
				{
					continue;
				}
				
				final String title = el.hasAttribute("title") ? el.getAttribute("title") : DEFAULT_TITLE;
				final String icon = el.hasAttribute("icon") ? el.getAttribute("icon") : DEFAULT_ICON;
				final String color = el.hasAttribute("color") ? el.getAttribute("color") : DEFAULT_COLOR;
				final String highlight = el.hasAttribute("highlight") ? el.getAttribute("highlight") : DEFAULT_HIGHLIGHT;
				_tips.add(new Tip(title, icon, color, highlight, text));
			}
		}
	}
	
	/**
	 * Returns the rendered HTML block of the currently active tip (same for every player). Returns an empty string if the feature is disabled or there are no tips loaded.
	 * @return the HTML block or an empty string
	 */
	public String getActiveTipHtml()
	{
		return _activeTipHtml;
	}
	
	private String renderTip(Tip tip)
	{
		final String body = tip.text.replace("[b]", "</font><font color=\"" + tip.highlight + "\">").replace("[/b]", "</font><font color=\"" + tip.color + "\">");
		
		final StringBuilder sb = new StringBuilder(512);
		sb.append("<table width=720 height=55 cellpadding=0 cellspacing=0 border=0 background=\"L2UI_CT1.Windows_DF_TooltipBG\">");
		sb.append("<tr><td height=12></td></tr>");
		sb.append("<tr><td fixwidth=720 height=46 valign=center>");
		sb.append("<table width=720 cellpadding=4 cellspacing=0 border=0><tr>");
		sb.append("<td width=46 align=center valign=center>");
		sb.append("<img src=\"").append(tip.icon).append("\" width=32 height=32>");
		sb.append("</td>");
		sb.append("<td width=560 align=left valign=center>");
		sb.append("<font color=\"").append(TITLE_COLOR).append("\">").append(tip.title).append("</font><br1>");
		sb.append("<font color=\"").append(tip.color).append("\">").append(body).append("</font>");
		sb.append("</td></tr></table>");
		sb.append("</td></tr></table>");
		return sb.toString();
	}
	
	private static final class Tip
	{
		final String title;
		final String icon;
		final String color;
		final String highlight;
		final String text;
		
		Tip(String title, String icon, String color, String highlight, String text)
		{
			this.title = title;
			this.icon = icon;
			this.color = color;
			this.highlight = highlight;
			this.text = text;
		}
	}
	
	public static TipsBoard getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final TipsBoard INSTANCE = new TipsBoard();
	}
}
