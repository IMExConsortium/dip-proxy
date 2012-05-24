package edu.ucla.mbi.proxy.ebi;

/*===========================================================================
 * $HeadURL: https://wyu@imex.mbi.ucla.edu/svn/dip-ws/trunk/dip-proxy/src/#$
 * $Id$
 * Version: $Rev$
 *===========================================================================
 *
 * Category:
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

public class EbiServiceClient {

    public static void main( java.lang.String[] args ) {

        String sr = args[0]; // service URL
        String op = args[1]; // operation

        long timeout = 300 * 1000; // client TimeOut 5 mins
        String ac = "";
        String detail = "";
        String ns = "";
        for ( int i = 2; i < args.length; i++ ) {
            if ( args[i].startsWith( "AC=" ) ) {
                ac = args[i].replaceFirst( "AC=", "" );
            }
            if ( args[i].startsWith( "DETAIL=" ) ) {
                detail = args[i].replaceFirst( "DETAIL=", "" );
            }
            if ( args[i].startsWith( "NS=" ) ) {
                ns = args[i].replaceFirst( "NS=", "" );
            }
        }
        if ( detail.equals( "" ) ) {
            detail = "stub";
        }

        System.out.println( "SRV: " + sr + " OP: " + op + " -> AC=" + ac
                + " -> NS=" + ns + " -> DETAIL=" + detail );
        try {

            EbiProxyService service = new EbiProxyService();
            EbiProxyPort port = service.getProxyPort();

            // set server location
            // ---------------------

            ((BindingProvider) port).getRequestContext().put(
                    BindingProvider.ENDPOINT_ADDRESS_PROPERTY, sr );

            // set client Timeout
            // ------------------

            ((BindingProvider) port).getRequestContext().put(
                    JAXWSProperties.CONNECT_TIMEOUT, timeout );

            if ( op.equals( "getUniprot" ) ) {
                GetUniprot( port, ac, detail );
            }
            if ( op.equals( "getPicrList" ) ) {
                GetPicrList( port, ac, ns, detail );
            }
        } catch ( Exception e ) {
            // e.printStackTrace();
            System.out.println( "EbiPublic " + e.toString() );
        }
    }

    /* GetUniprot */

    public static void GetUniprot( EbiProxyPort port, String ac, String detail ) {

        try {

            Holder<DatasetType> resDataset = new Holder<DatasetType>();
            Holder<String> resNative = new Holder<String>();
            Holder<XMLGregorianCalendar> timestamp = new Holder<XMLGregorianCalendar>();

            port.getUniprot( "uniprot", ac, "", detail, "", "", 0, timestamp,
                    resDataset, resNative );
            edu.ucla.mbi.dxf14.ObjectFactory dof = new edu.ucla.mbi.dxf14.ObjectFactory();

            DatasetType dataset = resDataset.value;

            JAXBContext jc = DxfJAXBContext.getDxfContext();

            Marshaller marshaller = jc.createMarshaller();
            marshaller.setProperty( Marshaller.JAXB_FORMATTED_OUTPUT,
                    new Boolean( true ) );

            java.io.StringWriter sw = new StringWriter();
            marshaller.setProperty( Marshaller.JAXB_ENCODING, "UTF-8" );

            marshaller.marshal( dof.createDataset( dataset ), sw );

            String resultStr = sw.toString();

            System.out.println( "Result:" );
            System.out.println( resultStr );

        } catch ( Exception e ) {
            // e.printStackTrace();
            System.out.println( e.toString() );
        }
    }

    /* GetPicrList */
    public static void GetPicrList( EbiProxyPort port, String ac, String ns,
            String detail ) {
        try {

            Holder<DatasetType> resDataset = new Holder<DatasetType>();
            Holder<String> resNative = new Holder<String>();
            Holder<XMLGregorianCalendar> timestamp = new Holder<XMLGregorianCalendar>();

            port.getPicrList( ns, ac, "", detail, "", "", 0, timestamp,
                    resDataset, resNative );

            edu.ucla.mbi.dxf14.ObjectFactory dof = new edu.ucla.mbi.dxf14.ObjectFactory();

            DatasetType dataset = resDataset.value;

            JAXBContext jc = DxfJAXBContext.getDxfContext();

            Marshaller marshaller = jc.createMarshaller();
            marshaller.setProperty( Marshaller.JAXB_FORMATTED_OUTPUT,
                    new Boolean( true ) );

            java.io.StringWriter sw = new StringWriter();
            marshaller.setProperty( Marshaller.JAXB_ENCODING, "UTF-8" );

            marshaller.marshal( dof.createDataset( dataset ), sw );

            String resultStr = sw.toString();

            System.out.println( "Result:" );
            System.out.println( resultStr );

        } catch ( Exception e ) {
            // e.printStackTrace();
            System.out.println( e.toString() );
        } catch ( Error e ) {
            System.out.println( e.toString() );
        }
    }
}
