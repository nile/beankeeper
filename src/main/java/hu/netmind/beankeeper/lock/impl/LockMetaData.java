/**
 * Copyright (C) 2008 NetMind Consulting Bt.
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

package hu.netmind.beankeeper.lock.impl;

import java.io.Serializable;
import java.util.Date;
import hu.netmind.beankeeper.object.PersistenceMetaData;
import hu.netmind.beankeeper.lock.*;

/**
 * Meta-data for locking objects.
 * @author Brautigam Robert
 * @version CVS Revision: $Revision$
 */
public class LockMetaData implements Comparable, Serializable, PersistenceMetaData
{
   private Long objectId;
   private Class objectClass;
   private Long lastCurrentSerial;
   private Long startSerial;
   private Long endSerial;

   public String toString()
   {
      if ( objectId == null )
         return "[Lock class "+objectClass.getName()+" ("+lastCurrentSerial+")"+"]";
      else
         return "[Lock object "+objectClass.getName()+":"+objectId+" ("+lastCurrentSerial+")]";
   }

   public int compareTo(Object obj)
   {
      LockMetaData o = (LockMetaData) obj;
      if ( (o.objectId==null) && (objectId==null) )
      {
         // Two classes, compare with names
         return objectClass.getName().compareTo(o.objectClass.getName());
      } else if ( (o.objectId!=null) && (objectId==null) ) {
         // This is a class, other is not, so this is first
         return -1;
      } else if ( (o.objectId==null) && (objectId!=null) ) {
         // Other way around
         return 1;
      }
      // Default, both are objects
      return objectId.compareTo(o.objectId);
   }

   public Long getPersistenceId()
   {
      return objectId;
   }
   public void setPersistenceId(Long objectId)
   {
      this.objectId=objectId;
   }

   public Class getObjectClass()
   {
      return objectClass;
   }
   public void setObjectClass(Class objectClass)
   {
      this.objectClass=objectClass;
   }

   public Long getPersistenceStart()
   {
      return startSerial;
   }
   public void setPersistenceStart(Long startSerial)
   {
      this.startSerial=startSerial;
   }

   public Long getPersistenceEnd()
   {
      return endSerial;
   }
   public void setPersistenceEnd(Long endSerial)
   {
      this.endSerial=endSerial;
   }

   public Long getLastCurrentSerial()
   {
      return lastCurrentSerial;
   }
   public void setLastCurrentSerial(Long lastCurrentSerial)
   {
      this.lastCurrentSerial=lastCurrentSerial;
   }

   public Date getCreationDate()
   {
      return null;
   }
   public Date getRemoveDate()
   {
      return null;
   }
   public Long getRegistrationSerial()
   {
      return null;
   }
}


