package DipProxy::ProxyEBI;

##==============================================================================
# $HeadURL::                                                                   $
# $Id::                                                                        $
# Version: $Rev::                                                              $
## ===============================================================================
#
# ProxyEBI.pm: 
#   object method proxyEBI ($operation, $ac, $ns, $detail, $testnum, $ncbitaxid)           
#   required args: $operation, $ac, $ns, $detail, $testnum
#                  $ncbitaxid is for picr query
#   return: ($dataset_string, $fault)
#
##=================================================================================

use DipProxy::ProxyNCBI;
use SOAP::Lite;
use XML::Dumper;
use Data::Dumper;
use XML::Simple;

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

sub proxyEBI{
    my $self = shift;
    my ($operation, $ac, $ns, $detail, $testnum, $format, $ncbitaxid) = @_;

    my $soap_server = SOAP::Lite
            ->uri($proxy_conf{EBI_URI})
            ->proxy($proxy_conf{EBI_SRV}, timeout=>300)
            ->default_ns($proxy_conf{EBI_RNS})
            ->on_action(sub{sprintf '%s/%s', @_})
            ->encoding('utf-8')
            ->readable('true')
            ->outputxml('true');

    my $dataset_out = "";

    if( $operation eq "getUniprot" ){
        $dataset_out =$soap_server->getUniprot(
            SOAP::Data->name('ns')->type(string=>"uniprot"),
            SOAP::Data->name('ac')->type(string=>$ac),
            SOAP::Data->name('format')->type(string=>$format),
            SOAP::Data->name('detail')->type(string=>$detail));
    } elsif ($operation eq "getPicrList" ){
        $dataset_out =$soap_server->getPicrList(
            SOAP::Data->name('ns')->type(string=>$ns),
            SOAP::Data->name('ac')->type(string=>$ac),
            SOAP::Data->name('format')->type(string=>$format),
            SOAP::Data->name('detail')->type(string=>$detail));
    } else {
        return ("0", "operation is invalid");
    }

    if($format eq "native"){
        print "ProxyEBI.pm: format is native.\n";
        return ($dataset_out, "0");
    }

    #print "EBI.pm: dataset_out is \n$dataset_out\n";

    my ($soap_out, $fault) = checkProxySoapOut($operation, $dataset_out, $ac, $ns, $detail,
                                     ++$testnum, $ncbitaxid);

    #print "EBI.pm: soap_out is \n$soap_out\n";
    if( $soap_out eq "0" ){
        return ($soap_out, $fault);
    }

    if( $operation eq "getPicrList" && $detail eq "full" && defined($ncbitaxid) 
        && $ncbitaxid ne "0" && $format eq "dxf"){
        #print "ProxyEBI.pm: before filterPicrDataset: \$soap_out is below:\n$soap_out\n";
        ($soap_out, $fault) = filterPicrDataset($soap_out, $ncbitaxid); 
        #print "ProxyEBI.pm after filterPicrDataset: \$soap_out is below: \n$soap_out\n";    
    }

    return ($soap_out, $fault);
}

sub checkProxySoapOut{
    my ($operation, $datasetOut, $ac, $ns, $detail, $testnum, $format, $ncbitaxid) = @_;

    my $retest = 0;

    if( $datasetOut =~ /<(ns2:|ns3:)?dataset/ ){
        if( $datasetOut !~ /<(ns2:|ns3:)?node/ ){
            return("0", "no hit record");
        }

        $datasetOut =~ s/<S:Envelope.*?><S:Body><(ns3:|ns2:)?result.*?><(ns2:|ns3)?dataset.*?>/<dataset>/;

        $datasetOut =~ s/<\/(ns3:|ns2:){0,1}result>//;
        $datasetOut =~ s/<\/S:Body><\/S:Envelope>//;
        $datasetOut =~ s/(ns2:|ns3:)//g;

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
        proxyEBI("trash", $operation, $ac, $ns, $detail, $testnum, $format, $ncbitaxid);
    }

}

#using ncbitaxid and uniprot and refseq to filter proxy-picr output
# for unknown taxon id with '-3', query proxyGetUniprot and proxyGetRefseq to figure out
sub filterPicrDataset{

    my($dataset_string, $ncbitaxid) = @_;

    my $simple = new XML::Simple();
    my $data = $simple->XMLin( $dataset_string, KeepRoot=>1, KeyAttr=>[], ParserOpts=>['XML::LibXML'] );
    my $node_xrefs_ref = $data->{dataset}->{node}->{xrefList}->{xref};
    $node_xrefs_ref = (ref $node_xrefs_ref eq 'ARRAY' )?$node_xrefs_ref:[$node_xrefs_ref];

    my $new_dataset_string = "<dataset>";

    $new_dataset_string .= "<node ac=\"$data->{dataset}->{node}->{ac}\" ";
    $new_dataset_string .= "id=\"$data->{dataset}->{node}->{id}\" ";
    $new_dataset_string .= "ns=\"$data->{dataset}->{node}->{ns}\">";

    $new_dataset_string .= "<type ac=\"dxf:0003\" name=\"protein\" ns=\"dxf\"/>";
    $new_dataset_string .= "<label>$data->{dataset}->{node}->{label}</label>";
    $new_dataset_string .= "<xrefList>";

    my $match = 0;
    foreach my $xref(@$node_xrefs_ref){
        #only keep ns with uniprot and refseq for type 'related-to'

        my $match_taxid = 0;
        if( $xref->{ns} !~ /(uniprot)|(refseq)/i ){
            next;
        }

        #print "ProxyEBI.pm: filterPicrDataset: taxonid is $xref->{node}->{xrefList}->{xref}->{ac}\n";
        #only keep specific taxid
        if( $xref->{node}->{xrefList}->{xref}->{ac} eq $ncbitaxid){
            $match = 1;
            $match_taxid = 1;
            $new_dataset_string .= "<xref type=\"related-to\" typeAc=\"dxf:0018\" ";
            $new_dataset_string .= "typeNs=\"dxf\" ac=\"$xref->{ac}\" ns=\"$xref->{ns}\"/>";
        }

        #print "filterPicrDataset: new_dataset_string before -3 is \n$new_dataset_string\n";

        #using ncbi proxy getTaxon to find unknow taxon id
        if( $xref->{node}->{xrefList}->{xref}->{ac} eq "-3"){
            my $unknown_dataset;
            my $fault;

            if($xref->{ns} =~ /uniprot/i ){
                ($unknown_dataset, $fault) = proxyEBI("trash", "getUniprot", $xref->{ac}, "uniprot", "base", 0, "dxf", "");
            }
            if($xref->{ns} =~ /refseq/i){
                my $proxyNCBI = DipProxy::ProxyNCBI->new();
                
                ($unknown_dataset, $fault) = $proxyNCBI->proxyNCBI("getRefseq", $xref->{ac}, "refseq", "base", 0, "dxf", "");
                #print "filterPicrDataset: after getRefseq: fault is \n$fault\n and unknown_dataset is below:\n$unknown_dataset\n";
                
            }

            if( $unknown_dataset eq "0"){
                return("0", $fault);
            }

            my ($unknown_ncbitaxid, $fault) = extractTaxonId($unknown_dataset);

            #print "filterPicrDataset: after extractTaxonId, fault is $fault and unknown_ncbitaxid is \n$unknown_ncbitaxid\n";
            if( $unknown_ncbitaxid eq "0"){
                return ("0", $fault);
            }

            if( $unknown_ncbitaxid eq $ncbitaxid ){
                $match = 1;
                $match_taxid = 1;
                $new_dataset_string .= "<xref type=\"related-to\" typeAc=\"dxf:0018\" ".
                                       "typeNs=\"dxf\" ac=\"$xref->{ac}\" ns=\"$xref->{ns}\"/>";
            }
            #print "filterPicrDataset: new_dataset_string after -3 is \n$new_dataset_string\n";    
        }

        

        if( $match_taxid ){
            $new_dataset_string .= "<node ac=\"$xref->{node}->{ac}\" ";
            $new_dataset_string .= "id=\"$xref->{node}->{id}\" ";
            $new_dataset_string .= "ns=\"$xref->{node}->{ns}\">";

            $new_dataset_string .= "<type ac=\"dxf:0003\" name=\"protein\" ns=\"dxf\"/>";
            $new_dataset_string .= "<label>$xref->{node}->{label}</label>";
            $new_dataset_string .= "<xrefList>";
            $new_dataset_string .= "<xref type=\"produced-by\" typeAc=\"dxf:0007\" ";
            $new_dataset_string .= "typeNs=\"dxf\" ac=\"$ncbitaxid\" ns=\"$xref->{node}->{xrefList}->{xref}->{ns}\"/>";
            $new_dataset_string .= "</xrefList>";
            $new_dataset_string .= "<attrList>";
            $new_dataset_string .= "<attr name=\"sequence\" ac=\"dip:0008\" ns=\"dip\">";
            $new_dataset_string .= "<value>$xref->{node}->{attrList}->{attr}->{value}</value>";
            $new_dataset_string .= "</attr></attrList></node>";

            $new_dataset_string .= "</xref>";
            #print "filterDataset: in match \$new_dataset_string is \n$new_dataset_string\n";
        }

    }

    undef($data); #release memory

    if(!$match){
        #picr did not returned any related-to with matched ncbitaxonid
        my $fault = "picr did not have any match with the node.";
        return ("0", $fault);
    }

    $new_dataset_string .= "</xrefList></node></dataset>";
    #print "filterDataset: finally new_dataset_string is \n$new_dataset_string\n";
    
    return ($new_dataset_string, "0");
}

#This function is used to extract taxon id from output of proxyGetUniprot 
#and proxyGetRefseq
sub extractTaxonId{
    my $unknown_dataset = shift;
    #parse the dataset and extract taxon id
    my ($throw, $node_xrefs_ref) = parseDataset($unknown_dataset);

    if( $throw eq "0" ){
        return ("0", $node_xrefs_ref);
    }

    foreach my $xref (@$node_xrefs_ref){
        if($xref->{ns} eq "ncbitaxid"){
            return ($xref->{ac}, "0");
        }
    }

    return ("0", $node_xrefs_ref);
}

sub parseDataset{
    my $dataset_string = shift;
    my $faultstring;

    $dataset_string =~ s/(dxf:|ns2:)//g;

    if( $dataset_string eq "" || $dataset_string !~ /<node ac=/ ){
        $fault = "parseDataset: the dataset is empty.";
        return ("0", $fault);
    }

    #check if the dataset contains more node
    if( $dataset_string =~ /<\/node>\s*<node/){
        $fault = "parseDataset: the dataset contains more nodes.";
        return ("0", $fault);
    }

    my $simple = new XML::Simple();
    my $data = $simple->XMLin( $dataset_string, KeepRoot=>1, KeyAttr=>[], ParserOpts=>['XML::LibXML'] );

    my $node_ac = $data->{dataset}->{node}->{ac};

    my $node_xrefs_ref = $data->{dataset}->{node}->{xrefList}->{xref};
    undef($data); #release memory

    $node_xrefs_ref = (ref $node_xrefs_ref eq 'ARRAY' )?$node_xrefs_ref:[$node_xrefs_ref];

    return ($node_ac, $node_xrefs_ref);
}

sub DESTROY{}
return 1;

