#!/bin/bash

invocation=$*
this=${0##/}

usage() { 
	echo "usage: $this mainclass [args]" 
}

if [ $# -lt 1 ]; then
	usage
	exit 1
fi

jar_dir="target"
mainclass=$1
shift
mainopts=$*
java_opts="-Xmx1g"

classpath=
for jar in $jar_dir/*.jar; do 
	classpath=$classpath:$jar
done	

java $java_opts -cp $classpath $mainclass $mainopts

