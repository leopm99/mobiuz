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
package com.l2journey.commons.database;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import com.l2journey.Config;
import com.l2journey.commons.enums.ServerMode;

/**
 * @author Mobius
 */
public class DatabaseBackup
{
	private DatabaseBackup()
	{
	}

	public static void performBackup()
	{
		// Delete old files.
		if (Config.BACKUP_DAYS > 0)
		{
			final long cut = LocalDateTime.now(ZoneId.systemDefault()).minusDays(Config.BACKUP_DAYS).toEpochSecond(ZoneOffset.UTC);
			final Path path = Paths.get(Config.BACKUP_PATH);
			try
			{
				Files.list(path).filter(n ->
				{
					try
					{
						return Files.getLastModifiedTime(n).to(TimeUnit.SECONDS) < cut;
					}
					catch (Exception _)
					{
						return false;
					}
				}).forEach(n ->
				{
					try
					{
						Files.delete(n);
					}
					catch (Exception _)
					{
						// Ignore.
					}
				});
			}
			catch (Exception _)
			{
				// Ignore.
			}
		}
		
		// Dump to file.
		final String mysqldumpPath = System.getProperty("os.name").toLowerCase().contains("win") ? Config.MYSQL_BIN_PATH : "";
		try
		{
			// Java 17
			// Java 18
			final String backupFileName = Config.BACKUP_PATH + (Config.SERVER_MODE == ServerMode.GAME ? "game" : "login") + new SimpleDateFormat("_yyyy_MM_dd_HH_mm'.sql'").format(new Date());
			final String databaseName = Config.DATABASE_URL.replace("jdbc:mysql://", "").replaceAll(".*\\/|\\?.*", "");
			final String[] command =
			{
				mysqldumpPath + "mysqldump",
				"-u",
				Config.DATABASE_LOGIN,
				Config.DATABASE_PASSWORD.trim().isEmpty() ? "" : "-p" + Config.DATABASE_PASSWORD,
				databaseName,
				"-r",
				backupFileName
			};
			
			final Process process = Runtime.getRuntime().exec(command);
			process.waitFor();
		}
		catch (Exception _)
		{
			// Ignore.
		}
	}
}
