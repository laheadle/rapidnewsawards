package org.rapidnewsawards.client;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;


public class NavBox extends Composite {

	private SimplePanel sPanel = new SimplePanel();

	public NavBox() {
		sPanel = new SimplePanel();
		initWidget(sPanel);
		setLabelText("");
		// setStyleName("rna-leftnav");
	}

	public void setEditionLink(String text, String key) {
		sPanel.setWidget(new Hyperlink(text, key));		
	}

	public void setLabelText(String text) {
		sPanel.setWidget(new Label(text));		
	}

}
