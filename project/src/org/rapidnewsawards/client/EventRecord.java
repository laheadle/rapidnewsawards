package org.rapidnewsawards.client;

import org.rapidnewsawards.shared.User;

import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.InlineHTML;
import com.google.gwt.user.client.ui.InlineLabel;


public class EventRecord extends Composite {
	
	private HTMLPanel myPanel;
	private static long count = 0; // for making unique id's

	public static Anchor getUserLink(User subject) {
		return new Anchor(subject.getDisplayName(), "#user:"+subject.id+":null");
	}
	
	public EventRecord(User subject, String verb, Anchor object) {
		count++;
		String spanid = "span"+count;
		String paraid = "para"+count;
		String div = "<div>"+
		"	<p style=\"margin: 10px 0px 10px 15px; width:68ex; font-size: medium\" id='"+paraid+"'> <span id='"+spanid+"'> </span> "+
		"</p> </div>";

		myPanel = new HTMLPanel(div);
		if (subject == null) {
			InlineHTML h = new InlineHTML("RNA Journalism");
			myPanel.add(h, spanid);
		}
		else {
			Anchor a = getUserLink(subject);
			myPanel.add(a, spanid);
		}
		myPanel.add(new InlineLabel(verb), paraid);
		myPanel.add(object, paraid);		
		if (subject == null) {
			InlineHTML h = new InlineHTML("!");
			myPanel.add(h, paraid);
		}
		initWidget(myPanel);
	}

}
