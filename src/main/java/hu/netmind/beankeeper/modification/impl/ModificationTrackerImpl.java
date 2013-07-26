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

package hu.netmind.beankeeper.modification.impl;

import hu.netmind.beankeeper.model.*;
import hu.netmind.beankeeper.event.EventDispatcher;
import hu.netmind.beankeeper.event.PersistenceEventListener;
import hu.netmind.beankeeper.event.PersistenceEvent;
import hu.netmind.beankeeper.transaction.event.RemoteTransactionPostCommitEvent;
import hu.netmind.beankeeper.store.event.ObjectsFinalizationEvent;
import hu.netmind.beankeeper.modification.ModificationTracker;
import hu.netmind.beankeeper.object.PersistenceMetaData;
import hu.netmind.beankeeper.object.Identifier;
import hu.netmind.beankeeper.object.ObjectTracker;
import hu.netmind.beankeeper.node.NodeManager;
import hu.netmind.beankeeper.operation.OperationTracker;
import hu.netmind.beankeeper.query.QueryService;
import java.util.*;
import org.apache.log4j.Logger;
import org.apache.commons.configuration.event.ConfigurationEvent;
import hu.netmind.beankeeper.config.ExtendedConfigurationListener;
import hu.netmind.beankeeper.config.ConfigurationTracker;

/**
 * This class tracks changes to objects. It caches the most recent changes, but
 * also determines whether an object changed by reading the database, if the
 * cache can't give a definite answer.
 * @author Robert Brautigam
 * @version CVS Revision: $Revision$
 */
public class ModificationTrackerImpl implements ModificationTracker, 
      ExtendedConfigurationListener, PersistenceEventListener
{
   private long MAX_ENTRY_AGE = 30*60*1000;
   private int MAX_ENTRY_ITEMS = 10000;
   private static Logger logger = Logger.getLogger(ModificationTrackerImpl.class);

   private Map entriesById = new HashMap();
   private SortedSet entriesByDate = new TreeSet();
   private Map entriesByTxSerial = new HashMap();
   private Map entriesByClass = new HashMap();
   private Map lastSerialsByClass = new HashMap();
   private Long lastCacheRemovedSerial = null;

   private EventDispatcher eventDispatcher = null; // Injected
   private ConfigurationTracker configurationTracker = null; // Injected
   private OperationTracker operationTracker = null; // Injected
   private NodeManager nodeManager = null; // Injected
   private ObjectTracker objectTracker = null; // Injected
   private QueryService queryService = null; // Injected

   public void init(Map parameters)
   {
      eventDispatcher.registerListener(this,EventDispatcher.PRI_SYSTEM_LOW);
      // Config
      configurationReload();
      configurationTracker.addListener(this);
   }

   public void release()
   {
      entriesById = new HashMap();
      entriesByDate = new TreeSet();
      entriesByTxSerial = new HashMap();
      entriesByClass = new HashMap();
      lastSerialsByClass = new HashMap();
      lastCacheRemovedSerial = null;
      eventDispatcher.unregisterListener(this);
      configurationTracker.removeListener(this);
   }

   private void modifyClassEntries(ModificationEntry entry, boolean add)
   {
      modifyClassEntries(entry.objectClass,entry,add);
   }

   private void modifyClassEntries(Class currentClass, ModificationEntry entry, boolean add)
   {
      if ( currentClass == null )
         return;
      // Do current class
      Set entries = (Set) entriesByClass.get(currentClass);
      if ( entries == null )
      {
         entries = new HashSet();
         entriesByClass.put(currentClass,entries);
      }
      if ( add )
         entries.add(entry);
      else
         entries.remove(entry);
      if ( entries.size() == 0 )
         entriesByClass.remove(currentClass);
      // Do superclass
      modifyClassEntries(currentClass.getSuperclass(),entry,add);
      // Do interfaces
      Class interfaces[] = currentClass.getInterfaces();
      for ( int i=0; i<interfaces.length; i++ )
         modifyClassEntries(interfaces[i],entry,add);
   }

   /**
    * This method registers "potential" change serials for a given id. The
    * change becomes final only when the transaction's commit ends.
    * @param meta The list of metainformation for objects that changed.
    * @param endSerial The serial on which these changes went into database.
    * @param txSerial The tx identifier for later tracking, whether the tx committed.
    */
   private void changeCandidates(List<PersistenceMetaData> metas, Long endSerial, Long txSerial)
   {
      synchronized ( operationTracker.getMutex() )
      {
         // Clear obsolate data
         while ( ((!entriesByDate.isEmpty()) && ( System.currentTimeMillis()-
                  ((ModificationEntry) entriesByDate.first()).entryDate.getTime() > MAX_ENTRY_AGE )) ||
              (entriesByDate.size() > MAX_ENTRY_ITEMS) )
         {
            // The first entry is obsolate, so clear it from the cache
            ModificationEntry entry = (ModificationEntry) entriesByDate.first();
            entriesById.remove(entry.id);
            entriesByDate.remove(entry);
            if ( entry.potentialTxSerial != null )
               entriesByTxSerial.remove(entry.potentialTxSerial);
            if ( entry.lastChangeSerial != null )
               lastCacheRemovedSerial = entry.lastChangeSerial;
            modifyClassEntries(entry,false); // Remove
         }
         // Add all data to internal structures
         if ( logger.isDebugEnabled() )
            logger.debug("changed following objects: "+metas+", with endserial: "+endSerial+", tx: "+txSerial);
         ArrayList entries = new ArrayList();
         entriesByTxSerial.put(txSerial,entries);
         for ( int i=0; i<metas.size(); i++ )
         {
            PersistenceMetaData meta = (PersistenceMetaData) metas.get(i);
            // Search or create entry
            ModificationEntry entry = (ModificationEntry) entriesById.get(meta.getPersistenceId());
            if ( entry == null )
            {
               entry = new ModificationEntry();
               entry.id = meta.getPersistenceId();
               entry.objectClass = meta.getObjectClass();
               entry.entryDate = new Date();
               // Update data structure
               entriesById.put(entry.id,entry);
               entriesByDate.add(entry);
               modifyClassEntries(entry,true);
            }
            entry.potentialChangeSerial=endSerial;
            if ( (entry.potentialTxSerial != null) && (!txSerial.equals(entry.potentialTxSerial)) )
            {
               // This entry was part of an old transaction, which was
               // unfinished (endCommit was not received). So delete from
               // it's list.
               List obsolateTxList = (List) entriesByTxSerial.get(entry.potentialTxSerial);
               if ( obsolateTxList != null )
               {
                  obsolateTxList.remove(entry);
                  if ( obsolateTxList.isEmpty() )
                     entriesByTxSerial.remove(entry.potentialTxSerial);
               }
            }
            entry.potentialTxSerial=txSerial;
            entries.add(entry);
         }
      }
   }

   /**
    * Set definite last modification serial for a class, and for all superclasses
    * and interfaces.
    */
   private void setLastSerial(Class currentClass, Long serial)
   {
      if ( currentClass == null )
         return;
      // Handle class
      lastSerialsByClass.put(currentClass,serial);
      // Superclass
      setLastSerial(currentClass.getSuperclass(),serial);
      // Interfaces
      Class interfaces[] = currentClass.getInterfaces();
      for ( int i=0; i<interfaces.length; i++ )
         setLastSerial(interfaces[i],serial);
   }

   public void handle(PersistenceEvent event)
   {
      // Handle end of transactions
      if ( event instanceof RemoteTransactionPostCommitEvent )
      {
         if ( nodeManager.getRole() == NodeManager.NodeRole.SERVER )
            endTransaction(((RemoteTransactionPostCommitEvent) event).getTxSerial());
      }
      if ( event instanceof ObjectsFinalizationEvent )
      {
         if ( nodeManager.getRole() == NodeManager.NodeRole.SERVER )
         {
            ObjectsFinalizationEvent finEvent = (ObjectsFinalizationEvent) event;
            changeCandidates(finEvent.getMetas(),finEvent.getSerial(),finEvent.getTxSerial());
         }
      }
   }

   /**
    * Ends a transaction, which means it finalizes the entries associated
    * with the given transaction.
    */
   private void endTransaction(Long txSerial)
   {
      synchronized ( operationTracker.getMutex() )
      {
         // Get and remove entries from tx map
         List entries = (List) entriesByTxSerial.remove(txSerial);
         if ( entries == null )
         {
            logger.debug("tx "+txSerial+" contained no changes, closing tx.");
            return; // No ids, probably it became obsolate
         }
         logger.debug("activating "+entries.size()+" changes for tx: "+txSerial);
         // Go through entries of ids and actualize them
         for ( int i=0; i<entries.size(); i++ )
         {
            ModificationEntry entry = (ModificationEntry) entries.get(i);
            entry.lastChangeSerial = entry.potentialChangeSerial;
            entry.potentialTxSerial = null;
            // Insert into the 'persistent' class serial table.
            setLastSerial(entry.objectClass,entry.lastChangeSerial);
         }
      }
   }

   /**
    * Return whether the given object changed since the given serial.
    */
   private boolean hasChanged(Long id, Class objectClass, Long serial)
   {
      // Try to get a modification entry if there is one for that object
      ModificationEntry entry = (ModificationEntry) entriesById.get(id);
      // If there was no entry, then there was no modification
      // since the last removed serial. If the query serial is newer than
      // the last removed serial, then the object must be current.
      if ( (entry==null) && ((lastCacheRemovedSerial==null) || 
               (serial.longValue() > lastCacheRemovedSerial.longValue())) )
      {
         logger.debug("there was no modification yet for this class in cache, "+
               "but the serial is newer the cache's start serial, so object is current.");
         return false;
      }
      // If there was no entry, and the query serial is old, we must check the
      // database whether that object is current. Note, that at this point the
      // object should be locked, or else this query can not tell for sure.
      if ( (entry==null) && (lastCacheRemovedSerial!=null) && 
            (serial.longValue() <= lastCacheRemovedSerial.longValue()) )
      {
         // Query the object now
         Object current = queryService.findSingle("find obj("+objectClass.getName()+") where obj = ?", new Object[] { new Identifier(id) });
         if ( current == null )
         {
            logger.debug("object does not exists, so it's unchanged: "+id);
            return false; // Object does not yet exist globally
         }
         PersistenceMetaData persistenceMeta = objectTracker.getMetaData(current);
         if ( persistenceMeta.getPersistenceStart().longValue() <= serial.longValue() )
         {
            logger.debug("object is old, but database said it is current.");
            return false;
         } else {
            logger.debug("object is old, and database said it is also not current.");
            return true;
         }
      }
      // If there is an enty, but it has potentialTxSerial, that means
      // it was in a transaction, which started commiting, but never finished.
      // The client disconnected, and the locks were relinquished. In this
      // case, we must query the database to make sure the transaction commited.
      if ( entry.potentialTxSerial!=null )
      {
         // Query the object now
         Object current = queryService.findSingle("find obj("+objectClass.getName()+") where obj = ?",new Object[] { new Identifier(id) });
         if ( current == null )
         {
            logger.debug("unfinished transaction found, but object not found in database: "+id);
            return false; // Object does not yet exist globally
         }
         PersistenceMetaData persistenceMeta = objectTracker.getMetaData(current);
         // If the metadata's serial equals the potentialChangeSerial, then commit
         // the transaction manually.
         if ( persistenceMeta.getPersistenceStart().longValue() >= entry.potentialChangeSerial.longValue() )
         {
            logger.info("transaction commit not received for serial: "+entry.potentialTxSerial+", but transaction commited according to database.");
            // Transaction was committed successfully
            endTransaction(entry.potentialTxSerial);
         } else {
            logger.info("transaction commit not received for serial: "+entry.potentialTxSerial+", and transaction failed according to database.");
            // Originating transaction failed. Remove those entries which do
            // not have a lastChangeSerial, because those were not modified yet.
            List entries = (List) entriesByTxSerial.remove(entry.potentialTxSerial);
            for ( int i=0; (entries!=null) && (i<entries.size()); i++ )
            {
               ModificationEntry removeEntry = (ModificationEntry) entries.get(i);
               entriesById.remove(removeEntry.id);
               entriesByDate.remove(removeEntry);
               modifyClassEntries(removeEntry,false);
            }
         }
         // Now compare the query serial with the object start serial
         if ( persistenceMeta.getPersistenceStart().longValue() <= serial.longValue() )
         {
            logger.debug("unfinished transaction is resolved, current object did not change.");
            return false;
         }
      }
      // Default is to compare entry's last change serial with the query date.
      // If the lastChangeSerial is not yet present in the entry, that means
      // the modification already started, but not yet commited. This means it is
      // not modified.
      if ( (entry.lastChangeSerial==null) || (entry.lastChangeSerial.longValue() <= serial.longValue()) )
      {
         logger.debug("there was a change for that object in cache, but the object is newer. "+
               "Last change was on: "+entry.lastChangeSerial+", current object selected: "+serial);
         return false;
      }
      // Last default is changed
      logger.debug("object was not current, fallthrough return");
      return true;
   }

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
   public boolean isCurrent(Object obj)
   {
      return isCurrent(objectTracker.getMetaData(obj));
   }

   /**
    * Returns whether the given class changed since the given serial. A class changed,
    * if any objects of the given class were changed.
    */
   public boolean isCurrent(Class cl, Long serial)
   {
      logger.debug("making sure class "+cl+" is current at: "+serial);
      if ( serial == null )
         return true; // No serial given, this is always current
      // Make it a server call
      if ( nodeManager.getRole() == NodeManager.NodeRole.CLIENT )
      {
         return (Boolean) nodeManager.callServer(ClassTracker.class.getName(),
               "isCurrent",new Class[] { Class.class, long.class },
               new Object[] { cl, serial });
      }
      // Local
      synchronized ( operationTracker.getMutex() )
      {
         // We must check each modification
         // which concerns this class, whether they are newer than the given serial.
         Set entries = (Set) entriesByClass.get(cl);
         if ( entries != null )
         {
            // If the entries are not null, then check whether there was a valid
            // modification.
            Iterator entriesIterator = entries.iterator();
            while ( entriesIterator.hasNext() )
            {
               ModificationEntry entry = (ModificationEntry) entriesIterator.next();
               if ( hasChanged(entry.id,entry.objectClass,serial) )
               {
                  logger.debug("meta is a class, and there was an object of this class newer.");
                  return false; // Change of object is newer, so class changed
               }
            }
         }
         // There were no modifications registered by object changes, but
         // the class still could have changed before, so check change date.
         Long lastTableSerial = (Long) lastSerialsByClass.get(cl);
         if ( (lastTableSerial==null) || (lastTableSerial.longValue() < serial) )
         {
            logger.debug("class modification table says class is current.");
            return true;
         }
         // Fallback (not current).
         logger.debug("all checks failed, object is not current");
         return false;
      }
   }

   /**
    * Internal remote call for objects.
    */
   public boolean isCurrent(PersistenceMetaData meta)
   {
      // Make it a server call
      if ( nodeManager.getRole() == NodeManager.NodeRole.CLIENT )
      {
         return (Boolean) nodeManager.callServer(ClassTracker.class.getName(),
               "isCurrent",new Class[] { PersistenceMetaData.class },
               new Object[] { meta });
      }
      // If this is not a persisted object, return current
      if ( meta.getPersistenceId()==null )
         return true; 
      // Make checks for object
      synchronized ( operationTracker.getMutex() )
      {
         logger.debug("making sure meta: "+meta+" is current.");
         // If the startdate is not set, then object is not yet saved, it is current.
         if ( meta.getPersistenceStart()==null )
         {
            logger.debug("object is not yet persistent, so it is current.");
            return true;
         }
         // If the end serial is set, then the object is definitely not current
         // (it is deleted)! Implementation note: object is only deleted, if
         // the endserial is in the past, but we don't know the actual serial,
         // so the workaround is to see, if endserial is less then Long.MAX_VALUE,
         // assuming that there are no "future" deleted objects.
         if ( (meta.getPersistenceEnd()!=null) &&
           (meta.getPersistenceEnd().longValue() < Long.MAX_VALUE) )
         {
            logger.debug("object has enddate set, it will not be considered current.");
            return false;
         }
         // If the query date is not set, then we have no data to match to, 
         // return NOT current.
         if ( meta.getLastCurrentSerial() == null )
         {
            logger.debug("metadata contained no query serial, returning 'current'.");
            return false;
         }
         // Test whether entry indicates a change since the query of the meta we received
         if ( ! hasChanged(meta.getPersistenceId(),meta.getObjectClass(),meta.getLastCurrentSerial()) )
         {
            logger.debug("meta represents an object, and haschanged reports not changed.");
            return true;
         }
         // Fallback (not current).
         logger.debug("all checks failed, object is not current");
         return false;
      }
   }

   public class ModificationEntry implements Comparable
   {
      public Long id;
      public Class objectClass;
      public Date entryDate;
      public Long lastChangeSerial;
      public Long potentialChangeSerial;
      public Long potentialTxSerial;

      public int compareTo(Object entry)
      {
         return entryDate.compareTo(((ModificationEntry)entry).entryDate);
      }

      public int hashCode()
      {
         return id.hashCode();
      }

      public boolean equals(Object o)
      {
         return id==((ModificationEntry) o).id;
      }
   }

   public void configurationChanged(ConfigurationEvent event)
   {
      if ( (event.getPropertyName()!=null) && 
            (event.getPropertyName().startsWith("beankeeper.cache.modification_")) )
         configurationReload();
   }

   public void configurationReload()
   {
      MAX_ENTRY_AGE = configurationTracker.getConfiguration().
         getInt("beankeeper.cache.modification_max_age",30*60*1000);
      MAX_ENTRY_ITEMS = configurationTracker.getConfiguration().
         getInt("beankeeper.cache.modification_max_items",10000);
   }
}


