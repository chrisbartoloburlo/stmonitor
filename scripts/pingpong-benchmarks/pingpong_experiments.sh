#!/bin/bash
experiments=$1
increments=20

requests=100
rampup=2

wd=`pwd`

echo "Removing contents of results directory"
rm -r $wd/scripts/pingpong-benchmarks/results/control/*
rm -r $wd/scripts/pingpong-benchmarks/results/monitored/*
rm -r $wd/scripts/pingpong-benchmarks/results/detached_monitored/*

while [ "$increments" -ne 0 ] ; do

  sh $wd/scripts/pingpong-benchmarks/control_experiment.sh $requests $rampup $experiments
  sh $wd/scripts/pingpong-benchmarks/monitored_experiment.sh $requests $rampup $experiments
  sh $wd/scripts/pingpong-benchmarks/detached_monitored_experiment.sh $requests $rampup $experiments

  requests=$((requests+100))
  rampup=$((rampup+2))
  increments=$((increments-1))
done

python3 $wd/scripts/pingpong-benchmarks/pingpong-plots.py $wd $experiments