#!/bin/sh
export JAVA_HOME=$JDK_8_HOME

gradle build
export JAVA_TOOL_OPTIONS=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005
$JAVA_HOME/bin/java -jar $PATH_TO_JAVA_INVOKE_JAR --server