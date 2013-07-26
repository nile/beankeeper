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

package hu.netmind.beankeeper.lock;

import hu.netmind.beankeeper.service.Service;
import org.apache.log4j.Logger;
import java.util.*;

/**
 * This class tracks locks on objects. Any persistable object can be locked, just like
 * with the <code>synchronized</code> keyword in Java, and it does roughly the same thing
 * too. If an object is locked, no database operations can occur outside of the lock owner
 * transaction. All <code>Store</code> operations automatically try to lock the objects
 * they work with, so there can't be any concurrent modifications.<br>
 * You can also lock classes or interfaces. These equal to locking database tables, only they are 
 * hierarchical. That means, if you lock a class, all subclasses will also be locked 
 * automatically. For example, if you lock <code>Object.class</code> successfully, then
 * only the owner of that lock will be able to modify anything. Of course, a class can't be
 * locked, if there is another thread which holds lock on any super-, or sub-classes, or
 * any instances of this class, or any subclass.<br>
 * There are two flavors of locks: read-only locks and read-write locks. In short, read-only
 * locks prevent read-write locks to be established (and with it prevent 
 * <code>save()</code> and <code>remove()</code> calls). While read-write locks prevent both
 * other read-write locks and read-only locks too. Note, that using the <code>find()</code>
 * methods is prevented with none of the locks, since the finders always work correctly,
 * representing a consistent state at the moment when the <code>find()</code> is executed.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public interface LockTracker extends Service
{
   /**
    * Get the session provider of locks. A session provider is
    * a factory class that creates a session object for the locks.
    * A session info object is a map which holds data about the lock
    * session.
    */
   SessionInfoProvider getProvider();

   /**
    * Set the session info provider implementation.
    */
   void setProvider(SessionInfoProvider provider);

   /**
    * Lock a single object, and ensure that the object given is the
    * most recent version of the object.  The session information is gathered
    * from the session info provider.
    * @throws ConcurrentModificationException If the object is already
    * locked by another thread.
    */
   void lockEnsureCurrent(Object obj);

   /**
    * Lock a single object. The session information is gathered
    * from the session info provider.
    * @throws ConcurrentModificationException If the object is already
    * locked by another thread.
    */
   void lock(Object obj);

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
   void lockEnsureCurrent(Object obj,int wait);

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
   void lock(Object obj,int wait);

   /**
    * Lock a single object with the session information given,
    * and check is the given object is the current version.
    * @throws ConcurrentModificationException If the object is already
    * locked by another thread.
    */
   void lockEnsureCurrent(Object obj, SessionInfo info);
   
   /**
    * Lock a single object with the session information given.
    * @throws ConcurrentModificationException If the object is already
    * locked by another thread.
    */
   void lock(Object obj, SessionInfo info);
   
   /**
    * Lock multiple objects, and check whether given objects
    * are current. The session information is gathered
    * from the session info provider. Use this method, if you want
    * to lock multiple objects at the same time.
    * @throws ConcurrentModificationException If the object is already
    * locked by another thread.
    */
   void lockEnsureCurrent(Object[] objs);
  
   /**
    * Lock multiple objects. The session information is gathered
    * from the session info provider. Use this method, if you want
    * to lock multiple objects at the same time.
    * @throws ConcurrentModificationException If the object is already
    * locked by another thread.
    */
   void lock(Object[] objs);
  
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
   void lockEnsureCurrent(Object[] objs, int wait);
  
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
   void lock(Object[] objs, int wait);
  
   /**
    * Lock a single object read-only, and ensure that the object given is the
    * most recent version of the object.  The session information is gathered
    * from the session info provider.
    * @throws ConcurrentModificationException If the object is already
    * locked by another thread.
    */
   void lockReadOnlyEnsureCurrent(Object obj);

   /**
    * Lock a single object read-only. The session information is gathered
    * from the session info provider.
    * @throws ConcurrentModificationException If the object is already
    * locked by another thread.
    */
   void lockReadOnly(Object obj);

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
   void lockReadOnlyEnsureCurrent(Object obj,int wait);

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
   void lockReadOnly(Object obj,int wait);

   /**
    * Lock a single object read-only with the session information given,
    * and check is the given object is the current version.
    * @throws ConcurrentModificationException If the object is already
    * locked by another thread.
    */
   void lockReadOnlyEnsureCurrent(Object obj, SessionInfo info);
   
   /**
    * Lock a single object read-only with the session information given.
    * @throws ConcurrentModificationException If the object is already
    * locked by another thread.
    */
   void lockReadOnly(Object obj, SessionInfo info);
   
   /**
    * Lock multiple objects read-only, and check whether given objects
    * are current. The session information is gathered
    * from the session info provider. Use this method, if you want
    * to lock multiple objects at the same time.
    * @throws ConcurrentModificationException If the object is already
    * locked by another thread.
    */
   void lockReadOnlyEnsureCurrent(Object[] objs);
  
   /**
    * Lock multiple objects read-only. The session information is gathered
    * from the session info provider. Use this method, if you want
    * to lock multiple objects at the same time.
    * @throws ConcurrentModificationException If the object is already
    * locked by another thread.
    */
   void lockReadOnly(Object[] objs);
  
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
   void lockReadOnlyEnsureCurrent(Object[] objs, int wait);
  
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
   void lockReadOnly(Object[] objs, int wait);
  
   /**
    * Lock multiple objects with all possible parameters specified.
    * Use this method, if you want to lock multiple objects at the same time.
    * If classes are asked to be ensured to be current, the following date is
    * taken into account:
    * <ul>
    *    <li>If the lock is called from inside a transaction, the
    *    transaction's creation date is taken.</li>
    *    <li>If there are regular objects with this lock call, then
    *    the oldest object's read date is taken.</li>
    * </ul>
    * Whichever was earlier, the classes are checked against that date.
    * @param objs The objects to lock simultaniously.
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
   void lock(Object[] objs, SessionInfo info, int wait, boolean ensureCurrent, boolean readOnly);

   /**
    * Unlock a single object. If the object is not locked, nothing is done.
    */
   void unlock(Object obj);

   /**
    * Unlock multiple objects.
    */
   void unlock(Object[] objs);
}


