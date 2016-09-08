#!/bin/bash

# Runs the appropriate Maven build commands to produce a Java-executable JAR (i.e., 'java -jar crux.jar [OPTIONS]')
# then runs a second process to make the JAR file a directly OS-executable JAR (i.e., 'crux.jar [OPTIONS]')

if [ -e pom.xml ]; then
  echo "Building library..."
  mvn clean package assembly:single -Dmaven.install.skip=true
  status=$?
  if [ status == 0 ]; then
    bin/make-executable-jar.sh target/crux-*.jar
  else
  exit $status #error code of mvn command
else
  echo "ERROR: must be run from the directory with pom.xml"
  exit 1;
fi
