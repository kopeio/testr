package executor

import (
	"bufio"
	"fmt"
	"os"
	"os/exec"
	"github.com/kopeio/testr/go/pkg/model"
	"github.com/kopeio/testr/go/pkg/utils"
	"io/ioutil"
	"syscall"
	"github.com/golang/glog"
)

type StepExecutor struct {
	execution *Execution

	logger *ExecutionLogger
}

func NewStepExecutor(execution *Execution) *StepExecutor {
	s := &StepExecutor{
		execution: execution,
		logger: execution.Logger,
	}
	return s
}

func (s *StepExecutor) Execute(plan *model.Step) (bool, error) {
	success, err := s.runStep(plan)
	return success, err
}


func (s *StepExecutor) runFetchCodeStep(step *model.Step, fetchCode *model.FetchCodeStep) (bool, error) {
	err := os.MkdirAll("/src", 0777)
	if err != nil {
		return false, fmt.Errorf("error creating directory '/src': %v", err)
	}

	args := []string{"clone", fetchCode.Url}
	if fetchCode.Branch != "" {
		args = append(args, "--branch", fetchCode.Branch)
	}
	args = append(args, "/src")
	cmd := exec.Command("git", args...)
	message := fmt.Sprintf("cloning git repo %q", fetchCode.Url)
	if fetchCode.Branch != "" {
		message += fmt.Sprintf(" with branch %q", fetchCode.Branch)
	}
	success, err := s.runCommand(step, cmd, message)
	if !success || err != nil {
		return success, err
	}

	if fetchCode.Revision != "" {
		message := fmt.Sprintf("checking out revision %s", fetchCode.Revision)

		args := []string{"checkout", fetchCode.Revision}
		cmd := exec.Command("git", args...)
		cmd.Dir = "/src"

		success, err := s.runCommand(step, cmd, message)
		if !success || err != nil {
			return success, err
		}
	}

	return true, nil
}


func (s *StepExecutor) runMultiStep(step *model.Step, multiStep *model.MultiStep) (bool, error) {
	for _, childStep := range multiStep.Steps {
		success, err := s.runStep(childStep)
		if err != nil || !success {
			return success, err
		}
	}
	return true, nil
}


func (s *StepExecutor) runStep(step *model.Step) (bool, error) {
	log := s.logger

	glog.Info("Running step: ", step)

	startEvent := &model.StepEvent{
		StartStepEvent: &model.StartStepEvent{
			Step: step,
		},
	}
	log.RecordStepEvent(step, startEvent)

	var err error
	var success bool

	if step.ScriptStep != nil {
		success, err = s.runScriptStep(step, step.ScriptStep)
	} else if step.FetchCodeStep != nil {
		success, err = s.runFetchCodeStep(step, step.FetchCodeStep)
	} else if step.MultiStep != nil {
		success, err = s.runMultiStep(step, step.MultiStep)
	} else {
		success = false
		err = fmt.Errorf("Unknown step type in %v", step)
	}


	endEvent := &model.StepEvent{
		EndStepEvent: &model.EndStepEvent{
			Success: success,
		},
	}
	if err != nil {
		endEvent.Error = buildErrorEvent(err)
	}
	log.RecordStepEvent(step, endEvent)

	return success, err
}

func buildErrorEvent(err error) *model.StepEventError {
	errType := fmt.Sprintf("%T", err);
	e := &model.StepEventError{
		Text: err.Error(),
		Type: errType,
	}
	return e
}

// We need to differentiate between an unexpected error and a single-step error; hence (bool, error)
func (s *StepExecutor) runScriptStep(step *model.Step, script *model.ScriptStep) (bool, error) {

	f, err := ioutil.TempFile("", "script")
	if err != nil {
		return false, fmt.Errorf("error creating temp file: %v", err)
	}
	defer utils.CleanupTempFile(f)

	_, err = f.WriteString(script.Script)
	if err != nil {
		return false, fmt.Errorf("error writing script to temp file: %v", err)
	}

	args := []string{"-ex"}
	args = append(args, f.Name())

	cmd := exec.Command("bash", args...)

	env := []string{}
	for _, s := range os.Environ() {
		env = append(env, s)
	}

	// To include seconds since the epoch: \\D{%s}
	env = append(env, "PS4=+${BASH_SOURCE}:${LINENO}:${FUNCNAME[0]}: ")
	//	if s.Branch != "" {
	//		// TODO: Really we should only do this for PRs
	//		env = append(env, "BASE_BRANCH="+s.Branch)
	//	}

	cmd.Dir = "/src"
	cmd.Env = env

	description := fmt.Sprintf("Running script")
	success, err := s.runCommand(step, cmd, description)

	return success, err
}

func (s*StepExecutor) runCommand(step *model.Step, cmd *exec.Cmd, description string) (bool, error) {
	log := s.logger

	stdout, err := cmd.StdoutPipe()
	if err != nil {
		return false, fmt.Errorf("error connecting to stdout: %v", err)
	}

	stderr, err := cmd.StderrPipe()
	if err != nil {
		return false, fmt.Errorf("error connecting to stderr: %v", err)
	}

	fullCommand := []string{cmd.Path }
	fullCommand = append(fullCommand, cmd.Args...)

	startCommandEvent := &model.StepEvent{
		StartCommandEvent: &model.StartCommandEvent{
			Description: description,
			Command: fullCommand,
		},
	}
	log.RecordStepEvent(step, startCommandEvent)
	glog.Infof("%s: %q", description, fullCommand)

	// Note: Don't start the goroutines until we're pretty sure we're able to call Run
	go func() {
		scanner := bufio.NewScanner(stdout)
		for scanner.Scan() {
			line := scanner.Text()

			event := &model.StepEvent{
				OutputEvent: &model.OutputEvent{
					OutputType: model.OutputType_STDOUT,
					Message: line,
				},
			}
			log.RecordStepEvent(step, event)
		}
	}()
	go func() {
		scanner := bufio.NewScanner(stderr)
		for scanner.Scan() {
			line := scanner.Text()

			event := &model.StepEvent{
				OutputEvent: &model.OutputEvent{
					OutputType: model.OutputType_STDERR,
					Message: line,
				},
			}
			log.RecordStepEvent(step, event)
		}
	}()

	err = cmd.Run()

	// TODO: Do we need to wait for goroutines to exit?
	endCommandEvent := &model.StepEvent{
		EndCommandEvent: &model.EndCommandEvent{
			ExitCode: 0,
		},
	}
	success := (err == nil)
	if err != nil {
		// Check for expected errors (exit-code != 0), that are not an error of the runner
		exitError, ok := err.(*exec.ExitError)
		if ok {
			if status, ok := exitError.Sys().(syscall.WaitStatus); ok {
				endCommandEvent.EndCommandEvent.ExitCode = int32(status.ExitStatus())
				// Don't report as an error
				if endCommandEvent.EndCommandEvent.ExitCode != 0 {
					// Still failed, but not an 'internal' error
					err = nil
				}
			}
		}

		if err != nil {
			endCommandEvent.Error = buildErrorEvent(err)
		}
	}
	log.RecordStepEvent(step, endCommandEvent)

	if err != nil {
		return false, fmt.Errorf("error running command: %v", err)
	}

	return success, err
}
