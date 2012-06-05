#!/bin/sh

#java -cp target/dip-proxy-jar-with-dependencies.jar edu.ucla.mbi.proxy.ncbi.NcbiServiceClient http://dip.doe-mbi.ucla.edu:50606/ProxyWS/services/ncbiPublic $1 $2 $3 $4 $5 $6 $7 $8 $9 $10

if [ "$1" = "proxyM" ]; then
java -cp target/dip-proxy-server-client.jar:../dip-proxy-api/target/dip-proxy-api.jar edu.ucla.mbi.proxy.ProxyServiceClient http://10.1.1.201:8080/dip-proxy-server/proxy-service $2 $3 $4 $5 $6 $7 $8 $9 $10
fi


if [ "$1" = "ncbiProxy" ]; then
#java -cp target/dip-proxy-jar-with-dependencies.jar edu.ucla.mbi.proxy.ncbi.NcbiServiceClient http://dip.mbi.ucla.edu:55501/dip-proxy/ncbi-service $2 $3 $4 $5 $6 $7 $8 $9 $10
java -cp target/dip-proxy-client.jar:../dip-api-ws/target/dip-services.jar edu.ucla.mbi.proxy.ncbi.NcbiServiceClient http://10.1.1.201:8080/dip-proxy/ncbi-service $2 $3 $4 $5 $6 $7 $8 $9 $10
fi


if [ "$1" = "ncbiProxyM" ]; then
java -cp target/dip-proxy-client.jar:../dip-api-ws/target/dip-services.jar edu.ucla.mbi.proxy.ncbi.NcbiServiceClient http://10.1.1.203:50000/dip-proxy/ncbi-service $2 $3 $4 $5 $6 $7 $8 $9 $10
fi

if [ "$1" = "ncbiProxyS" ]; then
java -cp target/dip-proxy-client.jar:../dip-api-ws/target/dip-services.jar edu.ucla.mbi.proxy.ncbi.NcbiServiceClient http://dip.doe-mbi.ucla.edu/dip-proxy/ncbi-service $2 $3 $4 $5 $6 $7 $8 $9 $10
fi

if [ "$1" = "ebiProxy" ]; then
#java -cp target/dip-proxy-jar-with-dependencies.jar edu.ucla.mbi.proxy.ncbi.NcbiServiceClient http://dip.mbi.ucla.edu:55501/dip-proxy/ncbi-service $2 $3 $4 $5 $6 $7 $8 $9 $10
java -cp target/dip-proxy-client.jar:../dip-api-ws/target/dip-services.jar edu.ucla.mbi.proxy.ebi.EbiServiceClient http://10.1.1.202:8080/dip-proxy/ebi-service $2 $3 $4 $5 $6 $7 $8 $9 $10
fi

if [ "$1" = "ebiProxyM" ]; then
#java -cp target/dip-proxy-jar-with-dependencies.jar edu.ucla.mbi.proxy.ncbi.NcbiServiceClient http://dip.mbi.ucla.edu:55501/dip-proxy/ncbi-service $2 $3 $4 $5 $6 $7 $8 $9 $10
java -cp target/dip-proxy-server-client.jar:../dip-proxy-api/target/dip-proxy-api.jar edu.ucla.mbi.proxy.ebi.EbiServiceClient http://10.1.1.201:8080/dip-proxy-server/ebi-service $2 $3 $4 $5 $6 $7 $8 $9 $10
fi

if [ "$1" = "ebiProxyS" ]; then
java -cp target/dip-proxy-client.jar:../dip-api-ws/target/dip-services.jar edu.ucla.mbi.proxy.ebi.EbiServiceClient http://dip.doe-mbi.ucla.edu/dip-proxy/ebi-service $2 $3 $4 $5 $6 $7 $8 $9 $10
fi


 
