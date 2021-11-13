#!/bin/bash
iterations=$1
experiments=$2
log=$3

run=1
wd=`pwd`

while [ "$experiments" -ne 0 ] ; do

  port=1330

  screen -S gameserver -dm bash -c "java -cp ./examples/target/scala-2.12/examples-assembly-0.0.3.jar examples.game.Server"

  sleep 1

  if [ "$log" = "false" ]; then
    echo "Running GAME Detached Monitored without logging Experiment $run with $iterations emails..."
    screen -S pstmonbench -dm bash -c "/usr/bin/time --format=\"%P,%M,%K\" java -cp ./examples/target/scala-2.12/examples-assembly-0.0.3.jar examples.game.MonWrapper 1330 1331 0.6745 2>> $wd/scripts/game-benchmarks/results/detached_monitored/${iterations}_cpu_mem_run.txt"
  elif [ "$log" = "true" ]; then 
    echo "Running GAME Detached Monitored with logging Experiment $run with $iterations emails..."
    screen -S pstmonbench -dm bash -c "/usr/bin/time --format=\"%P,%M,%K\" java -cp ./examples/target/scala-2.12/examples-assembly-0.0.3.jar examples.game.MonWrapper 1330 1331 0.6745 2>> $wd/scripts/game-benchmarks/results/detached_monitored_logging/${iterations}_cpu_mem_run.txt"
  else
    echo "Error in logging flag"
    exit
  fi

  sleep 1

  if [ "$log" = "false" ]; then
    python3 $wd/scripts/game-benchmarks/game-client-benchmark.py ${port} 0.2 ${iterations} $wd/scripts/game-benchmarks/results/detached_monitored/resp_time_run${run}.csv
  else
    python3 $wd/scripts/game-benchmarks/game-client-benchmark.py ${port} 0.2 ${iterations} $wd/scripts/game-benchmarks/results/detached_monitored_logging/resp_time_run${run}.csv
  fi

  screen -S pstmonbench -p 0 -X stuff "^C" > /dev/null 2>&1
  screen -S gameserver -p 0 -X stuff "^C" > /dev/null 2>&1
  echo "Finished GAME Detached Monitored Experiment $run.\n"

  run=$((run+1))
  experiments=$((experiments-1))
done