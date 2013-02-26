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

format like this ./dipProxyClient.sh proxy-service provider service AC= NS= FORMAT= DETAIL=

exmaples:
./dipProxyClient.sh dbid EBI picr AC=NP_500606 NS=refseq FORMAT=dxf DETAIL=base
./dipProxyClient.sh dbid EBI picr AC=NP_500606 NS=refseq FORMAT=native

./dipProxyClient.sh dbid NCBI nlm AC=101526034 FORMAT=dxf DETAIL=full

./dipProxyClient.sh dbid SGD yeastmine AC=YFL039C NS=sgd FORMAT=native 

------------FOR OLD VERSION services ---------------------------------
./dipProxyClient.sh ncbi-service getJournal AC=101526034 DETAIL=full

