package edu.ucla.mbi.proxy.dht;

/* #=======================================================================
   # $Id::                                                                $
   # Version: $Rev::                                                      $
   #=======================================================================
   #
   # DhtProxyImpl - Dht Proxy access
   #
   #==================================================================== */

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Iterator;
import java.util.List;
import java.util.Calendar;
import java.util.Date;

import javax.jws.WebService;

import javax.xml.ws.Holder;

import edu.ucla.mbi.dxf14.*;
import edu.ucla.mbi.server.WSContext;
import edu.ucla.mbi.proxy.router.*;
import edu.ucla.mbi.proxy.*;

@WebService(endpointInterface="edu.ucla.mbi.proxy.dht.DhtProxyPort")

public class DhtProxyImpl extends ConfigurablePortImpl implements DhtProxyPort {

    public Result getDhtConfig() throws DhtFault {

        edu.ucla.mbi.proxy.dht.ObjectFactory dhtFactory = 
            new edu.ucla.mbi.proxy.dht.ObjectFactory();
        
        Result result = dhtFactory.createResult();
        
        edu.ucla.mbi.dxf14.ObjectFactory dxf
            = new edu.ucla.mbi.dxf14.ObjectFactory();
        
        DatasetType dht = dxf.createDatasetType();
        result.setDataset( dht );

        //-----------------------------------------------------------------
        
        Dht proxyDht = wsContext.getDht();

        return result;
    }

    
    
    //---------------------------------------------------------------------

    public void setDhtConfig( Holder<DatasetType> dataset )
        throws DhtFault {

        DatasetType dht = dataset.value;
    }
    

    //---------------------------------------------------------------------

    public DatasetType getDhtRecord( String provider, String service,
                                     String ns, String ac ) 
        throws DhtFault {

        Log log = LogFactory.getLog( DhtProxyImpl.class );

        edu.ucla.mbi.dxf14.ObjectFactory dxf
                = new edu.ucla.mbi.dxf14.ObjectFactory();
        
        DatasetType dht = dxf.createDatasetType();        

        Dht proxyDht = wsContext.getDht();

        int id=1;

        log.info( "dht: PR=" + provider + " SRV=" + service + 
                  " NS=" + ns + " AC=" + ac );

        List<DhtRouterList> list = 
           proxyDht.getDhtRouterList( provider, service, ns, ac );
        
        for ( Iterator<DhtRouterList> i = list.iterator(); i.hasNext(); ){
            
            DhtRouterList drl = i.next();
            
            NodeType drlNode = dxf.createNodeType();
            dht.getNode().add( drlNode );

            drlNode.setNs( ns );
            drlNode.setAc( ac );
            drlNode.setId( id++ );

            TypeDefType drlType = dxf.createTypeDefType();
            drlNode.setType( drlType ); 

            drlType.setNs( "dxf" );
            drlType.setAc( "dxf:999" );
            drlType.setName( "list" );
            
            NodeType.AttrList attl = dxf.createNodeTypeAttrList();
            drlNode.setAttrList( attl );
            
            for ( Iterator<DhtRouterItem> ii = drl.iterator(); 
                  ii.hasNext(); ){
                
                DhtRouterItem dri = ii.next();
            
                log.info( " dht item: ADR=" + dri.getAddress() + 
                          " CR= " + dri.getCreateTime() + 
                          " EX= " + dri.getExpireTime() );

                AttrType att =  dxf.createAttrType();
                attl.getAttr().add( att );
                att.setName( "proxy" );
                NodeType prnd = dxf.createNodeType();
                att.setNode( prnd );
                prnd.setNs( ns );
                prnd.setAc( ac );
                NodeType.AttrList nattl = dxf.createNodeTypeAttrList();
                prnd.setAttrList( nattl );
                
                AttrType attUrl =  dxf.createAttrType();
                attUrl.setName( "address" );
                AttrType.Value av = dxf.createAttrTypeValue();
                av.setValue( dri.getAddress() );
                attUrl.setValue( av );
                nattl.getAttr().add( attUrl );

                AttrType attCrt =  dxf.createAttrType();
                attCrt.setName( "creation-time" );
                AttrType.Value cv = dxf.createAttrTypeValue();
                Calendar cdt = Calendar.getInstance();
                cdt.setTimeInMillis( dri.getCreateTime() );
                cv.setValue( cdt.getTime().toString() );
                attCrt.setValue( cv );
                nattl.getAttr().add( attCrt );

                AttrType attExt =  dxf.createAttrType();
                attExt.setName( "expiration-time" );
                AttrType.Value ev = dxf.createAttrTypeValue();
                Calendar edt = Calendar.getInstance();
                edt.setTimeInMillis( dri.getExpireTime() );
                ev.setValue( edt.getTime().toString() );
                attExt.setValue( ev );
                nattl.getAttr().add( attExt );

                //nodeT = getDipNodeType(mapEntry.getValue(), mapEntry.getKey(), nodeid++);
            }
        }
        return dht;
    }
}
