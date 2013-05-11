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


public class ProlinksServer implements NativeServer{

    private Log log = LogFactory.getLog( ProlinksServer.class );
    private NativeRestServer nativeRestServer = null;
    private Map<String,Object> context = null;

    public void setContext( Map<String,Object> context ) {
        this.context = context;
    }

    public void initialize() throws ServerFault {
        if(  context == null ) {
            log.warn( "ProlinksServer: initializing failed " +
                      "because context is null. " );
            throw ServerFaultFactory.newInstance( Fault.JSON_CONFIGURATION );
        }

        nativeRestServer = (NativeRestServer)context.get( "nativeRestServer" );

        if( nativeRestServer == null ) {
            log.warn( "ProlinksServer: initializing failed " +
                      "because nativeRestServer is null. " );
            throw ServerFaultFactory.newInstance( Fault.JSON_CONFIGURATION );
        }
    }

    public NativeRecord getNative( String provider, String service,
                                   String ns, String ac, int timeout  
                                   ) throws ServerFault {
        
        return nativeRestServer.getNative( provider, service, ns, ac, timeout );
    }



    /*

    public DatasetType buildDxf( String strNative, String ns, String ac,
                                 String detail, String provider, 
                                 String service ) throws ServerFault 
    {
        Log log = LogFactory.getLog( ProlinksServer.class );
        log.info( " buildDxf called: " + ac );

        String ncbiProxyAddress = ( String ) getContext().get( "ncbiProxyAddress" );

        if( ncbiProxyAddress != null &&  ncbiProxyAddress.length() > 0 ) {
            ncbiProxyAddress = ncbiProxyAddress.replaceAll( "\\s", "" );
        } else {
            log.warn( "buildDxf: ncbiProxyAddress is not initialized. " );
            throw ServerFaultFactory.newInstance( Fault.REMOTE_FAULT );
        }

        edu.ucla.mbi.dxf14.DatasetType dxfResult = 
                super.buildDxf ( strNative, ns, ac, detail, provider, service );
       
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
                    } catch ( ServerFault fault ) {
                        throw fault;
                    } catch ( Exception e ) {
                        log.info( "ProlinksServer: NCBI getRefseq: "
                                  + e.toString() );
                        throw ServerFaultFactory.newInstance( Fault.UNKNOWN );
                    }
                }
            }
        }
        return dxfResult;
    }
    */

}
