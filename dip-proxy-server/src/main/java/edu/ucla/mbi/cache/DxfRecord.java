package edu.ucla.mbi.cache;

/* #=======================================================================
   # $Id::                                                                $
   # Version: $Rev::                                                      $
   #=======================================================================
   #                                                                       
   # Dxf Cache record                                                      
   #   stores string representation of native data identified              
   #   by ns/ac pair as returned by a provider/service and transformed     
   #into DXF at the detail level                                           
   #                                                                       
   #==================================================================== */

import java.util.*;
import java.io.Serializable;

public class DxfRecord implements Record {

    private int id  = -1;
    
    private String  provider = "";
    private String  service = "";    
    private String  ns = "";
    private String  ac = "";
    private String  detail = "";
   
    private String dxf = "";   // dip representation

    private Date createTime = null;
    private Date expireTime = null;   

    public DxfRecord() { };
    
    public DxfRecord( String provider, String service,
                      String ns, String ac, String detail ){
        
        this.provider = provider;
        this.service = service;
        this.ns = ns;
        this.ac = ac;
        this.detail = detail;
        this.createTime = Calendar.getInstance().getTime();
    }
    
    // setters
    //--------

    public DxfRecord setId( int id ) {
        this.id = id;
        return this;
    }

    public DxfRecord setProvider( String provider ) {
        this.provider = provider;
        return this;
    }

    public DxfRecord setService( String service ) {
        this.service = service;
        return this;
    }
    
    public DxfRecord setNs( String ns ) {
        this.ns = ns;
        return this;
    }
    
    public DxfRecord setAc( String ac ) {
        this.ac = ac;
        return this;
    }

    public DxfRecord setDetail( String detail ) {
        this.detail = detail;
        return this;
    }
    
    public DxfRecord setDxf( String dxf ) {
        this.dxf = dxf;
        return this;
    }

    public DxfRecord setCreateTime( Date time ) {
        this.createTime = time;
        return this;
    }

    public DxfRecord setExpireTime( Date time ) {
        this.expireTime = time;
	return this;
    }
    
    // getters
    //--------

    public int getId() {
        return id;
    }

    public String getProvider(){
        return provider;
    }

    public String getService(){
        return service;
    }
    
    public String getNs() {
        return ns;
    }

    public String getAc() {
        return ac;
    }

    public String getDetail() {
        return detail;
    }

    public String getDxf() {
        return dxf;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public Date getExpireTime() {
        return expireTime;
    }
    

    public String toString() {

        int lastPos = dxf.length()-1;
        if( lastPos > 48 ){
            lastPos = 48;
        }
        
      String str = "[DxfRecord: id= " + id + "; ns= " + ns + 
          ";ac = " + ac + ";detail = "+ detail + 
          ";dxf = " + dxf.substring(0,lastPos) + 
          "\n;createTime " + createTime.toString() + "]";
      return str;
    }


    public void setCreateTime() {
        createTime = Calendar.getInstance().getTime();
    }

    public void resetExpireTime( int ttl ) {
	
        if(createTime == null){
            createTime = Calendar.getInstance().getTime();
        }

        Calendar expCal = Calendar.getInstance();
        expCal.setTime( createTime );
        expCal.add( Calendar.SECOND, ttl );
        expireTime = expCal.getTime();
    }  

    public void resetExpireTime( Date now, int ttl ) {

        if( createTime == null ) {
            createTime = Calendar.getInstance().getTime();
        }

        Calendar expCal = Calendar.getInstance();
        expCal.setTime( now );
        expCal.add( Calendar.SECOND, ttl );
        expireTime = expCal.getTime();
    }
 
}
