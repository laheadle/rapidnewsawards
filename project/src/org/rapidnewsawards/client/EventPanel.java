package org.rapidnewsawards.client;

import java.util.LinkedList;

import org.rapidnewsawards.shared.Editor_Judge;
import org.rapidnewsawards.shared.User_Link;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.InlineHTML;
import com.google.gwt.user.client.ui.VerticalPanel;

public class EventPanel extends Composite {

	private VerticalPanel vPanel = new VerticalPanel();

	public EventPanel() {
		vPanel = new VerticalPanel();
		vPanel.setSpacing(0);
		vPanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
		initWidget(vPanel);
		setStyleName("rna-eventPanel");
	}

	private class Record extends InlineHTML {
		public Record(EventRecord r) {
			super(r.getElement());
		}
	}

	public void clearPanel() {
		vPanel.clear();
	}

	public void showVotes(LinkedList<User_Link> votes) {
		vPanel.clear();

		for (User_Link v : votes) {
			Record rec = new Record(new EventRecord(v.user.getName(), "voted for", v.link.url));
			vPanel.add(rec);
			vPanel.setCellWidth(rec, "100%");

		}

	}
	
	public void showSocials(LinkedList<Editor_Judge> socials) {
		vPanel.clear();

		for (Editor_Judge s : socials) {
			Record rec = new Record(new EventRecord(s.editor.getName(), "followed", s.judge.getName()));
			vPanel.add(rec);
			vPanel.setCellWidth(rec, "100%");

		}

	}


}
