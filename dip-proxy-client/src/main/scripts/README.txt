testing case:

*******************************************
provider    service     ns
*******************************************
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
********************************************

--------------------------------------------------------------------------------------------
command exmaple:
1) Java Client:
format like this ./dipProxyClient.sh proxy-service provider service AC= NS= FORMAT= DETAIL=

exmaples:
./dipProxyClient.sh proxy-service EBI picr AC=NP_500606 NS=refseq FORMAT=dxf DETAIL=base
./dipProxyClient.sh proxy-service EBI picr AC=NP_500606 NS=refseq FORMAT=native

./dipProxyClient.sh proxy-service NCBI nlm AC=101526034 FORMAT=dxf DETAIL=full

------------FOR OLD VERSION services ---------------------------------
./dipProxyClient.sh ncbi-service getJournal AC=101526034 DETAIL=full


-----------------------------------------------------------------------------------------------
2) Perl Client:
perl proxyServiceClient.pl PROVIDER=EBI SERVICE=picr AC=NP_500606 NS=refseq DETAIL=full FORMAT=dxf TESTNUM=0 TAXID=6239
perl proxyServiceClient.pl PROVIDER=EBI SERVICE=picr AC=NP_500606 NS=refseq FORMAT=native
 
-------------------------------------------------------------------------------------------------------------
