all: run

schema:
	curl http://127.0.0.1:8083/graphql/schema > data/schema.json

install:
	npm install

run: schema
	npm start
