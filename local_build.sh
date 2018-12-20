#!/usr/bin/env bash

mvn clean install -DskipTests
mvn dependency:copy-dependencies


