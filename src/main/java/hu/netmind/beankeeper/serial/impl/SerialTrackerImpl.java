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

package hu.netmind.beankeeper.serial.impl;

import hu.netmind.beankeeper.serial.SerialTracker;
import hu.netmind.beankeeper.serial.Serial;
import hu.netmind.beankeeper.common.StoreException;
import hu.netmind.beankeeper.node.NodeManager;
import java.util.Date;
import java.util.Map;

/**
 * This implementation offers serials based on dates.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public class SerialTrackerImpl implements SerialTracker
{
   private long offset = 0;
   private long lastSerial = 0;
   private int subSerial = 0;

   private NodeManager nodeManager = null; // Inject

   public void init(Map parameters)
   {
   }

   public void release()
   {
   }

   /**
    * Set the offset for serial numbers. To determine the offset,
    * a valid serial must be given to this method, and the difference
    * between it and the current serial will be added to the current offset,
    * if it's positive.
    */
   // TODO: adjust offset across nodes, or require time synchronization
   private void adjustOffset(Long serial)
   {
      long serialCooked = 10000*(serial.longValue() / 10000 + 1) ; // Chop off sub-serial
      Long currentSerial = getNextSerial();
      long currentCooked = 10000*(currentSerial.longValue() / 10000 + 1); // Chop of sub-serial
      if ( serialCooked > currentCooked )
      {
         // Current serial should be greated or equal to the supplied
         // serial. If it's not, then it has to be corrected
         offset += (serialCooked-currentCooked);
      }
   }

   /**
    * Get the next serial for database functions.
    */
   public Long getNextSerial()
   {
      // Execute on server
      if ( nodeManager.getRole() == NodeManager.NodeRole.CLIENT )
      {
         return (Long) nodeManager.callServer(SerialTracker.class.getName(),
               "getNextSerial",new Class[] {}, new Object[] {});
      }
      // Server side
      synchronized ( this )
      {
         // Get the serial from the current date. This is supposed to
         // be millisecond precision.
         long result = Serial.getSerial(new Date()).getValue();
         result+=offset;
         // Implement sub-millisecond precision here
         if ( lastSerial == result )
         {
            // This means we were executed in the same millisecond
            // as last time. Add sub-serial number, and increase if possible.
            if ( subSerial >= 9999 )
               throw new StoreException("Serial number exhausted. "+
                     "More than 10.000 operations in the same millisecond. Wow. "+
                     "Is this the distant future? Do you sit in front of a quantum computer? ...Hm... Does it run Linux?");
            subSerial++;
         } else {
            // If lastSerial is greater, then something is very wrong
            if ( lastSerial > result )
               throw new StoreException("Next serial is in the past, something is wrong.");
            // This is more likely. We are at least one milisecond further
            // in time, so zero the subSerial number.
            subSerial=0;
         }
         // Return
         lastSerial=result;
         return new Long(result+subSerial);
      }
   }

}


