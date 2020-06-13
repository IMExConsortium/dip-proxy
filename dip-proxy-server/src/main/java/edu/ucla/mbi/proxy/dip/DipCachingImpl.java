package edu.ucla.mbi.proxy.dip;

/*==============================================================================
 *
 * DipCachingImpl - Dip Database access implemented through SOAP service
 *
 *=========================================================================== */

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.ucla.mbi.dxf14.*;
import edu.ucla.mbi.proxy.*;
import edu.ucla.mbi.cache.*;


import edu.ucla.mbi.proxy.context.*;

import edu.ucla.mbi.fault.*;
import edu.ucla.mbi.util.TimeStamp;

import javax.jws.WebService;
import javax.xml.ws.Holder;

import javax.xml.datatype.XMLGregorianCalendar;

@WebService(endpointInterface = "edu.ucla.mbi.proxy.DipProxyPort")
public class DipCachingImpl extends ConfigurableServer 
    implements DipProxyPort {

    private CachingService cachingSrv;

    public void setCachingService ( CachingService service ) {
        this.cachingSrv = service;
    }

    // Fetch record from Dip
    
    public Result getDipRecord( GetDipRecord request )
        throws ProxyFault {
        
        String provider = "DIP";
        String service = "dip";

        ObjectFactory of = new ObjectFactory();
        Result result = of.createResult();
        
        Log log = LogFactory.getLog( DipCachingImpl.class );
        log.info( "getPubmedArticle " +
                  " NS=" + request.getNs() +
                  " AC=" + request.getAc() +
                  " DT=" + request.getDetail() );

        if( !request.getNs().equalsIgnoreCase( "dip" ) ){
            log.info( " forcing pubmed as ns" );
            request.setNs( "dip" );
        }

        if( request.getAc() == null || request.getAc().equals( "" ) ){
            log.info( "DipCaching: missing accession" );
            throw FaultFactory.newInstance( Fault.MISSING_ID );            
        }

        if( request.getDetail() == null ){
            request.setDetail( "stub" );
        }else if( request.getDetail().equalsIgnoreCase( "short" )
                  || request.getDetail().equalsIgnoreCase( "stub" ) ){
            request.setDetail( "stub" );
        } else {
            request.setDetail( "full" );
        }
        
        try {

            if ( request.getFormat() == null
                 || request.getFormat().equals( "" )
                 || request.getFormat().equalsIgnoreCase( "dxf" )
                 || request.getFormat().equalsIgnoreCase( "both" ) ) {
                
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

            if ( request.getFormat() != null
                 && ( request.getFormat().equalsIgnoreCase( "native" ) 
                      || request.getFormat().equalsIgnoreCase( "both" ) ) ) {
                
                NativeRecord natRec = 
                    cachingSrv.getNative( provider, service,
                                          request.getNs(),
                                          request.getAc() );
                
                if ( natRec != null && natRec.getNativeXml() != null
                     && natRec.getNativeXml().length() > 0 ) {

                    result.setNativerecord( natRec.getNativeXml() );

                    result.setTimestamp( TimeStamp.toXmlDate( natRec.getQueryTime() ) );
                    
                } else {
                    log.info( "return dataset is null " );
                    throw FaultFactory.newInstance( Fault.NO_RECORD );
                }
            }

        } catch ( ProxyFault fault ) {
            log.info( "ServiceFault: " + fault.getFaultInfo().getMessage() );
            throw  fault;
        } catch ( Exception e ) {
            log.info( "DipCachingImpl: " + e.toString() );
            throw FaultFactory.newInstance( Fault.UNKNOWN );
        }

        return result;
    }
}
