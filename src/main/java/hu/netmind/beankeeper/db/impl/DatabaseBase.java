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

package hu.netmind.beankeeper.db.impl;

import java.util.*;
import java.io.*;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import org.apache.log4j.Logger;
import hu.netmind.beankeeper.service.StoreContext;
import hu.netmind.beankeeper.parser.*;
import hu.netmind.beankeeper.common.StoreException;
import hu.netmind.beankeeper.transaction.*;
import hu.netmind.beankeeper.transaction.event.TransactionEvent;
import hu.netmind.beankeeper.transaction.event.TransactionCommittedEvent;
import hu.netmind.beankeeper.transaction.event.TransactionRolledbackEvent;
import hu.netmind.beankeeper.db.*;
import hu.netmind.beankeeper.management.ManagementTracker;
import hu.netmind.beankeeper.event.EventDispatcher;
import hu.netmind.beankeeper.event.PersistenceEventListener;
import hu.netmind.beankeeper.event.PersistenceEvent;

/**
 * This is a database superclass offers basic functions that
 * will need to be addressed in every database implementation.
 * The following tasks are currently handled by this superclass:<br>
 * <ul>
 *    <li>Transaction handling. Implementations only receive connections
 *    from here on.</li>
 *    <li>Table name handling. All table names are transformed to match
 *    the maximum length supported by database software. Implementations
 *    are guaranteed to receive only good table names.</li>
 *    <li>Table column name handling. All attribute names are transformed
 *    to suitable column names. Reserved words will be escaped.</li>
 *    <li>Keeping track of transaction statistics.</li>
 * </ul>
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public abstract class DatabaseBase implements PersistenceEventListener, Database
{
   private static Logger logger = Logger.getLogger(DatabaseBase.class);
   
   private Map reservedWords; // Reserved words of database
   private Map reverseReservedWords; // Reverse of translated words
   
   private ConnectionSource connectionSource;
   private int maxTableNameLength;

   private Object tableNameMutex = new Object(); // Mutex for accessing table names
   private Map tableNames; // Contains alias->realname mappings
   private Map transactionNames; // Contains mapping for specific transaction

   private SQLStatistics sqlStatistics = null;
   private EventDispatcher eventDispatcher = null; // Injected
   private ManagementTracker managementTracker = null; // Injected

   /**
    * Initialize this implementation.
    */
   public void init(Map parameters)
   {
      connectionSource=(ConnectionSource) parameters.get(
            StoreContext.PARAM_CONNECTIONSOURCE);
      // Init reserved words
      readReservedWords();
      // Register listener
      eventDispatcher.registerListener(this);
      // Create database table name mappings, or read them, if 
      // they exist
      Connection connection = null;
      try
      {
         // Determine max lengths
         connection = connectionSource.getConnection();
         DatabaseMetaData dmd = connection.getMetaData();
         maxTableNameLength = dmd.getMaxTableNameLength();
         logger.debug("database says it can handle "+maxTableNameLength+" character table names.");
         if ( maxTableNameLength == 0 )
            maxTableNameLength = Integer.MAX_VALUE;
         if ( maxTableNameLength < 10 )
            throw new StoreException("database can't handle 10 charachter length table names (only "+maxTableNameLength+"). Must be Oracle or something.");
      } catch ( StoreException e ) {
         throw e;
      } catch ( Exception e ) {
         throw new StoreException("database table name mapping table could not be created.",e);
      } finally {
         if ( connection != null )
            connectionSource.releaseConnection(connection);
      }
      // Create and register mbean
      sqlStatistics = new SQLStatistics();
      managementTracker.registerBean("SQLStatistics",sqlStatistics);
   }

   /**
    * Release all resources.
    */
   public void release()
   {
      logger.debug("releasing all connections...");
      eventDispatcher.unregisterListener(this);
      connectionSource.release();
      managementTracker.deregisterBean("SQLStatistics");
   }

   /**
    * Get the connection source of this database.
    */
   public ConnectionSource getConnectionSource()
   {
      return connectionSource;
   }

   /**
    * Modifies an object already in database with given fields.
    * @param tableName The table to save attributes to.
    * @param keys The keys of object to save (All object entries have keys).
    * @param attributes The attributes in form of name:value pairs.
    */
   public void save(Transaction transaction, String tableName, 
         Map keys, Map attributes)
   {
      if ( attributes.size() != 0 )
      {
         TransactionStatistics stats=save(transaction.getConnection(), 
               transformTableName(transaction,tableName), transformAttributes(keys), transformAttributes(attributes));
         transaction.getStats().add(stats);
         sqlStatistics.add(stats); // To accumulated sql stats
      }
   }

   /**
    * Insert an object into the database.
    * @param tableName The table to save attributes to.
    * @param id The id of object to save (All object entries have an id).
    * @param attributes The attributes in form of name:value pairs.
    */
   public void insert(Transaction transaction, String tableName, 
         Map attributes)
   {
      if ( attributes.size() != 0 )
      {
         TransactionStatistics stats=insert(transaction.getConnection(), 
               transformTableName(transaction,tableName), transformAttributes(attributes));
         transaction.getStats().add(stats);
         sqlStatistics.add(stats); // To accumulated sql stats
      }
   }

   /**
    * Remove an entry from database.
    * @param tableName The table to remove object from.
    * @param attributes The attributes which identify the object.
    * Equality is assumed with each attribute and it's value.
    */
   public void remove(Transaction transaction, String tableName,
         Map attributes)
   {
      TransactionStatistics stats=remove(transaction.getConnection(), 
            transformTableName(transaction,tableName), transformAttributes(attributes));
      transaction.getStats().add(stats);
      sqlStatistics.add(stats); // To accumulated sql stats
   }

   /**
    * Get the reverse translation of a column name.
    */
   private String reverseName(String name)
   {
      // If empty, nop
      if ( name == null )
         return null;
      name = name.toLowerCase();
      // If it's in the reverse map, return reverse name
      String result = (String) reverseReservedWords.get(name);
      if ( result != null )
         return result;
      // Fall-through
      return name;
   }

   /**
    * Translate name of a column in given table.
    */
   private String translateName(String name)
   {
      // If name is empty, don't do anything
      if ( name == null )
         return null;
      name = name.toLowerCase();
      // If name is in the reverse map, than this name
      // should not be used, because it would make the
      // given reverse name not unique
      if ( name.endsWith("_underscore") )
         throw new StoreException("can not use the field name: "+name+", it is reserved for translating names");
      // Translate name, if it's in the reserved words list
      String result = (String) reservedWords.get(name);
      if ( result != null )
         return result;
      // Fall-through, return original string
      return name;
   }

   /**
    * Ensure that table exists in database.
    * @param tableName The table to check.
    * @param attributeTypes The attribute names together with which
    * java class they should hold.
    * @param create If true, create table physically, if false, only
    * update internal representations, but do not create table.
    */
   public void ensureTable(Transaction transaction, String tableName,
         Map attributeTypes, List keyAttributeNames, boolean create)
   {
      TransactionStatistics stats=ensureTable(transaction.getConnection(), 
            transformTableName(transaction,tableName), transformAttributes(attributeTypes), transformAttributes(keyAttributeNames),create);
      transaction.getStats().add(stats);
      sqlStatistics.add(stats); // To accumulated sql stats
   }

   /**
    * Select objects from database as ordered list of attribute maps.
    * @param transaction The transaction to run in.
    * @param stmt The query statement.
    * @param limits The limits of the result. (Offset, maximum result count)
    * @return The result object.
    */
   public SearchResult search(Transaction transaction, 
         QueryStatement stmt, Limits limits)
   {
      QueryStatement newStmt = new QueryStatement(stmt);
      newStmt.setSpecifiedTerms(new HashSet(replaceTableNames(transaction,newStmt.getSpecifiedTerms())));
      newStmt.setQueryExpression(replaceTableNames(transaction,newStmt.getQueryExpression()));
      newStmt.setSelectTerms(replaceTableNames(transaction,newStmt.getSelectTerms()));
      newStmt.setOrderByList(replaceOrderTableNames(transaction,newStmt.getOrderByList()));
      // Run query
      SearchResult rawResult = new SearchResult();
      TransactionStatistics stats = search(transaction.getConnection(), 
            newStmt, limits, rawResult);
      transaction.getStats().add(stats);
      sqlStatistics.add(stats); // To accumulated sql stats
      // Transform result. As the names of the select terms were altered,
      // go through the original select terms and populate a new map based
      // on the original names.
      SearchResult result = new SearchResult();
      result.setResultSize(rawResult.getResultSize());
      List<Map> rawResultList = rawResult.getResult();
      List<Map> resultList = new ArrayList<Map>();
      for ( Map rawResultMap : rawResultList )
      {
         Map resultMap = new HashMap();
         Set<Map.Entry<String,Object>> rawResultMapEntries = rawResultMap.entrySet();
         for ( Map.Entry<String,Object> rawResultEntry : rawResultMapEntries )
            resultMap.put(reverseName(rawResultEntry.getKey()),rawResultEntry.getValue());
         resultList.add(resultMap);
         if ( logger.isTraceEnabled() )
            logger.trace("transforming result: "+rawResultMap+", into: "+resultMap);
      }
      result.setResult(resultList);
      // Return result transformed
      return result;
   }

   /**
    * Replace all table names in the list of terms.
    */
   private List replaceTableNames(Transaction transaction, Collection tableTerms)
   {
      List result = new ArrayList();
      Iterator iterator = tableTerms.iterator();
      while ( iterator.hasNext() )
      {
         TableTerm term = (TableTerm) iterator.next();
         result.add(replaceTableName(transaction,term));
      }
      return result;
   }

   /**
    * Replace the term with a translated term.
    */
   private TableTerm replaceTableName(Transaction transaction, TableTerm term)
   {
      TableTerm newTerm = null;
      if ( term instanceof ReferenceTerm )
      {
         newTerm = new ReferenceTerm((ReferenceTerm)term);
         ((ReferenceTerm)newTerm).setColumnName(translateName(((ReferenceTerm)term).getColumnName()));
      } else if ( term instanceof SpecifiedTableTerm ) {
         SpecifiedTableTerm specifiedTerm = (SpecifiedTableTerm) term;
         SpecifiedTableTerm specifiedNewTerm = new SpecifiedTableTerm(term);
         specifiedNewTerm.setReferencedLeftTerms(new ArrayList());
         specifiedNewTerm.setRelatedLeftTerms(new ArrayList());
         newTerm=specifiedNewTerm;
         // Recursively replace left terms
         for ( int i=0; (specifiedTerm.getRelatedLeftTerms()!=null) && 
               (i<specifiedTerm.getRelatedLeftTerms().size()); i++ )
         {
            SpecifiedTableTerm.LeftjoinEntry entry = (SpecifiedTableTerm.LeftjoinEntry) 
               specifiedTerm.getRelatedLeftTerms().get(i);
            SpecifiedTableTerm.LeftjoinEntry newEntry = new SpecifiedTableTerm.LeftjoinEntry();
            newEntry.term = replaceTableName(transaction,entry.term);
            newEntry.expression = replaceTableNames(transaction,entry.expression);
            specifiedNewTerm.getRelatedLeftTerms().add(newEntry);
         }
         for ( int i=0; (specifiedTerm.getReferencedLeftTerms()!=null) && 
               (i<specifiedTerm.getReferencedLeftTerms().size()); i++ )
         {
            SpecifiedTableTerm.LeftjoinEntry entry = (SpecifiedTableTerm.LeftjoinEntry) 
               specifiedTerm.getReferencedLeftTerms().get(i);
            SpecifiedTableTerm.LeftjoinEntry newEntry = new SpecifiedTableTerm.LeftjoinEntry();
            newEntry.term = replaceTableName(transaction,entry.term);
            newEntry.expression = replaceTableNames(transaction,entry.expression);
            specifiedNewTerm.getReferencedLeftTerms().add(newEntry);
         }
      } else {
         newTerm = new TableTerm(term);
      }
      newTerm.setTableName(transformTableName(transaction,term.getTableName()));
      newTerm.setAlias(translateName(newTerm.getAlias()));
      // Return with translated term
      return newTerm;
   }
   
   /**
    * Replace all tables names in order by statement.
    */
   private List replaceOrderTableNames(Transaction transaction, List orderbys)
   {
      if ( orderbys == null )
         return null;
      ArrayList result = new ArrayList();
      for ( int i=0; i<orderbys.size(); i++ )
      {
         OrderBy orderby = (OrderBy) orderbys.get(i);
         ReferenceTerm refTerm = (ReferenceTerm) orderby.getReferenceTerm();
         result.add(new OrderBy(new ReferenceTerm(
                  transformTableName(transaction,refTerm.getTableName()),
                  refTerm.getAlias(),
                  translateName(refTerm.getColumnName())),
                  orderby.getDirection()));
      }
      return result;
   }
   
   /**
    * Replace all table names in the expression recursively.
    */
   private Expression replaceTableNames(Transaction transaction, Expression expr)
   {
      if ( expr == null )
         return null;
      Expression result = new Expression();
      for ( int i=0; i<expr.size(); i++ )
      {
         Object term = expr.get(i);
         if ( term instanceof ReferenceTerm )
         {
            ReferenceTerm refTerm = (ReferenceTerm) term;
            result.add(new ReferenceTerm(
                     transformTableName(transaction,refTerm.getTableName()),
                     translateName(refTerm.getAlias()),
                     translateName(refTerm.getColumnName()),
                     refTerm.getFunction()));
         } else if ( term instanceof Expression ) {
            result.add(replaceTableNames(transaction,(Expression) term));
         } else
            result.add(term);
      }
      return result;
   }

   /**
    * Transform the keys of the given list as if they were attribute names
    * for the given table.
    * @return A list with names transformed.
    */
   private List transformAttributes(List attributes)
   {
      ArrayList result = new ArrayList();
      Iterator entryIterator = attributes.iterator();
      while ( entryIterator.hasNext() )
      {
         String entry = (String) entryIterator.next();
         result.add(translateName(entry));
      }
      return result;
   }

   /**
    * Transform the keys of the given map as if they were attribute names
    * for the given table.
    * @return A map with the same values as the given map, but the keys
    * transformed possibly to new names.
    */
   private Map transformAttributes(Map attributes)
   {
      Map result = new HashMap();
      Iterator entryIterator = attributes.entrySet().iterator();
      while ( entryIterator.hasNext() )
      {
         Map.Entry entry = (Map.Entry) entryIterator.next();
         result.put(translateName((String) entry.getKey()),entry.getValue());
      }
      return result;
   }

   /**
    * Get the real table name for use with database. This method
    * transforms the name to fit database table max name length, and
    * makes the name lower case.
    */
   private String transformTableName(Transaction transaction, String tableName)
   {
      if ( tableName.indexOf('_') < 0 ) 
         return tableName; // Must be already translated, because no packages
      // Check if table exists, and load
      synchronized ( tableNameMutex )
      {
         if ( (tableNames==null) || (transactionNames==null) )
         {
            // No table yet, so check if it exists
            HashMap tableMapAttributes = new HashMap();
            tableMapAttributes.put("realname",String.class);
            tableMapAttributes.put("alias",String.class);
            ArrayList tableMapKeys = new ArrayList();
            tableMapKeys.add("alias");
            TransactionStatistics stats = ensureTable(transaction.getConnection(),"tablemap",tableMapAttributes,
                  tableMapKeys, true);
            transaction.getStats().add(stats);
            sqlStatistics.add(stats); // To accumulated sql stats
            // Now read the whole thing
            QueryStatement stmt = new QueryStatement("tablemap",null,null);
            SearchResult result = new SearchResult();
            stats = search(transaction.getConnection(),stmt,null,result);
            transaction.getStats().add(stats);
            sqlStatistics.add(stats); // To accumulated sql stats
            tableNames = new HashMap();
            for ( int i=0; i<result.getResult().size(); i++ )
            {
               Map attributes = (Map) result.getResult().get(i);
               tableNames.put(attributes.get("alias"),attributes.get("realname"));
            }
            // Add self
            tableNames.put("tablemap","tablemap");
            transactionNames = new HashMap();
         }
      }
      // Check table, whether this alias exists
      // First check in transaction table map,
      // then in global transaction map
      String tableNameCooked = tableName.toLowerCase();
      String realName = getTableName(transaction,tableNameCooked);
      if ( realName != null )
         return realName;
      if ( logger.isDebugEnabled() )
      {
         synchronized ( tableNames )
         {
            logger.debug("could not find table alias: "+tableNameCooked+" from: "+tableNames+", will create it.");
         }
      }
      // Ok, name does not exist yet, so create real name
      // for this alias.
      // First check whether simple names are approriate
      // so hu.netmind.beankeeper_Book becomes simply 'book'.
      logger.debug("could not find computed name for preliminary table name: "+tableNameCooked+", calculating one.");
      String tableNameSimple;
      int lastIndex = tableNameCooked.length();
      if ( tableNameCooked.endsWith("_") )
      {
         // This is a subtable, so inlcude the previous tag too
         if ( tableNameCooked.length() < 2 )
            throw new StoreException("table name too short: "+tableNameCooked);
         lastIndex = tableNameCooked.lastIndexOf('_',tableNameCooked.length()-2);
         if ( lastIndex <= 0 )
            throw new StoreException("table name ends with '_', but has no parent: "+tableNameCooked);
      }
      lastIndex = tableNameCooked.lastIndexOf('_',lastIndex-1);
      if ( lastIndex != -1 )
         tableNameSimple = tableNameCooked.substring(lastIndex+1);
      else
         tableNameSimple = tableNameCooked;
      // Check, whether simple name is a reserved word. If it
      // is, then translate it.
      tableNameSimple = translateName(tableNameSimple);
      // Check now, whether simple name is good, if not, then
      // extend it with package names.
      // If name becomes too long, then use numbers to distinguish
      logger.debug("trying simple name: "+tableNameSimple);
      while ( (tableNameSimple.length()<maxTableNameLength) &&
            (!tableNameSimple.startsWith("_")) &&
            (isRealTableNameTaken(transaction,tableNameSimple)) &&
            (lastIndex > 0) )
      {
         // This means table name is still not unambigous,
         // but at least it's short, so add another package
         // back to the simple name
         lastIndex = tableNameCooked.lastIndexOf('_',lastIndex-1);
         tableNameSimple = tableNameCooked.substring(lastIndex+1);
      }
      logger.debug("final simple name: "+tableNameSimple);
      // If name became too long, or still not unambigous, then
      // add number to the end
      String newTableName = tableNameSimple;
      for ( int index=0; 
            (newTableName.length()>maxTableNameLength) ||
            (getTableName(transaction,newTableName)!=null) ; 
            index++ )
      {
         if ( index>1000 )
            throw new StoreException("something is wrong, could not calculate unabigous name for: "+tableNameCooked);
         newTableName = tableNameSimple.substring(0,maxTableNameLength-3)+index;
      }
      tableNameSimple = newTableName;
      // Ok, so far so good. tableNameSimple now contains an unambigous
      // appropriately short name for given alias, now only insert, then
      // append to table map and return.
      // There is a little dirty trick though. Before inserting a class,
      // first remove it from the table. This work arounds a problem:
      // if two nodes are active, both start without knowning a class,
      // then the first inserts it, the second can not, because now it
      // already is contained in the database.
      logger.debug("translated table name: "+tableNameCooked+" to: "+tableNameSimple);
      Map insertTableName = new HashMap();
      insertTableName.put("alias",tableNameCooked);
      TransactionStatistics stats = remove(transaction.getConnection(),"tablemap",insertTableName);
      transaction.getStats().add(stats);
      sqlStatistics.add(stats); // To accumulated sql stats
      insertTableName.put("realname",tableNameSimple);
      stats = insert(transaction.getConnection(),"tablemap",insertTableName);
      transaction.getStats().add(stats);
      sqlStatistics.add(stats); // To accumulated sql stats
      synchronized ( tableNameMutex )
      {
         Map transactionTable = (Map) transactionNames.get(transaction);
         if ( transactionTable == null )
         {
            transactionTable = new HashMap();
            transactionNames.put(transaction,transactionTable);
         }
         transactionTable.put(tableNameCooked,tableNameSimple);
      }
      // Return already
      return tableNameSimple;
   }

   /**
    * Check if table name is taken.
    */
   private boolean isRealTableNameTaken(Transaction transaction, String tableName)
   {
      synchronized ( tableNameMutex )
      {
         if ( tableNames.containsValue(tableName) )
            return true;
         Map transactionTable = (Map) transactionNames.get(transaction);
         if ( (transactionTable!=null) && (transactionTable.containsValue(tableName)) )
            return true;
         return false;
      }
   }
   
   /**
    * Check whether that alias is already assigned a real table name,
    * and returns that name.
    */
   private String getTableName(Transaction transaction, String alias)
   {
      synchronized ( tableNameMutex )
      {
         Map transactionTable = (Map) transactionNames.get(transaction);
         if ( (transactionTable!=null) && (transactionTable.get(alias)!=null) )
            return (String) transactionTable.get(alias);
         if ( tableNames.get(alias) != null )
            return (String) tableNames.get(alias);
         return null;
      }
   }

   /**
    * Activate or discard table names added in the transaction.
    */
   public void handle(PersistenceEvent event)
   {
      if ( ! (event instanceof TransactionEvent) )
         return; // Quick exit
      Transaction transaction = ((TransactionEvent) event).getTransaction();
      if ( event instanceof TransactionCommittedEvent )
      {
         synchronized ( tableNameMutex )
         {
            Map transactionTables = (Map) transactionNames.get(transaction);
            if ( transactionTables == null )
               return;
            tableNames.putAll(transactionTables);
            transactionNames.remove(transaction);
         }
      }
      if ( event instanceof TransactionRolledbackEvent )
      {
         synchronized ( tableNameMutex )
         {
            transactionNames.remove(transaction);
         }
      }
   }

   /**
    * Read the reserved word list.
    */
   private void readReservedWords()
   {
      reverseReservedWords = new HashMap();
      reservedWords = new HashMap();
      // Read from list
      try
      {
         ClassLoader loader = Database.class.getClassLoader();
         BufferedReader reader = new BufferedReader(new InputStreamReader(loader.getResourceAsStream("reserved.words")));
         String line = null;
         String source;
         String target;
         String obscure;
         while ( (line=reader.readLine()) != null )
         {
            source = line.toLowerCase();
            target = source+"_";
            // Put in the normal fields
            reservedWords.put(source,target);
            reverseReservedWords.put(target,source);
            // Make a transation for the reverse words, maybe user wants
            // to use those, escape them to an obscure name. If user wants
            // to use the obscure name, she'll get an error.
            obscure = source+"_underscore";
            reservedWords.put(target,obscure);
            reverseReservedWords.put(obscure,target);
         }
      } catch ( Exception e ) {
         throw new StoreException("error while reading reserved words list",e);
      }
   }

   
   /**
    * Modifies an object already in database with given fields.
    * @param tableName The table to save attributes to.
    * @param id The id of object to save (All object entries have an id).
    * @param attributes The attributes in form of name:value pairs.
    */
   protected abstract TransactionStatistics save(Connection connection, String tableName, 
         Map keys, Map attributes);

   /**
    * Insert an object into the database.
    * @param tableName The table to save attributes to.
    * @param attributes The attributes in form of name:value pairs.
    */
   protected abstract TransactionStatistics insert(Connection connection, String tableName, 
         Map attributes);

   /**
    * Remove an entry from database.
    * @param tableName The table to remove object from.
    * @param attributes The attributes which identify the object.
    * Equality is assumed with each attribute and it's value.
    */
   protected abstract TransactionStatistics remove(Connection connection, String tableName,
         Map attributes);
   
   /**
    * Ensure that table exists in database. It is the responsibility of
    * the implementation to ensure that the named table with given
    * parameters exists. If the table exists, but is not defined
    * as in 'attributeTypes', the implementation <strong>must</strong>
    * retain all common attributes during the re-structuring of that
    * table. Of course, if no common attributes exist, the implementation
    * is free to drop the table and recreate it.<br>
    * In every database, the field named 'persistence_id' is the primary
    * key if it exists, or no primary key if the attributeTypes do not
    * contain it.<br>
    * All columns will be indexed by default.
    * @param tableName The table to check.
    * @param attributeTypes The attribute names together with which
    * java class they should hold.
    * @param create If true, create table physically, if false, only
    * update internal representations, but do not create table.
    */
   protected abstract TransactionStatistics ensureTable(Connection connection, String tableName,
         Map attributeTypes, List keyAttributeNames, boolean create);
   
   /**
    * Select objects from database as ordered list of attribute maps.
    * @param connection The connection to use.
    * @param stmt The query statement.
    * @param limits The limits of the result. (Offset, maximum result count)
    * @param result The result object. It will be filled with data.
    */
   protected abstract TransactionStatistics search(Connection connection, QueryStatement stmt, 
         Limits limits, SearchResult result);
}


