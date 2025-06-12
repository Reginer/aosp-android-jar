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
*/
/*******************************************************************************
* Product of NIST/ITL Advanced Networking Technologies Division (ANTD).        *
*******************************************************************************/
package gov.nist.javax.sip.header;

import java.text.ParseException;

/**
 * Allow SIPHeader.
 *
 * @author M. Ranganathan   <br/>
 * @version 1.2 $Revision: 1.6 $ $Date: 2009/07/17 18:57:26 $
 * @since 1.1
 *
 *
 */
public final class Allow extends
    SIPHeader implements javax.sip.header.AllowHeader {

    /**
     * Comment for <code>serialVersionUID</code>
     */
    private static final long serialVersionUID = -3105079479020693930L;
    /** method field
     */
    protected String method;

    /** default constructor
     */
    public Allow() {
        super(ALLOW);
    }

    /** constructor
     * @param m String to set
     */
    public Allow(String m) {
        super(ALLOW);
        method = m;
    }

    /** get the method field
     * @return String
     */
    public String getMethod() {
        return method;
    }

    /**
     * Set the method member
     * @param method method to set.
     */
    public void setMethod(String method) throws ParseException {
        if (method == null)
            throw new NullPointerException(
                "JAIN-SIP Exception"
                    + ", Allow, setMethod(), the method parameter is null.");
        this.method = method;
    }

    /** Return body encoded in canonical form.
     * @return body encoded as a string.
     */
    protected String encodeBody() {
        return method;
    }
}
