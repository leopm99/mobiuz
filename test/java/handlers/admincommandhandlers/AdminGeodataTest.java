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
package handlers.admincommandhandlers;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.l2journey.gameserver.GeoData;
import com.l2journey.gameserver.model.World;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.util.GeoUtils;

/**
 * Unit tests for {@link AdminGeodata} geomap and geocell commands.
 */
class AdminGeodataTest
{
	@Test
	void getAdminCommandListContainsGeomapAndGeocell()
	{
		final String[] commands = new AdminGeodata().getAdminCommandList();
		assertTrue(Arrays.asList(commands).contains("admin_geomap"));
		assertTrue(Arrays.asList(commands).contains("admin_geocell"));
	}
	
	@Test
	void geomapSendsFormattedRegionMessage()
	{
		final Player player = mock(Player.class);
		when(player.getX()).thenReturn(World.WORLD_X_MIN);
		when(player.getY()).thenReturn(World.WORLD_Y_MIN);
		
		new AdminGeodata().useAdminCommand("admin_geomap", player);
		
		verify(player).sendMessage(GeoUtils.formatGeoMap(World.WORLD_X_MIN, World.WORLD_Y_MIN));
	}
	
	@Test
	void geocellSendsCellCoordinates()
	{
		final Player player = mock(Player.class);
		when(player.getX()).thenReturn(1000);
		when(player.getY()).thenReturn(2000);
		when(player.getZ()).thenReturn(3000);
		
		final GeoData geoData = mock(GeoData.class);
		when(geoData.getGeoX(1000)).thenReturn(10);
		when(geoData.getGeoY(2000)).thenReturn(20);
		when(geoData.getNearestZ(10, 20, 3000)).thenReturn(3050);
		when(geoData.getWorldX(10)).thenReturn(1016);
		when(geoData.getWorldY(20)).thenReturn(2016);
		
		try (MockedStatic<GeoData> geoStatic = mockStatic(GeoData.class))
		{
			geoStatic.when(GeoData::getInstance).thenReturn(geoData);
			
			new AdminGeodata().useAdminCommand("admin_geocell", player);
		}
		
		verify(player).sendMessage("GeoCell: 10, 20. XYZ (1016, 2016, 3050)");
	}
}
