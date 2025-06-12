/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * $Id: Or.java 468655 2006-10-28 07:12:06Z minchau $
 */
package org.apache.xpath.operations;

import org.apache.xpath.XPathContext;
import org.apache.xpath.objects.XBoolean;
import org.apache.xpath.objects.XObject;

/**
 * The 'or' operation expression executer.
 */
public class Or extends Operation
{
    static final long serialVersionUID = -644107191353853079L;

  /**
   * OR two expressions and return the boolean result. Override
   * superclass method for optimization purposes.
   *
   * @param xctxt The runtime execution context.
   *
   * @return {@link org.apache.xpath.objects.XBoolean#S_TRUE} or 
   * {@link org.apache.xpath.objects.XBoolean#S_FALSE}.
   *
   * @throws javax.xml.transform.TransformerException
   */
  public XObject execute(XPathContext xctxt) throws javax.xml.transform.TransformerException
  {

    XObject expr1 = m_left.execute(xctxt);

    if (!expr1.bool())
    {
      XObject expr2 = m_right.execute(xctxt);

      return expr2.bool() ? XBoolean.S_TRUE : XBoolean.S_FALSE;
    }
    else
      return XBoolean.S_TRUE;
  }
  
  /**
   * Evaluate this operation directly to a boolean.
   *
   * @param xctxt The runtime execution context.
   *
   * @return The result of the operation as a boolean.
   *
   * @throws javax.xml.transform.TransformerException
   */
  public boolean bool(XPathContext xctxt)
          throws javax.xml.transform.TransformerException
  {
    return (m_left.bool(xctxt) || m_right.bool(xctxt));
  }

}
