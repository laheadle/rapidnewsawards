package org.rapidnewsawards.client;

import java.util.LinkedList;

import org.rapidnewsawards.shared.SocialInfo;
import org.rapidnewsawards.shared.UserInfo;
import org.rapidnewsawards.shared.User_Link;
import org.rapidnewsawards.shared.Vote_Link;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.InlineHTML;
import com.google.gwt.user.client.ui.VerticalPanel;

public class EventPanel extends Composite {

	private VerticalPanel vPanel;

	public EventPanel() {
		vPanel = new VerticalPanel();
		vPanel.setSpacing(0);
		vPanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
		initWidget(vPanel);
		setStyleName("rna-eventPanel");
	}

	public void clearPanel() {
		vPanel.clear();
	}

	public void showVotes(LinkedList<User_Link> votes) {
		vPanel.clear();

		for (User_Link v : votes) {
			EventRecord rec = new EventRecord(v.user, " voted for ", v.link.url);
			vPanel.add(rec);
			vPanel.setCellWidth(rec, "100%");

		}

	}
	
	public void showSocials(LinkedList<SocialInfo> socials) {
		vPanel.clear();

		for (SocialInfo s : socials) {
			EventRecord rec = new EventRecord(s.editor, s.on ? " is about to follow " : " is about to unfollow ", s.judge.getName());
			vPanel.add(rec);
			vPanel.setCellWidth(rec, "100%");

		}

	}

	public void showUser(UserInfo ui) {
		vPanel.clear();

		for (Vote_Link v : ui.votes) {
			EventRecord rec = new EventRecord(ui.user, " voted for ", v.link.url);
			vPanel.add(rec);
			vPanel.setCellWidth(rec, "100%");
		}

	}

}
