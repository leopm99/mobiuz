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
package quests.Q00627_HeartInSearchOfPower;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.quest.QuestState;
import com.l2journey.gameserver.model.quest.State;
import com.l2journey.gameserver.scripting.QuestScriptTestSupport;
import com.l2journey.gameserver.scripting.QuestScriptTestSupport.Scope;

/**
 * CREATED-state level-gate coverage for {@link Q00627_HeartInSearchOfPower}.
 * <p>
 * Do not enable JUnit parallel execution for script tests ({@code QuestManager} is process-wide).
 */
class Q00627_HeartInSearchOfPowerTest
{
	private static final int MYSTERIOUS_NECROMANCER = 31518;

	@Test
	void onTalkCreatedRejectsUnderLevelPlayer()
	{
		try (Scope<Q00627_HeartInSearchOfPower> scope = QuestScriptTestSupport.createQuest(Q00627_HeartInSearchOfPower::new))
		{
			final Q00627_HeartInSearchOfPower quest = scope.getQuest();
			final Player player = mock(Player.class);
			final Npc npc = mock(Npc.class);
			final QuestState qs = new QuestState(quest, player, State.CREATED);

			when(player.getQuestState(quest.getName())).thenReturn(qs);
			when(player.getLevel()).thenReturn(59);
			when(npc.getId()).thenReturn(MYSTERIOUS_NECROMANCER);

			assertEquals("31518-00.htm", quest.onTalk(npc, player));
		}
	}

	@Test
	void onTalkCreatedOffersQuestAtMinimumLevel()
	{
		try (Scope<Q00627_HeartInSearchOfPower> scope = QuestScriptTestSupport.createQuest(Q00627_HeartInSearchOfPower::new))
		{
			final Q00627_HeartInSearchOfPower quest = scope.getQuest();
			final Player player = mock(Player.class);
			final Npc npc = mock(Npc.class);
			final QuestState qs = new QuestState(quest, player, State.CREATED);

			when(player.getQuestState(quest.getName())).thenReturn(qs);
			when(player.getLevel()).thenReturn(60);
			when(npc.getId()).thenReturn(MYSTERIOUS_NECROMANCER);

			assertEquals("31518-01.htm", quest.onTalk(npc, player));
		}
	}
}
