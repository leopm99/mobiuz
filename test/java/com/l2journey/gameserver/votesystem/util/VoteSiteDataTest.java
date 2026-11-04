package com.l2journey.gameserver.votesystem.util;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.xml.sax.InputSource;

class VoteSiteDataTest
{
	private static VoteSiteData reader()
	{
		return new VoteSiteData() { @Override public void load() { } };
	}

	private static void parse(VoteSiteData data, String xml) throws Exception
	{
		final var factory = DocumentBuilderFactory.newInstance();
		factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		data.parseDocument(factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml))), new File("votesystem.xml"));
	}

	@Test
	void readsExistingFormatAndProtectsRewardCollection() throws Exception
	{
		final var data = reader();
		parse(data, "<list><votesite ordinal='0' name='Test Site'><items><item itemId='57' itemCount='1000'/></items></votesite></list>");
		assertEquals("Test Site", data.getSiteName(0));
		final var rewards = data.getRewards(0);
		assertEquals(57, rewards.iterator().next().getItemId());
		assertEquals(1000, rewards.iterator().next().getItemCount());
		assertThrows(UnsupportedOperationException.class, rewards::clear);
		assertTrue(data.getRewards(99).isEmpty());
		assertEquals("Unknown vote site #99", data.getSiteName(99));
	}

	@Test
	void reloadReplacesRemovedSitesAndDoesNotAccumulateRewards() throws Exception
	{
		final var data = reader();
		final String xml = "<list><votesite ordinal='0' name='Test'><items><item itemId='57' itemCount='1'/></items></votesite></list>";
		parse(data, xml);
		parse(data, xml);
		assertEquals(1, data.getRewards(0).size());
		parse(data, "<list/>");
		assertTrue(data.getRewards(0).isEmpty());
	}

	@Test
	void acceptsTheSpecialGlobalRewardEntry() throws Exception
	{
		final var data = reader();
		parse(data, "<list><votesite ordinal='11' name='Global'><items><item itemId='57' itemCount='100'/></items></votesite></list>");
		assertEquals("Global", data.getSiteName(VoteSiteData.GLOBAL_REWARD_SITE));
		assertEquals(1, data.getRewards(VoteSiteData.GLOBAL_REWARD_SITE).size());
	}

	@Test
	void invalidSiteDoesNotPreventValidSitesFromLoading() throws Exception
	{
		final var data = reader();
		parse(data, "<list><votesite ordinal='0' name='Bad'><items><item itemId='57' itemCount='0'/></items></votesite><votesite ordinal='1' name='Valid'><items><item itemId='4037' itemCount='2'/></items></votesite><votesite ordinal='1' name='Duplicate'/></list>");
		assertTrue(data.getRewards(0).isEmpty());
		assertEquals("Valid", data.getSiteName(1));
		assertEquals(1, data.getRewards(1).size());
	}

	@Test
	void newSitesHaveIndependentOptionsAndGlobalRewardOverrides() throws Exception
	{
		final var data = reader();
		parse(data, "<list><votesite ordinal='11' name='Global'><items><item itemId='57' itemCount='100'/></items></votesite><votesite ordinal='12' name='Hopzone.eu' individualEnabled='false' globalEnabled='true' globalStep='20' checkInterval='30' timeZone='America/Sao_Paulo'><items/><globalItems><item itemId='4037' itemCount='2'/></globalItems></votesite></list>");
		assertFalse(data.isIndividualEnabled(12));
		assertTrue(data.isGlobalEnabled(12));
		assertEquals(20, data.getSettings(12).globalStep());
		assertEquals(30000, data.getSettings(12).checkInterval());
		assertEquals("America/Sao_Paulo", data.getSettings(12).zone().getId());
		assertEquals(4037, data.getGlobalRewards(12).iterator().next().getItemId());
		assertEquals(57, data.getGlobalRewards(0).iterator().next().getItemId());
		parse(data, "<list/>");
		assertFalse(data.isGlobalEnabled(12));
		assertTrue(data.getGlobalRewards(12).isEmpty());
	}

	@Test
	void invalidTimeZoneDoesNotPublishSiteOrItsRewards() throws Exception
	{
		final var data = reader();
		parse(data, "<list><votesite ordinal='12' name='Bad' timeZone='Invalid/Zone'><items/><globalItems><item itemId='57' itemCount='1'/></globalItems></votesite><votesite ordinal='15' name='Valid'><items/></votesite></list>");
		assertFalse(data.isIndividualEnabled(12));
		assertTrue(data.getGlobalRewards(12).isEmpty());
		assertTrue(data.isIndividualEnabled(15));
	}
}
