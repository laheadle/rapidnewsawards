package org.rapidnewsawards.client;

import java.util.LinkedList;

import org.rapidnewsawards.shared.Edition;
import org.rapidnewsawards.shared.Link;
import org.rapidnewsawards.shared.Return;
import org.rapidnewsawards.shared.StoryInfo;
import org.rapidnewsawards.shared.User;
import org.rapidnewsawards.shared.User_Authority;
import org.rapidnewsawards.shared.VoteResult;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.OpenEvent;
import com.google.gwt.event.logical.shared.OpenHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DisclosurePanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.InlineHTML;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;

public class StoryRecord extends Composite {

	private HTMLPanel myPanel;
	public Edition edition;
	public StoryInfo info;
	private static long count = 0; // for making unique id's

	public StoryRecord(StoryInfo info, Edition edition) {
		count++;

		this.info = info;
		this.edition = edition;
		
		String metaid = "meta" + count;
		String titleid ="title" + count;
		String recordid = "record" + count;
		String votesid = "votes" + count;

		String scoreStr;
		if (info.revenue > 0)
			scoreStr = "$" + info.revenue / 100 + "." + info.revenue % 100;
		else
			scoreStr = "" + info.score;
		
		InlineHTML score = new InlineHTML("<span style='color: blue; margin-right: 5px; font-size: large'>"+scoreStr+"</span>");

		String metaText = info.link.domain + " / "+info.submitter.getDisplayName();
		String linkText = info.link.title;
		
		String html = 				"<div style='font-size: large; margin: 10px 10px 10px 10px' id='"+titleid+"'> </div>" +  // title

		"<div id='"+recordid+"' style='margin: 10px 10px 0 0; padding-bottom: 0'>"+
				"<span style='margin-left: 10px; font-size: medium'; id='"+metaid+"'> </span>" + // score, domain, submitter 
				"<div style='margin-left: 5px; ' id='"+votesid+"'> </div>" +
				//"<div style='display: inline; padding-left: 3px' id='"+supportid+"'> </div>" + // support
		 "</div>";
		
		myPanel = new HTMLPanel(html);
		myPanel.add(new Anchor(linkText, true, info.link.url, "_blank"), titleid);
		myPanel.add(score, metaid);
		myPanel.add(new InlineHTML(metaText), metaid);
		
		// myPanel.add(getSupportCheckBox(info.link.url, false), supportid);
		

	    DisclosurePanel disclosure = new DisclosurePanel("Votes");
	    disclosure.setStylePrimaryName("my-DisclosurePanel"); 

	    Voters voters = new Voters();
	    disclosure.setContent(voters);
	    disclosure.addOpenHandler(voters);
	    
		myPanel.add(disclosure, votesid);
		initWidget(myPanel);
	}
	
	private class Voters extends Composite implements OpenHandler<DisclosurePanel> {
		private SimplePanel sPanel;
		
		public Voters () {
			sPanel = new SimplePanel();
			initWidget(sPanel);
			setLabelText("Fetching votes...");
		}

		
		public void setLabelText(String text) {
			sPanel.setWidget(new Label(text));		
		}


		@Override
		public void onOpen(OpenEvent<DisclosurePanel> event) {
	        RNA.rnaService.getVoters(info.link, edition, new AsyncCallback<LinkedList<User_Authority>>() {
	        	
				public void onSuccess(LinkedList<User_Authority> result) {
					setVoters(result);
				}

				public void onFailure(Throwable caught) {
					setLabelText("FAILED: " + caught);
				}
			});			
		}


		protected void setVoters(LinkedList<User_Authority> voters) {
			String str = new String();
			for (User_Authority ua : voters) {
				str += ua.user.getDisplayName() + "(" + ua.authority + ") ";
			}
			sPanel.setWidget(new InlineHTML(str));
		}
	}
	
	private CheckBox getSupportCheckBox(final String link, boolean value) {
		CheckBox cb = new CheckBox("Support");
		cb.setValue(value);

		cb.addClickHandler(new ClickHandler() {
	      public void onClick(ClickEvent event) {
	    	final CheckBox self = (CheckBox) event.getSource();
	        final boolean checked = self.getValue();

	        RNA.rnaService.voteFor(link, 
	        		RNA.instance.getCurrentUrl(), 
	        		edition,
	        		checked,
	        		new AsyncCallback<VoteResult>() {
	        	
				public void onSuccess(VoteResult result) {
					RNA.instance.setStatus(result.returnVal.s);
					if (checked) {
						if (result.returnVal.equals(Return.SUCCESS) ||
								result.returnVal.equals(Return.ALREADY_VOTED)) {
							self.setValue(true);
						}
						else {
							self.setValue(false);
						}
					}
					else {
						if (result.returnVal.equals(Return.SUCCESS) ||
								result.returnVal.equals(Return.HAS_NOT_VOTED)) {
							self.setValue(false);
						}
						else {
							self.setValue(true);
						}						
					}
				}

				public void onFailure(Throwable caught) {
					RNA.instance.setStatus("FAILED: " + caught);
				}
			});

	      }
	    });
		return cb;
		
	}

}
