package com.l2journey.gameserver.donation;

import java.text.Normalizer;

/** Optional, user-supplied payer details. Never derive these from a character name. */
public record BuyerDetails(String firstName, String lastName, String cpf)
{
	public static final BuyerDetails EMPTY = new BuyerDetails(null, null, null);
	
	public BuyerDetails
	{
		firstName = normalizeName(firstName);
		lastName = normalizeName(lastName);
		cpf = normalizeCpf(cpf);
	}
	
	/**
	 * Delimiters preserve empty fields and compound names submitted by the native HTML client.
	 * @param command
	 * @return
	 */
	public static BuyerDetails parseForm(String command)
	{
		if ("skip".equals(command))
		{
			return EMPTY;
		}
		if (!command.startsWith("set|"))
		{
			throw new IllegalArgumentException("Invalid buyer form");
		}
		final String[] fields = command.substring(4).split("\\|", -1);
		if ((fields.length != 4) || !fields[3].isEmpty())
		{
			throw new IllegalArgumentException("Invalid buyer form");
		}
		return new BuyerDetails(fields[0], fields[1], fields[2]);
	}
	
	@Override
	public String toString()
	{
		return "BuyerDetails[redacted]";
	}
	
	private static String optional(String value)
	{
		// The native client may submit 0 for an empty edit box.
		return (value == null) || value.isBlank() || "0".equals(value.trim()) ? null : value;
	}
	
	private static String normalizeName(String value)
	{
		value = optional(value);
		if (value == null)
		{
			return null;
		}
		if (value.codePoints().anyMatch(c -> Character.isISOControl(c) || (Character.getType(c) == Character.FORMAT)))
		{
			throw new IllegalArgumentException("Invalid buyer name");
		}
		value = Normalizer.normalize(value.trim(), Normalizer.Form.NFC).replaceAll(" +", " ");
		if ((value.length() > 64) || !value.matches("[\\p{L}\\p{M}]+(?:[ '’-][\\p{L}\\p{M}]+)*"))
		{
			throw new IllegalArgumentException("Invalid buyer name");
		}
		return value;
	}
	
	private static String normalizeCpf(String value)
	{
		value = optional(value);
		if (value == null)
		{
			return null;
		}
		value = value.trim();
		if (!value.matches("(?:[0-9]{11}|[0-9]{3}\\.[0-9]{3}\\.[0-9]{3}-[0-9]{2})"))
		{
			throw new IllegalArgumentException("Invalid buyer CPF");
		}
		value = value.replace(".", "").replace("-", "");
		if (value.chars().distinct().count() == 1)
		{
			throw new IllegalArgumentException("Invalid buyer CPF");
		}
		for (int digit = 9; digit <= 10; digit++)
		{
			int sum = 0;
			for (int i = 0; i < digit; i++)
			{
				sum += (value.charAt(i) - '0') * ((digit + 1) - i);
			}
			int check = 11 - (sum % 11);
			if (check >= 10)
			{
				check = 0;
			}
			if ((value.charAt(digit) - '0') != check)
			{
				throw new IllegalArgumentException("Invalid buyer CPF");
			}
		}
		return value;
	}
}
