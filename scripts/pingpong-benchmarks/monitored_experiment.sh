#!/bin/bash
run=1
requests=$1
rampup=$2
experiments=$3

while [ "$experiments" -ne 0 ] ; do
  echo Running Separate Monitored Experiment ${run} with ${requests} requests and ${rampup} seconds rampup time.

  screen -dm -S stmonbench bash -c "/usr/bin/time --format=\"%P,%M,%K\" java -cp ../../examples-assembly-0.0.3.jar examples.pingpong.MonWrapper 2>> results/separate_monitored/${requests}_cpu_mem.txt"

  sleep 1

  startTime=$(($(date +%s%N)/1000000))

  jmeter -n -t test-plan.jmx -Jpath=/root/documents/experiments/pingpong-separate/results/separate_monitored/${requests}_resp_time_run${run}.csv -Jrequests=${requests} -Jrampup=${rampup}

  endTime=$(($(date +%s%N)/1000000))
  executionTime=$((endTime-startTime)) #in ms
  echo ${run},${executionTime} >> results/separate_monitored/${requests}_exec_times.txt

  screen -S stmonbench -p 0 -X stuff "^C"
  echo End of Separate Monitored Experiment ${run} with ${requests} requests.
  sleep 1

  run=$((run+1))
  experiments=$((experiments-1))
done
