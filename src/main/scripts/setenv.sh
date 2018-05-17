#!/bin/bash

JSVC_EXECUTABLE="$( which jsvc )"
JSVC_PID_FILE=/tmp/bpclinrec.pid

if [ -z "$JSVC_USER" ]; then
	JSVC_USER="$USER"
fi

DIST_DIR="/Users/tchechem/wslirmm/BPClinicalRecognizer"


JAVA_EXEC="$( which java )"
JAVA_CLASSPATH="/Users/tchechem/wslirmm/BPClinicalRecognizer/target/clinicalrecognizer.jar"
JAVA_MAIN_CLASS="org.sifrproject.server.ClinicalRecognizerServer"

if [ -z "$JAVA_HOME" ]; then
	export JAVA_HOME="$( $JAVA_EXEC -cp "$JAVA_CLASSPATH" -server \
		org.sifrproject.server.GetProperty java.home )"
fi

export JSVC_EXECUTABLE JSVC_PID_FIL JSVC_USER DIST_DIR CONF_DIR JAVA_EXEC \
	JAVA_CLASSPATH JAVA_MAIN_CLASS JAVA_HOME