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
 * 
 * ---
 * 
 * Portions of this software are derived from the L2JMobius Project, 
 * shared under the MIT License. The original license terms are preserved where 
 * applicable..
 * 
 */
package com.l2journey.gameserver.network.serverpackets;

import com.l2journey.gameserver.cache.HtmCache;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.network.PacketLogger;
import com.l2journey.gameserver.network.enums.HtmlActionScope;
import com.l2journey.gameserver.util.HtmlUtil;

/**
 * @author HorridoJoho
 */
public abstract class AbstractHtmlPacket extends ServerPacket
{
	public static final char VAR_PARAM_START_CHAR = '$';
	
	private final int _npcObjId;
	private String _html = null;
	private boolean _disabledValidation = false;
	
	protected AbstractHtmlPacket()
	{
		_npcObjId = 0;
	}
	
	protected AbstractHtmlPacket(int npcObjId)
	{
		if (npcObjId < 0)
		{
			throw new IllegalArgumentException();
		}
		_npcObjId = npcObjId;
	}
	
	protected AbstractHtmlPacket(String html)
	{
		_npcObjId = 0;
		setHtml(html);
	}
	
	protected AbstractHtmlPacket(int npcObjId, String html)
	{
		if (npcObjId < 0)
		{
			throw new IllegalArgumentException();
		}
		_npcObjId = npcObjId;
		setHtml(html);
	}
	
	public void disableValidation()
	{
		_disabledValidation = true;
	}
	
	public void setHtml(String html)
	{
		if (html.length() > 17200)
		{
			PacketLogger.warning(getClass().getSimpleName() + ": Html is too long! this will crash the client!");
			_html = html.substring(0, 17200);
		}
		else
		{
			if (!html.contains("<html") && !html.startsWith("..\\L2"))
			{
				_html = "<html><body>" + html + "</body></html>";
			}
			else
			{
				_html = html;
			}
		}
	}
	
	public boolean setFile(Player player, String path)
	{
		final String content = HtmCache.getInstance().getHtm(player, path);
		if (content == null)
		{
			setHtml("<html><body>My Text is missing:<br>" + path + "</body></html>");
			PacketLogger.warning(getClass().getSimpleName() + ": Missing html page " + path);
			return false;
		}
		setHtml(content);
		return true;
	}
	
	public void replace(String pattern, String value)
	{
		_html = _html.replaceAll(pattern, value.replace("$", "\\$"));
	}
	
	public void replace(String pattern, CharSequence value)
	{
		replace(pattern, String.valueOf(value));
	}
	
	public void replace(String pattern, boolean value)
	{
		replace(pattern, String.valueOf(value));
	}
	
	public void replace(String pattern, int value)
	{
		replace(pattern, String.valueOf(value));
	}
	
	public void replace(String pattern, long value)
	{
		replace(pattern, String.valueOf(value));
	}
	
	public void replace(String pattern, double value)
	{
		replace(pattern, String.valueOf(value));
	}
	
	@Override
	public void runImpl(Player player)
	{
		if (player != null)
		{
			player.clearHtmlActions(getScope());
		}
		if (_disabledValidation)
		{
			return;
		}
		if (player != null)
		{
			HtmlUtil.buildHtmlActionCache(player, getScope(), _npcObjId, _html);
		}
	}
	
	public int getNpcObjId()
	{
		return _npcObjId;
	}
	
	public String getHtml()
	{
		return _html;
	}
	
	public abstract HtmlActionScope getScope();
}
