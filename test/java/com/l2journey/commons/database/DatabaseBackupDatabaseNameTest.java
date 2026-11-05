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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link DatabaseBackup#databaseName(String)} (java:S8786).
 */
class DatabaseBackupDatabaseNameTest
{
	@Test
	void hostWithQueryYieldsDatabaseName()
	{
		assertEquals("l2journey", DatabaseBackup.databaseName("jdbc:mysql://localhost/l2journey?useSSL=false"));
	}
	
	@Test
	void hostPortWithQueryYieldsDatabaseName()
	{
		assertEquals("l2journey", DatabaseBackup.databaseName("jdbc:mysql://db:3306/l2journey?useUnicode=true"));
	}
	
	@Test
	void hostPortWithoutQueryYieldsDatabaseName()
	{
		assertEquals("l2journey", DatabaseBackup.databaseName("jdbc:mysql://db:3306/l2journey"));
	}
	
	@Test
	void noPathSlashBeyondSchemeYieldsHostAsName()
	{
		assertEquals("l2journey", DatabaseBackup.databaseName("jdbc:mysql://l2journey"));
	}
	
	@Test
	void noSlashReturnsEntireInput()
	{
		assertEquals("l2journey", DatabaseBackup.databaseName("l2journey"));
	}
	
	@Test
	void slashInsideQueryDoesNotChangeDatabaseName()
	{
		assertEquals("l2journey", DatabaseBackup.databaseName("jdbc:mysql://db:3306/l2journey?path=/tmp"));
	}
	
	@Test
	void trailingSlashYieldsEmptyName()
	{
		assertEquals("", DatabaseBackup.databaseName("jdbc:mysql://db:3306/"));
	}
}
