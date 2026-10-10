package com.l2journey.gameserver.model.phantom;

import java.util.ArrayList;
import java.util.List;

import com.l2journey.commons.util.Rnd;
import com.l2journey.gameserver.data.xml.ExperienceData;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.enums.player.PlayerClass;
import com.l2journey.gameserver.model.item.enums.ItemProcessType;
import com.l2journey.gameserver.model.item.instance.Item;

public class PhantomProgression
{
	private static final int KARMIAN_TUNIC = 438;
	private static final int KARMIAN_STOCKINGS = 470;
	
	private static final String VAR_LEVEL_CAP = "PHANTOM_MAX_LEVEL_CAP";
	private static final PlayerClass[] PLAYER_CLASS_VALUES = PlayerClass.values();
	
	private static int phantomCounter = 0;
	private PhantomProgression()
	{
	}

	public static void checkProgression(Player bot)
	{
		if (bot == null)
		{
			return;
		}
		
		enforceLevelCap(bot);
		checkLowLevelItems(bot);
		handleClassChange(bot, bot.getLevel());
	}
	
	// LEVEL CAP POR QUANTIDADE EXATA DE BOTS
	private static void enforceLevelCap(Player bot)
	{
		if (!bot.getVariables().hasVariable(VAR_LEVEL_CAP))
		{
			assignLevelCapByQuantity(bot);
		}
		
		int levelCap = bot.getVariables().getInt(VAR_LEVEL_CAP, 85);
		
		if (bot.getLevel() >= levelCap)
		{
			long maxExpForCap = ExperienceData.getInstance().getExpForLevel(levelCap);
			if (bot.getExp() > maxExpForCap)
			{
				bot.getStat().setExp(maxExpForCap);
			}
			
			if (!bot.getVariables().getBoolean("expoff_checked", false))
			{
				bot.getVariables().set("expoff", "true");
				bot.getVariables().set("expoff_checked", true);
				
				PhantomManager.logToFile(bot.getName(), "Reached the level cap (Level " + levelCap + "). Exp permanently blocked.");
			}
		}
	}
	
	private static synchronized void assignLevelCapByQuantity(Player bot)
	{
		phantomCounter++;
		int cap;
		
		if (phantomCounter <= 5)
		{
			cap = 10; // 1-5: Lvl 10 (Grau NG)
		}
		else if (phantomCounter <= 10)
		{
			cap = 21; // 6-10: Lvl 20 (Grau D)
		}
		else if (phantomCounter <= 15)
		{
			cap = 35; // 11-15: Lvl 35 (Grau C inicial)
		}
		else if (phantomCounter <= 20)
		{
			cap = 41; // 16-20: Lvl 40 (Grau C)
		}
		else if (phantomCounter <= 25)
		{
			cap = 53; // 21-25: Lvl 52 (Grau B)
		}
		else if (phantomCounter <= 30) // Corrigido para 30 (5 bots)
		{
			cap = 62; // 26-30: Lvl 62 (Grau A)
		}
		else
		{
			cap = 85; // 31-60: Lvl 85 (Grau S)
		}
		
		// Reiniciamos o ciclo ao chegar a 60 bots (corrigido)
		if (phantomCounter >= 60)
		{
			phantomCounter = 0;
		}
		
		bot.getVariables().set(VAR_LEVEL_CAP, cap);
		
		PhantomManager.logToFile(bot.getName(), "Level cap assigned: Level " + cap + " (Bot #" + phantomCounter + " of the cycle)");
	}
	
	static void checkLowLevelItems(Player bot)
	{
		// Only strip Karmian leftovers from NG mages. D-grade robe kits (20-39) intentionally
		// include Mithril Stockings (470); destroying them here re-adds them every AI tick and OOMs.
		if (bot.isMageClass() && (bot.getLevel() < 20))
		{
			List<Item> itemsToDestroy = new ArrayList<>();
			
			for (Item item : bot.getInventory().getItems())
			{
				if ((item != null) && ((item.getId() == KARMIAN_TUNIC) || (item.getId() == KARMIAN_STOCKINGS)))
				{
					itemsToDestroy.add(item);
				}
			}
			
			for (Item item : itemsToDestroy)
			{
				if (item.isEquipped())
				{
					bot.getInventory().unEquipItemInSlot(item.getLocationSlot());
				}
				bot.destroyItem(ItemProcessType.DESTROY, item, bot, false);
			}
		}
	}
	
	private static void handleClassChange(Player bot, int lvl)
	{
		PlayerClass currentClass = bot.getPlayerClass();
		if (currentClass == null)
		{
			return;
		}
		
		int classLevel = currentClass.level();
		boolean needsClassChange = ((lvl >= 20) && (classLevel == 0)) || ((lvl >= 40) && (classLevel == 1)) || ((lvl >= 76) && (classLevel == 2)) || ((lvl >= 85) && (classLevel == 3));
		
		if (!needsClassChange)
		{
			return;
		}
		
		List<PlayerClass> nextClasses = new ArrayList<>();
		for (PlayerClass cid : PLAYER_CLASS_VALUES)
		{
			if (cid.getParent() == currentClass)
			{
				nextClasses.add(cid);
			}
		}
		
		if (nextClasses.isEmpty())
		{
			return;
		}
		
		PlayerClass newClass = null;
		int chance = Rnd.get(100);
		
		switch (currentClass)
		{
			case MAGE:
				newClass = (chance < 85) ? PlayerClass.WIZARD : PlayerClass.CLERIC;
				break;
			case ELVEN_MAGE:
				newClass = (chance < 85) ? PlayerClass.ELVEN_WIZARD : PlayerClass.ORACLE;
				break;
			case DARK_MAGE:
				newClass = (chance < 85) ? PlayerClass.DARK_WIZARD : PlayerClass.SHILLIEN_ORACLE;
				break;
			case WIZARD:
				if (chance < 40)
				{
					newClass = PlayerClass.SORCERER;
				}
				else if (chance < 80)
				{
					newClass = PlayerClass.NECROMANCER;
				}
				else
				{
					newClass = PlayerClass.WARLOCK;
				}
				break;
			default:
			{
				break;
			}
		}
		
		if (newClass == null)
		{
			newClass = nextClasses.get(Rnd.get(nextClasses.size()));
		}
		
		bot.setPlayerClass(newClass.getId());
		bot.setBaseClass(newClass.getId());
		bot.broadcastUserInfo();
		
		PhantomManager.logToFile(bot.getName(), "Class change: " + currentClass + " -> " + newClass);
	}
}
