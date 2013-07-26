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
import java.util.Iterator;
import java.util.List;
import java.util.Date;
import java.sql.Connection;
import java.sql.Types;
import hu.netmind.beankeeper.parser.*;
import hu.netmind.beankeeper.common.StoreException;
import hu.netmind.beankeeper.transaction.*;
import hu.netmind.beankeeper.db.Database;
import hu.netmind.beankeeper.db.Limits;
import org.apache.log4j.Logger;

/**
 * MySQL database implementation.
 * Limitations:
 * <ul>
 *    <li>Date fields are stored only with seconds precision.</li>
 *    <li>Can not support more than 255 characters of map keys.</li>
 *    <li>InnoDB support must be compiled into MySQL to support
 *    transactions.</li>
 * </ul>
 * @author Brautigam Robert
 * @version CVS Revision: $Revision$
 */
public class MysqlDatabaseImpl extends GenericDatabase implements Database
{
   private static final int STRING_LENGTH = 255;

   private static Logger logger = Logger.getLogger(MysqlDatabaseImpl.class);

   public MysqlDatabaseImpl()
   {
      super();
   }
   
   /**
    * Get the limit component of statement, if it can be expressed in
    * the current database with simple statement part.
    * @param limits The limits to apply.
    */
   protected String getLimitStatement(String statement, Limits limits, List types)
   {
      StringBuffer result = new StringBuffer(statement);
      if ( limits.getLimit() > 0 )
         result.append(" limit "+limits.getLimit());
      if ( limits.getOffset() > 0 )
         result.append(" offset "+limits.getOffset());
      return result.toString();
   }

   /**
    * Create table with given name, attribute types, and keys.
    * Difference from generic: Mysql does not support 'text' type primary
    * keys, so avoid in keys.
    * @param tableName The table to create.
    * @param attributeTypes The attribute names together with which
    * java class they should hold.
    */
   protected TransactionStatistics createTable(Connection connection, String tableName,
         Map attributeTypes, List keyAttributeNames)
   {
      TransactionStatistics stats = new TransactionStatistics();
      // Create statement
      StringBuffer statement = new StringBuffer("create table "+tableName+" (");
      Iterator iterator = attributeTypes.entrySet().iterator();
      while ( iterator.hasNext() )
      {
         Map.Entry entry = (Map.Entry) iterator.next();
         String sqlTypeName = getSQLTypeName(getSQLType(((Class) entry.getValue())));
         // If attribute was a text type and a key, then alter type to
         // constrainted character string.
         if ( (sqlTypeName.startsWith("text")) && (keyAttributeNames.contains(entry.getKey())) )
            statement.append(entry.getKey().toString()+" varchar("+STRING_LENGTH+"),");
         else
            statement.append(entry.getKey().toString()+" "+sqlTypeName+",");
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
      statement.append(") type innodb");
      // Execute statement
      long startTime = System.currentTimeMillis();
      executeUpdate(connection,statement.toString());
      long endTime = System.currentTimeMillis();
      stats.setSchemaCount(1);
      stats.setSchemaTime(endTime-startTime);
      // Create initial indexes (currently all attributes will be indexed)
      stats.add(createIndexes(connection, tableName, attributeTypes));
      return stats;
   }

   /**
    * Convert incoming values into database acceptable format.
    */
   protected Object getSQLValue(Object value)
   {
      // Characters will be forwarded as strings
      if ( value instanceof Character )
         return value.toString();
      return super.getSQLValue(value);
   }
  
   /**
    * Convert incoming value from database into java format.
    */
   protected Object getJavaValue(Object value, int type, Class javaType)
   {
      try
      {
         if ( value == null )
            return null;
         Class booleanClass = boolean.class;
         if ( ((Boolean.class.equals(javaType)) || (booleanClass.equals(javaType))) &&
               (value instanceof Integer) )
            return new Boolean(((Integer) value).intValue() > 0);
         if ( (value instanceof byte[]) && (type==Types.LONGVARBINARY) && (String.class.equals(javaType)) )
            return new String((byte[]) value,"utf8");
         return super.getJavaValue(value,type,javaType);
      } catch ( StoreException e ) {
         throw e;
      } catch ( Exception e ) {
         throw new StoreException("conversion error tried to convert: "+value+"("+value.getClass()+"), of sql type: "+type+", java type: "+javaType);
      }
   }

   /**
    * Get the class for an sql type. Override timestamp to set default.
    */
   protected String getSQLTypeName(int sqltype)
   {
      switch ( sqltype )
      {
         case Types.TINYINT:
            return "boolean";
         case Types.TIMESTAMP:
            return "datetime";
         case Types.LONGVARCHAR:
         default:
            return super.getSQLTypeName(sqltype);
      }
   }

   /**
    * Transform 'ilike' to 'like', and 'like' to 'like binary' operators.
    * @param expr The expression to possibly transform.
    * @return A transformed expression.
    */
   protected Expression transformExpression(Expression expr)
   {
      Expression result = new Expression(expr);
      result.clear();
      for ( int i=0; i<expr.size(); i++ )
      {
         Object item = expr.get(i);
         if ( "like".equals(item) ||
              "=".equals(item) ||
              "!=".equals(item) ||
              "<".equals(item) ||
              "<=".equals(item) ||
              ">=".equals(item) ||
              ">".equals(item) ||
              "<>".equals(item)
               )
         {
            result.add(item);
            // Now, if any of the arguments to the operator is
            // a string, then we must apply 'binary' to make
            // the operator case sensitive.
            // Note: collection constant terms are not handled,
            // because they may be empty anyway.
            // Lhs and Rhs must exist, because these
            // operators are infix operators.
            Object lhs = expr.get(i-1);
            Object rhs = expr.get(i+1);
            if ( ("like".equals(item)) || (isStringType(lhs)) || 
                  (isStringType(rhs)) )
               result.add("binary");
         } else if ( "ilike".equals(item) )
            result.add("like");
         else
            result.add(item);
      }
      return result;
   }

   /**
    * Determine whether a term is string type or not.
    * @return True if the term represents a String type, false if unknown.
    */
   private boolean isStringType(Object term)
   {
      if ( term instanceof ConstantTerm )
      {
         ConstantTerm constantTerm = (ConstantTerm) term;
         return (constantTerm.getValue() != null) && (constantTerm.getValue() instanceof String);
      }
      if ( term instanceof ReferenceTerm )
      {
         ReferenceTerm refTerm = (ReferenceTerm) term;
         return String.class.equals(getAttributeType(refTerm.getTableName(),refTerm.getColumnName()));
      }
      return false;
   }

   /**
    * Get index creation statement for a given table and field.
    * @return The statement to use, or null, of no such index can be
    * created.
    */
   protected String getCreateIndexStatement(String indexName,String tableName, String field,
         Class fieldClass)
   {
      if ( fieldClass.equals(String.class) )
         return "create index "+indexName+
            " on "+tableName+" ("+field+"("+STRING_LENGTH+"))";
      return super.getCreateIndexStatement(indexName,tableName,field,fieldClass);
   }

}



