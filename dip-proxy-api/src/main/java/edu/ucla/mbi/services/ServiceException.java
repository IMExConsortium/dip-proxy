package edu.ucla.mbi.services;

/*===========================================================================
 * $HeadURL::                                                               $
 * $Id::                                                                    $
 * Version: $Rev::                                                          $
 *===========================================================================
 *
 * FaultDef:
 *
 *    fault definitions
 *
 *========================================================================= */

import java.lang.Exception;

public class ServiceException extends Exception {

    private ServiceFault fault;

    public ServiceException( ServiceFault fault ) {
	this.fault = fault;
    }

    public ServiceFault getServiceFault() {
	return fault;
    }

}
