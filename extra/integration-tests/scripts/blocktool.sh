#!/bin/sh
MAVEN_OPTS=-Xmx4096m mvn exec:java -Dexec.mainClass=it.nibbles.javacoin.BlockTool -Dexec.args="$*"
