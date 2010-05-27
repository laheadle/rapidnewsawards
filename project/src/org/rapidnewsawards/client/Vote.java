package org.rapidnewsawards.client;

import org.rapidnewsawards.shared.Link;
import org.rapidnewsawards.shared.RelatedUserInfo;
import org.rapidnewsawards.shared.Return;
import org.rapidnewsawards.shared.VoteResult;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.SimplePanel;

public class Vote extends Composite implements EntryPoint {

	private SimplePanel sPanel;
	
	private void setStatus(String status) {
		sPanel.setWidget(new HTML(status));
	}
	
	@Override
	public void onModuleLoad() {
		RootLayoutPanel root = RootLayoutPanel.get();
		String href = Window.Location.getParameter("href");
		sPanel = new SimplePanel();
		initWidget(sPanel);
		setStatus("Voting for " + href + "...");
		
		root.add(this);
		setVisible(true);
		
		RNA.rnaService.voteFor(href, Window.Location.createUrlBuilder().buildString(), null, true, new AsyncCallback<VoteResult>() {

			public void onSuccess(VoteResult result) {
				Return returnVal = result.returnVal;
				
				if (returnVal.equals(Return.SUCCESS)) {
					String authLink = "<a href=\"" + result.authUrl + "\"> (Log out) </a>";
					setStatus("Your vote was counted" + authLink);
				}
				else if (returnVal.equals(Return.NOT_LOGGED_IN)) {
					String authLink = "<a href=\"" + result.authUrl + "\"> Please log in </a>";
					setStatus(authLink);
				}
				else {
					setStatus(returnVal+"");
				}
			}

			public void onFailure(Throwable caught) {
				setStatus("ERROR: " + caught);
			}
		});	
	}
}

