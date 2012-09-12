package edu.ucla.mbi.proxy.ncbi;

/*==============================================================================
 * $HeadURL::                                                                  $
 * $Id::                                                                       $
 * Version: $Rev::                                                             $
 *==============================================================================
 *
 * NcbiCachingImpl - NCBI Database access implemented 
 * through efetch SOAP
 *
 *  NOTE: Modify gen-src/axis2/ncbi/resources/services.xml to use
 *  this instead of default NcbiPublicSkeleton.
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
import edu.ucla.mbi.server.WSContext;

import edu.ucla.mbi.cache.NativeRecord;
import edu.ucla.mbi.proxy.router.Router;

@WebService(endpointInterface="edu.ucla.mbi.proxy.NcbiProxyPort")

public class NcbiCachingImpl implements NcbiProxyPort {
    
    /* 
     * Fetch journal from nlm
     */

    public void getJournal( String ns, String ac, String match,
                            String detail, String format,
                            String client, Integer depth,
                            Holder<XMLGregorianCalendar> timestamp,
                            Holder<DatasetType> dataset,
                            Holder<String> nativerecord 
                            ) throws ProxyFault {
        
        String provider = "NCBI";
	    String service = "nlm";
       
	    Log log = LogFactory.getLog( NcbiCachingImpl.class );
	    log.info( "getJournal " +
		            " NS=" + ns + " AC=" + ac + " DT=" + detail );
	
        if ( !ns.equalsIgnoreCase( "nlmid" ) ) {
	        log.info( " forcing nlm as ns" ); 
	        ns = "nlmid"; 
	    }	

	    if ( ac == null || ac.equals( "" ) ) {
	        log.info( "missing accession" );
            throw FaultFactory.newInstance( Fault.MISSING_ID );
	    }
	
	    if ( detail == null ) {
	        detail = "stub";

	    } else if ( detail.equalsIgnoreCase( "short" ) || 
		            detail.equalsIgnoreCase( "stub" ) ) {

	        detail = "stub";
	    } else if ( detail.equalsIgnoreCase( "base" ) ){
            detail = "base";
    
        } else {
	        detail = "full";
	    } 
        
        try {
            Router router = 
                WSContext.getServerContext( provider ).createRouter() ;

            CachingService cachingSrv = 
                new CachingService( provider, router,
                                    WSContext.getServerContext( provider ) );
            
            if ( format == null || format.equals( "" ) ||
                 format.equalsIgnoreCase( "dxf" ) ||
                 format.equalsIgnoreCase( "both" ) ) {
                
                DatasetType result = 
                    cachingSrv.getDxf( provider, service, ns, ac, detail );
                if ( result != null ) {
                    dataset.value = result ;
                } else {
                    log.info("return dataset is null ");
                    throw FaultFactory.newInstance( Fault.NO_RECORD );
		        }
            }
	    
            if ( format != null &&
                 ( format.equalsIgnoreCase( "native" ) ||
                   format.equalsIgnoreCase( "both" ) )
                 ) {
                
                NativeRecord natRec = 
                    cachingSrv.getNative( provider, service, ns, ac );
		
		        if ( natRec != null && natRec.getNativeXml() != null &&
                     natRec.getNativeXml().length() > 0 ) {
                    nativerecord.value = natRec.getNativeXml();
                    timestamp.value =
                        TimeStamp.toXmlDate( natRec.getQueryTime() );
		        } else {
                    log.info("return dataset is null ");
                    throw FaultFactory.newInstance( Fault.NO_RECORD );
		        }                
            }
            
        }catch ( ProxyFault fault ) {
            String message = fault.getFaultInfo().getMessage();
            log.warn( "NcbiCachingImpl: ServiceException: message= " + message);

            throw fault;

        } catch ( Exception e ) {
            log.warn( "NcbiCachingImpl: " + e.toString() );
            ServiceFault fault = new ServiceFault();
            throw FaultFactory.newInstance( Fault.UNKNOWN );
        }
 
        return;

        
    }

    /* 
     * Fetch article from pubmed
     */
    
    public void getPubmedArticle( String ns, String ac, String match,
				                  String detail, String format,
				                  String client, Integer depth,
				                  Holder<XMLGregorianCalendar> timestamp,
				                  Holder<DatasetType> dataset,
				                  Holder<String> nativerecord 
                                  ) throws ProxyFault {
        
        String provider = "NCBI";
	    String service = "pubmed";
       
	    Log log = LogFactory.getLog( NcbiCachingImpl.class );
	    log.info( "getPubmedArticle " +
		            " NS=" + ns + " AC=" + ac + " DT=" + detail );
	
        if ( !ns.equalsIgnoreCase( "pmid" ) ) {
	        log.info( " forcing pubmed as ns" ); 
	        ns = "pmid"; 
	    }	

	    if ( ac == null || ac.equals( "" ) ) {
	        log.info( "missing accession" );
            throw FaultFactory.newInstance( Fault.MISSING_ID );
	    }
	
	    if ( detail == null ) {
	        detail = "stub";
	    } else if ( detail.equalsIgnoreCase( "short" ) || 
		            detail.equalsIgnoreCase( "stub" ) ) {
	        detail = "stub";
        } else if ( detail.equalsIgnoreCase( "base" ) ){
            detail = "base";
	    } else {
	        detail = "full";
	    } 
        
        try {
            Router router = 
                WSContext.getServerContext( provider ).createRouter() ;

            CachingService cachingSrv = 
                new CachingService( provider, router,
                                    WSContext.getServerContext( provider ) );
            
            if ( format == null || format.equals( "" ) ||
                 format.equalsIgnoreCase( "dxf" ) ||
                 format.equalsIgnoreCase( "both" ) ) {
                
                DatasetType result = 
                    cachingSrv.getDxf( provider, service, ns, ac, detail );
                if ( result != null ) {
                    dataset.value = result ;
                } else {
                    log.info("return dataset is null ");
                    throw FaultFactory.newInstance( Fault.NO_RECORD );
		        }
            }
	    
            if ( format != null &&
                 ( format.equalsIgnoreCase( "native" ) ||
                   format.equalsIgnoreCase( "both" ) )
                 ) {
                
                NativeRecord natRec = 
                    cachingSrv.getNative( provider, service, ns, ac );
		
		        if ( natRec != null && natRec.getNativeXml() != null &&
                     natRec.getNativeXml().length() > 0 ) {
                    nativerecord.value = natRec.getNativeXml();
                    timestamp.value =
                        TimeStamp.toXmlDate( natRec.getQueryTime() );
		        } else {
                    log.info("return dataset is null ");
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
        
        return;
    }

    public void getRefseq( String ns, String ac, String match,
			               String detail, String format,
			               String client, Integer depth,
			               Holder<XMLGregorianCalendar> timestamp,
			               Holder<DatasetType> dataset,
			               Holder<String> nativerecord 
                           ) throws ProxyFault {
	
        String provider = "NCBI";
        String service = "refseq";
        
	    Log log = LogFactory.getLog( NcbiCachingImpl.class );
	    log.info( "getRefseq " + 
		    " NS=" + ns + " AC=" + ac + " DT=" + detail );
	
       	if ( !ns.equalsIgnoreCase( "refseq" ) ) {
	        log.info( "forcing refseq as ns" ); 
	        ns = "refseq";
        }	
	
	    if ( ac == null || ac.equals( "" ) ) {
            log.info( " missing accession" );
            throw FaultFactory.newInstance( Fault.MISSING_ID );
        }   
	
	    if( detail == null ) {
	        detail = "stub";
	    } else if( detail.equalsIgnoreCase("short") || 
                   detail.equalsIgnoreCase("stub") ) {

	        detail = "stub";

        } else if ( detail.equalsIgnoreCase( "base" ) ){
            detail = "base";

	    } else {
	        detail = "full";
	    }   
	
	    try {
            Router router = 
                WSContext.getServerContext( provider ).createRouter() ;

            CachingService cachingSrv = 
                new CachingService( provider, router,
                                    WSContext.getServerContext( provider ) );
            
            if ( format == null || format.equals( "" ) ||
                 format.equalsIgnoreCase( "dxf" ) ||
                 format.equalsIgnoreCase( "both" )
                 ) {
                
                DatasetType result = 
                    cachingSrv.getDxf( provider, service, ns, ac, detail );
                if ( result != null ) {
                    dataset.value = result;     
                } else {
		            log.info( "return dataset is null " );
                    throw FaultFactory.newInstance( Fault.NO_RECORD );
		        }      
            }
            
            if ( format != null &&
                 ( format.equalsIgnoreCase( "native" ) ||
                   format.equalsIgnoreCase( "both" ) )
                 ) {

                NativeRecord natRec = 
		                cachingSrv.getNative( provider, service, ns, ac );
                if ( natRec != null ) {   
		            nativerecord.value = natRec.getNativeXml();
                    timestamp.value = 
                        TimeStamp.toXmlDate( natRec.getCreateTime() );
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
 
        return;
    }
    
    public void getGene( String ns, String ac, String match,
			             String detail, String format,
			             String client, Integer depth,
			             Holder<XMLGregorianCalendar> timestamp,
			             Holder<DatasetType> dataset,
			             Holder<String> nativerecord 
                         ) throws ProxyFault {
	
        String provider = "NCBI";
        String service = "entrezgene";
 
        Log log = LogFactory.getLog( NcbiCachingImpl.class );
	    log.info( "getGene " + 
		    "NS=" + ns + " AC=" + ac + " DT=" + detail );
	
	    if( !ns.equalsIgnoreCase( "entrezgene" ) ) {
	        log.info( " forcing entrezgene as ns" ); 
	        ns = "entrezgene";
	    }	
	
	    if( ac == null || ac.equals( "" ) ) {
	        log.info( "missing accession" );
            throw FaultFactory.newInstance( Fault.MISSING_ID );
	    }
	
        if( detail == null ) {
            detail = "stub";
        } else if( detail.equalsIgnoreCase("short") ||
                   detail.equalsIgnoreCase("stub") ) {

            detail = "stub";

        } else if ( detail.equalsIgnoreCase( "base" ) ){
            detail = "base";

        } else {
            detail = "full";
        }
        
	    try {
            Router router = 
                WSContext.getServerContext( provider ).createRouter() ;

            CachingService cachingSrv = 
                new CachingService( provider, router,
                                    WSContext.getServerContext( provider ) );

            if ( format == null || format.equals( "" ) ||
                 format.equalsIgnoreCase( "dxf" ) ||
                 format.equalsIgnoreCase( "both" )
                 ) {
                
                DatasetType result = 
                    cachingSrv.getDxf( provider, service, ns, ac, detail );
                if ( result != null ) {
                    dataset.value = result;
                } else {
                    log.info( "return dataset is null " );
		            throw FaultFactory.newInstance( Fault.NO_RECORD );
		        }
            }
            
            if ( format != null &&
                 ( format.equalsIgnoreCase( "native" ) ||
                   format.equalsIgnoreCase( "both" ) )
                 ) {

		        NativeRecord natRec =
                    cachingSrv.getNative( provider, service, ns, ac );
                if ( natRec != null ) {
                    nativerecord.value = natRec.getNativeXml();
                    timestamp.value = 
                        TimeStamp.toXmlDate( natRec.getCreateTime() );
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
 
        return;
    }

    //---------------------------------------------------------------------    

    
    public void getTaxon( String ns, String ac, String match,
			              String detail, String format,
			              String client, Integer depth,
			              Holder<XMLGregorianCalendar> timestamp,
			              Holder<DatasetType> dataset,
			              Holder<String> nativerecord 
                          ) throws ProxyFault {
	
        String provider = "NCBI";
        String service = "taxon";

	    Log log = LogFactory.getLog( NcbiCachingImpl.class );
	    log.info( "getTaxon " + 
		    "NS=" + ns + " AC=" + ac + " DT=" + detail );
	
	    if ( !ns.equalsIgnoreCase( "ncbitaxid" ) ) {
	        log.info( "forcing ncbitaxid as ns" ); 
	        ns = "ncbitaxid";
	    }	
        
	    if( ac == null || ac.equals( "" ) ) {   
	        log.info( "missing accession" );
            throw FaultFactory.newInstance( Fault.MISSING_ID );
	    }
	
        if( detail == null ) {
            detail = "stub";
        } else if( detail.equalsIgnoreCase("short") ||
                   detail.equalsIgnoreCase("stub") ) {

            detail = "stub";

        } else if ( detail.equalsIgnoreCase( "base" ) ){
            detail = "base";

        } else {
            detail = "full";
        }
	
	    try {
            Router router = 
                WSContext.getServerContext( provider ).createRouter() ;

            CachingService cachingSrv = 
                new CachingService( provider, router,
                                    WSContext.getServerContext( provider ) );
            
            if ( format == null || format.equals( "" ) ||
                 format.equalsIgnoreCase( "dxf" ) ||
                 format.equalsIgnoreCase( "both" )
                 ) {
                
                DatasetType result = 
                    cachingSrv.getDxf( provider, service, ns, ac, detail );
                if ( result != null ) {
                    dataset.value = result;
                } else {  
                    log.info( "return dataset is null " );
                    throw FaultFactory.newInstance( Fault.NO_RECORD );
                }
            }
            
            if ( format != null &&
                 ( format.equalsIgnoreCase( "native" ) ||
                   format.equalsIgnoreCase( "both" ) )
                 ) {

                NativeRecord natRec =
                    cachingSrv.getNative( provider, service, ns, ac );
                if ( natRec != null ) {
                    nativerecord.value = natRec.getNativeXml();
                    timestamp.value = 
                        TimeStamp.toXmlDate( natRec.getCreateTime() );
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

        return;
    }

}
