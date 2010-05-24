package org.rapidnewsawards.client;

import org.rapidnewsawards.shared.StoryInfo;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DisclosurePanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.InlineHTML;
import com.google.gwt.user.client.ui.InlineLabel;

public class StoryRecord extends Composite {

	private HTMLPanel myPanel;
	private static long count = 0; // for making unique id's

	public StoryRecord(StoryInfo info) {
		count++;
		
		String metaid = "meta" + count;
		String titleid ="title" + count;
		String recordid = "record" + count;

		InlineHTML score = new InlineHTML("<span style='color: blue; margin-right: 5px; font-size: large'>"+info.score+"</span>");
		InlineHTML domain = new InlineHTML(" "+info.link.url);
		InlineHTML submitter = new InlineHTML(" / "+info.submitter.email);
		InlineHTML title = new InlineHTML(""+info.link.title);
		
		String html = "<div id='"+recordid+"' style='margin: 10px 0 0 0; padding-bottom: 0'>"+
				"<span style='font-size: medium'; id='"+metaid+"'> </span>" + // score, domain, submitter 
				"<div style='font-size: large; margin: 10px 0 10px 0' id='"+titleid+"'> </div>"  // title
		+ "</div>";
		
		myPanel = new HTMLPanel(html);
		myPanel.add(score, metaid);
		myPanel.add(domain, metaid);
		myPanel.add(submitter, metaid);
		myPanel.add(title, titleid);
		

	    DisclosurePanel disclosure = new DisclosurePanel("Show Votes");
	    disclosure.setContent(new HTML("foo"));

		myPanel.add(disclosure, recordid);
		initWidget(myPanel);
	}
}
