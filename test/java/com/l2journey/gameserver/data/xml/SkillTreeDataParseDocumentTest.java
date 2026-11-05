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
package com.l2journey.gameserver.data.xml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.l2journey.commons.util.XmlFactory;
import com.l2journey.gameserver.model.SkillLearn;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.enums.creature.Race;
import com.l2journey.gameserver.model.actor.enums.player.PlayerClass;
import com.l2journey.gameserver.model.actor.enums.player.SocialClass;
import com.l2journey.gameserver.model.item.holders.ItemHolder;
import com.l2journey.gameserver.model.skill.holders.SkillHolder;

/**
 * Characterization of {@link SkillTreeData#parseDocument} and the lookups listed in #51.
 */
class SkillTreeDataParseDocumentTest
{
	private static final File UNUSED_FILE = new File("unused.xml");
	
	private static final String ELEMENT_LIST = "list";
	private static final String ELEMENT_SKILL_TREE = "skillTree";
	private static final String ELEMENT_SKILL = "skill";
	private static final String ELEMENT_OTHER = "other";
	private static final String ELEMENT_ITEM = "item";
	private static final String ELEMENT_PRE_REQ = "preRequisiteSkill";
	private static final String ELEMENT_RACE = "race";
	private static final String ELEMENT_RESIDENCE_ID = "residenceId";
	private static final String ELEMENT_SOCIAL_CLASS = "socialClass";
	private static final String ELEMENT_SUB_CLASS = "subClassConditions";
	private static final String ELEMENT_UNKNOWN = "unknownChild";
	
	private static final String ATTR_TYPE = "type";
	private static final String ATTR_CLASS_ID = "classId";
	private static final String ATTR_PARENT_CLASS_ID = "parentClassId";
	private static final String ATTR_SKILL_NAME = "skillName";
	private static final String ATTR_SKILL_ID = "skillId";
	private static final String ATTR_SKILL_LEVEL = "skillLevel";
	private static final String ATTR_GET_LEVEL = "getLevel";
	private static final String ATTR_LEARNED_BY_NPC = "learnedByNpc";
	private static final String ATTR_ID = "id";
	private static final String ATTR_COUNT = "count";
	private static final String ATTR_LVL = "lvl";
	private static final String ATTR_SLOT = "slot";
	
	private static final String TYPE_CLASS = "classSkillTree";
	private static final String TYPE_FISHING = "fishingSkillTree";
	private static final String TYPE_PLEDGE = "pledgeSkillTree";
	private static final String TYPE_COLLECT = "collectSkillTree";
	private static final String TYPE_SUB_CLASS = "subClassSkillTree";
	private static final String TYPE_UNKNOWN = "notARealSkillTree";
	
	private static final int SKILL_FISHING = 999001;
	private static final int SKILL_PLEDGE = 999002;
	private static final int SKILL_COLLECT = 999003;
	private static final int SKILL_SUB_CLASS = 999004;
	private static final int SKILL_COMMON = 999005;
	private static final int SKILL_CLASS = 999006;
	private static final int SKILL_CLASS_MERGED = 999007;
	private static final int SKILL_UNKNOWN_TREE = 999008;
	private static final int SKILL_MIN_ABOVE = 999009;
	private static final int SKILL_MIN_HIGHER = 999010;
	private static final int SKILL_MIN_NON_NPC = 999011;
	private static final int SKILL_MIN_REACHED = 999012;
	
	private static final int LEVEL_ONE = 1;
	private static final int GET_LEVEL_DEFAULT = 10;
	private static final int ITEM_ID = 57;
	private static final int ITEM_COUNT = 1000;
	private static final int PRE_REQ_ID = 100;
	private static final int PRE_REQ_LVL = 2;
	private static final int RESIDENCE_ID = 5;
	private static final int SUB_SLOT = 1;
	private static final int SUB_LVL = 65;
	private static final int PLAYER_LEVEL = 10;
	private static final int GET_LEVEL_ABOVE = 20;
	private static final int GET_LEVEL_HIGHER = 30;
	private static final int GET_LEVEL_NON_NPC = 15;
	private static final int GET_LEVEL_REACHED = 5;
	
	@Test
	void parseDocumentStoresDirectTreesWithChildDataAndCrossTreeNegatives() throws Exception
	{
		final Document document = newDocument();
		final Element list = document.createElement(ELEMENT_LIST);
		list.appendChild(document.createTextNode(" "));
		list.appendChild(document.createElement(ELEMENT_OTHER));
		
		final Element fishingTree = createSkillTree(document, TYPE_FISHING, null, null);
		final Element fishingSkill = createSkill(document, SKILL_FISHING, LEVEL_ONE, GET_LEVEL_DEFAULT, true);
		appendFishingChildren(document, fishingSkill);
		fishingTree.appendChild(fishingSkill);
		list.appendChild(fishingTree);
		
		list.appendChild(treeWithSkill(document, TYPE_PLEDGE, null, null, SKILL_PLEDGE));
		list.appendChild(treeWithSkill(document, TYPE_COLLECT, null, null, SKILL_COLLECT));
		list.appendChild(treeWithSkill(document, TYPE_SUB_CLASS, null, null, SKILL_SUB_CLASS));
		list.appendChild(treeWithSkill(document, TYPE_CLASS, null, null, SKILL_COMMON));
		list.appendChild(treeWithSkill(document, TYPE_UNKNOWN, null, null, SKILL_UNKNOWN_TREE));
		document.appendChild(list);
		
		final MemorySkillTreeData data = new MemorySkillTreeData();
		data.parseDocument(document, UNUSED_FILE);
		
		final SkillLearn fishing = data.getFishingSkill(SKILL_FISHING, LEVEL_ONE);
		assertNotNull(fishing);
		assertEquals(SKILL_FISHING, fishing.getSkillId());
		assertEquals(LEVEL_ONE, fishing.getSkillLevel());
		assertRequiredItem(fishing);
		assertPreReq(fishing);
		assertTrue(fishing.getRaces().contains(Race.DWARF));
		assertTrue(fishing.getResidenceIds().contains(Integer.valueOf(RESIDENCE_ID)));
		assertEquals(SocialClass.KNIGHT, fishing.getSocialClass());
		assertEquals(1, fishing.getSubClassConditions().size());
		assertEquals(SUB_SLOT, fishing.getSubClassConditions().get(0).getSlot());
		assertEquals(SUB_LVL, fishing.getSubClassConditions().get(0).getLvl());
		assertAbsentFromOthers(data, SKILL_FISHING, LEVEL_ONE, LookupKind.FISHING);
		
		assertDirectLookup(data, SKILL_PLEDGE, LookupKind.PLEDGE);
		assertDirectLookup(data, SKILL_COLLECT, LookupKind.COLLECT);
		assertDirectLookup(data, SKILL_SUB_CLASS, LookupKind.SUB_CLASS);
		assertDirectLookup(data, SKILL_COMMON, LookupKind.COMMON);
		
		assertNull(data.getFishingSkill(SKILL_UNKNOWN_TREE, LEVEL_ONE));
		assertNull(data.getFishingSkill(SKILL_FISHING, LEVEL_ONE + 1));
		assertNull(data.getFishingSkill(SKILL_FISHING + 50, LEVEL_ONE));
		assertNull(data.getClassSkill(SKILL_COMMON, LEVEL_ONE, PlayerClass.FIGHTER));
	}
	
	@Test
	void parseDocumentClassTreesLeaveGetClassSkillNull() throws Exception
	{
		final Document document = newDocument();
		final Element list = document.createElement(ELEMENT_LIST);
		
		list.appendChild(treeWithSkill(document, TYPE_CLASS, PlayerClass.WARRIOR.getId(), PlayerClass.FIGHTER.getId(), SKILL_CLASS));
		list.appendChild(treeWithSkill(document, TYPE_CLASS, PlayerClass.WARRIOR.getId(), PlayerClass.FIGHTER.getId(), SKILL_CLASS_MERGED));
		list.appendChild(treeWithSkill(document, TYPE_CLASS, PlayerClass.WARRIOR.getId(), PlayerClass.WARRIOR.getId(), SKILL_CLASS + 10));
		list.appendChild(treeWithSkill(document, TYPE_CLASS, PlayerClass.WARRIOR.getId(), -1, SKILL_CLASS + 11));
		list.appendChild(treeWithSkill(document, TYPE_CLASS, null, null, SKILL_COMMON));
		document.appendChild(list);
		
		final MemorySkillTreeData data = new MemorySkillTreeData();
		data.parseDocument(document, UNUSED_FILE);
		
		assertNull(data.getClassSkill(SKILL_CLASS, LEVEL_ONE, PlayerClass.WARRIOR));
		assertNull(data.getClassSkill(SKILL_CLASS_MERGED, LEVEL_ONE, PlayerClass.WARRIOR));
		assertNull(data.getCommonSkill(SKILL_CLASS, LEVEL_ONE));
		assertNull(data.getFishingSkill(SKILL_CLASS, LEVEL_ONE));
		assertNull(data.getPledgeSkill(SKILL_CLASS, LEVEL_ONE));
		assertNull(data.getCollectSkill(SKILL_CLASS, LEVEL_ONE));
		assertNull(data.getSubClassSkill(SKILL_CLASS, LEVEL_ONE));
		
		final SkillLearn common = data.getCommonSkill(SKILL_COMMON, LEVEL_ONE);
		assertNotNull(common);
		assertEquals(SKILL_COMMON, common.getSkillId());
		assertNull(data.getClassSkill(SKILL_COMMON, LEVEL_ONE, PlayerClass.FIGHTER));
		assertNull(data.getFishingSkill(SKILL_COMMON, LEVEL_ONE));
	}
	
	@Test
	void parseDocumentIgnoresRootThatIsNotList() throws Exception
	{
		final Document document = newDocument();
		final Element root = document.createElement(ELEMENT_OTHER);
		root.appendChild(treeWithSkill(document, TYPE_FISHING, null, null, SKILL_FISHING));
		document.appendChild(root);
		
		final MemorySkillTreeData data = new MemorySkillTreeData();
		data.parseDocument(document, UNUSED_FILE);
		
		assertNull(data.getFishingSkill(SKILL_FISHING, LEVEL_ONE));
		assertNull(data.getPledgeSkill(SKILL_FISHING, LEVEL_ONE));
		assertNull(data.getCollectSkill(SKILL_FISHING, LEVEL_ONE));
		assertNull(data.getSubClassSkill(SKILL_FISHING, LEVEL_ONE));
		assertNull(data.getCommonSkill(SKILL_FISHING, LEVEL_ONE));
		assertNull(data.getClassSkill(SKILL_FISHING, LEVEL_ONE, PlayerClass.FIGHTER));
	}
	
	@Test
	void getMinLevelForNewSkillReturnsNumericMinimumAbovePlayer() throws Exception
	{
		final Document document = newDocument();
		final Element list = document.createElement(ELEMENT_LIST);
		final Element fishingTree = createSkillTree(document, TYPE_FISHING, null, null);
		fishingTree.appendChild(createSkill(document, SKILL_MIN_HIGHER, LEVEL_ONE, GET_LEVEL_HIGHER, true));
		fishingTree.appendChild(createSkill(document, SKILL_MIN_NON_NPC, LEVEL_ONE, GET_LEVEL_NON_NPC, false));
		fishingTree.appendChild(createSkill(document, SKILL_MIN_ABOVE, LEVEL_ONE, GET_LEVEL_ABOVE, true));
		fishingTree.appendChild(createSkill(document, SKILL_MIN_REACHED, LEVEL_ONE, GET_LEVEL_REACHED, true));
		list.appendChild(fishingTree);
		document.appendChild(list);
		
		final MemorySkillTreeData data = new MemorySkillTreeData();
		data.parseDocument(document, UNUSED_FILE);
		
		final Player player = mock(Player.class);
		when(player.getLevel()).thenReturn(PLAYER_LEVEL);
		
		assertEquals(GET_LEVEL_ABOVE, data.getMinLevelForNewSkill(player, data.getFishingSkillTree()));
	}
	
	@Test
	void getMinLevelForNewSkillEmptyTreeReturnsZero()
	{
		final MemorySkillTreeData data = new MemorySkillTreeData();
		final Player player = mock(Player.class);
		when(player.getLevel()).thenReturn(PLAYER_LEVEL);
		
		assertEquals(0, data.getMinLevelForNewSkill(player, Collections.emptyMap()));
	}
	
	private static void assertDirectLookup(MemorySkillTreeData data, int skillId, LookupKind kind)
	{
		assertNotNull(lookup(data, skillId, LEVEL_ONE, kind));
		assertEquals(skillId, lookup(data, skillId, LEVEL_ONE, kind).getSkillId());
		assertAbsentFromOthers(data, skillId, LEVEL_ONE, kind);
		assertNull(data.getClassSkill(skillId, LEVEL_ONE, PlayerClass.FIGHTER));
	}
	
	private static void assertAbsentFromOthers(MemorySkillTreeData data, int skillId, int level, LookupKind present)
	{
		for (LookupKind kind : LookupKind.values())
		{
			if (kind == present)
			{
				continue;
			}
			assertNull(lookup(data, skillId, level, kind));
		}
	}
	
	private static SkillLearn lookup(MemorySkillTreeData data, int skillId, int level, LookupKind kind)
	{
		return switch (kind)
		{
			case FISHING -> data.getFishingSkill(skillId, level);
			case PLEDGE -> data.getPledgeSkill(skillId, level);
			case COLLECT -> data.getCollectSkill(skillId, level);
			case SUB_CLASS -> data.getSubClassSkill(skillId, level);
			case COMMON -> data.getCommonSkill(skillId, level);
		};
	}
	
	private static void assertRequiredItem(SkillLearn skillLearn)
	{
		final Set<ItemHolder> items = skillLearn.getRequiredItems();
		assertEquals(1, items.size());
		final ItemHolder item = items.iterator().next();
		assertEquals(ITEM_ID, item.getId());
		assertEquals(ITEM_COUNT, item.getCount());
	}
	
	private static void assertPreReq(SkillLearn skillLearn)
	{
		final Set<SkillHolder> preReqs = skillLearn.getPreReqSkills();
		assertEquals(1, preReqs.size());
		final SkillHolder holder = preReqs.iterator().next();
		assertEquals(PRE_REQ_ID, holder.getSkillId());
		assertEquals(PRE_REQ_LVL, holder.getSkillLevel());
	}
	
	private static void appendFishingChildren(Document document, Element skill)
	{
		final Element item = document.createElement(ELEMENT_ITEM);
		item.setAttribute(ATTR_ID, Integer.toString(ITEM_ID));
		item.setAttribute(ATTR_COUNT, Integer.toString(ITEM_COUNT));
		skill.appendChild(item);
		
		final Element preReq = document.createElement(ELEMENT_PRE_REQ);
		preReq.setAttribute(ATTR_ID, Integer.toString(PRE_REQ_ID));
		preReq.setAttribute(ATTR_LVL, Integer.toString(PRE_REQ_LVL));
		skill.appendChild(preReq);
		
		final Element race = document.createElement(ELEMENT_RACE);
		race.setTextContent(Race.DWARF.name());
		skill.appendChild(race);
		
		final Element residence = document.createElement(ELEMENT_RESIDENCE_ID);
		residence.setTextContent(Integer.toString(RESIDENCE_ID));
		skill.appendChild(residence);
		
		final Element social = document.createElement(ELEMENT_SOCIAL_CLASS);
		social.setTextContent(SocialClass.KNIGHT.name());
		skill.appendChild(social);
		
		final Element subClass = document.createElement(ELEMENT_SUB_CLASS);
		subClass.setAttribute(ATTR_SLOT, Integer.toString(SUB_SLOT));
		subClass.setAttribute(ATTR_LVL, Integer.toString(SUB_LVL));
		skill.appendChild(subClass);
		
		skill.appendChild(document.createElement(ELEMENT_UNKNOWN));
	}
	
	private static Element treeWithSkill(Document document, String type, Integer classId, Integer parentClassId, int skillId)
	{
		final Element tree = createSkillTree(document, type, classId, parentClassId);
		tree.appendChild(createSkill(document, skillId, LEVEL_ONE, GET_LEVEL_DEFAULT, true));
		return tree;
	}
	
	private static Element createSkillTree(Document document, String type, Integer classId, Integer parentClassId)
	{
		final Element tree = document.createElement(ELEMENT_SKILL_TREE);
		tree.setAttribute(ATTR_TYPE, type);
		if (classId != null)
		{
			tree.setAttribute(ATTR_CLASS_ID, Integer.toString(classId));
		}
		if (parentClassId != null)
		{
			tree.setAttribute(ATTR_PARENT_CLASS_ID, Integer.toString(parentClassId));
		}
		return tree;
	}
	
	private static Element createSkill(Document document, int skillId, int skillLevel, int getLevel, boolean learnedByNpc)
	{
		final Element skill = document.createElement(ELEMENT_SKILL);
		skill.setAttribute(ATTR_SKILL_NAME, "Test Skill " + skillId);
		skill.setAttribute(ATTR_SKILL_ID, Integer.toString(skillId));
		skill.setAttribute(ATTR_SKILL_LEVEL, Integer.toString(skillLevel));
		skill.setAttribute(ATTR_GET_LEVEL, Integer.toString(getLevel));
		skill.setAttribute(ATTR_LEARNED_BY_NPC, Boolean.toString(learnedByNpc));
		return skill;
	}
	
	private static Document newDocument() throws Exception
	{
		return XmlFactory.newDocumentBuilderFactory().newDocumentBuilder().newDocument();
	}
	
	private enum LookupKind
	{
		FISHING,
		PLEDGE,
		COLLECT,
		SUB_CLASS,
		COMMON
	}
	
	/**
	 * SkillTreeData whose constructor does not parse the datapack.
	 */
	private static final class MemorySkillTreeData extends SkillTreeData
	{
		MemorySkillTreeData()
		{
			// Super constructor calls load(); the override below keeps maps empty.
		}
		
		@Override
		public void load()
		{
			// Skip datapack parsing so tests drive parseDocument with in-memory DOMs.
		}
	}
}
