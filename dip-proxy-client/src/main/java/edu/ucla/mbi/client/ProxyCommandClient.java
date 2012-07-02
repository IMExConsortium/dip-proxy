package edu.ucla.mbi.client;

/*==============================================================================
 * $HeadURL$
 * $Id$
 * Version: $Rev$
 *==============================================================================
 *
 * ProxyCommandClient:
 *
 *=========================================================================== */

import edu.ucla.mbi.dxf14.*;
import edu.ucla.mbi.proxy.*;

import javax.xml.bind.*;
import java.io.*;

import javax.xml.ws.BindingProvider;
import javax.xml.ws.Holder;
import com.sun.xml.ws.developer.JAXWSProperties;

import javax.xml.datatype.XMLGregorianCalendar;

public class ProxyCommandClient {
    
    public static void main( java.lang.String[] args ) {

        String sr = args[0]; // service URL
        String provider = args[1]; // provider
        String service = args[2];  // service

        int timeout = 300 * 1000; // client TimeOut 5 mins
        String ac = "";
        String ns = "";
        String detail = "stub";
        String format = "dxf";

        for ( int i = 3; i < args.length; i++ ) {
            if ( args[i].startsWith( "AC=" ) ) {
                ac = args[i].replaceFirst( "AC=", "" );
            }

            if ( args[i].startsWith( "NS=" ) ) {
                ns = args[i].replaceFirst( "NS=", "" );
            }


            if ( args[i].startsWith( "DETAIL=" ) ) {
                detail = args[i].replaceFirst( "DETAIL=", "" );
            }
            
            if ( args[i].startsWith( "FORMAT=" ) ) {
                format = args[i].replaceFirst( "FORMAT=", "" );
            }

        }

        System.out.println( "SRV: " + sr + " ->Provider: " + provider
                            + " ->service: " + service
                            + " -> AC=" + ac
                            + " -> NS=" + ac
                            + " -> FORMAT=" + format
                            + " -> DETAIL=" + detail );

        try {

            ProxyService proxyService = new ProxyService();
            ProxyPort port = proxyService.getProxyPort();


            // set server location
            // ---------------------

            ((BindingProvider) port).getRequestContext().put(
                    BindingProvider.ENDPOINT_ADDRESS_PROPERTY, sr );

            // set client Timeout
            // ------------------

            ((BindingProvider) port).getRequestContext().put(
                    JAXWSProperties.CONNECT_TIMEOUT, timeout );
            System.out.println( "Proxy: ProxyService=" + service
                                + " port=" + port );

            Holder<DatasetType> resDataset = new Holder<DatasetType>();
            Holder<String> resNative = new Holder<String>();
            Holder<XMLGregorianCalendar> timestamp =
                                new Holder<XMLGregorianCalendar>();

            String resultStr;

            port.getRecord( provider, service, ns, ac, "", detail, format, "", 0,
                            timestamp, resDataset, resNative );

            edu.ucla.mbi.dxf14.ObjectFactory dof =
                                new edu.ucla.mbi.dxf14.ObjectFactory();

            if( format.equalsIgnoreCase( "dxf" )
                    || format.equalsIgnoreCase( "both" ) )
            {
                DatasetType dataset = resDataset.value;

                JAXBContext jc = DxfJAXBContext.getDxfContext();

                Marshaller marshaller = jc.createMarshaller();
                marshaller.setProperty( Marshaller.JAXB_FORMATTED_OUTPUT,
                                    new Boolean( true ) );

                java.io.StringWriter sw = new StringWriter();
                marshaller.setProperty( Marshaller.JAXB_ENCODING, "UTF-8" );

                marshaller.marshal( dof.createDataset( dataset ), sw );

                resultStr = sw.toString();

                System.out.println( "DXF Result:" );
                System.out.println( resultStr );
            }

            if ( format.equalsIgnoreCase( "native" )
                    || format.equalsIgnoreCase( "both" ) )
            {
                resultStr = resNative.value;
                System.out.println( "NATIVE Result:" );
                System.out.println( resultStr );
            }

        } catch ( Exception e ) {
            System.out.println( e.toString() );
        }
    }


}

