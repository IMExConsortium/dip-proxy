#!/usr/bin/perl

#use Proxy;
use DipProxy::ProxyNCBITest;
use DipProxy::ProxyEBITest;
use DipProxy::ProxyNCBI;

#subroutine1();
my $proxyNCBI = DipProxy::ProxyNCBITest->new();
my $proxyEBI = DipProxy::ProxyEBITest->new();

=for
#proxyEBI testisng
my ($output_stub, $fault) = $proxyEBI->proxyEBI("getPicrList", "P60016", "refseq", "stub", "0", "9606");

#my ($output, $fault) = $proxyEBI->proxyEBI("getPicrList", "NP_612815", "refseq", "base", "0", "9606");

print "testProxyEBI: output is for detail=stub:\n$output_stub\n\n";
my ($output_base, $fault) = $proxyEBI->proxyEBI("getPicrList", "P60016", "refseq", "base", "0", "9606");

print "testProxyEBI: output is for detail=base:\n$output_base\n\n";

my ($output_full, $fault) = $proxyEBI->proxyEBI("getPicrList", "P60016", "refseq", "full", "0", "9606");

print "testProxyEBI: output is for detail=full:\n$output_full\n";
=cut

#proxyNCBI testing
my ($output_stub, $fault) = $proxyNCBI->proxyNCBI("getPubmedArticle", "1115", "pubmed", "stub", "0");

print "testProxyNCBI: output is for detail=stub:\n$output_stub\n\n";
my ($output_base, $fault) = $proxyNCBI->proxyNCBI("getPubmedArticle", "1115", "pubmed", "base", "0");

print "testProxyNCBI: output is for detail=base:\n$output_base\n\n";

my ($output_full, $fault) = $proxyNCBI->proxyNCBI("getPubmedArticle", "1115", "pubmed", "full", "0");

print "testProxyNCBI: output is for detail=full:\n$output_full\n";

