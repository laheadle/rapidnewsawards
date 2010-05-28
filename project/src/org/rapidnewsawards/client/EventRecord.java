package org.rapidnewsawards.client;

import org.rapidnewsawards.shared.User;

import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.InlineLabel;


public class EventRecord extends Composite {
	
	private HTMLPanel myPanel;
	private static long count = 0; // for making unique id's
	
	public EventRecord(User subject, String verb, String object) {
		count++;
		String spanid = "span"+count;
		String paraid = "para"+count;
		String div = "<div>"+
		"	<p style=\"margin-left: 15px; font-size: medium\" id='"+paraid+"'> <span id='"+spanid+"'> </span> "+
		"</p> </div>";

		myPanel = new HTMLPanel(div);
		Anchor a = new Anchor(subject.getDisplayName(), "#user:"+subject.id+":null");
		myPanel.add(a, spanid);
		myPanel.add(new InlineLabel(verb), paraid);
		myPanel.add(new InlineLabel(object), paraid);		
		initWidget(myPanel);
	}

}
