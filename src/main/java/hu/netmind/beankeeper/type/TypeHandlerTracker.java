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

package hu.netmind.beankeeper.type;

import hu.netmind.beankeeper.service.Service;

/**
 * This service tracks custom attribute type handlers. Attributes that
 * are not primitive, boxed primitive, arrays, String or Data are not
 * handled by default and need a type handler. Type handlers need to
 * be registered in this tracker.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public interface TypeHandlerTracker extends Service
{
   /**
    * Register a type handler for a given attribute type.
    */
   void registerHandler(Class type, TypeHandler handler);

   /**
    * Get the current handler for a given type.
    */
   TypeHandler getHandler(Class type);
  
   /**
    * Returns whether there is a handler set for the given type.
    */ 
   boolean isHandled(Class type);
}


