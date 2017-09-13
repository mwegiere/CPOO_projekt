#!/bin/bash
echo "Preparing files"
SRC=$(find ./src -name "*.java")
LIBS=$(find ./lib -name "*.jar")
echo $SRC
mkdir -p ./bin

echo "Building application"
javac -encoding ISO-8859-1 -d ./bin -sourcepath ./src -cp $LIBS $SRC

echo "-------------------------------------------------------------------------------------"
echo "Running application:"
java -Djava.library.path=./lib -Dfile.encoding=UTF-8 -classpath ./bin:./lib/opencv-320.jar pl.cpoo.AplikacjaCPOO
