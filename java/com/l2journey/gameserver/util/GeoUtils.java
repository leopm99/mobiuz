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
package com.l2journey.gameserver.util;

import java.awt.Color;

import com.l2journey.commons.terrain.geoengine.Direction;
import com.l2journey.gameserver.GeoData;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.network.serverpackets.ExServerPrimitive;

/**
 * @author HorridoJoho
 */
public final class GeoUtils
{
	private GeoUtils()
	{
	}

	public static void debug2DLine(Player player, int x, int y, int tx, int ty, int z)
	{
		int gx = GeoData.getInstance().getGeoX(x);
		int gy = GeoData.getInstance().getGeoY(y);
		
		int tgx = GeoData.getInstance().getGeoX(tx);
		int tgy = GeoData.getInstance().getGeoY(ty);
		
		ExServerPrimitive prim = new ExServerPrimitive("Debug2DLine", x, y, z);
		prim.addLine(Color.BLUE, GeoData.getInstance().getWorldX(gx), GeoData.getInstance().getWorldY(gy), z, GeoData.getInstance().getWorldX(tgx), GeoData.getInstance().getWorldY(tgy), z);
		
		LinePointIterator iter = new LinePointIterator(gx, gy, tgx, tgy);
		
		while (iter.next())
		{
			int wx = GeoData.getInstance().getWorldX(iter.x());
			int wy = GeoData.getInstance().getWorldY(iter.y());
			
			prim.addPoint(Color.RED, wx, wy, z);
		}
		player.sendPacket(prim);
	}
	
	public static void debug3DLine(Player player, int x, int y, int z, int tx, int ty, int tz)
	{
		int gx = GeoData.getInstance().getGeoX(x);
		int gy = GeoData.getInstance().getGeoY(y);
		
		int tgx = GeoData.getInstance().getGeoX(tx);
		int tgy = GeoData.getInstance().getGeoY(ty);
		
		ExServerPrimitive prim = new ExServerPrimitive("Debug3DLine", x, y, z);
		prim.addLine(Color.BLUE, GeoData.getInstance().getWorldX(gx), GeoData.getInstance().getWorldY(gy), z, GeoData.getInstance().getWorldX(tgx), GeoData.getInstance().getWorldY(tgy), tz);
		
		LinePointIterator3D iter = new LinePointIterator3D(gx, gy, z, tgx, tgy, tz);
		iter.next();
		int prevX = iter.x();
		int prevY = iter.y();
		int wx = GeoData.getInstance().getWorldX(prevX);
		int wy = GeoData.getInstance().getWorldY(prevY);
		int wz = iter.z();
		prim.addPoint(Color.RED, wx, wy, wz);
		
		while (iter.next())
		{
			int curX = iter.x();
			int curY = iter.y();
			
			if ((curX != prevX) || (curY != prevY))
			{
				wx = GeoData.getInstance().getWorldX(curX);
				wy = GeoData.getInstance().getWorldY(curY);
				wz = iter.z();
				
				prim.addPoint(Color.RED, wx, wy, wz);
				
				prevX = curX;
				prevY = curY;
			}
		}
		player.sendPacket(prim);
	}
	
	private static Color getDirectionColor(int x, int y, int z, Direction dir)
	{
		if (GeoData.getInstance().canEnterNeighbors(x, y, z, dir))
		{
			return Color.GREEN;
		}
		return Color.RED;
	}
	
	public static void debugGrid(Player player)
	{
		int geoRadius = 10;
		int blocksPerPacket = 49;
		if (geoRadius < 0)
		{
			throw new IllegalArgumentException("geoRadius < 0");
		}
		
		int iBlock = blocksPerPacket;
		int iPacket = 0;
		
		ExServerPrimitive exsp = null;
		GeoData gd = GeoData.getInstance();
		int playerGx = gd.getGeoX(player.getX());
		int playerGy = gd.getGeoY(player.getY());
		for (int dx = -geoRadius; dx <= geoRadius; ++dx)
		{
			for (int dy = -geoRadius; dy <= geoRadius; ++dy)
			{
				if (iBlock >= blocksPerPacket)
				{
					iBlock = 0;
					if (exsp != null)
					{
						++iPacket;
						player.sendPacket(exsp);
					}
					exsp = new ExServerPrimitive("DebugGrid_" + iPacket, player.getX(), player.getY(), -16000);
				}
				
				if (exsp == null)
				{
					throw new IllegalStateException();
				}
				
				int gx = playerGx + dx;
				int gy = playerGy + dy;
				
				int x = gd.getWorldX(gx);
				int y = gd.getWorldY(gy);
				int z = gd.getNearestZ(gx, gy, player.getZ());
				
				// north arrow
				Color col = getDirectionColor(gx, gy, z, Direction.NORTH);
				exsp.addLine(col, x - 1, y - 7, z, x + 1, y - 7, z);
				exsp.addLine(col, x - 2, y - 6, z, x + 2, y - 6, z);
				exsp.addLine(col, x - 3, y - 5, z, x + 3, y - 5, z);
				exsp.addLine(col, x - 4, y - 4, z, x + 4, y - 4, z);
				
				// east arrow
				col = getDirectionColor(gx, gy, z, Direction.EAST);
				exsp.addLine(col, x + 7, y - 1, z, x + 7, y + 1, z);
				exsp.addLine(col, x + 6, y - 2, z, x + 6, y + 2, z);
				exsp.addLine(col, x + 5, y - 3, z, x + 5, y + 3, z);
				exsp.addLine(col, x + 4, y - 4, z, x + 4, y + 4, z);
				
				// south arrow
				col = getDirectionColor(gx, gy, z, Direction.SOUTH);
				exsp.addLine(col, x - 1, y + 7, z, x + 1, y + 7, z);
				exsp.addLine(col, x - 2, y + 6, z, x + 2, y + 6, z);
				exsp.addLine(col, x - 3, y + 5, z, x + 3, y + 5, z);
				exsp.addLine(col, x - 4, y + 4, z, x + 4, y + 4, z);
				
				col = getDirectionColor(gx, gy, z, Direction.WEST);
				exsp.addLine(col, x - 7, y - 1, z, x - 7, y + 1, z);
				exsp.addLine(col, x - 6, y - 2, z, x - 6, y + 2, z);
				exsp.addLine(col, x - 5, y - 3, z, x - 5, y + 3, z);
				exsp.addLine(col, x - 4, y - 4, z, x - 4, y + 4, z);
				
				++iBlock;
			}
		}
		
		player.sendPacket(exsp);
	}
	
	/**
	 * difference between x values: never above 1<br>
	 * difference between y values: never above 1
	 * @param lastX
	 * @param lastY
	 * @param x
	 * @param y
	 * @return
	 */
	public static Direction computeDirection(int lastX, int lastY, int x, int y)
	{
		if (x > lastX) // east
		{
			if (y > lastY)
			{
				return Direction.SOUTH_EAST;
			}
			else if (y < lastY)
			{
				return Direction.NORTH_EAST;
			}
			else
			{
				return Direction.EAST;
			}
		}
		else if (x < lastX) // west
		{
			if (y > lastY)
			{
				return Direction.SOUTH_WEST;
			}
			else if (y < lastY)
			{
				return Direction.NORTH_WEST;
			}
			else
			{
				return Direction.WEST;
			}
		}
		else
		// unchanged x
		{
			if (y > lastY)
			{
				return Direction.SOUTH;
			}
			else if (y < lastY)
			{
				return Direction.NORTH;
			}
			else
			{
				throw new RuntimeException();
			}
		}
	}
}