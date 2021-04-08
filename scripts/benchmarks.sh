#!/bin/bash
# Usage: sh benchmarks.sh <number of experiments> <list of benchmarks>

wd=`pwd`

python -c "import csv"
python -c "import matplotlib.pyplot"
python -c "import statistics"
python -c "import sys"

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