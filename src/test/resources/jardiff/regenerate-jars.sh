#!/usr/bin/env bash
pushd .
DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
cd $DIR

mkdir -p a b
cd src/a
javac *.java
jar cvf ../../a/thejar.jar *.class
rm *.class
cd -
cd src/b
javac *.java
jar cvf ../../b/thejar.jar *.class
rm *.class
cd -

popd
