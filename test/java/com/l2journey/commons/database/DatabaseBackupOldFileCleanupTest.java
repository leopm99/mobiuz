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
package com.l2journey.commons.database;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.l2journey.Config;

/**
 * Unit tests for old backup cleanup in {@link DatabaseBackup#performBackup()}.
 */
class DatabaseBackupOldFileCleanupTest
{
	@TempDir
	File tempDir;
	
	private String previousBackupPath;
	private int previousBackupDays;
	private String previousMysqlBinPath;
	
	@BeforeEach
	void saveConfig()
	{
		previousBackupPath = Config.BACKUP_PATH;
		previousBackupDays = Config.BACKUP_DAYS;
		previousMysqlBinPath = Config.MYSQL_BIN_PATH;
	}
	
	@AfterEach
	void restoreConfig()
	{
		Config.BACKUP_PATH = previousBackupPath;
		Config.BACKUP_DAYS = previousBackupDays;
		Config.MYSQL_BIN_PATH = previousMysqlBinPath;
	}
	
	@Test
	void performBackupDeletesFilesOlderThanBackupDays() throws Exception
	{
		final File backupDir = new File(tempDir, "backup");
		assertTrue(backupDir.mkdirs());
		
		final File oldFile = new File(backupDir, "old.sql");
		final File recentFile = new File(backupDir, "recent.sql");
		Files.writeString(oldFile.toPath(), "old");
		Files.writeString(recentFile.toPath(), "recent");
		Files.setLastModifiedTime(oldFile.toPath(), FileTime.from(Instant.now().minus(40, ChronoUnit.DAYS)));
		Files.setLastModifiedTime(recentFile.toPath(), FileTime.from(Instant.now()));
		
		Config.BACKUP_PATH = backupDir.getAbsolutePath() + File.separator;
		Config.BACKUP_DAYS = 30;
		Config.MYSQL_BIN_PATH = "nonexistent-mysqldump-path-";
		
		DatabaseBackup.performBackup();
		
		assertFalse(oldFile.exists());
		assertTrue(recentFile.exists());
	}
}
