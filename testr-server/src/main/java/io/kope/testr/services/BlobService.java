package io.kope.testr.services;

import java.io.IOException;
import java.io.InputStream;

import io.kope.testr.protobuf.model.Model.ExecutionKey;

public interface BlobService {

	void uploadBlob(ExecutionKey executionKey, String artifactPath, InputStream is) throws IOException;

	InputStream findBlob(ExecutionKey executionKey, String artifactPath);
}
