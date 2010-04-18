package org.rapidnewsawards.client;

import java.util.Date;

import org.rapidnewsawards.shared.Edition;
import org.rapidnewsawards.shared.RecentSocials;
import org.rapidnewsawards.shared.RecentVotes;
import org.rapidnewsawards.shared.UserRecord;
import org.rapidnewsawards.shared.VotingRecord;


import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * History Token format: Action:Arg1:Arg2 where 
 * Action = 'stories' | 'social'
 * 
 */
public class RNA extends Composite implements EntryPoint, ValueChangeHandler<String>  {

	interface UB extends UiBinder<Widget, RNA> {}

	@UiField Label status;
	@UiField Label title;	
	@UiField SimplePanel mainPanel;
	@UiField Button showStories;
	@UiField Button showSocial;
	@UiField NavBox leftBox;
	
	EventPanel eventPanel;
	
	 /**
	  *  text describing how much time left until publication, or link to next edition	
	  */
	@UiField NavBox rightBox;
	@UiField DialogBox messageBox;

	// The ticker is updated every minute
	private Timer tickerTimer;
	
	// there is always a current edition, until the periodical is terminated
	private Edition edition;
	protected int numEditions;
	
	public void setStatus(String string) {
		status.setText(string);

	}

	/**
	 * entry point method.
	 */
	public void onModuleLoad() {
		RootLayoutPanel root = RootLayoutPanel.get();
		initWidget(uiBinder.createAndBindUi(this));
		root.add(this);
		setVisible(true);		

		messageBox.show();
		messageBox.hide();

		eventPanel = new EventPanel();
		
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
	      public void run() {
	    	  updateTicker();
	      }
	    };

	    // run every second
	    tickerTimer.scheduleRepeating(1000);
	  }

	private void cancelTickerTimer() {
		// TODO say when edition was completed
		if (edition.number == numEditions)
			rightBox.setLabelText("Completed");
		else 
			rightBox.setEditionLink("Next Edition", getStoriesLink(edition.number + 1));
		if (tickerTimer == null)
			return;
		
		tickerTimer.cancel();		
	}

	
	@UiHandler("showStories")
	void showStories(ClickEvent e) {
		String[] tokens = History.getToken().split(":");
		History.newItem("stories:" + tokens[1] + ":" + tokens[2]);
		History.fireCurrentHistoryState();
	}

	@UiHandler("showSocial")
	void showSocial(ClickEvent e) {
		String[] tokens = History.getToken().split(":");
		History.newItem("social:" + tokens[1] + ":" + tokens[2]);
		History.fireCurrentHistoryState();
	}

	@UiHandler("okButton")
	void handleOk(ClickEvent e) {
		messageBox.hide();		
	}

	private String getStoriesLink(int edition) {
		return "stories:" + edition + ":null";		
	}

	private void getStories(Integer editionNum) {
		// fetch edition from server
		status.setText("getting Stories");
		eventPanel.clearPanel();
		eventPanel.setVisible(true);
		mainPanel.setWidget(eventPanel);

		rnaService.sendRecentVotes(editionNum, new AsyncCallback<RecentVotes>() {

			public void onSuccess(RecentVotes result) {
				numEditions = result.numEditions;
				if (result.edition == null) {
					edition = null;
					status.setText("Rapid News Awards is complete");
					title.setText("Rapid News Awards");
					leftBox.setEditionLink("Last Edition", getStoriesLink(result.numEditions));
					cancelTickerTimer();
				}
				else {
					edition = result.edition;
					eventPanel.showVotes(result.votes);
					final int number = result.edition.number;
					status.setText("Got " + result.votes.size() + " votes");
					title.setText("Rapid News Awards #" + number);
					if (number > 1)
						leftBox.setEditionLink("Previous Edition", getStoriesLink(number - 1));
					scheduleTickerTimer();
				}
			}

			public void onFailure(Throwable caught) {
				status.setText("FAILED: " + caught);
			}
		});
	}


	private void getUser(Integer editionNum, long userId) {
		// TODO Auto-generated method stub
		status.setText("getting User");
		userPanel.clearPanel();
		userPanel.setVisible(true);
		mainPanel.setWidget(userPanel);			
		
		rnaService.sendUser(editionNum, userId, new AsyncCallback<VotingRecord>() {

			public void onSuccess(VotingRecord result) {
				numEditions = result.numEditions;
				if (result.edition == null) {
					edition = null;
					status.setText("Rapid News Awards is complete");
					title.setText("Rapid News Awards");
					leftBox.setEditionLink("Last Edition", getStoriesLink(result.numEditions));
					cancelTickerTimer();
				}
				else {
					edition = result.edition;
					final int number = result.edition.number;
					status.setText("Got " + result.socials.size() + " socials");
					title.setText("Rapid News Awards #" + number);
					if (number > 1)
						leftBox.setEditionLink("Previous Edition", getStoriesLink(number - 1));
					scheduleTickerTimer();
					eventPanel.clearPanel();
					userPanel.showUser(result);
				}
			}

			public void onFailure(Throwable caught) {
				status.setText("FAILED: " + caught);
			}
		});
		
	}

	private void getSocial(Integer editionNum) {
		status.setText("getting Socials");
		eventPanel.clearPanel();
		eventPanel.setVisible(true);
		mainPanel.setWidget(eventPanel);			
		
		rnaService.sendRecentSocials(editionNum, new AsyncCallback<RecentSocials>() {

			public void onSuccess(RecentSocials result) {
				numEditions = result.numEditions;
				if (result.edition == null) {
					edition = null;
					status.setText("Rapid News Awards is complete");
					title.setText("Rapid News Awards");
					leftBox.setEditionLink("Last Edition", getStoriesLink(result.numEditions));
					cancelTickerTimer();
				}
				else {
					edition = result.edition;
					final int number = result.edition.number;
					status.setText("Got " + result.socials.size() + " socials");
					title.setText("Rapid News Awards #" + number);
					if (number > 1)
						leftBox.setEditionLink("Previous Edition", getStoriesLink(number - 1));
					scheduleTickerTimer();
					eventPanel.clearPanel();
					eventPanel.showSocials(result.socials);
				}
			}

			public void onFailure(Throwable caught) {
				status.setText("FAILED: " + caught);
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
				status.setText("Bad edition");
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

		// try to parse a user link
		if (tokens[0].equals("user")) {
			try {
				Long userId = Long.parseLong(tokens[2]);
				getUser(editionNum, userId);
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
		status.setText("Unable to understand your request.  Please try again");		
	}

	/**
	 * Create a remote service proxy to talk to the server-side service.
	 */
	private final RNAServiceAsync rnaService = GWT
	.create(RNAService.class);

	private static RNA instance;

	private static UB uiBinder = GWT.create(UB.class);



}
