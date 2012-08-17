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
import edu.ucla.mbi.server.*;

import javax.xml.bind.*;
import java.io.*;
import java.util.*;
import java.util.regex.*;

import javax.xml.ws.BindingProvider;
import com.sun.xml.ws.developer.JAXWSProperties;
import javax.xml.ws.Holder;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.transform.stream.StreamSource;


public class ProlinksServer extends RemoteNativeServer {

    private String ncbiProxyAddress = null;

    public void setNcbiProxyAddress( String ncbiProxy ) {
        Log log = LogFactory.getLog( ProlinksServer.class );

        ncbiProxy = ncbiProxy.replaceAll( "^\\s*", "" );
        ncbiProxy = ncbiProxy.replaceAll( "\\s*$", "" );
        log.info( " NcbiProxy=" + ncbiProxy );
        this.ncbiProxyAddress = ncbiProxy;
    }

    public NativeRecord getNative( String provider, String service, String ns,
                                   String ac, int timeOut ) throws ProxyFault 
    {
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
            
            try {
                retVal = NativeURL.query( url, timeOut );
            } catch ( ProxyFault fault ) {
                throw fault;
            }
        }

        Pattern pattern = Pattern.compile( "<faultCode>(\\d+)</faultCode>" );
        Matcher matcher = pattern.matcher( retVal );
        if( matcher.find() ) {
            String faultCode = matcher.group(1);
            
            if( faultCode.equals("97") ) {
                log.info( "query: return faultCode 97. " );
                throw FaultFactory.newInstance( Fault.REMOTE_FAULT );
            } else if ( faultCode.equals( "5" ) ) {
                log.info( "query: return faultCode 5. " );
                throw FaultFactory.newInstance( Fault.NO_RECORD );
            } else {
                log.info( "query: return faultCode=" + faultCode );
                throw FaultFactory.newInstance( Fault.REMOTE_FAULT );
            }
        }

        NativeRecord record = new NativeRecord( provider, service, ns, ac );
        record.setNativeXml( retVal );
        return record;
    }

    
    public DatasetType buildDxf( String strNative, String ns, String ac,
                                 String detail, String service, 
                                 ProxyTransformer pTrans 
                                 ) throws ProxyFault 
    {
        Log log = LogFactory.getLog( ProlinksServer.class );
        log.info( " buildDxf called: " + ac );
        
        edu.ucla.mbi.dxf14.DatasetType dxfResult = 
                super.buildDxf ( strNative, ns, ac, detail, service, pTrans );
       
        if( detail.equals( "full" ) ) {
            //*** take detail info of refseq node from NCBI service    
            ProxyService proxySrv = new ProxyService();
            ProxyPort port = proxySrv.getProxyPort();

            RemoteServerContext rsc = WSContext.getServerContext( "NCBI" );

            // set server location 
            // ---------------------

            ((BindingProvider) port).getRequestContext()
                    .put( BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                            ncbiProxyAddress );

            // set client Timeout
            // ----

            ((BindingProvider) port).getRequestContext().put(
                    JAXWSProperties.CONNECT_TIMEOUT, rsc.getTimeout() );

            List<NodeType> node = dxfResult.getNode();
            
            for ( Iterator iterator = node.iterator(); iterator.hasNext(); ) {
                NodeType nodetype = (NodeType) iterator.next();
                List<edu.ucla.mbi.dxf14.NodeType.PartList.Part> part =
                nodetype.getPartList().getPart();
                
                for ( Iterator iterator1 = part.iterator(); 
                      iterator1.hasNext(); ) 
                {
            
                    PartType parttype = (PartType) iterator1.next();
                    NodeType nodeOld = parttype.getNode();
                    String node_ac = nodeOld.getAc();
                    long node_id = nodeOld.getId();

                    try {
                        log.info( "ProlinksServer: port.getRefseq call "
                                  + "(loop):"
                                  + " NS=refseq" + " AC=" + node_ac + " DT="
                                  + detail );

                        Holder<DatasetType> resDataset =
                                    new Holder<DatasetType>();
                        Holder<String> resNative = new Holder<String>();
                        Holder<XMLGregorianCalendar> timestamp = null;

                        port.getRecord( "NCBI", "refseq", "refseq", node_ac, 
                                        "", "base", "", "", 0, timestamp, 
                                        resDataset, resNative );

                        DatasetType dataset = resDataset.value;

                        NodeType nodeNew = 
                                (NodeType) dataset.getNode().get( 0 );
                        nodeNew.setId( node_id );
                        parttype.setNode( nodeNew );
                    } catch ( ProxyFault fault ) {
                        throw fault;
                    } catch ( Exception e ) {
                        log.info( "ProlinksServer: NCBI getRefseq: "
                                  + e.toString() );
                        throw FaultFactory.newInstance( Fault.UNKNOWN );
                    }
                }
            }
        }
        return dxfResult;
    }
}
