#!/usr/bin/env bash
pushd .
DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
cd $DIR

mkdir -p a b
cd src/a
javac *.java
jar cf ../../a/thejar.jar *.class
rm *.class
cd -
cd src/b
javac *.java
jar cf ../../b/thejar.jar *.class
rm *.class

popd
