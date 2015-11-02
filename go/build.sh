#!/bin/bash

set -ex

export GOPATH=`pwd`/gopath/
mkdir -p ${GOPATH}/src/github.com/kopeio/
cd ${GOPATH}/src/github.com/kopeio/
ln -sf ../../../../../../java testr
go get github.com/golang/glog
go get github.com/golang/protobuf/proto
go install github.com/kopeio/testr/go/cmd/testr-executor
