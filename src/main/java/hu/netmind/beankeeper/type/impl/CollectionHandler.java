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
 * Collection handler implementation.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public class CollectionHandler extends ContainerHandler
{
   private Class collectionClass;
   private TransactionTracker transactionTracker = null; // Injected
   private Database database = null; // Injected
   private SchemaManager schemaManager = null; // Injected
   
   /**
    * Constructor.
    */
   public CollectionHandler(Class collectionClass)
   {
      this.collectionClass=collectionClass;
   }
   
   public Class getContainerClass()
   {
      return collectionClass;
   }
   
   /**
    * Create the subtable for the collection.
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
         ArrayList listKeys = new ArrayList();
         listKeys.add("persistence_id");
         listKeys.add("persistence_txstart");
         listKeys.add("value");
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

   /**
    * Create the approriate symbol entry when parsing a query.
    */
   public WhereResolver.SymbolTableEntry getSymbolEntry(AttributeSpecifier spec,
         WhereResolver.SymbolTableEntry previousEntry, ClassInfo previousInfo, ReferenceTerm previousTerm)
   {
      String attributeName = spec.getIdentifier();
      // Create entry
      WhereResolver.SymbolTableEntry entry = new WhereResolver.SymbolTableEntry();
      entry.specifiedTerm = new SpecifiedTableTerm(
            schemaManager.getTableName(
               previousInfo.getAttributeClassEntry(attributeName),attributeName), null);
      entry.automatic = true;
      entry.referenceColumn="value";
      entry.type = WhereResolver.SymbolTableEntry.TYPE_HANDLED;
      // Create expression
      Expression connectorExpression = new Expression();
      ReferenceTerm leftTerm1 = previousTerm;
      if ( previousEntry.automatic )
         previousEntry.termList.add(leftTerm1);
      connectorExpression.add(leftTerm1);
      connectorExpression.add("=");
      ReferenceTerm rightTerm1 = new ReferenceTerm(entry.specifiedTerm,"persistence_id");
      entry.termList.add(rightTerm1);
      connectorExpression.add(rightTerm1);
      entry.expression=connectorExpression;
      // Return
      return entry;
   }

   /**
    * Determine the next class info after the given specifier.
    */
   public ClassInfo getSymbolInfo(WhereResolver.SymbolTableEntry entry,
         AttributeSpecifier spec)
      throws ParserException
   {
      // A list should be the last item, so there should be
      // no next specifier.
      throw new ParserException(ParserException.ABORT,"list type found, and is not the last item, but it should");
   }
}


