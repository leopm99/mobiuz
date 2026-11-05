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
package com.l2journey.gameserver.util;

import com.l2journey.gameserver.model.actor.Creature;

/**
 * Builds the admin HTML bypass used by the Effects page Back button.
 * <p>
 * Returns the command that reopens the panel the GM came from (character info, NPC info, or summon info) instead of always opening the GM's own character panel.
 */
public final class BuffEffectsBackBypass
{
	private static final String ADMIN_MAIN = "admin_admin";
	
	private BuffEffectsBackBypass()
	{
	}
	
	/**
	 * @param target creature whose effects are being viewed; may be {@code null}
	 * @return admin bypass without the {@code bypass} prefix
	 */
	public static String forTarget(Creature target)
	{
		if (target == null)
		{
			return ADMIN_MAIN;
		}
		if (target.isPlayer())
		{
			return "admin_character_info " + target.getName();
		}
		if (target.isNpc())
		{
			return "admin_npcinfo " + target.getObjectId();
		}
		if (target.isSummon())
		{
			return "admin_summon_info";
		}
		return ADMIN_MAIN;
	}
}
