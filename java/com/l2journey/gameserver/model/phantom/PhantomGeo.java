package com.l2journey.gameserver.model.phantom;

import com.l2journey.gameserver.GeoData;
import com.l2journey.gameserver.model.Location;
import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.commons.util.Rnd;

public class PhantomGeo
{
	private static final int DEFAULT_GEO_INDEX = 0;
	
	public static boolean canMove(Creature cha, int x, int y, int z)
	{
		return GeoData.getInstance().canMove(cha.getX(), cha.getY(), cha.getZ(), x, y, z, DEFAULT_GEO_INDEX);
	}
	
	public static boolean canSeeTarget(Creature cha, Creature target)
	{
		return GeoData.getInstance().canSeeTarget(cha, target);
	}
	
	public static Location getValidLocation(Creature cha, int x, int y, int z)
	{
		int gx = GeoData.getInstance().getGeoX(x);
		int gy = GeoData.getInstance().getGeoY(y);
		int targetZ = GeoData.getInstance().getNearestZ(gx, gy, z);
		
		// Filtro anti-vazio: se o nearestZ for um absurdo, mantemos o Z original do bot
		if (Math.abs(targetZ - z) > 1500)
		{
			targetZ = z; 
		}
		
		return new Location(x, y, targetZ);
	}
	
	public static int getHeight(int x, int y, int z)
	{
		int gx = GeoData.getInstance().getGeoX(x);
		int gy = GeoData.getInstance().getGeoY(y);
		return GeoData.getInstance().getNearestZ(gx, gy, z);
	}
	
	public static Location getNpcLikeSpawn(Location loc)
	{
		if (loc == null) return null;
		
		int gx = GeoData.getInstance().getGeoX(loc.getX());
		int gy = GeoData.getInstance().getGeoY(loc.getY());
		int z = GeoData.getInstance().getNearestZ(gx, gy, loc.getZ());
		
		return new Location(loc.getX(), loc.getY(), z);
	}

	// MELHORIAS HUMANAS (ANTI-STUCK E MOVIMENTO FLUIDO)
	public static Location getRandomReachableLocation(Creature bot, int maxRadius)
	{
		// Os humanos nao dao "passinhos" minusculos ao acaso, forcamos uma distancia minima razoavel
		int minRadius = maxRadius > 100 ? 100 : maxRadius / 2;
		
		// Aumentamos para 8 tentativas para garantir que encontre caminho
		for (int i = 0; i < 8; i++) 
		{
			// Usamos Coordenadas Polares (Circulos) em vez de uma Grade
			double angle = Rnd.nextDouble() * 2 * Math.PI; // 360 graus de liberdade humana
			int distance = Rnd.get(minRadius, maxRadius);
			
			int newX = (int) (bot.getX() + distance * Math.cos(angle));
			int newY = (int) (bot.getY() + distance * Math.sin(angle));
			
			int newZ = getHeight(newX, newY, bot.getZ());
			
			// Se a diferenca de altura for uma parede gigante ou um abismo, aborta esse ponto
			if (Math.abs(newZ - bot.getZ()) > 500)
			{
				continue;
			}

			if (canMove(bot, newX, newY, newZ))
			{
				return new Location(newX, newY, newZ);
			}
		}
		
		for (int i = 0; i < 4; i++)
		{
			int shortX = bot.getX() + Rnd.get(-50, 50);
			int shortY = bot.getY() + Rnd.get(-50, 50);
			int shortZ = getHeight(shortX, shortY, bot.getZ());
			
			if (canMove(bot, shortX, shortY, shortZ))
			{
				return new Location(shortX, shortY, shortZ);
			}
		}
		
		return bot.getLocation(); 
	}

	public static Location getSafeRetreatLocation(Creature bot, Creature danger, int distance)
	{
		// Vetor de direcao de fuga
		double dx = bot.getX() - danger.getX();
		double dy = bot.getY() - danger.getY();
		
		double length = Math.sqrt(dx * dx + dy * dy);
		if (length == 0) return getRandomReachableLocation(bot, distance);
		
		dx /= length;
		dy /= length;
		
		// 1. Tentativa Humana de Fugir em Linha Reta
		int targetX = (int) (bot.getX() + (dx * distance));
		int targetY = (int) (bot.getY() + (dy * distance));
		int targetZ = getHeight(targetX, targetY, bot.getZ());

		if (Math.abs(targetZ - bot.getZ()) <= 500 && canMove(bot, targetX, targetY, targetZ))
		{
			return new Location(targetX, targetY, targetZ);
		}

		int[] baseAngles = {30, -30, 60, -60, 80, -80}; 
		
		for (int baseAngle : baseAngles)
		{
			int humanAngle = baseAngle + Rnd.get(-10, 10);
			double radian = Math.toRadians(humanAngle);
			
			double rotX = dx * Math.cos(radian) - dy * Math.sin(radian);
			double rotY = dx * Math.sin(radian) + dy * Math.cos(radian);
			
			int altX = (int) (bot.getX() + (rotX * distance));
			int altY = (int) (bot.getY() + (rotY * distance));
			int altZ = getHeight(altX, altY, bot.getZ());
			
			if (Math.abs(altZ - bot.getZ()) <= 500 && canMove(bot, altX, altY, altZ))
			{
				return new Location(altX, altY, altZ);
			}
		}

		if (distance > 100)
		{
			return getSafeRetreatLocation(bot, danger, distance / 2);
		}

		// Encurralado total
		return bot.getLocation();
	}
}
