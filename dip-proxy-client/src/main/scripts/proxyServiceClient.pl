#!/usr/bin/perl

push(@Inc, `pwd`);
use ProxyService;

my ( $provider, $service, $ac, $ns, $detail, $format, $testnum, $taxid );

$testnum = 0; #default value

for( my $i = 0; $i<@ARGV; $i++ ) {
    if ( $ARGV[$i] =~ /PROVIDER=(.+)/ ) {
        $provider = $1;
    }

    if ( $ARGV[$i] =~ /SERVICE=(.+)/ ) {
        $service = $1;
    }

    if ( $ARGV[$i] =~ /AC=(.+)/ ) {
        $ac = $1;
    }

    if ( $ARGV[$i] =~ /NS=(.+)/ ) {
        $ns = $1;
    }

    if ( $ARGV[$i] =~ /DETAIL=(.+)/ ) {
        $detail = $1;
    }

    if ( $ARGV[$i] =~ /FORMAT=(.+)/ ) {
        $format = $1;
    }

    if ( $ARGV[$i] =~ /TESTNUM=(.+)/ ) {
        $testnum = $1;
    }

    if ( $ARGV[$i] =~ /TAXID=(.+)/ ) {
        $taxid = $1;
    }
}

print "INPUT ARGUMENTS:".
      "\n-->PROVIDER=".$provider." -->SERVICE=".$service." -->AC=".$ac.
      "\n-->NS=".$ns." -->DETAIL=".$detail." -->FORMAT=".$format.
      "\n-->TESTNUM=".$testnum." -->TAXID=".$taxid."\n";

my ($output, $fault);

my $proxyService = ProxyService->new();
    
($output, $fault) = $proxyService->proxyService( $provider, $service, 
                        $ac, $ns, $detail, $format, $testnum, $taxid );

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

