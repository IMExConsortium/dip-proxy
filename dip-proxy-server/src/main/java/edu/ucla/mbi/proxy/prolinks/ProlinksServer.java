package edu.ucla.mbi.proxy.prolinks;

/*==============================================================================
 * $HeadURL:: https://wyu@imex.mbi.ucla.edu/svn/dip-ws/dip-proxy/trunk/dip-prox$
 * $Id::                                                                       $
 * Version: $Rev::                                                             $
 *==============================================================================
 *
 * ProlinksServer:
 *    services provided by Prolinks web services
 *
 *=========================================================================== */

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.ucla.mbi.cache.NativeRecord;
import edu.ucla.mbi.dxf14.*;
import edu.ucla.mbi.proxy.*;
import edu.ucla.mbi.proxy.ncbi.*;
import edu.ucla.mbi.fault.*;

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
            String ac, int timeOut ) throws ProxyFault {

        Log log = LogFactory.getLog( ProlinksServer.class );
        String retVal = null;

        log.info( " getNative: NS=" + ns + " AC=" + ac + " SRV=" + service );

        if ( service.equals( "prolinks" ) ) {

            String url = getRestUrl();
            String acTag = getRestAcTag();

            log.info( "getNative restURL=" + url );
            log.info( "getNative restTag=" + acTag );

            url = url.replaceAll( acTag, ac );
            log.info( "getNative: query url=" + url );
            retVal = NativeURL.query( url, timeOut );
        }

        if ( retVal == null ) {
            log.info( "getNative  null returned" );
            throw FaultFactory.newInstance( Fault.NO_RECORD ); // no hits
        }

        NativeRecord record = new NativeRecord( provider, service, ns, ac );
        record.setNativeXml( retVal );
        return record;
    }

    public DatasetType buildDxf( String strNative, String ns, String ac,
                                 String detail, String service, 
                                 ProxyTransformer pTrans 
                                 ) throws ProxyFault {

        Log log = LogFactory.getLog( ProlinksServer.class );
        log.info( " buildDxf called: " + ac );

        try {
            // -------------------------------------------------------------

            ProxyService proxySrv = new ProxyService();
            ProxyPort port = proxySrv.getProxyPort();

            // set server location
            // ---------------------

            ((BindingProvider) port).getRequestContext()
                    .put( BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                            ncbiProxyAddress );

            // set client Timeout
            // ------------------
            JAXBContext dxfJc = DxfJAXBContext.getDxfContext();
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

                        port.getRecord( "NCBI", "refseq", "refseq", node_ac, "", 
                                        detail, "", "", 0, timestamp, 
                                        resDataset, resNative );

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

                        throw FaultFactory.newInstance( Fault.UNKNOWN );
                    }
                }
            }
            return dxfResult;
        } catch ( Exception e ) {
            log.info( "ProlinksServer: " + e.toString() );
            String fault = e.toString();
            if ( fault.contains( "03" ) ) {
                throw FaultFactory.newInstance( Fault.UNKNOWN );
            } else {
                throw FaultFactory.newInstance( Fault.UNKNOWN );
            }
        }
    }
}
