package io.kope.utils;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;

import com.google.common.base.Charsets;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import com.google.protobuf.util.JsonFormat.Printer;

public class ProtobufMessageBodyWriter implements MessageBodyWriter<Message> {

	final Printer jsonPrinter = JsonFormat.printer();

	@Override
	public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
		if (Message.class.isAssignableFrom(type)) {
			return true;
		}
		return false;
	}

	@Override
	public long getSize(Message t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
		return -1;
	}

	@Override
	public void writeTo(Message t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
			MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
					throws IOException, WebApplicationException {
		OutputStreamWriter appendable = new OutputStreamWriter(entityStream, Charsets.UTF_8);
		jsonPrinter.appendTo(t, appendable);
		appendable.flush();
	}

}
