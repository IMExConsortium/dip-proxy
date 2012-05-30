package edu.ucla.mbi.client;

/*==============================================================================
 * $HeadURL::                                                                  $           
 * $Id::                                                                       $
 * Version: $Rev::                                                             $
 *==============================================================================
 *
 * DipTrimURL:  
 *
 *============================================================================*/

public class DipTrimURL {

    public static String trim ( String serviceURL ) {

        serviceURL = serviceURL.replaceAll( "^\\s*", "" );
        serviceURL = serviceURL.replaceAll( "\\s*$", "" );

        return serviceURL;

    }

}
