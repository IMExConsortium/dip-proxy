package edu.ucla.mbi.cache;

/* #=======================================================================
   # $Id::                                                                $
   # Version: $Rev::                                                      $
   #=======================================================================
   #
   # NativeAudit: 
   #   
   #   
   #
   #==================================================================== */

import java.util.*;
import java.io.Serializable;

public class NativeAudit {

    private int id   = -1;

    private String provider ="";
    private String service = "";
    private String ns = "";
    private String ac = "";
   
    private Date time = null;
    private long response = 0;
    private int status = 0;

    public NativeAudit() { };


    public NativeAudit( String provider, String service, 
                        String ns, String ac,
                        Date time, long response) {
     
        this(provider,service,ns,ac,time,response, 0 );
    }


    public NativeAudit( String provider, String service, 
                        String ns, String ac,
                        Date time, long response, int status) {
	
        this.provider = provider;
        this.service = service;
        this.ns = ns;
	this.ac = ac;
	this.time = time;
	this.response = response;
	this.status = status;
    }

    // setters
    //--------
    
    public NativeAudit setId( int id) {
        this.id=id;
        return this;
    }
    
    public NativeAudit setProvider( String provider ) {
        this.provider = provider;
	return this;
    }

    public NativeAudit setService( String service ) {
        this.service = service;
	return this;
    }
     
    public NativeAudit setNs( String ns ) {
        this.ns = ns;
        return this;
    }
    
    public NativeAudit setAc( String ac ) {
        this.ac = ac;
        return this;
    }

    public NativeAudit setTime( Date time ) {
        this.time = time;
        return this;
    }
    public NativeAudit setResponseTime( long time) {
        this.response = time;
        return this;
    }

    public NativeAudit setStatus( int status ){
        this.status = status;
        return this;
    }
    
    // getters
    //--------
    
    public int getId(){
        return id;
    }
    
    public String getProvider(){
        return provider;
    }

    public String getService(){
        return service;
    }

    public String getNs(){
        return ns;
    }

    public String getAc(){
        return ac;
    }

    public Date getTime(){
        return time;
    }

    public long getResponseTime(){
        return response;
    }

    public int getStatus(){
        return status;
    }

}
