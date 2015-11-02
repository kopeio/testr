package io.kope.utils;

import java.io.File;
import java.io.IOException;

import com.google.common.io.ByteSink;
import com.google.common.io.Files;

public class TempFile implements AutoCloseable {
	File tempFile;

	TempFile(String prefix, String suffix) throws IOException {
		this.tempFile = File.createTempFile(prefix, suffix);
	}

	public static TempFile create(String prefix, String suffix) throws IOException {
		return new TempFile(prefix, suffix);
	}

	@Override
	public void close() throws IOException {
		if (tempFile != null) {
			tempFile.delete();
			tempFile = null;
		}
	}

	public File getFile() {
		return tempFile;
	}

	public ByteSink asByteSink() {
		return Files.asByteSink(tempFile);
	}
}
