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
package com.l2journey.gameserver.communitybbs.BB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.l2journey.commons.database.DatabaseFactory;
import com.l2journey.gameserver.communitybbs.Manager.PostBBSManager;

/**
 * @author Maktakien
 */
public class Post
{
	private static final Logger LOGGER = Logger.getLogger(Post.class.getName());
	
	public static class CPost
	{
		private int _postId;
		private String _postOwner;
		private int _postOwnerId;
		private long _postDate;
		private int _postTopicId;
		private int _postForumId;
		private String _postText;
		
		public void setPostId(int postId)
		{
			_postId = postId;
		}
		
		public int getPostId()
		{
			return _postId;
		}
		
		public void setPostOwner(String postOwner)
		{
			_postOwner = postOwner;
		}
		
		public String getPostOwner()
		{
			return _postOwner;
		}
		
		public void setPostOwnerId(int postOwnerId)
		{
			_postOwnerId = postOwnerId;
		}
		
		public int getPostOwnerId()
		{
			return _postOwnerId;
		}
		
		public void setPostDate(long postDate)
		{
			_postDate = postDate;
		}
		
		public long getPostDate()
		{
			return _postDate;
		}
		
		public void setPostTopicId(int postTopicId)
		{
			_postTopicId = postTopicId;
		}
		
		public int getPostTopicId()
		{
			return _postTopicId;
		}
		
		public void setPostForumId(int postForumId)
		{
			_postForumId = postForumId;
		}
		
		public int getPostForumId()
		{
			return _postForumId;
		}
		
		public void setPostText(String postText)
		{
			_postText = postText;
		}
		
		public String getPostText()
		{
			if (_postText == null)
			{
				return "";
			}
			
			// Bypass exploit check.
			final String text = _postText.toLowerCase();
			if (text.contains("action") && text.contains("bypass"))
			{
				return "";
			}
			
			// Returns text without tags.
			return _postText.replaceAll("<.*?>", "");
		}
	}
	
	private final Collection<CPost> _post;
	
	/**
	 * @param postOwner
	 * @param postOwnerId
	 * @param date
	 * @param tid
	 * @param postForumId
	 * @param txt
	 */
	public Post(String postOwner, int postOwnerId, long date, int tid, int postForumId, String txt)
	{
		_post = ConcurrentHashMap.newKeySet();
		final CPost cp = new CPost();
		cp.setPostId(0);
		cp.setPostOwner(postOwner);
		cp.setPostOwnerId(postOwnerId);
		cp.setPostDate(date);
		cp.setPostTopicId(tid);
		cp.setPostForumId(postForumId);
		cp.setPostText(txt);
		_post.add(cp);
		insertindb(cp);
	}
	
	private void insertindb(CPost cp)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("INSERT INTO posts (post_id,post_owner_name,post_ownerid,post_date,post_topic_id,post_forum_id,post_txt) values (?,?,?,?,?,?,?)"))
		{
			ps.setInt(1, cp.getPostId());
			ps.setString(2, cp.getPostOwner());
			ps.setInt(3, cp.getPostOwnerId());
			ps.setLong(4, cp.getPostDate());
			ps.setInt(5, cp.getPostTopicId());
			ps.setInt(6, cp.getPostForumId());
			ps.setString(7, cp.getPostText());
			ps.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e, () -> "Error while saving new Post to db " + e.getMessage());
		}
	}
	
	public Post(Topic t)
	{
		_post = ConcurrentHashMap.newKeySet();
		load(t);
	}
	
	public CPost getCPost(int id)
	{
		int i = 0;
		for (CPost cp : _post)
		{
			if (i++ == id)
			{
				return cp;
			}
		}
		return null;
	}
	
	public void deleteMe(Topic t)
	{
		PostBBSManager.getInstance().delPostByTopic(t);
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("DELETE FROM posts WHERE post_forum_id=? AND post_topic_id=?"))
		{
			ps.setInt(1, t.getForumID());
			ps.setInt(2, t.getID());
			ps.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e, () -> "Error while deleting post: " + e.getMessage());
		}
	}
	
	private void load(Topic t)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT post_id, post_owner_name, post_ownerid, post_date, post_topic_id, post_forum_id, post_txt FROM posts WHERE post_forum_id=? AND post_topic_id=? ORDER BY post_id ASC"))
		{
			ps.setInt(1, t.getForumID());
			ps.setInt(2, t.getID());
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					final CPost cp = new CPost();
					cp.setPostId(rs.getInt("post_id"));
					cp.setPostOwner(rs.getString("post_owner_name"));
					cp.setPostOwnerId(rs.getInt("post_ownerid"));
					cp.setPostDate(rs.getLong("post_date"));
					cp.setPostTopicId(rs.getInt("post_topic_id"));
					cp.setPostForumId(rs.getInt("post_forum_id"));
					cp.setPostText(rs.getString("post_txt"));
					_post.add(cp);
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e, () -> "Data error on Post " + t.getForumID() + "/" + t.getID() + " : " + e.getMessage());
		}
	}
	
	public void updateText(int i)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("UPDATE posts SET post_txt=? WHERE post_id=? AND post_topic_id=? AND post_forum_id=?"))
		{
			final CPost cp = getCPost(i);
			ps.setString(1, cp.getPostText());
			ps.setInt(2, cp.getPostId());
			ps.setInt(3, cp.getPostTopicId());
			ps.setInt(4, cp.getPostForumId());
			ps.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e, () -> "Error while saving new Post to db " + e.getMessage());
		}
	}
}
