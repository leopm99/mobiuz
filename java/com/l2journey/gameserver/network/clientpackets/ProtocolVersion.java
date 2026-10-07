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
package com.l2journey.gameserver.network.clientpackets;

import java.util.logging.Logger;

import com.l2journey.Config;
import com.l2journey.gameserver.network.GameClient;
import com.l2journey.gameserver.network.serverpackets.KeyPacket;

/**
 * @version $Revision: 1.5.2.8.2.8 $ $Date: 2005/04/02 10:43:04 $
 */
public class ProtocolVersion extends ClientPacket
{
	private static final Logger LOGGER_ACCOUNTING = Logger.getLogger("accounting");
	
	private int _version;
	
	@Override
	protected void readImpl()
	{
		try
		{
			_version = readInt();
		}
		catch (Exception _)
		{
			_version = 0;
		}
	}
	
	@Override
	protected void runImpl()
	{
		// This packet is never encrypted.
		final GameClient client = getClient();
		if (_version == -2)
		{
			// This is just a ping attempt from the new C2 client.
			client.disconnect();
		}
		else if (!Config.PROTOCOL_LIST.contains(_version))
		{
			LOGGER_ACCOUNTING.warning(() -> "Wrong protocol version " + _version + ", " + client);
			client.setProtocolOk(false);
			client.close(new KeyPacket(client.enableCrypt(), 0));
		}
		else
		{
			client.setProtocolVersion(_version);
			client.setProtocolOk(true);
			client.sendPacket(new KeyPacket(client.enableCrypt(), 1));
		}
	}
}
