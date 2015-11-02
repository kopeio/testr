package io.kope.testr.stores;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.InvalidProtocolBufferException;

import io.kope.testr.protobuf.model.Model.Execution;
import io.kope.testr.protobuf.model.Model.Execution.Builder;
import io.kope.testr.protobuf.model.Model.ExecutionKey;
import io.kope.testr.protobuf.model.Model.JobData;
import io.kope.testr.services.ExecutionService;
import io.kope.testr.services.JobService;
import io.kope.testr.stores.SqlHelper.SqlCommand;

public class SqlStore implements ExecutionService, JobService {

	private static final Logger log = LoggerFactory.getLogger(SqlStore.class);

	final SqlHelper helper;

	public SqlStore(DataSource ds) {
		this.helper = new SqlHelper(ds);
	}

	Optional<Integer> findJobId(String jobName) {
		// TODO: Cache
		Optional<Integer> id = helper.command("SELECT id FROM job WHERE name=?", jobName).fetchOne();
		return id;
	}

	@Override
	public List<ExecutionKey> listExecutionsForRevision(String jobName, String revision) {
		Optional<Integer> jobId = findJobId(jobName);
		if (!jobId.isPresent()) {
			return Collections.emptyList();
		}

		List<ExecutionKey> executions = helper
				.command("SELECT timestamp FROM execution WHERE job=? AND revision=?", jobId.get(), revision)
				.map((ResultSet rs) -> {
					long timestamp;
					try {
						timestamp = rs.getLong(1);
					} catch (SQLException e) {
						throw new StoreException("Error reading data store", e);
					}

					ExecutionKey execution = ExecutionKey.newBuilder().setJob(jobName).setRevision(revision)
							.setTimestamp(timestamp).build();
					return execution;
				});
		return executions;
	}

	@Override
	public void createExecution(ExecutionKey executionKey) {
		Execution result;
		{
			Builder b = Execution.newBuilder();
			b.setKey(executionKey);
			result = b.build();
		}

		byte[] resultBytes = result.toByteArray();
		Optional<Integer> jobId = findJobId(executionKey.getJob());
		if (!jobId.isPresent()) {
			throw new IllegalArgumentException("Job not found: " + executionKey.getJob());
		}

		helper.executeUpdate("INSERT INTO execution (job, revision, timestamp, data) VALUES (?, ?, ?, ?)", jobId.get(),
				executionKey.getRevision(), executionKey.getTimestamp(), resultBytes);
	}

	List<JobData> queryJobs(SqlCommand command) {
		return command.map((ResultSet rs) -> {
			byte[] data;
			try {
				data = rs.getBytes(1);
			} catch (SQLException e) {
				throw new StoreException("Error reading data store", e);
			}

			try {
				return JobData.parseFrom(data);
			} catch (InvalidProtocolBufferException e) {
				throw new StoreException("Error reading data store", e);
			}
		});
	}

	List<Execution> queryExecutions(SqlCommand command) {
		return command.map((ResultSet rs) -> {
			byte[] data;
			try {
				data = rs.getBytes(1);
			} catch (SQLException e) {
				throw new StoreException("Error reading data store", e);
			}

			try {
				return Execution.parseFrom(data);
			} catch (InvalidProtocolBufferException e) {
				throw new StoreException("Error reading data store", e);
			}
		});
	}

	@Override
	public List<JobData> listAllJobs() {
		return queryJobs(helper.command("SELECT data FROM job"));
	}

	@Override
	public JobData findJob(String jobName) {
		List<JobData> jobs = queryJobs(helper.command("SELECT data FROM job WHERE name=?", jobName));
		if (jobs.isEmpty()) {
			return null;
		}
		if (jobs.size() != 1) {
			throw new IllegalStateException("Found multiple jobs with name " + jobName);
		}
		return jobs.get(0);
	}

	@Override
	public Execution findExecution(ExecutionKey executionKey) {
		Optional<Integer> jobId = findJobId(executionKey.getJob());
		if (!jobId.isPresent()) {
			return null;
		}

		List<Execution> executions = queryExecutions(
				helper.command("SELECT data FROM execution WHERE job=? AND revision=? AND timestamp=?", jobId.get(),
						executionKey.getRevision(), executionKey.getTimestamp()));
		if (executions.isEmpty()) {
			return null;
		}
		if (executions.size() != 1) {
			throw new IllegalStateException("Found multiple executions with same key " + executionKey);
		}
		return executions.get(0);
	}

	@Override
	public void createJob(JobData job) {
		byte[] data = job.toByteArray();

		helper.executeUpdate("INSERT INTO job (name, data) VALUES (?, ?)", job.getJob(), data);
	}

	@Override
	public void recordExecution(Execution execution) {
		Optional<Integer> jobId = findJobId(execution.getKey().getJob());
		if (!jobId.isPresent()) {
			throw new IllegalArgumentException("Job not found: " + execution.getKey());
		}

		byte[] executionBytes = execution.toByteArray();

		int rowCount = helper.executeUpdate("UPDATE execution SET data=? WHERE job=? AND revision=? AND timestamp=?",
				executionBytes, jobId.get(), execution.getKey().getRevision(), execution.getKey().getTimestamp());

		if (rowCount != 1) {
			log.warn("Unexpected row count when updating execution result: {}", rowCount);
			if (rowCount == 0) {
				throw new IllegalArgumentException("Failed to find execution to record result");
			} else {
				// Not good ... we updated multiple rows
				// TODO: Rollback?
			}
		}
	}

	@Override
	public List<ExecutionKey> listExecutions(String jobName) {
		Optional<Integer> jobId = findJobId(jobName);
		if (!jobId.isPresent()) {
			return Collections.emptyList();
		}

		List<ExecutionKey> executions = helper
				.command("SELECT revision, timestamp FROM execution WHERE job=? ORDER BY timestamp DESC", jobId.get())
				.map((ResultSet rs) -> {
					String revision;
					long timestamp;
					try {
						revision = rs.getString(1);
						timestamp = rs.getLong(2);
					} catch (SQLException e) {
						throw new StoreException("Error reading data store", e);
					}

					ExecutionKey execution = ExecutionKey.newBuilder().setJob(jobName).setRevision(revision)
							.setTimestamp(timestamp).build();
					return execution;
				});
		return executions;
	}

}
