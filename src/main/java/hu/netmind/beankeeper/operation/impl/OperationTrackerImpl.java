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

package hu.netmind.beankeeper.operation.impl;

import hu.netmind.beankeeper.event.EventDispatcher;
import hu.netmind.beankeeper.event.PersistenceEventListener;
import hu.netmind.beankeeper.event.PersistenceEvent;
import hu.netmind.beankeeper.serial.SerialTracker;
import hu.netmind.beankeeper.operation.OperationTracker;
import hu.netmind.beankeeper.node.NodeManager;
import hu.netmind.beankeeper.node.event.RemoteStateChangeEvent;
import org.apache.log4j.Logger;
import java.util.*;

/**
 * This class tracks current commits and queries. The public methods are all server side methods.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public class OperationTrackerImpl implements OperationTracker,PersistenceEventListener
{
   private static Logger logger = Logger.getLogger(OperationTrackerImpl.class);
   
   private Object mutex = new Object();
   private HashMap commitsByIndex;
   private SortedSet commitSerials;

   private EventDispatcher eventDispatcher = null; // Injected
   private NodeManager nodeManager = null; // Injected
   private SerialTracker serialTracker = null; // Injected

   /**
    * Initialize.
    */
   public void init(Map parameters)
   {
      commitsByIndex = new HashMap();
      commitSerials = new TreeSet();
      eventDispatcher.registerListener(this,EventDispatcher.PRI_SYSTEM_LOW);
   }

   /**
    * Release all resources associated with this service.
    */
   public void release()
   {
      eventDispatcher.unregisterListener(this);
      commitsByIndex = new HashMap();
      commitSerials = new TreeSet();
   }

   /**
    * Get the operations mutex. This method is not usable remotely.
    */
   public Object getMutex()
   {
      return mutex;
   }

   public Long startCommit(int index)
   {
      // Make it a server call
      if ( nodeManager.getRole() == NodeManager.NodeRole.CLIENT )
      {
         return (Long) nodeManager.callServer(OperationTracker.class.getName(),
               "startCommit",new Class[] { int.class },
               new Object[] { index });
      }
      synchronized ( getMutex() )
      {
         // Get serial and register it
         Long serial = serialTracker.getNextSerial();
         Set serials = (Set) commitsByIndex.get(new Integer(index));
         if ( serials == null )
         {
            serials = new HashSet();
            commitsByIndex.put(new Integer(index),serials);
         }
         serials.add(serial);
         commitSerials.add(serial);
         // Return serial
         if ( logger.isDebugEnabled() )
            logger.debug("starting commit lock for node "+index+", serial: "+serial);
         return serial;
      }
   }

   public void endCommit(int index, Long serial, Long txSerial)
   {
      // Make it a server call
      if ( nodeManager.getRole() == NodeManager.NodeRole.CLIENT )
      {
         nodeManager.callServer(OperationTracker.class.getName(),
               "endCommit",new Class[] { int.class, Long.class, Long.class },
               new Object[] { index, serial, txSerial });
         return;
      }
      // Local
      if ( logger.isDebugEnabled() )
         logger.debug("ending commit lock for node "+index+", serial: "+serial);
      synchronized ( getMutex() )
      {
         // Remove the serial
         Set serials = (Set) commitsByIndex.get(new Integer(index));
         if ( serials == null )
            return;
         serials.remove(serial);
         if ( serials.size() == 0 )
            commitsByIndex.remove(new Integer(index));
         commitSerials.remove(serial);
         // Notify waiting queries to re-check commit serials
         getMutex().notifyAll();
      }
   }

   public void handle(PersistenceEvent event)
   {
      if ( event instanceof RemoteStateChangeEvent )
      {
         // If a remote node disconnected, then remove all the current
         // operations of that node.
         int index = ((RemoteStateChangeEvent) event).getNodeId();
         synchronized ( getMutex() )
         {
            Set serials = (Set) commitsByIndex.remove(new Integer(index));
            if ( serials == null )
               return;
            Iterator serialIterator = serials.iterator();
            while ( serialIterator.hasNext() )
               commitSerials.remove(serialIterator.next());
            // Notify waiting queries to re-check commit serials
            getMutex().notifyAll();
         }
      }
   }

   /**
    * This method must block until all commits which are lower
    * serials are finished.
    */
   public void waitForQuery(Long serial)
   {
      // Make it a server call
      if ( nodeManager.getRole() == NodeManager.NodeRole.CLIENT )
      {
         nodeManager.callServer(OperationTracker.class.getName(),
               "waitForQuery",new Class[] { Long.class },
               new Object[] { serial });
         return;
      }
      // Local
      synchronized ( getMutex() )
      {
         // Get the lowest commit serial. If that is higher than this
         // serial, than all commits are finished before this query.
         Long lowest = null;
         while ( (!commitSerials.isEmpty()) && ((lowest=(Long) commitSerials.first()).compareTo(serial) <= 0) )
         {
            if ( logger.isDebugEnabled() )
               logger.debug("serial '"+serial+"' blocked by: "+lowest);
            try
            {
               // Wait until some commit finishes
               getMutex().wait();
            } catch ( InterruptedException e ) {
               logger.warn("query wait was interrupted",e);
            }
         }
      }
   }
}


