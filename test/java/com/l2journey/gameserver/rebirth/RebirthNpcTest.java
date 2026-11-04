package com.l2journey.gameserver.rebirth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.instance.RebirthInstance;
import com.l2journey.gameserver.network.serverpackets.NpcHtmlMessage;

class RebirthNpcTest
{
	@Test
	void configuredNpcClassExistsAndTheTrainerHasTheStandardBypass() throws Exception
	{
		final String npcs = Files.readString(Path.of("dist/game/data/stats/npcs/custom/custom_l2journey.xml"));
		assertTrue(npcs.contains("type=\"RebirthInstance\""));
		assertNotNull(Class.forName("com.l2journey.gameserver.model.actor.instance.RebirthInstance").getConstructor(com.l2journey.gameserver.model.actor.templates.NpcTemplate.class));
		assertTrue(Files.readString(Path.of("dist/game/data/html/trainer/70003.htm")).contains("npc_%objectId%_SkillList"));
	}
	@Test
	void outOfRangeNpcCannotPerformRebirth()
	{
		try (var manager = mockStatic(RebirthManager.class))
		{
			final RebirthInstance npc = mock(RebirthInstance.class, CALLS_REAL_METHODS);
			final Player player = mock(Player.class);
			doReturn(false).when(npc).canInteract(player);
			npc.onBypassFeedback(player, "performRebirth");
			manager.verifyNoInteractions();
		}
	}
	@Test
	void dialogueUsesTheRebirthHtmlAndReplacesTheNpcObjectId()
	{
		try (var html = mockConstruction(NpcHtmlMessage.class))
		{
			final RebirthInstance npc = mock(RebirthInstance.class, CALLS_REAL_METHODS);
			final Player player = mock(Player.class);
			doReturn(true).when(npc).canInteract(player);
			doReturn(1234).when(npc).getObjectId();
			npc.showChatWindow(player);
			final NpcHtmlMessage packet = html.constructed().getFirst();
			verify(packet).setFile(player, "data/html/rebirth/rebirth.htm");
			verify(packet).replace("%objectId%", 1234);
			verify(player).sendPacket(packet);
		}
	}
}
