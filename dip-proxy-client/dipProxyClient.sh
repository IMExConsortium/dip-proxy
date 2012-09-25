#!/bin/sh

if [ "$1" = "proxy-service" ]; then
java -cp target/dip-proxy-client-jar-with-dependencies.jar edu.ucla.mbi.client.ProxyCommandClient http://10.1.200.201:8080/dip-proxy-server/proxy-service $2 $3 $4 $5 $6 $7 $8 $9 $10
#java -cp target/dip-proxy-client-jar-with-dependencies.jar edu.ucla.mbi.client.ProxyCommandClient http://dip.doe-mbi.ucla.edu/dip-proxy/proxy-service $2 $3 $4 $5 $6 $7 $8 $9 $10
fi


if [ "$1" = "ncbi-service" ]; then
#java -cp target/dip-proxy-client-jar-with-dependencies.jar edu.ucla.mbi.client.NcbiServiceClient http://10.1.200.201:8080/dip-proxy-server/ncbi-service $2 $3 $4 $5 $6 $7 $8 $9 $10
java -cp target/dip-proxy-client-jar-with-dependencies.jar edu.ucla.mbi.client.ProxyCommandClient http://dip.doe-mbi.ucla.edu:80/dip-proxy/ncbi-service $2 $3 $4 $5 $6 $7 $8 $9 $10
fi

if [ "$1" = "ebi-service" ]; then
java -cp target/dip-proxy-client-jar-with-dependencies.jar edu.ucla.mbi.client.ProxyCommandClient http://10.1.200.201:8080/dip-proxy-server/ebi-service $2 $3 $4 $5 $6 $7 $8 $9 $10
fi

