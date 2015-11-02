package io.kope.testr.services;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.io.ByteSource;

import io.kope.testr.protobuf.model.Model.ExecutionKey;
import io.kope.testr.protobuf.model.Model.FetchCodeStep;
import io.kope.testr.protobuf.model.Model.JobData;
import io.kope.testr.protobuf.model.Model.Step;

public class ScriptBuilder {

	private static final Logger log = LoggerFactory.getLogger(ScriptBuilder.class);

	final ByteSource executorBlob;
	final String baseUrl;

	public ScriptBuilder(ByteSource executorBlob, String baseUrl) {
		Preconditions.checkNotNull(executorBlob);
		Preconditions.checkArgument(!Strings.isNullOrEmpty(baseUrl));

		this.executorBlob = executorBlob;
		this.baseUrl = baseUrl;
	}

	public Step getPlan(JobData job, ExecutionKey executionKey) {
		Step plan = job.getPlan();
		Step.Builder planBuilder;
		if (plan == null) {
			log.warn("Building empty plan for job {}", job);
			planBuilder = Step.newBuilder();
		} else {
			planBuilder = Step.newBuilder(plan);
		}

		// Also copies the origin plan
		Step.Builder b = Step.newBuilder(plan);
		if (!updatePlanWithRepo(b, executionKey)) {
			throw new IllegalStateException("Could not find repo to update in plan");
		}

		return planBuilder.build();
	}

	private boolean updatePlanWithRepo(Step.Builder b, ExecutionKey executionKey) {
		boolean changed = false;

		if (b.hasFetchCodeStep()) {
			FetchCodeStep.Builder fetchCodeStep = b.getFetchCodeStepBuilder();
			// TODO: Multiple repos
			fetchCodeStep.setRevision(executionKey.getRevision());
			changed = true;
		} else if (b.hasMultiStep()) {
			for (Step.Builder childStep : b.getMultiStepBuilder().getStepsBuilderList()) {
				changed |= updatePlanWithRepo(childStep, executionKey);
			}
		}

		return changed;
	}

	public String getBoostrapScript(String token, ExecutionKey executionKey) {
		// TODO: Change URLs ... make it always
		// <owner>/<job>/<revision>/<timestamp>/<service>/<extras...>
		// then we can put it all into the base url
		// and probably redo all the handlers also
		// though it does mean we give up the root
		// also what if we don't have a timestamp on an URL...
		// TOOD: Maybe not!

		String executorUrl = baseUrl + "api/system/" + executionKey.getJob() + "/" + executionKey.getRevision() + "/"
				+ executionKey.getTimestamp() + "/executor";

		StringBuilder b = new StringBuilder();
		b.append("#!/bin/bash -ex\n");
		b.append("\n");
		b.append("echo 'Downloading executor'\n");
		b.append("cd /\n");
		// TODO: gzip??
		// TODO: Much cheaper if we download from S3
		b.append("curl -s --fail -H 'Authorization: " + token + "' -O " + executorUrl + "\n");
		b.append("chmod +x /executor\n");
		List<String> args = Lists.newArrayList();

		args.add("--server");
		args.add(baseUrl);

		args.add("--token");
		args.add(token);

		args.add("--job");
		args.add(executionKey.getJob());

		args.add("--revision");
		args.add(executionKey.getRevision());

		args.add("--timestamp");
		args.add(executionKey.getTimestamp() + "");

		b.append("/executor " + Joiner.on(" ").join(args) + "\n");
		b.append("\n");

		return b.toString();
	}

	public ByteSource getExecutor() {
		return executorBlob;
	}

	public String buildBootstrapUrl(ExecutionKey executionKey) {
		return baseUrl + "api/system/" + executionKey.getJob() + "/" + executionKey.getRevision() + "/"
				+ executionKey.getTimestamp() + "/bootstrap";
	}

}
