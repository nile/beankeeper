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

package hu.netmind.beankeeper.type.impl;

import hu.netmind.beankeeper.service.Service;
import hu.netmind.beankeeper.type.TypeHandlerTracker;
import java.util.*;

/**
 * Register the default type handlers into the tracker.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public class DefaultHandlersService implements Service
{
   private TypeHandlerTracker typeHandlerTracker = null; // Injected

   public void init(Map parameters)
   {
      // Default handlers
      typeHandlerTracker.registerHandler(Collection.class,new CollectionHandler(SetImpl.class));
      typeHandlerTracker.registerHandler(List.class,new ListHandler(ListImpl.class));
      typeHandlerTracker.registerHandler(Set.class,new CollectionHandler(SetImpl.class));
      typeHandlerTracker.registerHandler(Map.class,new MapHandler());
   }

   public void release()
   {
   }
}


