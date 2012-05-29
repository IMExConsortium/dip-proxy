#!/usr/bin/perl

#use Proxy;
use DipProxy::ProxyNCBI;
use DipProxy::ProxyEBI;

#subroutine1();
my $proxyNCBI = DipProxy::ProxyNCBI->new();
my $proxyEBI = DipProxy::ProxyEBI->new();

$proxyName =@ARGV[0];
$operation = @ARGV[1];
$ac = @ARGV[2];
$ns = @ARGV[3];
$detail = @ARGV[4];
$format = @ARGV[5];
$taxid = @ARGV[6];

my ($output, $fault);

if($proxyName eq "NCBI"){
    my $proxyNCBI = DipProxy::ProxyNCBI->new();
    ($output, $fault) = $proxyNCBI->proxyNCBI($operation, $ac, $ns, $detail, $format, 0);
}

if( $proxyName eq "EBI"){
    my $proxyEBI = DipProxy::ProxyEBI->new();
    ($output, $fault) = $proxyEBI->proxyEBI($operation, $ac, $ns, $detail, "0", $format, $taxid);
    print "after to call proxyEBI\n";
}

#proxyNCBI parameters: $operation, $ac, $ns, $detail, $testnum, $ncbitaxid, $format
#my ($output, $fault) = $proxyNCBI->proxyNCBI("getPubmedArticle", "1111", "pubmed", "stub", "dxf");
#my ($output, $fault) = $proxyNCBI->proxyNCBI("getTaxon", "9606", "ncbitaxon", "stub", "dxf");
#my ($output, $fault) = $proxyNCBI->proxyNCBI("getTaxon", $ac, $ns, $detail, $format);
#my ($output, $fault) = $proxyNCBI->proxyNCBI("getPubmedArticle", "1111", "pubmed", "stub", "dxf" );
#my ($output, $fault) = $proxyNCBI->proxyNCBI("getRefseq", $ac, "refseq", "base", $format);

#my ($output, $fault) = $proxyNCBI->proxyNCBI("getRefseq", "NP_015400", "refseq", "base", "native");

#proxyEBI parameters: the last one is format: dxf is for dxf record, native is for native record
#my ($output, $fault) = $proxyEBI->proxyEBI("getPicrList", $ac, $ns, "full", "0", $format, $taxid);
#my ($output, $fault) = $proxyEBI->proxyEBI("getUniprot", "WNT0_MOUSSE", "uniprot", "base", "0", "native", 10090 );
print "fault: $fault\n";
print "$output\n";

