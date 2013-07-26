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

package hu.netmind.beankeeper.event.impl;

import hu.netmind.beankeeper.event.EventDispatcher;
import hu.netmind.beankeeper.event.PersistenceEventListener;
import hu.netmind.beankeeper.event.PersistenceEvent;
import java.util.*;
import org.apache.log4j.Logger;

/**
 * Dispatches all events from the library internals to all registered
 * listeners.
 * @author Brautigam Robert
 * @version CVS Revision: $Revision$
 */
public class EventDispatcherImpl implements EventDispatcher
{
   private static Logger logger = Logger.getLogger(EventDispatcherImpl.class);
   private List<PersistenceEventListener> listeners =
      new LinkedList<PersistenceEventListener>();
   private ThreadLocal<Set<PersistenceEventListener>> runningListeners = 
      new ThreadLocal<Set<PersistenceEventListener>>();

   public void init(Map parameters)
   {
   }

   public void release()
   {
   }

   /**
    * Register the given listener to this dispatcher.
    */
   public void registerListener(PersistenceEventListener listener)
   {
      registerListener(listener,PRI_NORMAL);
   }

   /**
    * Register the given listener to this dispatcher with a priority.
    */
   public void registerListener(PersistenceEventListener listener, int priority)
   {
      synchronized ( listeners )
      {
         if ( ! listeners.contains(listener) )
            listeners.add(listener);
      }
   }

   /**
    * Remove the given listener from this dispatcher.
    */
   public void unregisterListener(PersistenceEventListener listener)
   {
      synchronized ( listeners )
      {
         listeners.remove(listener);
      }
   }

   /**
    * Notify all listeners of given event. This may not reach all event 
    * handlers, if there was an exception.
    * @throws Exception The event handlers' exception is forwarded as-is.
    */
   public void notify(PersistenceEvent event)
      throws Exception
   {
      notify(event,true);
   }

   /**
    * Notify all listeners in a guaranteed way. Exceptions that occur
    * in the handlers will be logged, but not cause the event delivery
    * to fail.
    * @return True if the event delivery was a full success, false is one or
    * more event handlers failed.
    */
   public boolean notifyAll(PersistenceEvent event)
   {
      try
      {
         return notify(event,false);
      } catch ( Exception e ) {
         // This should not be
         logger.error("guaranteed delivery threw error, this should no be possible",e);
      }
      return false;
   }

   /**
    * Go through all listeners and notify them. Also, prevent a listener notifying
    * itself in an infinite loop.
    * @param throwExceptions If true, method will stop on the first error and
    * throw it back.
    */
   private boolean notify(PersistenceEvent event, boolean throwExceptions)
      throws Exception
   {
      if ( runningListeners.get() == null )
         runningListeners.set(new HashSet<PersistenceEventListener>());
      boolean success = true;
      if ( logger.isDebugEnabled() )
         logger.debug("delivering "+event+" to "+listeners.size()+" listeners");
      // Copy listeners into a local list
      List<PersistenceEventListener> localListeners = null;
      synchronized ( listeners )
      {
         localListeners = new ArrayList(listeners);
      }
      // Deliver events to local copy of listeners
      for ( PersistenceEventListener listener : localListeners )
      {
         // Insert listener to a thread local list, so the next
         // time this method gets called (recursively) then this
         // listener will not be executed again.
         if ( runningListeners.get().contains(listener) )
            continue; // Skip this listener, it's already on our call stack
         runningListeners.get().add(listener);
         // Run the handler
         try
         {
            listener.handle(event);
         } catch ( Exception e ) {
            if ( throwExceptions )
               throw e;
            logger.warn("an event handler for event: "+event+", was not successful on guaranteed delivery",e);
            success=false;
         }
         // Remove the handler from running list
         runningListeners.get().remove(listener);
      }
      return success;
   }

}


