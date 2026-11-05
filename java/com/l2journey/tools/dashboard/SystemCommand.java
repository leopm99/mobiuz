package com.l2journey.tools.dashboard;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Resolves Windows system tools to absolute paths so Dashboard never searches PATH.
 */
final class SystemCommand
{
	private SystemCommand()
	{
	}
	
	static Optional<Path> windows(String... segments)
	{
		return fromRoots(System.getenv("SystemRoot"), System.getenv("WINDIR"), segments);
	}
	
	static Optional<Path> fromRoots(String systemRoot, String windir, String... segments)
	{
		String root = systemRoot;
		if ((root == null) || root.isBlank())
		{
			root = windir;
		}
		return resolve(root, segments);
	}
	
	static Optional<Path> resolve(String windowsRoot, String... segments)
	{
		if ((windowsRoot == null) || windowsRoot.isBlank())
		{
			return Optional.empty();
		}
		final Path path = Paths.get(windowsRoot, segments);
		if (!path.isAbsolute() || !Files.isRegularFile(path))
		{
			return Optional.empty();
		}
		return Optional.of(path);
	}
	
	static Optional<Path> resolveJcmd()
	{
		return resolveJcmd(System.getProperty("java.home"), System.getenv("JAVA_HOME"));
	}
	
	static Optional<Path> resolveJcmd(String javaHome, String javaHomeEnv)
	{
		for (String home : new String[]
		{
			javaHome,
			javaHomeEnv
		})
		{
			if ((home != null) && !home.isBlank())
			{
				final Path candidate = Paths.get(home, "bin", "jcmd.exe");
				if (candidate.isAbsolute() && Files.isRegularFile(candidate))
				{
					return Optional.of(candidate);
				}
			}
		}
		return Optional.empty();
	}
	
	static Process start(Path directory, boolean redirectErrorStream, Optional<Path> executable, String... args) throws IOException
	{
		if (executable.isEmpty() || !executable.get().isAbsolute() || !Files.isRegularFile(executable.get()))
		{
			throw new IOException("Executable not found: " + executable.map(Path::toString).orElse("none"));
		}
		final List<String> command = new ArrayList<>(1 + args.length);
		command.add(executable.get().toString());
		for (String arg : args)
		{
			command.add(arg);
		}
		final ProcessBuilder builder = new ProcessBuilder(command);
		if (directory != null)
		{
			builder.directory(directory.toFile());
		}
		if (redirectErrorStream)
		{
			builder.redirectErrorStream(true);
		}
		return builder.start();
	}
}
