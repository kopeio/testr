package io.kope.testr.endpoints;

import java.util.Iterator;
import java.util.List;

import io.kope.testr.protobuf.model.Model.StepEvent;

public class StepEvents {
	final List<StepEvent> events;

	public StepEvents(List<StepEvent> events) {
		this.events = events;
	}

	public Iterator<StepEvent> getEvents() {
		return events.iterator();
	}
}
