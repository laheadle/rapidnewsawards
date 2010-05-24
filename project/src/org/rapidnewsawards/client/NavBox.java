package org.rapidnewsawards.client;

import org.rapidnewsawards.shared.Return;
import org.rapidnewsawards.shared.User;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;


public class NavBox extends Composite {

	private SimplePanel sPanel;

	public NavBox() {
		sPanel = new SimplePanel();
		initWidget(sPanel);
		setLabelText("");
		// setStyleName("rna-leftnav");
	}

	public void setFollowCheckBox(boolean value, final User from, final User to, final RNA rna) {
		CheckBox cb = new CheckBox("Following");
		cb.setValue(value);

		cb.addClickHandler(new ClickHandler() {
	      public void onClick(ClickEvent event) {
	        boolean checked = ((CheckBox) event.getSource()).getValue();

	        RNA.rnaService.doSocial(to, checked, new AsyncCallback<Return>() {
	        	
				public void onSuccess(Return result) {
					rna.setStatus(result.s);
				}

				public void onFailure(Throwable caught) {
					rna.setStatus("FAILED: " + caught);
				}
			});

	      }
	    });
	    
		sPanel.setWidget(cb);
	}
	
	public void setLink(String text, String key) {
		sPanel.setWidget(new Hyperlink(text, key));		
	}

	public void setLabelText(String text) {
		sPanel.setWidget(new Label(text));		
	}

}
