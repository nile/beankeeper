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

package hu.netmind.beankeeper.event;

import hu.netmind.beankeeper.service.Service;

/**
 * Dispatches all events from the library internals to all registered
 * listeners.
 * <strong>Note:</strong> Implementations check for listeners
 * who cause event loops, and temporary exclude these from event delivery
 * to prevent endless loops.
 * @author Brautigam Robert
 * @version CVS Revision: $Revision$
 */
public interface EventDispatcher extends Service
{
   int PRI_SYSTEM_LOW = -100;
   int PRI_LOW = 0;
   int PRI_NORMAL = 50;
   int PRI_HIGH = 100;
   int PRI_SYSTEM_HIGH = 200;

   /**
    * Register the given listener to this dispatcher.
    */
   void registerListener(PersistenceEventListener listener);

   /**
    * Register the given listener to this dispatcher with a 
    * priority.
    * @param priority The priority of the listener. The lower the earlier
    * the listener gets called. This has to be between PRI_LOW,
    * and PRI_HIGH inclusive.
    */
   void registerListener(PersistenceEventListener listener, int priority);

   /**
    * Remove the given listener from this dispatcher.
    */
   void unregisterListener(PersistenceEventListener listener);

   /**
    * Notify all listeners of given event. This may not reach all event 
    * handlers, if there was an exception.
    * @throws Exception The event handlers' exception is forwarded as-is.
    */
   void notify(PersistenceEvent event)
      throws Exception;

   /**
    * Notify all listeners in a guaranteed way. Exceptions that occur
    * in the handlers will be logged, but not cause the event delivery
    * to fail.
    * @return True if the event delivery was a full success, false is one or
    * more event handlers failed.
    */
   boolean notifyAll(PersistenceEvent event);

}


