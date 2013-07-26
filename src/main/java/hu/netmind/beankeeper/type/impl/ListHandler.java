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
import hu.netmind.beankeeper.common.StoreException;
import hu.netmind.beankeeper.model.*;
import hu.netmind.beankeeper.transaction.Transaction;
import hu.netmind.beankeeper.transaction.TransactionTracker;
import hu.netmind.beankeeper.db.Database;
import hu.netmind.beankeeper.schema.SchemaManager;

/**
 * List handler implementation.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public class ListHandler extends CollectionHandler
{
   private TransactionTracker transactionTracker = null; // Injected
   private Database database = null; // Injected
   private SchemaManager schemaManager = null; // Injected

   /**
    * Constructor.
    */
   public ListHandler(Class collectionClass)
   {
      super(collectionClass);
   }

   /**
    * Create the subtable for the list.
    */
   public void ensureTableExists(ClassInfo parentInfo, String attributeName, boolean create)
   {
      Transaction tx = transactionTracker.getTransaction(TransactionTracker.TX_REQUIRED);
      tx.begin();
      try 
      {
         // Ensure map helper table
         HashMap listAttributeTypes = new HashMap();
         listAttributeTypes.put("persistence_id",Long.class);
         listAttributeTypes.put("persistence_start",Long.class);
         listAttributeTypes.put("persistence_end",Long.class);
         listAttributeTypes.put("persistence_txstartid",Long.class);
         listAttributeTypes.put("persistence_txstart",Long.class);
         listAttributeTypes.put("persistence_txendid",Long.class);
         listAttributeTypes.put("persistence_txend",Long.class);
         listAttributeTypes.put("value",Long.class);
         listAttributeTypes.put("container_index",Long.class);
         ArrayList listKeys = new ArrayList();
         listKeys.add("persistence_id");
         listKeys.add("persistence_txstart");
         listKeys.add("container_index");
         database.ensureTable(
               tx,schemaManager.getTableName(
                  parentInfo.getAttributeClassEntry(attributeName),attributeName),
               listAttributeTypes,listKeys,create);
      } catch ( StoreException e ) {
         tx.markRollbackOnly();
         throw e;
      } catch ( Throwable e ) {
         tx.markRollbackOnly();
         throw new StoreException("Unknown exception",e);
      } finally {
         tx.commit();
      }
   }

}


