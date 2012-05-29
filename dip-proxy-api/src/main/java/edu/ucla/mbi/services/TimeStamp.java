package edu.ucla.mbi.services;

/*===========================================================================
 * $HeadURL::                                                               $
 * $Id::                                                                    $
 * Version: $Rev::                                                          $
 *===========================================================================
 *
 * TimeStamp:
 *
 *    utility class - time format conversions
 *
 *========================================================================= */

import java.util.Date;
import java.util.Calendar;
import java.util.GregorianCalendar;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.datatype.DatatypeConfigurationException;

import javax.xml.datatype.XMLGregorianCalendar;

public class TimeStamp {
    
    private static DatatypeFactory df;
    
    static {
	try {
	    df = DatatypeFactory.newInstance();
	} catch( DatatypeConfigurationException dce ) {
	    df = null;
	}
    }

    private TimeStamp() {}
    
    public static XMLGregorianCalendar toXmlDate( Date d ) {
	
	GregorianCalendar gc = new GregorianCalendar();
	gc.setTime( d );
	
	XMLGregorianCalendar xgc = df.newXMLGregorianCalendar( gc );
	return xgc.normalize() ;
    }
}
