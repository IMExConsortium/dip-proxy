package DipProxy::ProxyNCBI;

##===============================================================================
# $HeadURL: http://wyu@imex.mbi.ucla.edu/svn/dip-ws/trunk/scripts/dip-proxy/#$
# $Id$
# Version: $Rev$
## ===============================================================================
#
# ProxyNCBI.pm: 
#   object method proxyNCBI ($operation, $ac, $ns, $detail, $testnum, $ncbitaxid)           
#   required args: $operation, $ac, $ns, $detail, $testnum
#   return: ($dataset_string, $fault)
#
##=================================================================================

use SOAP::Lite;

my $conf_file = "DipProxy/proxy.conf";
my %proxy_conf;

open (CONF, $conf_file) or die "cannot open the file $conf_file: $!\n";
while(<CONF>){
    chomp;
    next if (/^\s*$/ or /^\s*\#/ ); # skip empty / comment lines
    my ($key, $value) = split("=", $_);
    $key =~ s/\s//g;
    $value =~ s/\s//g;
    $proxy_conf{$key} = $value;
}
close (CONF);


sub new{
    my $self = {};
    bless($self);
    return $self;
}

sub proxyNCBI{
    my $self = shift;
    my ($operation, $ac, $ns, $detail, $format, $testnum) = @_;

    #call soap service ebiPublic for getUniprot
    my $soap_server = SOAP::Lite
            ->uri($proxy_conf{NCBI_URI})
            ->proxy($proxy_conf{NCBI_SRV}, timeout=>300)
            ->default_ns($proxy_conf{NCBI_RNS})
            ->on_action(sub{sprintf '%s/%s', @_})
            ->encoding('utf-8')
            ->readable('true')
            ->outputxml('true');

    my $dataset_out = "";
    my $soap_out = "";

    if( $operation eq "getRefseq" ){
        $dataset_out =$soap_server->getRefseq(
            SOAP::Data->name('ns')->type(string=>"refseq"),
            SOAP::Data->name('ac')->type(string=>$ac),
            SOAP::Data->name('format')->type(string=>$format),
            SOAP::Data->name('detail')->type(string=>$detail));

    } elsif ( $operation eq "getPubmedArticle" ){
         $dataset_out =$soap_server->getPubmedArticle(
            SOAP::Data->name('ns')->type(string=>"pmid"),
            SOAP::Data->name('ac')->type(string=>$ac),
            SOAP::Data->name('format')->type(string=>$format),
            SOAP::Data->name('detail')->type(string=>$detail));

    } elsif ( $operation eq "getGene" ){
        $dataset_out =$soap_server->getGene(
            SOAP::Data->name('ns')->type(string=>"entrezgene"),
            SOAP::Data->name('ac')->type(string=>$ac),
            SOAP::Data->name('format')->type(string=>$format),
            SOAP::Data->name('detail')->type(string=>$detail));

    } elsif( $operation eq "getTaxon" ){
        $dataset_out =$soap_server->getTaxon(
            SOAP::Data->name('ns')->type(string=>"taxonomy"),
            SOAP::Data->name('ac')->type(string=>$ac),
            SOAP::Data->name('format')->type(string=>$format),
            SOAP::Data->name('detail')->type(string=>$detail));

    } elsif( $operation eq "getJournal") {
        $dataset_out =$soap_server->getJournal(
            SOAP::Data->name('ns')->type(string=>"nlmid"),
            SOAP::Data->name('ac')->type(string=>$ac),
            SOAP::Data->name('format')->type(string=>$format),
            SOAP::Data->name('detail')->type(string=>$detail));

    }else {
        return ("0", "operation is invalid");
    }

    if( $format eq "native"){
        return ($dataset_out, "0");
    }

    my ($soap_out, $fault) = checkProxySoapOut( $operation, $dataset_out, $ac, $ns, $detail, $format, ++$testnum);

    return ($soap_out, $fault);
}

sub checkProxySoapOut{
    my ($operation, $datasetOut, $ac, $ns, $detail, $format, $testnum) = @_;

    my $retest = 0;

    #print "ProxyNCBI.pm: checkProxySoapOut: coming in \$datasetOut is \n$datasetOut\n";
    if( $datasetOut =~ /<(ns2:)?dataset/ ){
        if( $datasetOut !~ /<(ns2:)?node/ ){
            return("0", "no hit record")
        }

        $datasetOut =~ s/<S:Envelope.*?><S:Body><(ns3:|ns2:)?result.*?><(ns2:|ns3:)?dataset.*?>/<dataset>/;

        $datasetOut =~ s/<\/(ns3:|ns2:){0,1}result>//;
        $datasetOut =~ s/<\/S:Body><\/S:Envelope>//;
        $datasetOut =~ s/(ns2:|ns3:)//g;

        #print "ProxyNCBI.pm: checkProxySoapOut: coming out\$datasetOut is \n$datasetOut\n";
        return ($datasetOut, "0");

    }elsif($datasetOut =~ /<(ns2:|ns3:)?message>(.*)<\/(ns2:|ns3:)?message>/ ){
        return("0", $2);

    }elsif($datasetOut =~ /<faultstring>(.*)<\/faultstring>/ ){
        return ("0", $1);

    }elsif($datasetOut =~ /Service Temporarily Unavailable/i ){
        #in case the service server is not available
        return("0", $datasetOut)
    }else{
        $retest = 1;

        if($testnum > 2 ){
            return("0", $datasetOut);
        }
    }

    if( $retest == 1 ){
        proxyNCBI("trash", $operation, $ac, $ns, $detail, $format, $testnum, $ncbitaxid);
    }

}


sub DESTROY{}

return 1;

