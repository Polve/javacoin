#!/bin/sh
mvn exec:java -Dexec.mainClass=it.nibbles.bitcoin.BlockTool -Dexec.args="$*"
