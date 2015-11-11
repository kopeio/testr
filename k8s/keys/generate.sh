#!/bin/bash

ID=$1

openssl genrsa -out ${ID}.pem 2048
openssl rsa -in ${ID}.pem -pubout -out ${ID}.pubkey

