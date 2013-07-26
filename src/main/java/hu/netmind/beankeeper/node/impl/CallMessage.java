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

/**
 * Send a call request.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public class CallMessage extends CommObject
{
   private String service;
   private String method;
   private Class[] parameterTypes;
   private Object[] parameters;
   private boolean broadcast;

   public CallMessage(String service, String method, Class[] parameterTypes,
         Object[] parameters, boolean broadcast)
   {
      super();
      this.service=service;
      this.method=method;
      this.parameterTypes=parameterTypes;
      this.parameters=parameters;
      this.broadcast=broadcast;
   }

   public boolean isBroadcast()
   {
      return broadcast;
   }

   public String getService()
   {
      return service;
   }

   public String getMethod()
   {
      return method;
   }

   public Class[] getParameterTypes()
   {
      return parameterTypes;
   }

   public Object[] getParameters()
   {
      return parameters;
   }

   public String toString()
   {
      return "[Call: "+service+"."+method+" ("+getSessionId()+")]";
   }
}


