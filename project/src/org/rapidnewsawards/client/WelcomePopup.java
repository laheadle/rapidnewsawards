package org.rapidnewsawards.client;

import org.rapidnewsawards.shared.VoteResult;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.TextBox;

public class WelcomePopup extends PopupPanel {

	public RNA rna;
	FlexTable table;
	TextBox donateBox;
	TextBox nickBox;
	
	public void setStatus(String status) {
		table.setWidget(0, 0, new Label(status));
	}
	
	public WelcomePopup(RNA rna) {
		super(false); // don't hide on click

		this.rna = rna;
		nickBox = new TextBox();
		nickBox.setVisibleLength(60);
		donateBox = new TextBox();
		donateBox.setVisibleLength(7);
		table = new FlexTable();

		final Label title = new Label("Welcome to RNA Journalism");
		table.setWidget(0, 0, title);
	    table.getFlexCellFormatter().setColSpan(0, 0, 3);
	    table.getCellFormatter().setHorizontalAlignment(0, 0, HasHorizontalAlignment.ALIGN_CENTER);
	    
		table.setWidget(1, 0, new Label("Please enter your name or nickname"));

		table.setWidget(2, 0, new Label("How many whole dollars would you give to a system like this over the next two months if it were really funding journalism?  Your answer will go towards our hypothetical cash supply."));
		table.setWidget(1, 1, nickBox);
		table.setWidget(2, 1, donateBox);
		
		table.getColumnFormatter().setWidth(0, "250px");
		Button submit = new Button("Submit");
		submit.addClickHandler(new ClickHandler() { 
			
			@Override					
			public void onClick(ClickEvent ev) {
				
				if (!goodDonation()) {
					setStatus("--- Please enter a whole dollar donation amount ----");
					return;
				}
				
				if (badNumber()) {
					setStatus("--- Bad Whole Number ----");
					return;					
				}

				// nine milliion 999k dollars
				if (getDonation() > 999999900) {
					setStatus("--- Yeah, right ----");
					return;
				}
				
				if (noNick()) {
					setStatus("--- Please Enter a Name or Nickname ----");
					return;
				}
				
				
				int donation = getDonation();
				
				RNA.rnaService.welcomeUser(nickBox.getText(), donation, new AsyncCallback<String>() {

					public void onSuccess(String result) {
						hide();
						RNA.instance.setStatus("Welcome, " + nickBox.getText() + "!");
						RNA.instance.fetchUserInfo();
					}

					public void onFailure(Throwable caught) {
						setStatus("ERROR: " + caught);
					}
				});
			}
		});


		table.setWidget(3, 1, submit);

		setWidget(table);
	}

	protected boolean noNick() {
		return nickBox.getText() == null || nickBox.getText().length() == 0;
	}

	protected boolean badNumber() {
		String text = donateBox.getText();
		try {
		Integer.parseInt(text); // convert to pennies		
		}
		catch (NumberFormatException e) {
			return true;
		}
		return false;
	}
	
	protected int getDonation() {
		String text = donateBox.getText();
		return Integer.parseInt(text) * 100; // convert to pennies		
	}

	protected boolean goodDonation() {
		String text = donateBox.getText();
		return text.matches("[0-9]+");
	}
}
