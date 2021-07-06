#!/bin/bash
run=1
requests=$1
rampup=$2
experiments=$3
wd=`pwd`

while [ "$experiments" -ne 0 ] ; do
  echo "Running Ping Pong Detached Monitored Experiment ${run} with ${requests} requests and ${rampup} seconds rampup time..."

  screen -dm -S serverbench bash -c "java -cp ./examples/target/scala-2.12/examples-assembly-0.0.3.jar examples.pingpong.ServerWrapper 8081"

  sleep 1

  screen -dm -S stmonbench bash -c "/usr/bin/time --format=\"%P,%M,%K\" java -cp ./examples/target/scala-2.12/examples-assembly-0.0.3.jar examples.pingpong.MonWrapper 8080 127.0.0.1 8081 2>> $wd/scripts/pingpong-benchmarks/results/detached_monitored/${requests}_cpu_mem.txt"

  sleep 1

  startTime=$(($(date +%s%N)/1000000))

  jmeter -n -t $wd/scripts/pingpong-benchmarks/test-plan.jmx -Jpath=$wd/scripts/pingpong-benchmarks/results/detached_monitored/${requests}_resp_time_run${run}.csv -Jrequests=${requests} -Jrampup=${rampup}

  endTime=$(($(date +%s%N)/1000000))
  executionTime=$((endTime-startTime)) #in ms
  echo ${run},${executionTime} >> $wd/scripts/pingpong-benchmarks/results/detached_monitored/${requests}_exec_times.txt

  screen -S stmonbench -p 0 -X stuff "^C"
  screen -S serverbench -p 0 -X stuff "^C"
  echo "Finished Ping Pong Detached Monitored Experiment ${run} with ${requests} requests.\n"
  sleep 2

  run=$((run+1))
  experiments=$((experiments-1))
done