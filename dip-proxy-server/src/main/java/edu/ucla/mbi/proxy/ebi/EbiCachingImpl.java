package edu.ucla.mbi.proxy.ebi;

/*==============================================================================
 *
 * EbiCachingImpl - EBI Database access implemented through efetch SOAP
 *
 *=========================================================================== */

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.ucla.mbi.dxf14.*;
import edu.ucla.mbi.proxy.*;
import edu.ucla.mbi.cache.*;

import edu.ucla.mbi.fault.*;
import edu.ucla.mbi.util.TimeStamp;

import javax.jws.WebService;
import javax.xml.ws.Holder;
import javax.xml.datatype.XMLGregorianCalendar;

import edu.ucla.mbi.proxy.context.*;

@WebService(endpointInterface = "edu.ucla.mbi.proxy.EbiProxyPort")
public class EbiCachingImpl extends ConfigurableServer 
    implements EbiProxyPort {
    
    private CachingService cachingSrv;

    public void setCachingService ( CachingService service ) {
        this.cachingSrv = service;
        
        Log log = LogFactory.getLog( EbiCachingImpl.class );
        log.info( "EbiCachingImpl: CachingService set");	
    }

    // Fetch uniprot from ebi dbfetch
    
    public Result getUniprot( GetUniprot request )
        throws ProxyFault{
        
        String provider = "EBI";
        String service = "uniprot";

        ObjectFactory of =  new ObjectFactory();
        Result result =of.createResult();
        
        Log log = LogFactory.getLog( EbiCachingImpl.class );
        log.info( "EbiCaching: getUniprot " +
                  " NS=" + request.getNs() +
                  " AC=" + request.getAc() +
                  " DT=" + request.getDetail() );
        
        if( !request.getNs().equalsIgnoreCase( "uniprot" ) ){
            log.info( "EbiCaching: forcing uniprot as ns" );
            request.setNs( "uniprot" );
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
                    log.info( "return dataset is null" );
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
                
                if ( natRec != null && natRec.getNativeXml() != null
                     && natRec.getNativeXml().length() > 0 ) {
                    result.setNativerecord( natRec.getNativeXml() );
                    result.setTimestamp( TimeStamp.toXmlDate( natRec.getQueryTime() ));
                } else {
                    log.info( "return dataset is null " );
                    throw FaultFactory.newInstance( Fault.NO_RECORD );
                }
            }

        } catch ( ProxyFault fault ) {
            String message = fault.getFaultInfo().getMessage();
            log.warn( "EbiCachingImpl: ServiceException: message= " + message);

            throw fault;

        } catch ( Exception e ) {
            log.warn( "EbiCachingImpl: " + e.toString() );
            throw FaultFactory.newInstance( Fault.UNKNOWN );
        }

        return result; 
    }
}
