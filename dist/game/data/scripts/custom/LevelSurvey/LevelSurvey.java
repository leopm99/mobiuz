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
package custom.LevelSurvey;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.l2journey.commons.util.IXmlReader;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.events.EventType;
import com.l2journey.gameserver.model.events.ListenerRegisterType;
import com.l2journey.gameserver.model.events.annotations.RegisterEvent;
import com.l2journey.gameserver.model.events.annotations.RegisterType;
import com.l2journey.gameserver.model.events.holders.actor.player.OnPlayerLevelChanged;
import com.l2journey.gameserver.model.item.enums.ItemProcessType;
import com.l2journey.gameserver.model.quest.Quest;

/**
 * Level Reward System Reads configurations from an XML file and gives rewards when players reach specific levels Features: - Uses 'character_variables' table to persist already received rewards - Each level has a unique variable per player (LEVEL_SURVEY_X) - Prevents item accumulation if player
 * loses and gains level again - Persists between logouts and server reboots
 * @author KingHanker
 */
public class LevelSurvey extends Quest implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(LevelSurvey.class.getName());
	private static final Map<Integer, LevelReward> LEVEL_REWARDS = new HashMap<>();
	private static boolean SYSTEM_ENABLED = true;
	
	public LevelSurvey()
	{
		super(-1);
		load();
	}
	
	@Override
	public void load()
	{
		LEVEL_REWARDS.clear();
		parseFile(new File("config/player/levelsurvey.xml"));
		
		if (SYSTEM_ENABLED)
		{
			LOGGER.info(() -> "Level Survey System loaded with " + LEVEL_REWARDS.size() + " level rewards.");
		}
		else
		{
			LOGGER.info("Level Survey System is disabled.");
		}
	}
	
	@Override
	public void parseDocument(Document doc, File f)
	{
		// Check if system is enabled
		final NamedNodeMap listAttrs = doc.getDocumentElement().getAttributes();
		SYSTEM_ENABLED = parseBoolean(listAttrs, "enableSystem", true);
		
		if (!SYSTEM_ENABLED)
		{
			LOGGER.info("Level Survey System is disabled in configuration.");
			return;
		}
		
		forEach(doc, "list", listNode -> forEach(listNode, "level", this::parseLevelReward));
	}
	
	private void parseLevelReward(Node levelNode)
	{
		final NamedNodeMap attrs = levelNode.getAttributes();
		final int level = parseInteger(attrs, "id");
		final String message = parseString(attrs, "message", "Congratulations on reaching level " + level + "!");
		
		final List<ItemReward> items = new ArrayList<>();
		forEach(levelNode, "item", itemNode ->
		{
			final NamedNodeMap itemAttrs = itemNode.getAttributes();
			final int itemId = parseInteger(itemAttrs, "id");
			final int count = parseInteger(itemAttrs, "count");
			items.add(new ItemReward(itemId, count));
		});
		
		LEVEL_REWARDS.put(level, new LevelReward(level, message, items));
	}
	
	@RegisterEvent(EventType.ON_PLAYER_LEVEL_CHANGED)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void onPlayerLevelChanged(OnPlayerLevelChanged event)
	{
		// Check if system is enabled
		if (!SYSTEM_ENABLED)
		{
			return;
		}
		
		final Player player = event.getPlayer();
		final int newLevel = player.getLevel();
		
		// Check if there are rewards for this level
		final LevelReward levelReward = LEVEL_REWARDS.get(newLevel);
		if (levelReward == null)
		{
			return;
		}
		
		// Check if player has already received rewards for this level
		// Uses character_variables table to persist between logouts/reboots
		final String varName = "LEVEL_SURVEY_" + newLevel;
		if (player.getVariables().getBoolean(varName, false))
		{
			return;
		}
		
		// Check inventory space
		for (ItemReward item : levelReward.getItems())
		{
			if (!player.getInventory().validateCapacityByItemId(item.getId(), item.getCount()))
			{
				player.sendMessage("Your inventory is full! Please make space to receive level rewards.");
				return;
			}
		}
		
		// Give rewards
		for (ItemReward item : levelReward.getItems())
		{
			player.addItem(ItemProcessType.NONE, item.getId(), item.getCount(), null, true);
		}
		
		// Mark as already received in character_variables table
		player.getVariables().set(varName, true);
		
		// Custom message
		player.sendMessage(levelReward.getMessage());
	}
	
	public static void main(String[] args)
	{
		new LevelSurvey();
	}
	
	/**
	 * Class to store level reward data
	 */
	private static class LevelReward
	{
		private final String message;
		private final List<ItemReward> items;
		
		public LevelReward(int level, String message, List<ItemReward> items)
		{
			this.message = message;
			this.items = items;
		}
		
		public String getMessage()
		{
			return message;
		}
		
		public List<ItemReward> getItems()
		{
			return items;
		}
	}
	
	/**
	 * Class to store item reward data
	 */
	private static class ItemReward
	{
		private final int id;
		private final int count;
		
		public ItemReward(int id, int count)
		{
			this.id = id;
			this.count = count;
		}
		
		public int getId()
		{
			return id;
		}
		
		public int getCount()
		{
			return count;
		}
	}
}
