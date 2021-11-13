#!/bin/bash
iterations=$1
experiments=$2
type=$3

run=1
wd=`pwd`

while [ "$experiments" -ne 0 ] ; do
  echo "Running GAME Control Experiment $run with $iterations guesses..."

  port=1330

  screen -dm -S pstmonbench bash -c "/usr/bin/time --format=\"%P,%M,%K\" java -cp ./examples/target/scala-2.12/examples-assembly-0.0.3.jar examples.game.Server 2>> $wd/scripts/game-benchmarks/results/control/${iterations}_cpu_mem_run.txt"

  sleep 1

  python3 $wd/scripts/game-benchmarks/game-client-benchmark.py ${port} 0.2 ${iterations} $wd/scripts/game-benchmarks/results/control/resp_time_run${run}.csv

  echo "Finished GAME Control Experiment $run.\n"

  run=$((run+1))
  experiments=$((experiments-1))
done