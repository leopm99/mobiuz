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
package com.l2journey.gameserver.model.actor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.junit.jupiter.api.Test;

import com.l2journey.commons.database.DatabaseFactory;
import com.l2journey.gameserver.model.actor.appearance.PlayerAppearance;
import com.l2journey.gameserver.model.variables.PlayerVariables;

class PlayerCommunityColorsTest
{
	@Test
	void restoredDatabaseVariablesOverrideTemporaryLoginColorsAndSurviveLogoutSave() throws Exception
	{
		try (var database = mockStatic(DatabaseFactory.class))
		{
			final Connection connection = mock(Connection.class);
			final PreparedStatement statement = mock(PreparedStatement.class);
			final ResultSet result = mock(ResultSet.class);
			database.when(DatabaseFactory::getConnection).thenReturn(connection);
			when(connection.prepareStatement(anyString())).thenReturn(statement);
			when(statement.executeQuery()).thenReturn(result);
			when(result.next()).thenReturn(true, true, false);
			when(result.getString("var")).thenReturn(PlayerVariables.COMMUNITY_NAME_COLOR, PlayerVariables.COMMUNITY_TITLE_COLOR);
			when(result.getString("val")).thenReturn("255", "16711680");

			// Construct a fresh variable container just as a newly loaded player does.
			final PlayerVariables variables = new PlayerVariables(42);
			final PlayerAppearance appearance = new PlayerAppearance((byte) 0, (byte) 0, (byte) 0, false);
			final Player player = mock(Player.class);
			when(player.getVariables()).thenReturn(variables);
			when(player.getAppearance()).thenReturn(appearance);
			doCallRealMethod().when(player).restoreCommunityColors();
			player.restoreCommunityColors();
			assertEquals(255, appearance.getNameColor());
			assertEquals(0xFF0000, appearance.getTitleColor());

			// Faction/PvP login defaults must not erase the purchased preference.
			appearance.setNameColor(0x112233);
			appearance.setTitleColor(0x445566);
			player.restoreCommunityColors();
			assertEquals(255, appearance.getNameColor());
			assertEquals(0xFF0000, appearance.getTitleColor());

			// Logout stores the preference independently of the displayed colors.
			variables.set("OTHER_SETTING", true);
			variables.storeMe();
			verify(statement).setString(3, "255");
			verify(statement).setString(3, "16711680");
			assertEquals(255, variables.getInt(PlayerVariables.COMMUNITY_NAME_COLOR));
			assertEquals(0xFF0000, variables.getInt(PlayerVariables.COMMUNITY_TITLE_COLOR));
		}
	}

	@Test
	void playersWithoutPurchasesKeepExistingColors()
	{
		final PlayerVariables variables = mock(PlayerVariables.class);
		when(variables.getInt(anyString(), org.mockito.ArgumentMatchers.anyInt())).thenAnswer(i -> i.getArgument(1));
		final PlayerAppearance appearance = new PlayerAppearance((byte) 0, (byte) 0, (byte) 0, false);
		appearance.setNameColor(0x123456);
		appearance.setTitleColor(0x654321);
		final Player player = mock(Player.class);
		when(player.getVariables()).thenReturn(variables);
		when(player.getAppearance()).thenReturn(appearance);
		doCallRealMethod().when(player).restoreCommunityColors();
		player.restoreCommunityColors();
		assertEquals(0x123456, appearance.getNameColor());
		assertEquals(0x654321, appearance.getTitleColor());
	}
}
