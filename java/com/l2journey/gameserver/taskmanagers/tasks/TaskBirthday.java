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
package com.l2journey.gameserver.taskmanagers.tasks;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import com.l2journey.Config;
import com.l2journey.commons.database.DatabaseFactory;
import com.l2journey.gameserver.managers.MailManager;
import com.l2journey.gameserver.model.Message;
import com.l2journey.gameserver.model.item.enums.ItemProcessType;
import com.l2journey.gameserver.network.enums.MessageSenderType;
import com.l2journey.gameserver.taskmanagers.PersistentTaskManager;
import com.l2journey.gameserver.taskmanagers.PersistentTaskManager.ExecutedTask;

/**
 * Birthday Gift task.
 * @author Zoey76
 */
public class TaskBirthday extends PersistentTask
{
	private static final String NAME = "birthday";
	/** Get all players that have had a birthday since last check. */
	private static final String SELECT_PENDING_BIRTHDAY_GIFTS = "SELECT charId, char_name, createDate, (YEAR(NOW()) - YEAR(createDate)) AS age FROM characters WHERE (YEAR(NOW()) - YEAR(createDate) > 0) AND ((DATE_ADD(createDate, INTERVAL (YEAR(NOW()) - YEAR(createDate)) YEAR)) BETWEEN FROM_UNIXTIME(?) AND NOW())";
	
	@Override
	public String getName()
	{
		return NAME;
	}
	
	@Override
	public void onTimeElapsed(ExecutedTask task)
	{
		LOGGER.info(() -> "BirthdayManager: " + giveBirthdayGifts(task.getLastActivation()) + " gifts sent.");
	}
	
	private int giveBirthdayGifts(long lastActivation)
	{
		int birthdayGiftCount = 0;
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(SELECT_PENDING_BIRTHDAY_GIFTS))
		{
			ps.setLong(1, TimeUnit.SECONDS.convert(lastActivation, TimeUnit.MILLISECONDS));
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					final String text = Config.ALT_BIRTHDAY_MAIL_TEXT.replaceAll("$c1", rs.getString("char_name")).replaceAll("$s1", Integer.toString(rs.getInt("age")));
					final Message msg = new Message(rs.getInt("charId"), Config.ALT_BIRTHDAY_MAIL_SUBJECT, text, MessageSenderType.ALEGRIA);
					msg.createAttachments().addItem(ItemProcessType.REWARD, Config.ALT_BIRTHDAY_GIFT, 1, null, null);
					MailManager.getInstance().sendMessage(msg);
					birthdayGiftCount++;
				}
			}
		}
		catch (SQLException e)
		{
			LOGGER.warning("Error checking birthdays: " + e.getMessage());
		}
		return birthdayGiftCount;
	}
	
	@Override
	public void initializate()
	{
		super.initializate();
		PersistentTaskManager.addUniqueTask(NAME, PersistentTaskType.TYPE_GLOBAL_TASK, "1", "06:30:00", "");
	}
}
