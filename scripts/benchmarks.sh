#!/bin/bash
# Usage: sh benchmarks.sh <number of repetitions per experiment> <list of benchmarks>

iterations=$1
wd=`pwd`

screen -v > /dev/null 2>&1
if [ "$?" = "1" ]; then
  echo "screen not found, cannot run benchmarks: terminating"
  exit
fi

python3 -c "import matplotlib.pyplot"
if [ "$?" = "1" ]; then
  echo "Python module matplotlib not found, cannot run benchmarks: terminating"
  exit
fi

echo "Creating a fat JAR of stmonitor/examples/..."
sbt examples/assembly
echo "Fat JAR created.\n"

if [ "$iterations" = "kickthetires" ]; then
  jmeter -v > /dev/null 2>&1
  if [ "$?" = "1" ]; then
    echo "Jmeter not found, cannot run benchmarks: terminating"
  else
    echo "Running a limited set of the experiments for the SMTP python, SMTP postfix, Ping Pong and HTTP benchmarks for the Kick-the-Tires phase...\n"
    echo "Running SMTP python benchmarks with 2 repetitions per experiment..."
    sh $wd/scripts/smtp-benchmarks/smtp_experiments.sh 2 true smtp-python
    echo "SMTP python benchmarks finished.\n"

    echo "Running SMTP postfix benchmarks with 2 repetitions per experiment..."
    sh $wd/scripts/smtp-benchmarks/smtp_experiments.sh 2 true smtp-postfix
    echo "SMTP postfix benchmarks finished.\n"

    echo "Running Ping Pong benchmarks with 2 repetitions per experiment..."
    sh $wd/scripts/pingpong-benchmarks/pingpong_experiments.sh 2 true
    echo "Ping Pong benchmarks finished.\n"

    echo "Running HTTP benchmarks with 2 repetitions per experiment..."
    sh $wd/scripts/http-benchmarks/http_experiments.sh 2 true
    echo "HTTP benchmarks finished.\n"

    echo "The experiments completed successfully if the respective directories contain the generated plots:"
    echo "SMTP Python benchmark plots can be found in the directory $wd/scripts/smtp-benchmarks/smtp-python/plots"
    echo "SMTP Postfix benchmark plots can be found in the directory $wd/scripts/smtp-benchmarks/smtp-postfix/plots"
    echo "Ping Pong benchmark plots can be found in the directory $wd/scripts/pingpong-benchmarks/plots"
    echo "HTTP benchmark plots can be found in the directory $wd/scripts/http-benchmarks/plots"
    exit
  fi
fi

re='^[0-9]+$'
if ! [[ $iterations =~ $re ]] ; then
   echo "ERROR: $iterations is not a number" >&2
   echo "USAGE: sh scripts/benchmarks.sh \$REPETITIONS \$EXPERIMENTS"
   echo "See README.md for info"
   exit
fi

experiments=""

for n in $(seq 2 $#); do
  if [ "$2" = "smtp-python" ]; then
    echo "Running SMTP python benchmarks with $iterations repetitions per experiment..."
    sh $wd/scripts/smtp-benchmarks/smtp_experiments.sh $iterations false smtp-python
    echo "SMTP python benchmarks finished.\n"
    experiments="$experiments smtp-python"
  elif [ "$2" = "pingpong" ]; then
    jmeter -v > /dev/null 2>&1
    if [ "$?" = "1" ]; then
      echo "Jmeter not found, skipping ping pong benchmarks"
    else
      echo "Running Ping Pong benchmarks with $iterations repetitions per experiment..."
      sh $wd/scripts/pingpong-benchmarks/pingpong_experiments.sh $iterations false
      echo "Ping Pong benchmarks finished.\n"
      experiments="$experiments pingpong"
    fi
  elif [ "$2" = "http" ]; then
    jmeter -v > /dev/null 2>&1
    if [ "$?" = "1" ]; then
      echo "Jmeter not found, skipping HTTP benchmarks"
    else
      echo "Running HTTP benchmarks with $iterations repetitions per experiment..."
      sh $wd/scripts/http-benchmarks/http_experiments.sh $iterations false
      echo "HTTP benchmarks finished.\n"
      experiments="$experiments http"
    fi
  elif [ "$2" = "smtp-postfix" ]; then
    echo "Running SMTP postfix benchmarks with $iterations repetitions per experiment..."
    sh $wd/scripts/smtp-benchmarks/smtp_experiments.sh $iterations false smtp-postfix
    echo "SMTP postfix benchmarks finished.\n"
    experiments="$experiments smtp-postfix"
  else
    echo "*** Unknown benchmark: $2 *** Available benchmarks: smtp-python pingpong http smtp-postfix\n"
  fi
  shift
done

for experiment in $experiments; do
  if [ "$experiment" = "smtp-python" ]; then
    echo "SMTP Python benchmark plots can be found in the directory $wd/scripts/smtp-benchmarks/smtp-python/plots"
  elif [ "$experiment" = "pingpong" ]; then
    echo "Ping Pong benchmark plots can be found in the directory $wd/scripts/pingpong-benchmarks/plots"
  elif [ "$experiment" = "http" ]; then
    echo "HTTP benchmark plots can be found in the directory $wd/scripts/http-benchmarks/plots"
  elif [ "$experiment" = "smtp-postfix" ]; then
    echo "SMTP Postfix benchmark plots can be found in the directory $wd/scripts/smtp-benchmarks/smtp-postfix/plots"
  else
    echo "*** Benchmarks not executed for: $experiment ***"
  fi
done