/*
* Conditions Of Use
*
* This software was developed by employees of the National Institute of
* Standards and Technology (NIST), an agency of the Federal Government.
* Pursuant to title 15 Untied States Code Section 105, works of NIST
* employees are not subject to copyright protection in the United States
* and are considered to be in the public domain.  As a result, a formal
* license is not needed to use the software.
*
* This software is provided by NIST as a service and is expressly
* provided "AS IS."  NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED
* OR STATUTORY, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF
* MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT
* AND DATA ACCURACY.  NIST does not warrant or make any representations
* regarding the use of the software or the results thereof, including but
* not limited to the correctness, accuracy, reliability or usefulness of
* the software.
*
* Permission to use this software is contingent upon your acceptance
* of the terms of this agreement
*
* .
*
*/
/*******************************************************************************
* Product of NIST/ITL Advanced Networking Technologies Division (ANTD).        *
*******************************************************************************/
package gov.nist.javax.sip.header;
import javax.sip.header.*;

/**
* In-Reply-To SIP header. Keeps a list of InReplyToHeader
*
* @version 1.2 $Revision: 1.6 $ $Date: 2009/07/17 18:57:31 $
* @since 1.1
*
*@author M. Ranganathan   <br/>
*
*
*
*/
public final class InReplyToList extends SIPHeaderList<InReplyTo>  {


    private static final long serialVersionUID = -7993498496830999237L;

    public Object clone() {
        InReplyToList retval = new InReplyToList();
        retval.clonehlist(this.hlist);
        return retval;
    }

    /** Default constructor
     */
    public InReplyToList() {
        super(InReplyTo.class, InReplyToHeader.NAME);
    }

}
