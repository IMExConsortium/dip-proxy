package edu.ucla.mbi.proxy.prolinks;

/*==============================================================================
 * $HeadURL$
 * $Id$
 * Version: $Rev$
 *==============================================================================
 *
 * prolinksTransformer:
 *
 *=========================================================================== */

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.ucla.mbi.fault.*;
import edu.ucla.mbi.dxf14.*;
import edu.ucla.mbi.proxy.*;
import edu.ucla.mbi.proxy.context.*;

import java.util.*;

public class prolinksTransformer extends ProxyTransformer {

    private WSContext wsContext;

    public void setWsContext( WSContext context ) {
        this.wsContext = context;
    }

    public DatasetType transform( String strNative, String detail ) throws ServerFault {

        DatasetType result = super.transform( strNative, detail );

        if( detail.equalsIgnoreCase("full") ) {

            return this.buildProlinksDxf( result );

        } 
            
        return result;
    }

    private DatasetType buildProlinksDxf( DatasetType dxfResult )
        throws ServerFault {

        Log log = LogFactory.getLog( ProxyDxfTransformer.class );

        List<NodeType> node = dxfResult.getNode();

        for ( Iterator iterator = node.iterator(); iterator.hasNext(); ) {
            NodeType nodetype = (NodeType) iterator.next();
            List<edu.ucla.mbi.dxf14.NodeType.PartList.Part> part =
            nodetype.getPartList().getPart();

            for ( Iterator iterator1 = part.iterator();
                  iterator1.hasNext(); ) {

                PartType parttype = (PartType) iterator1.next();
                NodeType nodeOld = parttype.getNode();
                String node_ac = nodeOld.getAc();
                long node_id = nodeOld.getId();

                if( nodeOld.getNs().equals( "refseq" ) ) { 
                    try {

                        log.info( "ProlinksServer: port.getRefseq call " +
                                  "(loop): NS=refseq" + " AC=" + node_ac );

                        CachingService cachingSrv = 
                            new CachingService( wsContext );

                        DatasetType dataset = cachingSrv.getDatasetType(
                            "NCBI", "refseq", "refseq", node_ac, "base" );

                        NodeType nodeNew =
                            (NodeType) dataset.getNode().get( 0 );

                        nodeNew.setId( node_id );

                        parttype.setNode( nodeNew );

                    } catch ( ServerFault fault ) {
                        throw fault;
                    }
                }
            }
        }

        return dxfResult;
    }

}
