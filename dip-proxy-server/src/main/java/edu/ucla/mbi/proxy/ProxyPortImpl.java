package edu.ucla.mbi.proxy;

/*==============================================================================
 * $HeadURL::                                                                  $
 * $Id::                                                                       $
 * Version: $Rev::                                                             $
 *==============================================================================
 *
 * ProxyPortImpl - dip-proxy services implemented 
 *                                  
 *=========================================================================== */

import edu.ucla.mbi.dxf14.*;
import edu.ucla.mbi.proxy.*;
import edu.ucla.mbi.fault.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.jws.WebService;
import javax.xml.ws.Holder;
import javax.xml.datatype.XMLGregorianCalendar;

@WebService(endpointInterface="edu.ucla.mbi.proxy.ProxyPort")
public class ProxyPortImpl implements ProxyPort {

    private Log log = LogFactory.getLog( ProxyPortImpl.class );

    private ProxyServer proxyServer;

    public void setProxyServer( ProxyServer server){

        proxyServer = server;
    }

    public void initialize() {
        log.info( "initializing... " ) ;
    }
    
    public void getRecord( String provider, String service,
                           String ns, String ac, String match,
                           String detail, String format,
                           String client, Integer depth,
                           Holder<XMLGregorianCalendar> timestamp,
                           Holder<DatasetType> dataset,
                           Holder<String> nativerecord
                           ) throws ProxyFault {

        try{

            ProxyServerRecord prxRec = proxyServer.getRecord( provider, service,
                                                              ns, ac, match,
                                                              detail, format,
                                                              client, depth );

            nativerecord.value = prxRec.getNativeRecord();     
            dataset.value = prxRec.getDataset();
            timestamp.value = prxRec.getTimestamp();

        } catch ( ServerFault fault ) {
            throw FaultFactory.newInstance( fault.getFaultCode() );
        }
            
        return;
    }
    
}
