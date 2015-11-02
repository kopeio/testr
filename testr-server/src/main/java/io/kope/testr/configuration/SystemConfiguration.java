package io.kope.testr.configuration;

import java.io.File;
import java.io.IOException;

import javax.sql.DataSource;

import org.keyczar.KeyczarFileReader;
import org.keyczar.Signer;
import org.keyczar.Verifier;
import org.keyczar.exceptions.KeyczarException;
import org.keyczar.interfaces.KeyczarReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.google.common.io.ByteSource;
import com.google.common.io.Files;

import io.kope.testr.auth.Authenticator;
import io.kope.testr.auth.SignedTokenAuthenticator;
import io.kope.testr.services.BlobService;
import io.kope.testr.services.ScriptBuilder;
import io.kope.testr.services.s3.S3BlobService;
import io.kope.testr.stores.SqlStore;

@Configuration
public class SystemConfiguration {
	@Value("${s3.keyprefix:artifacts}")
	private String s3KeyPrefix;

	@Value("${s3.bucket}")
	private String s3Bucket;

	@Value("${baseurl}")
	private String baseUrl;

	@Value("${executor.path}")
	private String executorPath;

	@Value("${session.keystore.path}")
	private String sessionKeystorePath;

	@Bean
	public AmazonS3 getS3() throws IOException {
		return new AmazonS3Client();
	}

	@Bean
	public BlobService blobService(AmazonS3 s3) throws IOException {
		return new S3BlobService(s3, s3Bucket, s3KeyPrefix);
	}

	@Bean
	public SqlStore sqlStore(DataSource ds) throws IOException {
		return new SqlStore(ds);
	}

	@Bean
	public ScriptBuilder scriptBuilder() {
		ByteSource executorBlob = Files.asByteSource(new File(executorPath));
		return new ScriptBuilder(executorBlob, baseUrl);
	}

	@Bean
	public Authenticator signedTokenAuthenticator() throws KeyczarException {
		KeyczarReader keyczarReader = new KeyczarFileReader(sessionKeystorePath);
		Signer signer = new Signer(keyczarReader);
		Verifier verifier = new Verifier(keyczarReader);

		SignedTokenAuthenticator signedTokenAuthenticator = new SignedTokenAuthenticator(signer, verifier);
		return signedTokenAuthenticator;
	}

}
