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
package com.l2journey.gameserver.scripting;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;

import org.w3c.dom.Document;

import com.l2journey.Config;
import com.l2journey.commons.util.IXmlReader;
import com.l2journey.commons.util.TraceUtil;
import com.l2journey.gameserver.scripting.java.JavaExecutionContext;

/**
 * @author Mobius
 */
public class ScriptEngineManager implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(ScriptEngineManager.class.getName());
	
	public static final Path SCRIPT_FOLDER = Config.SCRIPT_ROOT.toPath();
	public static final Path MASTER_HANDLER_FILE = Paths.get(SCRIPT_FOLDER.toString(), "handlers", "MasterHandler.java");
	public static final Path EFFECT_MASTER_HANDLER_FILE = Paths.get(SCRIPT_FOLDER.toString(), "handlers", "EffectMasterHandler.java");
	
	private static final JavaExecutionContext JAVA_EXECUTION_CONTEXT = new JavaExecutionContext();
	private static final Set<String> EXCLUSIONS = new HashSet<>();
	
	protected ScriptEngineManager()
	{
		// Load Scripts.xml
		load();
	}
	
	@Override
	public void load()
	{
		EXCLUSIONS.clear();
		parseDatapackFile("config/admin/scripts.xml");
		LOGGER.info("Loaded " + EXCLUSIONS.size() + " files to exclude.");
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		try
		{
			final Map<String, Set<String>> excludePaths = new HashMap<>();
			forEach(document, "list", listNode -> forEach(listNode, "exclude", excludeNode ->
			{
				final String excludeFile = parseString(excludeNode.getAttributes(), "file");
				excludePaths.putIfAbsent(excludeFile, new HashSet<>());
				
				forEach(excludeNode, "include", includeNode -> excludePaths.get(excludeFile).add(parseString(includeNode.getAttributes(), "file")));
			}));
			
			final int nameCount = SCRIPT_FOLDER.getNameCount();
			Files.walkFileTree(SCRIPT_FOLDER, new SimpleFileVisitor<Path>()
			{
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
				{
					final String fileName = file.getFileName().toString();
					if (!fileName.endsWith(".java"))
					{
						return FileVisitResult.CONTINUE;
					}
					
					final List<String> relativePathParts = new ArrayList<>();
					file.subpath(nameCount, file.getNameCount()).forEach(p -> relativePathParts.add(p.toString()));
					for (int i = 0; i < relativePathParts.size(); i++)
					{
						final String currentPart = relativePathParts.get(i);
						if (excludePaths.containsKey(currentPart))
						{
							boolean excludeScript = true;
							
							final Set<String> includePath = excludePaths.get(currentPart);
							for (int j = i + 1; j < relativePathParts.size(); j++)
							{
								if (includePath.contains(relativePathParts.get(j)))
								{
									excludeScript = false;
									break;
								}
							}
							
							if (excludeScript)
							{
								EXCLUSIONS.add(file.toUri().getPath());
								break;
							}
						}
					}
					
					return FileVisitResult.CONTINUE;
				}
			});
		}
		catch (IOException e)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Could not initialize. " + e.getMessage());
		}
	}
	
	private void processDirectory(File dir, List<Path> files)
	{
		for (File file : dir.listFiles())
		{
			if (file.isFile())
			{
				final String filePath = file.toURI().getPath();
				if (filePath.endsWith(".java") && !EXCLUSIONS.contains(filePath))
				{
					files.add(file.toPath().toAbsolutePath());
				}
			}
			else if (file.isDirectory())
			{
				processDirectory(file, files);
			}
		}
	}
	
	public void executeScript(Path sourceFiles) throws Exception
	{
		Path path = sourceFiles;
		if (!path.isAbsolute())
		{
			path = SCRIPT_FOLDER.resolve(path);
		}
		
		path = path.toAbsolutePath();
		
		// Check if the path exists
		if (!Files.exists(path))
		{
			throw new Exception("Script file does not exist: " + path.toString());
		}
		
		// Execute the script and check for errors.
		final Entry<Path, Throwable> error = JAVA_EXECUTION_CONTEXT.executeScript(path);
		if (error != null)
		{
			final Throwable cause = error.getValue();
			if (cause != null)
			{
				LOGGER.warning(TraceUtil.getStackTrace(cause));
			}
			throw new Exception("ScriptEngine: " + error.getKey() + " failed execution!", cause);
		}
	}
	
	public void executeScriptList() throws Exception
	{
		if (Config.NO_QUESTS)
		{
			return;
		}
		
		final List<Path> files = new ArrayList<>();
		processDirectory(SCRIPT_FOLDER.toFile(), files);
		
		final Map<Path, Throwable> invokationErrors = JAVA_EXECUTION_CONTEXT.executeScripts(files);
		for (Entry<Path, Throwable> entry : invokationErrors.entrySet())
		{
			LOGGER.warning(getClass().getSimpleName() + ": " + entry.getKey() + " failed execution! " + entry.getValue().getMessage());
		}
	}
	
	public Path getCurrentLoadingScript()
	{
		return JAVA_EXECUTION_CONTEXT.getCurrentExecutingScript();
	}
	
	public static ScriptEngineManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final ScriptEngineManager INSTANCE = new ScriptEngineManager();
	}
}