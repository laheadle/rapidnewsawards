package org.rapidnewsawards.server.test;

import static org.easymock.EasyMock.verify;

import org.junit.Test;
import org.rapidnewsawards.server.Config;
import org.rapidnewsawards.server.DAO;
import org.rapidnewsawards.server.Perishable;
import org.rapidnewsawards.shared.Edition;
import org.rapidnewsawards.shared.Name;
import org.rapidnewsawards.shared.User;

public class UserTransitionTest extends EditionTransitionTest {

		
	@Test
	public void testUsers() {
		Edition e1 = DAO.instance.getCurrentEdition(Name.JOURNALISM);
		
		User mg = DAO.instance.findUserByEditionAndUsername(e1, "megangarber");

		User rna = DAO.instance.findRNAUserByEdition(e1);

		Edition e2 = DAO.instance.getCurrentEdition(Name.JOURNALISM);
		User mg2 = DAO.instance.findUserByEditionAndUsername(e2, "megangarber");

		User rna2 = DAO.instance.findRNAUserByEdition(e2);

		assertFalse(mg2.equals(mg));
		assertFalse(rna2.equals(rna));
		
		for(Perishable p : module.mockPs)
			verify(p);
	}


}
