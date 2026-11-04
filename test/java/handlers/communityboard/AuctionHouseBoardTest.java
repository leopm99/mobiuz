package handlers.communityboard;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import com.l2journey.gameserver.auctionhouse.AuctionHouseManager;
import com.l2journey.gameserver.auctionhouse.AuctionListing;
import com.l2journey.gameserver.cache.HtmCache;
import com.l2journey.gameserver.handler.CommunityBoardHandler;
import com.l2journey.gameserver.model.actor.Player;

class AuctionHouseBoardTest
{
	private MockedStatic<AuctionHouseManager> managers;
	private MockedStatic<HtmCache> caches;
	private MockedStatic<CommunityBoardHandler> boards;
	private AuctionHouseManager manager;
	private Player player;
	private AuctionHouseBoard board;
	private List<String> pages;
	@BeforeEach void setup() throws Exception
	{
		manager=mock(AuctionHouseManager.class); managers=mockStatic(AuctionHouseManager.class);
		managers.when(AuctionHouseManager::getInstance).thenReturn(manager);
		player=mock(Player.class,RETURNS_DEEP_STUBS); when(player.getObjectId()).thenReturn(80);
		when(player.getClient().getFloodProtectors().canPerformTransaction()).thenReturn(true);
		caches=mockStatic(HtmCache.class); HtmCache cache=mock(HtmCache.class); caches.when(HtmCache::getInstance).thenReturn(cache);
		when(cache.getHtm(eq(player),anyString())).thenReturn(Files.readString(Path.of("dist/game/data/html/CommunityBoard/auction/main.html")));
		pages=new ArrayList<>(); boards=mockStatic(CommunityBoardHandler.class);
		boards.when(() -> CommunityBoardHandler.separateAndSend(anyString(),eq(player))).thenAnswer(i -> {pages.add(i.getArgument(0)); return null;});
		board=new AuctionHouseBoard();
		AuctionListing listing=new AuctionListing(1,10,100,57001,"Sword <test> & \"quoted\"", "icon.weapon_sword_i00","Weapons","S",4,40,100,System.currentTimeMillis()+600000);
		when(manager.listing(1)).thenReturn(listing);
		when(manager.listings(0)).thenReturn(java.util.stream.IntStream.range(1,16).mapToObj(i -> new AuctionListing(i,10,100+i,57001,"Sword "+i,"icon.weapon_sword_i00","Weapons","S",4,40,100,System.currentTimeMillis()+600000)).toList());
	}
	@AfterEach void cleanup() {boards.close(); caches.close(); managers.close();}
	private void open(String command)
	{
		assertTrue(board.parseCommunityBoardCommand(command,player)); assertFalse(pages.isEmpty());
		String html=pages.getLast(); assertTrue(html.length()<16383); assertFalse(html.contains("%content%"));
		assertFalse(html.contains("<style")); assertFalse(html.contains("javascript"));
		var stack=new ArrayDeque<String>(); var tags=Pattern.compile("<(\\/?)(table|tr|td)(?:\\s[^>]*|)>").matcher(html);
		while(tags.find()) { if(tags.group(1).isEmpty()) stack.push(tags.group(2)); else assertEquals(tags.group(2),stack.pop()); }
		assertTrue(stack.isEmpty());
	}
	@Test void marketFiltersAndPaginationStayWithinNativePacketBudget()
	{
		open("_bbsauction"); open("_bbsauction;list;market;Weapons;Sword;1;Price");
		assertTrue(pages.getLast().contains("Page 2 / 3")); assertTrue(pages.getLast().contains(";Weapons;Sword;0;Price"));
	}
	@Test void detailAndConfirmationEscapeNamesAndNeverChargeBeforeConfirm()
	{
		open("_bbsauction;buy;1;5");
		assertTrue(pages.getLast().contains("&lt;test&gt;")); assertFalse(pages.getLast().contains("Sword <test>"));
		verify(manager,never()).buy(any(),anyInt(),anyLong(),anyLong());
		var matcher=Pattern.compile("bypass _bbsauction;confirm;([a-f0-9-]+)").matcher(pages.getLast()); assertTrue(matcher.find());
		String action="_bbsauction;confirm;"+matcher.group(1);
		open(action); open(action);
		verify(manager,times(1)).buy(player,1,5,100);
	}
	@Test void forgedOrForeignConfirmationCannotCharge()
	{
		open("_bbsauction;confirm;00000000-0000-0000-0000-000000000000");
		verify(manager,never()).buy(any(),anyInt(),anyLong(),anyLong());
	}
	@Test void clientFormattedListingPriceReachesReviewAndChargesOnlyAfterConfirmation()
	{
		var item=mock(com.l2journey.gameserver.model.item.instance.Item.class,RETURNS_DEEP_STUBS);
		when(player.checkItemManipulation(100,20,"auction")).thenReturn(item);
		when(manager.canList(player,item)).thenReturn(true);
		when(item.getTemplate().getName()).thenReturn("GiantBootss Codex - Mastery");
		when(manager.listingFee(20,20000000,9)).thenReturn(18000000L);
		open("_bbsauction;quote;100;20;20,000,000;9");
		assertTrue(pages.getLast().contains("20,000,000 Adena each"));
		verify(manager,never()).create(any(),anyInt(),anyLong(),anyLong(),anyInt(),anyLong());
		var matcher=Pattern.compile("bypass _bbsauction;confirm;([a-f0-9-]+)").matcher(pages.getLast()); assertTrue(matcher.find());
		open("_bbsauction;confirm;"+matcher.group(1));
		verify(manager).create(player,100,20,20000000,9,18000000);
	}
	@Test void clientFormattedPurchaseQuantityIsAccepted()
	{
		when(manager.listing(1)).thenReturn(new AuctionListing(1,10,100,57001,"Crystal","icon.etc_adena_i00","Other","NONE",0,2000,100,Long.MAX_VALUE));
		open("_bbsauction;buy;1;1,000");
		assertTrue(pages.getLast().contains("100,000 Adena"));
		verify(manager,never()).buy(any(),anyInt(),anyLong(),anyLong());
	}
	@Test void priceFieldAndReviewSupportTenBillionWithoutClientNumericControl()
	{
		var item=mock(com.l2journey.gameserver.model.item.instance.Item.class,RETURNS_DEEP_STUBS);
		when(player.getInventory().getItemByObjectId(100)).thenReturn(item);
		when(player.checkItemManipulation(100,1,"auction")).thenReturn(item);
		when(manager.canList(player,item)).thenReturn(true);
		when(item.getTemplate().getName()).thenReturn("Sword");
		when(manager.listingFee(1,10000000000L,1)).thenReturn(50000000L);
		managers.when(() -> AuctionHouseManager.describe(0,80,item,0,0,0))
			.thenReturn(new AuctionListing(0,80,100,57001,"Sword","icon.etc_adena_i00","Weapons","S",0,1,0,0));
		open("_bbsauction;sell;100");
		assertTrue(pages.getLast().contains("<edit var=price width=180 height=18 length=14>"));
		open("_bbsauction;quote;100;1;10,000,000,000;1");
		assertTrue(pages.getLast().contains("10,000,000,000 Adena each"));
		verify(manager).listingFee(1,10000000000L,1);
		verify(manager,never()).create(any(),anyInt(),anyLong(),anyLong(),anyInt(),anyLong());
	}
	@Test void malformedGroupingAndDecimalsCannotReachListingFeeOrPurchase()
	{
		for(String value:List.of("20,00,000","20,000,00","20,,000","20,000.00","20.5",",200","-20,000","1,000,000,000,000,000,000"))
		{
			open("_bbsauction;quote;100;20;"+value+";9");
		}
		verify(manager,never()).listingFee(anyLong(),anyLong(),anyInt());
		verify(manager,never()).create(any(),anyInt(),anyLong(),anyLong(),anyInt(),anyLong());
	}
	private String fillButton(String label, java.util.Map<String,String> values)
	{
		var button=Pattern.compile("value=\""+Pattern.quote(label)+"\" action=\"bypass ([^\"]+)\"").matcher(pages.getLast());
		assertTrue(button.find(), "Missing button: "+label);
		// High Five form variables must be separate whitespace-delimited tokens.
		// In particular, $quantity;$price is not a valid variable name.
		var variables=Pattern.compile("\\$([^\\s]+)").matcher(button.group(1));
		var command=new StringBuilder();
		while(variables.find())
		{
			String replacement=values.get(variables.group(1));
			assertNotNull(replacement, "Invalid client variable token: "+variables.group(1));
			variables.appendReplacement(command,java.util.regex.Matcher.quoteReplacement(replacement));
		}
		variables.appendTail(command);
		return command.toString();
	}
	@Test void generatedListingButtonSubstitutesEachFieldBeforeParsing()
	{
		var item=mock(com.l2journey.gameserver.model.item.instance.Item.class,RETURNS_DEEP_STUBS);
		when(player.getInventory().getItemByObjectId(100)).thenReturn(item);
		when(player.checkItemManipulation(100,1,"auction")).thenReturn(item);
		when(manager.canList(player,item)).thenReturn(true);
		when(item.getCount()).thenReturn(22L);
		when(item.getTemplate().getName()).thenReturn("Codex");
		managers.when(() -> AuctionHouseManager.describe(0,80,item,22,0,0))
			.thenReturn(new AuctionListing(0,80,100,57001,"Codex","icon.etc_adena_i00","Other","NONE",0,22,0,0));
		when(manager.listingFee(1,1,1)).thenReturn(10000L);
		open("_bbsauction;sell;100");
		String command=fillButton("Review listing",java.util.Map.of("quantity","1","price","1","days","1"));
		assertEquals("_bbsauction;quote;100; 1 ; 1 ; 1 ",command);
		open(command);
		verify(manager).listingFee(1,1,1);
		assertTrue(pages.getLast().contains("bypass _bbsauction;confirm;"));
		verify(manager,never()).create(any(),anyInt(),anyLong(),anyLong(),anyInt(),anyLong());
	}
	@Test void generatedSearchButtonSubstitutesComboboxAndEditValues()
	{
		open("_bbsauction");
		open(fillButton("Search",java.util.Map.of("category","Weapons","query","Sword","sort","Price")));
		assertTrue(pages.getLast().contains("Search: Sword"));
		assertTrue(pages.getLast().contains("Page 1 / 3"));
		assertFalse(pages.getLast().contains("Invalid filter"));
	}
	@Test void generatedPurchaseButtonSubstitutesQuantity()
	{
		try(var info=mockStatic(com.l2journey.gameserver.data.sql.CharInfoTable.class))
		{
			var names=mock(com.l2journey.gameserver.data.sql.CharInfoTable.class);
			info.when(com.l2journey.gameserver.data.sql.CharInfoTable::getInstance).thenReturn(names);
			when(names.getNameById(10)).thenReturn("Seller");
			open("_bbsauction;view;1");
			open(fillButton("Review purchase",java.util.Map.of("quantity","1")));
			assertTrue(pages.getLast().contains("100 Adena"));
			assertTrue(pages.getLast().contains("bypass _bbsauction;confirm;"));
			verify(manager,never()).buy(any(),anyInt(),anyLong(),anyLong());
		}
	}
	@Test void malformedBypassesCannotReachFinancialOperations()
	{
		for(String command:List.of("_bbsauction;buy;1;-1","_bbsauction;buy;1;0","_bbsauction;buy;1;5;ignored","_bbsauction;list;market;All;\"<html>;0;Price","_bbsauction;cancel;1")) open(command);
		verify(manager,never()).buy(any(),anyInt(),anyLong(),anyLong()); verify(manager,never()).cancel(any(),anyInt());
	}
}
