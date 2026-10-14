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

import java.util.function.Function;
import java.util.function.IntFunction;

import com.l2journey.commons.util.StringUtil;
import com.l2journey.gameserver.model.WorldObject;
import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.actor.Player;

/**
 * Resolves an admin getbuffs target token to a creature.
 * <p>
 * Integer tokens are treated as object ids so NPC paging and back buttons work. Non-integer tokens are treated as online player names.
 */
public final class BuffTargetResolver
{
	private BuffTargetResolver()
	{
	}
	
	/**
	 * Resolves {@code token} to a creature.
	 * @param token object id or player name
	 * @param findObject lookup by object id
	 * @param findPlayer lookup by player name
	 * @return the matching creature, or {@code null} if none matches
	 */
	public static Creature resolve(String token, IntFunction<WorldObject> findObject, Function<String, Player> findPlayer)
	{
		if (StringUtil.isInteger(token))
		{
			final WorldObject object = findObject.apply(Integer.parseInt(token));
			if ((object != null) && object.isCreature())
			{
				return object.asCreature();
			}
			return null;
		}
		
		return findPlayer.apply(token);
	}
}
