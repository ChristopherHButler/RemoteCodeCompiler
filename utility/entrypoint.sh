#!/usr/bin/env bash
mv main.java test.c
javac test.c
ret=$?
if [ $ret -ne 0 ]
then
  exit 2
fi
ulimit -s 100
timeout --signal=SIGTERM 10 java t < input.txt
exit $?
