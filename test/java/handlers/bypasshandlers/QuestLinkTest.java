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
package handlers.bypasshandlers;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.l2journey.gameserver.managers.QuestManager;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.events.EventType;
import com.l2journey.gameserver.model.events.listeners.AbstractEventListener;
import com.l2journey.gameserver.model.quest.Quest;
import com.l2journey.gameserver.scripting.QuestScriptTestSupport;
import com.l2journey.gameserver.scripting.QuestScriptTestSupport.Scope;

/**
 * Quest link bypass when tutorial quest 255 is the NPC's only quest (#58).
 * <p>
 * Do not enable JUnit parallel execution for script tests ({@code QuestManager} is process-wide).
 */
class QuestLinkTest
{
	private static final int MITRAELL = 30129;

	/**
	 * Tutorial stand-in (id 255). Distinct simple name so {@link QuestManager} keys do not collide.
	 */
	private static final class TutorialQuest extends Quest
	{
		private boolean _talked;

		TutorialQuest()
		{
			super(255);
		}

		@Override
		public String onTalk(Npc npc, Player talker)
		{
			_talked = true;
			return "talked.htm";
		}

		boolean wasTalked()
		{
			return _talked;
		}
	}

	/**
	 * Non-tutorial stand-in (id 101). Distinct simple name so {@link QuestManager} keys do not collide.
	 */
	private static final class RealQuest extends Quest
	{
		private boolean _talked;

		RealQuest()
		{
			super(101);
		}

		@Override
		public String onTalk(Npc npc, Player talker)
		{
			_talked = true;
			return "talked.htm";
		}

		boolean wasTalked()
		{
			return _talked;
		}
	}

	@Test
	void questLinkOnTutorialOnlyNpcShowsNoQuestDialog()
	{
		try (Scope<TutorialQuest> scope = QuestScriptTestSupport.createQuest(TutorialQuest::new))
		{
			final TutorialQuest tutorial = scope.getQuest();
			final Player player = mock(Player.class);
			final Npc npc = mock(Npc.class);
			final AbstractEventListener startListener = mock(AbstractEventListener.class);

			when(npc.asNpc()).thenReturn(npc);
			when(npc.getId()).thenReturn(MITRAELL);
			when(npc.getObjectId()).thenReturn(1);
			when(npc.getListeners(EventType.ON_NPC_QUEST_START)).thenReturn(List.of(startListener));
			when(startListener.getOwner()).thenReturn(tutorial);
			when(player.isInventoryUnder90(true)).thenReturn(true);
			when(player.getAllActiveQuests()).thenReturn(Collections.emptyList());

			new QuestLink().useBypass("Quest", player, npc);

			verify(npc).insertObjectIdAndShowChatWindow(eq(player), contains("not on a quest"));
			assertFalse(tutorial.wasTalked());
		}
	}

	@Test
	void questLinkWithTutorialAndRealQuestShowsChooseWindow()
	{
		try (Scope<TutorialQuest> scope = QuestScriptTestSupport.createQuest(TutorialQuest::new))
		{
			final TutorialQuest tutorial = scope.getQuest();
			final RealQuest realQuest = new RealQuest();
			try
			{
				final Player player = mock(Player.class);
				final Npc npc = mock(Npc.class);
				final AbstractEventListener tutorialStart = mock(AbstractEventListener.class);
				final AbstractEventListener realStart = mock(AbstractEventListener.class);

				when(npc.asNpc()).thenReturn(npc);
				when(npc.getId()).thenReturn(MITRAELL);
				when(npc.getObjectId()).thenReturn(1);
				when(npc.getListeners(EventType.ON_NPC_QUEST_START)).thenReturn(List.of(tutorialStart, realStart));
				when(tutorialStart.getOwner()).thenReturn(tutorial);
				when(realStart.getOwner()).thenReturn(realQuest);
				when(player.isInventoryUnder90(true)).thenReturn(true);
				when(player.getAllActiveQuests()).thenReturn(Collections.emptyList());
				when(player.getQuestState(anyString())).thenReturn(null);

				new QuestLink().useBypass("Quest", player, npc);

				verify(npc).insertObjectIdAndShowChatWindow(eq(player), contains(realQuest.getName()));
				assertFalse(tutorial.wasTalked());
				assertFalse(realQuest.wasTalked());
			}
			finally
			{
				QuestManager.getInstance().unload(realQuest);
			}
		}
	}
}
