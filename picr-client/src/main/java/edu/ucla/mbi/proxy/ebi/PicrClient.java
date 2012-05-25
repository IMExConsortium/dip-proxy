package edu.ucla.mbi.proxy.ebi;

/*==============================================================================
 * $HeadURL$
 * $Id$
 * Version: $Rev$
 *==============================================================================
 *
 * PicrClient:
 *    services provided by EBI PICR web services
 *
 *=========================================================================== */

import java.io.*;
import java.net.URL;

import java.util.*;

import javax.xml.bind.*;
import javax.xml.namespace.QName;

import uk.ac.ebi.picr.*;
import uk.ac.ebi.picr.accessionmappingservice.*;
import uk.ac.ebi.picr.model.*;

public class PicrClient{

    static final JAXBContext acrContext = initAcrContext();

    public static void main(java.lang.String[] args){

        List<String> searchDB = new ArrayList<String>();
        searchDB.add("REFSEQ");
        searchDB.add("SWISSPROT");
        searchDB.add("PDB");

        String picrEndpoint = "http://www.ebi.ac.uk/Tools/picr/service";
        final String nsPicr = "http://www.ebi.ac.uk/picr/AccessionMappingService";
        final String nmPicr = "AccessionMapperService";

        if( args.length != 2 ){
            System.out.println("Please give two arguments: the first is input file and the second one is output file. ");
            System.exit(0);
        }

        File inputFile = new File(args[0]);
        File outputFile = new File(args[1]);

        if(!inputFile.exists() || !inputFile.canRead() ){
            System.out.println("warning: the input file " + args[0] + " does not exist or can't read.");
            return;
        }

        System.out.print("PicrClient will search the following database: ");

        for(int i = 0; i < searchDB.size(); i++){
            if( i == 0 ){
                System.out.print(searchDB.get(i));
            }else{
                System.out.print(", " + searchDB.get(i));
            }
        }

        System.out.print(".\n");

        uk.ac.ebi.picr.ObjectFactory of = new uk.ac.ebi.picr.ObjectFactory();
        GetUPIForAccessionResponse response = of.createGetUPIForAccessionResponse();

        // call EBI PICR utility
        AccessionMapperInterface port = null;

        try{

            AccessionMapperService amSrv =
                                new AccessionMapperService( new URL( picrEndpoint + "?wsdl" ),
                                                            new QName( nsPicr, nmPicr )
                                                            );

            port = amSrv.getAccessionMapperPort();
        } catch(Exception ex){
            System.out.println( "PicrClient: picr endpoint not set.");
        }

        System.out.println( "PicrClient starts to read input file " + args[0] + "." );

        try{
            FileReader fr = new FileReader( inputFile);
            BufferedReader in = new BufferedReader (fr);
            String acLine;

            while((acLine = in.readLine()) != null ){

                if( !acLine.trim().equals("") ){

                    List<UPEntry> entries = port.getUPIForAccession( acLine.trim(), "", searchDB, "", true );

                    if ( entries != null && entries.size() > 0 ) {

                        //System.out.println( "PicrClient: got entries: #" + entries.size() );                    
                        response.getGetUPIForAccessionReturn().addAll( entries );
                    }
                }

            }

            fr.close();

        }catch(Exception e){
            System.out.println( "exception: " + e.toString() );
        }

        System.out.println( "PicrClient is waiting for the response..." );

        try{

            FileOutputStream fos = new FileOutputStream(outputFile);

            // Marshal object to XML string
            JAXBContext jc = getAcrContext();
            Marshaller marshaller = jc.createMarshaller();


            //java.io.StringWriter sw = new StringWriter();
            marshaller.setProperty( Marshaller.JAXB_ENCODING, "UTF-8" );
            marshaller.setProperty( Marshaller.JAXB_FORMATTED_OUTPUT, new Boolean( true ) );

            System.out.println( "PicrClient has got marshaller" );

            marshaller.marshal( response, fos );

            System.out.println( "PicrClient has marshalled..." );

            fos.close();

        }catch(Exception e ){
            System.out.println("Exception in Marshaller is " + e.toString());
        }

        System.out.println("PicrClient has finished to write down in file " + args[1] + ".");

    }

    private static JAXBContext initAcrContext() {

        try {
            JAXBContext jbx =
                    JAXBContext.newInstance( "uk.ac.ebi.picr", PicrClient.class.getClassLoader() );
            return jbx;
        } catch ( JAXBException jbe ) {
            System.out.println( "JAXBContext.initAcrContext(): " + jbe.toString() );
        }
        return null;
    }

    public static JAXBContext getAcrContext() {
        return acrContext;
    }

}

