#!/bin/bash
# Usage: sh benchmarks.sh <number of experiments> <list of benchmarks>

wd=`pwd`

python3 -c "import matplotlib.pyplot"
if [ "$?" = "1" ]; then
  echo "Python module matplotlib not found, cannot run benchmarks: terminating"
  exit
fi

sbt examples/assembly

if [ "$1" = "kickthetires" ]; then
  jmeter -v > /dev/null 2>&1
  if [ "$?" = "1" ]; then
    echo "Jmeter not found, cannot run benchmarks: terminating"
  else
    echo "Running a limited set of the experiments for the SMTP, Ping Pong and HTTP benchmarks for the Kick-the-Tires phase"
    sh $wd/scripts/smtp-benchmarks/smtp_experiments.sh 2 true smtp-python
    sh $wd/scripts/pingpong-benchmarks/pingpong_experiments.sh 2 true
    sh $wd/scripts/http-benchmarks/http_experiments.sh 2 true
    echo "The experiments completed successfully if the respective directories contain the generated plots"
    exit
  fi
fi

for n in $(seq 2 $#); do
  if [ "$2" = "smtp-python" ]; then
    echo "Running SMTP python benchmarks with $1 iterations per experiment"
    sh $wd/scripts/smtp-benchmarks/smtp_experiments.sh $1 false smtp-python
  elif [ "$2" = "pingpong" ]; then
    jmeter -v > /dev/null 2>&1
    if [ "$?" = "1" ]; then
      echo "Jmeter not found, skipping ping pong benchmarks"
    else
      echo "Running Ping Pong benchmarks with $1 iterations per experiment"
      sh $wd/scripts/pingpong-benchmarks/pingpong_experiments.sh $1 false
    fi
  elif [ "$2" = "http" ]; then
    jmeter -v > /dev/null 2>&1
    if [ "$?" = "1" ]; then
      echo "Jmeter not found, skipping HTTP benchmarks"
    else
      echo "Running HTTP benchmarks with $1 iterations per experiment"
      sh $wd/scripts/http-benchmarks/http_experiments.sh $1 false
    fi
  elif [ "$2" = "smtp-postfix" ]; then
    echo "Running SMTP postfix benchmarks with $1 iterations per experiment"
    sh $wd/scripts/smtp-benchmarks/smtp_experiments.sh $1 false smtp-postfix
  else
    echo "*** Unknown benchmark: $2 *** Available benchmarks: smtp-python pingpong http smtp-postfix"
  fi
  shift
done