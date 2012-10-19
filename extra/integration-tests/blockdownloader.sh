#!/bin/bash
mvn exec:java -Dexec.mainClass=it.nibbles.bitcoin.ChainDownloader -Dexec.args="$*"
