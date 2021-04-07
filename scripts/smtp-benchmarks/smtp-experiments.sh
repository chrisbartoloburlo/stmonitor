screen -S smtpserver -dm python3 -m smtpd -c DebuggingServer -n localhost:1025

experiments=$1
increments=20

iterations=100
while [ "$increments" -ne 0 ] ; do

  sh control_experiment.sh $iterations $experiments
  sh monitored_experiment.sh $iterations $experiments
  sh detached_experiment.sh $iterations $experiments

  iterations=$((iterations+100))
  increments=$((increments-1))
done

screen -S smtpserver -X quit

python3 smtp-plots.py 5
