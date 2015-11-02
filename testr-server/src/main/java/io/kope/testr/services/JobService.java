package io.kope.testr.services;

import java.util.List;

import io.kope.testr.protobuf.model.Model.JobData;

public interface JobService {

	List<JobData> listAllJobs();

	JobData findJob(String job);

	void createJob(JobData job);

}
