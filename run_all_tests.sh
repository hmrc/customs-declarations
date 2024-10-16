#!/usr/bin/env bash

sbt clean coverage test it:test coverageReport dependencyUpdate
