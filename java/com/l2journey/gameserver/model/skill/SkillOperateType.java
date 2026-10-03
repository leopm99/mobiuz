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
package com.l2journey.gameserver.model.skill;

/**
 * This enum class holds the skill operative types:
 * <ul>
 * <li>A1</li>
 * <li>A2</li>
 * <li>A3</li>
 * <li>A4</li>
 * <li>CA1</li>
 * <li>CA5</li>
 * <li>DA1</li>
 * <li>DA2</li>
 * <li>P</li>
 * <li>T</li>
 * </ul>
 * @author Zoey76
 */
public enum SkillOperateType
{
	/**
	 * Active Skill with "Instant Effect" (for example damage skills heal/pdam/mdam/cpdam skills).
	 */
	A1,
	
	/**
	 * Active Skill with "Continuous effect + Instant effect" (for example buff/debuff or damage/heal over time skills).
	 */
	A2,
	
	/**
	 * Active Skill with "Instant effect + Continuous effect"
	 */
	A3,
	
	/**
	 * Active Skill with "Instant effect + ?" used for special event herb (itemId 20903, skillId 22158).
	 */
	A4,
	
	/**
	 * Continuous Active Skill with "instant effect" (instant effect casted by ticks).
	 */
	CA1,
	
	/**
	 * Continuous Active Skill with "continuous effect" (continuous effect casted by ticks).
	 */
	CA5,
	
	/**
	 * Directional Active Skill with "Charge/Rush instant effect".
	 */
	DA1,
	
	/**
	 * Directional Active Skill with "Charge/Rush Continuous effect".
	 */
	DA2,
	
	/**
	 * Passive Skill.
	 */
	P,
	
	/**
	 * Toggle Skill.
	 */
	T;
	
	/**
	 * Verifies if the operative type correspond to an active skill.
	 * @return {@code true} if the operative skill type is active, {@code false} otherwise
	 */
	public boolean isActive()
	{
		switch (this)
		{
			case A1, A2, A3, A4, CA1, CA5, DA1, DA2:
			{
				return true;
			}
			default:
			{
				return false;
			}
		}
	}
	
	/**
	 * Verifies if the operative type correspond to a continuous skill.
	 * @return {@code true} if the operative skill type is continuous, {@code false} otherwise
	 */
	public boolean isContinuous()
	{
		switch (this)
		{
			case A2, A4, DA2:
			{
				return true;
			}
			default:
			{
				return false;
			}
		}
	}
	
	/**
	 * Verifies if the operative type correspond to a continuous skill.
	 * @return {@code true} if the operative skill type is continuous, {@code false} otherwise
	 */
	public boolean isSelfContinuous()
	{
		return this == A3;
	}
	
	/**
	 * Verifies if the operative type correspond to a passive skill.
	 * @return {@code true} if the operative skill type is passive, {@code false} otherwise
	 */
	public boolean isPassive()
	{
		return this == P;
	}
	
	/**
	 * Verifies if the operative type correspond to a toggle skill.
	 * @return {@code true} if the operative skill type is toggle, {@code false} otherwise
	 */
	public boolean isToggle()
	{
		return this == T;
	}
	
	/**
	 * Verifies if the operative type correspond to a channeling skill.
	 * @return {@code true} if the operative skill type is channeling, {@code false} otherwise
	 */
	public boolean isChanneling()
	{
		switch (this)
		{
			case CA1, CA5:
			{
				return true;
			}
			default:
			{
				return false;
			}
		}
	}
}
