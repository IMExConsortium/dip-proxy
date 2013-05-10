package edu.ucla.mbi.fault;

/*===========================================================================
 * $HeadURL::                                                               $
 * $Id::                                                                    $
 * Version: $Rev::                                                          $
 *===========================================================================
 *
 * ServerFault:
 *
 *========================================================================= */

public class ServerFault extends Exception {
    
    protected int faultCode;
    protected String message;
    
    public ServerFault( String message, int code ){
        this.faultCode = code;
        this.message = message;
    }
    
    
    public int getFaultCode() {
        return faultCode;
    }

    public void setFaultCode(int value) {
        this.faultCode = value;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String value) {
        this.message = value;
    }

}
