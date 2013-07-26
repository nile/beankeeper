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

import hu.netmind.beankeeper.logging.AggregatorLogger;
import org.apache.log4j.Logger;
import java.util.*;

/**
 * This performance logger logs events in given intervals, and calculates
 * different values for viewing.
 * @author Brautigam Robert
 * @version CVS Revision: $Revision$
 */
public class AggregatorLoggerImpl implements AggregatorLogger
{
   private static Logger logger = Logger.getLogger(AggregatorLogger.class);
   private static int PERFORMANCE_INTERVAL = 5000;

   private long lastOutputTime = 0;
   private Map eventEntries;

   public void init(Map parameters)
   {
      eventEntries = Collections.synchronizedMap(new HashMap());
      try
      {
         ResourceBundle config = ResourceBundle.getBundle("beankeeper");
         PERFORMANCE_INTERVAL = Integer.valueOf(config.getString("beankeeper.performance.interval")).intValue();
      } catch ( Exception e ) {
         logger.error("could not read configuration file, using hardcoded defaults.",e);
      }
   }

   public void release()
   {
   }
   
   /**
    * Log an event.
    * @param message The message of given type.
    * @param values The values for given message.
    */
   public void log(String message, int[] values)
   {
      // If not enabled, do nothing
      if ( ! logger.isDebugEnabled() )
         return;
      // Adjust count
      AggregateEntry entry = (AggregateEntry) eventEntries.get(message);
      if ( entry == null )
      {
         entry = new AggregateEntry();
         eventEntries.put(message,entry);
      }
      // Modify entry
      entry.count++;
      if ( entry.valuesSum == null )
      {
         entry.valuesSum=values;
         entry.valuesMin=new int[values.length];
         System.arraycopy(values,0,entry.valuesMin,0,values.length);
         entry.valuesMax=new int[values.length];
         System.arraycopy(values,0,entry.valuesMax,0,values.length);
      } else {
         for ( int i=0; i<values.length; i++ )
         {
            entry.valuesSum[i]+=values[i];
            if ( values[i] > entry.valuesMax[i] )
               entry.valuesMax[i] = values[i];
            if ( values[i] < entry.valuesMin[i] )
               entry.valuesMin[i] = values[i];
         }
      }
      // Decide what to do
      long currentTime = System.currentTimeMillis();
      if ( currentTime > PERFORMANCE_INTERVAL + entry.lastOutputTime )
      {
         // Time's up, output now
         StringBuffer logline = new StringBuffer(message+": "+entry.count+" times, values: ");
         if ( entry.valuesSum == null )
         {
            logline.append("none");
         } else {
            for ( int i=0; i<entry.valuesSum.length; i++ )
               logline.append(""+(entry.valuesSum[i]/entry.count)+"-"+
                     entry.valuesMin[i]+"/"+entry.valuesMax[i]+" ");
         }
         logger.debug(logline);
         // Clear entry
         entry.lastOutputTime = currentTime;
         entry.count=0;
         entry.valuesSum=null;
         entry.valuesMin=null;
         entry.valuesMax=null;
      }
   }

   private static class AggregateEntry
   {
      public long lastOutputTime;
      public int count;
      public int[] valuesSum;
      public int[] valuesMin;
      public int[] valuesMax;

      public AggregateEntry()
      {
         lastOutputTime = 0;
         count = 0;
         valuesSum = null;
         valuesMin = null;
         valuesMax = null;
      }
   }
}


