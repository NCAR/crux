#!/bin/bash

# Builds a ZIP release file containing Crux JARs for deployment on GitHub or elsewhere

if [ -z "$1" ]; then
  echo "Usage:    make-release.sh [Crux release version]"
  exit 1
fi

version=$1

if [ -e pom.xml ]; then
  echo "Building release..."
  bin/build-crux.sh
  status=$?
  zipFile=crux-$version.zip
  if [ $status -eq 0 ]; then
    cd target
    zip $zipFile crux-*.jar
    cd ..
    echo "Created release file: target/$zipFile"
  fi
  
  exit $status #error code of build command
else
  echo "ERROR: must be run from the directory with pom.xml"
  exit 1;
fi
