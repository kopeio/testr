package executor

import (
	"fmt"
	"github.com/golang/glog"
	"github.com/kopeio/testr/go/pkg/model"
	"io/ioutil"
	"os"
	"path"
	"reflect"
	"time"
)

type Execution struct {
	executionKey *model.ExecutionKey
	result model.Execution

	basePath string

	Logger *ExecutionLogger

	done           bool
	executionError error

	artifacts []*Artifact

	step *model.Step
}

func NewExecution(executionKey *model.ExecutionKey) (*Execution, error) {
	e := &Execution{}

	e.result.Key = executionKey

	basePath, err := ioutil.TempDir("", "execution")
	if err != nil {
		return nil, fmt.Errorf("error creating temp dir: %v", err)
	}

	e.basePath = basePath

	logger, err := NewExecutionLogger(e)
	if err != nil {
		return nil, fmt.Errorf("error creating logger: %v", err)
	}
	e.Logger = logger

	e.result.StartTime = time.Now().Unix()

	return e, nil
}

func (e *Execution) MarkDone(executionError error) {
	e.done = true
	e.executionError = executionError
	err := e.Logger.Flush()
	if err != nil {
		glog.Warning("error flushing execution logger: %v", err)
	}

	if e.executionError != nil {
		e.result.Error = buildErrorInfo(executionError)
		e.result.Success = false
	} else {
		e.result.Success = true
	}
	e.result.EndTime = time.Now().Unix()
}

func (e *Execution) CreateArtifact(relativePath string) (*Artifact, error) {
	fullPath := path.Join(e.basePath, relativePath)
	f, err := os.Create(fullPath)
	if err != nil {
		return nil, fmt.Errorf("error creating file %q: %v", fullPath, err)
	}

	artifact := &Artifact{
		RelativePath: relativePath,
		File:         f,
	}
	e.artifacts = append(e.artifacts, artifact)
	return artifact, nil
}

func (e *Execution) Artifacts() []*Artifact {
	return e.artifacts
}

type Artifact struct {
	RelativePath string
	File         *os.File

	// Once the artifact is uploaded, UploadedSize is populated
	UploadedSize int64
}

func (e *Execution) BuildResult() *model.Execution {
	for _, artifact := range e.artifacts {
		artifactModel := &model.Artifact{
			RelativePath: artifact.RelativePath,
			Size:         artifact.UploadedSize,
		}

		e.result.Artifacts = append(e.result.Artifacts, artifactModel)
	}
	return &e.result
}

func buildErrorInfo(err error) *model.ErrorInfo {
	errType := reflect.TypeOf(err)
	errTypeName := errType.Name()

	message := err.Error()

	errorInfo := &model.ErrorInfo{
		ErrorType: errTypeName,
		Message:   message,
	}

	return errorInfo
}
