package edu.ucla.mbi.proxy.ebi;

/*===========================================================================
 * $HeadURL: https://wyu@imex.mbi.ucla.edu/svn/dip-ws/trunk/dip-proxy/src/#$
 * $Id$
 * Version: $Rev$
 *===========================================================================
 *
 * ProxyTransformer:
 *
 *========================================================================= */

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;
import java.io.*;
import org.w3c.dom.*;
import javax.xml.parsers.*;

import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.StreamSource;
import javax.xml.bind.util.JAXBResult;

import org.springframework.core.io.*;

import edu.ucla.mbi.proxy.*;

public class EbiProxyTransformer extends ProxyTransformer {
 
    public void transform( StreamSource xmlStreamSource, 
                           JAXBResult jaxbResult) {
	    
	Log log = LogFactory.getLog( ProxyTransformer.class );
	
	try {
	    getTransformer().transform(xmlStreamSource, jaxbResult );
	    
	    // jaxbResult contains transformed (dxf) data  
	    //-------------------------------------------
	    
	    
	    // substitute taxon info...
	    
	    
	    
	}catch(Exception e){
	    log.info("Transformation error="+e.toString());
	    // NOTE: should throw exception/fault
	}
    }
}
