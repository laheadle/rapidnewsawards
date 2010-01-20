package rapidnews.client;

import rapidnews.shared.Edition;
import rapidnews.shared.Reader;
import rapidnews.shared.Vote;

import com.google.gwt.dom.client.Element;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;

// Fixme rename to leaderboard

public class Votes extends Composite {

	private VerticalPanel vPanel = new VerticalPanel();

	public Votes() {
		vPanel = new VerticalPanel();
		vPanel.setSpacing(5);
		initWidget(vPanel);
		setStyleName("rna-votes");
	}

	public void setEdition(Edition result) {
		vPanel.clear();
		for (Reader r : result.getReaders()) {
			for (Vote v : r.getVotes()) {
				// Element e = new VoteRecord(r.getName(), v.getLink().getUrl()).getElement();
				//vPanel.getElement().appendChild(e);
				vPanel.add(new VoteRecord(r.getName(), v.getLink().getUrl()));
			}

		}

	}
}