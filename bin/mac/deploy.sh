#!/bin/bash
parent_path=$( cd "$(dirname "${BASH_SOURCE[0]}")" ; pwd -P )

cd "$parent_path" & 

mvn -f ../../broker/pom.xml clean deploy &

mvn -f ../../brokerRest/pom.xml clean deploy &

mvn -f ../../dil/pom.xml clean deploy &

mvn -f ../../integration/pom.xml clean deploy &

mvn -f ../../integrationRest/pom.xml clean deploy