package com.l2journey.gameserver.rebirth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.l2journey.RebirthEngineConfigs;
import com.l2journey.commons.util.ConfigReader;

class RebirthConfigTest
{
	private final Map<java.lang.reflect.Field,Object> _previous = new HashMap<>();
	@BeforeEach
	void setup() throws Exception { for (var field : RebirthEngineConfigs.class.getFields()) { _previous.put(field, field.get(null)); } }
	@AfterEach
	void cleanup() throws Exception { for (var entry : _previous.entrySet()) { entry.getKey().set(null, entry.getValue()); } }
	private void load(String reward)
	{
		try (var reader = mockConstruction(ConfigReader.class, (mock, context) ->
		{
			when(mock.getInt(anyString(), anyInt())).thenAnswer(call -> call.getArgument(1));
			when(mock.getLong(anyString(), anyLong())).thenAnswer(call -> call.getArgument(1));
			when(mock.getString(anyString(), anyString())).thenAnswer(call -> call.getArgument(1));
			when(mock.getBoolean(anyString(), anyBoolean())).thenAnswer(call -> call.getArgument(1));
			when(mock.getString("RebirthItems", "")).thenReturn(reward);
			when(mock.getString(eq("RebirthSkillIds"), anyString())).thenReturn("35200, 35200,35201");
		})) { RebirthEngineConfigs.load(); }
	}
	@Test
	void configurationAcceptsWhitespaceAndTrailingSemicolonAndDeduplicatesSkills()
	{
		load("2822,1; 2822, 1;2822,1;");
		assertTrue(RebirthEngineConfigs.REBIRTH_ENABLED);
		assertEquals(3, RebirthEngineConfigs.REBIRTH_REWARDS.size());
		assertEquals(10_000_000L, RebirthEngineConfigs.REBIRTH_SKILL_RESET_PRICE);
		assertEquals(java.util.List.of(35200,35201), RebirthEngineConfigs.REBIRTH_SKILL_IDS);
	}
	@Test
	void missingOrInvalidRewardDisablesTheService()
	{
		load("2822,1");
		assertFalse(RebirthEngineConfigs.REBIRTH_ENABLED);
		load("2822,-1;2822,1;2822,1");
		assertFalse(RebirthEngineConfigs.REBIRTH_ENABLED);
	}
}
