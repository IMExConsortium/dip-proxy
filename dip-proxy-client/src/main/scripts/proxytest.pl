#!/usr/bin/perl
use SOAP::Lite;
use XML::XPath;
use XML::XPath::XMLParser;

my $URL= "http://dip.doe-mbi.ucla.edu/dip-proxy";
my $PURL= "http://10.1.200.%%%:8080/dip-proxy";

my ( $ip, $srv, $mth, $ns, $ac, $format, $prv ) = @ARGV;

if ( $ip > 0 ) {
    $URL=$PURL;
    $URL=~s/%%%/$ip/;
} else {
    ( $srv, $mth, $ns, $ac, $format, $prv ) = @ARGV;
}

for( my $i = 0; $i<@ARGV; $i++ ) {

    if ( $ARGV[$i] =~ /IP=(\d+)/ ) {
        $ip=$1;
        $URL=$PURL;
        $URL=~s/%%%/$ip/;
    }
    
    if ( $ARGV[$i] =~ /IP=(\d+\.\d+\.\d+\.\d+)/ ) {
        $ip=$1;
        $URL=$PURL;
        $URL=~s/10.1.1.%%%/$ip/;
    }
    
    if ( $ARGV[$i] =~ /SRV=(.+)/ ) {
        $srv=$1;
    }   

    if ( $ARGV[$i] =~ /OP=(.+)/ ) {
        $mth=$1;
    }   

    if ( $ARGV[$i] =~ /NS=(.+)/ ) {
        $ns=$1;
    }   

    if ( $ARGV[$i] =~ /AC=(.+)/ ) {
        $ac=$1;
    }   

    if ( $ARGV[$i] =~ /FORMAT=(.+)/ ) {
        $format=$1;
    }   

    if ( $ARGV[$i] =~ /DET=(.+)/ ) {
        $prv=$1;
    }   

    if ( $ARGV[$i] =~ /PRV=(.+)/ ) {
        $prv=$1;
    }   
}


print "URL: $URL\n";

my $som="";
my $rns ="";    




if($mth eq "get-record"){

    $rns ="http://mbi.ucla.edu/proxy";    
    my $url=$URL."/proxy-service";

    print "URL:".$url." NS=".$rns." PRV=".$prv." SRV=".$srv." NS=".$ns." AC=".$ac."\n";

    $som=SOAP::Lite->uri($url)
        ->proxy($url)
        ->default_ns($rns)
        ->outputxml('true')
        -> getRecord(SOAP::Data->name("provider" => $prv),
                     SOAP::Data->name("service" => $srv),
                     SOAP::Data->name("ns" => $ns),
                     SOAP::Data->name("ac" => $ac),
                     SOAP::Data->name("format" => $format));
    print $som,"\n";
}








if($srv eq "DHT"){

    $rns ="http://mbi.ucla.edu/proxy/dht";    
    my $url=$URL."/dht-service";

    if($mth eq "getDhtRecord"){

        $som=SOAP::Lite->uri($url)
            ->proxy($url)
            ->default_ns($rns)
            ->outputxml('true')
            -> getDhtRecord(SOAP::Data->name("provider" => $prv),
                            SOAP::Data->name("service" => $format),
                            SOAP::Data->name("ns" => $ns),
                            SOAP::Data->name("ac" => $ac) );
	print $som,"\n";
    }
}

if($srv eq "EBI"){

    $rns ="http://mbi.ucla.edu/proxy/ebi";    
    my $url=$URL."/ebi-service"; 
    
    if($mth eq "getUniprot"){
	
	if($prv eq "" ){
            $prv="full";
        }
        
	$som=SOAP::Lite->uri($url)
	    ->proxy($url)
	    ->default_ns($rns)
	    ->outputxml('true')
	    -> getUniprot(SOAP::Data->name("ns" => $ns),
			  SOAP::Data->name("ac" => $ac),
                          SOAP::Data->name("format" => $format),
			  SOAP::Data->name("detail" => $prv));
    }

    if($mth eq "getPicrList"){

	if($prv eq "" ){
            $prv="full";
        }

	$som=SOAP::Lite->uri($url)
	    ->proxy($url)
	    ->default_ns($rns)
	    ->outputxml('true')
	    -> getPicrList(SOAP::Data->name("ns" => $ns),
			   SOAP::Data->name("ac" => $ac),
                           SOAP::Data->name("format" => $format),
			   SOAP::Data->name("detail" => $prv));
    }
}

if($srv eq "NCBI"){
    $rns ="http://mbi.ucla.edu/proxy/ncbi";    
    my $url=$URL."/ncbi-service"; 
    
    if($prv eq "" ){
        $prv="full";
    }
    
    if($mth eq "getPubmedArticle"){
	$som=SOAP::Lite->uri($url)
	    ->proxy($url)
	    ->default_ns($rns)
	    ->outputxml('true')
	    -> getPubmedArticle(SOAP::Data->name("ns" => $ns),
                                SOAP::Data->name("ac" => $ac),
                                SOAP::Data->name("format" => $format),
                                SOAP::Data->name("detail" => $prv));
    }

    if($mth eq "getJournal"){
	$som=SOAP::Lite->uri($url)
	    ->proxy($url)
	    ->default_ns($rns)
	    ->outputxml('true')
	    -> getJournal(SOAP::Data->name("ns" => $ns),
                                SOAP::Data->name("ac" => $ac),
                                SOAP::Data->name("format" => $format),
                                SOAP::Data->name("detail" => $prv));
    }

    if($mth eq "getRefseq"){
	
	$som=SOAP::Lite->uri($url)
	    ->proxy($url)
	    ->default_ns($rns)
	    ->outputxml('true')
	    -> getRefseq(SOAP::Data->name("ns" => $ns),
			 SOAP::Data->name("ac" => $ac),
                         SOAP::Data->name("format" => $format),
			 SOAP::Data->name("detail" => $prv));

        print "SOM: $url\n$rns\n $prv \n";
    }

    if($mth eq "getGene"){
	
	$som=SOAP::Lite->uri($url)
	    ->proxy($url)
	    ->default_ns($rns)
	    ->outputxml('true')
	    -> getGene(SOAP::Data->name("ns" => $ns),
		       SOAP::Data->name("ac" => $ac),
                       SOAP::Data->name("format" => $format),
		       SOAP::Data->name("detail" => $prv));
    }

    if($mth eq "getTaxon"){
	
	$som=SOAP::Lite->uri($url)
	    ->proxy($url)
	    ->default_ns($rns)
	    ->outputxml('true')
	    -> getTaxon(SOAP::Data->name("ns" => $ns),
			SOAP::Data->name("ac" => $ac),
                        SOAP::Data->name("format" => $format),
			SOAP::Data->name("detail" => $prv));
    }
}

if($srv eq "PRL"){

    $rns ="http://mbi.ucla.edu/proxy/prolinks";    
    my $url=$URL."/prolinks-service"; 
    
    if($prv eq "" ){
        $prv="full";
    }
    
    if($mth eq "getProlinks"){
	
	$som=SOAP::Lite->uri($url)
	    ->proxy($url)
	    ->default_ns($rns)
	    ->outputxml('true')
	    -> getProlinks(SOAP::Data->name("ns" => $ns),
                           SOAP::Data->name("ac" => $ac),
                           SOAP::Data->name("format" => $format),
                           SOAP::Data->name("detail" => $prv));
    }
}

if($srv eq "DIP"){

    $rns ="http://mbi.ucla.edu/proxy/dip";    
    my $url=$URL."/dip-service"; 
    
    if($prv eq "" ){
        $prv="full";
    }
    
    if($mth eq "getDipRecord"){
	
	$som=SOAP::Lite->uri($url)
	    ->proxy($url)
	    ->default_ns($rns)
	    ->outputxml('true')
	    -> getDipRecord(SOAP::Data->name("ns" => $ns),
                           SOAP::Data->name("ac" => $ac),
                           SOAP::Data->name("format" => $format),
                           SOAP::Data->name("detail" => $prv));
    }
}


print $som,"\n";

my $xp_som = XML::XPath->new(xml=>$som);
$xp_som->set_namespace("rns",$rns);

my $nodeset;

if( $format eq "native" ){
    $nodeset = $xp_som->find('//rns:nativerecord/text()'); # find all paragraphs
    foreach my $node ($nodeset->get_nodelist) {
        print "FOUND NATIVE\n\n",
        $node->string_value(),
        "\n\n";
    }
} else {
    $xp_som->set_namespace("rns","http://dip.doe-mbi.ucla.edu/services/dxf14");

    $nodeset = $xp_som->find('//rns:dataset'); # find all paragraphs
    foreach my $node ($nodeset->get_nodelist) {
        print "FOUND DXF\n\n",
        XML::XPath::XMLParser::as_string($node),"\n";
    }
}

