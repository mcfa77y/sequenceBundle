#!/bin/sh

# prefuse build script

echo "building prefuse..."

JAVA_HOME=/cygdrive/c/Program\ Files/Java/jdk1.7.0_45

LOCALCLASSPATH=$JAVA_HOME/lib/tools.jar:./lib/ant.jar
ANT_HOME=./lib

echo ... using classpath $LOCALCLASSPATH
echo

echo Starting Ant...
echo

"$JAVA_HOME/bin/java" -Dant.home=$ANT_HOME -classpath $LOCALCLASSPATH org.apache.tools.ant.Main $*
