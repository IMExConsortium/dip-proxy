package edu.ucla.mbi.fault;

public class ServerFault extends Exception {

    private int code;
    private String message;

    public ServerFault(){
        code=99;
        message="UNKNOWN";
        
    }

    public ServerFault( String message, int code ){
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;        
    }

    public String getMessage() {
        return message;
    }

}
