/**
 * Copyright (C) 2008 NetMind Consulting Bt.
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

package hu.netmind.beankeeper.object;

import java.io.Serializable;
import java.util.Date;

/**
 * An object of this class represents all persistence related information
 * about an object, which BeanKeeper is aware of.
 * @author Brautigam Robert
 * @version CVS Revision: $Revision$
 */
public interface PersistenceMetaData extends Serializable
{
   /**
    * Get the creation date of <strong>this version</strong>
    * of the object. If the object is not yet saved, this
    * value is null. Only committed creation dates are given.
    * If the object is saved in a transaction, but the
    * transaction did not yet commit, then this date will reflect
    * the last committed creation date of the object (before
    * the transaction).<br>
    * Selecting the object later with this date will give
    * the same version only if this version of the object
    * lived at least 1 millisecond (it was not deleted
    * or superseded in that time). If it didn't, then the next
    * version of the object is selected which lived at least
    * 1 millisecond.
    */
   Date getCreationDate();

   /**
    * Get the deletion date of <strong>this version</strong>
    * of the object. If the object was not yet saved, then
    * this value is null. If the object was not yet deleted,
    * but it exists in the database, then this date is an extremal
    * big date. Selecting the object on this date
    * will select the next version of the object which lived at least
    * 1 millisecond.
    */
   Date getRemoveDate();

   /**
    * Get the persistence id of the object. This id is
    * available to non-existent objects too, and it does
    * not ever change. Not if the object does not exist
    * yet, and not if a new version is saved or the object
    * is deleted.
    */
   Long getPersistenceId();

   /**
    * Get the creation serial number of this version. This is
    * a unique number based on dates. All versions will have different
    * serial numbers.
    */
   Long getPersistenceStart();

   /**
    * Get the end serial. If the object is not deleted, then
    * this is Long.MAX_VALUE.
    */
   Long getPersistenceEnd();

   /**
    * Get the serial number on which this object was registered into
    * the tracker.
    */
   Long getRegistrationSerial();

   /**
    * Get the object class.
    */
   Class getObjectClass();

   /**
    * Get the last serial this object was known to be current. An object
    * is known to be current (current on a given serial), when it is selected,
    * or modified.
    */
   Long getLastCurrentSerial();
}


