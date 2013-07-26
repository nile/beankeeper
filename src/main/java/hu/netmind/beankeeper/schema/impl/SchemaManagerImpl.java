/**
 * Copyright (C) 2010 NetMind Consulting Bt.
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

package hu.netmind.beankeeper.schema.impl;

import hu.netmind.beankeeper.schema.SchemaManager;
import hu.netmind.beankeeper.db.Database;
import hu.netmind.beankeeper.model.ClassTracker;
import hu.netmind.beankeeper.model.ClassInfo;
import hu.netmind.beankeeper.model.ClassEntry;
import hu.netmind.beankeeper.type.TypeHandlerTracker;
import hu.netmind.beankeeper.type.TypeHandler;
import hu.netmind.beankeeper.transaction.Transaction;
import hu.netmind.beankeeper.transaction.InternalTransactionTracker;
import hu.netmind.beankeeper.common.StoreException;
import hu.netmind.beankeeper.node.NodeManager;
import java.util.*;
import org.apache.log4j.Logger;

/**
 * Manages the relations between the object model
 * and the relational model.
 */
public class SchemaManagerImpl implements SchemaManager
{
   private static Logger logger = Logger.getLogger(SchemaManagerImpl.class);

   private Database database = null; // Injected
   private ClassTracker classTracker = null; // Injected
   private TypeHandlerTracker typeHandlerTracker = null; // Injected
   private InternalTransactionTracker transactionTracker = null; // Injected
   private NodeManager nodeManager = null; // Injected

   private Set<ClassEntry> ensuredEntries = new HashSet<ClassEntry>();
   private Set<ClassEntry> processingEntries = new HashSet<ClassEntry>();

   /**
    * Load the table names from database.
    */
   public void init(Map parameters)
   {
   }

   /**
    * Release all resources.
    */
   public void release()
   {
   }

   /**
    * Ensure that the specified entry is represented in the database
    * and synchronized with the Java model.
    */
   public synchronized void ensureSchema(ClassEntry entry)
   {
      if ( (entry == null) || (processingEntries.contains(entry)) )
         return;
      processingEntries.add(entry);
      try
      {
         // Determine if it needs to be ensured at all, or if it was before
         ClassInfo info = classTracker.getClassInfo(entry);
         if ( (ensuredEntries.contains(entry)) && (!info.hasChanged()) )
            return; // We have it and it didn't change
         // Mark it unensured
         ensuredEntries.remove(entry);
         // Make the info update itself to new structure
         info.update();
         // First, make sure superclasses are ensured
         ensureSchema(entry.getSuperEntry());
         // Make this call on sever but also remember locally
         // Check whether this class is entitled to a table.
         boolean create = info.isStorable();
         if ( nodeManager.getRole() == NodeManager.NodeRole.CLIENT )
         {
            // Call server
            nodeManager.callServer(SchemaManager.class.getName(),
                  "ensureSchema",new Class[] { ClassEntry.class }, new Object[] { entry });
            // We're local, so don't create
            create = false;
         }
         if ( logger.isDebugEnabled() )
            logger.debug("ensuring info: "+info+" (storable: "+create+"), attributes: "+info.getAttributeTypes(entry));
         // Ensure the table
         Transaction transaction = transactionTracker.getTransaction();
         transaction.begin();
         try
         {
            // Generate table schemas, one for class an also involve type handlers
            Iterator iterator = info.getAttributeNames(entry).iterator();
            HashMap attributeTypes = new HashMap();
            while ( iterator.hasNext() )
            {
               String attributeName = iterator.next().toString();
               Class type = info.getAttributeType(attributeName);
               switch ( classTracker.getType(type) )
               {
                  case TYPE_PRIMITIVE:
                     attributeTypes.put(attributeName,type);
                     break;
                  case TYPE_OBJECT:
                     attributeTypes.put(attributeName,Long.class);
                     break;
                  case TYPE_HANDLED:
                     TypeHandler handler = typeHandlerTracker.getHandler(type);
                     handler.ensureTableExists(info,attributeName,create);
                     attributeTypes.putAll(handler.getAttributeTypes(attributeName));
                     break;
                  default:
                     // Unknown type, leave it
                     logger.warn(entry+" has attribute '"+attributeName+"' of unhandled class: "+type);
                     break;
               }
            }
            // Remove reserved names
            Iterator typesIterator = attributeTypes.keySet().iterator();
            while ( typesIterator.hasNext() )
            {
               String name = (String) typesIterator.next();
               if ( name.startsWith("persistence") )
                  typesIterator.remove();
            }
            // Make object table
            ArrayList objectKeys = new ArrayList();
            objectKeys.add("persistence_id");
            objectKeys.add("persistence_txstart");
            attributeTypes.put("persistence_id",Long.class);
            attributeTypes.put("persistence_start",Long.class);
            attributeTypes.put("persistence_end",Long.class);
            attributeTypes.put("persistence_txstartid",Long.class);
            attributeTypes.put("persistence_txstart",Long.class);
            attributeTypes.put("persistence_txendid",Long.class);
            attributeTypes.put("persistence_txend",Long.class);
            logger.debug("creating table for entry: "+entry+", fields: "+attributeTypes+", keys: "+objectKeys);
            database.ensureTable(transaction,getTableName(entry),attributeTypes,objectKeys,create);
         } catch ( StoreException e ) {
            transaction.markRollbackOnly();
            throw e;
         } catch ( Throwable e ) {
            transaction.markRollbackOnly();
            throw new StoreException("unexpected error.",e);
         } finally {
            transaction.commit();
         }
         // Now if everything went ok, remember this entry as already ensured
         ensuredEntries.add(entry);
      } finally {
         processingEntries.remove(entry);
      }
   }

   /**
    * Get the table name for a given class entry.
    */
   public String getTableName(ClassEntry entry)
   {
      ensureSchema(entry);
      return entry.getFullName().replace("_","__").replace('.','_').toLowerCase();
   }

   /**
    * Get the table name for an attribute in an entry.
    * This can be used for attributes that need
    * a separate table for information, like many-to-many
    * relations, etc.
    */
   public String getTableName(ClassEntry entry, String attributeName)
   {
      return (getTableName(entry)+"_"+attributeName+"_").toLowerCase();
   }

   /**
    * Get a class entry for a table name.
    */
   public ClassEntry getClassEntry(String tableName)
   {
      return classTracker.getMatchingClassEntry(tableName.replace('_','.').replace("..","_"));
   }
}

