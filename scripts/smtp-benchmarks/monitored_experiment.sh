#!/bin/bash
iterations=$1
experiments=$2

run=1
wd=`pwd`

while [ "$experiments" -ne 0 ] ; do
  echo Running SMTP Monitored Experiment ${run} with ${iterations} emails

  /usr/bin/time --format="%P,%M,%K" java -cp ./examples/target/scala-2.12/examples-assembly-0.0.3.jar examples.smtp.MonitoredClient $wd/scripts/smtp-benchmarks/results/monitored/ ${iterations} ${run} 1025 2>> $wd/scripts/smtp-benchmarks/results/monitored/${iterations}_cpu_mem_run.txt

  sleep 1

  echo Finished SMTP Monitored Experiment ${run}

  run=$((run+1))
  experiments=$((experiments-1))
done