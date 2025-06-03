#!/bin/bash

echo Parameter 1: $1

if [ "$1" == "--startup" ]; then
	echo "yes"
	# this is from http://stackoverflow.com/questions/36538150/linuxs-equivalent-of-windows-timeout-command
	read -t 10 -p "press any key to continue: " || true
	# timeout 60 /nobreak
fi

CNT=-1
args=""
while true; do
	let CNT=$CNT+1 
	echo iteration: $CNT

	echo "`date` Starting eduSPIM (args = $args iteration = $CNT)" >> eduSPIM_start.log

	java -Xmx4g -cp target/EduSPIM-1.1.0-jar-with-dependencies.jar main.Microscope $args
	ERRORLEVEL=$?
	echo Error level:
	echo $ERRORLEVEL

	if [ $ERRORLEVEL == 0 ]; then
		break
	fi

	if [ $ERRORLEVEL == -5 ]; then
		echo Fatal error.
		args="--fatal"
	else
		echo Error, starting again.
		args=""
	fi

done

