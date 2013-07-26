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

package hu.netmind.beankeeper.lock.impl;

import hu.netmind.beankeeper.service.StoreContext;
import org.apache.log4j.Logger;
import java.util.*;
import hu.netmind.beankeeper.common.StoreException;
import hu.netmind.beankeeper.event.PersistenceEventListener;
import hu.netmind.beankeeper.event.PersistenceEvent;
import hu.netmind.beankeeper.event.EventDispatcher;
import hu.netmind.beankeeper.node.event.RemoteStateChangeEvent;
import hu.netmind.beankeeper.lock.LockTracker;
import hu.netmind.beankeeper.lock.SessionInfoProvider;
import hu.netmind.beankeeper.lock.SessionInfo;
import hu.netmind.beankeeper.transaction.Transaction;
import hu.netmind.beankeeper.transaction.TransactionTracker;
import hu.netmind.beankeeper.object.PersistenceMetaData;
import hu.netmind.beankeeper.object.ObjectTracker;
import hu.netmind.beankeeper.node.NodeManager;
import hu.netmind.beankeeper.modification.ModificationTracker;

/**
 * This implementation of a lock tracker uses transaction to make a preliminary check
 * on the locks, then forwards calls to the central lock tracker.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public class LockTrackerImpl implements LockTracker, PersistenceEventListener
{
   private static Logger logger = Logger.getLogger(LockTrackerImpl.class);
   
   private SessionInfoProvider provider;
   private EventDispatcher eventDispatcher = null; // Inject
   private TransactionTracker transactionTracker = null; // Inject
   private ObjectTracker objectTracker = null; // Inject
   private NodeManager nodeManager = null; // Inject
   private ModificationTracker modificationTracker = null; // Injected

   public void init(Map parameters)
   {
      this.provider=new TransactionInfoProvider(transactionTracker);
      eventDispatcher.registerListener(this);
   }

   public void release()
   {
      eventDispatcher.unregisterListener(this);
   }

   public SessionInfoProvider getProvider()
   {
      return provider;
   }
   public void setProvider(SessionInfoProvider provider)
   {
      this.provider=provider;
   }

   /**
    * Lock a single object, and ensure that the object given is the
    * most recent version of the object.  The session information is gathered
    * from the session info provider.
    * @throws ConcurrentModificationException If the object is already
    * locked by another thread.
    */
   public void lockEnsureCurrent(Object obj)
   {
      lock(new Object[] { obj },null,-1, true, false);
   }

   /**
    * Lock a single object. The session information is gathered
    * from the session info provider.
    * @throws ConcurrentModificationException If the object is already
    * locked by another thread.
    */
   public void lock(Object obj)
   {
      lock(new Object[] { obj },null,-1, false, false);
   }

   /**
    * Lock a single object, and guarantee it's current. 
    * The session information is gathered from the session info provider.
    * @param obj The object to lock.
    * @param wait Wait the given amount of milliseconds for the lock to
    * free up. Method only throws ConcurrentModificationException if
    * the lock is not available in the given time.
    * @throws ConcurrentModificationException If the object is already
    * locked by another thread.
    */
   public void lockEnsureCurrent(Object obj,int wait)
   {
      lock(new Object[] { obj },null,wait, true, false);
   }

   /**
    * Lock a single object with wait period given. The session information is gathered
    * from the session info provider.
    * @param obj The object to lock.
    * @param wait Wait the given amount of milliseconds for the lock to
    * free up. Method only throws ConcurrentModificationException if
    * the lock is not available in the given time.
    * @throws ConcurrentModificationException If the object is already
    * locked by another thread.
    */
   public void lock(Object obj,int wait)
   {
      lock(new Object[] { obj },null,wait, false, false);
   }

   /**
    * Lock a single object with the session information given,
    * and check is the given object is the current version.
    * @throws ConcurrentModificationException If the object is already
    * locked by another thread.
    */
   public void lockEnsureCurrent(Object obj, SessionInfo info)
   {
      lock(new Object[] { obj }, info, -1, true, false);
   }
   
   /**
    * Lock a single object with the session information given.
    * @throws ConcurrentModificationException If the object is already
    * locked by another thread.
    */
   public void lock(Object obj, SessionInfo info)
   {
      lock(new Object[] { obj }, info, -1, false, false);
   }
   
   /**
    * Lock multiple objects, and check whether given objects
    * are current. The session information is gathered
    * from the session info provider. Use this method, if you want
    * to lock multiple objects at the same time.
    * @throws ConcurrentModificationException If the object is already
    * locked by another thread.
    */
   public void lockEnsureCurrent(Object[] objs)
   {
      lock(objs,null,-1, true, false);
   }
  
   /**
    * Lock multiple objects. The session information is gathered
    * from the session info provider. Use this method, if you want
    * to lock multiple objects at the same time.
    * @throws ConcurrentModificationException If the object is already
    * locked by another thread.
    */
   public void lock(Object[] objs)
   {
      lock(objs,null,-1, false, false);
   }
  
   /**
    * Lock multiple objects, and check whether given objects
    * are current. The session information is gathered
    * from the session info provider. Use this method, if you want
    * to lock multiple objects at the same time.
    * @param wait Wait the given amount of milliseconds for the lock to
    * free up. Method only throws ConcurrentModificationException if
    * the lock is not available in the given time.
    * @throws ConcurrentModificationException If the object is already
    * locked by another thread.
    */
   public void lockEnsureCurrent(Object[] objs, int wait)
   {
      lock(objs,null,wait,true, false);
   }
  
   /**
    * Lock multiple objects. The session information is gathered
    * from the session info provider. Use this method, if you want
    * to lock multiple objects at the same time.
    * @param wait Wait the given amount of milliseconds for the lock to
    * free up. Method only throws ConcurrentModificationException if
    * the lock is not available in the given time.
    * @throws ConcurrentModificationException If the object is already
    * locked by another thread.
    */
   public void lock(Object[] objs, int wait)
   {
      lock(objs,null,wait,false, false);
   }
  
   /**
    * Lock a single object read-only, and ensure that the object given is the
    * most recent version of the object.  The session information is gathered
    * from the session info provider.
    * @throws ConcurrentModificationException If the object is already
    * locked by another thread.
    */
   public void lockReadOnlyEnsureCurrent(Object obj)
   {
      lock(new Object[] { obj },null,-1, true, true);
   }

   /**
    * Lock a single object read-only. The session information is gathered
    * from the session info provider.
    * @throws ConcurrentModificationException If the object is already
    * locked by another thread.
    */
   public void lockReadOnly(Object obj)
   {
      lock(new Object[] { obj },null,-1, false, true);
   }

   /**
    * Lock a single object read-only, and guarantee it's current. 
    * The session information is gathered from the session info provider.
    * @param obj The object to lock.
    * @param wait Wait the given amount of milliseconds for the lock to
    * free up. Method only throws ConcurrentModificationException if
    * the lock is not available in the given time.
    * @throws ConcurrentModificationException If the object is already
    * locked by another thread.
    */
   public void lockReadOnlyEnsureCurrent(Object obj,int wait)
   {
      lock(new Object[] { obj },null,wait, true, true);
   }

   /**
    * Lock a single object read-only with wait period given. The session information is gathered
    * from the session info provider.
    * @param obj The object to lock.
    * @param wait Wait the given amount of milliseconds for the lock to
    * free up. Method only throws ConcurrentModificationException if
    * the lock is not available in the given time.
    * @throws ConcurrentModificationException If the object is already
    * locked by another thread.
    */
   public void lockReadOnly(Object obj,int wait)
   {
      lock(new Object[] { obj },null,wait, false, true);
   }

   /**
    * Lock a single object read-only with the session information given,
    * and check is the given object is the current version.
    * @throws ConcurrentModificationException If the object is already
    * locked by another thread.
    */
   public void lockReadOnlyEnsureCurrent(Object obj, SessionInfo info)
   {
      lock(new Object[] { obj }, info, -1, true, true);
   }
   
   /**
    * Lock a single object read-only with the session information given.
    * @throws ConcurrentModificationException If the object is already
    * locked by another thread.
    */
   public void lockReadOnly(Object obj, SessionInfo info)
   {
      lock(new Object[] { obj }, info, -1, false, true);
   }
   
   /**
    * Lock multiple objects read-only, and check whether given objects
    * are current. The session information is gathered
    * from the session info provider. Use this method, if you want
    * to lock multiple objects at the same time.
    * @throws ConcurrentModificationException If the object is already
    * locked by another thread.
    */
   public void lockReadOnlyEnsureCurrent(Object[] objs)
   {
      lock(objs,null,-1, true, true);
   }
  
   /**
    * Lock multiple objects read-only. The session information is gathered
    * from the session info provider. Use this method, if you want
    * to lock multiple objects at the same time.
    * @throws ConcurrentModificationException If the object is already
    * locked by another thread.
    */
   public void lockReadOnly(Object[] objs)
   {
      lock(objs,null,-1, false, true);
   }
  
   /**
    * Lock multiple objects read-only, and check whether given objects
    * are current. The session information is gathered
    * from the session info provider. Use this method, if you want
    * to lock multiple objects at the same time.
    * @param wait Wait the given amount of milliseconds for the lock to
    * free up. Method only throws ConcurrentModificationException if
    * the lock is not available in the given time.
    * @throws ConcurrentModificationException If the object is already
    * locked by another thread.
    */
   public void lockReadOnlyEnsureCurrent(Object[] objs, int wait)
   {
      lock(objs,null,wait,true, true);
   }
  
   /**
    * Lock multiple objects read-only. The session information is gathered
    * from the session info provider. Use this method, if you want
    * to lock multiple objects at the same time.
    * @param wait Wait the given amount of milliseconds for the lock to
    * free up. Method only throws ConcurrentModificationException if
    * the lock is not available in the given time.
    * @throws ConcurrentModificationException If the object is already
    * locked by another thread.
    */
   public void lockReadOnly(Object[] objs, int wait)
   {
      lock(objs,null,wait,false, true);
   }
  
   /**
    * Lock multiple objects with all possible parameters specified.
    * Use this method, if you want to lock multiple objects at the same time.
    * If classes are asked to be ensured to be current, the following date is
    * taken into account:
    * <ul>
    *    <li>If the lock is called from inside a transaction, the
    *    transaction's first operation's date is taken.</li>
    *    <li>If there are regular objects with this lock call, then
    *    the oldest object's read date is taken.</li>
    * </ul>
    * Whichever was earlier, the classes are checked against that date.
    * @param objs The object to lock simultaniously.
    * @param info The session info to memorize for this lock. This object will
    * be included in the ConcurrentModificationException.
    * @param wait Wait the given amount of milliseconds for the lock to
    * free up. Method only throws ConcurrentModificationException if
    * the lock is not available in the given time.
    * @param ensureCurrent Do the objects need to be guaranteed to be current.
    * If this flag is set, the lock operation will fail, if any supplied
    * object has a newer version.
    * @param readOnly Whether the lock should be read-only. If a lock is read-only, then
    * other read-only locks can still be established on the specified objects, read-write
    * locks will however fail.
    * @throws ConcurrentModificationException If the object is already
    * locked by another process.
    */
   public synchronized void lock(Object[] objs, SessionInfo info, int wait, boolean ensureCurrent, boolean readOnly)
   {
      if ( logger.isDebugEnabled() )
         logger.debug("locking "+objs.length+" objects, info: "+info+", wait: "+wait);
      // Allocate session info
      if ( info == null )
      {
         info = new SessionInfo(); // Empty
         try
         {
            if ( provider != null )
               info = provider.getSessionInfo();
         } catch ( Throwable e ) {
            logger.warn("could not allocate session info from provider",e);
         }
      }
      // If this lock operation is inside a transacion,
      // then the start of the transaction will be the
      // date the classes (tables) are ensure to be current.
      // So if inside a transaction a class is asked to be
      // ensured actual, then it's ensured that it's not
      // been tampered with since the beginning of the transacion.
      Transaction tx = transactionTracker.getTransaction(
            TransactionTracker.TX_OPTIONAL);
      Long firstSerial = null;
      long txSerial = 0;
      if ( tx != null )
      {
         firstSerial = tx.getSerial();
         logger.debug("there was a transaction, classes will be compared to at least: "+firstSerial);
         if ( tx.getSerial() != null )
            txSerial = tx.getSerial().longValue();
      }
      // Create metadata for all objects
      ArrayList remoteLockMetas = new ArrayList();
      for ( int i=0; i<objs.length; i++ )
      {
         LockMetaData meta = new LockMetaData();
         if ( objs[i] instanceof Class )
         {
            // Classes
            meta.setObjectClass((Class)objs[i]);
         } else {
            // Objects
            meta.setObjectClass(objs[i].getClass());
            PersistenceMetaData persistenceMeta = objectTracker.getMetaData(objs[i]);
            meta.setPersistenceId(persistenceMeta.getPersistenceId());
            meta.setLastCurrentSerial(persistenceMeta.getLastCurrentSerial());
            meta.setPersistenceStart(persistenceMeta.getPersistenceStart());
            meta.setPersistenceEnd(persistenceMeta.getPersistenceEnd());
            // Check if this query serial is older than the first date current
            if ( (meta.getLastCurrentSerial()!=null) && ((firstSerial==null) || (firstSerial.longValue() > meta.getLastCurrentSerial().longValue())) )
               firstSerial = meta.getLastCurrentSerial();
         }
         remoteLockMetas.add(meta);
      }
      // Re-set the oldest serial to the class-only metas
      for ( int i=0; i<remoteLockMetas.size(); i++ )
      {
         LockMetaData meta = (LockMetaData) remoteLockMetas.get(i);
         if ( meta.getPersistenceId() == null )
            meta.setLastCurrentSerial(firstSerial);
      }
      // Now make an ordered list of the remote lock objects. It should be
      // ordered, because that reduces the likelyhood of a deadlock.
      Collections.sort(remoteLockMetas);
      // Lock with central lock tracker
      SessionInfo oldInfo = lock(nodeManager.getId(),
            Thread.currentThread().getId(),txSerial,remoteLockMetas,info,wait,ensureCurrent,readOnly);
      if ( oldInfo != null )
         throw new hu.netmind.beankeeper.lock.ConcurrentModificationException(oldInfo,objs,"Tried to lock, but was already locked.");
      // If object could be locked, and it ensured that those objects
      // were current, then set the last known current serial to current
      if ( tx != null )
      {
         Long serial = tx.getSerial();
         for ( int i=0; i<objs.length; i++ )
         {
            if ( objs[i] instanceof Class )
               continue;
            objectTracker.updateCurrent(objs[i],serial);
         }
      }
      // All is good in lockworld
      logger.debug("successful lock operation.");
   }

   /**
    * Unlock a single object. If the object is not locked, nothing is done.
    */
   public void unlock(Object obj)
   {
      unlock(new Object[] { obj });
   }

   /**
    * Unlock multiple objects.
    */
   public synchronized void unlock(Object[] objs)
   {
      if ( logger.isDebugEnabled() )
         logger.debug("unlocking "+objs.length+" objects.");
      // The transaction if there is any
      Transaction tx = transactionTracker.getTransaction(
            TransactionTracker.TX_OPTIONAL);
      long txSerial = 0;
      if ( (tx!=null) && (tx.getSerial()!=null) )
         txSerial = tx.getSerial().longValue();
      // Go through all objects and create lock metadata
      ArrayList remoteLockMetas = new ArrayList();
      for ( int i=0; i<objs.length; i++ )
      {
         LockMetaData meta = new LockMetaData();
         if ( objs[i] instanceof Class )
         {
            // Classes
            meta.setObjectClass((Class)objs[i]);
         } else {
            // Objects
            meta.setObjectClass(objs[i].getClass());
            PersistenceMetaData persistenceMeta = objectTracker.getMetaData(objs[i]);
            meta.setPersistenceId(persistenceMeta.getPersistenceId());
         }
         remoteLockMetas.add(meta);
      }
      // Remote unlock
      try
      {
         unlock(nodeManager.getId(),
               Thread.currentThread().getId(),txSerial,remoteLockMetas);
      } catch ( Exception e ) {
         // Do not throw excetion here, because unlock only fails,
         // if connection is severed, in which case the server will
         // invalidate all locks either way.
         logger.warn("could not unlock remotely",e);
      }
   }
   
   /*
    * Remote part of the service.
    * Tracks remote locks. There are two views of tracked data: the index
    * of the node it came from, and the object's id itself. There is also
    * a transaction associated with the lock.
    */
   private HashMap lockEntriesByName;
   private HashMap lockEntriesByIndex;
   private HashMap indirectLockEntriesByClass;

   public LockTrackerImpl()
   {
      lockEntriesByName = new HashMap();
      lockEntriesByIndex = new HashMap();
      indirectLockEntriesByClass = new HashMap();
   }

   private void modifyIndirectEntries(LockEntry entry, boolean add)
   {
      modifyIndirectEntries(entry.objectClass,entry,add);
   }

   private void modifyIndirectEntries(Class currentClass, LockEntry entry, boolean add)
   {
      // If there is no class, then return
      if ( currentClass == null )
         return;
      // Mark class
      Set entries = (Set) indirectLockEntriesByClass.get(currentClass);
      if ( entries == null )
      {
         entries = new HashSet();
         indirectLockEntriesByClass.put(currentClass,entries);
      }
      if ( add )
      {
         // Add entry to indicate that it indirectly uses this class
         entries.add(entry);
      } else {
         // Remove that entry
         entries.remove(entry);
         if ( entries.size() == 0 )
            indirectLockEntriesByClass.remove(currentClass);
      }
      // Mark superclass
      modifyIndirectEntries(currentClass.getSuperclass(),entry,add);
      // Mark interfaces
      Class[] interfaces = currentClass.getInterfaces();
      for ( int i=0; i<interfaces.length; i++ )
         modifyIndirectEntries(interfaces[i],entry,add);
   }
   
   public void handle(PersistenceEvent event)
   {
      if ( event instanceof RemoteStateChangeEvent )
         unlockAll(((RemoteStateChangeEvent)event).getNodeId());
   }

   private synchronized void unlockAll(int index)
   {
      if ( logger.isDebugEnabled() )
         logger.debug("unlocking all from: "+index);
      Set entries = (Set) lockEntriesByIndex.remove(new Integer(index));
      if ( entries == null )
         return;
      Iterator entriesIterator = entries.iterator();
      while ( entriesIterator.hasNext() )
      {
         LockEntry entry = (LockEntry) entriesIterator.next();
         Set nameEntries = (Set) lockEntriesByName.get(entry.name);
         if ( nameEntries != null )
         {
            nameEntries.remove(entry);
            if ( nameEntries.isEmpty() )
               lockEntriesByName.remove(entry.name);
         }
         modifyIndirectEntries(entry,false);
      }
      // Notify waiting threads, that locks became unlocked.
      notifyAll();
   }

   /**
    * Wait a given amount of time for an unlock event.
    * @return The new wait interval that's left of the input wait period, or -1 if during
    * the wait period no unlock events were generated.
    */
   private int waitForUnlock(int wait)
   {
      if ( wait < 0 )
         throw new StoreException("wait called with negative wit period, this should not happen");
      // Start wait
      long startTime = System.currentTimeMillis();
      try
      {
         // Wait for unlock notification
         if ( wait > 0 )
            wait(wait); 
         else
            wait();
      } catch ( Throwable e ) {
         throw new StoreException("wait interrupted",e);
      }
      if ( wait > 0 )
      {
         long endTime = System.currentTimeMillis();
         wait -= (endTime-startTime);
         if ( wait <= 0 ) // Would mean infinite
            wait = -1; // Expired instead
      }
      // Return the modified time
      return wait;
   }

   /**
    * Get the lock entries for a class or any of it's superclasses or superinterfaces.
    */
   private Set getClassLockEntry(Class currentClass)
   {
      if ( currentClass == null )
         return null;
      logger.debug("checking lock entry for class: "+currentClass);
      // Check class
      Set result = new HashSet();
      Set entries = (Set) lockEntriesByName.get(currentClass.getName());
      if ( entries != null )
         result.addAll(entries);
      // Check superclass
      entries = getClassLockEntry(currentClass.getSuperclass());
      if ( entries != null )
         result.addAll(entries);
      // Check interfaces
      Class[] interfaces = currentClass.getInterfaces();
      for ( int i=0; i<interfaces.length; i++ )
      {
         entries = getClassLockEntry(interfaces[i]);
         if ( entries != null )
            result.addAll(entries);
      }
      // Fallback
      return result;
   }

   /**
    * Get the composite name for a lock metadata.
    */
   private String getEntryName(LockMetaData meta)
   {
      return meta.getObjectClass().getName()+(meta.getPersistenceId()==null?"":(":"+meta.getPersistenceId()));
   }

   private SessionInfo lock(int index, long threadId, long txSerial, LockMetaData meta, SessionInfo info, int wait, boolean ensureCurrent, boolean readOnly)
   {
      if ( logger.isDebugEnabled() )
         logger.debug("locking from: "+index+":"+threadId+":"+txSerial+", meta: "+meta+", info: "+info+", wait: "+wait+", ensure current: "+ensureCurrent);
      // Check whether object can have a lock engaged. To do this we require the
      // following checks _in a single interation_, because if we yield to another
      // thread, the situation may change, and every check has to be made again.
      // The checks:
      // - Check if lock is explicitly given using lockEntriesByName (if object)
      // - Check if lock explicitly set for any class or superclass. 
      // - Check whether class has implicit usage by other lock (if class).
      boolean allChecksOk = false;
      LockEntry ownedEntry = null;
      while ( ! allChecksOk )
      {
         LockEntry lockingEntry = null;
         // Check whether object is locked
         if ( meta.getPersistenceId() != null )
         {
            Set entries = (Set) lockEntriesByName.get(getEntryName(meta));
            if ( (entries!=null) && (!entries.isEmpty()) )
            {
               Iterator entriesIterator = entries.iterator();
               while ( (entriesIterator.hasNext()) && (lockingEntry==null) )
               {
                  LockEntry entry = (LockEntry) entriesIterator.next();
                  if ( (entry.index == index) && (entry.threadId==threadId) &&
                    ((entry.txSerial==0) || (txSerial==0) || (entry.txSerial==txSerial)) )
                  {
                     ownedEntry = entry;
                  } else {
                     if ( (!readOnly) || (!entry.readOnly) )
                     {
                        logger.debug("lock is present from another node or thread, and either that or this lock should be exclusive, lock unsuccessful.");
                        lockingEntry = entry;
                     }
                  }
               }
            } else {
               logger.debug("no lock for object is present.");
            }
         }
         // Check class and superclasses, whether they are locked
         if ( lockingEntry == null )
         {
            Set entries = getClassLockEntry(meta.getObjectClass());
            Iterator entriesIterator = entries.iterator();
            while ( (entriesIterator.hasNext()) && (lockingEntry==null) )
            {
               LockEntry entry = (LockEntry) entriesIterator.next();
               if ((! ( (entry.index == index) && (entry.threadId==threadId) &&
                    ((entry.txSerial==0) || (txSerial==0) || (entry.txSerial==txSerial)) ) ) &&
                   (!(entry.readOnly && readOnly)) )
                  lockingEntry = entry;
            }
         }
         // If this is a class, then check, if it's indirectly used by
         // already activated locks, which are not owned by this thread.
         if ( (lockingEntry==null) && (meta.getPersistenceId() == null) )
         {
            Set entries = (Set) indirectLockEntriesByClass.get(meta.getObjectClass());
            if ( entries != null )
            {
               Iterator entriesIterator = entries.iterator();
               while ( (lockingEntry==null) && (entriesIterator.hasNext()))
               {
                  LockEntry entry = (LockEntry) entriesIterator.next();
                  if ((! ( (entry.index == index) && (entry.threadId==threadId) &&
                        ((entry.txSerial==0) || (txSerial==0) || (entry.txSerial==txSerial)) ) ) &&
                      (!(entry.readOnly && readOnly)) )
                     lockingEntry = entry;
               }
            }
         }
         // If a locking entry is found, then try to wait
         if ( lockingEntry != null )
         {
            if ( wait < 0 )
            {
               // Tried to lock from another node or another thread in the same node
               logger.debug("object already locked: "+getEntryName(meta)+", from: "+lockingEntry.index+":"+lockingEntry.threadId+":"+lockingEntry.txSerial);
               return lockingEntry.info;
            } else {
               // Wait for an unlock event
               wait = waitForUnlock(wait);
            }
            allChecksOk = false;
         } else {
            allChecksOk = true;
         }
      }
      // The lock is ready to be engaged. Now we check (if necessary)
      // if the object is current. If it is not, we can still throw
      // an exception, because the lock is not yet inserted in the
      // datamodel. On the other hand, the object is surely not locked,
      // or owned by caller, so it's sure, there won't be any operations
      // on the object now, during this call.
      if ( ensureCurrent )
      {
         boolean current = false;
         if ( meta.getPersistenceId()==null )
         {
            // This means target is a class
            current = modificationTracker.isCurrent(
                  meta.getObjectClass(),meta.getLastCurrentSerial());
         } else {
            // Meta describes an object
            current = modificationTracker.isCurrent(meta);
         }
         if ( ! current )
         {
            logger.debug("object was not current, returning session");
            return new SessionInfo(); // Return empty session info
         }
      }
      // If entry is already present, then just adjust
      if ( ownedEntry != null )
      {
         if ( logger.isDebugEnabled() )
            logger.debug("there is already a lock for that object, but from the same node"+
                 " and thread, and no other locks (or just read-only locks) present, lock success.");
         // Lock comes from the same node, and from the same thread,
         // so it's the owner of the lock. Increase depth level.
         ownedEntry.lockDepth++;
         // Adjust readonly flag
         ownedEntry.readOnly &= readOnly;
         return null;
      }
      // Create lock
      LockEntry entry = new LockEntry();
      entry.id=meta.getPersistenceId();
      entry.objectClass=meta.getObjectClass();
      entry.name=getEntryName(meta);
      entry.index=index;
      entry.threadId=threadId;
      entry.txSerial=txSerial;
      entry.info=info;
      entry.lockDepth=1;
      entry.readOnly=readOnly;
      entry.lastModificationTime=System.currentTimeMillis();
      // Update data model
      Set entries = (Set) lockEntriesByIndex.get(new Integer(index));
      if ( entries == null )
      {
         entries = new HashSet();
         lockEntriesByIndex.put(new Integer(index),entries);
      }
      entries.add(entry);
      Set nameEntries = (Set) lockEntriesByName.get(getEntryName(meta));
      if ( nameEntries == null )
      {
         nameEntries = new HashSet();
         lockEntriesByName.put(getEntryName(meta),nameEntries);
      }
      nameEntries.add(entry);
      modifyIndirectEntries(entry,true);
      // Return
      logger.debug("new lock successfully created");
      return null;
   }

   private void unlock(int index, long threadId, long txSerial, LockMetaData meta)
   {
      if ( logger.isDebugEnabled() )
         logger.debug("unlocking from: "+index+":"+threadId+":"+txSerial+", name: "+getEntryName(meta));
      // Get the entry first. Note: owner is the same
      // if it's the same index and thread. Transactions do not matter here.
      Set nameEntries = (Set) lockEntriesByName.get(getEntryName(meta));
      LockEntry entry = null;
      if ( nameEntries != null )
      {
         Iterator nameEntriesIterator = nameEntries.iterator();
         while ( (nameEntriesIterator.hasNext()) && (entry==null) )
         {
            LockEntry tmp = (LockEntry) nameEntriesIterator.next();
            if ( (tmp.index==index) && (tmp.threadId==threadId) )
               entry=tmp;
         }
      }
      // Check if it's there
      if ( entry == null )
      {
         logger.warn("unlocking from: "+index+":"+threadId+":"+txSerial+", name: "+getEntryName(meta));
         logger.warn("no lock present, altough tried to unlock, possible unbalanced lock-unlock in code.");
         return; // No lock present
      }
      // Decrease lock depth
      entry.lockDepth--;
      if ( entry.lockDepth > 0 )
      {
         logger.debug("lock depth decreased to: "+entry.lockDepth);
         return; // No unlock yet
      }
      // Full remove of the entry
      Set entries = (Set) lockEntriesByIndex.get(new Integer(index));
      if ( entries == null )
         return;
      entries.remove(entry);
      if ( entries.size() == 0 )
         lockEntriesByIndex.remove(new Integer(index));
      nameEntries.remove(entry);
      if ( nameEntries.isEmpty() )
         lockEntriesByName.remove(getEntryName(meta));
      modifyIndirectEntries(entry,false);
      logger.debug("successful unlock operation");
   }

   /**
    * Lock given ids.
    */
   public synchronized SessionInfo lock(int index, long threadId, long txSerial, 
         List metas, SessionInfo info, int wait, boolean ensureCurrent, boolean readOnly)
   {
      // Make it a server call
      if ( nodeManager.getRole() == NodeManager.NodeRole.CLIENT )
      {
         logger.debug("lock on client, sending to server");
         return (SessionInfo) nodeManager.callServer(LockTracker.class.getName(),
               "lock",new Class[] { int.class, long.class, long.class, 
               List.class, SessionInfo.class, int.class, boolean.class, boolean.class },
               new Object[] { index, threadId, txSerial, metas, info, wait, ensureCurrent, readOnly });
      }
      // Local
      ArrayList lockedMetas = new ArrayList();
      // Go through all ids, and lock'em all
      SessionInfo result = null;
      boolean ended = false;
      try
      {
         for ( int i=0; (i<metas.size()) && (result==null); i++ )
         {
            LockMetaData meta = (LockMetaData) metas.get(i);
            long startTime = System.currentTimeMillis();
            result = lock(index,threadId,txSerial,meta,info,wait,ensureCurrent,readOnly);
            if ( wait > 0 )
            {
               long endTime = System.currentTimeMillis();
               wait -= (endTime-startTime);
               if ( wait <= 0 ) // Would mean infinite
                  wait = -1; // Expired instead
            }
            if ( result == null )
               lockedMetas.add(meta);
         }
         ended = true;
      } finally {
         if ( (!ended) || (result!=null) )
         {
            // Some error happened, or a lock could not be established,
            // so unlock all successfully locked ones, because either
            // all locks are established, or none of them when the method
            // returns.
            for ( int o=0; o<lockedMetas.size(); o++ )
               unlock(index, threadId, txSerial, (LockMetaData) lockedMetas.get(o) );
         }
      }
      return result;
   }

   /**
    * Unlock given ids.
    */
   public synchronized void unlock(int index, long threadId, long txSerial, List metas)
   {
      // Make it a server call
      if ( nodeManager.getRole() == NodeManager.NodeRole.CLIENT )
      {
         nodeManager.callServer(LockTracker.class.getName(),
               "unlock",new Class[] { int.class, long.class, long.class, List.class },
               new Object[] { index, threadId, txSerial, metas });
         return;
      }
      // Go through all ids, and unlock'em all
      for ( int i=0; i<metas.size(); i++ )
         unlock(index,threadId,txSerial,(LockMetaData) metas.get(i));
      // Notify waiting threads, that locks became unlocked.
      notifyAll();
   }


   public class LockEntry
   {
      public Long id;
      public String name;
      public Class objectClass;

      public long threadId;
      public long txSerial;
      public int index;

      public SessionInfo info;
      public long lastModificationTime;
      public int lockDepth;
      public boolean readOnly;
   }

}


