package io.kope.testr.graphql;

import io.kope.testr.protobuf.model.Model.Artifact;

public class GqlArtifact {

	private Artifact data;

	public GqlArtifact(Artifact data) {
		this.data = data;
	}

	public String getPath() {
		return data.getRelativePath();
	}

	public long getSize() {
		return data.getSize();
	}
}
