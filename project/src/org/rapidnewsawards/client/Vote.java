package org.rapidnewsawards.client;

import org.rapidnewsawards.shared.Link;
import org.rapidnewsawards.shared.RelatedUserInfo;
import org.rapidnewsawards.shared.Return;
import org.rapidnewsawards.shared.VoteResult;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TextBox;

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
		setStatus("Looking up URL ...");
		
		root.add(this);
		setVisible(true);
		
		final String url = Window.Location.getParameter("href");
		
		RNA.rnaService.voteFor(href, Window.Location.createUrlBuilder().buildString(), null, true, new AsyncCallback<VoteResult>() {

			public void onSuccess(VoteResult result) {
				if (result == null) {
					// first time this link is submitted
					final TextBox urlBox = new TextBox();
					urlBox.setVisibleLength(90);
					urlBox.setText(url);
					final TextBox titleBox = new TextBox();					
					titleBox.setVisibleLength(90);
					final Button submit = new Button("Submit");
					Grid g = new Grid(3, 3);
					g.setWidget(0, 0, new Label("Url"));
					g.setWidget(1, 0, new Label("Title"));
					g.setWidget(0, 1, urlBox);
					g.setWidget(1, 1, titleBox);
					g.setWidget(2, 1, submit);
					Button guess = new Button("Guess");
					g.setWidget(1, 2, guess);					
					
				submit.addClickHandler(new ClickHandler() { 
						
						@Override					
						public void onClick(ClickEvent ev) {
							RNA.rnaService.submitStory(urlBox.getText(), titleBox.getText(), null, new AsyncCallback<VoteResult>() {

								public void onSuccess(VoteResult result) {
									setStatus("Your vote was counted");									
								}

								public void onFailure(Throwable caught) {
									setStatus("ERROR: " + caught);
								}
							});
						}
					});

				guess.addClickHandler(new ClickHandler() { 
					
					@Override					
					public void onClick(ClickEvent ev) {
						RNA.rnaService.grabTitle(urlBox.getText(), new AsyncCallback<String>() {

							public void onSuccess(String result) {
								titleBox.setText(result);
							}

							public void onFailure(Throwable caught) {
								setStatus("ERROR: " + caught);
							}
						});
					}
				});
					
				sPanel.setWidget(g);
				}
				else {
					Return returnVal = result.returnVal;
					if (returnVal.equals(Return.SUCCESS)) {
						setStatus("Your vote was counted");
					}
					else if (returnVal.equals(Return.NOT_LOGGED_IN)) {
						String authLink = "<a href=\"" + result.authUrl + "\"> Please log in </a>";
						setStatus(authLink);
					}
					else {
						setStatus(returnVal+"");
					}
				}
			}
				
			public void onFailure(Throwable caught) {
				setStatus("ERROR: " + caught);
			}
		});	
	}
}

