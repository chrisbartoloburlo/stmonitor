#!/bin/bash
screen -S smtpserver -dm python3 -m smtpd -c DebuggingServer -n localhost:1025

experiments=$1
increments=20

iterations=100

wd=`pwd`
while [ "$increments" -ne 0 ] ; do

  sh $wd/scripts/smtp-benchmarks/control_experiment.sh $iterations $experiments
  sh $wd/scripts/smtp-benchmarks/monitored_experiment.sh $iterations $experiments
  sh $wd/scripts/smtp-benchmarks/detached_monitored_experiment.sh $iterations $experiments

  iterations=$((iterations+100))
  increments=$((increments-1))
done

screen -S smtpserver -X quit

python3 $wd/scripts/smtp-benchmarks/smtp-plots.py 5