package io.kope.testr.graphql;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

@Component
public class GqlRoot {
	@Inject
	GqlDataStore dataStore;

	public GqlUser getViewer() {
		return dataStore.getAnonymousUser();
	}

	public GqlJob getJob(String id) {
		return dataStore.getJob(id);
	}

}
