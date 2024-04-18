#! /bin/bash

# benchmark.sh -d 100ms -j 10ms -l 1%
set -o xtrace

if [ "$#" -eq 0 ]; then
  echo "No arguments provided."
  echo "use the -h flag for help" >&2
  exit 1
fi

while getopts 'p:f:r:d:l:j:h:' OPTION; do
  case "$OPTION" in
    p)
      test_plan="$OPTARG"
      ;;
    f)
      test_file_size="$OPTARG"
      ;;
    r)
      results_file="$OPTARG"
      ;;
    d)
      delay="$OPTARG"
      ;;
    j)
      jitter="$OPTARG"
      ;;
    l)
      packet_loss=${OPTARG}
      ;;
    h)
      echo script usage: benchmark.sh -p \<path/to/testplan.csv\> -f \<size of file to transfer\> -r \<path/for/results/file.csv\> -d \<benchmark latency\> -j \<benchmark jitter\> -l \<packet loss\>
      exit 1
      ;;
  esac
done

# create testfile of specified size
fallocate -l ${test_file_size} testfile

# find mvn repository location
user_name=`whoami`
mvn_repository=/home/${user_name}/.m2/repository

# setup netem env here
sudo tc qdisc add dev lo root netem
sudo tc qdisc change dev lo root netem delay ${delay} ${jitter} loss ${packet_loss} 

# start receiver
java -classpath ../target/classes:${mvn_repository}/org/slf4j/slf4j-api/1.7.5/slf4j-api-1.7.5.jar:${mvn_repository}/org/slf4j/slf4j-log4j12/1.7.5/slf4j-log4j12-1.7.5.jar:${mvn_repository}/log4j/log4j/1.2.17/log4j-1.2.17.jar net.utp4j.examples.configtest.ConfigTestRead &
SERVER_ID=$!
echo $SERVER_ID

# handle interruption through ctrl-c
handler()
{
  echo exiting...
  # undo netem
  sudo tc qdisc del dev lo root
  # kill receiver
  kill $SERVER_ID
  kill $SERVER_ID
  kill $SERVER_ID
  # delete testfile
  rm testfile
}

trap handler SIGINT

# start sender
sudo java -classpath ../target/classes:${mvn_repository}/org/slf4j/slf4j-api/1.7.5/slf4j-api-1.7.5.jar:${mvn_repository}/org/slf4j/slf4j-log4j12/1.7.5/slf4j-log4j12-1.7.5.jar:${mvn_repository}/log4j/log4j/1.2.17/log4j-1.2.17.jar net.utp4j.examples.configtest.ConfigTestWrite ${test_plan} testfile ${results_file} localhost


