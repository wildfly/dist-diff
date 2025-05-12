#!/usr/bin/env bash
pushd .
DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
cd $DIR

mkdir -p a b
cd src/a
javac MyClass.java
jar cf ../../a/thejar.jar MyClass.class
rm MyClass.class
cd -
cd src/b
javac MyClass.java
jar cf ../../b/thejar.jar MyClass.class
rm MyClass.class

popd

