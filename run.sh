#!/bin/bash
export JAVA_HOME=$(dirname $(dirname $(readlink -f $(which javac 2>/dev/null || echo "/usr/lib/jvm/java-25-openjdk-amd64"))))
mvn compile exec:java -Dexec.mainClass="Main" -q