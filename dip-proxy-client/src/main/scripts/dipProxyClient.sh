#!/bin/sh

if [ "$1" = "proxyM" ]; then
java -cp ../../../target/dip-proxy-client-jar-with-dependencies.jar edu.ucla.mbi.client.ProxyCommandClient http://10.1.1.201:8080/dip-proxy-server/proxy-service $2 $3 $4 $5 $6 $7 $8 $9 $10
fi

