package io.kope.utils;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;

import com.google.common.base.Throwables;
import com.google.common.io.ByteSource;

public class ByteSourceMessageBodyWriter implements MessageBodyWriter<ByteSource> {

	@Override
	public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
		return ByteSource.class.isAssignableFrom(type);
	}

	@Override
	public long getSize(ByteSource t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
		try {
			return t.size();
		} catch (IOException e) {
			throw Throwables.propagate(e);
		}
	}

	@Override
	public void writeTo(ByteSource t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
			MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
					throws IOException, WebApplicationException {
		t.copyTo(entityStream);
	}

}
