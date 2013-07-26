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
 * Map implementation with LazyList backing.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public class MapHandler extends ContainerHandler
{
   private TransactionTracker transactionTracker = null; // Injected
   private Database database = null; // Injected
   private ClassTracker classTracker = null; // Injected
   private SchemaManager schemaManager = null; // Injected

   public Class getContainerClass()
   {
      return MapImpl.class;
   }
   
   /**
    * Create the subtable.
    */
   public void ensureTableExists(ClassInfo parentInfo, String attributeName, boolean create)
   {
      Transaction tx = transactionTracker.getTransaction(TransactionTracker.TX_REQUIRED);
      tx.begin();
      try 
      {
         // Ensure map helper table
         HashMap mapAttributeTypes = new HashMap();
         mapAttributeTypes.put("persistence_id",Long.class);
         mapAttributeTypes.put("persistence_start",Long.class);
         mapAttributeTypes.put("persistence_end",Long.class);
         mapAttributeTypes.put("persistence_txstartid",Long.class);
         mapAttributeTypes.put("persistence_txstart",Long.class);
         mapAttributeTypes.put("persistence_txendid",Long.class);
         mapAttributeTypes.put("persistence_txend",Long.class);
         mapAttributeTypes.put("container_key",String.class);
         mapAttributeTypes.put("value",Long.class);
         ArrayList mapKeys = new ArrayList();
         mapKeys.add("persistence_id");
         mapKeys.add("persistence_txstart");
         mapKeys.add("container_key");
         database.ensureTable(
               tx,schemaManager.getTableName(
                  parentInfo.getAttributeClassEntry(attributeName),attributeName),
               mapAttributeTypes,mapKeys,create);
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
         WhereResolver.SymbolTableEntry previousEntry, ClassInfo previousInfo,
         ReferenceTerm previousTerm)
   {
      String attributeName = spec.getIdentifier();
      // Create entry
      WhereResolver.SymbolTableEntry entry = new WhereResolver.SymbolTableEntry();
      String alias = null;
      if ( spec.getKeyname() != null )
      {
         // If a specific map object was specified, then this is to
         // be remembered.
         alias=spec.getIdentifier()+"["+spec.getKeyname()+"]";
      }
      entry.specifiedTerm = new SpecifiedTableTerm(
            schemaManager.getTableName(
               previousInfo.getAttributeClassEntry(attributeName),attributeName),alias);
      entry.automatic = true;
      entry.type = WhereResolver.SymbolTableEntry.TYPE_HANDLED;
      entry.referenceColumn="value";
      // Create expression.
      Expression connectorExpression = new Expression();
      ReferenceTerm leftTerm1 = previousTerm;
      if ( previousEntry.automatic )
         previousEntry.termList.add(leftTerm1);
      ReferenceTerm rightTerm1 = new ReferenceTerm(entry.specifiedTerm,"persistence_id");
      entry.termList.add(rightTerm1);
      connectorExpression.add(leftTerm1);
      connectorExpression.add("=");
      connectorExpression.add(rightTerm1);
      // This subexpression is only important, if
      // there are more attributes to follow
      if ( (spec.getKeyname()!=null) && (!"".equals(spec.getKeyname())) )
      {
         connectorExpression.add("and");
         ReferenceTerm leftTerm2 = new ReferenceTerm(entry.specifiedTerm,"container_key");
         entry.termList.add(leftTerm2);
         ConstantTerm rightTerm2 = new ConstantTerm(spec.getKeyname());
         connectorExpression.add(leftTerm2);
         connectorExpression.add("=");
         connectorExpression.add(rightTerm2);
      }
      // Expression
      entry.expression=connectorExpression;
      // Return entry
      return entry;
   }

   /**
    * Determine the next class info after the given specifier.
    */
   public ClassInfo getSymbolInfo(WhereResolver.SymbolTableEntry entry,
         AttributeSpecifier spec)
      throws ParserException
   {
      if ( spec.getClassName() == null )
         throw new ParserException(ParserException.SYMBOL_ERROR,"class was not given after map reference");
      ClassInfo previousInfo = classTracker.getClassInfo(
            classTracker.getMatchingClassEntry(spec.getClassName()));
      if ( previousInfo == null )
         throw new ParserException(ParserException.SYMBOL_ERROR,"could not find class to map item classname: "+spec.getClassName());
      return previousInfo;
   }
}


