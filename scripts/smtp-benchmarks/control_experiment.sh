iterations=$1
experiments=$2

run=1
wd=`pwd`

while [ "$experiments" -ne 0 ] ; do
  echo Running SMTP Control Experiment ${run} with ${iterations} emails

  /usr/bin/time --format="%P,%M,%K" java -cp ../examples-assembly-0.0.3.jar examples.smtp.Client $wd/results/control/ ${iterations} ${run} 1025 2>> results/control/${iterations}_cpu_mem_run.txt

  sleep 1

  echo SMTP Control Experiment ${run} finished

  run=$((run+1))
  experiments=$((experiments-1))
done