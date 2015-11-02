package io.kope.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;

import com.google.common.base.Charsets;
import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import com.google.protobuf.util.JsonFormat.Parser;

import jersey.repackaged.com.google.common.base.Throwables;

public class ProtobufGeneratedMessageBodyReader implements MessageBodyReader<GeneratedMessage> {

	final Parser jsonParser = JsonFormat.parser();

	@Override
	public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
		return GeneratedMessage.class.isAssignableFrom(type);
	}

	@Override
	public GeneratedMessage readFrom(Class<GeneratedMessage> type, Type genericType, Annotation[] annotations,
			MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
					throws IOException, WebApplicationException {
		GeneratedMessage messageDefault = getDefaultInstanceForType(type);
		Message.Builder builder = messageDefault.newBuilderForType();

		boolean json = true;
		if (mediaType.getType().equals("application")) {
			String subtype = mediaType.getSubtype();
			if (subtype.equals("protobuf") || subtype.equals("x-protobuf")) {
				json = false;
			}
		}
		if (json) {
			jsonParser.merge(new InputStreamReader(entityStream, Charsets.UTF_8), builder);
		} else {
			builder.mergeFrom(entityStream);
		}
		return (GeneratedMessage) builder.build();
	}

	private GeneratedMessage getDefaultInstanceForType(Class<GeneratedMessage> type) {
		try {
			Method method = type.getMethod("getDefaultInstance");
			return (GeneratedMessage) method.invoke(null);
		} catch (ReflectiveOperationException e) {
			throw Throwables.propagate(e);
		}
	}

}
