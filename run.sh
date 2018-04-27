#!/usr/bin/env bash
java -cp build/libs/distributed-algorithms.jar:lib/log4j-core-2.9.1.jar:lib/log4j-api-2.9.1.jar edu.membership.RemoteNode $@
