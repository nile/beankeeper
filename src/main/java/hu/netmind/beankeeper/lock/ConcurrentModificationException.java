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

package hu.netmind.beankeeper.lock;

import hu.netmind.beankeeper.common.StoreException;

/**
 * Exception is thrown, if an object is about to be saved in a transaction,
 * but the same object (or another representation of the same data) is under
 * modification in another thread. This exception is also thrown if one
 * of the objects is already locked.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public class ConcurrentModificationException extends StoreException
{
   private SessionInfo sessionInfo;
   private Object[] objs;
   
   public ConcurrentModificationException(SessionInfo sessionInfo, Object[] objs, String message)
   {
      super(message);
      this.sessionInfo=sessionInfo;
      this.objs=objs;
   }

   public ConcurrentModificationException(SessionInfo sessionInfo, Object[] objs, String message, Throwable cause)
   {
      super(message,cause);
      this.sessionInfo=sessionInfo;
      this.objs=objs;
   }

   /**
    * Returns the object which could not be modified.
    */
   public Object[] getObjects()
   {
      return objs;
   }

   /**
    * Returns the transaction which currently locks the object that could
    * not be modified.
    */
   public SessionInfo getSessionInfo()
   {
      return sessionInfo;
   }
}

