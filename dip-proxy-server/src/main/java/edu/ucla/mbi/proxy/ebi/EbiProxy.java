package edu.ucla.mbi.proxy.ebi;

/*=========================================================================
 * $HeadURL: https://wyu@imex.mbi.ucla.edu/svn/dip-ws/trunk/dip-proxy/src/#$
 * $Id$
 * Version: $Rev$
 *=========================================================================
 *
 * RemoteProxyServer:
 *
 *    returns string representation of a data record requested from the 
 *    server using ns/ac (namespace/accession) pair as identifier and
 *    operation as the remote service name
 *
 *====================================================================== */

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.ByteArrayInputStream;

import javax.xml.bind.util.JAXBResult;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBContext;

import javax.xml.transform.stream.StreamSource;

import javax.xml.ws.Holder;
import javax.xml.ws.BindingProvider;
import com.sun.xml.ws.developer.JAXWSProperties;

import edu.ucla.mbi.dxf14.DatasetType;
import edu.ucla.mbi.dxf14.DxfJAXBContext;

import java.util.Map;

import edu.ucla.mbi.proxy.*;
import edu.ucla.mbi.cache.NativeRecord;

import edu.ucla.mbi.fault.*;

import javax.xml.datatype.XMLGregorianCalendar;

public class EbiProxy extends RemoteProxyServer {

    private Log log = LogFactory.getLog( EbiProxy.class );
    private static Map<String, Object> context;

    public void setContext( Map<String, Object> context ) {
        EbiProxy.context = context;
    }

    public Map<String, Object> getContext() {
        return context;
    }

    public void initialize() {
        Log log = LogFactory.getLog( EbiProxy.class );
        log.info( "Initializing: " + this );
    }

    // factory

    public RemoteProxyServer getRemoteProxyServerInstance() {
        RemoteProxyServer proxy = new EbiProxy();
        proxy.setAddress( this.getAddress() );
        return proxy;
    }

    public NativeRecord getNative( String provider, String service, String ns,
            String ac, int timeout ) throws ProxyFault {

        Log log = LogFactory.getLog( EbiProxy.class );

        log.info( "getNative(NS=" + ns + " AC=" + ac + " OP=" + service + ")" );

        // call EBI proxy localized at ProxyAddress

        String url = getAddress();

        EbiProxyService proxySrv = new EbiProxyService();
        EbiProxyPort port = proxySrv.getProxyPort();

        NativeRecord natRecord = null;

        try {
            log.info( " this=" + this );
            log.info( " port=" + url + " timeout=" + timeout );

            // set server location
            // ---------------------

            ((BindingProvider) port).getRequestContext().put(
                    BindingProvider.ENDPOINT_ADDRESS_PROPERTY, url );

            // set client Timeout
            // ------------------

            ((BindingProvider) port).getRequestContext().put(
                    JAXWSProperties.CONNECT_TIMEOUT, timeout );

            if ( service.equals( "uniprot" ) ) {
                natRecord = getUniprot( port, provider, service, ns, ac );
            }

            if ( service.equals( "picr" ) ) {
                natRecord = getPicrList( port, provider, service, ns, ac );
            }

        } catch ( ProxyFault fault ) {
            throw fault;
        } catch ( Exception e ) {
            log.info( "EbiProxy: getUniprot: exception: " + e.toString() );

            if ( e.toString().contains( "No result found" ) ) {
                // ServiceFault fault =
                // new ServiceFault( "04 remote server: no hit." );
                throw FaultFactory.newInstance( Fault.NO_RECORD ); // no hits
            } else if ( e.toString().contains( "Read timed out" ) ) {
                // ServiceFault fault =
                // new ServiceFault( "02 remote server: timeout." );
                throw FaultFactory.newInstance( Fault.REMOTE_TIMEOUT ); // remote timeout
            } else {
                // ServiceFault fault =
                // new ServiceFault( "03 remote server: unknown." );
                throw FaultFactory.newInstance( Fault.UNKNOWN ); // unknown error ???
            }

        }
        return natRecord;

    }

    public DatasetType transform( String strNative, String ac, String ns,
                    String detail, String service, ProxyTransformer pTrans )
                                                            throws ProxyFault 
    {
        //Log log = LogFactory.getLog( EbiProxy.class );

        try {
            // native data in string representationa as input

            ByteArrayInputStream bisNative =
                    new ByteArrayInputStream( strNative.getBytes( "UTF-8" ) );

            StreamSource ssNative = new StreamSource( bisNative );

            // dxf as JAXBResult result of the transformation

            JAXBContext dxfJc = DxfJAXBContext.getDxfContext();
            JAXBResult result = new JAXBResult( dxfJc );

            // transform into DXF

            pTrans.setTransformer( service );
            pTrans.setParameters( detail, ns, ac );
            pTrans.transform( ssNative, result );

            DatasetType dxfResult =
                    (DatasetType) ((JAXBElement) result.getResult()).getValue();

            // test if dxfResult is empty
            if ( dxfResult.getNode().isEmpty() ) {
                // ServiceFault fault =
                // new ServiceFault( "05 remote server: transformer." );
                throw FaultFactory.newInstance( Fault.UNKNOWN ); // no hits
                // or
                // transformation ???
            }

            return dxfResult;

        } catch ( ProxyFault ex ) {
            log.info( "Transformer fault: empty dxfResult " );
            throw ex;
        } catch ( Exception e ) {
            log.info( "Exception=" + e.toString() );
            // ServiceFault fault =
            // new ServiceFault( "05 remote server: transform." );
            throw FaultFactory.newInstance( Fault.UNKNOWN ); // unknown error
            // ???
        }
    }

    public DatasetType buildDxf( String strNative, String ac, String ns,
            String detail, String service, ProxyTransformer pTrans )
            throws ProxyFault {

        // NOTE: overload if dxf building more complex than
        // a simple xslt transformation
        log.info( "buildDxf start... ");
        return this.transform( strNative, ac, ns, detail, service, pTrans );
    }

    private NativeRecord getUniprot( EbiProxyPort port, String provider,
            String service, String ns, String ac ) throws ProxyFault {

        Holder<DatasetType> resDataset = new Holder<DatasetType>();
        Holder<String> resNative = new Holder<String>();
        Holder<XMLGregorianCalendar> timestamp =
                new Holder<XMLGregorianCalendar>();
        try {

            Log log = LogFactory.getLog( EbiProxy.class );
            log.info( "port.getUniprot(" + "uniprot" + ":" + ac + ")" );

            port.getUniprot( "uniprot", ac, "", "", "native", "", 0, timestamp,
                    resDataset, resNative );
            
            XMLGregorianCalendar qtime = timestamp.value;

            NativeRecord record = new NativeRecord( provider, service, ns, ac );
            record.setNativeXml( resNative.value );
            record.setCreateTime( qtime.toGregorianCalendar().getTime() );
            return record;

        } catch ( Exception e ) {
            Log log = LogFactory.getLog( EbiProxy.class );
            log.info( e.toString() );

            throw FaultFactory.newInstance( Fault.REMOTE_FAULT ); // remote
            // server
        }
    }

    /* GetPicrList */

    private NativeRecord getPicrList( EbiProxyPort port, String provider,
            String service, String ns, String ac ) throws ProxyFault {

        Holder<DatasetType> resDataset = new Holder<DatasetType>();
        Holder<String> resNative = new Holder<String>();
        Holder<XMLGregorianCalendar> timestamp =
                new Holder<XMLGregorianCalendar>();
        try {
            port.getPicrList( ns, ac, "", "", "native", "", 0, timestamp,
                    resDataset, resNative );

            XMLGregorianCalendar qtime = timestamp.value;

            NativeRecord record = new NativeRecord( provider, service, ns, ac );
            record.setNativeXml( resNative.value );
            record.setCreateTime( qtime.toGregorianCalendar().getTime() );
            return record;
            
        } catch ( Exception e ) {
            Log log = LogFactory.getLog( EbiProxy.class );
            log.info( e.toString() );

            // ServiceFault fault =
            // new ServiceFault( "05 remote server: transformer." );
            throw FaultFactory.newInstance( Fault.TRANSFORM ); // transformation error ?

        } catch ( Error e ) {
            Log log = LogFactory.getLog( EbiProxy.class );
            log.info( e.toString() );

            // ServiceFault fault =
            // new ServiceFault( "05 remote server: transformer." );
            throw FaultFactory.newInstance( Fault.TRANSFORM ); // transformation error ?
        }
    }
}
