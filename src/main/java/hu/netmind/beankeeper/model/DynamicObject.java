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

package hu.netmind.beankeeper.model;

import java.util.Map;

/**
 * All objects implementing this interface are considered "dynamic", which
 * means they can define their own attributes,attribute types and the
 * table their "class" dynamically.<br>
 * Note however, that when you change the set of attributes for this kind of
 * objects, their representation in the database will change. All attributes
 * in dynamic objects are held in the same table (no separate value tables), so
 * if an attribute becomes deleted, it will resolve in a table column to be
 * dropped. The data dropped will not be available, even through historical
 * selects (because the table schema changed).<br>
 * Dynamic objects can be in their own namespace, meaning they can have
 * each it's own table, so caller may define different types dynamically
 * with dynamic objects.<br>
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public interface DynamicObject extends Map
{
   /**
    * Get the dynamic name of the class. If this is not null and non-empty,
    * the library will create this name as it were a classname in the
    * package designated by the real class' name. After saving such a 
    * class, the caller will be able to select for these kind of classes
    * with the name specified inside find statements.
    * @return The dynamic name of class. Any string which is null, or
    * does not start with a letter is treated as an empty string, in which
    * case the class' real name will be used.
    */
   String getPersistenceDynamicName();

   /**
    * This method is called by the library during a select from database.
    * During the unmarshalling of the object, the library will set the
    * dynamic name to the object,
    * @param cl The class the name is queried for.
    * @param dynamicName The dynamic name of the class as given when
    * it was saved. If no (empty) dynamic name was given, it will be
    * called with an empty string.
    */
   void setPersistenceDynamicName(String dynamicName);
}


