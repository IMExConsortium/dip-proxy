#!/bin/sh

if [ "$1" = "proxy-service" ]; then
java -cp target/dip-proxy-client-jar-with-dependencies.jar edu.ucla.mbi.client.ProxyCommandClient http://10.1.200.$2:8080/dip-proxy/proxy-service $3 $4 $5 $6 $7 $8 $9 
fi

if [ "$1" = "proxy-service-new" ]; then
java -cp target/dip-proxy-client-jar-with-dependencies.jar edu.ucla.mbi.client.ProxyCommandClient http://dip.mbi.ucla.edu/dip-proxy/ws/soap/proxy-service $2 $3 $4 $5 $6 $7 $8 $9 
fi

if [ "$1" = "cxf-proxy-service" ]; then
java -cp target/dip-proxy-client-jar-with-dependencies.jar edu.ucla.mbi.client.ProxyCommandClient http://10.1.200.$2:8080/dip-proxy/ws/soap/proxy-service $3 $4 $5 $6 $7 $8 $9
fi

if [ "$1" = "ncbi-service-production" ]; then
java -cp target/dip-proxy-client-jar-with-dependencies.jar edu.ucla.mbi.client.NcbiServiceClient http://dip.mbi.ucla.edu/dip-proxy/ws/soap/ncbi-service $2 $3 $4 $5 $6 $7 $8 $9
fi

if [ "$1" = "ncbi-service-cxf" ]; then
java -cp target/dip-proxy-client-jar-with-dependencies.jar edu.ucla.mbi.client.NcbiServiceClient http://10.1.200.201:8080/dip-proxy/ws/soap/ncbi-service $2 $3 $4 $5 $6 $7 $8 $9
fi


#if [ "$1" = "proxy-service-209" ]; then
#java -cp target/dip-proxy-client-jar-with-dependencies.jar edu.ucla.mbi.client.ProxyCommandClient http://10.1.200.209:8080/dip-proxy/proxy-service $2 $3 $4 $5 $6 $7 $8 $9 $10
#java -cp target/dip-proxy-client-jar-with-dependencies.jar edu.ucla.mbi.client.ProxyCommandClient http://dip.doe-mbi.ucla.edu/dip-proxy/proxy-service $2 $3 $4 $5 $6 $7 $8 $9 $10
#fi
 
#if [ "$1" = "proxy-service-210" ]; then
#java -cp target/dip-proxy-client-jar-with-dependencies.jar edu.ucla.mbi.client.ProxyCommandClient http://10.1.200.210:8080/dip-proxy/proxy-service $2 $3 $4 $5 $6 $7 $8 $9 $10
#fi

#if [ "$1" = "ncbi-service" ]; then
#java -cp target/dip-proxy-client-jar-with-dependencies.jar edu.ucla.mbi.client.NcbiServiceClient http://10.1.200.201:8080/dip-proxy/ncbi-service $2 $3 $4 $5 $6 $7 $8 $9 $10

#java -cp target/dip-proxy-client-jar-with-dependencies.jar edu.ucla.mbi.client.ProxyCommandClient http://dip.doe-mbi.ucla.edu:80/dip-proxy/ncbi-service $2 $3 $4 $5 $6 $7 $8 $9 $10
#fi

#if [ "$1" = "ebi-service" ]; then
#java -cp target/dip-proxy-client-jar-with-dependencies.jar edu.ucla.mbi.client.ProxyCommandClient http://10.1.200.201:8080/dip-proxy-server/ebi-service $2 $3 $4 $5 $6 $7 $8 $9 $10
#fi

