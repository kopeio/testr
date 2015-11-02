package io.kope.testr.services.s3;

import java.io.IOException;
import java.io.InputStream;

import com.amazonaws.services.s3.AmazonS3;

import io.kope.testr.protobuf.model.Model.ExecutionKey;
import io.kope.testr.services.BlobService;

public class S3BlobService implements BlobService {

	final S3StoreHelper helper;

	public S3BlobService(AmazonS3 s3, String bucket, String keyPrefix) {
		this.helper = new S3StoreHelper(s3, bucket, keyPrefix);
	}

	@Override
	public void uploadBlob(ExecutionKey executionKey, String artifactPath, InputStream is) throws IOException {
		// TODO: Verify job & execution & artifactPath
		String relativeKey = "blob/" + executionKey.getJob() + "/" + executionKey.getRevision() + "/"
				+ executionKey.getTimestamp() + "/" + artifactPath;

		helper.uploadStreamToS3(relativeKey, is);
	}

	@Override
	public InputStream findBlob(ExecutionKey executionKey, String artifactPath) {
		// TODO: Verify job & execution & artifactPath
		String relativeKey = "blob/" + executionKey.getJob() + "/" + executionKey.getRevision() + "/"
				+ executionKey.getTimestamp() + "/" + artifactPath;

		return helper.findObject(relativeKey);
	}

}
