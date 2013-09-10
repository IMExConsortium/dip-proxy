Provider    Service     NS
------------------------------------------
NCBI        nlm         nlmid
NCBI        pubmed      pmid
NCBI        refseq      refseq
NCBI        taxon       ncbitaxid
NCBI        entrezgene  entrezgene
-------------------------------------------
EBI         uniprot     uniprot
EBI         picr        refseq/uniprot
-------------------------------------------
MBI         prolinks    prolinks
-------------------------------------------
DIP         dip         dip
DIP         diplegacy   dip
-------------------------------------------
SGD         yeastmine   sgd
********************************************

--------------------------------------------------------------------------------------------
command exmaple:

Java Client:

format like this ./dipProxyClient.sh service-name dbid provider service AC= NS= FORMAT= DETAIL=

exmaples:
-----------------------NEW VERSION--------------------------------------
production server:
./dipProxyClient.sh proxy-service-new NCBI entrezgene AC=7204 NS=entrezgene FORMAT=dxf DETAIL=base

dev server:
./dipProxyClient.sh cxf-proxy-service dbid EBI picr AC=NP_500606 NS=refseq FORMAT=dxf DETAIL=base

./dipProxyClient.sh cxf-proxy-service dbid NCBI nlm AC=101526034 FORMAT=dxf DETAIL=base

./dipProxyClient.sh cxf-proxy-service dbid DIP dip AC=DIP-222384NP NS=dip FORMAT=dxf DETAIL=base

--------------------- OBSOLETE VERSION--------------------------------------------------------
./dipProxyClient.sh dbid EBI picr AC=NP_500606 NS=refseq FORMAT=native

./dipProxyClient.sh dbid NCBI nlm AC=101526034 FORMAT=dxf DETAIL=full

./dipProxyClient.sh dbid SGD yeastmine AC=YFL039C NS=sgd FORMAT=native 

------------FOR OLD VERSION services ---------------------------------
./dipProxyClient.sh ncbi-service getJournal AC=101526034 DETAIL=full


################################################################################

curl command test RESTful service:

GET:
curl http://dip.mbi.ucla.edu:55601/dip-proxy/current/rest/proxy-service/native-record/EBI/picr/refseq/NP_500606

curl --get --data "detail=base" http://dip.mbi.ucla.edu:55601/dip-proxy/current/rest/proxy-service/dxf-record/EBI/picr/refseq/NP_500606

POST:
curl --data "{provider:'EBI', service:'picr', ns:'refseq', ac:'NP_500606'}" http://dip.mbi.ucla.edu:55601/dip-proxy/current/rest/proxy-service/query-native --header "Content-Type:application/json"

/* using test json file */
curl --data @test http://dip.mbi.ucla.edu:55601/dip-proxy/current/rest/proxy-service/query-dxf --header "Content-Type:application/json"

curl --data "{provider:'EBI', service:'picr', ns:'refseq', ac:'NP_500606', detail:'base'}" http://dip.mbi.ucla.edu:55601/dip-proxy/current/rest/proxy-service/query-dxf --header "Content-Type:application/json"

RETRIEVE GET RESPONSE HEADER( including fault info )
curl --head **** or curl -I ***
curl -I http://dip.mbi.ucla.edu:55601/dip-proxy/current/rest/proxy-service/native-record/EBI/picr/refseq/NP_500606
HTTP/1.1 200 OK
Date: Wed, 15 May 2013 23:41:47 GMT
Server: Jetty(6.1.25)
X-PROXY-timestamp: 2013-05-09T20:33:26.893Z
Content-Type: text/plain


RETRIEVE POST RESPONSE HEADER using -D-
curl --data "{provider:'EBI', service:'pic', ns:'refseq', ac:'NP_500606'}" http://dip.mbi.ucla.edu:55601/dip-proxy/current/rest/proxy-service/query-native --header "Content-Type:application/json" -D-

HTTP/1.1 500 Internal Server Error
Date: Wed, 15 May 2013 23:53:25 GMT
Server: Jetty(6.1.25)
Content-Length: 0
X-PROXY-error-code: 4
X-PROXY-error-message: unsupported operation
Connection: close
Content-Type: text/plain

################################################################################
