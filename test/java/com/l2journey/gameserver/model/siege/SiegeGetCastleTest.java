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
package com.l2journey.gameserver.model.siege;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.l2journey.gameserver.data.xml.SiegeScheduleData;

/**
 * Unit tests for null-safe castle access in {@link Siege}.
 */
class SiegeGetCastleTest
{
	private static final int RESIDENCE_ID = 1;
	
	@Test
	void constructorRejectsNullCastle()
	{
		assertThrows(IllegalStateException.class, () -> new Siege(null));
	}
	
	@Test
	void getCastleReturnsBoundCastleAndLooksUpSchedule()
	{
		final Castle castle = mock(Castle.class);
		final SiegeScheduleData scheduleData = mock(SiegeScheduleData.class);
		when(castle.getResidenceId()).thenReturn(RESIDENCE_ID);
		when(scheduleData.getScheduleDateForCastleId(RESIDENCE_ID)).thenReturn(null);
		
		try (MockedStatic<SiegeScheduleData> schedules = mockStatic(SiegeScheduleData.class))
		{
			schedules.when(SiegeScheduleData::getInstance).thenReturn(scheduleData);
			
			final Siege siege = new Siege(castle);
			assertEquals(castle, siege.getCastle());
			verify(scheduleData).getScheduleDateForCastleId(RESIDENCE_ID);
		}
	}
}
