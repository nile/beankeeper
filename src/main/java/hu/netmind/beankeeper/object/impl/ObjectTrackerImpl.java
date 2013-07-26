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

package hu.netmind.beankeeper.object.impl;

import hu.netmind.beankeeper.common.StoreException;
import hu.netmind.beankeeper.type.TypeHandler;
import hu.netmind.beankeeper.type.TypeHandlerTracker;
import hu.netmind.beankeeper.model.*;
import hu.netmind.beankeeper.object.ObjectTracker;
import hu.netmind.beankeeper.object.PersistenceMetaData;
import hu.netmind.beankeeper.object.Identifier;
import hu.netmind.beankeeper.logging.SnapshotLogger;
import java.util.*;
import java.lang.ref.WeakReference;
import org.apache.log4j.Logger;

/**
 * This class tracks objects' state for different transactions.
 * Basically this tracker can answer questions about the state of a
 * previously registered object.<br>
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public class ObjectTrackerImpl implements ObjectTracker, WeakMapListener
{
   private static Logger logger = Logger.getLogger(ObjectTrackerImpl.class);
   
   private Random random;
   private WeakMap objectData;
   private HashMap sharedData;

   private ClassTracker classTracker = null; // Injected
   private TypeHandlerTracker typeHandlerTracker = null; // Injected
   private SnapshotLogger snapshotLogger = null; // Injected

   public void init(Map parameters)
   {
      random = new Random();
      sharedData = new HashMap();
      objectData = new WeakMap(snapshotLogger);
      objectData.setListener(this);
   }

   public void release()
   {
   }

   /**
    * Get the data structure for an object.
    */
   public ObjectData getObjectData(Object obj)
   {
      // Only the objectData needs to be synchronized, because only one
      // transaction can write these objects at a given time. So many
      // readers may be using the objectdata at a time, but only
      // one can write it.
      synchronized ( objectData )
      {
         return (ObjectData) objectData.get(obj);
      }
   }

   /**
    * Return whether a given attribute of a given object changed.
    */
   public boolean hasChanged(ClassInfo info, Object obj, String attributeName, Map dbAttributes,
         Long serial)
   {
      // If object does no exists, then all attributes are changed. If
      // object exists, then the dbAttributes contains the current
      // attributes, as found in the database.
      if ( ! exists(obj) )
         return true;
      // Get values  
      Object value = info.getAttributeValue(obj,attributeName);
      Object dbValue = dbAttributes.get(attributeName);
      // Check nulls
      if ( (dbValue==null) && (value==null) )
      {
         logger.debug("both values were null, not changed.");
         return false;
      }
      if ( ((dbValue!=null) && (value==null)) ||
           ((dbValue==null) && (value!=null)) )
      {
         logger.debug("one value was null, and the other non-null, so changed.");
         return true;
      }
      // Check handled types
      Class type = info.getAttributeType(attributeName);
      if ( typeHandlerTracker.isHandled(type) )
      {
         TypeHandler handler = typeHandlerTracker.getHandler(type);
         boolean result = handler.hasChanged(info,obj,attributeName,dbAttributes,serial); 
         logger.debug("value was a tracked object, result will be: "+result);
         return result;
      }
      // Check byte array
      if ( dbValue instanceof byte[] )
      {
         boolean result = ! Arrays.equals((byte[]) dbValue, (byte[]) value);
         logger.debug("values were byte arrays, changed: "+result);
         return result;
      }
      // Check custom objects, in which case the dbValue is a persistence_id,
      // and the 'value' is an object.
      if ( classTracker.getType(value.getClass()) == ClassTracker.ClassType.TYPE_OBJECT )
      {
         boolean result = ! dbValue.equals(getIdentifier(value));
         logger.debug("value was a custom object, it's persistence id changed: "+result);
         return result;

      }
      // Fallback to equals()
      boolean result = !value.equals(dbValue);
      logger.debug("comparing values with equals(), changed: "+result+" ("+value.getClass().getName()+" vs. db "+dbValue.getClass().getName()+")");
      return result;
   }

   /**
    * Determines, whether two objects are of the same database instance.
    * Two objects are the same, if their ids equal. Note however, that
    * they do not need to contain the same values, or be of same version!
    */
   public boolean equals(Object o1, Object o2)
   {
      Long id1 = getIdentifier(o1);
      Long id2 = getIdentifier(o2);
      return (o1==o2) || ((id1!=null) && (id2!=null) && (id1.equals(id2)));
   }

   /**
    * Determine whether an object exists.
    * @return True, if object exists in store. This means that searches
    * will return the object. If this is false, the object cannot be
    * found yet.
    */
   public boolean exists(Object obj)
   {
      ObjectData data = getObjectData(obj);
      if ( data == null )
         return false;
      return data.currentlyExists();
   }

   /**
    * Mark object as existent. If this is inside a transaction,
    * the existence will only be permanent, if object is saved inside
    * the transaction.
    */
   public void makeExist(Object obj)
   {
      ObjectData data = getObjectData(obj);
      if ( data != null )
         data.setCurrentlyExists(true);
   }

   /**
    * Make the object current at the given serial. If the object is 
    * more current, nothing is done. It is assumed the object exists.
    */
   public void makeCurrent(Object obj, Long serial)
   {
      PersistenceMetaDataImpl persistenceMeta = getObjectData(obj).getMetaData();
      Long lastCurrentSerial = persistenceMeta.getLastCurrentSerial();
      if ( (serial!=null) && ((lastCurrentSerial==null) || 
               (lastCurrentSerial.longValue() < serial.longValue() )) )
         persistenceMeta.setLastCurrentSerial(serial);
   }

   /**
    * Mark an object as non-existent. This happens, if the object gets
    * deleted, but there are some object instances that are used.
    */
   public void makeUnexist(Object obj)
   {
      ObjectData data = getObjectData(obj);
      if ( data != null )
      {
         data.setExists(false);
         data.setCurrentlyExists(false);
      }
   }

   /**
    * Register a non-existing object into the tracker. If the object is
    * tracked by this tracker, then nothing is done.
    * @param obj The object to register.
    */
   public void registerObject(Object obj)
   {
      registerObject(obj,null,null,null,null);
   }

   /**
    * Register an object into the tracker. This means, the object will
    * get an id, and associated tracking data structure will be allocated.
    * If the object given has an attribute named persistenceId, then use
    * that to re-attach the object to the tracker.
    * @param obj The object to register.
    * @param id The id of object. If this is given (not null), the object is 
    * assumed to exist.
    * @param serial The current serial, can be null if unknown.
    * @param startSerial The start of this object, can be null, if unknown.
    * @param endSerial The ens serial of this object, if it's deleted, can be null.
    */
   public void registerObject(Object obj, Long id, Long serial,
         Long startSerial, Long endSerial)
   {
      ObjectData data = getObjectData(obj);
      if ( data != null )
         return;
      // Create object data
      data = new ObjectData();
      if ( id == null )
      {
         // Check, whether object has 'persistenceId' given
         ClassInfo info = classTracker.getClassInfo(obj.getClass(),obj);
         if ( (info.getAttributeNames().contains("persistenceid")) &&
              (info.getAttributeValue(obj,"persistenceid")!=null) &&
              (((Long)info.getAttributeValue(obj,"persistenceid")).longValue()!=0) )
         {
            Long persistenceId = (Long) info.getAttributeValue(obj,"persistenceid");
            ClassEntry idEntry = classTracker.getClassEntry(
                  new Identifier(persistenceId).getClassId());
            if ( ! info.getSourceEntry().equals(idEntry) )
               throw new StoreException("trying to re-attach object, but given persistenceid: "+
                     persistenceId+" indicates entry: "+idEntry+", object is: "+info.getSourceEntry());
            // Object must be re-attached. Re-attached objects
            // are assumed to exists. If the user assigns false ids
            // to objects, this would mean the exists() function would
            // return false information.
            data.setId(persistenceId);
            data.setExists(true);
            data.setCurrentlyExists(true);
            if ( logger.isDebugEnabled() )
               logger.debug("object re-attached as: "+persistenceId);
         } else {
            // This is a whole new object, so create new id
            data.setId(classTracker.getNextId(new ClassEntry(obj)));
            data.setExists(false);
            data.setCurrentlyExists(false);
            if ( logger.isDebugEnabled() )
               logger.debug("aquired new id to new object: "+data.getId());
         }
      } else {
         data.setId(id);
         data.setExists(true);
         data.setCurrentlyExists(true);
         if ( logger.isDebugEnabled() )
            logger.debug("object loaded with id: "+data.getId());
      }
      PersistenceMetaDataImpl metaData = new PersistenceMetaDataImpl();
      data.setMetaData(metaData);
      metaData.setPersistenceId(data.getId());
      metaData.setPersistenceStart(startSerial);
      metaData.setPersistenceEnd(endSerial);
      metaData.setRegistrationSerial(serial);
      metaData.setLastCurrentSerial(serial);
      metaData.setObjectClass(obj.getClass());
      synchronized ( objectData )
      {
         objectData.put(obj,data,data.getId());
         // Create shared state, if not present
         SharedData sData = data.getSharedData();
         if ( sData == null )
            sData = new SharedData();
         else
            sData.setRefCounter(sData.getRefCounter()+1);
         sharedData.put(data.getId(),sData);
      }
      // Profile
      snapshotLogger.log("sharedmap","Shared data size is: "+sharedData.size());
   }

   /**
    * Called when an object leaves the weak map. Note: This is thread-safe
    * because it is called from WeakMap, which is handled safely in this
    * class.
    */
   public void notifyValueLeave(Object id)
   {
      // Object is leaving weakmap, so remove shared state too if nescessary.
      SharedData data = (SharedData) sharedData.get(id);
      // Decrease count, remove if no object reference it
      data.setRefCounter(data.getRefCounter()-1);
      if ( data.getRefCounter() <= 0 )
         sharedData.remove(id);
   }

   /**
    * Get the identifier for a given object.
    * @param obj The object to identify.
    * @return The identifier, or 0 if the object is not registered.
    */
   public Long getIdentifier(Object obj)
   {
      ObjectData data = getObjectData(obj);
      if ( data == null )
         return null;
      return data.getId();
   }

   /**
    * Get the meta data associated with the object.
    */
   public PersistenceMetaData getMetaData(Object obj)
   {
      // Register to be sure
      registerObject(obj);
      // Get the metadata
      return getObjectData(obj).getMetaData();
   }

   /**
    * Get the current attributes of an object.
    * @return The current attributes of the object, null
    * if it's not known.
    */
   public Map getCurrentAttributes(Object obj)
   {
      ObjectData data = getObjectData(obj);
      return data.getSharedData().attributes;
   }

   /**
    * The object commited, all known changes should be made permanent now.
    * @param obj The object to update.
    * @param serial The serial of this update.
    */
   public void updateCommit(Object obj, Long serial)
   {
      ObjectData data = (ObjectData) getObjectData(obj);
      if ( data == null )
      {
         logger.warn("something may be wrong, an object commited which was not tracked, object was: "+obj);
      } else {
         // Set object transaction exists to stable exists
         data.setExists(data.currentlyExists());
         // Commit transactional attributes
         SharedData sData = data.getSharedData();
         sData.originalAttributes = null;
      }
   }
   
   /**
    * Indicate that the object is current at the given serial for sure.
    * @param obj The object to update.
    * @param serial The serial on which object was sure to be current.
    */
   public void updateCurrent(Object obj, Long serial)
   {
      // Update meta-data
      PersistenceMetaDataImpl metaData = (PersistenceMetaDataImpl) getMetaData(obj);
      if ( metaData.getPersistenceStart() == null )
         metaData.setPersistenceStart(serial);
      metaData.setLastCurrentSerial(serial);
   }
   
   /**
    * The object rolled back, all known changes should be rolled back too.
    * @param obj The object to update.
    * @param serial The serial of this update.
    */
   public void updateRollback(Object obj, Long serial)
   {
      ObjectData data = (ObjectData) getObjectData(obj);
      if ( data == null )
      {
         logger.warn("something may be wrong, an object rolled back which was not tracked, object was: "+obj);
      } else {
         // Set current exists flag to stable exists flag
         data.setCurrentlyExists(data.exists());
         // Clear transactional attributes
         SharedData sData = data.getSharedData();
         sData.attributes = sData.originalAttributes;
         sData.originalAttributes = null;
      }
   }
   
   /**
    * Update the object's data to indicate object is known to be current
    * at the given serial. It is assumed that the object exists.
    * @param obj The object to update.
    * @param serial The serial of this update.
    */
   public void updateObject(Object obj, Long serial, Long startSerial, Long endSerial)
   {
      PersistenceMetaDataImpl metaData = getObjectData(obj).getMetaData();
      if ( serial != null )
         metaData.setLastCurrentSerial(serial);
      if ( endSerial != null )
         metaData.setPersistenceEnd(endSerial);
      if ( startSerial != null )
         metaData.setPersistenceStart(startSerial);
   }
   
   /**
    * Update the current state of the given object.
    * @param obj The object to update.
    * @param attributes The attributes which indicate the _current_ state of
    * the object. If this attribute is null, then this indicates, that
    * the object has changed, but the current state is not known. 
    */
   public void updateObject(Object obj, Map attributes)
   {
      synchronized ( objectData )
      {
         // Get data
         ObjectData data = getObjectData(obj);
         updateObjectId(data.getId(),attributes);
      }
   }
   
   /**
    * Update the current state of the given object.
    * @param id The id of the object to update.
    * @param attributes The attributes which indicate the _current_ state of
    * the object. If this attribute is null, then this indicates, that
    * the object has changed, but the current state is not known. 
    */
   public void updateObjectId(Long id, Map attributes)
   {
      synchronized ( objectData )
      {
         SharedData sData = (SharedData) sharedData.get(id);
         if ( sData == null )
            return;
         // Set state
         if ( attributes == null )
         {
            sData.attributes = null;
            sData.originalAttributes = null;
         } else {
            sData.attributes=attributes;
            if ( sData.originalAttributes == null )
               sData.originalAttributes=attributes;
         }
      }
   }

   /**
    * Get an object wrapper for an object which disregards object's
    * own equals() and hashCode() methods.
    */
   public ObjectWrapper getWrapper(Object obj)
   {
      return new ObjectWrapperImpl(this,obj);
   }

   /**
    * Search for an item in a list based on id.
    */
   /**
    * This is the structure tracked for an object.
    */
   public class ObjectData implements Comparable
   {
      private Long id;
      private boolean exists;
      private boolean currentlyExists; // Inside transaction it may differ from 'exists'.
      private PersistenceMetaDataImpl metaData;

      public ObjectData()
      {
      }

      public PersistenceMetaDataImpl getMetaData()
      {
         return metaData;
      }
      public void setMetaData(PersistenceMetaDataImpl metaData)
      {
         this.metaData=metaData;
      }

      public int compareTo(Object obj)
      {
         return id.compareTo(((ObjectData)obj).id);
      }

      public SharedData getSharedData()
      {
         return (SharedData) sharedData.get(id);
      }

      public Long getId()
      {
         return id;
      }
      private void setId(Long id)
      {
         this.id=id;
      }

      public boolean exists()
      {
         return exists;
      }
      private void setExists(boolean exists)
      {
         this.exists=exists;
      }
      private boolean currentlyExists()
      {
         return currentlyExists;
      }
      public void setCurrentlyExists(boolean currentlyExists)
      {
         this.currentlyExists=currentlyExists;
      }
   }

   /**
    * This is a structure which is shared for objects of the same id.
    */
   public class SharedData
   {
      private Map attributes; // Attributes of _current_ version
      private Map originalAttributes; // Original attributes before tx
      private int refCounter;

      public SharedData()
      {
         refCounter=1;
      }

      public int getRefCounter()
      {
         return refCounter;
      }
      public void setRefCounter(int refCounter)
      {
         this.refCounter=refCounter;
      }

   }

   public class ObjectWrapperImpl implements ObjectWrapper
   {
      private Object obj;
      private Long id;

      public ObjectWrapperImpl(ObjectTracker objectTracker, Object obj)
      {
         this.obj=obj;
         registerObject(obj);
         this.id=objectTracker.getIdentifier(obj);
      }

      public Long getIdentifier()
      {
         return id;
      }

      public Object getObject()
      {
         return obj;
      }

      public int hashCode()
      {
         return (int) (id.longValue()>>32);
      }

      public boolean equals(Object rhs)
      {
         if ( ! (rhs instanceof ObjectWrapperImpl) )
            return false;
         return id.equals(((ObjectWrapperImpl) rhs).id);
      }
   }
}

