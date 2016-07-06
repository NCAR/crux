#!/bin/bash

# Make a Java-executable JAR (runnable using 'java -jar xyz.jar [OPTIONS]) and makes it fully executable in an OS sense
# (xyz.jar [OPTIONS]).  This is accomplished by the fact that ZIP files can have executable content prepended to them

if [ -z "$1" ]; then
  echo "Usage:        make-executable-jar.sh [file]"
  exit 1
fi

tmpfile=$1.realexec.tmp
cp $(dirname $0)/executable-jar-prepend.txt $tmpfile
cat $1 >> $tmpfile
chmod +x $tmpfile
mv $tmpfile $1

echo "$1 is now executable"
