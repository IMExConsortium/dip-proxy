package edu.ucla.mbi.services;

/*===========================================================================
 * $HeadURL::                                                               $
 * $Id::                                                                    $
 * Version: $Rev::                                                          $
 *===========================================================================
 *
 * FaultDef:
 *
 *    fault definitions
 *
 *========================================================================= */

import java.util.Map;
import java.util.HashMap;

public class Fault {

    public static final int MISSING_ID = 2;
    public static final int INVALID_ID = 3;
    public static final int UNSUPPORTED_OP = 4;
    public static final int NO_RECORD = 5;
    public static final int MARSHAL = 6;
    public static final int TRANSFORM = 7;
    public static final int QUERY_TYPE = 8;

    public static final int REMOTE_TIMEOUT = 12;
    public static final int REMOTE_FAULT = 13;

    public static final int DUPLICATE_ENTRY = 22;
    public static final int TRANSACTION = 23;
    public static final int FORMAT = 24;

    public static final int UNKNOWN = 99;

    private static ObjectFactory faultFactory;
    private static Map<Integer,ServiceFault> fault;

    static {

	fault = new HashMap<Integer,ServiceFault>();
	faultFactory = new ObjectFactory();

	fault.put(  2, 
		    Fault.createServiceFault( 2, "missing identifier" ) ); 
	fault.put(  3, 
		    Fault.createServiceFault( 3, "invalid identifier" ) ); 
	fault.put(  4, 
		    Fault.createServiceFault( 4, "unsupported operation" ) ); 
	fault.put(  5, 
		    Fault.createServiceFault( 5, "no record found" ) ); 
	fault.put(  6, 
		    Fault.createServiceFault( 6, "marshaling error" ) ); 
	fault.put(  7, 
		    Fault.createServiceFault( 7, "transformation error" ) ); 
	fault.put(  8, 
		    Fault.createServiceFault( 8, "invalid query type" ) ); 

	fault.put( 12, 
		   Fault.createServiceFault( 12, "remote server timeout" ) ); 
	fault.put( 13, 
		   Fault.createServiceFault( 13, "remote server fault" ) ); 
	
	fault.put( UNKNOWN,
		   Fault.createServiceFault( UNKNOWN, 
					     "unrecognized error" ) ); 
    }
    
    private static ServiceFault createServiceFault(int code, 
						   String message ) {
	
	ServiceFault sf = faultFactory.createServiceFault();
	sf.setFaultCode( code );
	sf.setMessage( message );
	return sf;
    }
    
    public static final ServiceFault getServiceFault() {
	return fault.get( UNKNOWN );
    }

    public static final ServiceException getServiceException() {
	return new ServiceException( fault.get( UNKNOWN ) );
    }
	    
    public static final ServiceFault getServiceFault( int code ) {
	
	if( fault.get( code ) != null ){
	    return fault.get( code );
	} else {
	    return fault.get( UNKNOWN );
	}
    }

    public static final ServiceException getServiceException( int code ) {
	
	if( fault.get( code ) != null ){
	    return new ServiceException( fault.get( code ) );
	} else {
	    return new ServiceException( fault.get( UNKNOWN ) );
	}
    }

}