package main

import (
	"flag"
	"os"

	"fmt"
	"github.com/golang/glog"
	"github.com/kopeio/testr/go/pkg/executor"
	"github.com/kopeio/testr/go/pkg/model"
	"encoding/json"
)

func main() {
	var token string
	flag.StringVar(&token, "token", "", "Authentication token")

	var serverUrl string
	flag.StringVar(&serverUrl, "server", "", "Server URL")

	var job string
	flag.StringVar(&job, "job", "", "Job ID")

	var revision string
	flag.StringVar(&revision, "revision", "", "Revision ID")

	var timestamp int64
	flag.Int64Var(&timestamp, "timestamp", 0, "Timestamp / sequence ID")

	flag.Set("alsologtostderr", "true")
	flag.Parse()

	if serverUrl == "" {
		fmt.Fprintf(os.Stderr, "server must be specified\n")
		os.Exit(1)
	}

	if job == "" {
		fmt.Fprintf(os.Stderr, "job must be specified\n")
		os.Exit(1)
	}

	if revision == "" {
		fmt.Fprintf(os.Stderr, "revision must be specified\n")
		os.Exit(1)
	}

	if timestamp == 0 {
		fmt.Fprintf(os.Stderr, "timestamp must be specified\n")
		os.Exit(1)
	}

	// TODO: We _could_ get this from the token
	executionKey := &model.ExecutionKey{
		Job: job,
		Revision: revision,
		Timestamp: timestamp,
	}

	client := executor.NewClient(serverUrl, executionKey, token)

	plan, err := client.GetPlan()
	if err != nil {
		fmt.Fprintf(os.Stderr, "error fetching plan: %v\n", err)
		os.Exit(1)
	}

	mainStep := &model.Step{}
	err = json.Unmarshal(plan, mainStep)
	if err != nil {
		fmt.Fprintf(os.Stderr, "invalid JSON in plan: %v\n", err)
		os.Exit(1)
	}

	execution, err := executor.NewExecution(executionKey)
	if err != nil {
		fmt.Fprintf(os.Stderr, "error building execution: %v", err)
		os.Exit(1)
	}

	stepExecutor := executor.NewStepExecutor(execution)

	success, err := stepExecutor.Execute(mainStep)
	if err != nil {
		fmt.Fprintf(os.Stderr, "unexpected error executing plan: %v\n", err)
		os.Exit(1)
	}
	if !success {
		fmt.Fprintf(os.Stderr, "execution failed");
	}

	execution.MarkDone(err)

	glog.Info("Uploading artifacts")
	artifacts := execution.Artifacts()
	uploadFailed := false
	for _, artifact := range artifacts {
		n, err := client.UploadArtifact(artifact)
		if err != nil {
			glog.Warningf("Failed to upload artifact %q: %v", artifact.RelativePath, err)
			uploadFailed = true
		}
		artifact.UploadedSize = n
	}

	if uploadFailed {
		glog.Warning("Failed to upload all artifacts; exiting with failure")
		os.Exit(1)
	}

	glog.Info("Uploading build result")
	result := execution.BuildResult()
	err = client.UploadResult(result)
	if err != nil {
		glog.Warningf("Failed to upload build result; exiting with failure: %v", err)
		os.Exit(1)
	}

	if err == nil {
		fmt.Printf("SUCCESS\n")
		os.Exit(0)
	} else {
		fmt.Printf("FAILURE: %v\n", err)
		os.Exit(0)
	}
}
