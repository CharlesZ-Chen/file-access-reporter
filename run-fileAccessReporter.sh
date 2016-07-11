#!/bin/bash

WORKING_DIR=$(pwd)
ROOT=$(cd $(dirname $0)/.. && pwd)

DATAFLOW="$ROOT"/dataflow-framework/dataflow
JAVACUTIL="$ROOT"/dataflow-framework/javacutil
JSR308_LANGTOOL="$ROOT"/jsr308-langtools

DATAFLOW_JAR="$DATAFLOW"/dist/dataflow.jar
JAVACUTIL_JAR="$JAVACUTIL"/dist/javacutil.jar
JAVAC_JAR="$JSR308_LANGTOOL"/dist/lib/javac.jar

FILE_ACCESS_REPORTER="$ROOT"/file-access-reporter
BIN="$FILE_ACCESS_REPORTER"/bin

JARS="$DATAFLOW_JAR":"$JAVACUTIL_JAR":"$JAVAC_JAR"

##TODO: currently the output_Dir "analysis-report" is HARD CODE in java source code
# FIleAccessProcessor#FileAccessProcessor(
# need to extract out as a cmd option
OUTPUT_DIR="$WORKING_DIR"/analysis-report
if [ ! -d "$OUTPUT_DIR" ] ; then
    mkdir "$OUTPUT_DIR"
fi

javac -cp "$BIN":"$JARS" -processor playground.FileAccessProcessor "$@"
