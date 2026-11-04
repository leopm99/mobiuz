package com.l2journey.gameserver.model.actor.appearance;

import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.enums.creature.Race;
import com.l2journey.gameserver.model.actor.enums.player.PlayerClass;
import com.l2journey.gameserver.model.variables.PlayerVariables;

/** Selects a client model without changing the character's server-side identity. */
public enum PlayerVisualProfile
{
	HUMAN_WARRIOR("Human Warrior", PlayerClass.FIGHTER),
	HUMAN_MYSTIC("Human Mystic", PlayerClass.MAGE),
	ELF("Elf", PlayerClass.ELVEN_FIGHTER),
	DARK_ELF("Dark Elf", PlayerClass.DARK_FIGHTER),
	ORC_WARRIOR("Orc Warrior", PlayerClass.ORC_FIGHTER),
	ORC_MYSTIC("Orc Mystic", PlayerClass.ORC_MAGE),
	DWARF("Dwarf", PlayerClass.DWARVEN_FIGHTER);

	private final String _label;
	private final PlayerClass _modelClass;

	PlayerVisualProfile(String label, PlayerClass modelClass)
	{
		_label = label;
		_modelClass = modelClass;
	}

	public String getLabel()
	{
		return _label;
	}

	public PlayerClass getModelClass()
	{
		return _modelClass;
	}

	public static PlayerVisualProfile fromKey(String key)
	{
		if (key == null || key.isEmpty())
		{
			return null;
		}
		try
		{
			return valueOf(key);
		}
		catch (IllegalArgumentException e)
		{
			return null;
		}
	}

	public static PlayerVisualProfile getSelected(Player player)
	{
		return fromKey(player.getVariables().getString(PlayerVariables.COMMUNITY_VISUAL_PROFILE, ""));
	}

	public static boolean supports(Player player)
	{
		return player.getRace() != Race.KAMAEL && player.getPlayerClass().getRace() != Race.KAMAEL;
	}

	public static PlayerVisualProfile getVisible(Player player)
	{
		final PlayerVisualProfile selected = getSelected(player);
		if (selected == null || player.isTransformed() || !supports(player))
		{
			return null;
		}
		return selected;
	}
}
