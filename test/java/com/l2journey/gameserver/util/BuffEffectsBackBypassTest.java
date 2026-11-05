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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.Summon;

/**
 * Unit tests for {@link BuffEffectsBackBypass}.
 */
class BuffEffectsBackBypassTest
{
	@Test
	void nullTargetReturnsAdminMain()
	{
		assertEquals("admin_admin", BuffEffectsBackBypass.forTarget(null));
	}
	
	@Test
	void playerReturnsCharacterInfoBypass()
	{
		final Player player = mock(Player.class);
		when(player.isPlayer()).thenReturn(true);
		when(player.getName()).thenReturn("DEUS");
		
		assertEquals("admin_character_info DEUS", BuffEffectsBackBypass.forTarget(player));
	}
	
	@Test
	void npcReturnsNpcInfoBypass()
	{
		final Npc npc = mock(Npc.class);
		when(npc.isPlayer()).thenReturn(false);
		when(npc.isNpc()).thenReturn(true);
		when(npc.getObjectId()).thenReturn(268461369);
		
		assertEquals("admin_npcinfo 268461369", BuffEffectsBackBypass.forTarget(npc));
	}
	
	@Test
	void summonReturnsSummonInfoBypass()
	{
		final Summon summon = mock(Summon.class);
		when(summon.isPlayer()).thenReturn(false);
		when(summon.isNpc()).thenReturn(false);
		when(summon.isSummon()).thenReturn(true);
		
		assertEquals("admin_summon_info", BuffEffectsBackBypass.forTarget(summon));
	}
	
	@Test
	void otherCreatureReturnsAdminMain()
	{
		final Creature creature = mock(Creature.class);
		when(creature.isPlayer()).thenReturn(false);
		when(creature.isNpc()).thenReturn(false);
		when(creature.isSummon()).thenReturn(false);
		
		assertEquals("admin_admin", BuffEffectsBackBypass.forTarget(creature));
	}
}
