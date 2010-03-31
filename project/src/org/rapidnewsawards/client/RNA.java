package org.rapidnewsawards.client;

import java.util.Date;

import org.rapidnewsawards.shared.Edition;
import org.rapidnewsawards.shared.State;


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
import com.google.gwt.user.client.ui.Widget;

/**
 * 
 */
public class RNA extends Composite implements EntryPoint, ValueChangeHandler<String>  {

	interface UB extends UiBinder<Widget, RNA> {}

	@UiField Label status;
	@UiField Label ticker; // text describing how much time left until publication
	@UiField Label title;	
	@UiField Votes votes;
	@UiField Button showStories;
	@UiField Button showUsers;
	@UiField NavBox navbox;
	@UiField DialogBox messageBox;

	// The ticker is updated every minute
	private Timer tickerTimer;
	
	// there is always a current edition, until the periodical is terminated
	private Edition edition;
	
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

	    // If the application starts with no history token, redirect to a new
	    // 'start' state.
	    String initToken = History.getToken();
	    if (initToken.length() == 0) {
	      History.newItem("start");
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
  	  ticker.setText(new Long(hours).toString() + "h" + new Long(minutes + 1).toString() + "m Remaining");
	}

	private long getTimeRemaining() {
  	  if (edition == null)
  		  return 0;

  	  return edition.getEnd().getTime() - new Date().getTime();
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
		ticker.setText("Completed");
		if (tickerTimer == null)
			return;
		
		tickerTimer.cancel();		
	}

	
	@UiHandler("showStories")
	void handleClick(ClickEvent e) {
		History.fireCurrentHistoryState();
	}

	@UiHandler("okButton")
	void handleOk(ClickEvent e) {
		messageBox.hide();		
	}


	public void onValueChange(ValueChangeEvent<String> event) {
		String s = event.getValue();
		Integer editionNum;
		if (s.equals("start")) {
			editionNum = null;
		}
		else {
			if (s.matches("e:[0-9]+")) {
				editionNum = Integer.parseInt(s.split(":")[1]);
			}
			else {
				messageBox.show();
				return;
			}
		}
		
		status.setText("getting Edition");
		rnaService.sendState(editionNum, new AsyncCallback<State>() {

			public void onSuccess(State result) {
				if (result.edition == null) {
					edition = null;
					votes.noEdition();
					status.setText("Rapid News Awards is complete");
					title.setText("Rapid News Awards");
					navbox.setEditionLink("Last Edition", "e:" + result.numEditions);
					cancelTickerTimer();
				}
				else {
					edition = result.edition;
					votes.showEdition(result.edition);
					final int number = result.edition.getNumber();
					status.setText("got Edition " + number);
					title.setText("Rapid News Awards #" + number);
					if (number > 1)
						navbox.setEditionLink("Previous Edition", "e:" + (number - 1));
					scheduleTickerTimer();
				}
			}

			public void onFailure(Throwable caught) {
				status.setText("FAILED: " + caught);
			}
		});
	}

	/**
	 * Create a remote service proxy to talk to the server-side service.
	 */
	private final RNAServiceAsync rnaService = GWT
	.create(RNAService.class);

	private static RNA instance;

	private static UB uiBinder = GWT.create(UB.class);



}
