#!/bin/bash
# Usage: sh benchmarks.sh <number of experiments> <list of benchmarks>

wd=`pwd`

python3 -c "import matplotlib.pyplot"
if [ "$?" = "1" ]; then
  echo "Python module matplotlib not found, cannot run benchmarks: terminating"
  exit
fi

#sbt examples/assembly

for n in $(seq 2 $#); do
  if [ "$2" = "smtp-python" ]; then
    echo "Running SMTP benchmarks with $1 iterations per experiment"
    sh $wd/scripts/smtp-benchmarks/smtp_experiments.sh $1
  elif [ "$2" = "pingpong" ]; then
    jmeter -v > /dev/null 2>&1
    if [ "$?" = "1" ]; then
      echo "Jmeter not found, skipping ping pong benchmarks"
    else
      echo "Running Ping Pong benchmarks with $1 iterations per experiment"
      sh $wd/scripts/pingpong-benchmarks/pingpong_experiments.sh $1
    fi
  else
    echo "*** Unknown benchmark: $2 *** Available benchmarks: smtp-python pingpong"
  fi
  shift
done