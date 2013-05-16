package edu.ucla.mbi.proxy;

/*==============================================================================
 * $HeadURL::                                                                  $
 * $Id::                                                                       $
 * Version: $Rev::                                                             $
 *==============================================================================
 *
 * ProxyServerRecord 
 *
 *=========================================================================== */

import edu.ucla.mbi.dxf14.*;
import edu.ucla.mbi.proxy.*;
import edu.ucla.mbi.fault.*;
import edu.ucla.mbi.util.TimeStamp;

import edu.ucla.mbi.cache.*;
import edu.ucla.mbi.proxy.router.Router;
import edu.ucla.mbi.proxy.context.*;

import java.util.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.jws.WebService;
import javax.xml.ws.Holder;
import javax.xml.datatype.XMLGregorianCalendar;


public class ProxyServerRecord  {

    private DatasetType dataset;
    private String nativeRecord;
    private XMLGregorianCalendar timestamp;
    
    public ProxyServerRecord( DatasetType dataset,
                              String nativeRecord,
                              XMLGregorianCalendar timestamp ) {

        this.dataset = dataset;
        this.nativeRecord = nativeRecord;
        this.timestamp = timestamp;
    }

    public DatasetType getDataset() {
        return dataset;
    }

    public String getNativeRecord() {
        return nativeRecord;
    }

    public XMLGregorianCalendar getTimestamp() {
        return timestamp;
    }


}
