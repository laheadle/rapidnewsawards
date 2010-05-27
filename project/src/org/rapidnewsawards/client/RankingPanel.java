package org.rapidnewsawards.client;

import java.util.LinkedList;

import org.rapidnewsawards.shared.Edition;
import org.rapidnewsawards.shared.StoryInfo;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlexTable;

public class RankingPanel  extends Composite {

	private FlexTable table;
	
	public RankingPanel() {
		table = new FlexTable();
	    //table.setHTML(0, 0, "<span style='font-size: large'> Top Stories </span>");
		initWidget(table);
	}
	
	public void showTopStories(LinkedList<StoryInfo> stories, Edition edition) {
		int i = 1;
		table.clear();
		
		for(StoryInfo info : stories) {
			StoryRecord rec = new StoryRecord(info, edition);
			table.setWidget(i++, 0, rec);
		    table.getFlexCellFormatter().setColSpan(i, 0, 3);
		}
	}
}
