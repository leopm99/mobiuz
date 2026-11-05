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
package com.l2journey.gameserver.model;

/**
 * Action Key DTO.
 * @author mrTJO, Zoey76
 */
public class ActionKey
{
	private final int _cat;
	private int _cmd = 0;
	private int _key = 0;
	private int _tgKey1 = 0;
	private int _tgKey2 = 0;
	private int _show = 1;
	
	/**
	 * @param cat category Id
	 */
	public ActionKey(int cat)
	{
		_cat = cat;
	}
	
	/**
	 * ActionKey Initialization
	 * @param cat Category ID
	 * @param cmd Command ID
	 * @param key User Defined Primary Key
	 * @param tgKey1 1st Toggled Key (eg. Alt, Ctrl or Shift)
	 * @param tgKey2 2nd Toggled Key (eg. Alt, Ctrl or Shift)
	 * @param show Show Action in UI
	 */
	public ActionKey(int cat, int cmd, int key, int tgKey1, int tgKey2, int show)
	{
		_cat = cat;
		_cmd = cmd;
		_key = key;
		_tgKey1 = tgKey1;
		_tgKey2 = tgKey2;
		_show = show;
	}
	
	public int getCategory()
	{
		return _cat;
	}
	
	public int getCommandId()
	{
		return _cmd;
	}
	
	public void setCommandId(int cmd)
	{
		_cmd = cmd;
	}
	
	public int getKeyId()
	{
		return _key;
	}
	
	public void setKeyId(int key)
	{
		_key = key;
	}
	
	public int getToogleKey1()
	{
		return _tgKey1;
	}
	
	public void setToogleKey1(int tKey1)
	{
		_tgKey1 = tKey1;
	}
	
	public int getToogleKey2()
	{
		return _tgKey2;
	}
	
	public void setToogleKey2(int tKey2)
	{
		_tgKey2 = tKey2;
	}
	
	public int getShowStatus()
	{
		return _show;
	}
	
	public void setShowStatus(int show)
	{
		_show = show;
	}
}
