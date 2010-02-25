package org.rapidnewsawards.client;

import org.rapidnewsawards.shared.Edition;
import org.rapidnewsawards.shared.Link;
import org.rapidnewsawards.shared.User;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.InlineHTML;
import com.google.gwt.user.client.ui.VerticalPanel;

public class Votes extends Composite {

	private VerticalPanel vPanel = new VerticalPanel();

	public Votes() {
		vPanel = new VerticalPanel();
		vPanel.setSpacing(0);
		vPanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
		initWidget(vPanel);
		setStyleName("rna-votes");
	}

	private class Record extends InlineHTML {
		public Record(VoteRecord vr) {
			super(vr.getElement());
		}
	}
	
	public void noEdition() {
		vPanel.clear();
	}
	
	public void showEdition(Edition e) {
		assert(e != null);
		vPanel.clear();
		
		for (User u : e.getUsers()) {
			for (Link v : u.getVotes()) {
				Record rec = new Record(new VoteRecord(u.getName(), v.getUrl()));
				vPanel.add(rec);
				vPanel.setCellWidth(rec, "100%");

			}

		}

	}
}