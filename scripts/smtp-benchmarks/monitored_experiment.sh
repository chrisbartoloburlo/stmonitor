#!/bin/bash
iterations=$1
experiments=$2
type=$3
log=$4

run=1
wd=`pwd`

while [ "$experiments" -ne 0 ] ; do
  echo "Running SMTP $type Monitored Experiment $run with $iterations emails..."

  if [ "$type" = "smtp-python" ]; then
    port=1025
  else
    port=25
  fi

  if [ "$log" = "false" ]; then
    /usr/bin/time --format="%P,%M,%K" java -cp ./examples/target/scala-2.12/examples-assembly-0.0.3.jar examples.smtp.MonitoredClient $wd/scripts/smtp-benchmarks/$type/results/monitored/ ${iterations} ${run} ${port} 2>> $wd/scripts/smtp-benchmarks/$type/results/monitored/${iterations}_cpu_mem_run.txt ${log}
  elif [ "$log" = "true" ]; then 
    /usr/bin/time --format="%P,%M,%K" java -cp ./examples/target/scala-2.12/examples-assembly-0.0.3.jar examples.smtp.MonitoredClient $wd/scripts/smtp-benchmarks/$type/results/monitored_logging/ ${iterations} ${run} ${port} 2>> $wd/scripts/smtp-benchmarks/$type/results/monitored_logging/${iterations}_cpu_mem_run.txt ${log}
  else
    echo "Error in logging flag"
    exit
  fi

  sleep 1

  echo "Finished SMTP $type Monitored Experiment $run.\n"

  run=$((run+1))
  experiments=$((experiments-1))
done