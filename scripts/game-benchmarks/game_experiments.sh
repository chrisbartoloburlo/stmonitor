#!/bin/bash

experiments=$1
increments=10

iterations=200

wd=`pwd`

echo "Removing contents of game-benchmarks/results...\n"
rm -r $wd/scripts/game-benchmarks/results/control/* > /dev/null 2>&1
rm -r $wd/scripts/game-benchmarks/results/monitored/* > /dev/null 2>&1
rm -r $wd/scripts/game-benchmarks/results/detached_monitored/* > /dev/null 2>&1

while [ "$increments" -ne 0 ] ; do

  sh $wd/scripts/game-benchmarks/control_experiment.sh $iterations $experiments
  sh $wd/scripts/game-benchmarks/monitored_experiment.sh $iterations $experiments false
#  sh $wd/scripts/game-benchmarks/monitored_experiment.sh $iterations $experiments true
  sh $wd/scripts/game-benchmarks/detached_monitored_experiment.sh $iterations $experiments false
#  sh $wd/scripts/game-benchmarks/detached_monitored_experiment.sh $iterations $experiments true

  iterations=$((iterations+200))
  increments=$((increments-1))
done

#if [ "$type" = "smtp-python" ]; then
#  screen -S smtpserver -X quit > /dev/null 2>&1
#fi

python3 $wd/scripts/game-benchmarks/game-plots.py $wd $experiments 0