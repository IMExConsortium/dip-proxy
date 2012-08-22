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

import edu.ucla.mbi.dip.dbservice.*;

public class DipServer extends RemoteNativeServer {

    private DipLegacyPort diplegacyPort;
    private String diplegacyEndpoint = null;
    
    private DipDxfPort dipPort;
    private String dipEndpoint = null;

    private final String detail = "full";
    
    private final String diplegacyNsSrv = "http://mbi.ucla.edu/services/legacy/dip";
    private final String diplegacyNmSrv = "DipLegacyService";

    private final String dipNsSrv = "http://mbi.ucla.edu/dip/dbservice";
    private final String dipNmSrv = "dipDxfService";

    //--------------------------------------------------------------------------

    public void initialize() {
        
        Log log = LogFactory.getLog( DipServer.class );
        log.info( "initialize service" );
        
        if( getContext() != null ){

            dipEndpoint = (String) getContext().get( "dipEndpoint" );
            diplegacyEndpoint = (String) getContext().get( "diplegacyEndpoint" );

            if( dipEndpoint != null ){
                dipEndpoint = dipEndpoint.replaceAll( "^\\s+", "" );
                dipEndpoint = dipEndpoint.replaceAll( "\\s+$", "" );
            }

            if( diplegacyEndpoint != null ){
                diplegacyEndpoint = diplegacyEndpoint.replaceAll( "^\\s+", "" );
                diplegacyEndpoint = diplegacyEndpoint.replaceAll( "\\s+$", "" );
            }
        }
        
        if( dipEndpoint != null && dipEndpoint.length() > 0 ){

            log.info( " dipEndpoint=" + dipEndpoint );
           
            try { 
                DipDxfService service = 
                    new DipDxfService( new URL( dipEndpoint + "?wsdl" ), 
                                          new QName( dipNsSrv, dipNmSrv ) );
            
                dipPort = service.getDipDxfPort();

                if ( dipPort != null ) {
                    ((BindingProvider) dipPort).getRequestContext()
                        .put( BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                              dipEndpoint );
                }
            } catch ( Exception e ) {
                log.warn( "DipServer: DipDxfService initializing failed: "  
                          + "reason=" +  e.toString() + ". ");
                return;
            }     
        } else {
            log.warn( "DipServer: dipEndpoint is null . ");
        }

        if( diplegacyEndpoint != null && diplegacyEndpoint.length() > 0 ){

            log.info( " diplegacyEndpoint=" + diplegacyEndpoint );
            
            try {
                DipLegacyService service =
                    new DipLegacyService( new URL( diplegacyEndpoint + "?wsdl" ),
                                          new QName( diplegacyNsSrv, diplegacyNmSrv ) );

                diplegacyPort = service.getLegacyPort();

                if ( diplegacyPort != null ) {
                    ((BindingProvider) diplegacyPort).getRequestContext()
                        .put( BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                              diplegacyEndpoint );
                } 
            } catch ( Exception e ) {
                log.warn( "DipServer: DipLegacyService initializing failed: "   
                          + "reason=" +  e.toString() + ". ");
                return;
            }
        } else {
            log.warn( "DipServer: diplegacyEndpoint is null . ");
        }
    }

    // ---------------------------------------------------------------------

    public NativeRecord getNative( String provider, String service, String ns,
            String ac, int timeOut ) throws ProxyFault {

        Log log = LogFactory.getLog( DipServer.class );
        log.info( "srv=" + service + " ns=" + ns + " ac=" + ac );

        String retVal = null;
        List<NodeType> retList = null;
        String detail = "full";
 
        if ( ns.equals( "dip" ) ) {
             if ( service.equals( "dip" ) ) {
                if( ac.matches( "DIP-\\d+LP" ) ) {
                    log.info( "ac=" + ac + " for getLink. " );
                    try {
                        log.info( "getNative: getLink. " );            
                        retList = dipPort.getLink( "dip", ac, "", detail, "dxf" );
                        log.info( "getNative: after getLink. retList=" + retList );
                    } catch ( DipDbFault fault ) {
                        log.warn( "getNative: fault=" + fault.getMessage() );
                        throw FaultFactory.newInstance( Fault.REMOTE_FAULT ); 
                    }
                } else if ( ac.matches( "DIP-\\d+XE" ) ) {
                    try {
                        retList = dipPort.getEvidence( "dip", ac, "", detail, "dxf" );
                    } catch ( DipDbFault fault ) {
                        log.warn( "getNative: fault=" + fault.getMessage() );
                        throw FaultFactory.newInstance( Fault.REMOTE_FAULT );
                    }
                } else if ( ac.matches( "DIP-\\d+SA" ) ) {
                    try {
                        retList = dipPort.getSource( "dip", ac, "", detail, "dxf" );
                    } catch ( DipDbFault fault ) {
                        log.warn( "getNative: fault=" + fault.getMessage() );
                        throw FaultFactory.newInstance( Fault.REMOTE_FAULT );
                    }
                } else if ( ac.matches( "(DIP-\\d+NP) | (DIP-\\d+NG) | (DIP-\\d+NM) " ) ) {
                    try {
                        retList = dipPort.getNode( "dip", ac, "", detail, "dxf" );
                    } catch ( DipDbFault fault ) {
                        log.warn( "getNative: fault=" + fault.getMessage() );
                        throw FaultFactory.newInstance( Fault.REMOTE_FAULT );
                    }
                } else {
                    log.warn( "getNative: ac=" + ac + " is invalid id. " );
                    throw FaultFactory.newInstance( Fault.INVALID_ID );
                }
            } else if ( service.equals( "diplegacy" ) ) {
                if( ac.matches( "DIP-\\d+E" ) ) {
                    try {
                        retList = diplegacyPort.getLink( "dip", ac, "", detail, "dxf" );
                    } catch ( Exception ex ) {
                        log.info( "exception=" + ex.toString() );
                        throw FaultFactory.newInstance( Fault.REMOTE_FAULT ); 
                    }
                } else if ( ac.matches( "DIP-\\d+N" ) ) {
                    try {
                        retList = diplegacyPort.getNode( "dip", ac, "", "", detail, "dxf" );
                    } catch ( Exception ex ) {
                        log.info( "exception=" + ex.toString() );
                        throw FaultFactory.newInstance( Fault.REMOTE_FAULT );
                    }
                } else if ( ac.matches( "DIP-\\d+X" ) ) {
                    try {
                        retList = diplegacyPort.getEvidence( "dip", ac, "", detail, "dxf" );
                    } catch ( Exception ex ) {
                        log.info( "exception=" + ex.toString() );
                        throw FaultFactory.newInstance( Fault.REMOTE_FAULT );
                    }
                } else if ( ac.matches( "DIP-\\d+S" ) ) {
                    try {
                        retList = diplegacyPort.getSource( "dip", ac, "", detail, "dxf" );
                    } catch ( Exception ex ) {
                        log.info( "exception=" + ex.toString() );
                        throw FaultFactory.newInstance( Fault.REMOTE_FAULT );
                    }
                } else {
                    log.warn( "getNative: ac=" + ac + " is invalid id. " );
                    throw FaultFactory.newInstance( Fault.INVALID_ID );
                }
            } else {
                log.warn( "getNative: service=" + service + " is a invalid service. " );
                throw FaultFactory.newInstance( Fault.UNSUPPORTED_OP );
            }
        } else {
            log.warn( "getNative: ns=" + ns + " is a unrecognized namespace. " );
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
