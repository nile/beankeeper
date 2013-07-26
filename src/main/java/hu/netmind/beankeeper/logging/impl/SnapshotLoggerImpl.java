/**
 * Copyright (C) 2007 NetMind Consulting Bt.
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

package hu.netmind.beankeeper.logging.impl;

import hu.netmind.beankeeper.logging.SnapshotLogger;
import org.apache.log4j.Logger;
import java.util.*;

/**
 * This implementation uses configuration to get the output interval of
 * categories, and uses apache logger for output.
 * @author Brautigam Robert
 * @version CVS Revision: $Revision$
 */
public class SnapshotLoggerImpl implements SnapshotLogger 
{
   private static Logger logger = Logger.getLogger(SnapshotLogger.class);
   private static int PROFILE_INTERVAL = 5000;

   private long lastOutputTime = 0;
   private Map eventEntries;

   public void init(Map parameters)
   {
      eventEntries = Collections.synchronizedMap(new HashMap());
      try
      {
         ResourceBundle config = ResourceBundle.getBundle("beankeeper");
         PROFILE_INTERVAL = Integer.valueOf(config.getString("beankeeper.profile.interval")).intValue();
      } catch ( Exception e ) {
         logger.error("could not read configuration file, using hardcoded defaults.",e);
      }
   }

   public void release()
   {
   }
   
   /**
    * Log an event.
    * @param category The category identifier of the event.
    * @param message The message of given type.
    */
   public void log(String category,String message)
   {
      // If not enabled, do nothing
      if ( ! logger.isDebugEnabled() )
         return;
      // Adjust count
      ProfileEntry entry = (ProfileEntry) eventEntries.get(category);
      if ( entry == null )
      {
         entry = new ProfileEntry();
         eventEntries.put(category,entry);
      }
      entry.count++;
      // Decide what to do
      long currentTime = System.currentTimeMillis();
      if ( currentTime > PROFILE_INTERVAL + entry.lastOutputTime )
      {
         // Time's up, output now
         logger.debug("["+category+":"+entry.count+"] "+message);
         entry.lastOutputTime = currentTime;
         // Clear entry
         entry.count=0;
      }
   }

   private static class ProfileEntry
   {
      public long lastOutputTime;
      public int count;

      public ProfileEntry()
      {
         lastOutputTime = 0;
         count = 0;
      }
   }
}


