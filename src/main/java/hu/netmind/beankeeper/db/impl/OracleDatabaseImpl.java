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

import java.sql.Types;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.List;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.util.Date;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import org.apache.log4j.Logger;
import hu.netmind.beankeeper.parser.*;
import hu.netmind.beankeeper.common.StoreException;
import hu.netmind.beankeeper.db.*;

/**
 * Oracle database implementation.
 * Limitations:
 * <ul>
 *    <li>Strings are limited to 1024 characters.</li>
 * </ul>
 * @author Brautigam Robert
 * @version CVS Revision: $Revision$
 */
public class OracleDatabaseImpl extends GenericDatabase implements Database
{
   private static Logger logger = Logger.getLogger(OracleDatabaseImpl.class);
   private static Class blobClass = null;
   private static Class timestampClass = null;

   public OracleDatabaseImpl()
   {
      super();
   }
   
   /**
    * Set the limits of the prepared statement if offset is 0. This
    * is to avoid using complicated limit+offset inner selects, when
    * the first page is queried.
    */
   protected void prepareStatement(PreparedStatement pstmt, Limits limits)
      throws SQLException
   {
      if ( (limits == null) || (limits.isEmpty()) )
         return;
      if ( (limits.getOffset()==0) && (limits.getLimit()>0) )
         pstmt.setMaxRows((int) limits.getLimit());
   }

   /**
    * Get the limit component of statement, if it can be expressed in
    * the current database with simple statement part.
    * @param limits The limits to apply.
    */
   protected String getLimitStatement(String statement,Limits limits, List types)
   {
      if ( (limits.getLimit() == 0) || (limits.getOffset() == 0) )
      {
         if ( limits.getOffset() != 0 )
         {
            // No limit, there should be no offset
            throw new StoreException("received a limit of 0, but offset was: "+limits.getOffset());
         }
         // Nothing
         return statement;
      }
      // Add type of rownum (see below for explanation)
      types.add(Long.class);
      // Now, oracle does not have limits and offsets,
      // so don't look at me, this is the way it's done.
      // In a stateless environment anyway.
      return "select * from (select sub.*, rownum rnum from ("+statement+
         ") sub where rownum <= "+(limits.getOffset()+limits.getLimit())+
         ") where rnum > "+limits.getOffset();
   }

   /**
    * Override to correct type conflicts an unsupported types.
    */
   protected String getSQLTypeName(int sqltype)
   {
      switch ( sqltype )
      {
         case Types.LONGVARCHAR:
         case Types.VARCHAR:
            return "varchar2(1024)";
         case Types.BIGINT:
         case Types.INTEGER:
         case Types.SMALLINT:
         case Types.DECIMAL:
            return "number(*,0)";
         case Types.BIT:
         case Types.BOOLEAN:
            return "number(1,0)";
         case Types.CHAR:
            return "char(1)";
         default:
            return super.getSQLTypeName(sqltype);
      }
   }

   /**
    * Get the sql type string for a class.
    */
   protected int getSQLType(Class type)
   {
      // Booleans are numbers here
      Class booleanClass = boolean.class;
      if ( (booleanClass.equals(type)) || (Boolean.class.equals(type)) )
         return Types.INTEGER;
      // We don't really want to create BLOB objects, because those are
      // complex to handle
      if ( byte[].class.equals(type) )
         return Types.LONGVARBINARY;
      // Re-use super
      return super.getSQLType(type);
   }

   /**
    * Throw exception when String is longer than Oracle can handle.
    */
   protected Object getSQLValue(Object value)
   {
      if ( (value instanceof String) && ("".equals((String) value)) )
         return new String("$"); // Don't let empty string through
      if ( value instanceof String )
      {
         // Escape all $ signs
         StringBuffer str = new StringBuffer((String) value);
         for ( int i=0; i<str.length(); i++ )
         {
            if ( str.charAt(i) == '$' )
            {
               str.insert(i,"$");
               i++;
            }
         }
         return str.toString();
      }
      if ( value instanceof Character )
         return value.toString();
      if ( value instanceof Boolean )
         return new Integer(((Boolean) value).booleanValue()?1:0);
      if ( (value instanceof String) && (((String) value).length() > 1024) )
         throw new StoreException("received a string which was too long for Oracle to handle "+
               "(more than 1024 characters), the string started with: "+((String)value).substring(0,100));
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
         logger.debug("transforming value: "+value+", type: "+type+", java type: "+javaType);
         Class booleanClass = boolean.class;
         if ( (Boolean.class.equals(javaType)) || (booleanClass.equals(javaType)) )
            return new Boolean( ((Number) value).intValue() > 0 );
         if ( (value instanceof String) && (((String) value).length()==1)
               && (((String) value).charAt(0) == '$') )
            return ""; // Deconvert dollar to empty string
         if ( value instanceof String )
         {
            // Unescape all $ signs
            StringBuffer str = new StringBuffer((String) value);
            for ( int i=0; i<str.length(); i++ )
            {
               if ( str.charAt(i) == '$' )
               {
                  str.deleteCharAt(i);
                  i--;
               }
            }
            return str.toString();
         }  
         if ( blobClass.isAssignableFrom(value.getClass()) )
         {
            // Here we must read out the actual values in the blob
            InputStream blobStream = (InputStream) internalCall(value,"getBinaryStream");
            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            int b = 0;
            while ( (b = blobStream.read()) >= 0 )
               byteOut.write( (byte) b );
            blobStream.close();
            byteOut.close();
            return byteOut.toByteArray();
         }
         if ( timestampClass.isAssignableFrom(value.getClass()) )
            return (Date) internalCall(value,"timestampValue");
         if ( value instanceof BigDecimal )
         {
            if ( javaType.equals(Long.class) || javaType.equals(long.class) )
            return new Long(((BigDecimal) value).longValue());
            if ( javaType.equals(Integer.class) || javaType.equals(int.class) )
            return new Integer(((BigDecimal) value).intValue());
         }
         return super.getJavaValue(value,type,javaType);
      } catch ( StoreException e ) {
         throw e;
      } catch ( Exception e ) {
         throw new StoreException("conversion error tried to convert: "+value+", of sql type: "+type,e);
      }
   }

   /**
    * This method's purpose is to decouple this code from Oracle libraries, because
    * those are not freely available.
    */
   private Object internalCall(Object obj, String methodName)
      throws NoSuchMethodException, IllegalAccessException, InvocationTargetException
   {
      return internalCall(obj,methodName,new Class[] {}, new Object[] {});
   }

   /**
    * This method's purpose is to decouple this code from Oracle libraries, because
    * those are not freely available.
    */
   private Object internalCall(Object obj, String methodName, Class[] classes, Object[] params)
      throws NoSuchMethodException, IllegalAccessException, InvocationTargetException
   {
      Method method = obj.getClass().getMethod(methodName, classes);
      return method.invoke(obj, params);
   }

   /**
    * Get the count statement for the given statement.
    */
   protected String getCountStatement(String stmt)
   {
      return "select count(*) from ("+stmt+")";
   }

   /**
    * Get the data types of a given table.
    * @return A map of names with the sql type number as value.
    */
   protected HashMap getTableAttributeTypes(Connection connection,
         String tableName)
      throws SQLException
   {
      return super.getTableAttributeTypes(connection,tableName.toUpperCase());
   }

   /**
    * Fix custom data types not supported by database.
    */
   protected int getTableAttributeType(ResultSet rs)
      throws SQLException
   {
      int columnType = rs.getInt("DATA_TYPE");
      // Recognize boolean type
      if ( (columnType == Types.DECIMAL) && (rs.getInt("COLUMN_SIZE")==1) )
         columnType = Types.BOOLEAN;
      return columnType;
   }
   
   /**
    * Get the table declaration for a select statment.
    */
   protected String getTableDeclaration(String tableName, String alias)
   {
      if ( alias == null )
         return tableName;
      else
         return tableName+" "+alias;
   }

   /**
    * Transform functions for hsql.
    */
   protected List transformTerms(List terms)
   {
      for ( int i=0; i<terms.size(); i++ )
      {
         Object term = terms.get(i);
         if ( term instanceof ReferenceTerm )
         {
            Function function = ((ReferenceTerm) term).getFunction();
            if ( (function!=null) && (function instanceof MathematicalPostfixFunction) &&
                  (((MathematicalPostfixFunction)function).getFunction().equals(">>")) )
            {
               // Oracle does not support the bitshift operator, so we modify
               // the function
               MathematicalPostfixFunction mathFunction = (MathematicalPostfixFunction) function;
               ((ReferenceTerm)term).setFunction(new PrefixFunction("floor",
                  new MathematicalPostfixFunction("/",""+
                     (1L<<(Long.parseLong(mathFunction.getOperand()))) )));
            }
         }
      }
      return terms;
   }

   /**
    * Get the statement to add a column to a table.
    */
   protected String getAddColumnStatement(String tableName, String columnName, String columnType)
   {
      return "alter table "+tableName+" add "+columnName+" "+columnType;
   }

   /**
    * Transform 'ilike' to upper case like.
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
         if ( "ilike".equals(item) )
         {
            // Here we need to upper() the argument before and after like
            Object arg = result.remove(result.size()-1);
            result.add("upper(");
            result.add(arg);
            result.add(")");
            result.add("like");
            result.add("upper(");
            result.add(expr.get(i+1));
            result.add(")");
            i++; // We used an argument
         } else {
            result.add(item);
         }
      }
      // Transform functions too
      transformTerms(result);
      return result;
   }
   
   /**
    * Get an unused index name.
    */
   protected String getCreateIndexName(Connection connection, String tableName,
         String field)
   {
      return super.getCreateIndexName(connection,tableName.toUpperCase(),field);
   }

   static
   {
      try
      {
         blobClass = Class.forName("oracle.sql.BLOB");
         timestampClass = Class.forName("oracle.sql.TIMESTAMP");
      } catch ( Exception e ) {
         logger.fatal("could not get necessary oracle classes, oracle database will probably not work",e);
      }
   }
}



