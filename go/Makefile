all: install

protobuf:
	protoc --go_out=. pkg/model/*.proto

install: protobuf
	./build.sh
