package org.rapidnewsawards.server.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;
import org.rapidnewsawards.core.Edition;
import org.rapidnewsawards.core.User;
import org.rapidnewsawards.server.DAO;
import org.rapidnewsawards.server.MakeDataServlet;
import org.rapidnewsawards.server.TitleGrabber;

public class DAOTest extends RNATest {

	// todo
	// don't require case sensitive editor logins
	// test w/explorer
	// make top story submitters / voters into user links
	// test judge joins
	// no self follows
	// improve vote interactivity
	// add support checkbox
	// don't spend first edition, etc
	// user link style
	// single story page
	// make killdata admin
	// send time remaining from server
	// forbid voting / joining dead periodical
	// don't display future editions
	// add time to display vote history
	// break transitions into smaller tasks
	
	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		MakeDataServlet.makeData(2, 30 * 60 * MakeDataServlet.ONE_SECOND, null);
	}

	void verifyEdition() {
		Edition e = DAO.instance.editions.getCurrentEdition();
		assertNotNull(e);
	}

	@Test
	public void testEditions() {
		verifyEdition();
	}

	// TODO disallow voting in expired editions


	@Test
	public void testUser() {
		User mg = getUser(null);
		
		final String displayName = mg.getDisplayName();
		assertEquals(displayName, "john q public");
		assertNotNull(mg);
	}

	@Test
	public void testParser() {
		final String title = "<Title>abc</Title>";
		assertEquals(TitleGrabber.tryGrab(title), "abc");
		assertEquals(TitleGrabber.tryGrab(" <Title>\n j \n </Title> "), 
				"j");
	}
	

}
