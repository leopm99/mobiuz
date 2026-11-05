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
package com.l2journey.gameserver.model.phantom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.l2journey.Config;
import com.l2journey.PhantomConfig;
import com.l2journey.commons.util.Rnd;
import com.l2journey.gameserver.data.sql.ClanTable;
import com.l2journey.gameserver.data.sql.CrestTable;
import com.l2journey.gameserver.model.Crest;
import com.l2journey.gameserver.model.clan.Clan;
import com.l2journey.gameserver.model.clan.enums.CrestType;

/**
 * Unit tests for phantom clan crest DDS validation and clan-count gate helpers.
 */
class PhantomClanCrestTest
{
	@Test
	void isValidPledgeCrestAcceptsDxt1SizedBuffer()
	{
		byte[] data = new byte[128];
		writeIntLE(data, 0, 0x20534444); // DDS
		writeIntLE(data, 84, 0x31545844); // DXT1
		assertTrue(PhantomClan.isValidPledgeCrest(data));
	}

	@Test
	void isValidPledgeCrestRejectsBadMagicSizeOrCodec()
	{
		assertFalse(PhantomClan.isValidPledgeCrest(null));
		assertFalse(PhantomClan.isValidPledgeCrest(new byte[64]));

		byte[] tooBig = new byte[300];
		writeIntLE(tooBig, 0, 0x20534444);
		writeIntLE(tooBig, 84, 0x31545844);
		assertFalse(PhantomClan.isValidPledgeCrest(tooBig));

		byte[] wrongCodec = new byte[128];
		writeIntLE(wrongCodec, 0, 0x20534444);
		writeIntLE(wrongCodec, 84, 0x30315844); // DXT3-ish
		assertFalse(PhantomClan.isValidPledgeCrest(wrongCodec));

		byte[] wrongMagic = new byte[128];
		Arrays.fill(wrongMagic, (byte) 1);
		assertFalse(PhantomClan.isValidPledgeCrest(wrongMagic));
	}

	@Test
	void canCreatePhantomClanStopsAtFive()
	{
		assertTrue(PhantomClan.canCreatePhantomClan(0));
		assertTrue(PhantomClan.canCreatePhantomClan(4));
		assertFalse(PhantomClan.canCreatePhantomClan(5));
		assertFalse(PhantomClan.canCreatePhantomClan(6));
	}

	@Test
	void assignRandomCrestSkipsNullClan()
	{
		PhantomClan.assignRandomCrest(null);
	}

	@Test
	void countExistingPhantomClansUsesTheClanTable()
	{
		ClanTable table = mock(ClanTable.class);
		Clan clan = mock(Clan.class);
		when(clan.getLeaderId()).thenReturn(42);
		List<Integer> previousIds = new ArrayList<>(PhantomConfig.PHANTOM_IDS);
		PhantomConfig.PHANTOM_IDS.clear();
		PhantomConfig.PHANTOM_IDS.add(42);
		try (MockedStatic<ClanTable> clans = mockStatic(ClanTable.class))
		{
			clans.when(ClanTable::getInstance).thenReturn(table);
			when(table.getClanByName(anyString())).thenReturn(null);
			when(table.getClanByName("IronWolves")).thenReturn(clan);
			assertEquals(1, PhantomClan.countExistingPhantomClans());
			assertTrue(PhantomClan.canCreatePhantomClan());

			when(table.getClanByName(anyString())).thenReturn(clan);
			assertFalse(PhantomClan.canCreatePhantomClan());
		}
		finally
		{
			PhantomConfig.PHANTOM_IDS.clear();
			PhantomConfig.PHANTOM_IDS.addAll(previousIds);
		}
	}

	@Test
	void playerClanWithAReservedNameIsNotAPhantomClan()
	{
		List<Integer> previousIds = new ArrayList<>(PhantomConfig.PHANTOM_IDS);
		PhantomConfig.PHANTOM_IDS.clear();
		PhantomConfig.PHANTOM_IDS.add(42);
		try
		{
			assertFalse(PhantomClan.isPhantomClan(null));

			Clan playerClan = mock(Clan.class);
			when(playerClan.getLeaderId()).thenReturn(7);
			when(playerClan.getMembersCount()).thenReturn(1);
			assertFalse(PhantomClan.isPhantomClan(playerClan));
			assertFalse(PhantomClan.isJoinablePhantomClan(playerClan));

			Clan phantomClan = mock(Clan.class);
			when(phantomClan.getLeaderId()).thenReturn(42);
			when(phantomClan.getMembersCount()).thenReturn(15);
			assertTrue(PhantomClan.isPhantomClan(phantomClan));
			assertFalse(PhantomClan.isJoinablePhantomClan(phantomClan));
			when(phantomClan.getMembersCount()).thenReturn(14);
			assertTrue(PhantomClan.isJoinablePhantomClan(phantomClan));

			ClanTable table = mock(ClanTable.class);
			try (MockedStatic<ClanTable> clans = mockStatic(ClanTable.class);
				MockedStatic<Rnd> rnd = mockStatic(Rnd.class))
			{
				clans.when(ClanTable::getInstance).thenReturn(table);
				rnd.when(() -> Rnd.get(anyInt())).thenReturn(0);
				when(table.getClanByName(anyString())).thenReturn(playerClan);
				assertEquals(0, PhantomClan.countExistingPhantomClans());
				assertNull(PhantomClan.findPhantomClanToJoin());

				when(table.getClanByName("IronWolves")).thenReturn(phantomClan);
				assertSame(phantomClan, PhantomClan.findPhantomClanToJoin());

				rnd.when(() -> Rnd.get(anyInt())).thenReturn(-1);
				assertNull(PhantomClan.findPhantomClanToJoin());
				rnd.when(() -> Rnd.get(anyInt())).thenReturn(1000);
				assertNull(PhantomClan.findPhantomClanToJoin());
			}
		}
		finally
		{
			PhantomConfig.PHANTOM_IDS.clear();
			PhantomConfig.PHANTOM_IDS.addAll(previousIds);
		}
	}

	@Test
	void assignRandomCrestAppliesAValidFileAndSkipsBadOnes() throws Exception
	{
		File root = Files.createTempDirectory("phantom-crests").toFile();
		File crestDir = new File(root, "data/PhantomCrests");
		assertTrue(crestDir.mkdirs());
		byte[] valid = new byte[128];
		writeIntLE(valid, 0, 0x20534444);
		writeIntLE(valid, 84, 0x31545844);
		Files.write(new File(crestDir, "good.dds").toPath(), valid);
		Files.write(new File(crestDir, "bad.dds").toPath(), new byte[128]);
		assertTrue(new File(crestDir, "broken.dds").mkdir());

		File previous = Config.DATAPACK_ROOT;
		Config.DATAPACK_ROOT = new File(root, "missing");
		PhantomClan.assignRandomCrest(mock(Clan.class));

		Config.DATAPACK_ROOT = root;
		try
		{
			Clan clan = mock(Clan.class);
			CrestTable table = mock(CrestTable.class);
			Crest crest = mock(Crest.class);
			when(crest.getId()).thenReturn(9);
			when(table.createCrest(any(), eq(CrestType.PLEDGE))).thenReturn(crest);
			try (MockedStatic<CrestTable> crests = mockStatic(CrestTable.class))
			{
				crests.when(CrestTable::getInstance).thenReturn(table);
				PhantomClan.assignRandomCrest(clan);
			}
			verify(clan).changeClanCrest(9);

			when(table.createCrest(any(), eq(CrestType.PLEDGE))).thenReturn(null);
			try (MockedStatic<CrestTable> crests = mockStatic(CrestTable.class))
			{
				crests.when(CrestTable::getInstance).thenReturn(table);
				PhantomClan.assignRandomCrest(clan);
			}
			verify(clan).changeClanCrest(9);

			when(table.createCrest(any(), eq(CrestType.PLEDGE))).thenThrow(new IllegalStateException("db"));
			try (MockedStatic<CrestTable> crests = mockStatic(CrestTable.class))
			{
				crests.when(CrestTable::getInstance).thenReturn(table);
				PhantomClan.assignRandomCrest(clan);
			}
		}
		finally
		{
			Config.DATAPACK_ROOT = previous;
			Files.walk(root.toPath()).sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
		}
	}

	private static void writeIntLE(byte[] data, int offset, int value)
	{
		data[offset] = (byte) (value & 0xFF);
		data[offset + 1] = (byte) ((value >> 8) & 0xFF);
		data[offset + 2] = (byte) ((value >> 16) & 0xFF);
		data[offset + 3] = (byte) ((value >> 24) & 0xFF);
	}
}
