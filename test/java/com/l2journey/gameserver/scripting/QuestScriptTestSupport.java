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
package com.l2journey.gameserver.scripting;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.mockito.MockedStatic;

import com.l2journey.Config;
import com.l2journey.gameserver.cache.HtmCache;
import com.l2journey.gameserver.data.xml.ItemData;
import com.l2journey.gameserver.data.xml.NpcData;
import com.l2journey.gameserver.managers.QuestManager;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.templates.NpcTemplate;
import com.l2journey.gameserver.model.events.listeners.AbstractEventListener;
import com.l2journey.gameserver.model.item.ItemTemplate;
import com.l2journey.gameserver.model.quest.Quest;

/**
 * Test harness for constructing datapack {@link Quest} scripts without loading the full game datapack.
 * <p>
 * Script tests must not run under JUnit parallel execution: {@link QuestManager} is a JVM-wide singleton.
 */
public final class QuestScriptTestSupport
{
	private QuestScriptTestSupport()
	{
	}

	/**
	 * Holds a constructed quest and the static mocks required for its lifetime.
	 * {@link #close()} always unloads the quest from {@link QuestManager} and closes the mocks.
	 * @param <T> quest type
	 */
	public static final class Scope<T extends Quest> implements AutoCloseable
	{
		private final T _quest;
		private final List<MockedStatic<?>> _mocks;
		private final File _previousScriptRoot;
		private final boolean _restoreScriptRoot;

		private Scope(T quest, List<MockedStatic<?>> mocks, File previousScriptRoot, boolean restoreScriptRoot)
		{
			_quest = quest;
			_mocks = mocks;
			_previousScriptRoot = previousScriptRoot;
			_restoreScriptRoot = restoreScriptRoot;
		}

		public T getQuest()
		{
			return _quest;
		}

		@Override
		public void close()
		{
			try
			{
				QuestManager.getInstance().unload(_quest);
			}
			finally
			{
				for (int i = _mocks.size() - 1; i >= 0; i--)
				{
					_mocks.get(i).close();
				}
				if (_restoreScriptRoot)
				{
					Config.SCRIPT_ROOT = _previousScriptRoot;
				}
			}
		}
	}

	/**
	 * Opens static mocks, constructs a quest, and returns a scope that cleans up on close.
	 * Keep the scope open for the whole test body (including {@code onTalk} / {@code onEvent} calls).
	 * @param <T> quest type
	 * @param factory quest constructor (typically {@code SomeQuest::new})
	 * @return open scope owning the quest and mocks
	 */
	public static <T extends Quest> Scope<T> createQuest(Supplier<T> factory)
	{
		// ScriptEngineManager static fields call Config.SCRIPT_ROOT.toPath() during class init.
		final File previousScriptRoot = Config.SCRIPT_ROOT;
		final boolean restoreScriptRoot = previousScriptRoot == null;
		if (restoreScriptRoot)
		{
			Config.SCRIPT_ROOT = new File("dist/game/data/scripts");
		}

		final List<MockedStatic<?>> mocks = new ArrayList<>(4);
		try
		{
			final MockedStatic<ScriptEngineManager> scriptEngine = mockStatic(ScriptEngineManager.class);
			mocks.add(scriptEngine);

			final ScriptEngineManager scriptEngineManager = mock(ScriptEngineManager.class);
			when(scriptEngineManager.getCurrentLoadingScript()).thenReturn(Path.of("test-script.java"));
			scriptEngine.when(ScriptEngineManager::getInstance).thenReturn(scriptEngineManager);

			final MockedStatic<NpcData> npcDataStatic = mockStatic(NpcData.class);
			mocks.add(npcDataStatic);

			final NpcData npcData = mock(NpcData.class);
			final NpcTemplate npcTemplate = mock(NpcTemplate.class);
			final AbstractEventListener eventListener = mock(AbstractEventListener.class);

			when(npcTemplate.addListener(any())).thenReturn(eventListener);
			when(npcData.getTemplate(anyInt())).thenReturn(npcTemplate);
			npcDataStatic.when(NpcData::getInstance).thenReturn(npcData);

			final MockedStatic<ItemData> itemDataStatic = mockStatic(ItemData.class);
			mocks.add(itemDataStatic);

			final ItemData itemData = mock(ItemData.class);
			final ItemTemplate itemTemplate = mock(ItemTemplate.class);
			when(itemData.getTemplate(anyInt())).thenReturn(itemTemplate);
			itemDataStatic.when(ItemData::getInstance).thenReturn(itemData);

			final MockedStatic<HtmCache> htmCacheStatic = mockStatic(HtmCache.class);
			mocks.add(htmCacheStatic);

			final HtmCache htmCache = mock(HtmCache.class);
			when(htmCache.getHtm(nullable(Player.class), anyString())).thenReturn(null);
			htmCacheStatic.when(HtmCache::getInstance).thenReturn(htmCache);

			return new Scope<>(factory.get(), mocks, previousScriptRoot, restoreScriptRoot);
		}
		catch (RuntimeException | Error e)
		{
			for (int i = mocks.size() - 1; i >= 0; i--)
			{
				mocks.get(i).close();
			}
			if (restoreScriptRoot)
			{
				Config.SCRIPT_ROOT = previousScriptRoot;
			}
			throw e;
		}
	}
}
