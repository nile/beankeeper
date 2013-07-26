/**
 * Copyright (C) 2006 NetMind Consulting Bt.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package hu.netmind.beankeeper.node.impl;

import hu.netmind.beankeeper.common.StoreException;

/**
 * A response to a call request. When this response contains an exception (other than null),
 * the method call had thrown this exception, and the result value should be considered
 * invalid.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public class CallResponse extends CommResponse
{
   private Object value;
   private StoreException exception;

   public CallResponse(CommObject source, Object value, StoreException exception)
   {
      super(source, exception==null?CommResponse.ACTION_SUCCESS:CommResponse.UNEXPECTED_ERROR);
      this.value=value;
      this.exception=exception;
   }

   public Object getReturnValue()
   {
      return value;
   }

   public StoreException getException()
   {
      return exception;
   }

   public String toString()
   {
      return "[Call response: "+value+", exception: "+exception+" ("+getSessionId()+")]";
   }
}


