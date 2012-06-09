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

import edu.ucla.mbi.services.*;
//import edu.ucla.mbi.services.dip.*;
//import edu.ucla.mbi.services.dip.direct.*;

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
            String ac, int timeOut ) throws ServiceException {

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
            throw Fault.getServiceException( Fault.INVALID_ID );
        }

        if ( retList == null ) {
            log.info( "no record found " );
            throw Fault.getServiceException( Fault.NO_RECORD );
        }

        // marshall List<NodeType> into dataset element
        // ---------------------------------------------

        retVal = marshall( retList );
        
        NativeRecord record = new NativeRecord( provider, service, ns, ac);
        record.setNativeXml( retVal );
        return record;
    }

    private String marshall( List<NodeType> nodeList ) throws ServiceException {
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
            return result;

        } catch ( Exception e ) {

            Log log = LogFactory.getLog( DipServer.class );
            log.info( "marshalling exception " + e.toString() );
            throw Fault.getServiceException( Fault.MARSHAL );
        }
    }
}
