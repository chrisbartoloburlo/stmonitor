#!/bin/bash
experiments=$1
limited=$2
increments=10

requests=200
rampup=4

wd=`pwd`

echo "Removing contents of pingpong-benchmarks/results...\n"
rm -r $wd/scripts/pingpong-benchmarks/results/control/* > /dev/null 2>&1
rm -r $wd/scripts/pingpong-benchmarks/results/monitored/* > /dev/null 2>&1
rm -r $wd/scripts/pingpong-benchmarks/results/detached_monitored/* > /dev/null 2>&1

if [ "$limited" = "true" ]; then
  sh $wd/scripts/pingpong-benchmarks/control_experiment.sh 200 4 $experiments
  sh $wd/scripts/pingpong-benchmarks/monitored_experiment.sh 200 4 $experiments
  sh $wd/scripts/pingpong-benchmarks/detached_monitored_experiment.sh 200 4 $experiments

  sh $wd/scripts/pingpong-benchmarks/control_experiment.sh 600 12 $experiments
  sh $wd/scripts/pingpong-benchmarks/monitored_experiment.sh 600 12 $experiments
  sh $wd/scripts/pingpong-benchmarks/detached_monitored_experiment.sh 600 12 $experiments

  python3 $wd/scripts/pingpong-benchmarks/pingpong-plots.py $wd $experiments 1
  exit
fi

while [ "$increments" -ne 0 ] ; do

  sh $wd/scripts/pingpong-benchmarks/control_experiment.sh $requests $rampup $experiments
  sh $wd/scripts/pingpong-benchmarks/monitored_experiment.sh $requests $rampup $experiments
  sh $wd/scripts/pingpong-benchmarks/detached_monitored_experiment.sh $requests $rampup $experiments

  requests=$((requests+200))
  rampup=$((rampup+4))
  increments=$((increments-1))
done

python3 $wd/scripts/pingpong-benchmarks/pingpong-plots.py $wd $experiments 0