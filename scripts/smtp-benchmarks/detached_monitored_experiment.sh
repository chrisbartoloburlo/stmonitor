iterations=$1
experiments=$2

run=1
wd=`pwd`

while [ "$experiments" -ne 0 ] ; do
  echo Running SMTPD Detached Monitored Experiment ${run} with ${iterations} emails

  screen -S stmonbench -dm bash -c "/usr/bin/time --format=\"%P,%M,%K\" java -cp ../examples/target/scala-2.12/examples-assembly-0.0.3.jar examples.smtp.MonWrapper 1025 1026 2>> results/detached_monitored/${iterations}_cpu_mem_run.txt"

  sleep 1

  java -cp ../examples/target/scala-2.12/examples-assembly-0.0.3.jar examples.smtp.Client $wd/results/detached_monitored/ ${iterations} ${run} 1026

  screen -S stmonbench -p 0 -X stuff "^C"
  echo Finished SMTPD Detached Monitored Experiment ${run}

  run=$((run+1))
  experiments=$((experiments-1))
done