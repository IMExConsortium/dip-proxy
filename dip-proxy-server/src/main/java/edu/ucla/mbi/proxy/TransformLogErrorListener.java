package edu.ucla.mbi.proxy;

/*===========================================================================
 * $HeadURL::                                                               $
 * $Id::                                                                    $
 * Version: $Rev::                                                          $
 *===========================================================================
 *
 * TransformLogErrorListener:
 *
 *========================================================================= */


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import javax.xml.transform.*;

public class TransformLogErrorListener implements ErrorListener{

    public void warning( TransformerException exception ) {

	    Log log = LogFactory.getLog( TransformLogErrorListener.class );
	    log.info( "TransformLogErrorListener: warning: " + 
		exception.getMessage() );
    }
    
    public void error( TransformerException exception ) 
                                        throws TransformerException 
    {
        Log log = LogFactory.getLog( TransformLogErrorListener.class );
	    log.info( "TransformLogErrorListener: error: " + 
		exception.getMessage() );
	    throw exception;
    }

    public void fatalError( TransformerException exception ) 
                                        throws TransformerException 
    {
	    Log log = LogFactory.getLog( TransformLogErrorListener.class );
	    log.info( "TransformLogErrorListener: fatalError: " + 
		exception.getMessage() );
	    throw exception;
    }
}

