package edu.ucla.mbi.proxy;

/*==============================================================================
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
    
    public Result getRecord(
                          //String provider, String service,
                          //String ns, String ac, String match,
                          //String detail, String format,
                          //String client, Integer depth,
                          //Holder<XMLGregorianCalendar> timestamp,
                          //Holder<DatasetType> dataset,
                          //Holder<String> nativerecord

                          GetRecord request
                          ) throws ProxyFault {

        ObjectFactory of = new ObjectFactory();
        Result result = of.createResult();

        try{

            

            
            log.info( " provider=>" + request.getProvider() +
                      " service=>" + request.getService() +
                      " ns=>" + request.getNs() +
                      " ac=>" + request.getAc() );
            
            ProxyServerRecord prxRec =
                proxyServer.getRecord( request.getProvider(),
                                       request.getService(),
                                       request.getNs(),
                                       request.getAc(),
                                       request.getMatch(),
                                       request.getDetail(),
                                       request.getFormat(),
                                       request.getClient(),
                                       request.getDepth() );
            
            result.setNativerecord( prxRec.getNativeRecord() );
            result.setDataset( prxRec.getDataset() );
            result.setTimestamp( prxRec.getTimestamp() );
            

        } catch ( ServerFault fault ) {
            throw FaultFactory.newInstance( fault.getFaultCode() );
        }
            
        return result ;
    }
    
}
