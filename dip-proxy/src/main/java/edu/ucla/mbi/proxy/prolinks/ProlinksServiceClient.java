package edu.ucla.mbi.proxy.prolinks;

/*===========================================================================
 * $HeadURL: https://wyu@imex.mbi.ucla.edu/svn/dip-ws/trunk/dip-proxy/src/#$
 * $Id$
 * Version: $Rev$
 *===========================================================================
 *
 * ProlinksServiceClient:
 *
 *========================================================================= */

import edu.ucla.mbi.dxf14.*;
import edu.ucla.mbi.proxy.*;

import javax.xml.bind.*;
import java.io.*;

import javax.xml.ws.BindingProvider;
import javax.xml.ws.Holder;

import com.sun.xml.ws.developer.JAXWSProperties;
import javax.xml.datatype.XMLGregorianCalendar;

public class ProlinksServiceClient {

    public static void main( java.lang.String[] args ) {

        String sr = args[0]; // service URL
        String op = args[1]; // operation

        String ac = "";
        String detail = "";
        int timeout = 300 * 1000; // client timeout 5 mins

        for ( int i = 2; i < args.length; i++ ) {
            if ( args[i].startsWith( "AC=" ) ) {
                ac = args[i].replaceFirst( "AC=", "" );
            }
            if ( args[i].startsWith( "DETAIL=" ) ) {
                detail = args[i].replaceFirst( "DETAIL=", "" );
            }
        }

        if ( detail.equals( "" ) ) {
            detail = "stub";
        }

        System.out.println( "SRV: " + sr + " OP: " + op + " -> AC=" + ac
                + " -> DETAIL=" + detail );

        try {
            ProlinksProxyService service = new ProlinksProxyService();

            ProlinksProxyPort port = service.getProxyPort();

            // set server location
            // ---------------------

            ((BindingProvider) port).getRequestContext().put(
                    BindingProvider.ENDPOINT_ADDRESS_PROPERTY, sr );

            // set client Timeout
            // ------------------

            ((BindingProvider) port).getRequestContext().put(
                    JAXWSProperties.CONNECT_TIMEOUT, timeout );

            System.out.println( "Proxy: ProlinksProxyService=" + service
                    + " port=" + port );

            if ( op.equals( "getProlinks" ) ) {
                GetProlinks( port, "refseq", ac, detail );
            }

        } catch ( Exception e ) {
            e.printStackTrace();
            System.out.println( "\n\n\n" );
        }
    }

    /* GetProlinks */

    public static void GetProlinks( ProlinksProxyPort port, String ns,
            String ac, String detail ) {
        try {

            Holder<DatasetType> resDataset = new Holder<DatasetType>();
            Holder<String> resNative = new Holder<String>();
            Holder<XMLGregorianCalendar> timestamp = new Holder<XMLGregorianCalendar>();

            port.getProlinks( ns, ac, "", "", "", "", 0, timestamp, resDataset,
                    resNative );

            DatasetType dataset = resDataset.value;

            JAXBContext jc = DxfJAXBContext.getDxfContext();

            Marshaller marshaller = jc.createMarshaller();
            marshaller.setProperty( Marshaller.JAXB_FORMATTED_OUTPUT,
                    new Boolean( true ) );

            java.io.StringWriter sw = new StringWriter();
            marshaller.setProperty( Marshaller.JAXB_ENCODING, "UTF-8" );

            marshaller.marshal( dataset, sw );

            String resultStr = sw.toString();

            System.out.println( "Result:" );
            System.out.println( resultStr );

        } catch ( Exception e ) {
            e.printStackTrace();
        }
    }
}
