package com.l2journey.gameserver.model.phantom;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.l2journey.PhantomConfig;
import com.l2journey.commons.threads.ThreadPool;
import com.l2journey.commons.util.Rnd;
import com.l2journey.gameserver.ai.PhantomAI;
import com.l2journey.gameserver.model.Location;
import com.l2journey.gameserver.model.World;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.item.enums.ItemProcessType;
import com.l2journey.gameserver.model.item.instance.Item;

public class PhantomEngine
{
	public static final List<Player> activePhantoms = new CopyOnWriteArrayList<>();
	private static final Map<Integer, Long> FATIGUE_TIMES = new ConcurrentHashMap<>();
	
	public static boolean isRunning = false;
	
	// Filtro de limpeza para eliminar equipamento indesejado (Karmian) ao nascer
	public static void forceWipe(Player bot)
	{
		if ((bot == null) || !bot.isMageClass() || (bot.getLevel() >= 40))
		{
			return;
		}
		
		for (Item item : bot.getInventory().getItems())
		{
			if ((item != null) && ((item.getId() == 438) || (item.getId() == 470)))
			{
				if (item.isEquipped())
				{
					bot.getInventory().unEquipItemInSlot(item.getLocationSlot());
				}
				
				bot.destroyItem(ItemProcessType.DESTROY, item, bot, false);
			}
		}
	}
	
	public static void startSystem(Player gm)
	{
		PhantomManager.startLogSession("PHANTOM_START");
		isRunning = true;
		
		if (gm != null)
		{
			gm.sendMessage(">> Sistema Phantom iniciado.");
			PhantomMenu.showMenu(gm);
		}
	}
	
	// ADICIONADO "synchronized" PARA EVITAR CLONAGENS SE FOR CHAMADO MULTIPLAS VEZES NO MESMO MILISSEGUNDO
	public static synchronized void startBatch(int count, Player gm)
	{
		if (isRunning && (gm != null))
		{
			PhantomManager.logToFile("SYSTEM", "Sistema ya estaba corriendo. Agregando lote de " + count + ".");
		}
		else if (!isRunning)
		{
			PhantomManager.startLogSession("PHANTOM_START");
			isRunning = true;
		}
		
		int loadedCount = 0;
		PhantomManager.logToFile("SYSTEM", "Iniciando lote de hasta " + count + " phantoms.");
		
		for (int charId : PhantomConfig.PHANTOM_IDS)
		{
			if (loadedCount >= count)
			{
				break;
			}
			
			try
			{
				if (World.getInstance().getPlayer(charId) != null)
				{
					continue;
				}
				
				Player phantom = Player.load(charId);
				if (phantom == null)
				{
					continue;
				}
				
				preparePhantom(phantom, false);
				registerPhantom(phantom, Rnd.get(100) < 10, Rnd.get(100) < 3);
				startPhantomAI(phantom);
				loadedCount++;
				
				PhantomManager.logToFile(phantom.getName(), "Phantom iniciado correctamente.");
			}
			catch (Exception e)
			{
				PhantomManager.logException("SPAWN", "Error al cargar phantom ID " + charId, e);
			}
		}
		
		if (gm != null)
		{
			gm.sendMessage(">>> Se iniciaron " + loadedCount + " phantoms.");
			PhantomMenu.showMenu(gm);
		}
	}
	
	public static synchronized void createAndStart(int count, Player gm)
	{
		if (!isRunning)
		{
			isRunning = true;
		}
		int createdCount = 0;
		
		for (Player phantom : PhantomFactory.createPhantoms(count))
		{
			try
			{
				preparePhantom(phantom, true);
				registerPhantom(phantom, Rnd.get(100) < 17, Rnd.get(100) < 6);
				PhantomConfig.addPhantomId(phantom.getObjectId());
				startPhantomAI(phantom);
				createdCount++;
			}
			catch (Exception e)
			{
				PhantomManager.logException("AUTO_CREATE", "Error creando phantom", e);
			}
		}
		
		if (createdCount > 0)
		{
			PhantomConfig.saveXML();
		}
		
		if (gm != null)
		{
			gm.sendMessage(">> Se crearon " + createdCount + " phantoms.");
			PhantomMenu.showMenu(gm);
		}
	}
	
	// Desconexao e purga total de RAM para um bot
	public static void disconnectPhantom(Player p)
	{
		if (p == null)
		{
			return;
		}
		try
		{
			// 1. Purgamos ele dos mapas internos
			activePhantoms.remove(p);
			FATIGUE_TIMES.remove(p.getObjectId());
			PhantomWalk.removeBot(p);
			PhantomAI.unregisterBot(p.getObjectId());
			PhantomState.unregister(p.getObjectId());
			PhantomState.REVIVING_PHANTOMS.remove(p.getObjectId());
			
			// --- 2. INICIO DE PURGA PROFUNDA DE RAM ---
			p.setTarget(null);
			p.abortAttack();
			p.abortCast();
			p.stopMove(null);
			p.stopAllEffects();
			
			if (p.getSummon() != null)
			{
				p.getSummon().unSummon(p);
			}
			
			if (p.isOnline())
			{
				if (p.isSpawned())
				{
					p.decayMe();
				}
				// Salvamos os itens/stats antes de apaga-lo
				try
				{
					p.store(true);
				}
				catch (Exception ignored)
				{
				}
				p.deleteMe();
				p.setOnlineStatus(false, true);
			}
			World.getInstance().removeObject(p);
		}
		catch (Exception e)
		{
			PhantomManager.logException(p.getName(), "Error disconnectPhantom", e);
		}
	}
	
	public static int stopSome(int count, Player gm)
	{
		int stopped = 0;
		// Usamos uma lista temporaria para nao ter conflitos ao apagar
		List<Player> toStop = new ArrayList<>();
		
		for (Player p : activePhantoms)
		{
			if (stopped >= count)
			{
				break;
			}
			if (p != null)
			{
				toStop.add(p);
				stopped++;
			}
		}
		
		for (Player p : toStop)
		{
			disconnectPhantom(p);
		}
		
		if (activePhantoms.isEmpty())
		{
			isRunning = false;
		}
		if (gm != null)
		{
			gm.sendMessage(">>> Se desconectaron " + stopped + " phantoms.");
			PhantomMenu.showMenu(gm);
		}
		return stopped;
	}
	
	public static void stopSystem(Player gm)
	{
		isRunning = false;
		// Iteramos sobre uma copia da lista para garantir o apagamento completo
		for (Player p : new ArrayList<>(activePhantoms))
		{
			disconnectPhantom(p);
		}
		
		activePhantoms.clear();
		FATIGUE_TIMES.clear(); // Esvaziamos os temporizadores ao desligar
		PhantomWalk.clearAll();
		PhantomAI.clearRuntimeState();
		PhantomState.clear();
		
		if (gm != null)
		{
			gm.sendMessage(">>> Sistema Phantom detenido y memoria purgada.");
		}
	}
	
	public static Player getPhantomByName(String name)
	{
		return activePhantoms.stream().filter(p -> p.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
	}
	
	public static void movePhantomTo(Player phantom, Location loc, String reason)
	{
		if ((phantom == null) || (loc == null))
		{
			return;
		}
		
		try
		{
			phantom.abortAttack();
			phantom.abortCast();
			phantom.stopMove(null);
			phantom.setTarget(null);
			phantom.setInvisible(false);
			phantom.setInvul(false);
			phantom.setRunning();
			if (phantom.isDead())
			{
				phantom.doRevive();
			}
			if (phantom.isSpawned())
			{
				phantom.decayMe();
			}
			phantom.spawnMe(loc.getX(), loc.getY(), loc.getZ());
			if (loc.getHeading() != 0)
			{
				phantom.setHeading(loc.getHeading());
			}
			phantom.broadcastUserInfo();
			phantom.broadcastInfo();
			PhantomManager.logToFile(phantom.getName(), reason + ": " + loc.getX() + ", " + loc.getY() + ", " + loc.getZ());
		}
		catch (Exception e)
		{
			PhantomManager.logException(phantom.getName(), "Error movePhantomTo", e);
		}
	}
	
	private static void preparePhantom(Player phantom, boolean creationSpawn)
	{
		try
		{
			forceWipe(phantom);
			
			phantom.setOnlineStatus(true, true);
			phantom.setRunning();
			phantom.setInvul(false);
			phantom.setInvisible(false);
			phantom.setEnteredWorld();
			
			// AQUI ESTA O ADICIONAL PARA RECARREGAR OS CRISTAIS E SS AO NASCER
			int ssId = 1463; // Pode ajustar isso dinamicamente conforme o grau
			int bssId = 3947;
			if (phantom.getLevel() >= 76)
			{
				ssId = 1462;
				bssId = 3952;
			}
			else if (phantom.getLevel() >= 61)
			{
				ssId = 1467;
				bssId = 3951;
			}
			else if (phantom.getLevel() >= 52)
			{
				ssId = 1466;
				bssId = 3950;
			}
			else if (phantom.getLevel() >= 40)
			{
				ssId = 1465;
				bssId = 3949;
			}
			else if (phantom.getLevel() >= 20)
			{
				ssId = 1464;
				bssId = 3948;
			}
			PhantomAI.handleItemRefill(phantom, phantom.getObjectId(), ssId, bssId);
			
			if (creationSpawn)
			{
				spawnAtCreationPoint(phantom);
			}
			else
			{
				spawnPhantom(phantom);
			}
			
			phantom.broadcastUserInfo();
		}
		catch (Exception e)
		{
			PhantomManager.logException(phantom.getName(), "Error preparePhantom", e);
		}
	}
	
	private static void registerPhantom(Player phantom, boolean aggressive, boolean pkMode)
	{
		if (!activePhantoms.contains(phantom))
		{
			activePhantoms.add(phantom);
		}
		PhantomState.register(phantom.getObjectId(), aggressive, pkMode);
		if (pkMode)
		{
			phantom.setPkKills(Math.max(1, phantom.getPkKills()));
			phantom.updatePvpTitleAndColor(true);
			phantom.broadcastUserInfo();
		}
		
		// SISTEMA DE FADIGA: Atribuimos um tempo de jogo aleatorio entre 5 e 8 horas
		long fatigueTime = System.currentTimeMillis() + (Rnd.get(5, 8) * 60 * 60 * 1000L);
		FATIGUE_TIMES.put(phantom.getObjectId(), fatigueTime);
	}
	
	private static void spawnPhantom(Player phantom)
	{
		Location spot = PhantomHuntingSpots.getRandomSpot(phantom.getLevel());
		if (spot == null)
		{
			PhantomConfig.FarmZone zone = PhantomConfig.getIdealZone(phantom.getLevel());
			spot = new Location(zone.townX, zone.townY, zone.townZ);
		}
		Location safe = PhantomGeo.getNpcLikeSpawn(spot);
		movePhantomTo(phantom, safe, "Spawn zona leveo");
	}
	
	private static void spawnAtCreationPoint(Player phantom)
	{
		Location point = phantom.getTemplate().getCreationPoint();
		Location safe = PhantomGeo.getNpcLikeSpawn(point);
		movePhantomTo(phantom, safe, "Spawn ciudad origen");
		PhantomState.HUNT_LEVEL_BAND.put(phantom.getObjectId(), -1);
		PhantomState.NEXT_HUNT_TELEPORT.put(phantom.getObjectId(), System.currentTimeMillis() + 30000L);
	}
	
	public static void startPhantomAI(Player bot)
	{
		if ((bot == null) || !isRunning)
		{
			return;
		}
		
		ThreadPool.schedule(() ->
		{
			if (!isRunning || !activePhantoms.contains(bot))
			{
				return;
			}
			
			// SISTEMA ANTI-LEAK: Se o bot desapareceu do mundo, cortamos a IA e limpamos a RAM
			if (World.getInstance().getPlayer(bot.getObjectId()) == null)
			{
				disconnectPhantom(bot);
				return;
			}
			
			// --- NOVO: SISTEMA DE FADIGA E ROTACAO ---
			if (FATIGUE_TIMES.containsKey(bot.getObjectId()) && (System.currentTimeMillis() > FATIGUE_TIMES.get(bot.getObjectId())))
			{
				PhantomManager.logToFile(bot.getName(), "Fatiga alcanzada. Se desconecta para limpiar RAM.");
				disconnectPhantom(bot);
				
				// AGORA ALEATORIZADO POR SEGUNDOS (240 a 420 segs = 4 a 7 min)
				// Isso evita que todos disparem o timer no mesmo milissegundo exato.
				long reconnectDelay = Rnd.get(240, 420) * 1000L;
				ThreadPool.schedule(() ->
				{
					if (isRunning)
					{
						startBatch(1, null);
					}
				}, reconnectDelay);
				
				return; // Cortamos a thread deste bot
			}
			// -------------------------------------------
			
			try
			{
				if (!bot.isSpawned() && !bot.isDead())
				{
					spawnPhantom(bot);
				}
				
				if (!bot.isDead())
				{
					PhantomAI.thinkAndFarm(bot);
				}
				else
				{
					if (!PhantomState.REVIVING_PHANTOMS.add(bot.getObjectId()))
					{
						return;
					}
					ThreadPool.schedule(() ->
					{
						try
						{
							if (isRunning && activePhantoms.contains(bot) && bot.isOnline())
							{
								bot.doRevive();
								bot.setCurrentHp(bot.getMaxHp());
								bot.setCurrentMp(bot.getMaxMp());
								spawnAtCreationPoint(bot);
								bot.broadcastUserInfo();
								PhantomState.MP_RECOVERY_STATE.put(bot.getObjectId(), false);
							}
						}
						catch (Exception e)
						{
							PhantomManager.logException(bot.getName(), "Error revive", e);
						}
						finally
						{
							PhantomState.REVIVING_PHANTOMS.remove(bot.getObjectId());
						}
					}, 15000);
				}
			}
			catch (Exception e)
			{
				PhantomManager.logException(bot.getName(), "Error IA", e);
			}
			finally
			{
				// Aqui esta a checagem de encerramento
				if (isRunning && activePhantoms.contains(bot))
				{
					startPhantomAI(bot);
				}
			}
		}, 500); // <-- DELAY DE 500ms PARA AS SKILLS/ACOES
	}
}