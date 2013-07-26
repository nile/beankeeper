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

import java.util.Map;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.ArrayList;
import java.util.Date;
import java.util.Collections;
import java.util.Collection;
import java.util.List;
import java.util.HashMap;
import java.util.Stack;
import java.util.LinkedList;
import java.sql.Timestamp;
import org.apache.log4j.Logger;
import java.sql.ResultSet;
import java.sql.DatabaseMetaData;
import java.sql.Types;
import java.sql.ResultSetMetaData;
import hu.netmind.beankeeper.parser.*;
import hu.netmind.beankeeper.common.StoreException;
import hu.netmind.beankeeper.transaction.*;
import hu.netmind.beankeeper.db.*;
import hu.netmind.beankeeper.logging.AggregatorLogger;

/**
 * This is a generic database implementation. It contains no optimization,
 * and tries to be as generic with types and sql syntax as possible, this also
 * means, it cannot handle limits and offsets, which causes <strong>all</strong>
 * results to be returned, even with lazy lists.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public class GenericDatabase extends DatabaseBase implements Database
{
   private static Logger logger = Logger.getLogger(GenericDatabase.class);
   private static Logger sqlLogger = Logger.getLogger("hu.netmind.beankeeper.sql");

   private AggregatorLogger aggregatorLogger = null; // Injected
  
   /**
    * Save basic attribute information, so we have it ready when needed.
    */
   private Map tableAttributes = Collections.synchronizedMap(new HashMap());

   protected Map getAttributes(String tableName)
   {
      return (Map) tableAttributes.get(tableName.toLowerCase());
   }

   protected Class getAttributeType(String tableName, String attributeName)
   {
      return (Class) getAttributes(tableName).get(attributeName.toLowerCase());
   }

   private void setAttributes(String tableName, Map attributes)
   {
      tableAttributes.put(tableName,attributes);
   }

   /**
    * Make a save statement for given table, id and changed attributes.
    * Override this method in a subclass for a non-generic behaviour.
    * The attributes' placeholders must be in the same order as given
    * by the attributeNames list.
    * @param tableName The table to save attributes to.
    * @param keyNames The keys of object to save (All object entries have keys).
    * @param attributeNames The attributes that will change.
    * the statement.
    * @param keys Keys.
    * @return An SQL save/update statement specific to database backend.
    */
   protected String getSaveStatement(String tableName, List keyNames, List attributeNames,
         Map keys)
   {
      // Make a generic update statement
      StringBuffer statement = new StringBuffer("update "+tableName+
            " set ");
      for ( int i=0; i<attributeNames.size(); i++ )
         statement.append(attributeNames.get(i).toString()+"=?,");
      statement.delete(statement.length()-1,statement.length());
      if ( keyNames.size() > 0 )
         statement.append(" where ");
      for ( int i=0; i<keyNames.size(); i++ )
      {
         String keyName = keyNames.get(i).toString();
         if ( keys.get(keyName) == null )
         {
            statement.append(keyName+" is null and ");
            keyNames.remove(keyName);
            i--;
         } else 
            statement.append(keyName+"=? and ");
      }
      statement.delete(statement.length()-5,statement.length());
      sqlLogger.debug("preparing update statement: "+statement.toString());
      // Return 
      return statement.toString();
   }
   
   /**
    * Modifies an object already in database with given fields.
    * @param tableName The table to save attributes to.
    * @param id The id of object to save (All object entries have an id).
    * @param attributes The attributes in form of name:value pairs.
    */
   protected TransactionStatistics save(Connection connection, String tableName, 
         Map keys, Map attributes)
   {
      TransactionStatistics stats = new TransactionStatistics();
      // Clear null values from attributes
      attributes = new HashMap(attributes);
      ArrayList nullArrayList = new ArrayList();
      nullArrayList.add(null);
      attributes.values().removeAll(nullArrayList);
      // Make a generic insert statement
      ArrayList attributeNames = new ArrayList(attributes.keySet());
      ArrayList keyNames = new ArrayList(keys.keySet());
      String statement = getSaveStatement(tableName,keyNames,attributeNames,keys);
      // Prepare
      PreparedStatement pstmt;
      try
      {
         pstmt = connection.prepareStatement(statement);
         int i=0;
         for ( ; i<attributeNames.size(); i++ )
         {
            Object value = getSQLValue(attributes.get(attributeNames.get(i)));
            Class type = getAttributeType(tableName,(String) attributeNames.get(i));
            sqlLogger.debug("setting statement parameter #"+i+": "+value);
            pstmt.setObject(i+1,value,getSQLType(type));
         }
         for ( ; i<keyNames.size()+attributeNames.size(); i++ )
         {
            Object value = getSQLValue(keys.get(keyNames.get(i-attributeNames.size())));
            sqlLogger.debug("setting statement parameter #"+i+": "+value);
            pstmt.setObject(i+1,value);
         }
      } catch ( Exception e ) {
         throw new StoreException("cannot prepare statement: "+statement,e);
      }
      // Execute
      try
      {
         if ( logger.isDebugEnabled() )
            logger.debug("excuting update statement: "+statement);
         long startTime = System.currentTimeMillis();
         pstmt.executeUpdate();
         long endTime = System.currentTimeMillis();
         aggregatorLogger.log("Update statement execution",new int[] { (int) (endTime-startTime) });
         stats.setUpdateCount(1);
         stats.setUpdateTime(endTime-startTime);
      } catch ( Exception e ) {
         throw new StoreException("exception while sql update",e);
      } finally {
         try
         {
            pstmt.close();
         } catch ( Exception e ) {
            logger.debug("unable to close statement",e);
         }
      }
      // Return
      return stats;
   }

   /**
    * Make an insert statement for given table, id and attributes.
    * Override this method in a subclass for a non-generic behaviour.
    * The attributes' placeholders must be in the same order as given
    * by the attributeNames list.
    * @param tableName The table to insert attributes to.
    * @param attributeNames The attributes that will be inserted.
    * @return An SQL insert statement specific to database backend.
    */
   protected String getInsertStatement(String tableName, List attributeNames)
   {
      // Make a generic insert statement
      StringBuffer statement = new StringBuffer("insert into "+tableName+
            " (");
      for ( int i=0; i<attributeNames.size(); i++ )
         statement.append(attributeNames.get(i).toString()+",");
      statement.delete(statement.length()-1,statement.length());
      statement.append(") values (");
      for ( int i=0; i<attributeNames.size(); i++ )
         statement.append("?,");
      statement.delete(statement.length()-1,statement.length());
      statement.append(")");
      sqlLogger.debug("preparing insert statement: "+statement.toString());
      // Return
      return statement.toString();
   }

   /**
    * Insert an object into the database.
    * @param tableName The table to save attributes to.
    * @param id The id of object to save (All object entries have an id).
    * @param attributes The attributes in form of name:value pairs.
    */
   protected TransactionStatistics insert(Connection connection, String tableName, Map attributes)
   {
      TransactionStatistics stats = new TransactionStatistics();
      // Clear null values from attributes
      attributes = new HashMap(attributes);
      ArrayList nullArrayList = new ArrayList();
      nullArrayList.add(null);
      attributes.values().removeAll(nullArrayList);
      // Make a generic insert statement
      ArrayList attributeNames = new ArrayList(attributes.keySet());
      String statement = getInsertStatement(tableName, attributeNames);
      // Execute
      PreparedStatement pstmt;
      try
      {
         pstmt = connection.prepareStatement(statement);
         for ( int i=0; i<attributeNames.size(); i++ )
         {
            Object value = getSQLValue(attributes.get(attributeNames.get(i)));
            Class type = getAttributeType(tableName,(String) attributeNames.get(i));
            sqlLogger.debug("setting statement parameter #"+i+": "+value);
            pstmt.setObject(i+1,value,getSQLType(type));
         }
      } catch ( Exception e ) {
         throw new StoreException("cannot prepare statement: "+statement,e);
      }
      // Execute
      try
      {
         if ( logger.isDebugEnabled() )
            logger.debug("excuting insert statement: "+statement);
         long startTime = System.currentTimeMillis();
         pstmt.executeUpdate();
         long endTime = System.currentTimeMillis();
         aggregatorLogger.log("Insert statement execution",new int[] { (int) (endTime-startTime) });
         stats.setInsertCount(1);
         stats.setInsertTime(endTime-startTime);
      } catch ( Exception e ) {
         throw new StoreException("exception while sql insert",e);
      } finally {
         try
         {
            pstmt.close();
         } catch ( Exception e ) {
            logger.debug("unable to close statement",e);
         }
      }
      // Return
      return stats;
   }

   /**
    * Make a remove statement for given table and attributes.
    * Override this method in a subclass for a non-generic behaviour.
    * The attributes' placeholders must be in the same order as given
    * by the attributeNames list.
    * @param tableName The table to remove attributes from.
    * @param attributeNames The attributes that will be search for by the remove.
    * @return An SQL insert statement specific to database backend.
    */
   protected String getRemoveStatement(String tableName, List attributeNames)
   {
      // Make a generic delete statement
      // Please note the paticularly funny delete-6 to handle
      // empty lists and list ends.
      StringBuffer statement = new StringBuffer("delete from "+tableName);
      if ( !attributeNames.isEmpty() )
         statement.append(" where ");
      for ( int i=0; i<attributeNames.size(); i++ )
         statement.append(attributeNames.get(i).toString()+"=?  and  ");
      if ( !attributeNames.isEmpty() )
         statement.delete(statement.length()-6,statement.length());
      sqlLogger.debug("preparing delete statement: "+statement.toString());
      // Return
      return statement.toString();
   }
   
   /**
    * Remove an entry from database.
    * @param tableName The table to remove object from.
    * @param attributes The attributes which identify the object.
    * Equality is assumed with each attribute and it's value.
    */
   protected TransactionStatistics remove(Connection connection, String tableName,
         Map attributes)
   {
      TransactionStatistics stats = new TransactionStatistics();
      // Clear null values from attributes
      attributes = new HashMap(attributes);
      ArrayList nullArrayList = new ArrayList();
      nullArrayList.add(null);
      attributes.values().removeAll(nullArrayList);
      // Make statement
      ArrayList attributeNames = new ArrayList(attributes.keySet());
      String statement = getRemoveStatement(tableName, attributeNames);
      // Prepare
      PreparedStatement pstmt;
      try
      {
         pstmt = connection.prepareStatement(statement);
         for ( int i=0; i<attributeNames.size(); i++ )
            pstmt.setObject(i+1,getSQLValue(attributes.get(attributeNames.get(i))));
      } catch ( Exception e ) {
         throw new StoreException("cannot prepare statement: "+statement,e);
      }
      // Execute
      try
      {
         long startTime = System.currentTimeMillis();
         pstmt.executeUpdate();
         long endTime = System.currentTimeMillis();
         aggregatorLogger.log("Remove statement execution",new int[] { (int) (endTime-startTime) });
         stats.setDeleteCount(1);
         stats.setDeleteTime(endTime-startTime);
      } catch ( Exception e ) {
         throw new StoreException("exception while sql delete",e);
      } finally {
         try
         {
            pstmt.close();
         } catch ( Exception e ) {
            logger.debug("unable to close statement",e);
         }
      }
      // Return
      return stats;
   }

   /**
    * Drop the table with given name.
    * This method is not called directly, but from <code>ensureTable</code>.
    * @param tableName The table to drop.
    */
   protected TransactionStatistics dropTable(Connection connection, String tableName)
   {
      TransactionStatistics stats = new TransactionStatistics();
      // Create statement
      String statement = "drop table "+tableName;
      // Execute statement
      long startTime = System.currentTimeMillis();
      executeUpdate(connection,statement);
      long endTime = System.currentTimeMillis();
      stats.setSchemaCount(1);
      stats.setSchemaTime(endTime-startTime);
      return stats;
   }

   /**
    * Get the create table statement before the attributes part.
    */
   protected String getCreateTableStatement(Connection connection, String tableName)
   {
      return "create table "+tableName;
   }

   /**
    * Create table with given name, attribute types, and keys.
    * This method is not called directly, but from <code>ensureTable</code>.
    * @param tableName The table to create.
    * @param attributeTypes The attribute names together with which
    * java class they should hold.
    */
   protected TransactionStatistics createTable(Connection connection, String tableName,
         Map attributeTypes, List keyAttributeNames)
   {
      TransactionStatistics stats = new TransactionStatistics();
      // Create statement
      StringBuffer statement = new StringBuffer(getCreateTableStatement(connection,tableName)+" (");
      Iterator iterator = attributeTypes.entrySet().iterator();
      while ( iterator.hasNext() )
      {
         Map.Entry entry = (Map.Entry) iterator.next();
         statement.append(entry.getKey().toString()+" "+getSQLTypeName(getSQLType(((Class) entry.getValue())))+",");
      }
      if ( (keyAttributeNames!=null) && (keyAttributeNames.size() > 0) )
      {
         statement.append(" primary key (");
         for ( int i=0; i<keyAttributeNames.size(); i++ )
            statement.append(keyAttributeNames.get(i)+",");
         statement.delete(statement.length()-1,statement.length());
         statement.append(")  ");
      }
      statement.delete(statement.length()-1,statement.length());
      statement.append(")");
      // Execute statement
      long startTime = System.currentTimeMillis();
      executeUpdate(connection,statement.toString());
      long endTime = System.currentTimeMillis();
      stats.setSchemaCount(1);
      stats.setSchemaTime(endTime-startTime);
      // Create initial indexes (currently all attributes will be indexed)
      // Do not create for reserved tables
      if ( 
           (!tableName.equalsIgnoreCase("tablemap")) &&
           (!tableName.equalsIgnoreCase("nodes")) &&
           (!tableName.equalsIgnoreCase("classes")) )
         stats.add(createIndexes(connection, tableName, attributeTypes));
      return stats;
   }

   /**
    * Get the statement to drop a column.
    */
   protected String getDropColumnStatement(String tableName, String columnName)
   {
      return "alter table "+tableName+" drop column "+columnName;
   }

   /**
    * Get the statement to add a column to a table.
    */
   protected String getAddColumnStatement(String tableName, String columnName, String columnType)
   {
      return "alter table "+tableName+" add column "+columnName+" "+columnType;
   }

   /**
    * Drop a column from a table.
    * @param connection The connection object.
    * @param tableName The table to drop column from.
    * @param columnName The column to drop.
    */
   protected TransactionStatistics dropColumn(Connection connection, String tableName,
         String columnName)
   {
      TransactionStatistics stats = new TransactionStatistics();
      long startTime = System.currentTimeMillis();
      executeUpdate(connection,getDropColumnStatement(tableName,columnName));
      long endTime = System.currentTimeMillis();
      stats.setSchemaCount(1);
      stats.setSchemaTime(endTime-startTime);
      return stats;
   }

   /**
    * Add a column to a table.
    * @param connection The connection object.
    * @param tableName The table to drop column from.
    * @param columnName The column to create.
    * @param columnType The column type to create.
    */
   protected TransactionStatistics addColumn(Connection connection, String tableName,
         String columnName, String columnType)
   {
      TransactionStatistics stats = new TransactionStatistics();
      long startTime = System.currentTimeMillis();
      executeUpdate(connection,getAddColumnStatement(tableName,columnName,columnType));
      long endTime = System.currentTimeMillis();
      stats.setSchemaCount(1);
      stats.setSchemaTime(endTime-startTime);
      return stats;
   }

   /**
    * Alter the table. Implementation: Not all databases support
    * altering multiple columns, so all columns are separately modified.
    * @param connection The connection object.
    * @param tableName The table name.
    * @param removedAttributes Attribute names which should be removed.
    * @param addedAttributes Attribute names which should be added.
    * @param attributeTypes Types in form of Classes to given names.
    */
   protected TransactionStatistics alterTable(Connection connection, String tableName,
         List removedAttributes, List addedAttributes, Map attributeTypes, List keyAttributeNames)
   {
      TransactionStatistics stats = new TransactionStatistics();
      if ( (removedAttributes.size()==0) && (addedAttributes.size()==0) )
      {
         logger.debug("table '"+tableName+"' schema matches class, no modification required.");
         return stats;
      }
      logger.debug("table layout mismatch for table '"+tableName+"': removed columns: "+removedAttributes+", added columns: "+addedAttributes);
      // Assemble statements and execute them
      for ( int i=0; i<removedAttributes.size(); i++ )
      {
         String attributeName = (String) removedAttributes.get(i);
         stats.add(dropColumn(connection,tableName,attributeName));
      }
      for ( int i=0; i<addedAttributes.size(); i++ )
      {
         String attributeName = (String) addedAttributes.get(i);
         String sqlTypeName = getSQLTypeName(getSQLType((Class)attributeTypes.get(addedAttributes.get(i))));
         stats.add(addColumn(connection,tableName,attributeName,sqlTypeName));
      }
      // Create initial indexes (currently all new attributes will be indexed)
      HashMap addedTypes = new HashMap();
      for ( int i=0; i<addedAttributes.size(); i++ )
      {
         Object name = addedAttributes.get(i);
         addedTypes.put(name,attributeTypes.get(name));
      }
      stats.add(createIndexes(connection, tableName, addedTypes));
      return stats;
   }

   /**
    * Get index creation statement for a given table and field.
    * @return The statement to use, or null, of no such index can be
    * created.
    */
   protected String getCreateIndexStatement(String indexName,String tableName, String field,
         Class fieldClass)
   {
      if ( fieldClass.equals(byte[].class) )
         return null;
      return "create index "+indexName+" on "+tableName+" ("+field+")";
   }

   /**
    * Get an unused index name.
    */
   protected String getCreateIndexName(Connection connection, String tableName,
         String field)
   {
      try
      {
         // Get max length
         DatabaseMetaData dmd = connection.getMetaData();
         int maxTableNameLength = dmd.getMaxTableNameLength();
         if ( maxTableNameLength == 0 )
            maxTableNameLength = Integer.MAX_VALUE;
         // Check whether trivial name is good enough
         String result = tableName+"_idx_"+field;
         if ( result.length() <= maxTableNameLength )
            return result;
         // Assemble all indexes to table
         HashSet indexNames = new HashSet();
         ResultSet rs = dmd.getIndexInfo(null,null,tableName,false,true);
         while ( rs.next() )
         {
            String indexName = rs.getString("INDEX_NAME");
            if ( indexName != null )
               indexNames.add(indexName.toLowerCase());
         }
         rs.close();
         // Create an index name which does not currently exist
         if ( logger.isDebugEnabled() )
            logger.debug("creating index name, existing indexes: "+indexNames);
         result = result.substring(0,maxTableNameLength-4).toLowerCase();
         int nameIndex = 1;
         while ( indexNames.contains(result+nameIndex) )
            nameIndex++;
         // Return abbr. name
         logger.debug("new index name: "+result+nameIndex);
         return result+nameIndex;
      } catch ( SQLException e ) {
         throw new StoreException("Could not compute approriate index name.",e);
      }
   }

   /**
    * Create indexes to the given attributes.
    * @param connection The SQL connection.
    * @param tableName The table to create indexes to.
    * @param attributeTypes The attributes and their types to create indexes to.
    */
   protected TransactionStatistics createIndexes(Connection connection, String tableName,
         Map attributeTypes)
   {
      TransactionStatistics stats = new TransactionStatistics();
      Iterator entries = attributeTypes.entrySet().iterator();
      while ( entries.hasNext() )
      {
         Map.Entry entry = (Map.Entry) entries.next();
         String indexName = getCreateIndexName(connection,tableName,entry.getKey().toString());
         String statement = getCreateIndexStatement(indexName,tableName,
               entry.getKey().toString(), (Class) entry.getValue());
         if ( statement != null )
         {
            long startTime = System.currentTimeMillis();
            executeUpdate(connection,statement);
            long endTime = System.currentTimeMillis();
            stats.setSchemaCount(stats.getSchemaCount()+1);
            stats.setSchemaTime(stats.getSchemaTime()+(endTime-startTime));
         }
      }
      return stats;
   }

   /**
    * Get the data types of a given table.
    * @return A map of names with the sql type number as value.
    */
   protected HashMap getTableAttributeTypes(Connection connection,
         String tableName)
      throws SQLException
   {
         DatabaseMetaData dmd = connection.getMetaData();
         ResultSet rs = dmd.getColumns(null,null,tableName,null);
         HashMap databaseAttributeTypes = new HashMap();
         while ( rs.next() )
         {
            // The tablename was a wildcard, so make sure that we
            // get the right table's column (thanks Daniel)
            if ( rs.getString("TABLE_NAME").equalsIgnoreCase(tableName) )
            {
               int type = getTableAttributeType(rs);
               databaseAttributeTypes.put(rs.getObject("COLUMN_NAME").toString().toLowerCase(),new Integer(type));
            }
         }
         rs.close();
         return databaseAttributeTypes;
   }

   /**
    * Override this method to get different types for attributes than
    * the database reports.
    */
   protected int getTableAttributeType(ResultSet rs)
      throws SQLException
   {
      return rs.getInt("DATA_TYPE");
   }
   
   /**
    * Ensure that table exists in database. Implementation does
    * not check keys. If keys differ, this implementation will not correct
    * the problem. Column renames are not detected, if a column is renamed,
    * the old column will be dropped and a new column will be created.
    * @param tableName The table to check.
    * @param attributeTypes The attribute names together with which
    * java class they should hold.
    * @param keyAttributeNames The keys of table.
    */
   protected TransactionStatistics ensureTable(Connection connection, String tableName,
         Map attributeTypes, List keyAttributeNames, boolean create)
   {
      TransactionStatistics stats = new TransactionStatistics();
      try
      {
         if ( create )
         {
            // Query whether table exists
            HashMap databaseAttributeTypes = getTableAttributeTypes(connection,tableName);
            // Diff the database schema and attribute schema
            logger.debug("comparing database schema with class, database has: "+databaseAttributeTypes+", class has: "+attributeTypes);
            if ( databaseAttributeTypes.size() == 0 )
            {
               // No columns found, better create that table
               logger.debug("table '"+tableName+"' did not exist, creating.");
               stats.add(createTable(connection,tableName,attributeTypes,keyAttributeNames));
            } else {
               // Columns found, so make diff
               // Note, that not only removed and added columns count, but
               // also columns which types have changed, since that also
               // means deleting and adding the column
               ArrayList removedAttributes = new ArrayList(databaseAttributeTypes.keySet()); 
               removedAttributes.removeAll(attributeTypes.keySet());
               ArrayList addedAttributes = new ArrayList(attributeTypes.keySet());
               addedAttributes.removeAll(databaseAttributeTypes.keySet());
               ArrayList changedAttributes = new ArrayList(databaseAttributeTypes.keySet());
               changedAttributes.retainAll(attributeTypes.keySet());
               Iterator changedAttributesIterator = changedAttributes.iterator();
               while ( changedAttributesIterator.hasNext() )
               {
                  String attribute = (String) changedAttributesIterator.next().toString();
                  if ( getSQLTypeName(((Integer) databaseAttributeTypes.get(attribute)).intValue()).equals(
                        getSQLTypeName(getSQLType((Class) attributeTypes.get(attribute)))) )
                     changedAttributesIterator.remove(); // Remove if type matches
               }
               removedAttributes.addAll(changedAttributes);
               addedAttributes.addAll(changedAttributes);
               // If all columns are to be removed, drop the table and re-create
               // it, or else first drop the old columns and add the new ones
               databaseAttributeTypes.remove("persistence_id");
               databaseAttributeTypes.remove("persistence_start");
               databaseAttributeTypes.remove("persistence_end");
               databaseAttributeTypes.remove("persistence_txend");
               databaseAttributeTypes.remove("persistence_txstart");
               databaseAttributeTypes.remove("persistence_txstartid");
               databaseAttributeTypes.remove("persistence_txendid");
               if ( (removedAttributes.size() == databaseAttributeTypes.size()) &&
                     (removedAttributes.size()>0) )
               {
                  // Uh-oh, all attributes changed, re-create table
                  logger.debug("table '"+tableName+"' will be re-created, because all "+
                        "attributes changed apparently, removed: "+removedAttributes+", added: "+addedAttributes);
                  stats.add(dropTable(connection,tableName));
                  stats.add(createTable(connection,tableName,attributeTypes,keyAttributeNames));
               } else if ( (removedAttributes.size()>0) || (addedAttributes.size()>0) ) {
                  // Partial change, only change what is necessary
                  logger.debug("table '"+tableName+"' has a partial change, removing: "+removedAttributes+", adding: "+addedAttributes);
                  stats.add(alterTable(connection,tableName,removedAttributes,addedAttributes,attributeTypes, keyAttributeNames));
               } else {
                  logger.debug("nothing to change on table: "+tableName);
               }
            }
         } else {
            logger.debug("table is not to be created: "+tableName);
         }
         // Remember these attributes
         setAttributes(tableName,attributeTypes);
      } catch ( Exception e ) {
         throw new StoreException("could not ensure table exists: "+tableName,e);
      }
      // Return
      return stats;
   }

   /**
    * Get the limit component of statement, if it can be expressed in
    * the current database with simple statement part.
    * @param limits The limits to apply.
    */
   protected String getLimitStatement(String statement, Limits limits, List types)
   {
      return statement;
   }

   /**
    * Get the count statement for the given statement.
    */
   protected String getCountStatement(String stmt)
   {
      return "select count(*) from ("+stmt+") as cr";
   }

   /**
    * Get the table declaration for a select statment.
    */
   protected String getTableDeclaration(String tableName, String alias)
   {
      if ( alias == null )
         return tableName;
      else
         return tableName+" as "+alias;
   }

   /**
    * Assemble the query columns of a given term.
    */
   protected String getQuerySource(TableTerm term, Set queryColumns, List types)
   {
      // Determine name
      String name = term.getName();
      // List all attributes one-by-one
      StringBuffer result = new StringBuffer();
      Map attributeTypes = getAttributes(term.getTableName());
      if ( attributeTypes == null )
         throw new StoreException("attributes types not present for table: "+term.getTableName()+", map: "+tableAttributes.keySet());
      Iterator attributeEntryIterator =  attributeTypes.entrySet().iterator();
      while ( attributeEntryIterator.hasNext() )
      {
         Map.Entry entry = (Map.Entry) attributeEntryIterator.next();
         String attributeName = (String) entry.getKey();
         Class attributeType = (Class) entry.getValue();
         if ( ! attributeName.startsWith("persistence_") )
         {
            result.append(name+"."+attributeName+",");
            types.add(attributeType);
            checkAttribute(queryColumns,attributeName);
         }
      }
      if ( (result.length()>0) && (result.charAt(result.length()-1)==',') )
         result.deleteCharAt(result.length()-1);
      return result.toString();
   }
  
   /**
    * Check the attribute whether it's already contained in the
    * columns.
    */
   private void checkAttribute(Set queryColumns, String attributeName)
   {
      if ( ! queryColumns.add(attributeName) )
         throw new StoreException("query has multiple columns with same name, try to add: "+attributeName+", attributes until now: "+queryColumns);
   }
   
   /**
    * Assemble the query columns of select statement.
    * @param stmt The statement to get query source from.
    * @param types The type list to fill in. Each queried column's
    * type should be inserted into this list in order.
    * @return The columns part of the select statement.
    */
   protected String getQuerySource(QueryStatement stmt, List types)
   {
      if ( logger.isDebugEnabled() )
         logger.debug("computing query source from: "+stmt.getSelectTerms());
      HashSet queryColumns = new HashSet();
      StringBuffer querySource = new StringBuffer();
      for ( int i=0; i<stmt.getSelectTerms().size(); i++ )
      {
         if ( querySource.length() > 0 )
            querySource.append(",");
         TableTerm term = (TableTerm) stmt.getSelectTerms().get(i);
         if ( term instanceof ReferenceTerm )
         {
            logger.debug("adding reference term: "+term);
            // This is specifically an attribute
            ReferenceTerm refTerm = (ReferenceTerm) term;
            if ( refTerm.getFunction() == null )
               querySource.append(refTerm.getName()+"."+refTerm.getColumnName());
            else
               querySource.append(refTerm.getFunction().apply(
                        refTerm.getName()+"."+refTerm.getColumnName()));
            if ( refTerm.getColumnAlias() != null )
            {
               querySource.append(" as "+refTerm.getColumnAlias());
               checkAttribute(queryColumns,refTerm.getColumnAlias());
            } else {
               checkAttribute(queryColumns,refTerm.getColumnName());
            }
            Class attributeType = getAttributeType(term.getTableName(),
                  ((ReferenceTerm)term).getColumnName());
            types.add(attributeType);
         } else {
            SpecifiedTableTerm specifiedTerm = 
               stmt.getSpecifiedTerm(term); // Switch for specified term
            if ( logger.isDebugEnabled() )
               logger.debug("adding table term: "+specifiedTerm);
            // This is a table
            querySource.append(getQuerySource(specifiedTerm,queryColumns,types));
            for ( int o=0; o<specifiedTerm.getRelatedLeftTerms().size(); o++ )
            {
               TableTerm leftTableTerm = 
                  ((SpecifiedTableTerm.LeftjoinEntry) 
                   specifiedTerm.getRelatedLeftTerms().get(o)).term;
               String sourcePart = getQuerySource(leftTableTerm,queryColumns,types);
               if ( sourcePart.length() > 0 )
               {
                  if ( querySource.length()>0 )
                     querySource.append(",");
                  querySource.append(sourcePart);
               }
            }
         }
      }
      // Add persistence id/startserial/endserial
      TableTerm firstTerm = null;
      if ( stmt.getSelectTerms().size()>0 )
         firstTerm = (TableTerm) stmt.getSelectTerms().get(0);
      if ( (!"tablemap".equalsIgnoreCase(firstTerm.getTableName())) && 
           (!"classes".equalsIgnoreCase(firstTerm.getTableName())) &&
           (!"nodes".equalsIgnoreCase(firstTerm.getTableName())) &&
           (stmt.getMode()!=QueryStatement.MODE_VIEW) )
      {
         // Add persistence id
         if ( querySource.length()>0 )
            querySource.append(",");
         querySource.append(firstTerm.getName()+".persistence_id");
         types.add(Long.class);
         // Add serials
         ArrayList terms = new ArrayList();
         terms.add(firstTerm);
         terms.addAll(stmt.getSpecifiedTerm(firstTerm).getRelatedLeftTerms());
         for ( int i=0; i<terms.size(); i++ )
         {
            querySource.append(",");
            querySource.append(firstTerm.getName()+".persistence_start as persistence_start"+i);
            types.add(Long.class);
            querySource.append(",");
            querySource.append(firstTerm.getName()+".persistence_end as persistence_end"+i);
            types.add(Long.class);
         }
      }
      // Return
      return querySource.toString();
   }

   /**
    * Transform the select terms.
    */
   protected List transformTerms(List terms)
   {
      return terms;
   }

   /**
    * Transform the expression. This method is called on each subsequent
    * expressions too, so implementation does not have to be recursive.
    * @param expr The expression to possibly transform.
    * @return A transformed expression.
    */
   protected Expression transformExpression(Expression expr)
   {
      return expr;
   }

   /**
    * Checks whether a list of table terms contains a given reference term,
    * but not by using the standard <code>equals()</code> method, but by
    * comparing a reference term with own equality.
    */
   private boolean containsReferenceTerm(List terms, ReferenceTerm term)
   {
      for ( int i=0; i<terms.size(); i++ )
      {
         TableTerm objRaw = (TableTerm) terms.get(i);
         if ( objRaw instanceof ReferenceTerm )
         {
            ReferenceTerm obj = (ReferenceTerm) objRaw;
            if ( (obj.equals(term)) && (obj.getColumnName().equals(term.getColumnName())) )
               return true;
         }
      }
      return false;
   }

   /**
    * Walk the given expression tree, and produce and sql expression.
    * @param expression The beankeeper expression to process.
    * @param values All constants which have a placeholder in the sql
    * expression will be appended to this list in order.
    * @return An SQL expression matching the given beankeeper expression.
    */
   private String getExpression(Expression expression, List values)
   {
      if ( logger.isDebugEnabled() )
         logger.debug("expression, that will be translated to sql: "+expression);
      StringBuffer conditionPart = new StringBuffer();
      Stack openNodes = new Stack(); // Used in traversing the expression tree
      if ( expression != null )
         openNodes.push(new ArrayList(transformExpression(expression)));
      while ( ! openNodes.isEmpty() )
      {
         // Select the current expression part list
         ArrayList parts = (ArrayList) openNodes.peek();
         // If there are no parts, then return
         if ( parts.size() == 0 )
         {
            openNodes.pop();
            if ( ! openNodes.isEmpty() )
               conditionPart.append(")"); // Close sub-expression
            continue; // 20 goto 10
         }
         // Process the most left symbol
         Object nextPart = parts.remove(0);
         // There are basically 4 cases depending on the class of nextPart
         if ( nextPart instanceof Expression )
         {
            // It is an expression, so start new block and push expression
            // items
            conditionPart.append("(");
            openNodes.push(new ArrayList(transformExpression((Expression) nextPart)));
         }
         if ( nextPart instanceof String )
         {
            // It is an operator, so just append
            conditionPart.append(" "+nextPart.toString()+" ");
         }
         if ( nextPart instanceof ConstantTerm )
         {
            // It is a constant, add ? and remember value
            Object value = ((ConstantTerm) nextPart).getValue();
            if ( value instanceof Collection )
            {
               if ( logger.isDebugEnabled() )
                  logger.debug("detected collection constant value: "+value);
               // Add multiple values as an enumeration
               Iterator valueIterator = ((Collection) value).iterator();
               conditionPart.append(" (");
               while ( valueIterator.hasNext() )
               {
                  Object singleValue = valueIterator.next();
                  if ( singleValue == null )
                  {
                     conditionPart.append("null,");
                  } else {
                     conditionPart.append("?,");
                     values.add(singleValue);
                  }
               }
               if ( conditionPart.charAt(conditionPart.length()-1) == ',' )
                  conditionPart.delete(conditionPart.length()-1,conditionPart.length());
               conditionPart.append(") ");
            } else {
               if ( logger.isDebugEnabled() )
                  logger.debug("detected single constant value: "+value);
               // Add a single value
               if ( value == null )
               {
                  conditionPart.append(" null ");
               } else {
                  conditionPart.append(" ? ");
                  values.add(value);
               }
            }
         }
         if ( nextPart instanceof ReferenceTerm )
         {
            if ( logger.isDebugEnabled() )
               logger.debug("adding reference term: "+nextPart);
            // It is a reference type, remember name
            ReferenceTerm term = (ReferenceTerm) nextPart;
            if ( term.getFunction() == null )
               conditionPart.append(" "+term.getName()+"."+term.getColumnName()+" ");
            else
               conditionPart.append(" "+term.getFunction().apply(
                        term.getName()+"."+term.getColumnName())+" ");
         }
      }
      return conditionPart.toString();
   }

   /**
    * Select objects from database as ordered list of attribute maps.
    * @param connection The connection to run statements in.
    * @param stmt The query statement.
    * @param limits The limits of the result. (Offset, maximum result count)
    * @param result The result object.
    */
   protected TransactionStatistics search(Connection connection, 
         QueryStatement stmt, Limits limits, SearchResult searchResult)
   {
      TransactionStatistics stats = new TransactionStatistics();
      Expression expression = stmt.getQueryExpression();
      List orderBys = stmt.getOrderByList();
      // Assemble statement:
      // - Create query source sql
      // - Parse expression create sql
      // - Create statement without orderbys and limits 
      // - Run count statement to determine full count
      // - Apply limits and orderbys to statement
      // - Run statement to get results
     
      // Transform statement to fit database
      stmt.setSelectTerms(transformTerms(stmt.getSelectTerms()));
      // Compute query columns and tables
      ArrayList types = new ArrayList();
      String querySource = getQuerySource(stmt,types);
      if ( logger.isDebugEnabled() )
         logger.debug("query source: "+querySource+", type vector: "+types+", select terms: "+stmt.getSelectTerms());
      // Determine whether select will be distinct. Currently it is _not_
      // distinct if some field returned would be blob type.
      boolean isDistinct = ! types.contains(byte[].class);
      // Create tables full part (with left joins)
      if ( logger.isDebugEnabled() )
         logger.debug("creating tables part from specified terms: "+stmt.getSpecifiedTerms());
      ArrayList statementValues = new ArrayList(); // Will hold values for '?' signs in-order
      StringBuffer tablesPart = new StringBuffer();
      Iterator tableIterator = stmt.getSpecifiedTerms().iterator();
      while ( tableIterator.hasNext() )
      {
         SpecifiedTableTerm tableTerm = (SpecifiedTableTerm) tableIterator.next();
         tablesPart.append(getTableDeclaration(tableTerm.getTableName(),tableTerm.getAlias()));
         // This is a selected table, so include left terms
         for ( int i=0; i<tableTerm.getRelatedLeftTerms().size(); i++ )
         {
            SpecifiedTableTerm.LeftjoinEntry joinEntry = (SpecifiedTableTerm.LeftjoinEntry) 
               tableTerm.getRelatedLeftTerms().get(i);
            tablesPart.append(" left join ");
            tablesPart.append(getTableDeclaration(joinEntry.term.getTableName(),
                     joinEntry.term.getAlias()));
            tablesPart.append(" on ("+getExpression(joinEntry.expression,statementValues)+")");
         }
         // Add other left-joined tables on other specific attributes
         for ( int i=0; i<tableTerm.getReferencedLeftTerms().size(); i++ )
         {
            SpecifiedTableTerm.LeftjoinEntry joinEntry = (SpecifiedTableTerm.LeftjoinEntry) 
               tableTerm.getReferencedLeftTerms().get(i);
            tablesPart.append(" left join ");
            tablesPart.append(getTableDeclaration(joinEntry.term.getTableName(),
                     joinEntry.term.getAlias()));
            tablesPart.append(" on ("+getExpression(joinEntry.expression,statementValues)+")");
         }
         // Comma
         tablesPart.append(",");
      }
      tablesPart.delete(tablesPart.length()-1,tablesPart.length());
      // So first, parse conditions, also compute the conditions string
      // in the process. This could be easily written in a recursive
      // algorithm, but there are too many dependencies.
      StringBuffer conditionPart = new StringBuffer(); // Will hold the where statement
      if ( (expression != null) && (expression.size()>0) )
      {
         conditionPart.append(" where ");
         conditionPart.append(getExpression(expression,statementValues));
      }
      // Calculate order by
      StringBuffer orderByTerm = new StringBuffer();
      if ( (orderBys!=null) && (orderBys.size() > 0) )
      {
         orderByTerm.append(" order by ");
         for ( int i=0; i<orderBys.size(); i++ )
         {
            OrderBy orderBy = (OrderBy) orderBys.get(i);
            ReferenceTerm orderReferenceTerm = new ReferenceTerm(orderBy.getReferenceTerm());
            if ( 
                  ( (stmt.getMode()==QueryStatement.MODE_FIND) &&
                    (! stmt.getSelectTerms().contains(orderBy.getReferenceTerm())) ) ||
                  ( (stmt.getMode()==QueryStatement.MODE_VIEW) &&
                    (! containsReferenceTerm(stmt.getSelectTerms(),orderBy.getReferenceTerm())) )
               )
            {
               // Order by is not referencing the main tables. To maintain
               // distinct select, we must add this attribute to result
               querySource += ","+orderReferenceTerm.getName()+"."+orderReferenceTerm.getColumnName()+" as ordercol"+i;
               orderReferenceTerm.setColumnAlias("ordercol"+i);
               Class attributeType = getAttributeType(orderReferenceTerm.getTableName(),
                     orderReferenceTerm.getColumnName());
               types.add(attributeType);
            }
            // Add to order term
            if ( orderReferenceTerm.getColumnAlias() != null )
               orderByTerm.append(orderReferenceTerm.getColumnAlias());
            else
               orderByTerm.append(orderReferenceTerm.getName()+"."+orderReferenceTerm.getColumnName());
            orderByTerm.append(orderBy.getDirection()==OrderBy.ASCENDING?" asc,":" desc,");
         }
         orderByTerm.delete(orderByTerm.length()-1,orderByTerm.length());
      }
      if ( logger.isDebugEnabled() )
         logger.debug("select types: "+types);
      // Create count statement
      StringBuffer subStatement = new StringBuffer("select "+(isDistinct?"distinct ":"")+querySource+" from ");
      subStatement.append(tablesPart.toString());
      subStatement.append(conditionPart.toString());
      StringBuffer statement = new StringBuffer(getCountStatement(subStatement.toString()));
      logger.debug("preparing counting statement: "+statement.toString());
      String countStatement = statement.toString();
      // Prepare count statement
      PreparedStatement countPstmt;
      try
      {
         countPstmt = connection.prepareStatement(countStatement);
         for ( int i=0; i<statementValues.size(); i++ )
         {
            Object value = getSQLValue(statementValues.get(i));
            logger.debug("setting statement parameter #"+i+": "+value);
            sqlLogger.debug("setting statement parameter #"+i+": "+value);
            countPstmt.setObject(i+1,value);
         }
      } catch ( Exception e ) {
         throw new StoreException("cannot prepare statement: "+statement.toString(),e);
      }
      // Now create full statement (add order and limit)
      statement = new StringBuffer("select "+(isDistinct?"distinct ":"")+querySource+" from ");
      statement.append(tablesPart.toString());
      statement.append(conditionPart.toString());
      statement.append(orderByTerm.toString());
      // Add limits
      if ( limits != null )
         statement = new StringBuffer(getLimitStatement(statement.toString(),limits,types));
      // Run statement
      ArrayList result = new ArrayList();
      PreparedStatement pstmt = null;
      if ( (limits==null) || (!limits.isEmpty()) )
      {
         // Prepare
         try
         {
            pstmt = connection.prepareStatement(statement.toString());
            for ( int i=0; i<statementValues.size(); i++ )
            {
               Object value = getSQLValue(statementValues.get(i));
               logger.debug("setting statement parameter #"+i+": "+value);
               sqlLogger.debug("setting statement parameter #"+i+": "+value);
               pstmt.setObject(i+1,value);
            }
         } catch ( Exception e ) {
            throw new StoreException("cannot prepare statement: "+statement.toString(),e);
         }
         // Execute
         try
         {
            sqlLogger.debug("running select statement: "+statement.toString());
            long startTime = System.currentTimeMillis();
            prepareStatement(pstmt,limits);
            ResultSet rs = pstmt.executeQuery();
            long endTime = System.currentTimeMillis();
            aggregatorLogger.log("Query statement execution",new int[] { (int) (endTime-startTime) });
            stats.setSelectCount(stats.getSelectCount()+1);
            stats.setSelectTime(stats.getSelectTime()+(endTime-startTime));
            ResultSetMetaData rsmd = rs.getMetaData();
            // Get result and pack the attributes into a map
            prepareResultSet(rs,limits);
            while ( rs.next() )
            {
               Map attributes = new HashMap();
               for ( int i=0; i<rsmd.getColumnCount(); i++ )
               {
                  String columnName = rsmd.getColumnName(i+1).toLowerCase();
                  Object columnValue = getJavaValue(rs.getObject(i+1),rsmd.getColumnType(i+1),(Class) types.get(i));
                  if ( columnName.startsWith("persistence_start") )
                  {
                     // Handle persistence_starts
                     Long previousValue = (Long) attributes.get("persistence_start");
                     if ( (previousValue==null) || (previousValue.longValue() < 
                              ((Long) columnValue).longValue()) )
                        attributes.put("persistence_start",columnValue);
                  } else if ( columnName.startsWith("persistence_end") ) {
                     // Handle persistence_ends
                     Long previousValue = (Long) attributes.get("persistence_end");
                     if ( (previousValue==null) || (previousValue.longValue() >
                              ((Long) columnValue).longValue()) )
                        attributes.put("persistence_end",columnValue);
                  } else {
                     // Normal attributes
                     attributes.put(columnName, columnValue);
                  }
               }
               result.add(attributes);
            }
            rs.close();
         } catch ( Exception e ) {
            throw new StoreException("exception while sql select",e);
         } finally {
            try
            {
               pstmt.close();
            } catch ( Exception e ) {
               logger.debug("unable to close statement",e);
            }
         }
      } // End valid limits (running of query)
      // Execute count statement if necessary
      long resultSize;
      if ( limits == null )
      {
         // If there are no limits, the result is a full result
         resultSize = result.size();
      } else if ( limits.getSize()>=0 ) {
         // Size already known (possibly from previous select)
         resultSize = limits.getSize();
      } else if ( (limits.getLimit()>0) && (result.size()<limits.getLimit()) ) {
         // Count statement not necessary, this is the last page, we can
         // compute the size
         resultSize = limits.getOffset()+result.size();
      } else {
         // We must get the full size the hard way, so select
         if ( sqlLogger.isDebugEnabled() )
            sqlLogger.debug("running count statement: "+countStatement);
         try
         {
            long startTime = System.currentTimeMillis();
            ResultSet rs = countPstmt.executeQuery();
            long endTime = System.currentTimeMillis();
            aggregatorLogger.log("Query count statement execution",new int[] { (int) (endTime-startTime) });
            stats.setSelectCount(stats.getSelectCount()+1);
            stats.setSelectTime(stats.getSelectTime()+(endTime-startTime));
            rs.next();
            resultSize = rs.getLong(1);
            rs.close();
         } catch ( Exception e ) {
            throw new StoreException("exception while sql select count",e);
         } finally {
            try
            {
               countPstmt.close();
            } catch ( Exception e ) {
               logger.debug("unable to close statement",e);
            }
         }
      }
      // Assemble result and return
      searchResult.setResultSize(resultSize);
      searchResult.setResult(result);
      sqlLogger.debug("returning result, size: "+result.size()+" / "+resultSize);
      return stats;
   }

   /**
    * Convert incoming value from database into java format.
    */
   protected Object getJavaValue(Object value, int type, Class javaType)
   {
      if ( value instanceof Timestamp )
         return new Date(((Timestamp) value).getTime());
      return value;
   }

   /**
    * Convert incoming values from java into database acceptable format.
    */
   protected Object getSQLValue(Object value)
   {
      if ( (value!=null) && (value instanceof Date) )
         return new Timestamp(((Date) value).getTime());
      if( (value!=null) && value.getClass().isEnum())
    	  return ((Enum)value).name();
      return value;
   }
  
   /**
    * Get the class for an sql type.
    */
   protected String getSQLTypeName(int sqltype)
   {
      switch ( sqltype )
      {
         case Types.DATE:
         case Types.TIME:
         case Types.TIMESTAMP:
            return "timestamp";
         case Types.LONGVARCHAR:
         case Types.VARCHAR:
            return "text";
         case Types.BIT:
         case Types.BOOLEAN:
            return "boolean";
         case Types.INTEGER:
         case Types.NUMERIC:
         case Types.DECIMAL:
            return "integer";
         case Types.BIGINT:
            return "bigint";
         case Types.SMALLINT:
         case Types.TINYINT:
            return "smallint";
         case Types.DOUBLE:
         case Types.FLOAT:
         case Types.REAL:
            return "float";
         case Types.CHAR:
            return "char";
         case Types.BLOB:
         case Types.BINARY:
         case Types.VARBINARY:
         case Types.LONGVARBINARY:
            return "blob";
         default:
      }
      throw new StoreException("no sql type definition programmed for type: "+sqltype);
   }
   
   /**
    * Get the sql type string for a class.
    */
   protected int getSQLType(Class type)
   {
      if ( (Date.class.equals(type)) )
         return Types.TIMESTAMP;
      if ( String.class.equals(type) )
         return Types.VARCHAR;
      Class booleanClass = boolean.class;
      if ( (booleanClass.equals(type)) || (Boolean.class.equals(type)) )
         return Types.BOOLEAN;
      Class intClass = int.class;
      if ( (intClass.equals(type)) || (Integer.class.equals(type)) )
         return Types.INTEGER;
      Class longClass = long.class;
      if ( (longClass.equals(type)) || (Long.class.equals(type)) )
         return Types.BIGINT;
      Class byteClass = byte.class;
      if ( (byteClass.equals(type)) || (Byte.class.equals(type)) )
         return Types.SMALLINT;
      if ( byte[].class.equals(type) )
         return Types.BLOB;
      Class doubleClass = double.class;
      if ( (doubleClass.equals(type)) || (Double.class.equals(type)) )
         return Types.DOUBLE;
      Class floatClass = float.class;
      if ( (floatClass.equals(type)) || (Float.class.equals(type)) )
         return Types.FLOAT;
      Class shortClass = short.class;
      if ( (shortClass.equals(type)) || (Short.class.equals(type)) )
         return Types.SMALLINT;
      Class charClass = char.class;
      if ( (charClass.equals(type)) || (Character.class.equals(type)) )
         return Types.CHAR;
      if (type.isEnum())
    	  return Types.VARCHAR;
      throw new StoreException("type: "+type+" is not an allowed primitive type.");
   }

   /**
    * Execute update statement. Simply and safely execute the
    * given statement.
    * @param connection The connection to execute statement on.
    * @param statement The statement to execute.
    */
   protected void executeUpdate(Connection connection, String statement)
   {
      sqlLogger.debug("executing update statement: "+statement);
      // Prepare
      PreparedStatement pstmt;
      try
      {
         pstmt = connection.prepareStatement(statement.toString());
      } catch ( Exception e ) {
         throw new StoreException("cannot prepare statement: "+statement.toString(),e);
      }
      // Execute
      try
      {
         pstmt.executeUpdate();
      } catch ( Exception e ) {
         throw new StoreException("exception while executing statement: "+statement,e);
      } finally {
         try
         {
            pstmt.close();
         } catch ( Exception e ) {
            logger.debug("unable to close statement",e);
         }
      }
   }

   /**
    * Prepare the sql statment to be executed.
    */
   protected void prepareStatement(PreparedStatement pstmt, Limits limits)
      throws SQLException
   {
   }

   /**
    * Prepare the result set to be iterated.
    */
   protected void prepareResultSet(ResultSet rs, Limits limits)
      throws SQLException
   {
   }

}


