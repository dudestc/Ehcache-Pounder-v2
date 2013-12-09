#!/bin/bash

mvn clean javadoc:javadoc assembly:assembly

cd target
tar -xvzf ehcache-pounder-v2-0.4-distribution.tar.gz 
cd ehcache-pounder-v2-0.4

cp ../../*.key .

chmod a+x *.sh

./run-pounder.sh



