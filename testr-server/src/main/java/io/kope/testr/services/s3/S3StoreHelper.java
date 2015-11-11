package io.kope.testr.services.s3;

import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import io.kope.utils.TempFile;

public class S3StoreHelper {

	private static final Logger log = LoggerFactory.getLogger(S3StoreHelper.class);

	private final AmazonS3 s3;
	private final String bucket;
	private final String keyPrefix;

	public S3StoreHelper(AmazonS3 s3, String bucket, String keyPrefix) {
		Preconditions.checkNotNull(s3);
		Preconditions.checkState(!Strings.isNullOrEmpty(bucket));
		Preconditions.checkNotNull(keyPrefix);

		this.s3 = s3;
		this.bucket = bucket;
		this.keyPrefix = keyPrefix;
	}

	public PutObjectResult uploadStreamToS3(String relativeKey, InputStream is) throws IOException {
		String key = keyPrefix + "/" + relativeKey;
		log.info("Uploading to s3://{}/{}", bucket, key);

		try (TempFile tempFile = TempFile.create("s3upload", "")) {
			tempFile.asByteSink().writeFrom(is);

			PutObjectRequest request = new PutObjectRequest(bucket, key, tempFile.getFile());
			PutObjectResult response = s3.putObject(request);
			return response;
		}
	}

	S3ObjectInputStream findObject(String relativeKey) {
		String key = keyPrefix + "/" + relativeKey;

		log.info("Reading object data from S3: {}", key);

		GetObjectRequest request = new GetObjectRequest(bucket, key);

		S3Object s3Object;
		try {
			s3Object = s3.getObject(request);
		} catch (AmazonS3Exception e) {
			if (e.getErrorCode().equals("NoSuchKey")) {
				return null;
			}
			throw e;
		}

		return new S3ObjectInputStream(key, s3Object);
	}

}
