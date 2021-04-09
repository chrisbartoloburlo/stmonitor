#!/bin/bash
experiments=20
increments=20

requests=100
rampup=2

while [ "$increments" -ne 0 ] ; do

  sh control_experiment.sh $requests $rampup $experiments
  sh monitored_experiment.sh $requests $rampup $experiments
  sh detached_monitored_experiment.sh $requests $rampup $experiments

  requests=$((requests+100))
  rampup=$((rampup+2))
  increments=$((increments-1))
done