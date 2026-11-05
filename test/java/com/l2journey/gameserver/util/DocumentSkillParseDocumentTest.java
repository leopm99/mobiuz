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
package com.l2journey.gameserver.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.l2journey.Config;
import com.l2journey.commons.util.XmlFactory;
import com.l2journey.gameserver.model.skill.Skill;

/**
 * Characterization of {@link DocumentSkill#parseDocument(Document)}.
 */
class DocumentSkillParseDocumentTest
{
	private static final int SKILL_ID_LIST_FIRST = 999001;
	private static final int SKILL_ID_LIST_SECOND = 999002;
	private static final int SKILL_ID_BARE = 999003;
	private static final int LEVELS_SINGLE = 1;
	private static final int LEVELS_MULTI = 2;
	private static final String ATTR_ID = "id";
	private static final String ATTR_NAME = "name";
	private static final String ATTR_LEVELS = "levels";
	private static final String ATTR_VAL = "val";
	private static final String ELEMENT_LIST = "List";
	private static final String ELEMENT_SKILL = "Skill";
	private static final String ELEMENT_SKILL_LOWER = "skill";
	private static final String ELEMENT_SET = "set";
	private static final String ELEMENT_OTHER = "other";
	private static final String OPERATE_TYPE_NAME = "operateType";
	private static final String OPERATE_TYPE_A1 = "A1";
	private static final String NAME_LIST_FIRST = "Test List Skill One";
	private static final String NAME_LIST_SECOND = "Test List Skill Two";
	private static final String NAME_BARE = "Test Bare Skill";
	private static final File UNUSED_FILE = new File("unused.xml");
	
	private static final File PREVIOUS_DATAPACK_ROOT;
	
	static
	{
		PREVIOUS_DATAPACK_ROOT = Config.DATAPACK_ROOT;
		Config.DATAPACK_ROOT = new File("dist/game");
	}
	
	@AfterAll
	static void restoreDatapackRoot()
	{
		Config.DATAPACK_ROOT = PREVIOUS_DATAPACK_ROOT;
	}
	
	@Test
	void parseDocumentCollectsSkillsFromListIgnoringSiblings() throws Exception
	{
		final Document document = newDocument();
		final Element list = document.createElement(ELEMENT_LIST);
		list.appendChild(document.createTextNode(" "));
		list.appendChild(createSkillElement(document, ELEMENT_SKILL, SKILL_ID_LIST_FIRST, LEVELS_SINGLE, NAME_LIST_FIRST));
		list.appendChild(document.createElement(ELEMENT_OTHER));
		list.appendChild(createSkillElement(document, ELEMENT_SKILL, SKILL_ID_LIST_SECOND, LEVELS_MULTI, NAME_LIST_SECOND));
		document.appendChild(list);
		
		final DocumentSkill documentSkill = new DocumentSkill(UNUSED_FILE);
		documentSkill.parseDocument(document);
		
		final List<Skill> skills = documentSkill.getSkills();
		assertEquals(3, skills.size());
		assertSkill(skills.get(0), SKILL_ID_LIST_FIRST, 1, NAME_LIST_FIRST);
		assertSkill(skills.get(1), SKILL_ID_LIST_SECOND, 1, NAME_LIST_SECOND);
		assertSkill(skills.get(2), SKILL_ID_LIST_SECOND, 2, NAME_LIST_SECOND);
	}
	
	@Test
	void parseDocumentCollectsBareSkillIgnoringPrecedingSibling() throws Exception
	{
		final Document document = newDocument();
		// Document allows only one Element child; a comment is a valid non-skill sibling.
		document.appendChild(document.createComment("ignored"));
		document.appendChild(createSkillElement(document, ELEMENT_SKILL_LOWER, SKILL_ID_BARE, LEVELS_SINGLE, NAME_BARE));
		
		final DocumentSkill documentSkill = new DocumentSkill(UNUSED_FILE);
		documentSkill.parseDocument(document);
		
		final List<Skill> skills = documentSkill.getSkills();
		assertEquals(1, skills.size());
		assertSkill(skills.get(0), SKILL_ID_BARE, 1, NAME_BARE);
	}
	
	@Test
	void parseDocumentIgnoresRootThatIsNeitherListNorSkill() throws Exception
	{
		final Document document = newDocument();
		document.appendChild(document.createElement(ELEMENT_OTHER));
		
		final DocumentSkill documentSkill = new DocumentSkill(UNUSED_FILE);
		documentSkill.parseDocument(document);
		
		assertTrue(documentSkill.getSkills().isEmpty());
	}
	
	private static Document newDocument() throws Exception
	{
		return XmlFactory.newDocumentBuilderFactory().newDocumentBuilder().newDocument();
	}
	
	private static Element createSkillElement(Document document, String elementName, int skillId, int levels, String name)
	{
		final Element skill = document.createElement(elementName);
		skill.setAttribute(ATTR_ID, Integer.toString(skillId));
		skill.setAttribute(ATTR_NAME, name);
		skill.setAttribute(ATTR_LEVELS, Integer.toString(levels));
		
		final Element operateType = document.createElement(ELEMENT_SET);
		operateType.setAttribute(ATTR_NAME, OPERATE_TYPE_NAME);
		operateType.setAttribute(ATTR_VAL, OPERATE_TYPE_A1);
		skill.appendChild(operateType);
		return skill;
	}
	
	private static void assertSkill(Skill skill, int expectedId, int expectedLevel, String expectedName)
	{
		assertEquals(expectedId, skill.getId());
		assertEquals(expectedLevel, skill.getLevel());
		assertEquals(expectedName, skill.getName());
	}
}
