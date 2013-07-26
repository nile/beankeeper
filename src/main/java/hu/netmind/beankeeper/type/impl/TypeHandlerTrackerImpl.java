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

import hu.netmind.beankeeper.type.TypeHandlerTracker;
import hu.netmind.beankeeper.type.TypeHandler;
import hu.netmind.beankeeper.service.StoreContext;
import java.util.*;

/**
 * This tracker manages custom type handlers, such as Collection and
 * Map handlers. It is possible to register other handlers.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public class TypeHandlerTrackerImpl implements TypeHandlerTracker
{
   private HashMap handlers;

   private StoreContext context = null; // Injected

   public void init(Map parameters)
   {
      handlers = new HashMap();      
   }

   public void release()
   {
   }

   /**
    * Register a type handler for a given type.
    */
   public void registerHandler(Class type, TypeHandler handler)
   {
      context.injectServices(handler);
      handlers.put(type,handler);
   }

   /**
    * Get the current handler for a given type.
    */
   public TypeHandler getHandler(Class type)
   {
      return (TypeHandler) handlers.get(type);
   }
  
   /**
    * Returns whether there is a handler set for the given type.
    */ 
   public boolean isHandled(Class type)
   {
      return getHandler(type) != null;
   }

}


