#!/bin/bash

type=$3

if [ "$type" = "smtp-python" ]; then
  screen -S smtpserver -dm python3 -m smtpd -c DebuggingServer -n 127.0.0.1:1025
fi

experiments=$1
limited=$2
increments=10

iterations=200

wd=`pwd`

echo "Removing contents of $type/results...\n"
rm -r $wd/scripts/smtp-benchmarks/$type/results/control/* > /dev/null 2>&1
rm -r $wd/scripts/smtp-benchmarks/$type/results/monitored/* > /dev/null 2>&1
rm -r $wd/scripts/smtp-benchmarks/$type/results/detached_monitored/* > /dev/null 2>&1

if [ "$limited" = "true" ]; then
  sh $wd/scripts/smtp-benchmarks/control_experiment.sh 200 $experiments $type
  sh $wd/scripts/smtp-benchmarks/monitored_experiment.sh 200 $experiments $type
  sh $wd/scripts/smtp-benchmarks/detached_monitored_experiment.sh 200 $experiments $type

  sh $wd/scripts/smtp-benchmarks/control_experiment.sh 600 $experiments $type
  sh $wd/scripts/smtp-benchmarks/monitored_experiment.sh 600 $experiments $type
  sh $wd/scripts/smtp-benchmarks/detached_monitored_experiment.sh 600 $experiments $type

  sh $wd/scripts/smtp-benchmarks/control_experiment.sh 1000 $experiments $type
  sh $wd/scripts/smtp-benchmarks/monitored_experiment.sh 1000 $experiments $type
  sh $wd/scripts/smtp-benchmarks/detached_monitored_experiment.sh 1000 $experiments $type

  screen -S smtpserver -X quit > /dev/null 2>&1

  python3 $wd/scripts/smtp-benchmarks/smtp-plots.py $wd $experiments 1 $type
  exit
fi

while [ "$increments" -ne 0 ] ; do

  sh $wd/scripts/smtp-benchmarks/control_experiment.sh $iterations $experiments $type
  sh $wd/scripts/smtp-benchmarks/monitored_experiment.sh $iterations $experiments $type
  sh $wd/scripts/smtp-benchmarks/detached_monitored_experiment.sh $iterations $experiments $type

  iterations=$((iterations+200))
  increments=$((increments-1))
done

if [ "$type" = "smtp-python" ]; then
  screen -S smtpserver -X quit > /dev/null 2>&1
fi

python3 $wd/scripts/smtp-benchmarks/smtp-plots.py $wd $experiments 0 $type