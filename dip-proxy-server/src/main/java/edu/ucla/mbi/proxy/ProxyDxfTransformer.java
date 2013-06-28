package edu.ucla.mbi.proxy;

/*==============================================================================
 * $HeadURL::                                                                  $
 * $Id::                                                                       $
 * Version: $Rev::                                                             $
 *==============================================================================
 *
 * ProxyDxfTransformer:
 *
 *=========================================================================== */

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
 
import java.io.*;
import java.util.*;

import javax.xml.bind.util.JAXBResult;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBContext;
import javax.xml.transform.stream.StreamSource;

import edu.ucla.mbi.dxf14.*;
import edu.ucla.mbi.proxy.context.*;
import edu.ucla.mbi.fault.*;

public class ProxyDxfTransformer {
    
    private WSContext wsContext;
    
    public ProxyDxfTransformer( WSContext context ){
        wsContext = context;
    }

    private RemoteServerContext rsc;
    
    public ProxyDxfTransformer( RemoteServerContext rsc ){
        this.rsc = rsc;
    }

    /*  
    private DatasetType transform( String strNative,
                                   String ns, String ac, String detail,
                                   String provider, String service 
                                   ) throws ServerFault {

	    Log log = LogFactory.getLog( ProxyDxfTransformer.class );
	    
        try {
	        //*** native data in string representationa as input
                
	        ByteArrayInputStream bisNative =
		    new ByteArrayInputStream( strNative.getBytes( "UTF-8" ) );
	        StreamSource ssNative = new StreamSource( bisNative );
                
	        //*** dxf as JAXBResult result of the transformation
                
	        JAXBContext dxfJc = DxfJAXBContext.getDxfContext();
	        JAXBResult result = new JAXBResult( dxfJc );
                
	        //*** transform into DXF
	        ProxyTransformer pTrans = wsContext.getTransformer();
                
            synchronized ( pTrans ) {
                pTrans.setTransformer( provider, service );
                pTrans.setParameters( detail, ns, ac );
                pTrans.transform( ssNative, result );
            }
    
            DatasetType dxfResult  = 
                (DatasetType) ( (JAXBElement) result.getResult() ).getValue();
                
            //*** test if dxfResult is empty
            if ( dxfResult.getNode().isEmpty() 
                 || dxfResult.getNode().get(0).getAc().equals("") ) {
                    
                throw ServerFaultFactory.newInstance( Fault.TRANSFORM );  
	        }	    
              
            return dxfResult;
                
	    } catch ( ServerFault fault ) { 
	        log.info( "Transformer fault: empty dxfResult ");
	        throw fault;
        } catch ( Exception e ) {
	        throw ServerFaultFactory.newInstance( Fault.TRANSFORM );  
	    }   
    } */
    
    public DatasetType buildDxf( String strNative, String ns, String ac,
                                 String detail, String provider, 
                                 String service ) throws ServerFault {

        //*** transform into DXF
        //ProxyTransformer pTrans = wsContext.getTransformer();
        ProxyTransformer pTrans = wsContext.getTransformer();

        synchronized ( pTrans ) {
            pTrans.setTransformer( provider, service );
            pTrans.setParameters( detail, ns, ac );
            return pTrans.transform( strNative, detail );
        }

        /*	
        DatasetType trResult = this.transform( strNative, ns, ac, detail, 
                                               provider, service );

        
        if( provider.equalsIgnoreCase("mbi") 
            && service.equalsIgnoreCase("prolinks") 
            && detail.equalsIgnoreCase("full") ) {

            return this.buildProlinksDxf( trResult );

        } else {
            return trResult;
        }*/
    }

    //--------------------------------------------------------------------------

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

                //if( nodeOld.getNs().equals( "refseq" ) ) { // the ns="prl" in prolinks
                if( node_ac.startsWith( "NP_" ) ) {
                    try {
                    
                        log.info( "ProlinksServer: port.getRefseq call " +
                                  "(loop): NS=refseq" + " AC=" + node_ac ); 

                        CachingService cachingSrv = 
                            new CachingService( wsContext, "NCBI" );

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
