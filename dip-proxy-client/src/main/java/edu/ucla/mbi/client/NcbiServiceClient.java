package edu.ucla.mbi.client;

/*===========================================================================
 * $HeadURL: https://wyu@imex.mbi.ucla.edu/svn/dip-ws/trunk/dip-proxy/src/#$
 * $Id: NcbiServiceClient.java 2562 2012-07-17 21:05:14Z wyu $
 * Version: $Rev: 2562 $
 *===========================================================================
 *
 * NcbiServiceClient:
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
import javax.xml.ws.soap.SOAPFaultException;
import javax.xml.soap.*;
import java.util.*;

public class NcbiServiceClient {

    public static void main( java.lang.String[] args ) {

        String sr = args[0]; // service URL
        String op = args[1]; // operation

        int timeout = 300 * 1000; // client TimeOut 5 mins
        String ac = "";
        String detail = "";
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
            
            NcbiProxyService service = new NcbiProxyService();
            NcbiProxyPort port = service.getProxyPort();
            

            // set server location
            // ---------------------

            ((BindingProvider) port).getRequestContext().put(
                    BindingProvider.ENDPOINT_ADDRESS_PROPERTY, sr );

            // set client Timeout
            // ------------------

            ((BindingProvider) port).getRequestContext().put(
                    JAXWSProperties.CONNECT_TIMEOUT, timeout );

            System.out.println( "Proxy: NcbiProxyService=" + service + " port="
                    + port );

            if ( op.equals( "getJournal" ) ) {
                GetJournal( port, ac, detail );
            }

            if ( op.equals( "getPubmedArticle" ) ) {
                GetPubmedArticle( port, ac, detail );
            }

            if ( op.equals( "getRefseq" ) ) {
                GetRefseq( port, ac, detail );
            }

            if ( op.equals( "getGene" ) ) {
                GetGene( port, ac, detail );
            }

            if ( op.equals( "getTaxon" ) ) {
                GetTaxon( port, ac, detail );
            }

        } catch ( Exception e ) {
            System.out.println( e.toString() );
        }
    }

    /* GetJournal */

    public static void GetJournal( NcbiProxyPort port, String nlmid,
            String detail ) {

        try {
            Holder<DatasetType> resDataset = new Holder<DatasetType>();
            Holder<String> resNative = new Holder<String>();
            Holder<XMLGregorianCalendar> timestamp = new Holder<XMLGregorianCalendar>();

            port.getJournal( "nlm", nlmid, "", detail, "", "", 0,
                    timestamp, resDataset, resNative );

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
            System.out.println("getJournal Exception fault class is :" + e.getClass().getName());
            e.printStackTrace();
        }
    }

    /* GetArticle */

    public static void GetPubmedArticle( NcbiProxyPort port, String pmid,
            String detail ) {

        try {
            Holder<DatasetType> resDataset = new Holder<DatasetType>();
            Holder<String> resNative = new Holder<String>();
            Holder<XMLGregorianCalendar> timestamp = new Holder<XMLGregorianCalendar>();

            port.getPubmedArticle( "pubmed", pmid, "", detail, "", "", 0,
                    timestamp, resDataset, resNative );

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
        }catch ( Exception e ) {
            e.printStackTrace();
        }
    }

    /* GetRefseq */

    public static void GetRefseq( NcbiProxyPort port, String ac, String detail ) {
        try {

            Holder<DatasetType> resDataset = new Holder<DatasetType>();
            Holder<String> resNative = new Holder<String>();
            Holder<XMLGregorianCalendar> timestamp = new Holder<XMLGregorianCalendar>();

            port.getRefseq( "refseq", ac, "", detail, "", "", 0, timestamp,
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
            e.printStackTrace();
        }
    }

    /* Get Entrez Gene */

    public static void GetGene( NcbiProxyPort port, String ac, String detail ) {
        try {
            Holder<DatasetType> resDataset = new Holder<DatasetType>();
            Holder<String> resNative = new Holder<String>();
            Holder<XMLGregorianCalendar> timestamp = new Holder<XMLGregorianCalendar>();

            port.getGene( "entrezgene", ac, "", detail, "", "", 0, timestamp,
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
            e.printStackTrace();
        }
    }

    public static void GetTaxon( NcbiProxyPort port, String ac, String detail ) {
        try {
            Holder<DatasetType> resDataset = new Holder<DatasetType>();
            Holder<String> resNative = new Holder<String>();
            Holder<XMLGregorianCalendar> timestamp = new Holder<XMLGregorianCalendar>();

            port.getTaxon( "ncbitaxid", ac, "", detail, "", "", 0, timestamp,
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
            e.printStackTrace();
        }
    }
}
