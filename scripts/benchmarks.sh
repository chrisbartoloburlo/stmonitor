#!/bin/bash
# Usage: sh benchmarks.sh <number of experiments> <list of benchmarks>

wd=`pwd`

python3 -c "import csv"
python3 -c "import matplotlib.pyplot"
python3 -c "import statistics"
python3 -c "import sys"

sbt examples/assembly

for n in $(seq 2 $#); do
  if [ "$2" = "smtp-python" ]; then
    echo "Running SMTP benchmarks with $1 iterations per experiment"
    sh $wd/scripts/smtp-benchmarks/smtp-experiments.sh $1
  elif [ "$2" == "pingpong" ]; then
    echo "Running Ping Pong benchmarks with $1 iterations per experiment"
  else
    echo "*** Unknown benchmark: $2 *** Available benchmarks: smtp-python pingpong"
  fi
  shift
done