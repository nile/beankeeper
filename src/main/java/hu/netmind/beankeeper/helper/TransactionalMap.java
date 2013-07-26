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

package hu.netmind.beankeeper.helper;

import hu.netmind.beankeeper.Store;
import hu.netmind.beankeeper.transaction.Transaction;
import hu.netmind.beankeeper.transaction.TransactionTracker;
import hu.netmind.beankeeper.event.PersistenceEventListener;
import hu.netmind.beankeeper.event.PersistenceEvent;
import hu.netmind.beankeeper.transaction.event.TransactionEvent;
import hu.netmind.beankeeper.transaction.event.TransactionCommittedEvent;
import hu.netmind.beankeeper.transaction.event.TransactionRolledbackEvent;
import java.util.Map;
import java.util.Set;
import java.util.Collection;
import java.util.HashMap;
import java.util.ConcurrentModificationException;
import org.apache.log4j.Logger;

/**
 * This map implementation takes transactions into account when
 * getting and settings values. This means, that values set into
 * the map are only visible to the same transaction which set them,
 * until the commit, in which case the value becomes visible to all.
 * If the transaction rolled back, the set values are simply dropped.
 * Be sure to close this map if you don't use it.
 * <br><i>Note: </i>This implementation is not thread-safe, and supports
 * modifications from only one transaction at any given time.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public class TransactionalMap implements Map, PersistenceEventListener
{
   private Store store;
   private Map globalMap;
   private Map transactionMap;
   private Transaction modifier;

   /**
    * Construct a transactional map. This map listens for persistence
    * events, and commits or rolls back internal operations according
    * to transaction events.
    */
   public TransactionalMap(Store store)
   {
      globalMap = new HashMap();
      transactionMap = new HashMap();
      modifier = null; // No modifier
      this.store=store;
      // Note: this is not nice to do, registration should
      // be outside constructor.
      store.getEventDispatcher().registerListener(this);
   }

   /**
    * Get the map for the current transaction for 
    * reading.
    */
   private Map getReadMap()
   {
      if ( modifier == null )
         return globalMap;
      return transactionMap;
   }

   /**
    * Get map for current transaction for writing.
    */
   private Map getWriteMap()
   {
      Transaction transaction = store.getTransactionTracker().getTransaction(TransactionTracker.TX_OPTIONAL);
      if ( (transaction != modifier) && (modifier!=null) )
         throw new ConcurrentModificationException("map is modified from two different transactions, current: "+
               transaction+", but already under modification from: "+modifier);
      if ( transaction == null )
      {
         // There is no transaction, so modify global map
         return globalMap;
      } else {
         if ( modifier == null )
         {
            // Modification starts with this operations, so prepare
            modifier = transaction;
            transactionMap = new HashMap(globalMap);
         }
         return transactionMap;
      }
   }

   public void clear()
   {
      getWriteMap().clear();
   }

   public boolean containsKey(Object key)
   {
      return getReadMap().containsKey(key);
   }

   public boolean containsValue(Object value)
   {
      return getReadMap().containsValue(value);
   }

   public Set entrySet()
   {
      return getReadMap().entrySet();
   }

   public boolean equals(Object o)
   {
      return getReadMap().equals(o);
   }

   public Object get(Object key)
   {
      return getReadMap().get(key);
   }

   public int hashCode()
   {
      return getReadMap().hashCode();
   }

   public boolean isEmpty()
   {
      return getReadMap().isEmpty();
   }

   public Set keySet()
   {
      return getReadMap().keySet();
   }

   public Object put(Object key, Object value)
   {
      return getWriteMap().put(key,value);
   }

   public void putAll(Map m)
   {
      getWriteMap().putAll(m);
   }

   public Object remove(Object key)
   {
      return getWriteMap().remove(key);
   }

   public int size()
   {
      return getReadMap().size();
   }

   public Collection values()
   {
      return getReadMap().values();
   }

   /**
    * Close this map, deregister from transaction listener.
    */
   public void close()
   {
      store.getEventDispatcher().unregisterListener(this);
   }

   /**
    * Listen for transaction events.
    */
   public void handle(PersistenceEvent event)
   {
      if ( ! (event instanceof TransactionEvent) )
         return; // Quick exit
      Transaction transaction = ((TransactionEvent) event).getTransaction();
      if ( transaction != modifier )
         return; // Not our transaction
      if ( event instanceof TransactionCommittedEvent )
      {
         globalMap = transactionMap;
         modifier=null;
      }
      if ( event instanceof TransactionRolledbackEvent )
      {
         modifier=null;
      }
   }

}



