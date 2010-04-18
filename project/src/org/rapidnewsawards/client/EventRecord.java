package org.rapidnewsawards.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.UIObject;


public class EventRecord extends UIObject {

	private static EventRecordUiBinder uiBinder = GWT
			.create(EventRecordUiBinder.class);

	interface EventRecordUiBinder extends UiBinder<DivElement, EventRecord> {}

	@UiField SpanElement subject;
	@UiField SpanElement verb;
	@UiField SpanElement object;

	public EventRecord(String subject, String verb, String object) {
		setElement(uiBinder.createAndBindUi(this));
		this.subject.setInnerText(subject);
		this.verb.setInnerText(verb);
		this.object.setInnerText(object);	
	}

}
