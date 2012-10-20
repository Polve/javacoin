#!/bin/sh
l4jconfig=scripts/log4j-prodnet.properties
if [ ! -e $l4jconfig ] ; then
  echo "log4jconfig file not found: $l4jconfig"
  exit 1
fi
OPTS="-Dlog4j.configuration=$l4jconfig"
MAVEN_OPTS="$OPTS" mvn exec:java -Dexec.mainClass=it.nibbles.bitcoin.ChainDownloader -Dexec.args="$*"
