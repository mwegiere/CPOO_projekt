#!/bin/bash
echo "Preparing files"
SRC=$(find ./src -name "*.java")
OPENCV=$(find ./lib -name "*.jar" | head -n 1)
echo "Found opencv lib: $OPENCV"
echo "Sources: $SRC"
mkdir -p ./bin

echo "Building application"
javac -encoding ISO-8859-1 -d ./bin -sourcepath ./src -cp $OPENCV $SRC

echo "-------------------------------------------------------------------------------------"
echo "Running application:"
java -Djava.library.path=./lib -Dfile.encoding=UTF-8 -classpath ./bin:$OPENCV pl.cpoo.AplikacjaCPOO
