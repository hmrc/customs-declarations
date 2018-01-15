#!/usr/bin/env bash

sbt clean scalastyle coverage test it:test coverageReport
