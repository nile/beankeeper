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

package hu.netmind.beankeeper.type.impl;

import java.util.*;
import hu.netmind.beankeeper.parser.*;
import hu.netmind.beankeeper.model.*;
import hu.netmind.beankeeper.transaction.Transaction;

/**
 * A container class which can be used with the container handler must
 * implement this interface.
 * @author Brautigam Robert
 * @version CVS Revision: $Revision$
 */
public interface Container
{
   /**
    * Clear all values in this container.
    */
   void clear();

   /**
    * Retain all container elements inside the other container.
    */
   boolean retainAll(Object c);

   /**
    * Add all items in the other container.
    */
   boolean addAll(Object c);

   /**
    * Get the items' classname.
    */
   String getItemClassName();

   /**
    * Get the size of the container.
    */
   int size();

   /**
    * Returns whether the container changes internally since last save().
    */
   boolean hasChanged();

   /**
    * Save the container to database.
    */
   void save(Transaction transaction, Long currentSerial, Set waitingObjects, 
         Set saveTables, Set removeTables, List events);

   /**
    * Reload container from database. This will free any
    * resources and current yet not saved modifications.
    */
   void reload();

   /**
    * Get the serial number of last modification.
    */
   Long getLastSerial();

   /**
    * Get the parent (containing) object, for which this container was
    * originally created for.
    */
   Object getParent();
   
   /**
    * Initialize impl.
    */
   void init(ClassInfo classInfo, Object obj, 
         String attributeName, String itemClassName, Long lastSerial, TimeControl timeControl);
}


