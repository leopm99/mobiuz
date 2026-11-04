package com.l2journey.gameserver.votesystem.util;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Strict, bounded JSON reader for vote APIs. Malformed responses never confirm a vote. */
public final class VoteJson
{
	private final String _text;
	private int _pos;
	
	private VoteJson(String text)
	{
		_text = text;
	}
	
	public static Object parse(String text)
	{
		if ((text == null) || (text.length() > 1048576))
		{
			throw new IllegalArgumentException("Invalid JSON size");
		}
		final VoteJson reader = new VoteJson(text);
		final Object value = reader.value(0);
		reader.space();
		if (reader._pos != text.length())
		{
			throw new IllegalArgumentException("Trailing JSON data");
		}
		return value;
	}
	
	private void space()
	{
		while ((_pos < _text.length()) && (" \r\n\t".indexOf(_text.charAt(_pos)) >= 0))
		{
			_pos++;
		}
	}
	
	private boolean take(char c)
	{
		space();
		if ((_pos < _text.length()) && (_text.charAt(_pos) == c))
		{
			_pos++;
			return true;
		}
		return false;
	}
	
	private void expect(char c)
	{
		if (!take(c))
		{
			throw new IllegalArgumentException("Malformed JSON");
		}
	}
	
	private Object value(int depth)
	{
		space();
		if ((depth > 32) || (_pos >= _text.length()))
		{
			throw new IllegalArgumentException("Invalid JSON nesting");
		}
		final char c = _text.charAt(_pos);
		if (c == '"')
		{
			return string();
		}
		if (take('{'))
		{
			final Map<String, Object> map = new LinkedHashMap<>();
			if (take('}'))
			{
				return map;
			}
			do
			{
				final String key = string();
				expect(':');
				if (map.containsKey(key))
				{
					throw new IllegalArgumentException("Duplicate JSON key");
				}
				map.put(key, value(depth + 1));
			}
			while (take(','));
			expect('}');
			return map;
		}
		if (take('['))
		{
			final List<Object> values = new ArrayList<>();
			if (take(']'))
			{
				return values;
			}
			do
			{
				values.add(value(depth + 1));
			}
			while (take(','));
			expect(']');
			return values;
		}
		for (String literal : new String[]
		{
			"true",
			"false",
			"null"
		})
		{
			if (_text.startsWith(literal, _pos))
			{
				_pos += literal.length();
				return literal.equals("null") ? null : Boolean.valueOf(literal);
			}
		}
		final int start = _pos;
		while ((_pos < _text.length()) && ("-+0123456789.eE".indexOf(_text.charAt(_pos)) >= 0))
		{
			_pos++;
		}
		final String number = _text.substring(start, _pos);
		if (!number.matches("-?(0|[1-9][0-9]*)(\\.[0-9]+)?([eE][+-]?[0-9]+)?"))
		{
			throw new IllegalArgumentException("Invalid JSON number");
		}
		return new BigDecimal(number);
	}
	
	private String string()
	{
		expect('"');
		final StringBuilder result = new StringBuilder();
		while (_pos < _text.length())
		{
			char c = _text.charAt(_pos++);
			if (c == '"')
			{
				return result.toString();
			}
			if (c < 32)
			{
				throw new IllegalArgumentException("Control character in JSON");
			}
			if (c == '\\')
			{
				if (_pos >= _text.length())
				{
					throw new IllegalArgumentException("Truncated JSON escape");
				}
				c = _text.charAt(_pos++);
				switch (c)
				{
					case '"', '\\', '/' -> result.append(c);
					case 'b' -> result.append('\b');
					case 'f' -> result.append('\f');
					case 'n' -> result.append('\n');
					case 'r' -> result.append('\r');
					case 't' -> result.append('\t');
					case 'u' -> {
						if ((_pos + 4) > _text.length())
						{
							throw new IllegalArgumentException("Truncated Unicode escape");
						}
						final String hex = _text.substring(_pos, _pos + 4);
						if (!hex.matches("[0-9a-fA-F]{4}"))
						{
							throw new IllegalArgumentException("Invalid Unicode escape");
						}
						result.append((char) Integer.parseInt(hex, 16));
						_pos += 4;
					}
					default -> throw new IllegalArgumentException("Invalid JSON escape");
				}
			}
			else
			{
				result.append(c);
			}
		}
		throw new IllegalArgumentException("Unterminated JSON string");
	}
	
	/**
	 * Search known object wrappers, never unrelated list entries.
	 * @param value
	 * @param keys
	 * @return
	 */
	public static Object field(Object value, String... keys)
	{
		if (value instanceof Map<?, ?> map)
		{
			for (String key : keys)
			{
				for (var entry : map.entrySet())
				{
					if (key.equalsIgnoreCase(String.valueOf(entry.getKey())))
					{
						return entry.getValue();
					}
				}
			}
			for (String wrapper : new String[]
			{
				"result",
				"data",
				"response"
			})
			{
				final Object nested = map.get(wrapper);
				if (nested instanceof Map<?, ?>)
				{
					final Object found = field(nested, keys);
					if (found != null)
					{
						return found;
					}
				}
			}
		}
		if (value instanceof List<?> list && (list.size() == 1))
		{
			return field(list.getFirst(), keys);
		}
		return null;
	}
	
	public static String text(Object value)
	{
		return value == null ? "" : value.toString();
	}
	
	public static long integer(Object value)
	{
		return new BigDecimal(text(value)).longValueExact();
	}
	
	public static boolean confirmed(Object value)
	{
		return Boolean.TRUE.equals(value) || "true".equalsIgnoreCase(text(value)) || "1".equals(text(value)) || "completed".equalsIgnoreCase(text(value));
	}
}
