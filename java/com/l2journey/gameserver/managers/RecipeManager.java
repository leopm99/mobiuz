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
package com.l2journey.gameserver.managers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import com.l2journey.Config;
import com.l2journey.commons.threads.ThreadPool;
import com.l2journey.commons.util.Rnd;
import com.l2journey.gameserver.data.enums.StatType;
import com.l2journey.gameserver.data.holders.RecipeHolder;
import com.l2journey.gameserver.data.holders.RecipeStatHolder;
import com.l2journey.gameserver.data.xml.ItemData;
import com.l2journey.gameserver.data.xml.RecipeData;
import com.l2journey.gameserver.model.ManufactureItem;
import com.l2journey.gameserver.model.RecipeList;
import com.l2journey.gameserver.model.TempItem;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.item.ItemTemplate;
import com.l2journey.gameserver.model.item.enums.ItemProcessType;
import com.l2journey.gameserver.model.item.instance.Item;
import com.l2journey.gameserver.model.itemcontainer.Inventory;
import com.l2journey.gameserver.model.skill.CommonSkill;
import com.l2journey.gameserver.model.skill.Skill;
import com.l2journey.gameserver.model.stats.Stat;
import com.l2journey.gameserver.network.SystemMessageId;
import com.l2journey.gameserver.network.serverpackets.ActionFailed;
import com.l2journey.gameserver.network.serverpackets.MagicSkillUse;
import com.l2journey.gameserver.network.serverpackets.RecipeBookItemList;
import com.l2journey.gameserver.network.serverpackets.RecipeItemMakeInfo;
import com.l2journey.gameserver.network.serverpackets.RecipeShopItemInfo;
import com.l2journey.gameserver.network.serverpackets.SetupGauge;
import com.l2journey.gameserver.network.serverpackets.StatusUpdate;
import com.l2journey.gameserver.network.serverpackets.SystemMessage;
import com.l2journey.gameserver.taskmanagers.GameTimeTaskManager;
import com.l2journey.gameserver.util.Broadcast;

public class RecipeManager
{
	protected static final Map<Integer, RecipeItemMaker> _activeMakers = new ConcurrentHashMap<>();
	
	protected RecipeManager()
	{
		// Impede inicializacao externa.
	}
	
	public void requestBookOpen(Player player, boolean isDwarvenCraft)
	{
		// Verifica se o jogador esta tentando alterar o livro de receitas enquanto esta fabricando.
		if (!_activeMakers.containsKey(player.getObjectId()))
		{
			final RecipeBookItemList response = new RecipeBookItemList(isDwarvenCraft, player.getMaxMp());
			response.addRecipes(isDwarvenCraft ? player.getDwarvenRecipeBook() : player.getCommonRecipeBook());
			player.sendPacket(response);
			return;
		}
		player.sendPacket(SystemMessageId.YOU_MAY_NOT_ALTER_YOUR_RECIPE_BOOK_WHILE_ENGAGED_IN_MANUFACTURING);
	}
	
	public void requestMakeItemAbort(Player player)
	{
		_activeMakers.remove(player.getObjectId()); // TODO: fazer mais alguma coisa aqui?
	}
	
	public void requestManufactureItem(Player manufacturer, int recipeListId, Player player)
	{
		final RecipeList recipeList = RecipeData.getInstance().getValidRecipeList(player, recipeListId);
		if (recipeList == null)
		{
			return;
		}
		
		if (!manufacturer.getDwarvenRecipeBook().contains(recipeList) && !manufacturer.getCommonRecipeBook().contains(recipeList))
		{
			PunishmentManager.handleIllegalPlayerAction(player, "Warning!! Character " + player.getName() + " of account " + player.getAccountName() + " sent a false recipe id.", Config.DEFAULT_PUNISH);
			return;
		}
		
		// Verifica se o fabricante esta em loja de fabricacao ou loja privada.
		if (Config.ALT_GAME_CREATION && _activeMakers.containsKey(manufacturer.getObjectId()))
		{
			player.sendPacket(SystemMessageId.PLEASE_CLOSE_THE_SETUP_WINDOW_FOR_YOUR_PRIVATE_MANUFACTURING_STORE_OR_PRIVATE_STORE_AND_TRY_AGAIN);
			return;
		}
		
		final RecipeItemMaker maker = new RecipeItemMaker(manufacturer, recipeList, player);
		if (maker._isValid)
		{
			if (Config.ALT_GAME_CREATION)
			{
				_activeMakers.put(manufacturer.getObjectId(), maker);
				ThreadPool.schedule(maker, 100);
			}
			else
			{
				maker.run();
			}
		}
	}
	
	public void requestMakeItem(Player player, int recipeListId)
	{
		// Verifica se o jogador esta tentando operar loja privada ou oficina privada durante combate.
		if (player.isInCombat() || player.isInDuel())
		{
			player.sendPacket(SystemMessageId.WHILE_YOU_ARE_ENGAGED_IN_COMBAT_YOU_CANNOT_OPERATE_A_PRIVATE_STORE_OR_PRIVATE_WORKSHOP);
			return;
		}
		
		final RecipeList recipeList = RecipeData.getInstance().getValidRecipeList(player, recipeListId);
		if (recipeList == null)
		{
			return;
		}
		
		if (!player.getDwarvenRecipeBook().contains(recipeList) && !player.getCommonRecipeBook().contains(recipeList))
		{
			PunishmentManager.handleIllegalPlayerAction(player, "Warning!! Character " + player.getName() + " of account " + player.getAccountName() + " sent a false recipe id.", Config.DEFAULT_PUNISH);
			return;
		}
		
		// Verifica se o jogador esta ocupado, possivel quando alt game creation esta habilitado.
		if (Config.ALT_GAME_CREATION && _activeMakers.containsKey(player.getObjectId()))
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.S2_S1);
			sm.addItemName(recipeList.getItemId());
			sm.addString("You are busy creating.");
			player.sendPacket(sm);
			return;
		}
		
		final RecipeItemMaker maker = new RecipeItemMaker(player, recipeList, player);
		if (maker._isValid)
		{
			if (Config.ALT_GAME_CREATION)
			{
				_activeMakers.put(player.getObjectId(), maker);
				ThreadPool.schedule(maker, 100);
			}
			else
			{
				maker.run();
			}
		}
	}
	
	private static class RecipeItemMaker implements Runnable
	{
		private static final Logger LOGGER = Logger.getLogger(RecipeItemMaker.class.getName());
		protected boolean _isValid;
		protected List<TempItem> _items = null;
		protected final RecipeList _recipeList;
		protected final Player _player; // "crafter"
		protected final Player _target; // "customer"
		protected final Skill _skill;
		protected final int _skillId;
		protected final int _skillLevel;
		protected int _creationPasses = 1;
		protected int _itemGrab;
		protected int _exp = -1;
		protected int _sp = -1;
		protected long _price;
		protected int _totalItems;
		protected int _delay;
		
		public RecipeItemMaker(Player pPlayer, RecipeList pRecipeList, Player pTarget)
		{
			_player = pPlayer;
			_target = pTarget;
			_recipeList = pRecipeList;
			_isValid = false;
			_skillId = _recipeList.isDwarvenRecipe() ? CommonSkill.CREATE_DWARVEN.getId() : CommonSkill.CREATE_COMMON.getId();
			_skillLevel = _player.getSkillLevel(_skillId);
			_skill = _player.getKnownSkill(_skillId);
			_player.setCrafting(true);
			
			if (_player.isAlikeDead())
			{
				_player.sendPacket(ActionFailed.STATIC_PACKET);
				abort();
				return;
			}
			
			if (_target.isAlikeDead() || _target.isProcessingTransaction())
			{
				_target.sendPacket(ActionFailed.STATIC_PACKET);
				abort();
				return;
			}
			
			// Valida a lista de receita.
			// Valida o nivel da skill.
			if (_player.isProcessingTransaction() || (_recipeList.getRecipes().length == 0) || (_recipeList.getLevel() > _skillLevel))
			{
				_player.sendPacket(ActionFailed.STATIC_PACKET);
				abort();
				return;
			}
			
			// Verifica se o cliente pode pagar pelos servicos de criacao.
			if (_player != _target)
			{
				final ManufactureItem item = _player.getManufactureItems().get(_recipeList.getId());
				if (item != null)
				{
					_price = item.getCost();
					if (_target.getAdena() < _price) // check price
					{
						_target.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ENOUGH_ADENA);
						abort();
						return;
					}
				}
			}
			
			// Cria itens temporarios.
			_items = listItems(false);
			if (_items == null)
			{
				abort();
				return;
			}
			
			for (TempItem i : _items)
			{
				_totalItems += i.getQuantity();
			}
			
			// Checagens iniciais de statUse.
			if (!calculateStatUse(false, false))
			{
				abort();
				return;
			}
			
			// Checagens iniciais de AltStatChange.
			if (Config.ALT_GAME_CREATION)
			{
				calculateAltStatChange();
			}
			
			updateMakeInfo(true);
			updateCurMp();
			updateCurLoad();
			
			_player.setCrafting(false);
			_isValid = true;
		}
		
		@Override
		public void run()
		{
			if (!Config.IS_CRAFTING_ENABLED)
			{
				_target.sendMessage("Item creation is currently disabled.");
				abort();
				return;
			}
			
			if ((_player == null) || (_target == null))
			{
				LOGGER.warning("player or target == null (disconnected?), aborting" + _target + _player);
				abort();
				return;
			}
			
			if (Config.ALT_GAME_CREATION && !_activeMakers.containsKey(_player.getObjectId()))
			{
				if (_target != _player)
				{
					_target.sendMessage("Manufacture aborted");
					_player.sendMessage("Manufacture aborted");
				}
				else
				{
					_player.sendMessage("Item creation aborted");
				}
				
				abort();
				return;
			}
			
			if (Config.ALT_GAME_CREATION && !_items.isEmpty())
			{
				if (!calculateStatUse(true, true))
				{
					return; // check stat use
				}
				updateCurMp(); // update craft window mp bar
				grabSomeItems(); // grab (equip) some more items with a nice msg to player
				
				// Se ainda nao estiver vazio, agenda outra passada.
				if (!_items.isEmpty())
				{
					_delay = (int) (Config.ALT_GAME_CREATION_SPEED * _player.getMReuseRate(_skill) * GameTimeTaskManager.TICKS_PER_SECOND * GameTimeTaskManager.MILLIS_IN_TICK);
					
					// FIXME: corrigir este pacote para exibir a animacao de crafting.
					_player.broadcastPacket(new MagicSkillUse(_player, _skillId, _skillLevel, _delay, 0));
					_player.sendPacket(new SetupGauge(_player.getObjectId(), 0, _delay));
					ThreadPool.schedule(this, 100L + _delay);
				}
				else
				{
					// Para o modo alt, espera o delay em ms antes de finalizar.
					_player.sendPacket(new SetupGauge(_player.getObjectId(), 0, _delay));
					
					try
					{
						Thread.sleep(_delay);
					}
					catch (InterruptedException _)
					{
						Thread.currentThread().interrupt();
					}
					finally
					{
						finishCrafting();
					}
				}
			} // Para o modo de craft antigo, apenas finaliza.
			else
			{
				finishCrafting();
			}
		}
		
		private void finishCrafting()
		{
			if (!Config.ALT_GAME_CREATION)
			{
				calculateStatUse(false, true);
			}
			
			// Primeiro cobra a adena pela fabricacao.
			if ((_target != _player) && (_price > 0) && (_target.transferItem(ItemProcessType.TRANSFER, _target.getInventory().getAdenaInstance().getObjectId(), _price, _player.getInventory(), _player) == null)) // customer must pay for services
			{
				_target.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ENOUGH_ADENA);
				abort();
				return;
			}
			
			_items = listItems(true); // Esta linha realmente retira os materiais do inventario.
			if (_items != null)
			{
				final boolean success = Rnd.get(100) < _recipeList.getSuccessRate();
				if (success)
				{
					rewardPlayer();
				}
				else
				{
					if (_target != _player)
					{
						SystemMessage msg = new SystemMessage(SystemMessageId.YOUR_ATTEMPT_TO_CREATE_S2_FOR_C1_AT_THE_PRICE_OF_S3_ADENA_HAS_FAILED);
						msg.addString(_target.getName());
						msg.addItemName(_recipeList.getItemId());
						msg.addLong(_price);
						_player.sendPacket(msg);
						msg = new SystemMessage(SystemMessageId.C1_HAS_FAILED_TO_CREATE_S2_AT_THE_PRICE_OF_S3_ADENA);
						msg.addString(_player.getName());
						msg.addItemName(_recipeList.getItemId());
						msg.addLong(_price);
						_target.sendPacket(msg);
					}
					else
					{
						_target.sendPacket(SystemMessageId.YOU_FAILED_AT_MIXING_THE_ITEM);
					}
				}
				
				// A contagem da janela de criacao vem do inventario do cliente, entao a lista precisa chegar antes do updateMakeInfo.
				_target.sendItemList(false);
				updateMakeInfo(success);
			}
			// Atualiza a barra de carga e mana da janela de craft.
			updateCurMp();
			updateCurLoad();
			_activeMakers.remove(_player.getObjectId());
			_player.setCrafting(false);
		}
		
		private void updateMakeInfo(boolean success)
		{
			if (_target == _player)
			{
				_target.sendPacket(new RecipeItemMakeInfo(_recipeList.getId(), _target, success));
			}
			else
			{
				_target.sendPacket(new RecipeShopItemInfo(_player, _recipeList.getId()));
			}
		}
		
		private void updateCurLoad()
		{
			final StatusUpdate su = new StatusUpdate(_target);
			su.addAttribute(StatusUpdate.CUR_LOAD, _target.getCurrentLoad());
			_target.sendPacket(su);
		}
		
		private void updateCurMp()
		{
			final StatusUpdate su = new StatusUpdate(_target);
			su.addAttribute(StatusUpdate.CUR_MP, (int) _target.getCurrentMp());
			_target.sendPacket(su);
		}
		
		private void grabSomeItems()
		{
			int grabItems = _itemGrab;
			while ((grabItems > 0) && !_items.isEmpty())
			{
				final TempItem item = _items.get(0);
				final int count = item.getQuantity() >= grabItems ? grabItems : item.getQuantity();
				item.setQuantity(item.getQuantity() - count);
				if (item.getQuantity() <= 0)
				{
					_items.remove(0);
				}
				else
				{
					_items.set(0, item);
				}
				
				grabItems -= count;
				if (_target == _player)
				{
					final SystemMessage sm = new SystemMessage(SystemMessageId.EQUIPPED_S1_S2);
					sm.addLong(count);
					sm.addItemName(item.getItemId());
					_player.sendPacket(sm);
				}
				else
				{
					_target.sendMessage("Manufacturer " + _player.getName() + " used " + count + " " + item.getItemName());
				}
			}
		}
		
		// Os parametros de AltStatChange produzem efeito aqui.
		private void calculateAltStatChange()
		{
			_itemGrab = _skillLevel;
			for (RecipeStatHolder altStatChange : _recipeList.getAltStatChange())
			{
				if (altStatChange.getType() == StatType.XP)
				{
					_exp = altStatChange.getValue();
				}
				else if (altStatChange.getType() == StatType.SP)
				{
					_sp = altStatChange.getValue();
				}
				else if (altStatChange.getType() == StatType.GIM)
				{
					_itemGrab *= altStatChange.getValue();
				}
			}
			// Determina o numero de passagens necessarias para a criacao.
			_creationPasses = (_totalItems / _itemGrab) + ((_totalItems % _itemGrab) != 0 ? 1 : 0);
			if (_creationPasses < 1)
			{
				_creationPasses = 1;
			}
		}
		
		// StatUse.
		private boolean calculateStatUse(boolean isWait, boolean isReduce)
		{
			boolean ret = true;
			for (RecipeStatHolder statUse : _recipeList.getStatUse())
			{
				final double modifiedValue = statUse.getValue() / _creationPasses;
				if (statUse.getType() == StatType.HP)
				{
					// Nao queremos matar o jogador, entao o CurrentHP precisa ser maior que o valor de reducao.
					if (_player.getCurrentHp() <= modifiedValue)
					{
						// Descanso, espera por HP.
						if (Config.ALT_GAME_CREATION && isWait)
						{
							_player.sendPacket(new SetupGauge(_player.getObjectId(), 0, _delay));
							ThreadPool.schedule(this, 100L + _delay);
						}
						else
						{
							_target.sendPacket(SystemMessageId.NOT_ENOUGH_HP);
							abort();
						}
						ret = false;
					}
					else if (isReduce)
					{
						_player.reduceCurrentHp(modifiedValue, _player, _skill);
					}
				}
				else if (statUse.getType() == StatType.MP)
				{
					if (_player.getCurrentMp() < modifiedValue)
					{
						// Descanso, espera por MP.
						if (Config.ALT_GAME_CREATION && isWait)
						{
							_player.sendPacket(new SetupGauge(_player.getObjectId(), 0, _delay));
							ThreadPool.schedule(this, 100L + _delay);
						}
						else
						{
							_target.sendPacket(SystemMessageId.NOT_ENOUGH_MP);
							abort();
						}
						ret = false;
					}
					else if (isReduce)
					{
						_player.reduceCurrentMp(modifiedValue);
					}
				}
				else
				{
					// Existe um valor de StatUse desconhecido.
					_target.sendMessage("Recipe error!!!, please tell this to your GM.");
					ret = false;
					abort();
				}
			}
			return ret;
		}
		
		private List<TempItem> listItems(boolean remove)
		{
			final RecipeHolder[] recipes = _recipeList.getRecipes();
			final Inventory inv = _target.getInventory();
			final List<TempItem> materials = new ArrayList<>();
			SystemMessage sm;
			for (RecipeHolder recipe : recipes)
			{
				if (recipe.getQuantity() > 0)
				{
					final Item item = inv.getItemByItemId(recipe.getItemId());
					final long itemQuantityAmount = item == null ? 0 : item.getCount();
					
					// Verifica os materiais.
					if (itemQuantityAmount < recipe.getQuantity())
					{
						sm = new SystemMessage(SystemMessageId.YOU_ARE_MISSING_S2_S1_REQUIRED_TO_CREATE_THAT);
						sm.addItemName(recipe.getItemId());
						sm.addLong(recipe.getQuantity() - itemQuantityAmount);
						_target.sendPacket(sm);
						
						abort();
						return null;
					}
					
					// Cria um novo objeto temporario apenas para contagem.
					materials.add(new TempItem(item, recipe.getQuantity()));
				}
			}
			
			if (remove)
			{
				for (TempItem tmp : materials)
				{
					inv.destroyItemByItemId(ItemProcessType.FEE, tmp.getItemId(), tmp.getQuantity(), _target, _player);
					if (tmp.getQuantity() > 1)
					{
						sm = new SystemMessage(SystemMessageId.S2_S1_HAS_DISAPPEARED);
						sm.addItemName(tmp.getItemId());
						sm.addLong(tmp.getQuantity());
					}
					else
					{
						sm = new SystemMessage(SystemMessageId.S1_HAS_DISAPPEARED);
						sm.addItemName(tmp.getItemId());
					}
					_target.sendPacket(sm);
				}
			}
			return materials;
		}
		
		private void abort()
		{
			updateMakeInfo(false);
			_player.setCrafting(false);
			_activeMakers.remove(_player.getObjectId());
		}
		
		private void rewardPlayer()
		{
			final int rareProdId = _recipeList.getRareItemId();
			int itemId = _recipeList.getItemId();
			int itemCount = _recipeList.getCount();
			final ItemTemplate template = ItemData.getInstance().getTemplate(itemId);
			
			// Conquista: craft concluido com sucesso, apenas 1 incremento por receita independentemente da quantidade.
			try
			{
				_player.getCounters().onCraftSuccess();
			}
			catch (Exception _)
			{
				// Nao interrompe o fluxo de craft.
			}
			
			// Verifica se a receita atual possui producao rara.
			if (((rareProdId != -1) && ((rareProdId == itemId) || Config.CRAFT_MASTERWORK)) && (Rnd.get(100) < (_recipeList.getRarity() * Config.CRAFT_MASTERWORK_CHANCE_RATE)))
			{
				itemId = rareProdId;
				itemCount = _recipeList.getRareCount();
			}
			
			_target.getInventory().addItem(ItemProcessType.CRAFT, itemId, itemCount, _target, _player);
			
			// Anuncia para todo o servidor se o item craftado estiver na lista de anuncios.
			if (Config.ANNOUNCE_CRAFT_SUCCESS && Config.ANNOUNCE_CRAFT_SUCCESS_ITEMS.contains(itemId))
			{
				final ItemTemplate craftedTemplate = ItemData.getInstance().getTemplate(itemId);
				final String itemName = (craftedTemplate != null) ? craftedTemplate.getName() : String.valueOf(itemId);
				Broadcast.toAllOnlinePlayers("Player " + _player.getName() + ": successfully crafted the item " + itemName + ".");
			}
			
			// Informa ao cliente o item obtido.
			SystemMessage sm = null;
			if (_target != _player)
			{
				// Informa ao fabricante o lucro obtido.
				if (itemCount == 1)
				{
					sm = new SystemMessage(SystemMessageId.S2_HAS_BEEN_CREATED_FOR_C1_AFTER_THE_PAYMENT_OF_S3_ADENA_WAS_RECEIVED);
					sm.addString(_target.getName());
					sm.addItemName(itemId);
					sm.addLong(_price);
					_player.sendPacket(sm);
					
					sm = new SystemMessage(SystemMessageId.C1_CREATED_S2_AFTER_RECEIVING_S3_ADENA);
					sm.addString(_player.getName());
					sm.addItemName(itemId);
					sm.addLong(_price);
					_target.sendPacket(sm);
				}
				else
				{
					sm = new SystemMessage(SystemMessageId.S2_S3_HAVE_BEEN_CREATED_FOR_C1_AT_THE_PRICE_OF_S4_ADENA);
					sm.addString(_target.getName());
					sm.addInt(itemCount);
					sm.addItemName(itemId);
					sm.addLong(_price);
					_player.sendPacket(sm);
					
					sm = new SystemMessage(SystemMessageId.C1_CREATED_S2_S3_AT_THE_PRICE_OF_S4_ADENA);
					sm.addString(_player.getName());
					sm.addInt(itemCount);
					sm.addItemName(itemId);
					sm.addLong(_price);
					_target.sendPacket(sm);
				}
			}
			
			if (itemCount > 1)
			{
				sm = new SystemMessage(SystemMessageId.YOU_HAVE_EARNED_S2_S1_S);
				sm.addItemName(itemId);
				sm.addLong(itemCount);
			}
			else
			{
				sm = new SystemMessage(SystemMessageId.YOU_HAVE_EARNED_S1);
				sm.addItemName(itemId);
			}
			_target.sendPacket(sm);
			
			if (Config.ALT_GAME_CREATION)
			{
				final int recipeLevel = _recipeList.getLevel();
				if (_exp < 0)
				{
					_exp = template.getReferencePrice() * itemCount;
					_exp /= recipeLevel;
				}
				if (_sp < 0)
				{
					_sp = _exp / 10;
				}
				if (itemId == rareProdId)
				{
					_exp *= Config.ALT_GAME_CREATION_RARE_XPSP_RATE;
					_sp *= Config.ALT_GAME_CREATION_RARE_XPSP_RATE;
				}
				
				if (_exp < 0)
				{
					_exp = 0;
				}
				if (_sp < 0)
				{
					_sp = 0;
				}
				
				for (int i = _skillLevel; i > recipeLevel; i--)
				{
					_exp /= 4;
					_sp /= 4;
				}
				
				// Adiciona a multiplicacao da velocidade de criacao ao ganho de XP/SP: crafting mais lento gera mais XP,
				// crafting mais rapido gera menos XP. E possivel usar ALT_GAME_CREATION_XP_RATE/SP para ajustar o XP/SP ganho, padrao = 1.
				_player.addExpAndSp((int) _player.calcStat(Stat.EXPSP_RATE, _exp * Config.ALT_GAME_CREATION_XP_RATE * Config.ALT_GAME_CREATION_SPEED, null, null), (int) _player.calcStat(Stat.EXPSP_RATE, _sp * Config.ALT_GAME_CREATION_SP_RATE * Config.ALT_GAME_CREATION_SPEED, null, null));
			}
		}
	}
	
	public static RecipeManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final RecipeManager INSTANCE = new RecipeManager();
	}
}
