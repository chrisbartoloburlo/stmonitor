#!/bin/bash
# Usage: sh benchmarks.sh <number of repetitions per experiment> <list of benchmarks>

iterations=$1
wd=`pwd`

python3 -c "import matplotlib.pyplot"
if [ "$?" = "1" ]; then
  echo "Python module matplotlib not found, cannot run benchmarks: terminating"
  exit
fi

echo "Creating a fat JAR of stmonitor/examples/..."
sbt examples/assembly
echo "Fat JAR created.\n"

experiments=""

for n in $(seq 2 $#); do
  if [ "$2" = "smtp" ]; then
    echo "Running SMTP benchmarks with $iterations repetitions per experiment..."
    sh $wd/scripts/smtp-benchmarks/smtp_experiments.sh $iterations false smtp-postfix
    echo "SMTP benchmark finished.\n"
    experiments="$experiments smtp-postfix"
  else
    echo "*** Unknown benchmark: $2 *** Available benchmarks: smtp\n"
  fi
  shift
done

for experiment in $experiments; do
  if [ "$experiment" = "smtp" ]; then
    echo "SMTP benchmark plots can be found in the directory $wd/scripts/smtp-benchmarks/smtp-postfix/plots"
  else
    echo "*** Benchmarks not executed for: $experiment ***"
  fi
done