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

import javax.xml.bind.util.JAXBResult;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBContext;

import javax.xml.transform.stream.StreamSource;

import edu.ucla.mbi.dxf14.DatasetType;
import edu.ucla.mbi.dxf14.DxfJAXBContext;

import edu.ucla.mbi.server.WSContext;
import edu.ucla.mbi.fault.*;

public class ProxyDxfTransformer {

    public ProxyDxfTransformer(){}
    
    public DatasetType transform( String strNative,
                                  String ns, String ac, String detail,
                                  String provider, String service 
                                  ) throws ProxyFault {

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
	        ProxyTransformer pTrans = WSContext.getTransformer();
                
            //synchronize{

                pTrans.setTransformer( provider, service );
                pTrans.setParameters( detail, ns, ac );
                pTrans.transform( ssNative, result );
                
            //}
                
            DatasetType dxfResult  = 
                (DatasetType) ( (JAXBElement) result.getResult() ).getValue();
                
            //*** test if dxfResult is empty
            if ( dxfResult.getNode().isEmpty() 
                 || dxfResult.getNode().get(0).getAc().equals("") ) {
                    
                throw FaultFactory.newInstance( Fault.TRANSFORM );  
	        }	    
              
            return dxfResult;
                
	    } catch ( ProxyFault fault ) { 
	        log.info( "Transformer fault: empty dxfResult ");
	        throw fault;
        } catch ( Exception e ) {
	        throw FaultFactory.newInstance( Fault.TRANSFORM );  
	    }   
    }
    
    public DatasetType buildDxf( String strNative, String ns, String ac,
                                 String detail, String provider, 
                                 String service ) throws ProxyFault 
    {
	
    	// NOTE: overload if dxf building more complex than
        //       a simple xslt transformation
        
        DatasetType trResult = this.transform( strNative, ns, ac, detail, provider, service );

        
        if( !provider.equalsNoCase("mbi") || !service.equalsNoCase("prolinks") ){
            return trResult;
        } 
	
        if( detail.equalsNoCase("FULL") ){
            //return this.buildProlinksDxf( strNative, ns, ac, detail, provider, service );
            return this.buildProlinksDxf( trResult,  detail );
        }

        return trResult;
        
    }

    //--------------------------------------------------------------------------


    //public DatasetType buildProlinksDxf( String strNative, String ns, String ac,
    //                                     String detail, String provider, 
    //                                     String service ) throws ProxyFault 
    //{

    public DatasetType buildProlinksDxf( DatasetType dxfResult , String detail ) 
        throws ProxyFault {
        
        Log log = LogFactory.getLog( ProxyDxfTransformer.class );
        log.info( " buildProlinksDxf called: " + ac );
        
        //String ncbiProxyAddress = ( String ) getContext().get( "ncbiProxyAddress" );  //XX


        //String ncbiProxyAddress = ( String ) WSContext...... .get( "ncbiProxyAddress" );


        if( ncbiProxyAddress != null &&  ncbiProxyAddress.length() > 0 ) {
            ncbiProxyAddress = ncbiProxyAddress.replaceAll( "\\s", "" );
        } else {
            log.warn( "buildDxf: ncbiProxyAddress is not initialized. " );
            throw FaultFactory.newInstance( Fault.REMOTE_FAULT );
        }

        //edu.ucla.mbi.dxf14.DatasetType dxfResult = 
        //    this.buildDxf ( strNative, ns, ac, detail, provider, service );
        
     
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
        
        return dxfResult;
    }
}
