package org.rapidnewsawards.client;

import java.util.LinkedList;

import org.rapidnewsawards.shared.Link;
import org.rapidnewsawards.shared.SocialInfo;
import org.rapidnewsawards.shared.User;
import org.rapidnewsawards.shared.UserInfo;
import org.rapidnewsawards.shared.User_Vote_Link;
import org.rapidnewsawards.shared.Vote_Link;

import com.google.gwt.user.client.ui.Anchor;
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

	public void showVotes(LinkedList<User_Vote_Link> votes) {
		vPanel.clear();

		for (User_Vote_Link v : votes) {
			EventRecord rec = new EventRecord(v.user, getGavePoints(v.vote.authority), getAnchor(v.link));
			vPanel.add(rec);
			vPanel.setCellWidth(rec, "100%");

		}

	}
	
	public void showSocials(LinkedList<SocialInfo> socials) {
		vPanel.clear();

		for (SocialInfo s : socials) {
			Anchor judgeA = EventRecord.getUserLink(s.judge);
			EventRecord rec;
			if (s.editor.getKey().equals(User.getRNAEditor())) {
				rec = new EventRecord(null, "welcomes ", judgeA);				
			}
			else {
				rec = new EventRecord(s.editor, s.on ? " is about to follow " : " is about to unfollow ", judgeA);
			}
			vPanel.add(rec);
			vPanel.setCellWidth(rec, "100%");

		}

	}

	public Anchor getAnchor(Link link) {
		return new Anchor(link.domain + " / " + link.title, link.url, "_blank");
	}

	private String getGavePoints(int authority) {
		String points = authority == 1 ? " point " : " points ";
		return " gave " + authority + points + "to ";
	}
	
	public void showUser(UserInfo ui) {
		vPanel.clear();
		
		if (ui == null)
			return;
		
		for (Vote_Link v : ui.votes) {
			EventRecord rec = new EventRecord(ui.user, getGavePoints(v.vote.authority), getAnchor(v.link));
			vPanel.add(rec);
			vPanel.setCellWidth(rec, "100%");
		}

	}

}
