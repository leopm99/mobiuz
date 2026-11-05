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
package com.l2journey.tools.dashboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for {@link SystemCommand}.
 */
class SystemCommandTest
{
	@TempDir
	Path _temp;
	
	@Test
	void resolveRejectsBlankRoot()
	{
		assertTrue(SystemCommand.resolve(null, "System32", "cmd.exe").isEmpty());
		assertTrue(SystemCommand.resolve("", "System32", "cmd.exe").isEmpty());
		assertTrue(SystemCommand.resolve("   ", "System32", "cmd.exe").isEmpty());
	}
	
	@Test
	void resolveRejectsRelativeRoot()
	{
		assertTrue(SystemCommand.resolve("Windows", "System32", "cmd.exe").isEmpty());
	}
	
	@Test
	void resolveRejectsMissingFile() throws IOException
	{
		final Path root = Files.createDirectories(_temp.resolve("Windows"));
		assertTrue(SystemCommand.resolve(root.toString(), "System32", "cmd.exe").isEmpty());
	}
	
	@Test
	void resolveAcceptsAbsoluteExistingFile() throws IOException
	{
		final Path root = Files.createDirectories(_temp.resolve("Windows"));
		final Path cmd = Files.createDirectories(root.resolve("System32")).resolve("cmd.exe");
		Files.writeString(cmd, "stub");
		final Optional<Path> resolved = SystemCommand.resolve(root.toString(), "System32", "cmd.exe");
		assertTrue(resolved.isPresent());
		assertTrue(resolved.get().isAbsolute());
		assertEquals(cmd.toAbsolutePath().normalize(), resolved.get().toAbsolutePath().normalize());
	}
	
	@Test
	void windowsIsEmptyOrAbsoluteExistingFile()
	{
		final Optional<Path> cmd = SystemCommand.windows("System32", "cmd.exe");
		assertTrue(cmd.isEmpty() || (cmd.get().isAbsolute() && Files.isRegularFile(cmd.get())));
	}
	
	@Test
	void windowsFallsBackToWindir() throws IOException
	{
		final Path root = Files.createDirectories(_temp.resolve("WinDirRoot"));
		final Path cmd = Files.createDirectories(root.resolve("System32")).resolve("cmd.exe");
		Files.writeString(cmd, "stub");
		final Optional<Path> resolved = SystemCommand.fromRoots(null, root.toString(), "System32", "cmd.exe");
		assertTrue(resolved.isPresent());
		assertEquals(cmd.toAbsolutePath().normalize(), resolved.get().toAbsolutePath().normalize());
	}
	
	@Test
	void resolveJcmdUsesRuntimeHomes()
	{
		final Optional<Path> jcmd = SystemCommand.resolveJcmd();
		assertTrue(jcmd.isEmpty() || (jcmd.get().isAbsolute() && Files.isRegularFile(jcmd.get())));
	}
	
	@Test
	void startRejectsRelativeExecutable()
	{
		assertThrows(IOException.class, () -> SystemCommand.start(null, true, Optional.of(Path.of("cmd.exe")), "/c", "echo"));
	}
	
	@Test
	void startRejectsEmptyExecutable()
	{
		assertThrows(IOException.class, () -> SystemCommand.start(null, true, Optional.empty()));
	}
	
	@Test
	void startRunsAbsoluteJava() throws Exception
	{
		final Path java = javaExecutable();
		assertTrue(Files.isRegularFile(java), "java executable missing under java.home");
		Process process = null;
		try
		{
			process = SystemCommand.start(_temp, true, Optional.of(java), "-version");
			final String command = process.info().command().orElse("");
			assertTrue(command.toLowerCase(Locale.ROOT).contains("java"), command);
			assertTrue(process.waitFor(10, TimeUnit.SECONDS));
			assertEquals(0, process.exitValue());
		}
		finally
		{
			if (process != null)
			{
				process.destroyForcibly();
			}
		}
	}
	
	@Test
	void resolveJcmdFindsTempBinary() throws IOException
	{
		final Path home = Files.createDirectories(_temp.resolve("jdk"));
		final Path jcmd = Files.createDirectories(home.resolve("bin")).resolve("jcmd.exe");
		Files.writeString(jcmd, "stub");
		final Optional<Path> resolved = SystemCommand.resolveJcmd(home.toString(), null);
		assertTrue(resolved.isPresent());
		assertEquals(jcmd.toAbsolutePath().normalize(), resolved.get().toAbsolutePath().normalize());
	}
	
	@Test
	void resolveJcmdEmptyWhenHomesMissing()
	{
		assertTrue(SystemCommand.resolveJcmd(null, null).isEmpty());
		assertTrue(SystemCommand.resolveJcmd("", "   ").isEmpty());
	}
	
	private static Path javaExecutable()
	{
		final String name = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win") ? "java.exe" : "java";
		return Path.of(System.getProperty("java.home"), "bin", name).toAbsolutePath().normalize();
	}
}
