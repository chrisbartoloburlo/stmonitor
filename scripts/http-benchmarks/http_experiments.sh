#!/bin/bash
experiments=$1
increments=20

requests=100
rampup=2

wd=`pwd`

echo "Removing contents of results directory"
rm -r $wd/scripts/http-benchmarks/results/control/* > /dev/null 2>&1
rm -r $wd/scripts/http-benchmarks/results/monitored/* > /dev/null 2>&1

while [ "$increments" -ne 0 ] ; do

  sh $wd/scripts/http-benchmarks/control_experiment.sh $requests $rampup $experiments
  sh $wd/scripts/http-benchmarks/monitored_experiment.sh $requests $rampup $experiments

  requests=$((requests+100))
  rampup=$((rampup+2))
  increments=$((increments-1))
done

python3 $wd/scripts/http-benchmarks/http-plots.py $wd $experiments