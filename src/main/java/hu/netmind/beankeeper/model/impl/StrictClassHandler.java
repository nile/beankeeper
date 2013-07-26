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

package hu.netmind.beankeeper.model.impl;

import java.util.List;
import java.util.Map;
import hu.netmind.beankeeper.model.ClassEntry;

/**
 * This is a strict class' handler interface. A strict handler only handles
 * the exact class it is supposed to handle, regardless of sub- or super-classes.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public interface StrictClassHandler
{
   /**
    * Get source entry.
    */
   ClassEntry getSourceEntry();

   /**
    * Return whether the object schema changed compared to the state
    * held by the handler.
    */
   boolean hasChanged();

   /**
    * Update to current model.
    */
   void update();

   /**
    * Return the attribute names and it's types as a map.
    */
   Map getAttributeTypes();

   /**
    * Return all declared attribute's name.
    */
   List getAttributeNames();

   /**
    * Get an object's attribute value.
    */
   Object getAttributeValue(Object obj, String attributeName);

   /**
    * Set an object's attribute.
    */
   void setAttributeValue(Object obj, String attributeName, Object value);
}


