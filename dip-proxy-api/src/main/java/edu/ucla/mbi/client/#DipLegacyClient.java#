package edu.ucla.mbi.client;

/*==============================================================================
 * $HeadURL::                                                                  $
 * $Id::                                                                       $
 * Version: $Rev::                                                             $
 *==============================================================================
 *
 * DipLegacyClient:
 *
 *=========================================================================== */

import java.util.*;
import java.io.*;

import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;

import javax.xml.bind.*;

import edu.ucla.mbi.legacy.dip.*;

import edu.ucla.mbi.services.legacy.dip.*;
import edu.ucla.mbi.dxf14.*;


import org.apache.commons.cli.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class DipLegacyClient {
    
    private DipLegacyClient( String endpoint, String user, String pass,
                             String op, String mode ){
        
        DipLegacyService service = null;
        DipLegacyPort port = null;
        
        if( endpoint == null || endpoint.equals("") ){
                endpoint="http://127.0.0.1:8080/dip-legacy/dxf";
        }
        
        try {
            URL url = new URL( endpoint + "?wsdl" );
            //System.out.println( "WSDL: " + endpoint + "?wsdl" );
            QName qn = new QName("http://mbi.ucla.edu/services/legacy/dip",
                                 "DipLegacyService");
            service = new DipLegacyService( url, qn );
            port = service.getLegacyPort();

            ( (BindingProvider) port ).getRequestContext()
                .put( BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                      endpoint );

            if( user != null && !user.equals("") 
                && pass != null && !pass.equals("") 
                ){
                ( (BindingProvider) port ).getRequestContext()
                    .put( BindingProvider.USERNAME_PROPERTY, user );
                ( (BindingProvider) port ).getRequestContext()
                    .put( BindingProvider.PASSWORD_PROPERTY, pass );
            }
        } catch ( Exception ex ) {
            ex.printStackTrace();
            System.out.println( "IcentralService: cannot connect" );
        }

        if( op == null ){ return; }

        if( mode == null || mode.equals("") ){
            mode = "base";
        }
        
        System.out.println( "operation=" + op + " detail=" + mode );
        List<NodeType> ndl = null;
        
        //----------------------------------------------------------------------
        
        if( op.equals( "getNodeBounds" ) ){
            System.out.println( "getNodeBounds" );
            ndl = port.getNodeBounds( mode, "dxf");
        }

        if( op.equals( "getLinkBounds" ) ){
            System.out.println( "getNodeBounds" );
            ndl = port.getLinkBounds( mode, "dxf");
        }
        
        if( op.equals( "getSourceBounds" ) ){
            System.out.println( "getNodeBounds" );
            ndl = port.getSourceBounds( mode, "dxf");
        }

        //----------------------------------------------------------------------

        if( ndl != null ){
            try{
                edu.ucla.mbi.dxf14.ObjectFactory
                    dxf14of = new edu.ucla.mbi.dxf14.ObjectFactory();
                
                DatasetType dt = dxf14of.createDatasetType();
                
                for( Iterator<NodeType> ndi = ndl.iterator(); ndi.hasNext(); ){
                    NodeType cnd = ndi.next();
                    dt.getNode().add(cnd);
                }
                
                JAXBContext jc
                    = JAXBContext.newInstance( "edu.ucla.mbi.dxf14" );
                Marshaller m = jc.createMarshaller();
                System.out.println( "Result:");
                m.marshal(  dxf14of.createDataset( dt ), System.out );
                
            } catch(JAXBException jbx){
                jbx.printStackTrace();
            }
        }
    }
    
    private DipLegacyClient( String endpoint, String user, String pass,
                             String op, String file, String mode ){
        
        DipLegacyService service = null;
        DipLegacyPort port = null;
        
        if( endpoint == null || endpoint.equals("") ){
            endpoint="http://127.0.0.1:8080/dip-legacy/dxf";
        }
        
        try {
            URL url = new URL( endpoint + "?wsdl" );
            System.out.println( "user: " + user + " pass:" + pass);
            QName qn = new QName("http://mbi.ucla.edu/services/legacy/dip",
                                 "DipLegacyService");
            service = new DipLegacyService( url, qn );
            port = service.getLegacyPort();

            ( (BindingProvider) port ).getRequestContext()
                .put( BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                      endpoint );

            if( user != null && !user.equals("") 
                && pass != null && !pass.equals("") 
                ){
                ( (BindingProvider) port ).getRequestContext()
                    .put( BindingProvider.USERNAME_PROPERTY, user );
                ( (BindingProvider) port ).getRequestContext()
                    .put( BindingProvider.PASSWORD_PROPERTY, pass );
            }
        } catch ( Exception ex ) {
            ex.printStackTrace();
            System.out.println( "IcentralService: cannot connect" );
        }

        if( op == null || file == null ){ return; }

        if( mode == null || mode.equals("") ){
            mode = "create";
        }
        
        System.out.println( "operation=" + op + " mode=" + mode );
        System.out.println( "file=" + file );
        
        //----------------------------------------------------------------------

        if( op.equals( "setNode" ) || op.equals( "setLink" )
            || op.equals( "setEvidence" ) || op.equals( "setSource" ) 
            || op.equals( "matchNode" )
            ){
            
            DatasetType dataset = null;

            try{
                JAXBContext jc 
                    = JAXBContext.newInstance( "edu.ucla.mbi.dxf14" );
                Unmarshaller u = jc.createUnmarshaller();
                Object o = u.unmarshal( new File( file ) );
                
                dataset = (DatasetType) ((JAXBElement)o).getValue() ;
                
            } catch( JAXBException jbx ){
                jbx.printStackTrace();
                return;
            }
           
            List<NodeType> ndl = null;

            if( op.equals( "setNode" ) ){
                System.out.println( "dataset.node=" + dataset.getNode() );
                ndl = port.setNode( dataset, mode );
            }

            if( op.equals( "setLink" ) ){
                System.out.println( "dataset.node=" + dataset.getNode() );
                ndl = port.setLink( dataset, mode );
            }
            
            if( op.equals( "setEvidence" ) ){
                System.out.println( "dataset.node=" + dataset.getNode() );
                ndl = port.setEvidence( dataset, mode );
            }

            if( op.equals( "setSource" ) ){
                System.out.println( "dataset.node=" + dataset.getNode() );
                ndl = port.setSource( dataset, mode );
            }
            
            if( op.equals( "matchNode" ) ){
                System.out.println( "dataset.node=" + dataset.getNode() );
                ndl = port.matchNode( dataset, "match", mode, "dxf" );
            }     
        
            if( ndl != null ){
                try{
                    edu.ucla.mbi.dxf14.ObjectFactory
                        dxf14of = new edu.ucla.mbi.dxf14.ObjectFactory();
                    
                    DatasetType dt = dxf14of.createDatasetType();
                    
                    for( Iterator<NodeType> ndi = ndl.iterator(); ndi.hasNext(); ){
                        
                        NodeType cnd = ndi.next();
                        dt.getNode().add(cnd);
                    }
                    
                    JAXBContext jc
                        = JAXBContext.newInstance( "edu.ucla.mbi.dxf14" );
                    Marshaller m = jc.createMarshaller();
                    System.out.println( "Result:");
                    m.marshal(  dxf14of.createDataset( dt ), System.out );
                    
                } catch(JAXBException jbx){
                    jbx.printStackTrace();
                }
            }
        }
    }
    
    private DipLegacyClient( String endpoint, String user, String pass, 
                             String op, String ns, String ac, String detail ){
        
        DipLegacyService service = null;
        DipLegacyPort port = null;
        
        if( endpoint == null || endpoint.equals("") ){
            endpoint="http://127.0.0.1:8080/dip-legacy/dxf";
        }
        
        try {
            URL url = new URL( endpoint + "?wsdl" );
            //System.out.println( "WSDL: " + endpoint + "?wsdl" );
            QName qn = new QName("http://mbi.ucla.edu/services/legacy/dip",
                                 "DipLegacyService");
            service = new DipLegacyService( url, qn );
            port = service.getLegacyPort();

            ( (BindingProvider) port ).getRequestContext()
                .put( BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                      endpoint );


            if( user != null && !user.equals("") 
                && pass != null && !pass.equals("") 
                ){
                ( (BindingProvider) port ).getRequestContext()
                    .put( BindingProvider.USERNAME_PROPERTY, user );
                ( (BindingProvider) port ).getRequestContext()
                    .put( BindingProvider.PASSWORD_PROPERTY, pass );
            }
        } catch ( Exception ex ) {
            ex.printStackTrace();
            System.out.println( "IcentralService: cannot connect" );
        }
        
        System.out.println( "operation: " + op );
        if(op == null){ return; }

        if( ns == null ){
            ns ="dip";
        }        
        if( detail == null ){
            detail ="full";
        }

        List<String> acl = new ArrayList();

        if( ac != null ){
            try{
                String[] aca = ac.split(":");
                for( int i=0; i<aca.length;i++){
                    if( aca[i] != null && !aca[i].equals("") ){
                        acl.add( aca[i] );
                    }
                }
            } catch( Exception ex){
                // should not happen
            }
        }
        
        System.out.println( "ns: " + ns + " ac: " + ac );        
        System.out.println( "detail: " + detail );

        List<NodeType> ndl = null;

        if( op.equals("getNode") && acl.size()>0 ){
            for( Iterator<String> aci = acl.iterator();aci.hasNext();){
                String cac = aci.next();
                System.out.println( " get Node: NS=" + ns + " AC=" + cac );
                
                ndl = port.getNode( ns, cac, "", "", detail, "dxf" );                
            }
        }

        if( op.equals("getSource") && acl.size()>0 ){
            for( Iterator<String> aci = acl.iterator();aci.hasNext();){
                String cac = aci.next();
                System.out.println( " get Source: NS=" + ns + " AC=" + cac );
                
                ndl = port.getSource( ns, cac, "", detail, "dxf" );                
            }
        }
        
        if( op.equals("getImexSRec") && acl.size()>0 ){
            for( Iterator<String> aci = acl.iterator();aci.hasNext();){
                String cac = aci.next();
                System.out.println( " getImexSRec: NS=" + ns + " AC=" + cac );
                
                edu.ucla.mbi.services.legacy.dip.ObjectFactory
                    of = new edu.ucla.mbi.services.legacy.dip.ObjectFactory();
                GetImexSRec gisr =  of.createGetImexSRec();

                gisr.setNs( ns );
                gisr.setAc( cac );
                gisr.setFormat("mif25");
                String imexSRec = port.getImexSRec( gisr );
                
                System.out.println( "IMEx Source Record:");
                System.out.println( imexSRec );
            }
        }

        if( op.equals("getLinksByNodeSet") && acl.size()>0 ){

            edu.ucla.mbi.dxf14.ObjectFactory
                dxf14of = new edu.ucla.mbi.dxf14.ObjectFactory();
            
            DatasetType dt = dxf14of.createDatasetType();

            
            for( Iterator<String> aci = acl.iterator();aci.hasNext();){
                String cac = aci.next();

                NodeType cnd = dxf14of.createNodeType();
                cnd.setNs(ns);
                cnd.setAc(cac);

                dt.getNode().add(cnd);
            }

            ndl = port.getLinksByNodeSet( dt, "", detail, "dxf");
        }
        
        if( ndl != null ){
            try{
                edu.ucla.mbi.dxf14.ObjectFactory
                    dxf14of = new edu.ucla.mbi.dxf14.ObjectFactory();
                
                DatasetType dt = dxf14of.createDatasetType();
                
                for( Iterator<NodeType> ndi = ndl.iterator(); ndi.hasNext(); ){
                    
                    NodeType cnd = ndi.next();
                    dt.getNode().add(cnd);
            }
                
                JAXBContext jc
                    = JAXBContext.newInstance( "edu.ucla.mbi.dxf14" );
                Marshaller m = jc.createMarshaller();
                System.out.println( "Result:");
                m.marshal(  dxf14of.createDataset( dt ), System.out );
                
            } catch(JAXBException jbx){
                jbx.printStackTrace();
            }
        }
    }
    
    //--------------------------------------------------------------------------

    public static void main (String[] args) {

        Options options = new Options();
        
        Option opOption = OptionBuilder.withArgName( "operation name" )
            .hasArg()
            .withLongOpt( "operation" )
            .withDescription( "operation to perform " )
            .create( "op" );
        
        Option acOption = OptionBuilder.withArgName( "accession1[:accesion2:...]" )
            .hasArg()
            .withLongOpt( "accession" )
            .withDescription( "accession/accession list" )
            .create( "ac" );
        
        Option nsOption = OptionBuilder.withArgName( "namespace" )
            .hasArg()
            .withLongOpt( "namespace" )
            .withDescription( "namespace (default: dip)" )
            .create( "ns" );

        Option fileOption = OptionBuilder.withArgName( "input file" )
            .hasArg()
            .withLongOpt( "file" )
            .withDescription( "file" )
            .create( "f" );

        Option urlOption = OptionBuilder.withArgName( "url" )
            .hasArg()
            .withLongOpt( "url" )
            .withDescription( "server url (default: http://127.0.0.1:8080/dip-legacy/dxf)" )
            .create( "u" );

        Option mdOption = OptionBuilder.withArgName( "mode" )
            .hasArg()
            .withLongOpt( "mode" )
            .withDescription( "record operation mode (create/update; default: create)" )
            .create( "m" );


        Option dtOption = OptionBuilder.withArgName( "detail level" )
            .hasArg()
            .withLongOpt( "detail" )
            .withDescription( "detail level (default: full)" )
            .create( "dt" );
        
        Option helpOption = OptionBuilder.withLongOpt( "help" )
            .withDescription( "help " )
            .create( "h" );

        Option lOption = OptionBuilder.withArgName( "login" )
            .hasArg()
            .withLongOpt( "login" )
            .withDescription( "login " )
            .create( "l" );

        Option pOption = OptionBuilder.withArgName( "pasword" )
            .hasArg()
            .withLongOpt( "pass" )
            .withDescription( "password " )
            .create( "p" );

        options.addOption( helpOption );
        options.addOption( opOption );
        options.addOption( nsOption );
        options.addOption( acOption );
        options.addOption( dtOption );
        options.addOption( fileOption );
        options.addOption( mdOption );
        options.addOption( urlOption );
        options.addOption( lOption );
        options.addOption( pOption );
        
        try{
            CommandLineParser parser = new PosixParser();
            CommandLine cmd = parser.parse( options, args);

            if( cmd.hasOption("u") ){
                System.out.println( "URL: " + cmd.getOptionValue("u"));
            }

            if( cmd.hasOption("h") ){
                HelpFormatter formatter = new HelpFormatter();
                formatter.setWidth(127);
                formatter.printHelp( "DipLegacyClient", options );
                return;
            }

            if(cmd.hasOption("op")) {
                if( cmd.hasOption("f") ){
                     DipLegacyClient dlc =
                         new DipLegacyClient( cmd.getOptionValue("u"), 
                                              cmd.getOptionValue("l"),
                                              cmd.getOptionValue("p"),
                                              cmd.getOptionValue("op"),
                                              cmd.getOptionValue("f"),
                                              cmd.getOptionValue("m") );
                     
                }
                if( cmd.hasOption("ac") ){
                    DipLegacyClient dlc = 
                        new DipLegacyClient( cmd.getOptionValue("u"), 
                                             cmd.getOptionValue("l"),
                                             cmd.getOptionValue("p"),
                                             cmd.getOptionValue("op"), 
                                             cmd.getOptionValue("ns"),
                                             cmd.getOptionValue("ac"),
                                             cmd.getOptionValue("dt") );                
                }

                DipLegacyClient dlc =
                    new DipLegacyClient( cmd.getOptionValue("u"),
                                         cmd.getOptionValue("l"),
                                         cmd.getOptionValue("p"),
                                         cmd.getOptionValue("op"),
                                         cmd.getOptionValue("dt") );   
            }

        } catch( Exception exp ) {
            System.out.println( "Parsing failed.  Reason: " + exp.getMessage() );
            HelpFormatter formatter = new HelpFormatter();
            formatter.setWidth(127);
            formatter.printHelp( "DipLegacyClient", options );
        }
    }

}