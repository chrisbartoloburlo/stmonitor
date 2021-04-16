#!/bin/bash
run=1
requests=$1
rampup=$2
experiments=$3
wd=`pwd`

while [ "$experiments" -ne 0 ] ; do
  echo "Running HTTP Monitored Experiment ${run} with ${requests} requests and ${rampup} seconds rampup time..."

  screen -dm -S stmonbench bash -c "/usr/bin/time --format=\"%P,%M,%K\" java -cp ./examples/target/scala-2.12/examples-assembly-0.0.3.jar examples.http.ServerWithMonitor 2>> $wd/scripts/http-benchmarks/results/monitored/${requests}_cpu_mem.txt"

  sleep 1

  startTime=$(($(date +%s%N)/1000000))

  jmeter -n -t $wd/scripts/http-benchmarks/test-plan.jmx -Jpath=$wd/scripts/http-benchmarks/results/monitored/${requests}_resp_time_run${run}.csv -Jrequests=${requests} -Jrampup=${rampup}

  endTime=$(($(date +%s%N)/1000000))
  executionTime=$((endTime-startTime)) #in ms
  echo ${run},${executionTime} >> $wd/scripts/http-benchmarks/results/monitored/${requests}_exec_times.txt

  screen -S stmonbench -p 0 -X stuff "^C"
  echo "Finished HTTP Monitored Experiment ${run} with ${requests} requests.\n"
  sleep 1

  run=$((run+1))
  experiments=$((experiments-1))
done
