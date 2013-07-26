/**
 * Copyright (C) 2009 NetMind Consulting Bt.
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

package hu.netmind.beankeeper.modification;

import hu.netmind.beankeeper.service.Service;
import hu.netmind.beankeeper.object.PersistenceMetaData;

/**
 * This object tracks modifications of objects, and can be used to get the
 * status of any given object.
 */
public interface ModificationTracker extends Service
{
   /**
    * Returns whether the given object instance is a current representation
    * of that object identity among all nodes. When any object is saved, all
    * of it's attributes will be saved as-is, to protect data integrity. If any
    * other thread or node already modified that identity (database row), then
    * those modifications will be overwritten. This method returns if the
    * object given has any such modifications since it was queried from the
    * database. Objects not yet in the database are considered 'current'.
    * @return True, if object is current, false otherwise.
    */
   boolean isCurrent(Object obj);

   /**
    * Returns whether the given class changed since the given serial. A class changed,
    * if any objects of the given class were changed.
    */
   boolean isCurrent(Class cl, Long serial);

   /**
    * Returns whether an object represented by the given meta data is
    * current.
    */
   boolean isCurrent(PersistenceMetaData meta);
}

