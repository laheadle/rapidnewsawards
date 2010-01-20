package rapidnews.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.UIObject;


public class VoteRecord extends UIObject {

	private static VoteRecordUiBinder uiBinder = GWT
			.create(VoteRecordUiBinder.class);

	interface VoteRecordUiBinder extends UiBinder<DivElement, VoteRecord> {}

	@UiField SpanElement name;
	@UiField SpanElement link;

	public VoteRecord(String name, String link) {
		setElement(uiBinder.createAndBindUi(this));
		this.name.setInnerText(name);
		this.link.setInnerText(link);	
	}

}
