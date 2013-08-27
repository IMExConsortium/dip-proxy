#!/usr/bin/perl
use SOAP::Lite;
use XML::XPath;
use XML::XPath::XMLParser;

my $URL= "http://dip.mbi.ucla.edu/dip-proxy/ws/soap";
#my $URL= "http://dip.doe-mbi.ucla.edu/dip-proxy";
my $PURL= "http://10.1.200.%%%:8080/dip-proxy/ws/soap";

my ( $ip, $soapservice, $mth, $ns, $ac, $format, $det, $provider, $service );

$format="native";
$det="full";

for( my $i = 0; $i<@ARGV; $i++ ) {

    if ( $ARGV[$i] =~ /IP=(\d+)/ ) {
        $ip=$1;
        $URL=$PURL;
        $URL=~s/%%%/$ip/;
    }
    
    if ( $ARGV[$i] =~ /IP=(\d+\.\d+\.\d+\.\d+)/ ) {
        $ip=$1;
        $URL=$PURL;
        $URL=~s/10.1.200.%%%/$ip/;
    }
    
    if ( $ARGV[$i] =~ /SOAPSERVICE=(.+)/ ) {
        $soapservice=$1;
    }   

    if ( $ARGV[$i] =~ /PROVIDER=(.+)/ ) {
        $provider=$1;
    }   
    if ( $ARGV[$i] =~ /SERVICE=(.+)/ ) {
        $service=$1;
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
        $det=$1;
    }   
}
my $som="";
my $rns ="";    

if( $soapservice eq "DHT" ){

    $rns ="http://mbi.ucla.edu/proxy/dht";    
    my $url=$URL."/dht-service";

    my $client = SOAP::Lite->new(proxy => $url);
    $client->on_action(sub { "" });
    
    if($mth eq "getDhtRecord"){
        
        $som=$client->uri($url)
            ->default_ns($rns)
            ->outputxml('true')
            -> getDhtRecord(SOAP::Data->name("provider" => $prv),
                            SOAP::Data->name("service" => $format),
                            SOAP::Data->name("ns" => $ns),
                            SOAP::Data->name("ac" => $ac) );
	#print $som,"\n";
    }
}

if( $soapservice eq "EBI" ){

    $rns ="http://mbi.ucla.edu/proxy/ebi";    
    my $url=$URL."/ebi-service"; 
    
    my $client = SOAP::Lite->new(proxy => $url);
    $client->on_action(sub { "" });

    if($mth eq "getUniprot"){
	
	if($prv eq "" ){
            $prv="full";
        }
        print "URL: $url\n";
        print "NS=".$ns." AC=".$ac."\n";
  
        $som=$client->uri($url)
	    ->default_ns($rns)
	    ->outputxml('true')
	    -> getUniprot(SOAP::Data->name("ns" => $ns),
			  SOAP::Data->name("ac" => $ac),
                          SOAP::Data->name("format" => $format),
			  SOAP::Data->name("detail" => $det));
    }

    if($mth eq "getPicrList"){

	if($prv eq "" ){
            $prv="full";
        }
        
        $som=$client->uri($url)
	    ->default_ns($rns)
	    ->outputxml('true')
	    -> getPicrList(SOAP::Data->name("ns" => $ns),
			   SOAP::Data->name("ac" => $ac),
                           SOAP::Data->name("format" => $format),
			   SOAP::Data->name("detail" => $det));
    }
}

if($soapservice eq "NCBI"){
    $rns ="http://mbi.ucla.edu/proxy/ncbi";    
    my $url=$URL."/ncbi-service"; 
    
    my $client = SOAP::Lite->new(proxy => $url);
    $client->on_action(sub { "" });
    
    if($mth eq "getPubmedArticle"){
        
        $som=$client->uri($url)
	    ->default_ns($rns)
	    ->outputxml('true')
	    -> getPubmedArticle(SOAP::Data->name("ns" => $ns),
                                SOAP::Data->name("ac" => $ac),
                                SOAP::Data->name("format" => $format),
                                SOAP::Data->name("detail" => $det));
    }
    
    if($mth eq "getJournal"){
        
        $som=$client->uri($url)
	    ->default_ns($rns)
	    ->outputxml('true')
	    -> getJournal(SOAP::Data->name("ns" => $ns),
                          SOAP::Data->name("ac" => $ac),
                          SOAP::Data->name("format" => $format),
                          SOAP::Data->name("detail" => $det));
    }
    
    if($mth eq "getRefseq"){
        
        $som=$client->uri($url)
	    ->default_ns($rns)
	    ->outputxml('true')
	    -> getRefseq(SOAP::Data->name("ns" => $ns),
			 SOAP::Data->name("ac" => $ac),
                         SOAP::Data->name("format" => $format),
			 SOAP::Data->name("detail" => $det));
    }
    
    if($mth eq "getGene"){
        
        $som=$client->uri($url)
	    ->default_ns($rns)
	    ->outputxml('true')
	    -> getGene(SOAP::Data->name("ns" => $ns),
		       SOAP::Data->name("ac" => $ac),
                       SOAP::Data->name("format" => $format),
		       SOAP::Data->name("detail" => $det));
    }
    
    if($mth eq "getTaxon"){
        
        $som=$client->uri($url)
	    ->default_ns($rns)
	    ->outputxml('true')
	    -> getTaxon(SOAP::Data->name("ns" => $ns),
			SOAP::Data->name("ac" => $ac),
                        SOAP::Data->name("format" => $format),
			SOAP::Data->name("detail" => $det));
    }
}

if($soapservice eq "PRL"){

    $rns ="http://mbi.ucla.edu/proxy/prolinks";    
    my $url=$URL."/prolinks-service"; 
    
    my $client = SOAP::Lite->new(proxy => $url);
    $client->on_action(sub { "" });
    
    if($mth eq "getProlinks"){
        
        $som=$client->uri($url)
	    ->default_ns($rns)
	    ->outputxml('true')
	    -> getProlinks(SOAP::Data->name("ns" => $ns),
                           SOAP::Data->name("ac" => $ac),
                           SOAP::Data->name("format" => $format),
                           SOAP::Data->name("detail" => $det));
    }
}

if($soapservice eq "DIP"){
    
    $rns ="http://mbi.ucla.edu/proxy/dip";    
    my $url=$URL."/dip-service"; 
    
    my $client = SOAP::Lite->new(proxy => $url);
    $client->on_action(sub { "" });
    
    if($mth eq "getDipRecord"){
        
        print "OP: $mth  NS: $ns AC: $ac format: $format detail: $det\n";
        
        $som=$client->uri($url)
	    ->default_ns($rns)
	    ->outputxml('true')
	    -> getDipRecord(SOAP::Data->name("ns" => $ns),
                            SOAP::Data->name("ac" => $ac),
                            SOAP::Data->name("format" => $format),
                            SOAP::Data->name("detail" => $det));
    }
}

if( $soapservice eq "PROXY" ){
    $rns ="http://mbi.ucla.edu/proxy";    
    my $url=$URL."/proxy-service"; 

    my $client = SOAP::Lite->new(proxy => $url);
    $client->on_action(sub { "" });

    print "OP: getRecord  PRV: $provider SRV: $service".
        "NS: $ns AC: $ac format: $format detail: $det\n";
    
    $som=$client->uri($url)
        ->default_ns($rns)
        ->outputxml('true')
        -> getRecord(SOAP::Data->name("provider" => $provider),
                     SOAP::Data->name("service" => $service),
                     SOAP::Data->name("ns" => $ns),
                     SOAP::Data->name("ac" => $ac),
                     SOAP::Data->name("format" => $format),
                     SOAP::Data->name("detail" => $det));
}

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

