package com.l2journey.gameserver.model.phantom;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import com.l2journey.gameserver.data.xml.ItemData;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.item.enums.ItemProcessType;
import com.l2journey.gameserver.model.item.instance.Item;
import com.l2journey.gameserver.network.serverpackets.ExAutoSoulShot;

public class PhantomEquipment
{
	private static final Logger LOGGER = Logger.getLogger(PhantomEquipment.class.getName());
	private static final int INVENTORY_CLEAN_LIMIT = 25;
	private static final long PROGRESSION_INTERVAL_MS = 10000L;
	private static final Set<Integer> REPORTED_MISSING_ITEMS = ConcurrentHashMap.newKeySet();
	private static final Map<Integer, Long> LAST_PROGRESSION_CHECK = new ConcurrentHashMap<>();
	
	// --- NO-GRADE (Nivel 1 ao 19) ---
	private static final int[] MAGE_KIT_NG =
	{
		1104,
		1101,
		44,
		310,
		51,
		39,
		846,
		909,
		878,
		878,
		846,
		19,
		8187
	};
	private static final int[] FIGHTER_KIT_NG =
	{
		846,
		909,
		878,
		878,
		846,
		8973,
		9032,
		9033,
		44,
		51,
		39,
		19,
		8187
	};
	private static final int[] KAMAEL_KIT_NG =
	{
		846,
		909,
		878,
		878,
		846,
		8973,
		9032,
		9033,
		44,
		51,
		39,
		8187
	};
	
	// --- GRAU D (Nivel 20 ao 39) ---
	private static final int[] ROBE_KIT_D =
	{
		189,
		2411,
		437,
		470,
		2450,
		850,
		881,
		850,
		881,
		913,
		627,
		8187
	};
	private static final int[] LIGHT_KIT_D =
	{
		2499,
		2411,
		395,
		417,
		2424,
		2448,
		850,
		881,
		850,
		881,
		913,
		8187
	};
	private static final int[] HEAVY_KIT_D =
	{
		2499,
		352,
		2378,
		2411,
		2425,
		2449,
		2493,
		850,
		881,
		850,
		881,
		913,
		8187
	};
	
	// --- GRAU C (Nivel 40 ao 51) ---
	private static final int[] ROBE_KIT_C =
	{
		6313,
		439,
		471,
		2430,
		2414,
		2454,
		10492,
		919,
		857,
		857,
		888,
		888,
		2494,
		8187
	};
	private static final int[] LIGHT_KIT_C =
	{
		4708,
		2414,
		398,
		418,
		2431,
		2455,
		10492,
		919,
		857,
		857,
		888,
		888,
		8187
	};
	private static final int[] HEAVY_KIT_C =
	{
		6347,
		356,
		2414,
		2438,
		2462,
		2497,
		10492,
		919,
		857,
		857,
		888,
		888,
		8187
	};
	
	// --- GRAU B (Nivel 52 ao 60) ---
	private static final int[] ROBE_KIT_B =
	{
		7722,
		2398,
		2403,
		2416,
		5720,
		5736,
		10495,
		926,
		864,
		864,
		895,
		895,
		496,
		8187,
		14796
	};
	private static final int[] LIGHT_KIT_B =
	{
		4717,
		2392,
		5739,
		5723,
		2417,
		10495,
		926,
		864,
		864,
		895,
		6660,
		496,
		8187
	};
	private static final int[] HEAVY_KIT_B =
	{
		4717,
		358,
		2380,
		2416,
		5718,
		5734,
		10495,
		926,
		864,
		864,
		895,
		6660,
		496,
		8187
	};
	
	// --- GRAU A (Nivel 61 ao 75) ---
	private static final int[] ROBE_KIT_A =
	{
		5643,
		2407,
		5779,
		5767,
		512,
		6657,
		10497,
		862,
		862,
		893,
		893,
		496,
		8187,
		14925,
		14796
	};
	private static final int[] LIGHT_KIT_A =
	{
		8788,
		2395,
		5775,
		5787,
		2419,
		10497,
		924,
		862,
		862,
		893,
		6660,
		496,
		8187,
		14925
	};
	private static final int[] HEAVY_KIT_A =
	{
		8788,
		374,
		2418,
		5771,
		5783,
		10497,
		924,
		862,
		862,
		893,
		6660,
		496,
		8187,
		14925
	};
	
	// --- GRAU S (Nivel 76+) ---
	private static final int[] ROBE_KIT_S =
	{
		9877,
		9432,
		9437,
		9438,
		9437,
		9439,
		9440,
		12812,
		10500,
		6657,
		858,
		496,
		858,
		889,
		889,
		8187,
		14929
	};
	private static final int[] LIGHT_KIT_S =
	{
		6583,
		9425,
		9428,
		9429,
		9430,
		9431,
		6657,
		10500,
		858,
		858,
		889,
		496,
		6660,
		8187,
		14929
	};
	private static final int[] HEAVY_KIT_S =
	{
		6583,
		9416,
		9421,
		9422,
		9423,
		9424,
		10500,
		858,
		858,
		889,
		6657,
		496,
		6660,
		8187,
		14929
	};
	
	// --- GRAU S84 (Nivel 84+) ---
	private static final int[] ROBE_KIT_S84 =
	{
		15893,
		15574,
		15577,
		15580,
		15583,
		15586,
		15588,
		10500,
		6657,
		858,
		496,
		858,
		889,
		889,
		8187,
		14929
	};
	private static final int[] LIGHT_KIT_S84 =
	{
		6583,
		15576,
		15573,
		15579,
		15582,
		15585,
		6657,
		10500,
		858,
		858,
		889,
		496,
		6660,
		8187,
		14929
	};
	private static final int[] HEAVY_KIT_S84 =
	{
		6583,
		15575,
		15572,
		15578,
		15581,
		15584,
		10500,
		858,
		858,
		889,
		6657,
		496,
		6660,
		8187,
		14929
	};
	
	// =========================================================
	// 2. LOGICA PRINCIPAL DE PROGRESSAO
	// =========================================================
	
	public static void checkProgression(Player bot)
	{
		if (!beginProgressionCheck(bot))
		{
			return;
		}
		
		PhantomProgression.checkProgression(bot);
		
		updateGearByLevel(bot);
		enforceEquipment(bot);
		ensureShots(bot);
		ensureArrows(bot);
		limpiarMochila(bot);
	}
	
	/**
	 * Guard for gear progression: skips null bots, cursed-weapon holders, and checks within the throttle window.
	 * @param bot the phantom
	 * @return {@code true} when progression work should run
	 */
	static boolean beginProgressionCheck(Player bot)
	{
		if ((bot == null) || bot.isCursedWeaponEquipped())
		{
			return false;
		}
		
		long now = System.currentTimeMillis();
		int objectId = bot.getObjectId();
		long last = LAST_PROGRESSION_CHECK.getOrDefault(objectId, 0L);
		if ((now - last) < PROGRESSION_INTERVAL_MS)
		{
			return false;
		}
		LAST_PROGRESSION_CHECK.put(objectId, now);
		return true;
	}
	
	public static void unregisterBot(int objectId)
	{
		LAST_PROGRESSION_CHECK.remove(objectId);
	}
	
	public static void clearRuntimeState()
	{
		LAST_PROGRESSION_CHECK.clear();
	}
	
	private static int[] getActiveKit(Player bot)
	{
		boolean isMage = bot.isMageClass();
		int lvl = bot.getLevel();
		int[] baseKit;
		
		// Verificacao especial para armadura leve
		boolean isLight = !isMage && isLightArmorClass(bot.getBaseClass());
		
		// Identificamos se o bot e raca Kamael (Classes 123 a 136)
		boolean isKamael = (bot.getBaseClass() >= 123) && (bot.getBaseClass() <= 136);
		
		if (lvl >= 84)
		{
			baseKit = isMage ? ROBE_KIT_S84 : (isLight ? LIGHT_KIT_S84 : HEAVY_KIT_S84);
		}
		else if (lvl >= 76)
		{
			baseKit = isMage ? ROBE_KIT_S : (isLight ? LIGHT_KIT_S : HEAVY_KIT_S);
		}
		else if (lvl >= 61)
		{
			baseKit = isMage ? ROBE_KIT_A : (isLight ? LIGHT_KIT_A : HEAVY_KIT_A);
		}
		else if (lvl >= 52)
		{
			baseKit = isMage ? ROBE_KIT_B : (isLight ? LIGHT_KIT_B : HEAVY_KIT_B);
		}
		else if (lvl >= 40)
		{
			baseKit = isMage ? ROBE_KIT_C : (isLight ? LIGHT_KIT_C : HEAVY_KIT_C);
		}
		else if (lvl >= 20)
		{
			baseKit = isMage ? ROBE_KIT_D : (isLight ? LIGHT_KIT_D : HEAVY_KIT_D);
		}
		else
		{
			baseKit = isMage ? MAGE_KIT_NG : (isKamael ? KAMAEL_KIT_NG : FIGHTER_KIT_NG); // CORRECAO AQUI
		}
		
		if (!isMage && (lvl >= 20))
		{
			return customizeFighterKit(bot, baseKit, lvl);
		}
		
		return baseKit;
	}
	
	// Identificador para atribuir set Light a classes especificas
	private static boolean isLightArmorClass(int classId)
	{
		switch (classId)
		{
			// Arqueiros (Hawkeye, Silver Ranger, Phantom Ranger, etc.)
			case 9, 24, 37, 92, 102, 109:
				// Adagueiros e Anoes FS/BH (Scavenger, Bounty Hunter, Treasure Hunter, etc.)
			case 7, 8, 22, 23, 35, 36, 93, 101, 108, 54, 55, 117:
				// Garras (Monk, Tyrant, Grand Khavatari)
			case 47, 48, 114:
				// Kamaels (Todas as classes de Kamael costumam usar Light)
			case 123, 124, 125, 126, 127, 128, 129, 130, 131, 132, 133, 134, 135, 136:
				return true;
			default:
				return false;
		}
	}
	
	private static int[] customizeFighterKit(Player bot, int[] originalKit, int lvl)
	{
		int classId = bot.getBaseClass();
		int customWeaponId = originalKit[0];
		int customShieldId = 0;
		boolean isTwoHanded = false;
		
		switch (classId)
		{
			// === ARQUEIROS ===
			case 9, 24, 37, 92, 102, 109:
				isTwoHanded = true;
				if (lvl >= 84)
				{
					customWeaponId = 15902; // Thorne Crossbow
				}
				else if (lvl >= 76)
				{
					customWeaponId = 9865; // Dynasty
				}
				else if (lvl >= 61)
				{
					customWeaponId = 5613; // Soul
				}
				else if (lvl >= 52)
				{
					customWeaponId = 4828; // Peril
				}
				else if (lvl >= 40)
				{
					customWeaponId = 4822; // Eminence
				}
				else if (lvl >= 20)
				{
					customWeaponId = 280; // Gust
				}
				break;
			
			// === ADAGUEIROS & FORTUNE SEEKERS ===
			case 7, 8, 22, 23, 35, 36, 93, 101, 108, 54, 55, 117: // Scavenger, Bounty Hunter, Fortune Seeker
				if (lvl >= 84)
				{
					customWeaponId = 16156; // Mamba Edge Dual Daggers
				}
				else if (lvl >= 76)
				{
					customWeaponId = 9866; // Angel Slayer
				}
				else if (lvl >= 61)
				{
					customWeaponId = 4784; // Soul Separator
				}
				else if (lvl >= 52)
				{
					customWeaponId = 4778; // Demon Dagger
				}
				else if (lvl >= 40)
				{
					customWeaponId = 4775; // Crystal Dagger
				}
				else if (lvl >= 20)
				{
					customWeaponId = 225; // Mithril Dagger
				}
				break;
			
			// === ESPADA DE DUAS MAOS (Destroyer, Titan) ===
			case 45, 46, 113:
				isTwoHanded = true;
				if (lvl >= 84)
				{
					customWeaponId = 15884; // Lava Saw
				}
				else if (lvl >= 76)
				{
					customWeaponId = 9857; // Heaven's Divider
				}
				else if (lvl >= 61)
				{
					customWeaponId = 5646; // Dragon Slayer
				}
				else if (lvl >= 52)
				{
					customWeaponId = 4724; // Guardian Sword
				}
				else if (lvl >= 40)
				{
					customWeaponId = 6347; // Berserker Blade
				}
				else if (lvl >= 20)
				{
					customWeaponId = 7881; // Zweihander
				}
				break;
			
			// === GARRAS (Monk, Tyrant, Grand Khavatari) ===
			case 47, 48, 114:
				isTwoHanded = true;
				if (lvl >= 84)
				{
					customWeaponId = 15887; // Jade Claw
				}
				else if (lvl >= 76)
				{
					customWeaponId = 9879; // Blood Tornado
				}
				else if (lvl >= 61)
				{
					customWeaponId = 5624; // Blood Tornado
				}
				else if (lvl >= 52)
				{
					customWeaponId = 4805; // Bellion Cestus
				}
				else if (lvl >= 40)
				{
					customWeaponId = 4796; // Fist Blade
				}
				else if (lvl >= 20)
				{
					customWeaponId = 7828; // Bagh-Nakh
				}
				break;
			
			// === LANCA (Warlord, Dreadnought) ===
			case 3, 89:
				isTwoHanded = true;
				if (lvl >= 84)
				{
					customWeaponId = 15890; // Demitelum
				}
				else if (lvl >= 76)
				{
					customWeaponId = 9870; // Tallum Glaive
				}
				else if (lvl >= 61)
				{
					customWeaponId = 5634; // Tallum Glaive
				}
				else if (lvl >= 52)
				{
					customWeaponId = 4856; // Halberd
				}
				else if (lvl >= 40)
				{
					customWeaponId = 4852; // Orcish Poleaxe
				}
				else if (lvl >= 20)
				{
					customWeaponId = 297; // Glaive
				}
				break;
			
			// === ESPADAS DUPLAS (Gladiator, Duelist, Bladedancer, Spectral Dancer) ===
			case 2, 88, 34, 107:
				isTwoHanded = true;
				if (lvl >= 84)
				{
					customWeaponId = 16158; // Eternal Core Dual Sword
				}
				else if (lvl >= 76)
				{
					customWeaponId = 10004; // Tallum * Damascus
				}
				else if (lvl >= 61)
				{
					customWeaponId = 5706; // Damascus * Damascus
				}
				else if (lvl >= 52)
				{
					customWeaponId = 2624; // Tsurugi * Samurai
				}
				else if (lvl >= 40)
				{
					customWeaponId = 2591; // Stormbringer * Stormbringer
				}
				else if (lvl >= 20)
				{
					customWeaponId = 2516; // Saber * Saber
				}
				break;
			
			// === TANQUES (Humanos, Elfos, Elfos Negros) ===
			case 1, 4, 5, 21, 6, 19, 20, 32, 33, 90, 91, 99, 106:
				isTwoHanded = false;
				if (lvl >= 84)
				{
					customWeaponId = 15872;
					customShieldId = 15587;
				} // Eternal Core Sword + elegia shield
				else if (lvl >= 76)
				{
					customWeaponId = 9854;
					customShieldId = 9441;
				} // Forgotten Blade + IC Shield
				else if (lvl >= 61)
				{
					customWeaponId = 5647;
					customShieldId = 2498;
				} // Dark Legion + DC Shield
				else if (lvl >= 52)
				{
					customWeaponId = 4715;
					customShieldId = 110;
				} // Damascus + Blue Wolf Shield
				else if (lvl >= 40)
				{
					customWeaponId = 4708;
					customShieldId = 107;
				} // Samurai Long Sword + Full Plate Shield
				else if (lvl >= 20)
				{
					customWeaponId = 2499;
					customShieldId = 2493;
				} // Elven Long Sword + Kite Shield
				break;
			
			// === MACHADO E ESCUDO (Artisan, Warsmith, Maestro) ===
			case 56, 57, 118:
				isTwoHanded = false;
				if (lvl >= 84)
				{
					customWeaponId = 15878;
					customShieldId = 15587;
				} // Eversor Mace + elegia Shield
				else if (lvl >= 76)
				{
					customWeaponId = 9872;
					customShieldId = 9441;
				} // Basalt Battlehammer + IC Shield
				else if (lvl >= 61)
				{
					customWeaponId = 5604;
					customShieldId = 2498;
				} // Elysian + DC Shield
				else if (lvl >= 52)
				{
					customWeaponId = 4755;
					customShieldId = 110;
				} // Art of Battle Axe + BW Shield
				else if (lvl >= 40)
				{
					customWeaponId = 4744;
					customShieldId = 107;
				} // Dwarven War Hammer + Full Plate Shield
				else if (lvl >= 20)
				{
					customWeaponId = 158;
					customShieldId = 2493;
				} // Tomahawk + Kite Shield
				break;
			
			// === KAMAEL - ANCIENT SWORD (Berserker, Doombringer) ===
			case 127, 131:
				isTwoHanded = true;
				if (lvl >= 84)
				{
					customWeaponId = 15908; // Pyseal Blade
				}
				else if (lvl >= 76)
				{
					customWeaponId = 9381; // Gram
				}
				else if (lvl >= 61)
				{
					customWeaponId = 9357; // Innominate Victory
				}
				else if (lvl >= 52)
				{
					customWeaponId = 9323; // Dismantler
				}
				else if (lvl >= 40)
				{
					customWeaponId = 9297; // Inquisitor
				}
				else if (lvl >= 20)
				{
					customWeaponId = 7881; // Ironcleaver
				}
				break;
			
			// === KAMAEL - RAPIER (Soul Breaker, Soul Hound) ===
			case 128, 129, 132, 133, 135:
				isTwoHanded = false; // O Rapier permite escudo/sigilo
				if (lvl >= 84)
				{
					customWeaponId = 15905; // Heavenstare Rapier
				}
				else if (lvl >= 76)
				{
					customWeaponId = 9377; // Laevateinn
				}
				else if (lvl >= 61)
				{
					customWeaponId = 9341; // Lacerator
				}
				else if (lvl >= 52)
				{
					customWeaponId = 9317; // Colichemarde
				}
				else if (lvl >= 40)
				{
					customWeaponId = 9245; // Guisarme
				}
				else if (lvl >= 20)
				{
					customWeaponId = 9209; // Rapier
				}
				break;
			
			// === KAMAEL - CROSSBOW (Arbalester, Trickster) ===
			case 130, 134:
				isTwoHanded = true;
				if (lvl >= 84)
				{
					customWeaponId = 15911; // Thorne Crossbow
				}
				else if (lvl >= 76)
				{
					customWeaponId = 9387; // Saranga
				}
				else if (lvl >= 61)
				{
					customWeaponId = 9362; // Peacemaker
				}
				else if (lvl >= 52)
				{
					customWeaponId = 9325; // Reaper
				}
				else if (lvl >= 40)
				{
					customWeaponId = 9301; // Ballista
				}
				else if (lvl >= 20)
				{
					customWeaponId = 9227; // Sharpshooter
				}
				break;
		}
		
		// Montamos um novo Kit filtrando escudos se o bot usa armas de duas maos
		List<Integer> newKit = new ArrayList<>();
		newKit.add(customWeaponId); // Adicionamos a arma
		
		// Escudo so entra no kit se a arma nao for de duas maos
		if (!isTwoHanded && (customShieldId != 0))
		{
			newKit.add(customShieldId);
		}
		
		for (int i = 1; i < originalKit.length; i++)
		{
			int itemId = originalKit[i];
			
			if (isShield(itemId))
			{
				continue;
			}
			
			newKit.add(itemId);
		}
		
		int[] finalKit = new int[newKit.size()];
		for (int i = 0; i < newKit.size(); i++)
		{
			finalKit[i] = newKit.get(i);
		}
		
		return finalKit;
	}
	
	public static void updateGearByLevel(Player bot)
	{
		int[] kitCorrecto = getActiveKit(bot);
		boolean isKamael = ((bot.getBaseClass() >= 123) && (bot.getBaseClass() <= 136));
		
		// 1. Limpamos lixo
		for (Item item : bot.getInventory().getItems())
		{
			// Protegemos Adena, Soulshots E FLECHAS
			if ((item == null) || (item.getId() == 57) || isShot(item.getId()) || isArrow(item.getId()))
			{
				continue;
			}
			
			boolean isShieldNg = (item.getId() == 19);
			// Destroi se: Nao esta no kit, OU e Kamael e e o escudo ID 19 (Regra forcada)
			boolean shouldDestroy = !isItemInKit(item.getId(), kitCorrecto) || (isKamael && isShieldNg);
			
			if (item.isEquipable() && shouldDestroy)
			{
				if (item.isEquipped())
				{
					bot.getInventory().unEquipItemInSlot(item.getLocationSlot());
				}
				bot.destroyItem(ItemProcessType.DESTROY, item, bot, false);
			}
		}
		
		// 2. Montamos a lista do que falta
		List<Integer> faltantes = new ArrayList<>();
		for (int id : kitCorrecto)
		{
			faltantes.add(id);
		}
		
		for (Item item : bot.getInventory().getItems())
		{
			if (item != null)
			{
				faltantes.remove(Integer.valueOf(item.getId()));
			}
		}
		
		for (int itemId : faltantes)
		{
			if (ItemData.getInstance().getTemplate(itemId) == null)
			{
				if (REPORTED_MISSING_ITEMS.add(itemId))
				{
					LOGGER.warning("Phantom kit item does not exist and will be skipped. Item ID: " + itemId);
				}
				continue;
			}
			
			bot.addItem(ItemProcessType.REWARD, itemId, 1, bot, false);
		}
	}
	
	private static boolean isItemInKit(int itemId, int[] kit)
	{
		for (int id : kit)
		{
			if (id == itemId)
			{
				return true;
			}
		}
		return false;
	}
	
	private static void enforceEquipment(Player bot)
	{
		int[] kitCorrecto = getActiveKit(bot);
		
		// ==========================================
		// PASSO 1: Equipar Set, Joias e Armas PRIMEIRO
		// ==========================================
		for (Item item : bot.getInventory().getItems())
		{
			if ((item != null) && item.isEquipable() && isItemInKit(item.getId(), kitCorrecto))
			{
				int targetEnchant = 0;
				
				boolean isWeapon = item.isWeapon();
				long slot = item.getTemplate().getBodyPart();
				// Identificamos se e uma joia conforme o slot
				boolean isJoya = (slot == 8) || (slot == 2) || (slot == 4) || (slot == 6) || (slot == 16) || (slot == 32) || (slot == 48);
				
				// Avaliamos o grau do item para atribuir o enchant correspondente
				switch (item.getTemplate().getCrystalType())
				{
					case NONE: // No-Grade
						if (isWeapon)
						{
							targetEnchant = 0;
						}
						else if (isJoya)
						{
							targetEnchant = 16;
						}
						else
						{
							targetEnchant = 16; // Armor / Set
						}
						break;
					
					case D: // Grau D
						if (isWeapon)
						{
							targetEnchant = 0;
						}
						else if (isJoya)
						{
							targetEnchant = 16;
						}
						else
						{
							targetEnchant = 16; // Armor / Set
						}
						break;
					
					case C: // Grau C
						if (isWeapon)
						{
							targetEnchant = 3;
						}
						else if (isJoya)
						{
							targetEnchant = 16;
						}
						else
						{
							targetEnchant = 16;
						}
						break;
					
					case B: // Grau B
						if (isWeapon)
						{
							targetEnchant = 8;
						}
						else if (isJoya)
						{
							targetEnchant = 16;
						}
						else
						{
							targetEnchant = 16;
						}
						break;
					
					case A: // Grau A
						if (isWeapon)
						{
							targetEnchant = 10;
						}
						else if (isJoya)
						{
							targetEnchant = 16;
						}
						else
						{
							targetEnchant = 16;
						}
						break;
					
					case S: // Grau S
						if (isWeapon)
						{
							targetEnchant = 15;
						}
						else if (isJoya)
						{
							targetEnchant = 16;
						}
						else
						{
							targetEnchant = 16;
						}
						break;
					
					case S84: // Grau S84
						if (isWeapon)
						{
							targetEnchant = 16;
						}
						else if (isJoya)
						{
							targetEnchant = 16;
						}
						else
						{
							targetEnchant = 16;
						}
						break;
					
					default:
						targetEnchant = 0;
						break;
				}
				
				// Aplicamos o encantamento somente se o item precisar
				if (item.getEnchantLevel() < targetEnchant)
				{
					item.setEnchantLevel(targetEnchant);
				}
				
				// Equipamos o item (Arma/Roupa)
				if (!item.isEquipped())
				{
					bot.getInventory().equipItem(item);
				}
			}
		}
		
		// ==========================================
		// PASSO 2: Equipar Municao CORRETA EXATA
		// ==========================================
		int lvl = bot.getLevel();
		int targetAmmoId = -1;
		int classId = bot.getBaseClass();
		
		// Identificamos se e um arqueiro Kamael (Warder, Arbalester, Trickster)
		boolean isKamaelArcher = ((classId == 126) || (classId == 130) || (classId == 134));
		
		if (isKamaelArcher)
		{
			if (lvl >= 84)
			{
				targetAmmoId = 9637; // S
			}
			else if (lvl >= 76)
			{
				targetAmmoId = 9637; // S
			}
			else if (lvl >= 61)
			{
				targetAmmoId = 9636; // A
			}
			else if (lvl >= 52)
			{
				targetAmmoId = 9635; // B
			}
			else if (lvl >= 40)
			{
				targetAmmoId = 9634; // C
			}
			else if (lvl >= 20)
			{
				targetAmmoId = 9633; // D
			}
			else
			{
				targetAmmoId = 9632; // NG
			}
		}
		
		// Se e Kamael, buscamos EXATAMENTE sua municao correspondente e a equipamos
		if (targetAmmoId != -1)
		{
			for (Item item : bot.getInventory().getItems())
			{
				if ((item != null) && (item.getId() == targetAmmoId))
				{
					if (!item.isEquipped())
					{
						bot.getInventory().equipItem(item);
						break; // Uma vez que a equipou, cortamos a busca
					}
				}
			}
		}
		
		bot.getInventory().refreshWeight();
		bot.broadcastUserInfo();
	}
	
	public static void cleanInventory(Player bot)
	{
		if (bot.getInventory().getSize() > INVENTORY_CLEAN_LIMIT)
		{
			for (Item item : bot.getInventory().getItems())
			{
				// Protegemos flechas para que nao sejam apagadas por erro
				if (!item.isEquipped() && (item.getId() != 57) && !isShot(item.getId()) && !isArrow(item.getId()))
				{
					bot.destroyItem(ItemProcessType.DESTROY, item, bot, false);
				}
			}
		}
	}
	
	// =========================================================
	// 3. GESTAO AUTOMATICA DE CONSUMIVEIS
	// =========================================================
	
	private static void ensureShots(Player bot)
	{
		boolean isMage = bot.isMageClass();
		int lvl = bot.getLevel();
		int shotId;
		
		// IDs atualizados: Os magos agora recebem BLESSED Spiritshots (3947 a 3952)
		if (lvl >= 76)
		{
			shotId = isMage ? 3952 : 1467; // S-Grade (BSS: 3952 | SS: 1467)
		}
		else if (lvl >= 61)
		{
			shotId = isMage ? 3951 : 1466; // A-Grade (BSS: 3951 | SS: 1466)
		}
		else if (lvl >= 52)
		{
			shotId = isMage ? 3950 : 1465; // B-Grade (BSS: 3950 | SS: 1465)
		}
		else if (lvl >= 40)
		{
			shotId = isMage ? 3949 : 1464; // C-Grade (BSS: 3949 | SS: 1464)
		}
		else if (lvl >= 20)
		{
			shotId = isMage ? 3948 : 1463; // D-Grade (BSS: 3948 | SS: 1463)
		}
		else
		{
			shotId = isMage ? 3947 : 1835; // No-Grade (BSS: 3947 | SS: 1835)
		}
		
		if (bot.getInventory().getInventoryItemCount(shotId, -1) < 500)
		{
			bot.addItem(ItemProcessType.REWARD, shotId, 2000, bot, false);
		}
		
		if (!bot.getAutoSoulShot().contains(shotId))
		{
			bot.addAutoSoulShot(shotId);
		}
		
		bot.sendPacket(new ExAutoSoulShot(shotId, 1));
	}
	
	// --- SISTEMA INDEPENDENTE PARA AS FLECHAS E VIROTES ---
	private static void ensureArrows(Player bot)
	{
		int classId = bot.getBaseClass();
		// Arqueiros normais (Humanos, Elfos, Elfos Negros)
		boolean isArcher = ((classId == 9) || (classId == 24) || (classId == 37) || (classId == 92) || (classId == 102) || (classId == 109));
		// Arqueiros Kamael (Usam Besta e Virotes)
		boolean isKamaelArcher = ((classId == 130) || (classId == 134));
		
		if (!isArcher && !isKamaelArcher)
		{
			return;
		}
		
		int lvl = bot.getLevel();
		int ammoId;
		
		if (isKamaelArcher)
		{
			// Virotes (Bolts) para Bestas
			if (lvl >= 84)
			{
				ammoId = 9637; // Bolts S
			}
			else if (lvl >= 76)
			{
				ammoId = 9637; // Bolts s
			}
			else if (lvl >= 61)
			{
				ammoId = 9636; // Bolts A
			}
			else if (lvl >= 52)
			{
				ammoId = 9635; // Bolts B
			}
			else if (lvl >= 40)
			{
				ammoId = 9634; // Bolts C
			}
			else if (lvl >= 20)
			{
				ammoId = 9633; // Bolts D
			}
			else
			{
				ammoId = 9632; // Bolts NG
			}
		}
		else
		{
			// Flechas normais para Arcos
			if (lvl >= 84)
			{
				ammoId = 1345; // Flechas S84
			}
			else if (lvl >= 76)
			{
				ammoId = 1345; // Flechas s
			}
			else if (lvl >= 61)
			{
				ammoId = 1344; // Flechas A
			}
			else if (lvl >= 52)
			{
				ammoId = 1343; // Flechas B
			}
			else if (lvl >= 40)
			{
				ammoId = 1342; // Flechas C
			}
			else if (lvl >= 20)
			{
				ammoId = 1341; // Flechas D
			}
			else
			{
				ammoId = 17; // Flechas NG
			}
		}
		
		// Se tiver menos de 500, damos 2000
		if (bot.getInventory().getInventoryItemCount(ammoId, -1) < 500)
		{
			bot.addItem(ItemProcessType.REWARD, ammoId, 2000, bot, false);
		}
	}
	
	static boolean isShot(int itemId)
	{
		if (itemId == 728)
		{
			return true; // Pocao de Mana
		}
		if (itemId == 1539)
		{
			return true; // Healing Potion
		}
		
		return (// Soulshots (Fighters)
		(itemId == 1835) || (itemId == 1463) || (itemId == 1464) || (itemId == 1465) || (itemId == 1466) || (itemId == 1467) || (// Spiritshots Normais (Caso tenha sobrado algum antigo no inventario)
		itemId == 2509) || (itemId == 2510) || (itemId == 2511) || (itemId == 2512) || (itemId == 2513) || (itemId == 2514) || (// Blessed Spiritshots (Magos - Novos IDs para que nao sejam apagados)
		itemId == 3947) || (itemId == 3948) || (itemId == 3949) || (itemId == 3950) || (itemId == 3951) || (itemId == 3952));
	}
	
	// IDENTIFICADOR DE FLECHAS Y VOLT ---
	static boolean isArrow(int itemId)
	{
		return ((itemId == 1341) || (itemId == 1342) || (itemId == 1343) || (itemId == 1344) || (itemId == 1345) || (itemId == 17) || (itemId == 9632) || (itemId == 9633) || (itemId == 9634) || (itemId == 9635) || (itemId == 9636) || (itemId == 9637));
	}
	
	// IDENTIFICADOR DE ESCUDOS ---
	private static boolean isShield(int id)
	{
		// Adicionado o ID 19 (Escudo No-Grade)
		return (id == 19) || (id == 2493) || (id == 107) || (id == 110) || (id == 9441) || (id == 2498) || (id == 2497) || (id == 15587);
	}
	
	private static void limpiarMochila(Player bot)
	{
		int lvl = bot.getLevel();
		int ssValido, bssValido, arrowValido, boltValido;
		
		// 1. Definimos qual e o UNICO que deveria ter conforme seu grau atual
		if (lvl >= 76)
		{
			ssValido = 1467;
			bssValido = 3952;
			arrowValido = 1345;
			boltValido = 9637;
		} // S / S84
		else if (lvl >= 61)
		{
			ssValido = 1466;
			bssValido = 3951;
			arrowValido = 1344;
			boltValido = 9636;
		} // A
		else if (lvl >= 52)
		{
			ssValido = 1465;
			bssValido = 3950;
			arrowValido = 1343;
			boltValido = 9635;
		} // B
		else if (lvl >= 40)
		{
			ssValido = 1464;
			bssValido = 3949;
			arrowValido = 1342;
			boltValido = 9634;
		} // C
		else if (lvl >= 20)
		{
			ssValido = 1463;
			bssValido = 3948;
			arrowValido = 1341;
			boltValido = 9633;
		} // D
		else
		{
			ssValido = 1835;
			bssValido = 3947;
			arrowValido = 17;
			boltValido = 9632;
		} // NG
		
		// 2. Revisamos item por item no inventario para apagar o lixo antigo
		for (Item item : bot.getInventory().getItems())
		{
			if (item == null)
			{
				continue;
			}
			int id = item.getId();
			
			// Identificamos a qual categoria pertence o item
			boolean esCualquierSS = ((id >= 1463) && (id <= 1467)) || (id == 1835);
			boolean esCualquierBSS = ((id >= 3947) && (id <= 3952));
			boolean esCualquierArrow = ((id >= 1341) && (id <= 1345)) || (id == 17);
			boolean esCualquierBolt = ((id >= 9632) && (id <= 9637));
			
			// Se e um Shot ou Municao MAS NAO e o valido do seu grau, destruimos
			if ((esCualquierSS && (id != ssValido)) || (esCualquierBSS && (id != bssValido)) || (esCualquierArrow && (id != arrowValido)) || (esCualquierBolt && (id != boltValido)))
			{
				bot.getInventory().destroyItemByItemId(ItemProcessType.NONE, id, item.getCount(), bot, null);
			}
		}
		
		// 3. --- CONTROLE DE EXCESSO DE PESO POR CONSUMIVEIS ---
		int maxPots = 500;
		
		long hpPots = bot.getInventory().getInventoryItemCount(1539, -1); // Pocao HP
		if (hpPots > maxPots)
		{
			bot.getInventory().destroyItemByItemId(ItemProcessType.NONE, 1539, hpPots - maxPots, bot, null);
		}
		
		long mpPots = bot.getInventory().getInventoryItemCount(728, -1); // Pocao MP
		if (mpPots > maxPots)
		{
			bot.getInventory().destroyItemByItemId(ItemProcessType.NONE, 728, mpPots - maxPots, bot, null);
		}
		
		long spiritOre = bot.getInventory().getInventoryItemCount(3031, -1); // Spirit Ore
		if (spiritOre > 1000)
		{
			bot.getInventory().destroyItemByItemId(ItemProcessType.NONE, 3031, spiritOre - 1000, bot, null);
		}
		
		long soulOre = bot.getInventory().getInventoryItemCount(3030, -1); // Soul Ore
		if (soulOre > 1000)
		{
			bot.getInventory().destroyItemByItemId(ItemProcessType.NONE, 3030, soulOre - 1000, bot, null);
		}
		
		// 4. Atualizamos o peso
		bot.getInventory().refreshWeight();
		bot.broadcastUserInfo();
	}
}