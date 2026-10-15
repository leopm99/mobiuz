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
 */
package com.l2journey;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for Community Board premium effective-enable helpers.
 */
class CommunityPremiumGateTest
{
	private boolean _premiumSystemWas;
	private boolean _communityShopWas;
	private boolean _communityBufferWas;
	
	@BeforeEach
	void setUp()
	{
		_premiumSystemWas = Config.PREMIUM_SYSTEM_ENABLED;
		_communityShopWas = Config.COMMUNITY_PREMIUM_SYSTEM_ENABLED;
		_communityBufferWas = Config.COMMUNITYBOARD_BUFFER_PREMIUM;
	}
	
	@AfterEach
	void tearDown()
	{
		Config.PREMIUM_SYSTEM_ENABLED = _premiumSystemWas;
		Config.COMMUNITY_PREMIUM_SYSTEM_ENABLED = _communityShopWas;
		Config.COMMUNITYBOARD_BUFFER_PREMIUM = _communityBufferWas;
	}
	
	@Test
	void helpersAreFalseWhenPremiumSystemDisabledEvenIfCommunityFlagsTrue()
	{
		Config.PREMIUM_SYSTEM_ENABLED = false;
		Config.COMMUNITY_PREMIUM_SYSTEM_ENABLED = true;
		Config.COMMUNITYBOARD_BUFFER_PREMIUM = true;
		
		assertFalse(Config.isCommunityPremiumShopEnabled());
		assertFalse(Config.isCommunityPremiumBufferEnabled());
	}
	
	@Test
	void shopHelperTrueOnlyWhenBothPremiumSystemAndCommunityShopEnabled()
	{
		Config.PREMIUM_SYSTEM_ENABLED = true;
		Config.COMMUNITY_PREMIUM_SYSTEM_ENABLED = true;
		
		assertTrue(Config.isCommunityPremiumShopEnabled());
	}
	
	@Test
	void bufferHelperTrueOnlyWhenBothPremiumSystemAndCommunityBufferEnabled()
	{
		Config.PREMIUM_SYSTEM_ENABLED = true;
		Config.COMMUNITYBOARD_BUFFER_PREMIUM = true;
		
		assertTrue(Config.isCommunityPremiumBufferEnabled());
	}
	
	@Test
	void helpersAreFalseWhenCommunityFlagsFalseEvenIfPremiumSystemEnabled()
	{
		Config.PREMIUM_SYSTEM_ENABLED = true;
		Config.COMMUNITY_PREMIUM_SYSTEM_ENABLED = false;
		Config.COMMUNITYBOARD_BUFFER_PREMIUM = false;
		
		assertFalse(Config.isCommunityPremiumShopEnabled());
		assertFalse(Config.isCommunityPremiumBufferEnabled());
	}
}
