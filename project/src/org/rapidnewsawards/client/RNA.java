package org.rapidnewsawards.client;

import java.util.Date;

import org.rapidnewsawards.shared.Edition;
import org.rapidnewsawards.shared.RecentSocials;
import org.rapidnewsawards.shared.RecentStories;
import org.rapidnewsawards.shared.RecentVotes;
import org.rapidnewsawards.shared.RelatedUserInfo;
import org.rapidnewsawards.shared.Return;
import org.rapidnewsawards.shared.User;
import org.rapidnewsawards.shared.UserInfo;


import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * History Token format: Action:Arg1:Arg2 where 
 * Action = 'stories' | 'social' | 'top' | 'user'
 * 
 */
public class RNA extends Composite implements EntryPoint, ValueChangeHandler<String>  {

	interface UB extends UiBinder<Widget, RNA> {}


	interface RNAStyle extends CssResource {
		String sideButton();
		String sideButtonSelected();
	}
	
	@UiField RNAStyle style;

	@UiField Label status;
	@UiField Label title;	
	@UiField SimplePanel mainPanel;
	@UiField Button showStories;
	@UiField Button showCurrentEdition;
	@UiField Button showTopStories;
	@UiField Button showSocial;
	@UiField NavBox leftBox;
	
	EventPanel eventPanel;
	
	 /**
	  *  text describing how much time left until publication, or link to next edition	
	  */
	@UiField NavBox rightBox;
	@UiField LayoutPanel outerPanel;
	@UiField LayoutPanel rightColumn;
	@UiField LayoutPanel leftColumn;
	@UiField LayoutPanel topPanel;
	
	// The ticker is updated every minute
	private Timer tickerTimer;
	
	// there is always a current edition, until the periodical is terminated
	private Edition edition;
	protected int numEditions;
	/*
	 * The authenticated user
	 */
	private User user;

	private RankingPanel rankingPanel;
	
	public void setStatus(String string) {
		status.setText(string);
	}
	
	public void clearStatus() {
		setStatus("");
	}

	/**
	 * entry point method.
	 */
	public void onModuleLoad() {
		RootLayoutPanel root = RootLayoutPanel.get();
		eventPanel = new EventPanel();
		rankingPanel = new RankingPanel();
		initWidget(uiBinder.createAndBindUi(this));
		root.add(this);
		setVisible(true);		

        // If the application starts with no history token, redirect to a new
	    // 'start' state.
	    String initToken = History.getToken();
	    if (initToken.length() == 0) {
	      History.newItem("stories:current:null");
	    }

	    // Add history listener
	    History.addValueChangeHandler(this);

	    // Now that we've setup our listener, fire the initial history state.
	    History.fireCurrentHistoryState();
	    
	}
	  
    private void updateTicker() {
  	  long remaining = getTimeRemaining();

  	  if (remaining <= 0) {
  		  cancelTickerTimer();
  		  return;
  	  }
  	  
  	  long minutes = remaining / (1000 * 60);
  	  long hours = minutes / 60;
  	  minutes = minutes % 60;
  	  rightBox.setLabelText(new Long(hours).toString() + "h" + new Long(minutes + 1).toString() + "m Remaining");
	}

	private long getTimeRemaining() {
  	  if (edition == null)
  		  return 0;

  	  return edition.end.getTime() - new Date().getTime();
    }

	private void scheduleTickerTimer() {
		if (tickerTimer != null)
			tickerTimer.cancel();
		
		// sets text right away 
		updateTicker();
		
	    tickerTimer = new Timer() {
	      @Override
		public void run() {
	    	  updateTicker();
	      }
	    };

	    // run every second
	    tickerTimer.scheduleRepeating(1000);
	}

	private void cancelTickerTimer() {
		// TODO say when edition was completed
		if (edition != null) {
			if (edition.number == numEditions - 1)
				rightBox.setLabelText("Completed");
			else 
				rightBox.setLink("Next Edition", getStoriesLink(edition.number + 1));
		}
		if (tickerTimer == null)
			return;

		tickerTimer.cancel();		
	}

	void enable(Object o) {
		if (o == showSocial) {
			showSocial.getElement().removeClassName(style.sideButton());		
			showSocial.getElement().addClassName(style.sideButtonSelected());			
		}
		else {
			showSocial.getElement().removeClassName(style.sideButtonSelected());		
			showSocial.getElement().addClassName(style.sideButton());
		}
		if (o == showCurrentEdition) {
			showCurrentEdition.getElement().removeClassName(style.sideButton());		
			showCurrentEdition.getElement().addClassName(style.sideButtonSelected());						
		}
		else {
			showCurrentEdition.getElement().removeClassName(style.sideButtonSelected());		
			showCurrentEdition.getElement().addClassName(style.sideButton());	
		}
		if (o == showStories) {
			showStories.getElement().removeClassName(style.sideButton());		
			showStories.getElement().addClassName(style.sideButtonSelected());
		}
		else {
			showStories.getElement().removeClassName(style.sideButtonSelected());		
			showStories.getElement().addClassName(style.sideButton());					
		}
		if (o == showTopStories) {
			showTopStories.getElement().removeClassName(style.sideButton());		
			showTopStories.getElement().addClassName(style.sideButtonSelected());
		}
		else {
			showTopStories.getElement().removeClassName(style.sideButtonSelected());		
			showTopStories.getElement().addClassName(style.sideButton());					
		}
	}
	
	@UiHandler("showStories")
	void showStories(ClickEvent e) {
		if (edition instanceof Edition) {
			History.newItem("stories:"+ edition.number+":null");
		}
		else {
			History.newItem("stories:current:null");
		}
		History.fireCurrentHistoryState();
	}

	@UiHandler("showCurrentEdition")
	void showCurrentEdition(ClickEvent e) {
		History.newItem("stories:current:null");
		History.fireCurrentHistoryState();
	}

	@UiHandler("showSocial")
	void showSocial(ClickEvent e) {
		if (edition instanceof Edition) {
			History.newItem("social:"+ edition.number+":null");
		}
		else {
			History.newItem("social:current:null");
		}
		History.fireCurrentHistoryState();
	}

	@UiHandler("showTopStories")
	void showTopStories(ClickEvent e) {
		if (edition instanceof Edition) {
			History.newItem("top:"+ edition.number+":null");
		}
		else {
			History.newItem("top:current:null");
		}
		History.fireCurrentHistoryState();
	}

	private String getStoriesLink(int edition) {
		return "stories:" + edition + ":null";		
	}

	private void showEventPanel() {
		eventPanel.clearPanel();
		eventPanel.setVisible(true);
		rankingPanel.setVisible(false);
		mainPanel.setWidget(eventPanel);		
	}
	
	private void showRankingPanel() {
		eventPanel.setVisible(false);
		rankingPanel.setVisible(true);
		mainPanel.setWidget(rankingPanel);				
	}
	
	private void getStories(Integer editionNum) {
		// fetch edition from server
		setStatus("Getting votes...");
		enable(showStories);
		showEventPanel();

		rnaService.sendRecentVotes(editionNum, new AsyncCallback<RecentVotes>() {

			public void onSuccess(RecentVotes result) {
				openEdition(result.edition, result.numEditions);
				if (result.votes.size() == 0) {
					setStatus("No votes.");
				}
				else {
					clearStatus();
				}
				eventPanel.showVotes(result.votes);
			}

			public void onFailure(Throwable caught) {
				setStatus("FAILED: " + caught);
			}
		});
	}
 

	private void getUser(long userId) {
		setStatus("Getting user...");
		showEventPanel();
		
		final RNA rna = this;
		
		rnaService.sendRelatedUser(userId, new AsyncCallback<RelatedUserInfo>() {

			public void onSuccess(RelatedUserInfo result) {
				title.setText(result.userInfo.user.email);
				cancelTickerTimer();
				leftBox.setLabelText("");
				rightBox.setFollowCheckBox(result.following, user, result.userInfo.user, rna);
				if (result.userInfo.votes.size() == 0) {
					setStatus("No votes.");
				}
				else {
					clearStatus();
				}
				eventPanel.showUser(result.userInfo);
			}

			public void onFailure(Throwable caught) {
				setStatus("FAILED: " + caught);
			}
		});
		
	}

	private void openEdition(Edition e, int numEditions) {
		this.numEditions = numEditions;
		if (e == null) {
			edition = null;
			setStatus("Journalism is complete");
			title.setText("Journalism");
			leftBox.setLink("Last Edition", getStoriesLink(numEditions - 1));
			cancelTickerTimer();
		}
		else {
			edition = e;
			final int number = edition.number;
			title.setText("Journalism #" + number);
			if (number > 0)
				leftBox.setLink("Previous Edition", getStoriesLink(number - 1));
			scheduleTickerTimer();
		}
	}
	
	private void getSocial(Integer editionNum) {
		setStatus("Getting social events...");
		enable(showSocial);
		showEventPanel();
		
		rnaService.sendRecentSocials(editionNum, new AsyncCallback<RecentSocials>() {

			public void onSuccess(RecentSocials result) {
				openEdition(result.edition, result.numEditions);
				if (result.socials.size() == 0) {
					setStatus("No social events.");
				}
				else {
					clearStatus();
				}
				eventPanel.showSocials(result.socials);
			}

			public void onFailure(Throwable caught) {
				setStatus("FAILED: " + caught);
			}
		});
	}

	private void getTopStories(Integer editionNum) {
		setStatus("Getting stories...");
		enable(showTopStories);
		showRankingPanel();
		
		rnaService.sendTopStories(editionNum, new AsyncCallback<RecentStories>() {

			public void onSuccess(RecentStories result) {
				openEdition(result.edition, result.numEditions);
				if (result.stories.size() == 0) {
					setStatus("No stories");
				}
				else {
					clearStatus();
				}
				
				rankingPanel.showTopStories(result.stories);
			}

			public void onFailure(Throwable caught) {
				setStatus("FAILED: " + caught);
			}
		});
	}

	public void onValueChange(ValueChangeEvent<String> event) {
		// check history token for which edition we are on
		String s = event.getValue();
		Integer editionNum;

		String[] tokens = s.split(":");

		if (tokens.length != 3) {
			badRequest();
			return;
		}

		// figure out the edition
		if (tokens[1].equals("current")) {
			editionNum = null;				
		}
		else {
			try {
				editionNum = Integer.parseInt(tokens[1]);
			}
			catch (NumberFormatException e) {
				setStatus("Bad edition");
				return;
			}
		}

		// try to parse a stories link
		if (tokens[0].equals("stories")) {
			getStories(editionNum);
			return;
		}

		// try to parse a social link
		if (tokens[0].equals("social")) {
			getSocial(editionNum);
			return;
		}

		// try to parse a top stories link
		if (tokens[0].equals("top")) {
			getTopStories(editionNum);
			return;
		}

		// try to parse a user link
		if (tokens[0].equals("user")) {
			try {
				Long userId = Long.parseLong(tokens[1]);
				getUser(userId);
				return;
			}
			catch (NumberFormatException e) {
				badRequest();
				return;
			}
		}

		// give up
		badRequest();
		return;
	}

	private void badRequest() {
		setStatus("Unable to understand your request.  Please try again");		
	}

	/**
	 * Create a remote service proxy to talk to the server-side service.
	 */
	public static final RNAServiceAsync rnaService = GWT
	.create(RNAService.class);

	private static RNA instance;

	private static UB uiBinder = GWT.create(UB.class);



}
