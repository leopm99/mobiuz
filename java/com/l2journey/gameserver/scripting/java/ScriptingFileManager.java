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
package com.l2journey.gameserver.scripting.java;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Set;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.StandardJavaFileManager;

/**
 * @author HorridoJoho
 */
public class ScriptingFileManager extends ForwardingJavaFileManager<StandardJavaFileManager> implements StandardJavaFileManager
{
	private final LinkedList<ScriptingOutputFileObject> _classOutputs = new LinkedList<>();
	
	public ScriptingFileManager(StandardJavaFileManager wrapped)
	{
		super(wrapped);
	}
	
	Iterable<ScriptingOutputFileObject> getCompiledClasses()
	{
		return Collections.unmodifiableCollection(_classOutputs);
	}
	
	@Override
	public JavaFileObject getJavaFileForOutput(Location location, String className, Kind kind, FileObject sibling) throws IOException
	{
		if (kind != Kind.CLASS)
		{
			return fileManager.getJavaFileForOutput(location, className, kind, sibling);
		}
		
		String javaName = className;
		if (javaName.contains("/"))
		{
			javaName = javaName.replace('/', '.');
		}
		
		ScriptingOutputFileObject fileObject;
		if (sibling != null)
		{
			fileObject = new ScriptingOutputFileObject(Paths.get(sibling.getName()), javaName, javaName.substring(javaName.lastIndexOf('.') + 1));
		}
		else
		{
			fileObject = new ScriptingOutputFileObject(null, javaName, javaName.substring(javaName.lastIndexOf('.') + 1));
		}
		
		_classOutputs.add(fileObject);
		return fileObject;
	}
	
	@Override
	public Iterable<? extends JavaFileObject> getJavaFileObjectsFromFiles(Iterable<? extends File> files)
	{
		return fileManager.getJavaFileObjectsFromFiles(files);
	}
	
	@Override
	public Iterable<? extends JavaFileObject> getJavaFileObjects(File... files)
	{
		return fileManager.getJavaFileObjects(files);
	}
	
	@Override
	public Iterable<? extends JavaFileObject> getJavaFileObjectsFromStrings(Iterable<String> names)
	{
		return fileManager.getJavaFileObjectsFromStrings(names);
	}
	
	@Override
	public Iterable<? extends JavaFileObject> getJavaFileObjects(String... names)
	{
		return fileManager.getJavaFileObjects(names);
	}
	
	@Override
	public Iterable<? extends JavaFileObject> getJavaFileObjectsFromPaths(Iterable<? extends Path> paths)
	{
		return fileManager.getJavaFileObjectsFromPaths(paths);
	}
	
	@Override
	public Iterable<? extends JavaFileObject> getJavaFileObjects(Path... paths)
	{
		return fileManager.getJavaFileObjects(paths);
	}
	
	@Override
	public void setLocation(Location location, Iterable<? extends File> path) throws IOException
	{
		fileManager.setLocation(location, path);
	}
	
	@Override
	public void setLocationFromPaths(Location location, Collection<? extends Path> paths) throws IOException
	{
		fileManager.setLocationFromPaths(location, paths);
	}
	
	@Override
	public void setLocationForModule(Location location, String moduleName, Collection<? extends Path> paths) throws IOException
	{
		fileManager.setLocationForModule(location, moduleName, paths);
	}
	
	@Override
	public Iterable<? extends File> getLocation(Location location)
	{
		return fileManager.getLocation(location);
	}
	
	@Override
	public Iterable<? extends Path> getLocationAsPaths(Location location)
	{
		return fileManager.getLocationAsPaths(location);
	}
	
	@Override
	public Path asPath(FileObject file)
	{
		return fileManager.asPath(file);
	}
	
	@Override
	public void setPathFactory(PathFactory f)
	{
		fileManager.setPathFactory(f);
	}
	
	@Override
	public boolean isSameFile(FileObject a, FileObject b)
	{
		return fileManager.isSameFile(a, b);
	}
}
