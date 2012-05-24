package edu.ucla.mbi.proxy.prolinks;

/*===========================================================================
 * $HeadURL: https://wyu@imex.mbi.ucla.edu/svn/dip-ws/trunk/dip-proxy/src/#$
 * $Id$
 * Version: $Rev$
 *===========================================================================
 *
 * ProlinksServer:
 *    services provided by Prolinks web services
 *
 *========================================================================= */

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.ucla.mbi.cache.NativeRecord;
import edu.ucla.mbi.dxf14.*;
import edu.ucla.mbi.proxy.*;
import edu.ucla.mbi.proxy.ncbi.*;
import edu.ucla.mbi.services.Fault;
import edu.ucla.mbi.services.ServiceException;

import javax.xml.bind.*;
import java.io.*;
import java.util.*;

import javax.xml.ws.BindingProvider;
import javax.xml.ws.Holder;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.transform.stream.StreamSource;


public class ProlinksServer extends RemoteNativeServer {

    String ncbiProxyAddress = null;

    public void setNcbiProxy( String ncbiProxy ) {

        Log log = LogFactory.getLog( ProlinksServer.class );

        ncbiProxy = ncbiProxy.replaceAll( "^\\s*", "" );
        ncbiProxy = ncbiProxy.replaceAll( "\\s*$", "" );
        log.info( " NcbiProxy=" + ncbiProxy );
        this.ncbiProxyAddress = ncbiProxy;
    }

    public NativeRecord getNative( String provider, String service, String ns,
            String ac, int timeOut ) throws ServiceException {

        Log log = LogFactory.getLog( ProlinksServer.class );
        String retVal = null;

        log.info( " getNative: NS=" + ns + " AC=" + ac + " SRV=" + service );

        if ( service.equals( "prolinks" ) ) {

            String url = getRestUrl();
            String acTag = getRestAcTag();

            log.info( " restURL=" + url );
            log.info( " restTag=" + acTag );

            url = url.replaceAll( acTag, ac );
            retVal = NativeURL.query( url, timeOut );
        }
        if ( retVal == null ) {
            log.info( " null returned" );
            throw Fault.getServiceException( 5 ); // no hits
        }

        NativeRecord record = new NativeRecord( provider, service, ns, ac );
        return record;
    }

    public DatasetType buildDxf( String strNative, String ns, String ac,
            String detail, String service, ProxyTransformer pTrans )
            throws ServiceException {

        Log log = LogFactory.getLog( ProlinksServer.class );
        log.info( " buildDxf called: " + ac );
        // String refseqService = (String) ServiceContext
        // .getRsc("Prolinks").getProperty("refseqService");

        // String refseqService = ( (Map) pTrans.getContext().get( (String)
        // ServiceContext
        // .getRsc("Prolinks").getProperty("refseqService");

        // log.info( "ProlinksServer: refseqService=" + refseqService );

        try {
            // -------------------------------------------------------------

            // RemoteProxyServer proxy =
            // ncbiProxy().getRemoteProxyServerInstance();

            NcbiProxyService proxySrv = new NcbiProxyService();
            NcbiProxyPort port = proxySrv.getProxyPort();

            // set server location
            // ---------------------

            ((BindingProvider) port).getRequestContext()
                    .put( BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                            ncbiProxyAddress );

            // set client Timeout
            // ------------------

            /*
             * 
             * ( ( BindingProvider ) port ).getRequestContext() .put(
             * JAXWSProperties.CONNECT_TIMEOUT, timeout );
             */

            // edu.ucla.mbi.services.ncbi.ObjectFactory ncbiOF =
            // new edu.ucla.mbi.services.ncbi.ObjectFactory();
            // NcbiPublicStub stub = new NcbiPublicStub( refseqService);
            // log.info("ProlinksServer: refseqService="+refseqService+" stub="+stub);
            // GetRefseq grs = ncbiOF.createGetRefseq();
            JAXBContext dxfJc = DxfJAXBContext.getDxfContext();
            // JAXBContext dxfJc =
            // JAXBContext.newInstance("edu.ucla.mbi.dxf14");
            Unmarshaller u = dxfJc.createUnmarshaller();

            JAXBElement<DatasetType> datasetElement =
                    u.unmarshal(
                            new StreamSource( new StringReader( strNative ) ),
                            DatasetType.class );

            edu.ucla.mbi.dxf14.DatasetType dxfResult =
                    datasetElement.getValue();

            List<NodeType> node = dxfResult.getNode();
            for ( Iterator iterator = node.iterator(); iterator.hasNext(); ) {
                NodeType nodetype = (NodeType) iterator.next();
                List<edu.ucla.mbi.dxf14.NodeType.PartList.Part> part =
                        nodetype.getPartList().getPart();
                for ( Iterator iterator1 = part.iterator(); iterator1.hasNext(); ) {
                    PartType parttype = (PartType) iterator1.next();
                    NodeType nodeOld = parttype.getNode();
                    String node_ac = nodeOld.getAc();
                    long node_id = nodeOld.getId();

                    try {
                        log.info( "ProlinksServer: port.getRefseq call (loop):"
                                + " NS=refseq" + " AC=" + node_ac + " DT="
                                + detail );

                        Holder<DatasetType> resDataset =
                                new Holder<DatasetType>();
                        Holder<String> resNative = new Holder<String>();
                        Holder<XMLGregorianCalendar> timestamp = null;
                        port.getRefseq( "refseq", node_ac, "", detail, "", "",
                                0, timestamp, resDataset, resNative );

                        DatasetType dataset = resDataset.value;

                        NodeType nodeNew = (NodeType) dataset.getNode().get( 0 );
                        nodeNew.setId( node_id );
                        parttype.setNode( nodeNew );
                    } catch ( Exception e ) {
                        StringWriter sw = new StringWriter();
                        PrintWriter pw = new PrintWriter( sw );
                        e.printStackTrace( pw );
                        log.info( "ProlinksServer: stub getRefseq: "
                                + sw.toString() );

                        throw Fault.getServiceException( Fault.UNKNOWN );
                    }
                }
            }
            return dxfResult;
        } catch ( Exception e ) {
            log.info( "ProlinksServer: " + e.toString() );
            String fault = e.toString();
            // ServiceFault sf = null;
            if ( fault.contains( "03" ) ) {
                // sf = new ServiceFault( "03 remote server: unknown." );
                // new ServiceFailedException() );
                throw Fault.getServiceException( 99 );

            } else {
                // sf = new ServiceFault( "07 server: unknown." );
                // new ServiceFailedException() );
                throw Fault.getServiceException( Fault.UNKNOWN );
            }
        }
    }
}
