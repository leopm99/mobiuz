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
package com.l2journey.commons.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.TimeUnit;

/**
 * Represents a network connection associated with a client.<br>
 * This class encapsulates operations such as reading and writing data to and from the network channel, managing buffers and handling network events.
 * @param <T> The type of the client associated with this connection.
 * @author JoeAlisson
 */
public class Connection<T extends Client<Connection<T>>>
{
	private final AsynchronousSocketChannel _channel;
	private final ReadHandler<T> _readHandler;
	private final WriteHandler<T> _writeHandler;
	private final ConnectionConfig _config;
	private T _client;
	
	private ByteBuffer _readingBuffer;
	private ByteBuffer[] _writingBuffers;
	
	/**
	 * Constructs a Connection with the specified channel, handlers, and configuration.
	 * @param channel The AsynchronousSocketChannel for communication.
	 * @param readHandler The handler for read operations.
	 * @param writeHandler The handler for write operations.
	 * @param config The configuration for the connection.
	 */
	public Connection(AsynchronousSocketChannel channel, ReadHandler<T> readHandler, WriteHandler<T> writeHandler, ConnectionConfig config)
	{
		_channel = channel;
		_readHandler = readHandler;
		_writeHandler = writeHandler;
		_config = config;
	}
	
	/**
	 * Sets the client associated with this connection.
	 * @param client The client to associate with this connection.
	 */
	public void setClient(T client)
	{
		_client = client;
	}
	
	/**
	 * Initiates a read operation on the connection.
	 */
	public void read()
	{
		if (_channel.isOpen())
		{
			_channel.read(_readingBuffer, _client, _readHandler);
		}
	}
	
	/**
	 * Initiates a header read operation by releasing any existing buffer and obtaining a new header buffer from the resource pool.
	 */
	public void readHeader()
	{
		if (_channel.isOpen())
		{
			releaseReadingBuffer();
			_readingBuffer = _config.resourcePool.getHeaderBuffer();
			read();
		}
	}
	
	/**
	 * Reads a specific size of data by obtaining a buffer of the specified size from the resource pool and initiating a read operation.
	 * @param size The size of data to read.
	 */
	public void read(int size)
	{
		if (_channel.isOpen())
		{
			_readingBuffer = _config.resourcePool.recycleAndGetNew(_readingBuffer, size);
			read();
		}
	}
	
	/**
	 * Initiates a write operation with the specified buffers.
	 * @param buffers The ByteBuffers to write.
	 * @return {@code true} if the write operation was initiated, {@code false} otherwise.
	 */
	public boolean write(ByteBuffer[] buffers)
	{
		if (!_channel.isOpen())
		{
			return false;
		}
		
		_writingBuffers = buffers;
		write();
		return true;
	}
	
	/**
	 * Continues a write operation using the existing write buffers.<br>
	 * If the write completes, notifies the client to finish the writing process.
	 */
	public void write()
	{
		if (_channel.isOpen() && (_writingBuffers != null))
		{
			_channel.write(_writingBuffers, 0, _writingBuffers.length, -1, TimeUnit.MILLISECONDS, _client, _writeHandler);
		}
		else if (_client != null)
		{
			_client.finishWriting();
		}
	}
	
	/**
	 * Retrieves the current reading buffer.
	 * @return The reading buffer.
	 */
	public ByteBuffer getReadingBuffer()
	{
		return _readingBuffer;
	}
	
	/**
	 * Releases the reading buffer back to the resource pool.
	 */
	private void releaseReadingBuffer()
	{
		if (_readingBuffer != null)
		{
			_config.resourcePool.recycleBuffer(_readingBuffer);
			_readingBuffer = null;
		}
	}
	
	/**
	 * Releases the writing buffers back to the resource pool.
	 * @return {@code true} if any buffers were released, {@code false} otherwise.
	 */
	public boolean releaseWritingBuffer()
	{
		boolean released = false;
		if (_writingBuffers != null)
		{
			for (ByteBuffer buffer : _writingBuffers)
			{
				_config.resourcePool.recycleBuffer(buffer);
				released = true;
			}
			_writingBuffers = null;
		}
		return released;
	}
	
	/**
	 * Closes the connection, releasing both reading and writing buffers and closing the channel if it is open.
	 */
	public void close()
	{
		releaseReadingBuffer();
		releaseWritingBuffer();
		try
		{
			if (_channel.isOpen())
			{
				_channel.close();
			}
		}
		catch (IOException _)
		{
			// Placeholder for handling/logging IOException if needed.
		}
		finally
		{
			_client = null;
		}
	}
	
	/**
	 * Retrieves the remote IP address of the client connected to this connection.
	 * @return The IP address of the remote client as a string, or an empty string if unavailable.
	 */
	public String getRemoteAddress()
	{
		try
		{
			final InetSocketAddress address = (InetSocketAddress) _channel.getRemoteAddress();
			return address.getAddress().getHostAddress();
		}
		catch (IOException _)
		{
			return "";
		}
	}
	
	/**
	 * Checks if the connection is open.
	 * @return {@code true} if the connection is open, {@code false} otherwise.
	 */
	public boolean isOpen()
	{
		return _channel.isOpen();
	}
	
	/**
	 * Retrieves the resource pool associated with this connection.
	 * @return The {@link ResourcePool} used by this connection.
	 */
	public ResourcePool getResourcePool()
	{
		return _config.resourcePool;
	}
	
	/**
	 * Determines whether packet dropping is enabled for this connection.
	 * @return {@code true} if packet dropping is enabled, {@code false} otherwise.
	 */
	public boolean dropPackets()
	{
		return _config.dropPackets;
	}
	
	/**
	 * Retrieves the packet drop threshold for this connection.
	 * @return The packet drop threshold.
	 */
	public int dropPacketThreshold()
	{
		return _config.dropPacketThreshold;
	}
}
