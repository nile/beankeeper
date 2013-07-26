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

package hu.netmind.beankeeper.object;

import hu.netmind.beankeeper.service.Service;
import hu.netmind.beankeeper.model.*;
import java.util.*;

/**
 * This service tracks objects' state for different transactions.
 * Basically this tracker can answer questions about the state of a
 * previously registered object.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public interface ObjectTracker extends Service
{
   /**
    * Return whether a given attribute of a given object changed.
    * @param info The class info of the object.
    * @param obj The object.
    * @param attributeName The name of the attribute to check.
    * @param dbAttributes The attribute values as in the database.
    * @param serial The serial number this question is asked in.
    */
   boolean hasChanged(ClassInfo info, Object obj, String attributeName, Map dbAttributes,
         Long serial);

   /**
    * Determines, whether two objects are of the same database instance.
    * Two objects are the same, if their ids equal. Note however, that
    * they do not need to contain the same values, or be of same version!
    */
   boolean equals(Object o1, Object o2);

   /**
    * Determine whether an object exists.
    * @return True, if object exists in store. This means that searches
    * will return the object. If this is false, the object cannot be
    * found yet.
    */
   boolean exists(Object obj);

   /**
    * Mark object as existent. If this is inside a transaction,
    * the existence will only be permanent, if object is saved inside
    * the transaction.
    */
   void makeExist(Object obj);

   /**
    * Mark an object as non-existent. This happens, if the object gets
    * deleted, but there are some object instances that are used.
    */
   void makeUnexist(Object obj);

   /**
    * Register a non-existing object into the tracker. If the object is
    * tracked by this tracker, then nothing is done.
    * @param obj The object to register.
    */
   void registerObject(Object obj);

   /**
    * Register an existing object into the tracker.
    * @param obj The object to register.
    * @param id The id of the object.
    * @param serial The serial of the current operation.
    * @param startSerial The serial this object is available in the database from.
    * @param endSerial The serial this object was deleted.
    */
   void registerObject(Object obj, Long id, Long serial, Long startSerial, Long endSerial);

   /**
    * Update the object's meta data.
    * @param obj The object to register.
    * @param serial The serial of the current operation.
    * @param startSerial The serial this object is available in the database from.
    * @param endSerial The serial this object was deleted.
    */
   void updateObject(Object obj, Long serial, Long startSerial, Long endSerial);

   /**
    * Get the identifier for a given object.
    * @param obj The object to identify.
    * @return The identifier, or null if the object is not registered.
    */
   Long getIdentifier(Object obj);

   /**
    * Get the meta data associated with the object. Note, if
    * object is not registered, it will be now.
    * @return The meta data associated with the object. This is never null.
    */
   PersistenceMetaData getMetaData(Object obj);

   /**
    * Get the current attributes of an object, including in all parallel nodes.
    * @return The current attributes of the object, null
    * if it's not known.
    */
   Map<String,Object> getCurrentAttributes(Object obj);

   /**
    * Update the current state of the given object. Note, that this
    * does not mean that the metadata should change, because there is
    * no commit yet.
    * @param obj The object to update.
    * @param attributes The attributes which indicate the _current_ state of
    * the object among all nodes. If this attribute is null, then this indicates, that
    * the object has changed, but the current state is not known. 
    */
   void updateObject(Object obj, Map<String,Object> attributes);

   /**
    * Indicate that the object is current at the given serial for sure.
    * @param obj The object to update.
    * @param serial The serial on which object was sure to be current.
    */
   void updateCurrent(Object obj, Long serial);
   
   /**
    * The object commited, all known changes should be made permanent now.
    * @param obj The object to update.
    * @param serial The serial of this update.
    */
   void updateCommit(Object obj, Long serial);
   
   /**
    * The object rolled back, all known changes should be rolled back too.
    * @param obj The object to update.
    * @param serial The serial of this update.
    */
   void updateRollback(Object obj, Long serial);
   
   /**
    * Update the current state of the given object.
    * @param id The id of the object to update.
    * @param attributes The attributes which indicate the _current_ state of
    * the object. If this attribute is null, then this indicates, that
    * the object has changed, but the current state is not known. 
    * @param serial The serial the change took place.
    */
   void updateObjectId(Long id, Map<String,Object> attributes);

   /**
    * Get a wrapper for an object.
    */
   ObjectWrapper getWrapper(Object obj);

   /**
    * This clas wraps an object so it's equals and hashcode can be
    * overridden to provide persistent identity instead of object
    * identity.
    */
   interface ObjectWrapper
   {
      /**
       * Get the identifier.
       */
      Long getIdentifier();

      /**
       * Get the object.
       */
      Object getObject();
   }
}

