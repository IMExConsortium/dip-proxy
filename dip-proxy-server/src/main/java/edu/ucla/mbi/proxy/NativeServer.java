package edu.ucla.mbi.proxy;

/*==============================================================================
 * $HeadURL::                                                                  $
 * $Id::                                                                       $
 * Version: $Rev::                                                             $
 *==============================================================================
 *
 * NativeServer:
 *    An interface is of one function getNative(...)
 *
 *=========================================================================== */

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.net.*;
import java.io.*;
import java.util.Map;

import edu.ucla.mbi.proxy.*;
import edu.ucla.mbi.cache.NativeRecord;

import edu.ucla.mbi.fault.*;

public interface NativeServer {

    public NativeRecord getNativeRecord( String provider, String service, 
                                         String ns, String ac, int timeout 
                                         ) throws ServerFault; 

}
