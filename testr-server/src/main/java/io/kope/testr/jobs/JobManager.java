package io.kope.testr.jobs;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;

import io.kope.testr.AppInit;
import io.kope.testr.auth.Authenticator;
import io.kope.testr.protobuf.model.Model.JobData;
import io.kope.testr.services.ExecutionService;
import io.kope.testr.services.JobService;
import io.kope.testr.services.ScriptBuilder;

@Component
public class JobManager implements InitializingBean {
	private static final Logger log = LoggerFactory.getLogger(JobManager.class);

	@Inject
	JobService jobStore;
	@Inject
	ExecutionService executionStore;
	@Inject
	Authenticator authenticator;

	@Inject
	AppInit appInit;

	final Map<String, JobRunner> jobRunners = Maps.newHashMap();

	@Inject
	ScriptBuilder scriptBuilder;

	final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(4);

	public void run() {
		appInit.ensureInit();

		log.info("Performing job sync");
		List<JobData> jobs = jobStore.listAllJobs();

		for (JobData job : jobs) {
			String key = job.getJob();
			JobRunner jobRunner = jobRunners.get(key);
			if (jobRunner == null) {
				jobRunner = new JobRunner(this, job);
				jobRunners.put(key, jobRunner);
				jobRunner.start();
				continue;
			}
			log.warn("JobRunner updating not yet implemented");
			// TODO: jobRunner.update(job);

		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		log.info("Starting " + this.getClass());

		run();
	}

}
