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

import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.ArrayList;
import org.apache.log4j.Logger;
import hu.netmind.beankeeper.logging.SnapshotLogger;

/**
 * This is exactly like weak hashmap, but this implementation disregards
 * the object's <code>equals()</code> and <code>hashCode()</code> methods,
 * and uses object equality for testing. This means it will only return
 * an entry, if the object given as key exactly matches the key in the map.
 * This is not so trivial, since the <code>hashCode()</code> need not to be 
 * unique between objects, and we can't refer to the object directly
 * either, because then this wouldn't be a weak map.<br>
 * <i>Note</i>: Ok, this is not an implementation of Map, if you wish
 * you can write the necessary methods.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public class WeakMap
{
   private static Logger logger = Logger.getLogger(WeakMap.class);
   
   private Map objectMap;
   private Map referenceMap;
   private ReferenceQueue queue;
   private WeakMapListener listener = null;
   private SnapshotLogger snapshotLogger = null;

   public WeakMap(SnapshotLogger snapshotLogger)
   {
      this.snapshotLogger=snapshotLogger;
      objectMap = new HashMap();
      referenceMap = new HashMap();
      queue = new ReferenceQueue();
   }

   public void setListener(WeakMapListener listener)
   {
      this.listener=listener;
   }
   public WeakMapListener getListener()
   {
      return listener;
   }
   
   /**
    * Put an object with a key to the map.
    * @param key The object which will be referred weakly, may not be null.
    * @param value The value to the key.
    * @param id Some identifier which does not refer to the key.
    */
   public void put(Object key, Object value, Object id)
   {
      Integer hashCode = new Integer(System.identityHashCode(key));
      // Clear some entries
      clear();
      // Make entries
      WeakReference ref = new WeakReference(key,queue);
      List entries = (List) objectMap.get(hashCode);
      if ( entries == null )
      {
         entries = new ArrayList();
         objectMap.put(hashCode,entries);
      }
      entries.add(new Entry(ref,value,id));
      referenceMap.put(ref,hashCode);
   }

   /**
    * Clear obsolete entries.
    */
   private void clear()
   {
      // Clear obsolate entries
      Reference ref = null;
      while ( (ref=queue.poll()) != null )
      {
         // Clear that ref's entries
         Integer hashCode = (Integer) referenceMap.remove(ref);
         List entries = (List) objectMap.get(hashCode);
         if ( entries != null )
         {
            // We need to find the entry and clear it
            Iterator iterator = entries.iterator();
            boolean deleted = false;
            while ( (iterator.hasNext()) && (!deleted) )
            {
               Entry entry = (Entry) iterator.next();
               if ( entry.getReference() == ref )
               {
                  iterator.remove();
                  deleted=true;
                  // Notify
                  if ( getListener() != null )
                     getListener().notifyValueLeave(entry.getId());
               }
            }
         }
         if ( entries.size() == 0 )
            objectMap.remove(hashCode);
      }
      // Profile
      snapshotLogger.log("weakmap","Current map sizes: "+objectMap.size()+","+referenceMap.size());
   }

   /**
    * Get a value for the given key.
    * @param key The key object.
    * @return The value for exatcly the given object instance.
    */
   public Object get(Object key)
   {
      if ( key == null )
         return null;
      // Clear some entries
      clear();
      // After all entries are cleared it is still possible that now the gc
      // run and objects possibly became gc'd. This is not a problem,
      // since we're going to use '==' operator. == never lies.
      Integer hashCode = new Integer(System.identityHashCode(key));
      List entries = (List) objectMap.get(hashCode);
      if ( entries == null )
      {
         logger.debug("weak map did not find object of hashcode: "+hashCode+". Number of codes in map: "+objectMap.size());
         return null;
      }
      for ( int i=0; i<entries.size(); i++ )
      {
         Entry entry = (Entry) entries.get(i);
         // This is the thing that's making sure the key fits.
         // We get the referenced object and see if it is ==. Note,
         // that the referenced object may be gc'd by this time, it does not
         // matter, it will be null, our key is not.
         if ( entry.getReference().get() == key )
            return entry.getValue();
      }
      logger.debug("weak map did not find object with equality, altough there were "+entries.size()+" objects of same identity hash code: "+hashCode);
      return null;
   }

   public class Entry
   {
      private Reference ref;
      private Object value;
      private Object id;
      
      public Entry(Reference ref, Object value, Object id)
      {
         this.ref=ref;
         this.value=value;
         this.id=id;
      }
      public Reference getReference()
      {
         return ref;
      }
      public Object getValue()
      {
         return value;
      }
      public Object getId()
      {
         return id;
      }
   }
}


