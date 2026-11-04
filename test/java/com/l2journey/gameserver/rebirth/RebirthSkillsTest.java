package com.l2journey.gameserver.rebirth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import com.l2journey.RebirthEngineConfigs;
import com.l2journey.gameserver.data.xml.SkillData;
import com.l2journey.gameserver.data.xml.SkillTreeData;
import com.l2journey.gameserver.model.SkillLearn;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.instance.Folk;
import com.l2journey.gameserver.model.actor.templates.NpcTemplate;
import com.l2journey.gameserver.model.skill.Skill;
import com.l2journey.gameserver.model.skill.enums.AcquireSkillType;
import com.l2journey.gameserver.network.serverpackets.AcquireSkillList;

class RebirthSkillsTest
{
	private SkillTreeData _tree;
	private Player _player;
	private Npc _trainer;
	private final Map<Integer, SkillLearn> _skills = new ConcurrentHashMap<>();
	private boolean _enabled;
	private int _seller;
	@BeforeEach
	void setup() throws Exception
	{
		_enabled = RebirthEngineConfigs.REBIRTH_ENABLED;
		_seller = RebirthEngineConfigs.REBIRTH_SKILL_SELLER_ID;
		RebirthEngineConfigs.REBIRTH_ENABLED = true;
		RebirthEngineConfigs.REBIRTH_SKILL_SELLER_ID = 70003;
		_tree = mock(SkillTreeData.class, CALLS_REAL_METHODS);
		final var field = SkillTreeData.class.getDeclaredField("_rebirthSkillTree"); field.setAccessible(true); field.set(_tree, _skills);
		_player = mock(Player.class);
		when(_player.getLevel()).thenReturn(78);
		when(_player.getSkills()).thenReturn(Map.of());
		_trainer = mock(Npc.class);
		when(_trainer.getId()).thenReturn(70003);
		when(_trainer.canInteract(_player)).thenReturn(true);
		when(_player.getLastFolkNPC()).thenReturn(_trainer);
		final var template = mock(NpcTemplate.class);
		when(_trainer.getTemplate()).thenReturn(template);
		when(template.getId()).thenReturn(70003);
		for (int level = 1; level <= 3; level++)
		{
			final var learn = mock(SkillLearn.class);
			when(learn.getSkillId()).thenReturn(35200);
			when(learn.getSkillLevel()).thenReturn(level);
			when(learn.getGetLevel()).thenReturn(1);
			_skills.put(SkillData.getSkillHashCode(35200, level), learn);
		}
	}
	@AfterEach
	void cleanup() { RebirthEngineConfigs.REBIRTH_ENABLED = _enabled; RebirthEngineConfigs.REBIRTH_SKILL_SELLER_ID = _seller; }
	@Test
	void trainerCanTeachOnlyTheNextRebirthRank()
	{
		assertNotNull(_tree.getSkillLearn(AcquireSkillType.CLASS, 35200, 1, _player));
		assertNull(_tree.getSkillLearn(AcquireSkillType.CLASS, 35200, 2, _player));
		final var known = mock(Skill.class); when(known.getLevel()).thenReturn(1);
		when(_player.getSkills()).thenReturn(Map.of(35200, known));
		assertNotNull(_tree.getSkillLearn(AcquireSkillType.CLASS, 35200, 2, _player));
		assertNull(_tree.getSkillLearn(AcquireSkillType.CLASS, 35200, 3, _player));
	}
	@Test
	void anotherNpcCannotSellRebirthSkills()
	{
		when(_trainer.getId()).thenReturn(30001);
		assertNull(_tree.getSkillLearn(AcquireSkillType.CLASS, 35200, 1, _player));
	}
	@Test
	void distanceSubclassAndDisabledServiceAreEnforcedAtAcquisition()
	{
		when(_trainer.canInteract(_player)).thenReturn(false);
		assertNull(_tree.getSkillLearn(AcquireSkillType.CLASS, 35200, 1, _player));
		when(_trainer.canInteract(_player)).thenReturn(true);
		when(_player.isSubClassActive()).thenReturn(true);
		assertNull(_tree.getSkillLearn(AcquireSkillType.CLASS, 35200, 1, _player));
		when(_player.isSubClassActive()).thenReturn(false);
		RebirthEngineConfigs.REBIRTH_ENABLED = false;
		assertNull(_tree.getSkillLearn(AcquireSkillType.CLASS, 35200, 1, _player));
	}
	@Test
	void rebirthTrainerCannotSellUnrelatedClassSkills()
	{
		assertNull(_tree.getSkillLearn(AcquireSkillType.CLASS, 1, 1, _player));
	}
	@Test
	void ordinarySkillListRoutesToRebirthTreeAndSetsLearningContext()
	{
		try (MockedStatic<SkillTreeData> tree = mockStatic(SkillTreeData.class); MockedStatic<SkillData> data = mockStatic(SkillData.class);
			var managers = mockStatic(RebirthManager.class))
		{
			final var manager = mock(RebirthManager.class);
			managers.when(RebirthManager::getInstance).thenReturn(manager);
			when(manager.canLearnRebirthSkills(_player)).thenReturn(true);
			final var skills = mock(SkillData.class);
			data.when(SkillData::getInstance).thenReturn(skills);
			when(skills.getSkill(35200, 1)).thenReturn(mock(Skill.class));
			tree.when(SkillTreeData::getInstance).thenReturn(_tree);
			Folk.showSkillList(_player, _trainer, null);
			verify(_player).setLastFolkNPC(_trainer);
			verify(_player).setLearningClass(_player.getPlayerClass());
			verify(_player).sendPacket(any(AcquireSkillList.class));
			verify(_trainer.getTemplate(), never()).canTeach(any());
		}
	}
}
