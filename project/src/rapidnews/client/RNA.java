package rapidnews.client;

import rapidnews.shared.Edition;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
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
	@UiField Votes votes;
	@UiField Button refresh;

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


	@UiHandler("refresh")
	void handleClick(ClickEvent e) {
		refresh();
	}

	public void refresh() {
		status.setText("getting Edition");
		rnaService.sendEdition(new AsyncCallback<Edition>() {

			public void onSuccess(Edition result) {
				status.setText("got Edition");
				votes.setEdition(result);
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
