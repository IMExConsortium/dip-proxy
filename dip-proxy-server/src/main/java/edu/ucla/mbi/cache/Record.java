package edu.ucla.mbi.cache;

/* #=======================================================================
   # $Id::                                                                $
   # Version: $Rev::                                                      $
   #=======================================================================
   #
   # Record: 
   #     stores string representation of a database record identified by
   #     by ns/ac pair as returned by a server
   #
   #==================================================================== */

import java.util.*;
import java.io.Serializable;

public interface Record extends Serializable{

    public Record setId(int id);
    public Record setNs(String ns);
    public Record setAc(String ac);

    public Record setProvider(String server);
    public Record setService(String server);

    public Record setCreateTime(Date curtime);
    public Record setExpireTime(Date expiretime);

    public int getId();
    public String getNs();
    public String getAc();

    public String getProvider();
    public String getService();

    public Date getCreateTime();
    public Date getExpireTime();
}
