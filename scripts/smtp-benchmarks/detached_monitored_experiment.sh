#!/bin/bash
iterations=$1
experiments=$2
type=$3

run=1
wd=`pwd`

while [ "$experiments" -ne 0 ] ; do
  echo "Running SMTP $type Detached Monitored Experiment $run with $iterations emails..."

  if [ "$type" = "smtp-python" ]; then
    port=1025
  else
    port=25
  fi

  screen -S stmonbench -dm bash -c "/usr/bin/time --format=\"%P,%M,%K\" java -cp ./examples/target/scala-2.12/examples-assembly-0.0.3.jar examples.smtp.MonWrapper $port 1026 2>> $wd/scripts/smtp-benchmarks/$type/results/detached_monitored/${iterations}_cpu_mem_run.txt"

  sleep 1

  java -cp ./examples/target/scala-2.12/examples-assembly-0.0.3.jar examples.smtp.Client $wd/scripts/smtp-benchmarks/$type/results/detached_monitored/ ${iterations} ${run} 1026

  screen -S stmonbench -p 0 -X stuff "^C" > /dev/null 2>&1
  echo "Finished SMTP $type Detached Monitored Experiment $run.\n"

  run=$((run+1))
  experiments=$((experiments-1))
done