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
format like this ./dipProxyClient.sh proxyM provider service AC= NS= FORMAT= DETAIL=
exmaple: ./dipProxyClient.sh proxyM EBI picr AC=NP_500606 NS=refseq FORMAT=dxf DETAIL=base
exmaple: ./dipProxyClient.sh proxyM EBI picr AC=NP_500606 NS=refseq FORMAT=native

-----------------------------------------------------------------------------------------------
2) Perl Client:
perl proxyClient.pl EBI getPicrList NP_500606 refseq stub dxf 6239
perl proxyServiceClient.pl PROVIDER=EBI SERVICE=picr AC=NP_500606 NS=refseq DETAIL=full FORMAT=dxf TESTNUM=0 TAXID=6239
perl proxyServiceClient.pl PROVIDER=EBI SERVICE=picr AC=NP_500606 NS=refseq FORMAT=native
 
-------------------------------------------------------------------------------------------------------------
