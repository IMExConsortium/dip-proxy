package edu.ucla.mbi.proxy.ncbi;

/*==============================================================================
 *
 * NcbiCachingImpl - NCBI Database access implemented 
 * through efetch SOAP
 *
 *=========================================================================== */

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.datatype.XMLGregorianCalendar;

import javax.jws.WebService;
import javax.xml.ws.Holder;

import edu.ucla.mbi.dxf14.*;
import edu.ucla.mbi.proxy.*;
import edu.ucla.mbi.fault.*;
import edu.ucla.mbi.util.TimeStamp;

import edu.ucla.mbi.cache.NativeRecord;
import edu.ucla.mbi.proxy.context.*;

@WebService(endpointInterface="edu.ucla.mbi.proxy.NcbiProxyPort")

public class NcbiCachingImpl extends ConfigurableServer 
    implements NcbiProxyPort {

    private CachingService cachingSrv;

    public void setCachingService ( CachingService service ) {
        this.cachingSrv = service;
    }    

    // Fetch journal from nlm
    
    public Result getJournal( GetJournal request )
        throws ProxyFault{
        
        ObjectFactory of = new ObjectFactory();
        Result result = of.createResult();
        
        String provider = "NCBI";
	    String service = "nlm";
        
	    Log log = LogFactory.getLog( NcbiCachingImpl.class );
	    log.info( "getJournal " +
                  " NS=" + request.getNs() + 
                  " AC=" + request.getAc() +
                  " FT=" + request.getFormat() +
                  " DT=" + request.getDetail() );
        
        if( !request.getNs().equalsIgnoreCase( "nlmid" ) ){
	        log.info( " forcing nlm as ns" ); 
	        request.setNs( "nlmid" ); 
	    }	

	    if( request.getAc() == null || request.getAc().equals( "" ) ){
	        log.info( "missing accession" );
            throw FaultFactory.newInstance( Fault.MISSING_ID );
	    }
        
	    if( request.getDetail() == null ){
            request.setDetail( "stub" );            
	    }else if( request.getDetail().equalsIgnoreCase( "short" ) || 
                   request.getDetail().equalsIgnoreCase( "stub" ) ){
            request.setDetail( "stub" );
	    }else if( request.getDetail().equalsIgnoreCase( "base" ) ){
            request.setDetail( "base" );
    
        }else {
	        request.setDetail( "full" );
	    } 
       
        try{

            if ( request.getFormat() == null || request.getFormat().equals( "" ) 
                 || request.getFormat().equalsIgnoreCase( "dxf" ) 
                 || request.getFormat().equalsIgnoreCase( "both" ) ) {
                
	       
                log.info( "getting dxf " + request.getDetail() );

                DatasetType dtsresult 
                    = cachingSrv.getDatasetType( provider, service, 
                                                 request.getNs(),
                                                 request.getAc(),
                                                 request.getDetail() );
                if ( dtsresult != null ) {
                    result.setDataset( dtsresult ) ;
                } else {
                    log.info("return dataset is null ");
                    throw FaultFactory.newInstance( Fault.NO_RECORD );
		        }
            }
	    
            if( request.getFormat() != null 
                && ( request.getFormat().equalsIgnoreCase( "native" ) 
                     || request.getFormat().equalsIgnoreCase( "both" ) ) ){
                
                NativeRecord natRec = 
                    cachingSrv.getNative( provider, service,
                                          request.getNs(),
                                          request.getAc() );
                
		        if ( natRec != null && natRec.getNativeXml() != null &&
                     natRec.getNativeXml().length() > 0 ) {
                    result.setNativerecord( natRec.getNativeXml() );
                    result.setTimestamp( TimeStamp.toXmlDate( natRec.getQueryTime() ));
                }else{
                    log.info("return dataset is null ");
                    throw FaultFactory.newInstance( Fault.NO_RECORD );
		        }                
            }

        }catch( ProxyFault fault ){
            String message = fault.getFaultInfo().getMessage();
            log.warn( "NcbiCachingImpl: ServiceException: message= " + message);
            
            throw fault;
            
        }catch( Exception e ){
            log.warn( "NcbiCachingImpl: " + e.toString() );
            ServiceFault fault = new ServiceFault();
            throw FaultFactory.newInstance( Fault.UNKNOWN );
        }
 
        return result;        
    }

    // Fetch article from pubmed
         
    public Result getPubmedArticle( GetPubmedArticle request )
        throws ProxyFault {
        
        String provider = "NCBI";
        String service = "pubmed";
        
        Log log = LogFactory.getLog( this.getClass() );
        log.info( "getPubmedArticle " +
                  " NS=" + request.getNs() +
                  " AC=" + request.getAc() +
                  " DT=" + request.getDetail() );
        
        ObjectFactory of = new ObjectFactory();
        Result result  = of.createResult();
        
        if( request.getNs() == null ||
            !request.getNs().equalsIgnoreCase( "pmid" ) ){
            log.info( " forcing pubmed as ns" ); 
            request.setNs( "pmid" ); 
        }	
        
        if( request.getAc() == null || request.getAc().equals( "" ) ){
            log.info( "missing accession" );
            throw FaultFactory.newInstance( Fault.MISSING_ID );
        }
        
        if( request.getDetail() == null ){
            request.setDetail( "stub" );
        }else if( request.getDetail().equalsIgnoreCase( "short" ) || 
                  request.getDetail().equalsIgnoreCase( "stub" ) ){
            request.setDetail( "stub" );
        }else if( request.getDetail().equalsIgnoreCase( "base" ) ){
            request.setDetail( "base" );
        }else{
            request.setDetail( "full" );
        } 
        
        log.info( "NcbiCachingImpl: input arguments OK");
        
        try {
            
            if( request.getFormat() == null
                || request.getFormat().equals( "" ) 
                || request.getFormat().equalsIgnoreCase( "dxf" ) 
                || request.getFormat().equalsIgnoreCase( "both" )
                ){
                
                log.info( "NcbiCachingImpl: looking for dxf record" );
                
                DatasetType dstresult
                    = cachingSrv.getDatasetType( provider, service,
                                                 request.getNs(),
                                                 request.getAc(),
                                                 request.getDetail() );
                if ( dstresult != null ) {
                    result.setDataset( dstresult );
                } else {
                    log.info("return dataset is null ");
                    throw FaultFactory.newInstance( Fault.NO_RECORD );
                }
                
                log.info( "NcbiCachingImpl: dxf record found");                
            }
            
            if( request.getFormat() != null 
                && ( request.getFormat().equalsIgnoreCase( "native" ) 
                     || request.getFormat().equalsIgnoreCase( "both" ) )
                ){
                
                NativeRecord natRec = 
                    cachingSrv.getNative( provider, service,
                                          request.getNs(),
                                          request.getAc() );
                
		        if( natRec != null && natRec.getNativeXml() != null &&
                    natRec.getNativeXml().length() > 0 ){
                    result.setNativerecord( natRec.getNativeXml() );
                    result.setTimestamp( TimeStamp.toXmlDate( natRec.getQueryTime() ));
		        } else {
                    log.info("return dataset is null ");
                    throw FaultFactory.newInstance( Fault.NO_RECORD );
		        }                
            }
            
        }catch( ProxyFault fault ){
            String message = fault.getFaultInfo().getMessage();
            log.warn( "NcbiCachingImpl: ServiceException: message= " + message);
            
            throw fault;
            
        } catch ( Exception e ) {
            log.warn( "NcbiCachingImpl: " + e.toString() );
            throw FaultFactory.newInstance( Fault.UNKNOWN );
        }
        
        log.info( "NcbiCachingImpl: DONE");
        
        return result;
    }
    
    public Result getRefseq( GetRefseq request )
        throws ProxyFault {
        
        String provider = "NCBI";
        String service = "refseq";

        ObjectFactory of = new ObjectFactory();
        Result result  = of.createResult();
        
	    Log log = LogFactory.getLog( NcbiCachingImpl.class );
	    log.info( "getRefseq " + 
                  " NS=" + request.getNs() +
                  " AC=" + request.getAc() +
                  " DT=" + request.getDetail() );
        
       	if( !request.getNs().equalsIgnoreCase( "refseq" ) ){
	        log.info( "forcing refseq as ns" ); 
	        request.setNs( "refseq" );
        }	
        
	    if( request.getAc() == null ||
            request.getAc().equals( "" ) ){
            log.info( " missing accession" );
            throw FaultFactory.newInstance( Fault.MISSING_ID );
        }   
        
	    if( request.getDetail() == null ){
	        request.setDetail( "stub" );
	    }else if( request.getDetail().equalsIgnoreCase("short") || 
                  request.getDetail().equalsIgnoreCase("stub") ){
            
	        request.setDetail( "stub" );
            
        }else if( request.getDetail().equalsIgnoreCase( "base" ) ){
            request.setDetail( "base" );
        }else{
	        request.setDetail( "full" );
	    }   
        
	    try {
            
            if( request.getFormat() == null
                || request.getFormat().equals( "" ) 
                || request.getFormat().equalsIgnoreCase( "dxf" ) 
                || request.getFormat().equalsIgnoreCase( "both" ) ){
                
                DatasetType dstresult
                    = cachingSrv.getDatasetType( provider, service,
                                                 request.getNs(),
                                                 request.getAc(),
                                                 request.getDetail() );
                if ( dstresult != null ) {
                    result.setDataset( dstresult );     
                } else {
		            log.info( "return dataset is null " );
                    throw FaultFactory.newInstance( Fault.NO_RECORD );
		        }      
            }
            
            if( request.getFormat() != null 
                 && ( request.getFormat().equalsIgnoreCase( "native" ) 
                      || request.getFormat().equalsIgnoreCase( "both" ) )
                ){
                
                NativeRecord natRec = 
                    cachingSrv.getNative( provider, service,
                                          request.getNs(),
                                          request.getAc() );
                if( natRec != null ){   
		            result.setNativerecord( natRec.getNativeXml() );
                    result.setTimestamp( TimeStamp.toXmlDate( natRec.getCreateTime() ) );
                } else {
                    log.info("NcbiCachingImpl: return dataset is null ");
                    throw FaultFactory.newInstance( Fault.NO_RECORD );
		        }
            }
            
        }catch( ProxyFault fault ){
            String message = fault.getFaultInfo().getMessage();
            log.warn( "NcbiCachingImpl: ServiceException: message= " + message);
            
            throw fault;
            
        } catch ( Exception e ) {
            log.warn( "NcbiCachingImpl: " + e.toString() );
            throw FaultFactory.newInstance( Fault.UNKNOWN );
        }
        
        return result;
    }
    
    public Result getGene( GetGene request )
        throws ProxyFault {
	
        String provider = "NCBI";
        String service = "entrezgene";

        ObjectFactory of = new ObjectFactory();
        Result result  = of.createResult();
        
        Log log = LogFactory.getLog( NcbiCachingImpl.class );
	    log.info( "getGene " + 
                  "NS=" + request.getNs() +
                  " AC=" + request.getAc() +
                  " DT=" + request.getDetail() );
	
	    if( !request.getNs().equalsIgnoreCase( "entrezgene" ) ){
	        log.info( " forcing entrezgene as ns" ); 
	        request.setNs( "entrezgene" );
	    }	
        
	    if( request.getAc() == null || request.getAc().equals( "" ) ){
	        log.info( "missing accession" );
            throw FaultFactory.newInstance( Fault.MISSING_ID );
	    }
	
        if( request.getDetail() == null ){
            request.setDetail( "stub" );
        }else if( request.getDetail().equalsIgnoreCase("short") ||
                  request.getDetail().equalsIgnoreCase("stub") ){
            
            request.setDetail( "stub" );
            
        }else if( request.getDetail().equalsIgnoreCase( "base" ) ){
            request.setDetail( "base" );
            
        } else {
            request.setDetail( "full" );
        }
        
	    try {
            
            if( request.getFormat() == null
                || request.getFormat().equals( "" ) 
                || request.getFormat().equalsIgnoreCase( "dxf" ) 
                || request.getFormat().equalsIgnoreCase( "both" )
                ){
                
                DatasetType dstresult
                    = cachingSrv.getDatasetType( provider, service,
                                                 request.getNs(),
                                                 request.getAc(),
                                                 request.getDetail() );
                if ( dstresult != null ) {
                    result.setDataset( dstresult );
                } else {
                    log.info( "return dataset is null " );
		            throw FaultFactory.newInstance( Fault.NO_RECORD );
		        }
            }
            
            if( request.getFormat() != null 
                && ( request.getFormat().equalsIgnoreCase( "native" ) 
                     || request.getFormat().equalsIgnoreCase( "both" ) )
                ){
                
		        NativeRecord natRec =
                    cachingSrv.getNative( provider, service,
                                          request.getNs(),
                                          request.getAc() );

                if ( natRec != null ) {
                    result.setNativerecord( natRec.getNativeXml() );
                    result.setTimestamp( TimeStamp.toXmlDate( natRec.getCreateTime() ) );
                } else {
                    log.info("NcbiCachingImpl: return dataset is null ");
                    throw FaultFactory.newInstance( Fault.NO_RECORD );
                }
            }

	    }catch ( ProxyFault fault ) {
            String message = fault.getFaultInfo().getMessage();
            log.warn( "NcbiCachingImpl: ServiceException: message= " + message);

            throw fault;

        } catch ( Exception e ) {
            log.warn( "NcbiCachingImpl: " + e.toString() );
            throw FaultFactory.newInstance( Fault.UNKNOWN );
        }
 
        return result;
    }

    
    //---------------------------------------------------------------------    
    
    public Result getTaxon( GetTaxon request )
        throws ProxyFault{
	
        String provider = "NCBI";
        String service = "taxon";

        ObjectFactory of = new ObjectFactory();
        Result result  = of.createResult();
        
	    Log log = LogFactory.getLog( NcbiCachingImpl.class );
	    log.info( "getTaxon " + 
                  "NS=" + request.getNs() +
                  " AC=" + request.getAc() +
                  " DT=" + request.getDetail() );
        
	    if ( !request.getNs().equalsIgnoreCase( "ncbitaxid" ) ){
	        log.info( "forcing ncbitaxid as ns" ); 
	        request.setNs( "ncbitaxid" );
	    }	
        
	    if(  request.getAc() == null ||  request.getAc().equals( "" ) ){   
	        log.info( "missing accession" );
            throw FaultFactory.newInstance( Fault.MISSING_ID );
	    }
	
        if( request.getDetail() == null ) {
            request.setDetail( "stub" );
        }else if( request.getDetail().equalsIgnoreCase("short") ||
                   request.getDetail().equalsIgnoreCase("stub") ){
            
            request.setDetail( "stub" );

        }else if( request.getDetail().equalsIgnoreCase( "base" ) ){
            request.setDetail( "base" );

        } else {
            request.setDetail( "full" );
        }
        
	    try {

            if( request.getFormat() == null
                || request.getFormat().equals( "" ) 
                || request.getFormat().equalsIgnoreCase( "dxf" ) 
                || request.getFormat().equalsIgnoreCase( "both" )
                ){
                
                DatasetType dstresult
                    = cachingSrv.getDatasetType( provider, service,
                                                 request.getNs(),
                                                 request.getAc(),
                                                 request.getDetail() );

                if( dstresult != null ){
                    result.setDataset( dstresult );
                }else{  
                    log.info( "return dataset is null " );
                    throw FaultFactory.newInstance( Fault.NO_RECORD );
                }
            }
            
            if( request.getFormat() != null 
                && ( request.getFormat().equalsIgnoreCase( "native" ) 
                     || request.getFormat().equalsIgnoreCase( "both" ) )
                ){

                NativeRecord natRec =
                    cachingSrv.getNative( provider, service,
                                          request.getNs(),
                                          request.getAc() );

                if ( natRec != null ) {
                    result.setNativerecord( natRec.getNativeXml() );
                    result.setTimestamp( TimeStamp.toXmlDate( natRec.getCreateTime() ) );
                } else {
                    log.info("NcbiCachingImpl: return dataset is null ");
                    throw FaultFactory.newInstance( Fault.NO_RECORD );
                }
            }
            
	    }catch ( ProxyFault fault ) {
            String message = fault.getFaultInfo().getMessage();
            log.warn( "NcbiCachingImpl: ServiceException: message= " + message);
            
            throw fault;

        } catch ( Exception e ) {
            log.warn( "NcbiCachingImpl: " + e.toString() );
            throw FaultFactory.newInstance( Fault.UNKNOWN );
        }
        
        return result;
    }

}
