package executor

import (
	"bufio"
	"fmt"
	"github.com/kopeio/testr/go/pkg/model"
	"github.com/golang/protobuf/proto"
	"time"
	"sync"
	"github.com/golang/glog"
)

type ExecutionLogger struct {
	execution   *Execution
	logArtifact *Artifact
	writer      *bufio.Writer
	mutex sync.Mutex
	lastTimestamp int64
}

func NewExecutionLogger(execution *Execution) (*ExecutionLogger, error) {
	e := &ExecutionLogger{}
	e.execution = execution

	logArtifact, err := execution.CreateArtifact("output.log")
	if err != nil {
		return nil, fmt.Errorf("error creating log artifact: %v", err)
	}
	e.logArtifact = logArtifact
	e.writer = bufio.NewWriter(logArtifact.File)
	return e, nil
}

func (e *ExecutionLogger) RecordStepEvent(step *model.Step, event *model.StepEvent) {
	e.mutex.Lock()
	defer e.mutex.Unlock()

	event.StepId = step.Id
	t := time.Now()
	ts := t.UnixNano()
	ms := ts / 1000000
	if e.lastTimestamp != 0 {
		delta := ms - e.lastTimestamp
		event.TimestampDelta = delta
	} else {
		event.TimestampAbsolute = ms
	}

	data, err := proto.Marshal(event)
	if err != nil {
		glog.Fatalf("error formatting step event: %v", err)
	}

	lengthPrefix := proto.EncodeVarint(uint64(len(data)))
	_, err = e.writer.Write(lengthPrefix)
	if err == nil {
		_, err = e.writer.Write(data)
	}
	if err != nil {
		glog.Fatalf("error writing to execution log: %v", err)
	}

	e.lastTimestamp = ms
}

//func (e *ExecutionLogger) Infof(format string, args ...interface{}) {
//	glog.Infof(format, args...)
//	msg := fmt.Sprintf(format, args...)
//	_, err := e.writer.WriteString(msg + "\n")
//	if err != nil {
//		glog.Warning("error writing to output file: %v", err)
//	}
//}
//
//func (e *ExecutionLogger) Info(args ...interface{}) {
//	glog.Info(args...)
//	var buf bytes.Buffer
//	for i, arg := range args {
//		if i != 0 {
//			buf.WriteString(" ")
//		}
//		buf.WriteString(fmt.Sprintf("%v", arg))
//	}
//	buf.WriteString("\n")
//	_, err := e.writer.Write(buf.Bytes())
//	if err != nil {
//		glog.Warning("error writing to output file: %v", err)
//	}
//}

func (e *ExecutionLogger) Flush() error {
	return e.writer.Flush()
}
