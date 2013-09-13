#!/bin/bash

mvn clean javadoc:javadoc assembly:assembly

cd target
tar -xvzf ehcache-pounder-v2-*.tar.gz 

cp *.jar ../
cp *.tar.gz ../

