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
./dipProxyClient.sh proxy-service dbid EBI picr AC=NP_500606 NS=refseq FORMAT=dxf DETAIL=base
./dipProxyClient.sh cxf-proxy-service dbid EBI picr AC=NP_500606 NS=refseq FORMAT=dxf DETAIL=base

./dipProxyClient.sh cxf-proxy-service dbid NCBI nlm AC=101526034 FORMAT=dxf DETAIL=base


-----------------------------------------------------------------------------
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

RETRIEVE RESPONSE HEADER( including fault info )
curl --head **** or curl -I ***

################################################################################
