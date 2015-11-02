package io.kope.testr.services.s3;

import java.io.FilterInputStream;
import java.io.IOException;

import com.amazonaws.services.s3.model.S3Object;

public class S3ObjectInputStream extends FilterInputStream {
	private final String key;
	private final S3Object s3Object;

	public S3ObjectInputStream(String key, S3Object s3Object) {
		super(s3Object.getObjectContent());
		this.key = key;
		this.s3Object = s3Object;
	}

	public long size() {
		return s3Object.getObjectMetadata().getContentLength();
	}

	@Override
	public void close() throws IOException {
		s3Object.close();
		super.close();
	}
}
