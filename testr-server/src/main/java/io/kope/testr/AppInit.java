package io.kope.testr;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import com.google.common.base.Charsets;
import com.google.common.base.Throwables;
import com.google.common.io.CharStreams;

import io.kope.testr.protobuf.model.Model.JobData;
import io.kope.testr.protobuf.model.Model.Step;
import io.kope.testr.services.JobService;

@Component
public class AppInit implements InitializingBean {

	private static final Logger log = LoggerFactory.getLogger(AppInit.class);

	@Inject
	JobService jobService;

	@Override
	public void afterPropertiesSet() throws Exception {
	}

	public void ensureInit() {
		String jobName = "kubernetes";

		JobData job = jobService.findJob(jobName);
		if (job != null) {
			return;
		}

		Step plan;
		{
			Step.Builder b = Step.newBuilder();
			b.setId(1);
			{
				Step.Builder step = b.getMultiStepBuilder().addStepsBuilder();
				step.setId(2);
				step.getFetchCodeStepBuilder().setUrl("https://github.com/kubernetes/kubernetes").build();
			}

			String script;
			try (InputStream is = this.getClass().getResourceAsStream("/k8s-script")) {
				try (InputStreamReader isr = new InputStreamReader(is, Charsets.UTF_8)) {
					script = CharStreams.toString(isr);
				}
			} catch (IOException e) {
				throw Throwables.propagate(e);
			}

			{
				Step.Builder step = b.getMultiStepBuilder().addStepsBuilder();
				step.setId(3);
				step.getScriptStepBuilder().setScript(script);
			}
			plan = b.build();
		}

		{
			JobData.Builder b = JobData.newBuilder();
			b.setJob(jobName);
			b.setRepo("https://github.com/kubernetes/kubernetes");
			b.addBranches("refs/heads/master");
			b.setPlan(plan);
			b.setIsPublic(true);
			job = b.build();
		}

		log.info("Creating job: {}", job);
		jobService.createJob(job);
	}

}
