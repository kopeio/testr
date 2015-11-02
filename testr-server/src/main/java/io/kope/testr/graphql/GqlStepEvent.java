package io.kope.testr.graphql;

import com.google.common.base.Joiner;

import io.kope.testr.protobuf.model.Model.OutputEvent;
import io.kope.testr.protobuf.model.Model.StepEvent;

public class GqlStepEvent {

	private StepEvent data;

	public GqlStepEvent(StepEvent data) {
		this.data = data;
	}

	public String getLine() {
		if (data.hasOutputEvent()) {
			OutputEvent outputEvent = data.getOutputEvent();
			return "Output: " + outputEvent.getOutputType() + ": " + outputEvent.getMessage();
		}
		if (data.hasStartStepEvent()) {
			return "StartStep: " + data.getStepId();
		}
		if (data.hasEndStepEvent()) {
			return "EndStep: " + data.getStepId();
		}
		if (data.hasStartCommandEvent()) {
			String cmd = Joiner.on(" ").join(data.getStartCommandEvent().getCommandList());
			return "StartCommand: " + cmd;
		}
		if (data.hasEndCommandEvent()) {
			return "EndCommand: exit code" + data.getEndCommandEvent().getExitCode();
		}

		return "???";
	}
}
