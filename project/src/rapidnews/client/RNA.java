package rapidnews.client;

import java.util.Date;

import rapidnews.shared.Edition;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class RNA extends Composite implements EntryPoint  {

	interface UB extends UiBinder<Widget, RNA> {}

	@UiField Label status;
	@UiField Label ticker;
	@UiField Label title;	
	@UiField Votes votes;
	@UiField Button refresh;

	// The ticker is updated every minute
	private Timer tickerTimer;
	
	// there is always a current edition
	private Edition edition;
	
	public static RNA getInstance() {
		return instance;
	}

	public void setStatus(String string) {
		status.setText(string);

	}

	/**
	 * entry point method.
	 */
	public void onModuleLoad() {
		instance = this;
		RootLayoutPanel root = RootLayoutPanel.get();
		initWidget(uiBinder.createAndBindUi(this));
		root.add(this);
		setVisible(true);
		refresh();
	}


	private void scheduleTickerTimer() {
		if (tickerTimer != null)
			return; // already scheduled
		
	    tickerTimer = new Timer() {
	      public void run() {
	    	  long remaining = getRemaining();
	    	  if (remaining <= 0) {
		    	  ticker.setText("Completed");	    		  
		    	  return;
	    	  }
	    	  long minutes = remaining / (1000 * 60);
	    	  long hours = minutes / 60;
	    	  minutes = minutes % 60;
	    	  ticker.setText(new Long(hours).toString() + "h" + new Long(minutes + 1).toString() + "m Remaining");
	      }

	      private long getRemaining() {
	    	  if (edition == null)
	    		  return 0;

	    	  return edition.getEnd().getTime() - new Date().getTime();
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

	@UiHandler("refresh")
	void handleClick(ClickEvent e) {
		refresh();
	}

	public void refresh() {
		status.setText("getting Edition");
		rnaService.sendEdition(new AsyncCallback<Edition>() {

			public void onSuccess(Edition result) {
				if (result == null) {
					edition = null;
					votes.noEdition();
					status.setText("Rapid News Awards is complete");
					title.setText("Rapid News Awards");
					cancelTickerTimer();
				}
				else {
					edition = result;
					votes.showEdition(result);
					status.setText("got Edition " + result.getNumber());
					title.setText("Rapid News Awards #" + result.getNumber());
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
