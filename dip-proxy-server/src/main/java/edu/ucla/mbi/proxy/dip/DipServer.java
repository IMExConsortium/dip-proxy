package edu.ucla.mbi.proxy.dip;

/*==============================================================================
 * $HeadURL::                                                                  $
 * $Id::                                                                       $
 * Version: $Rev::                                                             $
 *==============================================================================
 *
 * DipServer:
 *    services provided by Dip web services
 *
 *=========================================================================== */

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.net.URL;

import javax.xml.bind.*;
import javax.xml.ws.BindingProvider;
import javax.xml.namespace.QName;

import java.util.List;
import java.io.StringWriter;

import edu.ucla.mbi.proxy.*;
import edu.ucla.mbi.dxf14.*;
import edu.ucla.mbi.cache.NativeRecord;
import edu.ucla.mbi.fault.*;

import edu.ucla.mbi.legacy.dip.*;
import edu.ucla.mbi.services.legacy.dip.*;

public class DipServer extends RemoteNativeServer {

    private DipLegacyPort port;
    private String endpoint = null;
    
    private final String detail = "full";
    
    private final String nsSrv = "http://mbi.ucla.edu/services/legacy/dip";
    private final String nmSrv = "DipLegacyService";

    //--------------------------------------------------------------------------

    public void initialize() {
        
        Log log = LogFactory.getLog( DipServer.class );
        log.info( "initialize service" );
        
        if( getContext() != null ){
            endpoint = (String) getContext().get( "endpoint" );
            if( endpoint != null ){
                endpoint = endpoint.replaceAll( "^\\s+", "" );
                endpoint = endpoint.replaceAll( "\\s+$", "" );
            }
        }
        
        if( endpoint != null && endpoint.length() > 0 ){

            log.info( " endpoint=" + endpoint );
            
            try{
                
                DipLegacyService service = 
                    new DipLegacyService( new URL( endpoint + "?wsdl" ), 
                                          new QName( nsSrv, nmSrv ) );
            
                port = service.getLegacyPort();
                
            } catch( Exception ex ){
                log.info( "DipServer: endpoint not set.");
                return;
            }
            
            if ( port != null ) {
                ((BindingProvider) port).getRequestContext()
                    .put( BindingProvider.ENDPOINT_ADDRESS_PROPERTY, 
                          endpoint );
            }
        }
    }

    // ---------------------------------------------------------------------

    public NativeRecord getNative( String provider, String service, String ns,
            String ac, int timeOut ) throws ProxyFault {

        Log log = LogFactory.getLog( DipServer.class );
        log.info( "srv=" + service + " ns=" + ns + " ac=" + ac );

        String retVal = null;
        List<NodeType> retList = null;
        if ( ns.equals( "dip" ) ) {
            if ( ac.endsWith( "E" ) ) {
                try {
                    retList = port.getLink( "dip", ac, "", detail, "" );
                } catch ( Exception ex ) {
                    log.info( "exception=" + ex.toString() );
                }
            }
            if ( ac.endsWith( "N" ) ) {
                try {
                    //retList = port.getNode( "dip", ac, "", detail, "" );
                    retList = port.getNode( "dip", ac, "", "", detail, "dxf" );
                } catch ( Exception ex ) {
                    log.info( "exception=" + ex.toString() );
                }

            }
            if ( ac.endsWith( "X" ) ) {
                try {
                    retList = port.getEvidence( "dip", ac, "", detail, "" );
                } catch ( Exception ex ) {
                    log.info( "exception=" + ex.toString() );
                }
            }
            if ( ac.endsWith( "S" ) ) {
                try {
                    retList = port.getSource( "dip", ac, "", detail, "" );
                } catch ( Exception ex ) {
                    log.info( "exception=" + ex.toString() );
                }
            }
        } else {
            log.info( "unrecognized namespace" );
            throw FaultFactory.newInstance( Fault.INVALID_ID );
        }

        if ( retList == null ) {
            log.info( "no record found " );
            throw FaultFactory.newInstance( Fault.NO_RECORD );
        } else if( retList.size() == 0 ) {
            log.info( "remote service return empty list. " );
            throw FaultFactory.newInstance( Fault.REMOTE_FAULT );
        } else {
            log.info( "getNative: retList.size=" + retList.size() ); 
        }

        // marshall List<NodeType> into dataset element
        // ---------------------------------------------

        retVal = marshall( retList );
        
        NativeRecord record = new NativeRecord( provider, service, ns, ac);
        record.setNativeXml( retVal );
        return record;
    }

    private String marshall( List<NodeType> nodeList ) throws ProxyFault {

        Log log = LogFactory.getLog( DipServer.class );
        log.info( "initialize service" );

        try {

            edu.ucla.mbi.dxf14.ObjectFactory dofDxf =
                    new edu.ucla.mbi.dxf14.ObjectFactory();

            DatasetType record = dofDxf.createDatasetType();
            record.getNode().addAll( nodeList );

            JAXBContext dxfJc = DxfJAXBContext.getDxfContext();

            Marshaller dxfMarshaller = dxfJc.createMarshaller();

            java.io.StringWriter swResult = new StringWriter();

            dxfMarshaller.setProperty( Marshaller.JAXB_ENCODING, "UTF-8" );
            dxfMarshaller.marshal( dofDxf.createDataset( record ), swResult );

            String result = swResult.toString();

            if( result.length() > 200 ) {
                log.info( "marshall: native string=" + result.substring(0, 200) );
            } else {
                log.info( "marshall: native string=" + result );
            }

            return result;

        } catch ( Exception e ) {

            log.info( "marshalling exception " + e.toString() );
            throw FaultFactory.newInstance( Fault.MARSHAL );
        }
    }
}
