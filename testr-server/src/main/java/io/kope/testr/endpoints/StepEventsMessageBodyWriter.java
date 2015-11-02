package io.kope.testr.endpoints;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Iterator;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;

import com.google.common.base.Charsets;
import com.google.protobuf.util.JsonFormat;
import com.google.protobuf.util.JsonFormat.Printer;

import io.kope.testr.protobuf.model.Model.StepEvent;

public class StepEventsMessageBodyWriter implements MessageBodyWriter<StepEvents> {
	final Printer jsonPrinter = JsonFormat.printer();

	@Override
	public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
		return StepEvents.class.isAssignableFrom(type);
	}

	@Override
	public long getSize(StepEvents t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
		return -1;
	}

	@Override
	public void writeTo(StepEvents t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
			MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
					throws IOException, WebApplicationException {
		OutputStreamWriter appendable = new OutputStreamWriter(entityStream, Charsets.UTF_8);
		Iterator<StepEvent> it = t.getEvents();

		int i = 0;
		appendable.append("[\n");
		while (it.hasNext()) {
			if (i != 0) {
				appendable.append(",\n");
			}
			StepEvent event = it.next();
			jsonPrinter.appendTo(event, appendable);
			i++;
		}
		appendable.append("\n]\n");
		appendable.flush();
	}

}
