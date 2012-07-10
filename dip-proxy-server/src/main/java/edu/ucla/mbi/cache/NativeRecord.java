package edu.ucla.mbi.cache;

/* #=======================================================================
   # $Id::                                                                $
   # Version: $Rev::                                                      $
   #=======================================================================
   #
   # Native Cache  Record: 
   #   stores string representation of native data identified
   #   by ns/ac pair as returned by a provider/service
   #
   #==================================================================== */

import java.util.*;
import java.io.Serializable;

public class NativeRecord implements Record {

    private int id   = -1;

    private String provider ="";
    private String service = "";
    private String ns = "";
    private String ac = "";
   
    private String nativeXml = "";   

    private Date createTime = null;
    private Date expireTime = null;
    private int ttl = 0;  // time-to-live (units: seconds)

    public NativeRecord() { };

    public NativeRecord( String provider, String service, 
                         String ns, String ac ) {
	
        this.provider = provider;
        this.service = service;
        this.ns = ns;
	    this.ac = ac;
	
	    this.createTime = Calendar.getInstance().getTime();
    }

    // setters
    //--------
    
    public NativeRecord setId( int id) {
        this.id=id;
        return this;
    }
    
    public NativeRecord setProvider( String provider ) {
        this.provider = provider;
	return this;
    }

    public NativeRecord setService( String service ) {
        this.service = service;
	return this;
    }
     
    public NativeRecord setNs( String ns ) {
        this.ns = ns;
        return this;
    }
    
    public NativeRecord setAc( String ac ) {
        this.ac = ac;
        return this;
    }
    
    public NativeRecord setNativeXml( String xml ) {
        this.nativeXml = xml;
        return this;
    }
    
    public NativeRecord setCreateTime( Date time ) {
        this.createTime = time;
        return this;
    }
    
    public NativeRecord setExpireTime( Date time ){
        this.expireTime = time;
        return this;
    }

    /* set time-to-live (units - seconds) */

    public NativeRecord setTtl( int time ){
        if ( time > 0 ){
            this.ttl = time;
        }
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

    public String getNativeXml(){
        return nativeXml;
    }

    public Date getCreateTime(){
        return createTime;
    }

    public Date getExpireTime(){
        return expireTime;
    }

    public int getTtl(){
        return ttl;
    }
    
    public String toString(){

        int lastPos = nativeXml.length()-1;
        if( lastPos > 48 ){
            lastPos = 48;
        }

        String str = "[NativeRecord: id= " + id + 
            "; ns= " + ns + ";ac = " + ac;

	    if ( nativeXml.length() > 0) {
 	        str = str + ";nativeXml = " + 
		    nativeXml.substring(0,lastPos); 
	    } else {
	        str = str + ";nativeXml = ()";
	    }
	
	    str = str + "\n;createTime " + 
	    createTime.toString() + "]";
	    return str;
    }
    
    
    public void setCreateTime(){
	    createTime = Calendar.getInstance().getTime();
    }

    public void resetExpireTime( int ttl ) {
	
	    if( createTime == null ) {
	        createTime = Calendar.getInstance().getTime();
	    }
                                                                           
	    Calendar expCal = Calendar.getInstance();
	    expCal.setTime( createTime );
	    expCal.add( Calendar.SECOND, ttl );	
	    expireTime = expCal.getTime();
        this.ttl = ttl;
    }

    public void resetExpireTime( Date now, int ttl ) {
	
	    if( createTime == null ) {
	        createTime = Calendar.getInstance().getTime();
	    }
                                                                           
	    Calendar expCal = Calendar.getInstance();
	    expCal.setTime( now );
	    expCal.add( Calendar.SECOND, ttl );	
	    expireTime = expCal.getTime();
        this.ttl = ttl;
    }

    public Date getQueryTime() {
	                                                                           
	    Calendar qCal = Calendar.getInstance();
	    qCal.setTime( expireTime );
	    qCal.add( Calendar.SECOND, -ttl );	
	
	    return qCal.getTime();
    }
}
