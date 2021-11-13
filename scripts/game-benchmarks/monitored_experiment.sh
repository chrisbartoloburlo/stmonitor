#!/bin/bash
iterations=$1
experiments=$2
log=$3

run=1
wd=`pwd`

while [ "$experiments" -ne 0 ] ; do
  port=1330

  if [ "$log" = "false" ]; then
    echo "Running GAME Monitored without logging Experiment $run with $iterations guesses..."
    screen -dm -S pstmonbench bash -c "/usr/bin/time --format=\"%P,%M,%K\" java -cp ./examples/target/scala-2.12/examples-assembly-0.0.3.jar examples.game.MonitoredServer ${log} 2>> $wd/scripts/game-benchmarks/results/monitored/${iterations}_cpu_mem_run.txt"
  elif [ "$log" = "true" ]; then
    echo "Running GAME Monitored with logging Experiment $run with $iterations guesses..."
    screen -dm -S pstmonbench bash -c "/usr/bin/time --format=\"%P,%M,%K\" java -cp ./examples/target/scala-2.12/examples-assembly-0.0.3.jar examples.game.MonitoredServer ${log} 2>> $wd/scripts/game-benchmarks/results/monitored_logging/${iterations}_cpu_mem_run.txt"
  else
    echo "Error in logging flag"
    exit
  fi

  sleep 1

  python3 $wd/scripts/game-benchmarks/game-client-benchmark.py ${port} 0.2 ${iterations} $wd/scripts/game-benchmarks/results/monitored/resp_time_run${run}.csv

  echo "Finished GAME Monitored Experiment $run.\n"

  run=$((run+1))
  experiments=$((experiments-1))
done